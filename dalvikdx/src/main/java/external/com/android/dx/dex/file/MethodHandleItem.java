/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package external.com.android.dx.dex.file;

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstBaseMethodRef;
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.rop.cst.CstInterfaceMethodRef;
import external.com.android.dx.rop.cst.CstMethodHandle;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;

/**
 * Representation of a method handle in a DEX file.
 */
public final class MethodHandleItem extends IndexedItem {

    /** The item size when placed in a DEX file. */
    private final int ITEM_SIZE = 8;

    /** {@code non-null;} The method handle represented by this item. */
    private final CstMethodHandle methodHandle;

    /**
     * Constructs an instance.
     *
     * @param methodHandle {@code non-null;} The method handle to represent in the DEX file.
     */
    public MethodHandleItem(CstMethodHandle methodHandle) {
        this.methodHandle = methodHandle;
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_METHOD_HANDLE_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public int writeSize() {
        return ITEM_SIZE;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        MethodHandlesSection methodHandles = file.getMethodHandles();
        methodHandles.intern(methodHandle);
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(DexFile file, AnnotatedOutput out) {
        int targetIndex = getTargetIndex(file);
        int mhType = methodHandle.getMethodHandleType();
        if (out.annotates()) {
            out.annotate(0, indexString() + ' ' + methodHandle.toString());
            String typeComment = " // " + CstMethodHandle.getMethodHandleTypeName(mhType);
            out.annotate(2, "type:     " + Hex.u2(mhType) + typeComment);
            out.annotate(2, "reserved: " + Hex.u2(0));
            String targetComment = " // " +  methodHandle.getRef().toString();
            if (methodHandle.isAccessor()) {
                out.annotate(2, "fieldId:  " + Hex.u2(targetIndex) + targetComment);
            } else {
                out.annotate(2, "methodId: " + Hex.u2(targetIndex) + targetComment);
            }
            out.annotate(2, "reserved: " + Hex.u2(0));
        }
        out.writeShort(mhType);
        out.writeShort(0);
        out.writeShort(getTargetIndex(file));
        out.writeShort(0);
    }

    private int getTargetIndex(DexFile file) {
        Constant ref = methodHandle.getRef();
        if (methodHandle.isAccessor()) {
            FieldIdsSection fieldIds = file.getFieldIds();
            return fieldIds.indexOf((CstFieldRef) ref);
        } else if (methodHandle.isInvocation()) {
            if (ref instanceof CstInterfaceMethodRef) {
                ref = ((CstInterfaceMethodRef)ref).toMethodRef();
            }
            MethodIdsSection methodIds = file.getMethodIds();
            return methodIds.indexOf((CstBaseMethodRef) ref);
        } else {
            throw new IllegalStateException("Unhandled invocation type");
        }
    }
}

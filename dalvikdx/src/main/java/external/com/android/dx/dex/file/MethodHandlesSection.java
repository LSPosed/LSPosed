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
import external.com.android.dx.rop.cst.CstMethodHandle;
import java.util.Collection;
import java.util.TreeMap;

public final class MethodHandlesSection extends UniformItemSection {

    private final TreeMap<CstMethodHandle, MethodHandleItem> methodHandles = new TreeMap<>();

    public MethodHandlesSection(DexFile dexFile) {
        super("method_handles", dexFile, 8);
    }

    @Override
    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();

        IndexedItem result = methodHandles.get((CstMethodHandle) cst);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }

    @Override
    protected void orderItems() {
        int index = 0;
        for (MethodHandleItem item : methodHandles.values()) {
            item.setIndex(index++);
        }
    }

    @Override
    public Collection<? extends Item> items() {
        return methodHandles.values();
    }

    public void intern(CstMethodHandle methodHandle) {
        if (methodHandle == null) {
            throw new NullPointerException("methodHandle == null");
        }

        throwIfPrepared();

        MethodHandleItem result = methodHandles.get(methodHandle);
        if (result == null) {
            result = new MethodHandleItem(methodHandle);
            methodHandles.put(methodHandle, result);
        }
    }

    int indexOf(CstMethodHandle cstMethodHandle) {
        return methodHandles.get(cstMethodHandle).getIndex();
    }
}

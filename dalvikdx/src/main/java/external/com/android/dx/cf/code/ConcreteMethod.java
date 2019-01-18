/*
 * Copyright (C) 2007 The Android Open Source Project
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

package external.com.android.dx.cf.code;

import external.com.android.dx.cf.attrib.AttCode;
import external.com.android.dx.cf.attrib.AttLineNumberTable;
import external.com.android.dx.cf.attrib.AttLocalVariableTable;
import external.com.android.dx.cf.attrib.AttLocalVariableTypeTable;
import external.com.android.dx.cf.iface.AttributeList;
import external.com.android.dx.cf.iface.ClassFile;
import external.com.android.dx.cf.iface.Method;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Prototype;

/**
 * Container for all the giblets that make up a concrete Java bytecode method.
 * It implements {@link Method}, so it provides all the original access
 * (by delegation), but it also constructs and keeps useful versions of
 * stuff extracted from the method's {@code Code} attribute.
 */
public final class ConcreteMethod implements Method {
    /** {@code non-null;} method being wrapped */
    private final Method method;

    /** {@code non-null;} the {@code ClassFile} the method belongs to. */
    private final ClassFile classFile;

    /** {@code non-null;} the code attribute */
    private final AttCode attCode;

    /** {@code non-null;} line number list */
    private final LineNumberList lineNumbers;

    /** {@code non-null;} local variable list */
    private final LocalVariableList localVariables;

    /**
     * Constructs an instance.
     *
     * @param method {@code non-null;} the method to be based on
     * @param classFile {@code non-null;} the class file that contains this method
     * @param keepLines whether to keep the line number information
     * (if any)
     * @param keepLocals whether to keep the local variable
     * information (if any)
     */
    public ConcreteMethod(Method method, ClassFile classFile,
            boolean keepLines, boolean keepLocals) {
        this.method = method;
        this.classFile = classFile;

        AttributeList attribs = method.getAttributes();
        this.attCode = (AttCode) attribs.findFirst(AttCode.ATTRIBUTE_NAME);

        AttributeList codeAttribs = attCode.getAttributes();

        /*
         * Combine all LineNumberTable attributes into one, with the
         * combined result saved into the instance. The following code
         * isn't particularly efficient for doing merges, but as far
         * as I know, this situation rarely occurs "in the
         * wild," so there's not much point in optimizing for it.
         */
        LineNumberList lnl = LineNumberList.EMPTY;
        if (keepLines) {
            for (AttLineNumberTable lnt = (AttLineNumberTable)
                     codeAttribs.findFirst(AttLineNumberTable.ATTRIBUTE_NAME);
                 lnt != null;
                 lnt = (AttLineNumberTable) codeAttribs.findNext(lnt)) {
                lnl = LineNumberList.concat(lnl, lnt.getLineNumbers());
            }
        }
        this.lineNumbers = lnl;

        LocalVariableList lvl = LocalVariableList.EMPTY;
        if (keepLocals) {
            /*
             * Do likewise (and with the same caveat) for
             * LocalVariableTable and LocalVariableTypeTable attributes.
             * This combines both of these kinds of attribute into a
             * single LocalVariableList.
             */
            for (AttLocalVariableTable lvt = (AttLocalVariableTable)
                     codeAttribs.findFirst(
                             AttLocalVariableTable.ATTRIBUTE_NAME);
                 lvt != null;
                 lvt = (AttLocalVariableTable) codeAttribs.findNext(lvt)) {

                lvl = LocalVariableList.concat(lvl, lvt.getLocalVariables());
            }

            LocalVariableList typeList = LocalVariableList.EMPTY;
            for (AttLocalVariableTypeTable lvtt = (AttLocalVariableTypeTable)
                     codeAttribs.findFirst(
                             AttLocalVariableTypeTable.ATTRIBUTE_NAME);
                 lvtt != null;
                 lvtt = (AttLocalVariableTypeTable) codeAttribs.findNext(lvtt)) {
                typeList = LocalVariableList.concat(typeList, lvtt.getLocalVariables());
            }

            if (typeList.size() != 0) {

                lvl = LocalVariableList.mergeDescriptorsAndSignatures(lvl, typeList);
            }
        }
        this.localVariables = lvl;
    }


    /**
     * Gets the source file associated with the method if known.
     * @return {null-ok;} the source file defining the method if known, null otherwise.
     */
    public CstString getSourceFile() {
        return classFile.getSourceFile();
    }

    /**
     * Tests whether the method is being defined on an interface.
     * @return true if the method is being defined on an interface.
     */
    public final boolean isDefaultOrStaticInterfaceMethod() {
        return (classFile.getAccessFlags() & AccessFlags.ACC_INTERFACE) != 0
            && !getNat().isClassInit();
    }

    /**
     * Tests whether the method is being defined is declared as static.
     * @return true if the method is being defined is declared as static.
     */
    public final boolean isStaticMethod() {
        return (getAccessFlags() & AccessFlags.ACC_STATIC) != 0;
    }

    /** {@inheritDoc} */
    @Override
    public CstNat getNat() {
        return method.getNat();
    }

    /** {@inheritDoc} */
    @Override
    public CstString getName() {
        return method.getName();
    }

    /** {@inheritDoc} */
    @Override
    public CstString getDescriptor() {
        return method.getDescriptor();
    }

    /** {@inheritDoc} */
    @Override
    public int getAccessFlags() {
        return method.getAccessFlags();
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList getAttributes() {
        return method.getAttributes();
    }

    /** {@inheritDoc} */
    @Override
    public CstType getDefiningClass() {
        return method.getDefiningClass();
    }

    /** {@inheritDoc} */
    @Override
    public Prototype getEffectiveDescriptor() {
        return method.getEffectiveDescriptor();
    }

    /**
     * Gets the maximum stack size.
     *
     * @return {@code >= 0;} the maximum stack size
     */
    public int getMaxStack() {
        return attCode.getMaxStack();
    }

    /**
     * Gets the number of locals.
     *
     * @return {@code >= 0;} the number of locals
     */
    public int getMaxLocals() {
        return attCode.getMaxLocals();
    }

    /**
     * Gets the bytecode array.
     *
     * @return {@code non-null;} the bytecode array
     */
    public BytecodeArray getCode() {
        return attCode.getCode();
    }

    /**
     * Gets the exception table.
     *
     * @return {@code non-null;} the exception table
     */
    public ByteCatchList getCatches() {
        return attCode.getCatches();
    }

    /**
     * Gets the line number list.
     *
     * @return {@code non-null;} the line number list
     */
    public LineNumberList getLineNumbers() {
        return lineNumbers;
    }

    /**
     * Gets the local variable list.
     *
     * @return {@code non-null;} the local variable list
     */
    public LocalVariableList getLocalVariables() {
        return localVariables;
    }

    /**
     * Returns a {@link SourcePosition} instance corresponding to the
     * given bytecode offset.
     *
     * @param offset {@code >= 0;} the bytecode offset
     * @return {@code non-null;} an appropriate instance
     */
    public SourcePosition makeSourcePosistion(int offset) {
        return new SourcePosition(getSourceFile(), offset,
                                  lineNumbers.pcToLine(offset));
    }
}

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

package external.com.android.dx.dex.code;

import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.util.Hex;

/**
 * Instruction which has a single constant argument in addition
 * to all the normal instruction information.
 */
public final class CstInsn extends FixedSizeInsn {
    /** {@code non-null;} the constant argument for this instruction */
    private final Constant constant;

    /**
     * {@code >= -1;} the constant pool index for {@link #constant}, or
     * {@code -1} if not yet set
     */
    private int index;

    /**
     * {@code >= -1;} the constant pool index for the class reference in
     * {@link #constant} if any, or {@code -1} if not yet set
     */
    private int classIndex;

    /**
     * Constructs an instance. The output address of this instance is
     * initially unknown ({@code -1}) as is the constant pool index.
     *
     * @param opcode the opcode; one of the constants from {@link Dops}
     * @param position {@code non-null;} source position
     * @param registers {@code non-null;} register list, including a
     * result register if appropriate (that is, registers may be either
     * ins or outs)
     * @param constant {@code non-null;} constant argument
     */
    public CstInsn(Dop opcode, SourcePosition position,
                   RegisterSpecList registers, Constant constant) {
        super(opcode, position, registers);

        if (constant == null) {
            throw new NullPointerException("constant == null");
        }

        this.constant = constant;
        this.index = -1;
        this.classIndex = -1;
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withOpcode(Dop opcode) {
        CstInsn result =
            new CstInsn(opcode, getPosition(), getRegisters(), constant);

        if (index >= 0) {
            result.setIndex(index);
        }

        if (classIndex >= 0) {
            result.setClassIndex(classIndex);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        CstInsn result =
            new CstInsn(getOpcode(), getPosition(), registers, constant);

        if (index >= 0) {
            result.setIndex(index);
        }

        if (classIndex >= 0) {
            result.setClassIndex(classIndex);
        }

        return result;
    }

    /**
     * Gets the constant argument.
     *
     * @return {@code non-null;} the constant argument
     */
    public Constant getConstant() {
        return constant;
    }

    /**
     * Gets the constant's index. It is only valid to call this after
     * {@link #setIndex} has been called.
     *
     * @return {@code >= 0;} the constant pool index
     */
    public int getIndex() {
        if (index < 0) {
            throw new IllegalStateException("index not yet set for " + constant);
        }

        return index;
    }

    /**
     * Returns whether the constant's index has been set for this instance.
     *
     * @see #setIndex
     *
     * @return {@code true} iff the index has been set
     */
    public boolean hasIndex() {
        return (index >= 0);
    }

    /**
     * Sets the constant's index. It is only valid to call this method once
     * per instance.
     *
     * @param index {@code index >= 0;} the constant pool index
     */
    public void setIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }

        if (this.index >= 0) {
            throw new IllegalStateException("index already set");
        }

        this.index = index;
    }

    /**
     * Gets the constant's class index. It is only valid to call this after
     * {@link #setClassIndex} has been called.
     *
     * @return {@code >= 0;} the constant's class's constant pool index
     */
    public int getClassIndex() {
        if (classIndex < 0) {
            throw new IllegalStateException("class index not yet set");
        }

        return classIndex;
    }

    /**
     * Returns whether the constant's class index has been set for this
     * instance.
     *
     * @see #setClassIndex
     *
     * @return {@code true} iff the index has been set
     */
    public boolean hasClassIndex() {
        return (classIndex >= 0);
    }

    /**
     * Sets the constant's class index. This is the constant pool index
     * for the class referred to by this instance's constant. Only
     * reference constants have a class, so it is only on instances
     * with reference constants that this method should ever be
     * called. It is only valid to call this method once per instance.
     *
     * @param index {@code index >= 0;} the constant's class's constant pool index
     */
    public void setClassIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }

        if (this.classIndex >= 0) {
            throw new IllegalStateException("class index already set");
        }

        this.classIndex = index;
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        return constant.toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public String cstString() {
        if (constant instanceof CstString) {
            return ((CstString) constant).toQuoted();
        }
        return constant.toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public String cstComment() {
        if (!hasIndex()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(20);
        sb.append(getConstant().typeName());
        sb.append('@');

        if (index < 65536) {
            sb.append(Hex.u2(index));
        } else {
            sb.append(Hex.u4(index));
        }

        return sb.toString();
    }
}

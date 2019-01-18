/*
 * Copyright (C) 2008 The Android Open Source Project
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

import external.com.android.dx.io.Opcodes;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstLiteral32;
import external.com.android.dx.rop.cst.CstLiteral64;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.util.ArrayList;

/**
 * Pseudo-instruction which holds fill array data.
 */
public final class ArrayData extends VariableSizeInsn {
    /**
     * {@code non-null;} address representing the instruction that uses this
     * instance
     */
    private final CodeAddress user;

    /** {@code non-null;} initial values to be filled into an array */
    private final ArrayList<Constant> values;

    /** non-null: type of constant that initializes the array */
    private final Constant arrayType;

    /** Width of the init value element */
    private final int elemWidth;

    /** Length of the init list */
    private final int initLength;

    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param position {@code non-null;} source position
     * @param user {@code non-null;} address representing the instruction that
     * uses this instance
     * @param values {@code non-null;} initial values to be filled into an array
     */
    public ArrayData(SourcePosition position, CodeAddress user,
                     ArrayList<Constant> values,
                     Constant arrayType) {
        super(position, RegisterSpecList.EMPTY);

        if (user == null) {
            throw new NullPointerException("user == null");
        }

        if (values == null) {
            throw new NullPointerException("values == null");
        }

        int sz = values.size();

        if (sz <= 0) {
            throw new IllegalArgumentException("Illegal number of init values");
        }

        this.arrayType = arrayType;

        if (arrayType == CstType.BYTE_ARRAY ||
                arrayType == CstType.BOOLEAN_ARRAY) {
            elemWidth = 1;
        } else if (arrayType == CstType.SHORT_ARRAY ||
                arrayType == CstType.CHAR_ARRAY) {
            elemWidth = 2;
        } else if (arrayType == CstType.INT_ARRAY ||
                arrayType == CstType.FLOAT_ARRAY) {
            elemWidth = 4;
        } else if (arrayType == CstType.LONG_ARRAY ||
                arrayType == CstType.DOUBLE_ARRAY) {
            elemWidth = 8;
        } else {
            throw new IllegalArgumentException("Unexpected constant type");
        }
        this.user = user;
        this.values = values;
        initLength = values.size();
    }

    /** {@inheritDoc} */
    @Override
    public int codeSize() {
        int sz = initLength;
        // Note: the unit here is 16-bit
        return 4 + ((sz * elemWidth) + 1) / 2;
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out) {
        int sz = values.size();

        out.writeShort(Opcodes.FILL_ARRAY_DATA_PAYLOAD);
        out.writeShort(elemWidth);
        out.writeInt(initLength);


        // For speed reasons, replicate the for loop in each case
        switch (elemWidth) {
            case 1: {
                for (int i = 0; i < sz; i++) {
                    Constant cst = values.get(i);
                    out.writeByte((byte) ((CstLiteral32) cst).getIntBits());
                }
                break;
            }
            case 2: {
                for (int i = 0; i < sz; i++) {
                    Constant cst = values.get(i);
                    out.writeShort((short) ((CstLiteral32) cst).getIntBits());
                }
                break;
            }
            case 4: {
                for (int i = 0; i < sz; i++) {
                    Constant cst = values.get(i);
                    out.writeInt(((CstLiteral32) cst).getIntBits());
                }
                break;
            }
            case 8: {
                for (int i = 0; i < sz; i++) {
                    Constant cst = values.get(i);
                    out.writeLong(((CstLiteral64) cst).getLongBits());
                }
                break;
            }
            default:
                break;
        }

        // Pad one byte to make the size of data table multiples of 16-bits
        if (elemWidth == 1 && (sz % 2 != 0)) {
            out.writeByte(0x00);
        }
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new ArrayData(getPosition(), user, values, arrayType);
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        StringBuilder sb = new StringBuilder(100);

        int sz = values.size();
        for (int i = 0; i < sz; i++) {
            sb.append("\n    ");
            sb.append(i);
            sb.append(": ");
            sb.append(values.get(i).toHuman());
        }

        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected String listingString0(boolean noteIndices) {
        int baseAddress = user.getAddress();
        StringBuilder sb = new StringBuilder(100);
        int sz = values.size();

        sb.append("fill-array-data-payload // for fill-array-data @ ");
        sb.append(Hex.u2(baseAddress));

        for (int i = 0; i < sz; i++) {
            sb.append("\n  ");
            sb.append(i);
            sb.append(": ");
            sb.append(values.get(i).toHuman());
        }

        return sb.toString();
    }
}

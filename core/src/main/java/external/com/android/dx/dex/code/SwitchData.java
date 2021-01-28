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

import external.com.android.dx.io.Opcodes;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;

/**
 * Pseudo-instruction which holds switch data. The switch data is
 * a map of values to target addresses, and this class writes the data
 * in either a "packed" or "sparse" form.
 */
public final class SwitchData extends VariableSizeInsn {
    /**
     * {@code non-null;} address representing the instruction that uses this
     * instance
     */
    private final CodeAddress user;

    /** {@code non-null;} sorted list of switch cases (keys) */
    private final IntList cases;

    /**
     * {@code non-null;} corresponding list of code addresses; the branch
     * target for each case
     */
    private final CodeAddress[] targets;

    /** whether the output table will be packed (vs. sparse) */
    private final boolean packed;

    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param position {@code non-null;} source position
     * @param user {@code non-null;} address representing the instruction that
     * uses this instance
     * @param cases {@code non-null;} sorted list of switch cases (keys)
     * @param targets {@code non-null;} corresponding list of code addresses; the
     * branch target for each case
     */
    public SwitchData(SourcePosition position, CodeAddress user,
                      IntList cases, CodeAddress[] targets) {
        super(position, RegisterSpecList.EMPTY);

        if (user == null) {
            throw new NullPointerException("user == null");
        }

        if (cases == null) {
            throw new NullPointerException("cases == null");
        }

        if (targets == null) {
            throw new NullPointerException("targets == null");
        }

        int sz = cases.size();

        if (sz != targets.length) {
            throw new IllegalArgumentException("cases / targets mismatch");
        }

        if (sz > 65535) {
            throw new IllegalArgumentException("too many cases");
        }

        this.user = user;
        this.cases = cases;
        this.targets = targets;
        this.packed = shouldPack(cases);
    }

    /** {@inheritDoc} */
    @Override
    public int codeSize() {
        return packed ? (int) packedCodeSize(cases) :
            (int) sparseCodeSize(cases);
    }

    /** {@inheritDoc} */
    @Override
    public void writeTo(AnnotatedOutput out) {
        int baseAddress = user.getAddress();
        int defaultTarget = Dops.PACKED_SWITCH.getFormat().codeSize();
        int sz = targets.length;

        if (packed) {
            int firstCase = (sz == 0) ? 0 : cases.get(0);
            int lastCase = (sz == 0) ? 0 : cases.get(sz - 1);
            int outSz = lastCase - firstCase + 1;

            out.writeShort(Opcodes.PACKED_SWITCH_PAYLOAD);
            out.writeShort(outSz);
            out.writeInt(firstCase);

            int caseAt = 0;
            for (int i = 0; i < outSz; i++) {
                int outCase = firstCase + i;
                int oneCase = cases.get(caseAt);
                int relTarget;

                if (oneCase > outCase) {
                    relTarget = defaultTarget;
                } else {
                    relTarget = targets[caseAt].getAddress() - baseAddress;
                    caseAt++;
                }

                out.writeInt(relTarget);
            }
        } else {
            out.writeShort(Opcodes.SPARSE_SWITCH_PAYLOAD);
            out.writeShort(sz);

            for (int i = 0; i < sz; i++) {
                out.writeInt(cases.get(i));
            }

            for (int i = 0; i < sz; i++) {
                int relTarget = targets[i].getAddress() - baseAddress;
                out.writeInt(relTarget);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new SwitchData(getPosition(), user, cases, targets);
    }

    /**
     * Returns whether or not this instance's data will be output as packed.
     *
     * @return {@code true} iff the data is to be packed
     */
    public boolean isPacked() {
        return packed;
    }

    /** {@inheritDoc} */
    @Override
    protected String argString() {
        StringBuilder sb = new StringBuilder(100);

        int sz = targets.length;
        for (int i = 0; i < sz; i++) {
            sb.append("\n    ");
            sb.append(cases.get(i));
            sb.append(": ");
            sb.append(targets[i]);
        }

        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected String listingString0(boolean noteIndices) {
        int baseAddress = user.getAddress();
        StringBuilder sb = new StringBuilder(100);
        int sz = targets.length;

        sb.append(packed ? "packed" : "sparse");
        sb.append("-switch-payload // for switch @ ");
        sb.append(Hex.u2(baseAddress));

        for (int i = 0; i < sz; i++) {
            int absTarget = targets[i].getAddress();
            int relTarget = absTarget - baseAddress;
            sb.append("\n  ");
            sb.append(cases.get(i));
            sb.append(": ");
            sb.append(Hex.u4(absTarget));
            sb.append(" // ");
            sb.append(Hex.s4(relTarget));
        }

        return sb.toString();
    }

    /**
     * Gets the size of a packed table for the given cases, in 16-bit code
     * units.
     *
     * @param cases {@code non-null;} sorted list of cases
     * @return {@code >= -1;} the packed table size or {@code -1} if the
     * cases couldn't possibly be represented as a packed table
     */
    private static long packedCodeSize(IntList cases) {
        int sz = cases.size();
        long low = cases.get(0);
        long high = cases.get(sz - 1);
        long result = ((high - low + 1)) * 2 + 4;

        return (result <= 0x7fffffff) ? result : -1;
    }

    /**
     * Gets the size of a sparse table for the given cases, in 16-bit code
     * units.
     *
     * @param cases {@code non-null;} sorted list of cases
     * @return {@code > 0;} the sparse table size
     */
    private static long sparseCodeSize(IntList cases) {
        int sz = cases.size();

        return (sz * 4L) + 2;
    }

    /**
     * Determines whether the given list of cases warrant being packed.
     *
     * @param cases {@code non-null;} sorted list of cases
     * @return {@code true} iff the table encoding the cases
     * should be packed
     */
    private static boolean shouldPack(IntList cases) {
        int sz = cases.size();

        if (sz < 2) {
            return true;
        }

        long packedSize = packedCodeSize(cases);
        long sparseSize = sparseCodeSize(cases);

        /*
         * We pick the packed representation if it is possible and
         * would be as small or smaller than 5/4 of the sparse
         * representation. That is, we accept some size overhead on
         * the packed representation, since that format is faster to
         * execute at runtime.
         */
        return (packedSize >= 0) && (packedSize <= ((sparseSize * 5) / 4));
    }
}

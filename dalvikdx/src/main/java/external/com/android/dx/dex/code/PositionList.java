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

import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.util.FixedSizeList;

/**
 * List of source position entries. This class includes a utility
 * method to extract an instance out of a {@link DalvInsnList}.
 */
public final class PositionList extends FixedSizeList {
    /** {@code non-null;} empty instance */
    public static final PositionList EMPTY = new PositionList(0);

    /**
     * constant for {@link #make} to indicate that no actual position
     * information should be returned
     */
    public static final int NONE = 1;

    /**
     * constant for {@link #make} to indicate that only line number
     * transitions should be returned
     */
    public static final int LINES = 2;

    /**
     * constant for {@link #make} to indicate that only "important" position
     * information should be returned. This includes block starts and
     * instructions that might throw.
     */
    public static final int IMPORTANT = 3;

    /**
     * Extracts and returns the source position information out of an
     * instruction list.
     *
     * @param insns {@code non-null;} instructions to convert
     * @param howMuch how much information should be included; one of the
     * static constants defined by this class
     * @return {@code non-null;} the positions list
     */
    public static PositionList make(DalvInsnList insns, int howMuch) {
        switch (howMuch) {
            case NONE: {
                return EMPTY;
            }
            case LINES:
            case IMPORTANT: {
                // Valid.
                break;
            }
            default: {
                throw new IllegalArgumentException("bogus howMuch");
            }
        }

        SourcePosition noInfo = SourcePosition.NO_INFO;
        SourcePosition cur = noInfo;
        int sz = insns.size();
        PositionList.Entry[] arr = new PositionList.Entry[sz];
        boolean lastWasTarget = false;
        int at = 0;

        for (int i = 0; i < sz; i++) {
            DalvInsn insn = insns.get(i);

            if (insn instanceof CodeAddress) {
                lastWasTarget = true;;
                continue;
            }

            SourcePosition pos = insn.getPosition();

            if (pos.equals(noInfo) || pos.sameLine(cur)) {
                continue;
            }

            if ((howMuch == IMPORTANT) && !lastWasTarget) {
                continue;
            }

            cur = pos;
            arr[at] = new PositionList.Entry(insn.getAddress(), pos);
            at++;

            lastWasTarget = false;
        }

        PositionList result = new PositionList(at);
        for (int i = 0; i < at; i++) {
            result.set(i, arr[i]);
        }

        result.setImmutable();
        return result;
    }

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size {@code >= 0;} the size of the list
     */
    public PositionList(int size) {
        super(size);
    }

    /**
     * Gets the element at the given index. It is an error to call
     * this with the index for an element which was never set; if you
     * do that, this will throw {@code NullPointerException}.
     *
     * @param n {@code >= 0, < size();} which index
     * @return {@code non-null;} element at that index
     */
    public Entry get(int n) {
        return (Entry) get0(n);
    }

    /**
     * Sets the entry at the given index.
     *
     * @param n {@code >= 0, < size();} which index
     * @param entry {@code non-null;} the entry to set at {@code n}
     */
    public void set(int n, Entry entry) {
        set0(n, entry);
    }

    /**
     * Entry in a position list.
     */
    public static class Entry {
        /** {@code >= 0;} address of this entry */
        private final int address;

        /** {@code non-null;} corresponding source position information */
        private final SourcePosition position;

        /**
         * Constructs an instance.
         *
         * @param address {@code >= 0;} address of this entry
         * @param position {@code non-null;} corresponding source position information
         */
        public Entry (int address, SourcePosition position) {
            if (address < 0) {
                throw new IllegalArgumentException("address < 0");
            }

            if (position == null) {
                throw new NullPointerException("position == null");
            }

            this.address = address;
            this.position = position;
        }

        /**
         * Gets the address.
         *
         * @return {@code >= 0;} the address
         */
        public int getAddress() {
            return address;
        }

        /**
         * Gets the source position information.
         *
         * @return {@code non-null;} the position information
         */
        public SourcePosition getPosition() {
            return position;
        }
    }
}

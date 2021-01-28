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

package external.com.android.dx.rop.code;

import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.FixedSizeList;
import java.util.BitSet;

/**
 * List of {@link RegisterSpec} instances.
 */
public final class RegisterSpecList
        extends FixedSizeList implements TypeList {
    /** {@code non-null;} no-element instance */
    public static final RegisterSpecList EMPTY = new RegisterSpecList(0);

    /**
     * Makes a single-element instance.
     *
     * @param spec {@code non-null;} the element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static RegisterSpecList make(RegisterSpec spec) {
        RegisterSpecList result = new RegisterSpecList(1);
        result.set(0, spec);
        return result;
    }

    /**
     * Makes a two-element instance.
     *
     * @param spec0 {@code non-null;} the first element
     * @param spec1 {@code non-null;} the second element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static RegisterSpecList make(RegisterSpec spec0,
                                        RegisterSpec spec1) {
        RegisterSpecList result = new RegisterSpecList(2);
        result.set(0, spec0);
        result.set(1, spec1);
        return result;
    }

    /**
     * Makes a three-element instance.
     *
     * @param spec0 {@code non-null;} the first element
     * @param spec1 {@code non-null;} the second element
     * @param spec2 {@code non-null;} the third element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static RegisterSpecList make(RegisterSpec spec0, RegisterSpec spec1,
                                        RegisterSpec spec2) {
        RegisterSpecList result = new RegisterSpecList(3);
        result.set(0, spec0);
        result.set(1, spec1);
        result.set(2, spec2);
        return result;
    }

    /**
     * Makes a four-element instance.
     *
     * @param spec0 {@code non-null;} the first element
     * @param spec1 {@code non-null;} the second element
     * @param spec2 {@code non-null;} the third element
     * @param spec3 {@code non-null;} the fourth element
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public static RegisterSpecList make(RegisterSpec spec0, RegisterSpec spec1,
                                        RegisterSpec spec2,
                                        RegisterSpec spec3) {
        RegisterSpecList result = new RegisterSpecList(4);
        result.set(0, spec0);
        result.set(1, spec1);
        result.set(2, spec2);
        result.set(3, spec3);
        return result;
    }

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size the size of the list
     */
    public RegisterSpecList(int size) {
        super(size);
    }

    /** {@inheritDoc} */
    @Override
    public Type getType(int n) {
        return get(n).getType().getType();
    }

    /** {@inheritDoc} */
    @Override
    public int getWordCount() {
        int sz = size();
        int result = 0;

        for (int i = 0; i < sz; i++) {
            result += getType(i).getCategory();
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public TypeList withAddedType(Type type) {
        throw new UnsupportedOperationException("unsupported");
    }

    /**
     * Gets the indicated element. It is an error to call this with the
     * index for an element which was never set; if you do that, this
     * will throw {@code NullPointerException}.
     *
     * @param n {@code >= 0, < size();} which element
     * @return {@code non-null;} the indicated element
     */
    public RegisterSpec get(int n) {
        return (RegisterSpec) get0(n);
    }

    /**
     * Returns a RegisterSpec in this list that uses the specified register,
     * or null if there is none in this list.
     * @param reg Register to find
     * @return RegisterSpec that uses argument or null.
     */
    public RegisterSpec specForRegister(int reg) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            RegisterSpec rs;

            rs = get(i);

            if (rs.getReg() == reg) {
                return rs;
            }
        }

        return null;
    }

    /**
     * Returns the index of a RegisterSpec in this list that uses the specified
     * register, or -1 if none in this list uses the register.
     * @param reg Register to find
     * @return index of RegisterSpec or -1
     */
    public int indexOfRegister(int reg) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            RegisterSpec rs;

            rs = get(i);

            if (rs.getReg() == reg) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Sets the element at the given index.
     *
     * @param n {@code >= 0, < size();} which element
     * @param spec {@code non-null;} the value to store
     */
    public void set(int n, RegisterSpec spec) {
        set0(n, spec);
    }

    /**
     * Gets the minimum required register count implied by this
     * instance. This is equal to the highest register number referred
     * to plus the widest width (largest category) of the type used in
     * that register.
     *
     * @return {@code >= 0;} the required registers size
     */
    public int getRegistersSize() {
        int sz = size();
        int result = 0;

        for (int i = 0; i < sz; i++) {
            RegisterSpec spec = (RegisterSpec) get0(i);
            if (spec != null) {
                int min = spec.getNextReg();
                if (min > result) {
                    result = min;
                }
            }
        }

        return result;
    }

    /**
     * Returns a new instance, which is the same as this instance,
     * except that it has an additional element prepended to the original.
     * Mutability of the result is inherited from the original.
     *
     * @param spec {@code non-null;} the new first spec (to prepend)
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpecList withFirst(RegisterSpec spec) {
        int sz = size();
        RegisterSpecList result = new RegisterSpecList(sz + 1);

        for (int i = 0; i < sz; i++) {
            result.set0(i + 1, get0(i));
        }

        result.set0(0, spec);
        if (isImmutable()) {
            result.setImmutable();
        }

        return result;
    }

    /**
     * Returns a new instance, which is the same as this instance,
     * except that its first element is removed. Mutability of the
     * result is inherited from the original.
     *
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpecList withoutFirst() {
        int newSize = size() - 1;

        if (newSize == 0) {
            return EMPTY;
        }

        RegisterSpecList result = new RegisterSpecList(newSize);

        for (int i = 0; i < newSize; i++) {
            result.set0(i, get0(i + 1));
        }

        if (isImmutable()) {
            result.setImmutable();
        }

        return result;
    }

    /**
     * Returns a new instance, which is the same as this instance,
     * except that its last element is removed. Mutability of the
     * result is inherited from the original.
     *
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpecList withoutLast() {
        int newSize = size() - 1;

        if (newSize == 0) {
            return EMPTY;
        }

        RegisterSpecList result = new RegisterSpecList(newSize);

        for (int i = 0; i < newSize; i++) {
            result.set0(i, get0(i));
        }

        if (isImmutable()) {
            result.setImmutable();
        }

        return result;
    }

    /**
     * Returns a new instance, which contains a subset of the elements
     * specified by the given BitSet. Indexes in the BitSet with a zero
     * are included, while indexes with a one are excluded. Mutability
     * of the result is inherited from the original.
     *
     * @param exclusionSet {@code non-null;} set of registers to exclude
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpecList subset(BitSet exclusionSet) {
        int newSize = size() - exclusionSet.cardinality();

        if (newSize == 0) {
            return EMPTY;
        }

        RegisterSpecList result = new RegisterSpecList(newSize);

        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < size(); oldIndex++) {
            if (!exclusionSet.get(oldIndex)) {
                result.set0(newIndex, get0(oldIndex));
                newIndex++;
            }
        }

        if (isImmutable()) {
            result.setImmutable();
        }

        return result;
    }

    /**
     * Returns an instance that is identical to this one, except that
     * all register numbers are offset by the given amount. Mutability
     * of the result is inherited from the original.
     *
     * @param delta the amount to offset the register numbers by
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpecList withOffset(int delta) {
        int sz = size();

        if (sz == 0) {
            // Don't bother making a new zero-element instance.
            return this;
        }

        RegisterSpecList result = new RegisterSpecList(sz);

        for (int i = 0; i < sz; i++) {
            RegisterSpec one = (RegisterSpec) get0(i);
            if (one != null) {
                result.set0(i, one.withOffset(delta));
            }
        }

        if (isImmutable()) {
            result.setImmutable();
        }

        return result;
    }

    /**
     * Returns an instance that is identical to this one, except that
     * all incompatible register numbers are renumbered sequentially from
     * the given base, with the first number duplicated if indicated. If
     * a null BitSet is given, it indicates all registers are incompatible.
     *
     * @param base the base register number
     * @param duplicateFirst whether to duplicate the first number
     * @param compatRegs {@code null-ok;} either a {@code non-null} set of
     * compatible registers, or {@code null} to indicate all registers are
     * incompatible
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpecList withExpandedRegisters(int base,
                                                  boolean duplicateFirst,
                                                  BitSet compatRegs) {
        int sz = size();

        if (sz == 0) {
            // Don't bother making a new zero-element instance.
            return this;
        }

        Expander expander = new Expander(this, compatRegs, base, duplicateFirst);

        for (int regIdx = 0; regIdx < sz; regIdx++) {
          expander.expandRegister(regIdx);
        }

        return expander.getResult();
    }

    private static class Expander {
      private final BitSet compatRegs;
      private final RegisterSpecList regSpecList;
      private int base;
      private final RegisterSpecList result;
      private boolean duplicateFirst;

      private Expander(RegisterSpecList regSpecList, BitSet compatRegs, int base,
          boolean duplicateFirst) {
        this.regSpecList = regSpecList;
        this.compatRegs = compatRegs;
        this.base = base;
        this.result = new RegisterSpecList(regSpecList.size());
        this.duplicateFirst = duplicateFirst;
      }

      private void expandRegister(int regIdx) {
        expandRegister(regIdx, (RegisterSpec) regSpecList.get0(regIdx));
      }

      private void expandRegister(int regIdx, RegisterSpec registerToExpand) {
        boolean replace = (compatRegs == null) ? true : !compatRegs.get(regIdx);
        RegisterSpec expandedReg;

        if (replace) {
          expandedReg = registerToExpand.withReg(base);
          if (!duplicateFirst) {
            base += expandedReg.getCategory();
          }
        } else {
          expandedReg = registerToExpand;
        }

        // Reset duplicateFirst when the first register has been dealt with.
        duplicateFirst = false;

        result.set0(regIdx, expandedReg);
      }

      private RegisterSpecList getResult() {
        if (regSpecList.isImmutable()) {
          result.setImmutable();
        }

        return result;
      }
    }
}

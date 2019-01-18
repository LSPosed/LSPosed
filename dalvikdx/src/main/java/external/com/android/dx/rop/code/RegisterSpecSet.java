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

import external.com.android.dx.util.MutabilityControl;

/**
 * Set of {@link RegisterSpec} instances, where a given register number
 * may appear only once in the set.
 */
public final class RegisterSpecSet
        extends MutabilityControl {
    /** {@code non-null;} no-element instance */
    public static final RegisterSpecSet EMPTY = new RegisterSpecSet(0);

    /**
     * {@code non-null;} array of register specs, where each element is
     * {@code null} or is an instance whose {@code reg}
     * matches the array index
     */
    private final RegisterSpec[] specs;

    /** {@code >= -1;} size of the set or {@code -1} if not yet calculated */
    private int size;

    /**
     * Constructs an instance. The instance is initially empty.
     *
     * @param maxSize {@code >= 0;} the maximum register number (exclusive) that
     * may be represented in this instance
     */
    public RegisterSpecSet(int maxSize) {
        super(maxSize != 0);

        this.specs = new RegisterSpec[maxSize];
        this.size = 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RegisterSpecSet)) {
            return false;
        }

        RegisterSpecSet otherSet = (RegisterSpecSet) other;
        RegisterSpec[] otherSpecs = otherSet.specs;
        int len = specs.length;

        if ((len != otherSpecs.length) || (size() != otherSet.size())) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            RegisterSpec s1 = specs[i];
            RegisterSpec s2 = otherSpecs[i];

            if (s1 == s2) {
                continue;
            }

            if ((s1 == null) || !s1.equals(s2)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int len = specs.length;
        int hash = 0;

        for (int i = 0; i < len; i++) {
            RegisterSpec spec = specs[i];
            int oneHash = (spec == null) ? 0 : spec.hashCode();
            hash = (hash * 31) + oneHash;
        }

        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        int len = specs.length;
        StringBuilder sb = new StringBuilder(len * 25);

        sb.append('{');

        boolean any = false;
        for (int i = 0; i < len; i++) {
            RegisterSpec spec = specs[i];
            if (spec != null) {
                if (any) {
                    sb.append(", ");
                } else {
                    any = true;
                }
                sb.append(spec);
            }
        }

        sb.append('}');
        return sb.toString();
    }

    /**
     * Gets the maximum number of registers that may be in this instance, which
     * is also the maximum-plus-one of register numbers that may be
     * represented.
     *
     * @return {@code >= 0;} the maximum size
     */
    public int getMaxSize() {
        return specs.length;
    }

    /**
     * Gets the current size of this instance.
     *
     * @return {@code >= 0;} the size
     */
    public int size() {
        int result = size;

        if (result < 0) {
            int len = specs.length;

            result = 0;
            for (int i = 0; i < len; i++) {
                if (specs[i] != null) {
                    result++;
                }
            }

            size = result;
        }

        return result;
    }

    /**
     * Gets the element with the given register number, if any.
     *
     * @param reg {@code >= 0;} the desired register number
     * @return {@code null-ok;} the element with the given register number or
     * {@code null} if there is none
     */
    public RegisterSpec get(int reg) {
        try {
            return specs[reg];
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("bogus reg");
        }
    }

    /**
     * Gets the element with the same register number as the given
     * spec, if any. This is just a convenient shorthand for
     * {@code get(spec.getReg())}.
     *
     * @param spec {@code non-null;} spec with the desired register number
     * @return {@code null-ok;} the element with the matching register number or
     * {@code null} if there is none
     */
    public RegisterSpec get(RegisterSpec spec) {
        return get(spec.getReg());
    }

    /**
     * Returns the spec in this set that's currently associated with a
     * given local (type, name, and signature), or {@code null} if there is
     * none. This ignores the register number of the given spec but
     * matches on everything else.
     *
     * @param spec {@code non-null;} local to look for
     * @return {@code null-ok;} first register found that matches, if any
     */
    public RegisterSpec findMatchingLocal(RegisterSpec spec) {
        int length = specs.length;

        for (int reg = 0; reg < length; reg++) {
            RegisterSpec s = specs[reg];

            if (s == null) {
                continue;
            }

            if (spec.matchesVariable(s)) {
                return s;
            }
        }

        return null;
    }

    /**
     * Returns the spec in this set that's currently associated with a given
     * local (name and signature), or {@code null} if there is none.
     *
     * @param local {@code non-null;} local item to search for
     * @return {@code null-ok;} first register found with matching name and signature
     */
    public RegisterSpec localItemToSpec(LocalItem local) {
        int length = specs.length;

        for (int reg = 0; reg < length; reg++) {
            RegisterSpec spec = specs[reg];

            if ((spec != null) && local.equals(spec.getLocalItem())) {
                return spec;
            }
        }

        return null;
    }

    /**
     * Removes a spec from the set. Only the register number
     * of the parameter is significant.
     *
     * @param toRemove {@code non-null;} register to remove.
     */
    public void remove(RegisterSpec toRemove) {
        try {
            specs[toRemove.getReg()] = null;
            size = -1;
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("bogus reg");
        }
    }

    /**
     * Puts the given spec into the set. If there is already an element in
     * the set with the same register number, it is replaced. Additionally,
     * if the previous element is for a category-2 register, then that
     * previous element is nullified. Finally, if the given spec is for
     * a category-2 register, then the immediately subsequent element
     * is nullified.
     *
     * @param spec {@code non-null;} the register spec to put in the instance
     */
    public void put(RegisterSpec spec) {
        throwIfImmutable();

        if (spec == null) {
            throw new NullPointerException("spec == null");
        }

        size = -1;

        try {
            int reg = spec.getReg();
            specs[reg] = spec;

            if (reg > 0) {
                int prevReg = reg - 1;
                RegisterSpec prevSpec = specs[prevReg];
                if ((prevSpec != null) && (prevSpec.getCategory() == 2)) {
                    specs[prevReg] = null;
                }
            }

            if (spec.getCategory() == 2) {
                specs[reg + 1] = null;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("spec.getReg() out of range");
        }
    }

    /**
     * Put the entire contents of the given set into this one.
     *
     * @param set {@code non-null;} the set to put into this instance
     */
    public void putAll(RegisterSpecSet set) {
        int max = set.getMaxSize();

        for (int i = 0; i < max; i++) {
            RegisterSpec spec = set.get(i);
            if (spec != null) {
                put(spec);
            }
        }
    }

    /**
     * Intersects this instance with the given one, modifying this
     * instance. The intersection consists of the pairwise
     * {@link RegisterSpec#intersect} of corresponding elements from
     * this instance and the given one where both are non-null.
     *
     * @param other {@code non-null;} set to intersect with
     * @param localPrimary whether local variables are primary to
     * the intersection; if {@code true}, then the only non-null
     * result elements occur when registers being intersected have
     * equal names (or both have {@code null} names)
     */
    public void intersect(RegisterSpecSet other, boolean localPrimary) {
        throwIfImmutable();

        RegisterSpec[] otherSpecs = other.specs;
        int thisLen = specs.length;
        int len = Math.min(thisLen, otherSpecs.length);

        size = -1;

        for (int i = 0; i < len; i++) {
            RegisterSpec spec = specs[i];

            if (spec == null) {
                continue;
            }

            RegisterSpec intersection =
                spec.intersect(otherSpecs[i], localPrimary);
            if (intersection != spec) {
                specs[i] = intersection;
            }
        }

        for (int i = len; i < thisLen; i++) {
            specs[i] = null;
        }
    }

    /**
     * Returns an instance that is identical to this one, except that
     * all register numbers are offset by the given amount. Mutability
     * of the result is inherited from the original.
     *
     * @param delta the amount to offset the register numbers by
     * @return {@code non-null;} an appropriately-constructed instance
     */
    public RegisterSpecSet withOffset(int delta) {
        int len = specs.length;
        RegisterSpecSet result = new RegisterSpecSet(len + delta);

        for (int i = 0; i < len; i++) {
            RegisterSpec spec = specs[i];
            if (spec != null) {
                result.put(spec.withOffset(delta));
            }
        }

        result.size = size;

        if (isImmutable()) {
            result.setImmutable();
        }

        return result;
    }

    /**
     * Makes and return a mutable copy of this instance.
     *
     * @return {@code non-null;} the mutable copy
     */
    public RegisterSpecSet mutableCopy() {
        int len = specs.length;
        RegisterSpecSet copy = new RegisterSpecSet(len);

        for (int i = 0; i < len; i++) {
            RegisterSpec spec = specs[i];
            if (spec != null) {
                copy.put(spec);
            }
        }

        copy.size = size;

        return copy;
    }
}

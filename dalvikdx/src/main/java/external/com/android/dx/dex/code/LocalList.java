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

import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecSet;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.FixedSizeList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * List of local variables. Each local variable entry indicates a
 * range of code which it is valid for, a register number, a name,
 * and a type.
 */
public final class LocalList extends FixedSizeList {
    /** {@code non-null;} empty instance */
    public static final LocalList EMPTY = new LocalList(0);

    /** whether to run the self-check code */
    private static final boolean DEBUG = false;

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size {@code >= 0;} the size of the list
     */
    public LocalList(int size) {
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
     * Does a human-friendly dump of this instance.
     *
     * @param out {@code non-null;} where to dump
     * @param prefix {@code non-null;} prefix to attach to each line of output
     */
    public void debugPrint(PrintStream out, String prefix) {
        int sz = size();

        for (int i = 0; i < sz; i++) {
            out.print(prefix);
            out.println(get(i));
        }
    }

    /**
     * Disposition of a local entry.
     */
    public static enum Disposition {
        /** local started (introduced) */
        START,

        /** local ended without being replaced */
        END_SIMPLY,

        /** local ended because it was directly replaced */
        END_REPLACED,

        /** local ended because it was moved to a different register */
        END_MOVED,

        /**
         * local ended because the previous local clobbered this one
         * (because it is category-2)
         */
        END_CLOBBERED_BY_PREV,

        /**
         * local ended because the next local clobbered this one
         * (because this one is a category-2)
         */
        END_CLOBBERED_BY_NEXT;
    }

    /**
     * Entry in a local list.
     */
    public static class Entry implements Comparable<Entry> {
        /** {@code >= 0;} address */
        private final int address;

        /** {@code non-null;} disposition of the local */
        private final Disposition disposition;

        /** {@code non-null;} register spec representing the variable */
        private final RegisterSpec spec;

        /** {@code non-null;} variable type (derived from {@code spec}) */
        private final CstType type;

        /**
         * Constructs an instance.
         *
         * @param address {@code >= 0;} address
         * @param disposition {@code non-null;} disposition of the local
         * @param spec {@code non-null;} register spec representing
         * the variable
         */
        public Entry(int address, Disposition disposition, RegisterSpec spec) {
            if (address < 0) {
                throw new IllegalArgumentException("address < 0");
            }

            if (disposition == null) {
                throw new NullPointerException("disposition == null");
            }

            try {
                if (spec.getLocalItem() == null) {
                    throw new NullPointerException(
                            "spec.getLocalItem() == null");
                }
            } catch (NullPointerException ex) {
                // Elucidate the exception.
                throw new NullPointerException("spec == null");
            }

            this.address = address;
            this.disposition = disposition;
            this.spec = spec;
            this.type = CstType.intern(spec.getType());
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return Integer.toHexString(address) + " " + disposition + " " +
                spec;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Entry)) {
                return false;
            }

            return (compareTo((Entry) other) == 0);
        }

        /**
         * Compares by (in priority order) address, end then start
         * disposition (variants of end are all consistered
         * equivalent), and spec.
         *
         * @param other {@code non-null;} entry to compare to
         * @return {@code -1..1;} standard result of comparison
         */
        @Override
        public int compareTo(Entry other) {
            if (address < other.address) {
                return -1;
            } else if (address > other.address) {
                return 1;
            }

            boolean thisIsStart = isStart();
            boolean otherIsStart = other.isStart();

            if (thisIsStart != otherIsStart) {
                return thisIsStart ? 1 : -1;
            }

            return spec.compareTo(other.spec);
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
         * Gets the disposition.
         *
         * @return {@code non-null;} the disposition
         */
        public Disposition getDisposition() {
            return disposition;
        }

        /**
         * Gets whether this is a local start. This is just shorthand for
         * {@code getDisposition() == Disposition.START}.
         *
         * @return {@code true} iff this is a start
         */
        public boolean isStart() {
            return disposition == Disposition.START;
        }

        /**
         * Gets the variable name.
         *
         * @return {@code null-ok;} the variable name
         */
        public CstString getName() {
            return spec.getLocalItem().getName();
        }

        /**
         * Gets the variable signature.
         *
         * @return {@code null-ok;} the variable signature
         */
        public CstString getSignature() {
            return spec.getLocalItem().getSignature();
        }

        /**
         * Gets the variable's type.
         *
         * @return {@code non-null;} the type
         */
        public CstType getType() {
            return type;
        }

        /**
         * Gets the number of the register holding the variable.
         *
         * @return {@code >= 0;} the number of the register holding
         * the variable
         */
        public int getRegister() {
            return spec.getReg();
        }

        /**
         * Gets the RegisterSpec of the register holding the variable.
         *
         * @return {@code non-null;} RegisterSpec of the holding register.
         */
        public RegisterSpec getRegisterSpec() {
            return spec;
        }

        /**
         * Returns whether or not this instance matches the given spec.
         *
         * @param otherSpec {@code non-null;} the spec in question
         * @return {@code true} iff this instance matches
         * {@code spec}
         */
        public boolean matches(RegisterSpec otherSpec) {
            return spec.equalsUsingSimpleType(otherSpec);
        }

        /**
         * Returns whether or not this instance matches the spec in
         * the given instance.
         *
         * @param other {@code non-null;} another entry
         * @return {@code true} iff this instance's spec matches
         * {@code other}
         */
        public boolean matches(Entry other) {
            return matches(other.spec);
        }

        /**
         * Returns an instance just like this one but with the disposition
         * set as given.
         *
         * @param disposition {@code non-null;} the new disposition
         * @return {@code non-null;} an appropriately-constructed instance
         */
        public Entry withDisposition(Disposition disposition) {
            if (disposition == this.disposition) {
                return this;
            }

            return new Entry(address, disposition, spec);
        }
    }

    /**
     * Constructs an instance for the given method, based on the given
     * block order and intermediate local information.
     *
     * @param insns {@code non-null;} instructions to convert
     * @return {@code non-null;} the constructed list
     */
    public static LocalList make(DalvInsnList insns) {
        int sz = insns.size();

        /*
         * Go through the insn list, looking for all the local
         * variable pseudoinstructions, splitting out LocalSnapshots
         * into separate per-variable starts, adding explicit ends
         * wherever a variable is replaced or moved, and collecting
         * these and all the other local variable "activity"
         * together into an output list (without the other insns).
         *
         * Note: As of this writing, this method won't be handed any
         * insn lists that contain local ends, but I (danfuzz) expect
         * that to change at some point, when we start feeding that
         * info explicitly into the rop layer rather than only trying
         * to infer it. So, given that expectation, this code is
         * written to deal with them.
         */

        MakeState state = new MakeState(sz);

        for (int i = 0; i < sz; i++) {
            DalvInsn insn = insns.get(i);

            if (insn instanceof LocalSnapshot) {
                RegisterSpecSet snapshot =
                    ((LocalSnapshot) insn).getLocals();
                state.snapshot(insn.getAddress(), snapshot);
            } else if (insn instanceof LocalStart) {
                RegisterSpec local = ((LocalStart) insn).getLocal();
                state.startLocal(insn.getAddress(), local);
            }
        }

        LocalList result = state.finish();

        if (DEBUG) {
            debugVerify(result);
        }

        return result;
    }

    /**
     * Debugging helper that verifies the constraint that a list doesn't
     * contain any redundant local starts and that local ends that are
     * due to replacements are properly annotated.
     */
    private static void debugVerify(LocalList locals) {
        try {
            debugVerify0(locals);
        } catch (RuntimeException ex) {
            int sz = locals.size();
            for (int i = 0; i < sz; i++) {
                System.err.println(locals.get(i));
            }
            throw ex;
        }

    }

    /**
     * Helper for {@link #debugVerify} which does most of the work.
     */
    private static void debugVerify0(LocalList locals) {
        int sz = locals.size();
        Entry[] active = new Entry[65536];

        for (int i = 0; i < sz; i++) {
            Entry e = locals.get(i);
            int reg = e.getRegister();

            if (e.isStart()) {
                Entry already = active[reg];

                if ((already != null) && e.matches(already)) {
                    throw new RuntimeException("redundant start at " +
                            Integer.toHexString(e.getAddress()) + ": got " +
                            e + "; had " + already);
                }

                active[reg] = e;
            } else {
                if (active[reg] == null) {
                    throw new RuntimeException("redundant end at " +
                            Integer.toHexString(e.getAddress()));
                }

                int addr = e.getAddress();
                boolean foundStart = false;

                for (int j = i + 1; j < sz; j++) {
                    Entry test = locals.get(j);
                    if (test.getAddress() != addr) {
                        break;
                    }
                    if (test.getRegisterSpec().getReg() == reg) {
                        if (test.isStart()) {
                            if (e.getDisposition()
                                    != Disposition.END_REPLACED) {
                                throw new RuntimeException(
                                        "improperly marked end at " +
                                        Integer.toHexString(addr));
                            }
                            foundStart = true;
                        } else {
                            throw new RuntimeException(
                                    "redundant end at " +
                                    Integer.toHexString(addr));
                        }
                    }
                }

                if (!foundStart &&
                        (e.getDisposition() == Disposition.END_REPLACED)) {
                    throw new RuntimeException(
                            "improper end replacement claim at " +
                            Integer.toHexString(addr));
                }

                active[reg] = null;
            }
        }
    }

    /**
     * Intermediate state when constructing a local list.
     */
    public static class MakeState {
        /** {@code non-null;} result being collected */
        private final ArrayList<Entry> result;

        /**
         * {@code >= 0;} running count of nulled result entries, to help with
         * sizing the final list
         */
        private int nullResultCount;

        /** {@code null-ok;} current register mappings */
        private RegisterSpecSet regs;

        /** {@code null-ok;} result indices where local ends are stored */
        private int[] endIndices;

        /** {@code >= 0;} last address seen */
        private final int lastAddress;

        /**
         * Constructs an instance.
         */
        public MakeState(int initialSize) {
            result = new ArrayList<Entry>(initialSize);
            nullResultCount = 0;
            regs = null;
            endIndices = null;
            lastAddress = 0;
        }

        /**
         * Checks the address and other vitals as a prerequisite to
         * further processing.
         *
         * @param address {@code >= 0;} address about to be processed
         * @param reg {@code >= 0;} register number about to be processed
         */
        private void aboutToProcess(int address, int reg) {
            boolean first = (endIndices == null);

            if ((address == lastAddress) && !first) {
                return;
            }

            if (address < lastAddress) {
                throw new RuntimeException("shouldn't happen");
            }

            if (first || (reg >= endIndices.length)) {
                /*
                 * This is the first allocation of the state set and
                 * index array, or we need to grow. (The latter doesn't
                 * happen much; in fact, we have only ever observed
                 * it happening in test cases, never in "real" code.)
                 */
                int newSz = reg + 1;
                RegisterSpecSet newRegs = new RegisterSpecSet(newSz);
                int[] newEnds = new int[newSz];
                Arrays.fill(newEnds, -1);

                if (!first) {
                    newRegs.putAll(regs);
                    System.arraycopy(endIndices, 0, newEnds, 0,
                            endIndices.length);
                }

                regs = newRegs;
                endIndices = newEnds;
            }
        }

        /**
         * Sets the local state at the given address to the given snapshot.
         * The first call on this instance must be to this method, so that
         * the register state can be properly sized.
         *
         * @param address {@code >= 0;} the address
         * @param specs {@code non-null;} spec set representing the locals
         */
        public void snapshot(int address, RegisterSpecSet specs) {
            if (DEBUG) {
                System.err.printf("%04x snapshot %s\n", address, specs);
            }

            int sz = specs.getMaxSize();
            aboutToProcess(address, sz - 1);

            for (int i = 0; i < sz; i++) {
                RegisterSpec oldSpec = regs.get(i);
                RegisterSpec newSpec = filterSpec(specs.get(i));

                if (oldSpec == null) {
                    if (newSpec != null) {
                        startLocal(address, newSpec);
                    }
                } else if (newSpec == null) {
                    endLocal(address, oldSpec);
                } else if (! newSpec.equalsUsingSimpleType(oldSpec)) {
                    endLocal(address, oldSpec);
                    startLocal(address, newSpec);
                }
            }

            if (DEBUG) {
                System.err.printf("%04x snapshot done\n", address);
            }
        }

        /**
         * Starts a local at the given address.
         *
         * @param address {@code >= 0;} the address
         * @param startedLocal {@code non-null;} spec representing the
         * started local
         */
        public void startLocal(int address, RegisterSpec startedLocal) {
            if (DEBUG) {
                System.err.printf("%04x start %s\n", address, startedLocal);
            }

            int regNum = startedLocal.getReg();

            startedLocal = filterSpec(startedLocal);
            aboutToProcess(address, regNum);

            RegisterSpec existingLocal = regs.get(regNum);

            if (startedLocal.equalsUsingSimpleType(existingLocal)) {
                // Silently ignore a redundant start.
                return;
            }

            RegisterSpec movedLocal = regs.findMatchingLocal(startedLocal);
            if (movedLocal != null) {
                /*
                 * The same variable was moved from one register to another.
                 * So add an end for its old location.
                 */
                addOrUpdateEnd(address, Disposition.END_MOVED, movedLocal);
            }

            int endAt = endIndices[regNum];

            if (existingLocal != null) {
                /*
                 * There is an existing (but non-matching) local.
                 * Add an explicit end for it.
                 */
                add(address, Disposition.END_REPLACED, existingLocal);
            } else if (endAt >= 0) {
                /*
                 * Look for an end local for the same register at the
                 * same address. If found, then update it or delete
                 * it, depending on whether or not it represents the
                 * same variable as the one being started.
                 */
                Entry endEntry = result.get(endAt);
                if (endEntry.getAddress() == address) {
                    if (endEntry.matches(startedLocal)) {
                        /*
                         * There was already an end local for the same
                         * variable at the same address. This turns
                         * out to be superfluous, as we are starting
                         * up the exact same local. This situation can
                         * happen when a single local variable got
                         * somehow "split up" during intermediate
                         * processing. In any case, rather than represent
                         * the end-then-start, just remove the old end.
                         */
                        result.set(endAt, null);
                        nullResultCount++;
                        regs.put(startedLocal);
                        endIndices[regNum] = -1;
                        return;
                    } else {
                        /*
                         * There was a different variable ended at the
                         * same address. Update it to indicate that
                         * it was ended due to a replacement (rather than
                         * ending for no particular reason).
                         */
                        endEntry = endEntry.withDisposition(
                                Disposition.END_REPLACED);
                        result.set(endAt, endEntry);
                    }
                }
            }

            /*
             * The code above didn't find and remove an unnecessary
             * local end, so we now have to add one or more entries to
             * the output to capture the transition.
             */

            /*
             * If the local just below (in the register set at reg-1)
             * is of category-2, then it is ended by this new start.
             */
            if (regNum > 0) {
                RegisterSpec justBelow = regs.get(regNum - 1);
                if ((justBelow != null) && justBelow.isCategory2()) {
                    addOrUpdateEnd(address,
                            Disposition.END_CLOBBERED_BY_NEXT,
                            justBelow);
                }
            }

            /*
             * Similarly, if this local is category-2, then the local
             * just above (if any) is ended by the start now being
             * emitted.
             */
            if (startedLocal.isCategory2()) {
                RegisterSpec justAbove = regs.get(regNum + 1);
                if (justAbove != null) {
                    addOrUpdateEnd(address,
                            Disposition.END_CLOBBERED_BY_PREV,
                            justAbove);
                }
            }

            /*
             * TODO: Add an end for the same local in a different reg,
             * if any (that is, if the local migrates from vX to vY,
             * we should note that as a local end in vX).
             */

            add(address, Disposition.START, startedLocal);
        }

        /**
         * Ends a local at the given address, using the disposition
         * {@code END_SIMPLY}.
         *
         * @param address {@code >= 0;} the address
         * @param endedLocal {@code non-null;} spec representing the
         * local being ended
         */
        public void endLocal(int address, RegisterSpec endedLocal) {
            endLocal(address, endedLocal, Disposition.END_SIMPLY);
        }

        /**
         * Ends a local at the given address.
         *
         * @param address {@code >= 0;} the address
         * @param endedLocal {@code non-null;} spec representing the
         * local being ended
         * @param disposition reason for the end
         */
        public void endLocal(int address, RegisterSpec endedLocal,
                Disposition disposition) {
            if (DEBUG) {
                System.err.printf("%04x end %s\n", address, endedLocal);
            }

            int regNum = endedLocal.getReg();

            endedLocal = filterSpec(endedLocal);
            aboutToProcess(address, regNum);

            int endAt = endIndices[regNum];

            if (endAt >= 0) {
                /*
                 * The local in the given register is already ended.
                 * Silently return without adding anything to the result.
                 */
                return;
            }

            // Check for start and end at the same address.
            if (checkForEmptyRange(address, endedLocal)) {
                return;
            }

            add(address, disposition, endedLocal);
        }

        /**
         * Helper for {@link #endLocal}, which handles the cases where
         * and end local is issued at the same address as a start local
         * for the same register. If this case is found, then this
         * method will remove the start (as the local was never actually
         * active), update the {@link #endIndices} to be accurate, and
         * if needed update the newly-active end to reflect an altered
         * disposition.
         *
         * @param address {@code >= 0;} the address
         * @param endedLocal {@code non-null;} spec representing the
         * local being ended
         * @return {@code true} iff this method found the case in question
         * and adjusted things accordingly
         */
        private boolean checkForEmptyRange(int address,
                RegisterSpec endedLocal) {
            int at = result.size() - 1;
            Entry entry;

            // Look for a previous entry at the same address.
            for (/*at*/; at >= 0; at--) {
                entry = result.get(at);

                if (entry == null) {
                    continue;
                }

                if (entry.getAddress() != address) {
                    // We didn't find any match at the same address.
                    return false;
                }

                if (entry.matches(endedLocal)) {
                    break;
                }
            }

            /*
             * In fact, we found that the endedLocal had started at the
             * same address, so do all the requisite cleanup.
             */

            regs.remove(endedLocal);
            result.set(at, null);
            nullResultCount++;

            int regNum = endedLocal.getReg();
            boolean found = false;
            entry = null;

            // Now look back further to update where the register ended.
            for (at--; at >= 0; at--) {
                entry = result.get(at);

                if (entry == null) {
                    continue;
                }

                if (entry.getRegisterSpec().getReg() == regNum) {
                    found = true;
                    break;
                }
            }

            if (found) {
                // We found an end for the same register.
                endIndices[regNum] = at;

                if (entry.getAddress() == address) {
                    /*
                     * It's still the same address, so update the
                     * disposition.
                     */
                    result.set(at,
                            entry.withDisposition(Disposition.END_SIMPLY));
                }
            }

            return true;
        }

        /**
         * Converts a given spec into the form acceptable for use in a
         * local list. This, in particular, transforms the "known
         * null" type into simply {@code Object}. This method needs to
         * be called for any spec that is on its way into a locals
         * list.
         *
         * <p>This isn't necessarily the cleanest way to achieve the
         * goal of not representing known nulls in a locals list, but
         * it gets the job done.</p>
         *
         * @param orig {@code null-ok;} the original spec
         * @return {@code null-ok;} an appropriately modified spec, or the
         * original if nothing needs to be done
         */
        private static RegisterSpec filterSpec(RegisterSpec orig) {
            if ((orig != null) && (orig.getType() == Type.KNOWN_NULL)) {
                return orig.withType(Type.OBJECT);
            }

            return orig;
        }

        /**
         * Adds an entry to the result, updating the adjunct tables
         * accordingly.
         *
         * @param address {@code >= 0;} the address
         * @param disposition {@code non-null;} the disposition
         * @param spec {@code non-null;} spec representing the local
         */
        private void add(int address, Disposition disposition,
                RegisterSpec spec) {
            int regNum = spec.getReg();

            result.add(new Entry(address, disposition, spec));

            if (disposition == Disposition.START) {
                regs.put(spec);
                endIndices[regNum] = -1;
            } else {
                regs.remove(spec);
                endIndices[regNum] = result.size() - 1;
            }
        }

        /**
         * Adds or updates an end local (changing its disposition). If
         * this would cause an empty range for a local, this instead
         * removes the local entirely.
         *
         * @param address {@code >= 0;} the address
         * @param disposition {@code non-null;} the disposition
         * @param spec {@code non-null;} spec representing the local
         */
        private void addOrUpdateEnd(int address, Disposition disposition,
                RegisterSpec spec) {
            if (disposition == Disposition.START) {
                throw new RuntimeException("shouldn't happen");
            }

            int regNum = spec.getReg();
            int endAt = endIndices[regNum];

            if (endAt >= 0) {
                // There is a previous end.
                Entry endEntry = result.get(endAt);
                if ((endEntry.getAddress() == address) &&
                        endEntry.getRegisterSpec().equals(spec)) {
                    /*
                     * The end is for the right address and variable, so
                     * update it.
                     */
                    result.set(endAt, endEntry.withDisposition(disposition));
                    regs.remove(spec); // TODO: Is this line superfluous?
                    return;
                }
            }

            endLocal(address, spec, disposition);
        }

        /**
         * Finishes processing altogether and gets the result.
         *
         * @return {@code non-null;} the result list
         */
        public LocalList finish() {
            aboutToProcess(Integer.MAX_VALUE, 0);

            int resultSz = result.size();
            int finalSz = resultSz - nullResultCount;

            if (finalSz == 0) {
                return EMPTY;
            }

            /*
             * Collect an array of only the non-null entries, and then
             * sort it to get a consistent order for everything: Local
             * ends and starts for a given address could come in any
             * order, but we want ends before starts as well as
             * registers in order (within ends or starts).
             */

            Entry[] resultArr = new Entry[finalSz];

            if (resultSz == finalSz) {
                result.toArray(resultArr);
            } else {
                int at = 0;
                for (Entry e : result) {
                    if (e != null) {
                        resultArr[at++] = e;
                    }
                }
            }

            Arrays.sort(resultArr);

            LocalList resultList = new LocalList(finalSz);

            for (int i = 0; i < finalSz; i++) {
                resultList.set(i, resultArr[i]);
            }

            resultList.setImmutable();
            return resultList;
        }
    }
}

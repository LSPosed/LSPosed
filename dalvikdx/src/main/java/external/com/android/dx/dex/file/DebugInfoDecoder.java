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

package external.com.android.dx.dex.file;

import external.com.android.dex.Leb128;
import external.com.android.dex.util.ByteArrayByteInput;
import external.com.android.dex.util.ByteInput;
import external.com.android.dex.util.ExceptionWithContext;
import external.com.android.dx.dex.code.DalvCode;
import external.com.android.dx.dex.code.DalvInsnList;
import external.com.android.dx.dex.code.LocalList;
import external.com.android.dx.dex.code.PositionList;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_ADVANCE_LINE;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_ADVANCE_PC;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_END_LOCAL;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_END_SEQUENCE;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_FIRST_SPECIAL;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_LINE_BASE;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_LINE_RANGE;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_RESTART_LOCAL;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_SET_EPILOGUE_BEGIN;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_SET_FILE;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_SET_PROLOGUE_END;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_START_LOCAL;
import static external.com.android.dx.dex.file.DebugInfoConstants.DBG_START_LOCAL_EXTENDED;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A decoder for the dex debug info state machine format.
 * This code exists mostly as a reference implementation and test for
 * for the {@code DebugInfoEncoder}
 */
public class DebugInfoDecoder {
    /** encoded debug info */
    private final byte[] encoded;

    /** positions decoded */
    private final ArrayList<PositionEntry> positions;

    /** locals decoded */
    private final ArrayList<LocalEntry> locals;

    /** size of code block in code units */
    private final int codesize;

    /** indexed by register, the last local variable live in a reg */
    private final LocalEntry[] lastEntryForReg;

    /** method descriptor of method this debug info is for */
    private final Prototype desc;

    /** true if method is static */
    private final boolean isStatic;

    /** dex file this debug info will be stored in */
    private final DexFile file;

    /**
     * register size, in register units, of the register space
     * used by this method
     */
    private final int regSize;

    /** current decoding state: line number */
    private int line = 1;

    /** current decoding state: bytecode address */
    private int address = 0;

    /** string index of the string "this" */
    private final int thisStringIdx;

    /**
     * Constructs an instance.
     *
     * @param encoded encoded debug info
     * @param codesize size of code block in code units
     * @param regSize register size, in register units, of the register space
     * used by this method
     * @param isStatic true if method is static
     * @param ref method descriptor of method this debug info is for
     * @param file dex file this debug info will be stored in
     */
    DebugInfoDecoder(byte[] encoded, int codesize, int regSize,
            boolean isStatic, CstMethodRef ref, DexFile file) {
        if (encoded == null) {
            throw new NullPointerException("encoded == null");
        }

        this.encoded = encoded;
        this.isStatic = isStatic;
        this.desc = ref.getPrototype();
        this.file = file;
        this.regSize = regSize;

        positions = new ArrayList<PositionEntry>();
        locals = new ArrayList<LocalEntry>();
        this.codesize = codesize;
        lastEntryForReg = new LocalEntry[regSize];

        int idx = -1;

        try {
            idx = file.getStringIds().indexOf(new CstString("this"));
        } catch (IllegalArgumentException ex) {
            /*
             * Silently tolerate not finding "this". It just means that
             * no method has local variable info that looks like
             * a standard instance method.
             */
        }

        thisStringIdx = idx;
    }

    /**
     * An entry in the resulting postions table
     */
    static private class PositionEntry {
        /** bytecode address */
        public int address;

        /** line number */
        public int line;

        public PositionEntry(int address, int line) {
            this.address = address;
            this.line = line;
        }
    }

    /**
     * An entry in the resulting locals table
     */
    static private class LocalEntry {
        /** address of event */
        public int address;

        /** {@code true} iff it's a local start */
        public boolean isStart;

        /** register number */
        public int reg;

        /** index of name in strings table */
        public int nameIndex;

        /** index of type in types table */
        public int typeIndex;

        /** index of type signature in strings table */
        public int signatureIndex;

        public LocalEntry(int address, boolean isStart, int reg, int nameIndex,
                int typeIndex, int signatureIndex) {
            this.address        = address;
            this.isStart        = isStart;
            this.reg            = reg;
            this.nameIndex      = nameIndex;
            this.typeIndex      = typeIndex;
            this.signatureIndex = signatureIndex;
        }

        @Override
        public String toString() {
            return String.format("[%x %s v%d %04x %04x %04x]",
                    address, isStart ? "start" : "end", reg,
                    nameIndex, typeIndex, signatureIndex);
        }
    }

    /**
     * Gets the decoded positions list.
     * Valid after calling {@code decode}.
     *
     * @return positions list in ascending address order.
     */
    public List<PositionEntry> getPositionList() {
        return positions;
    }

    /**
     * Gets the decoded locals list, in ascending start-address order.
     * Valid after calling {@code decode}.
     *
     * @return locals list in ascending address order.
     */
    public List<LocalEntry> getLocals() {
        return locals;
    }

    /**
     * Decodes the debug info sequence.
     */
    public void decode() {
        try {
            decode0();
        } catch (Exception ex) {
            throw ExceptionWithContext.withContext(ex,
                    "...while decoding debug info");
        }
    }

    /**
     * Reads a string index. String indicies are offset by 1, and a 0 value
     * in the stream (-1 as returned by this method) means "null"
     *
     * @return index into file's string ids table, -1 means null
     * @throws IOException
     */
    private int readStringIndex(ByteInput bs) throws IOException {
        int offsetIndex = Leb128.readUnsignedLeb128(bs);

        return offsetIndex - 1;
    }

    /**
     * Gets the register that begins the method's parameter range (including
     * the 'this' parameter for non-static methods). The range continues until
     * {@code regSize}
     *
     * @return register as noted above.
     */
    private int getParamBase() {
        return regSize
                - desc.getParameterTypes().getWordCount() - (isStatic? 0 : 1);
    }

    private void decode0() throws IOException {
        ByteInput bs = new ByteArrayByteInput(encoded);

        line = Leb128.readUnsignedLeb128(bs);
        int szParams = Leb128.readUnsignedLeb128(bs);
        StdTypeList params = desc.getParameterTypes();
        int curReg = getParamBase();

        if (szParams != params.size()) {
            throw new RuntimeException(
                    "Mismatch between parameters_size and prototype");
        }

        if (!isStatic) {
            // Start off with implicit 'this' entry
            LocalEntry thisEntry =
                new LocalEntry(0, true, curReg, thisStringIdx, 0, 0);
            locals.add(thisEntry);
            lastEntryForReg[curReg] = thisEntry;
            curReg++;
        }

        for (int i = 0; i < szParams; i++) {
            Type paramType = params.getType(i);
            LocalEntry le;

            int nameIdx = readStringIndex(bs);

            if (nameIdx == -1) {
                /*
                 * Unnamed parameter; often but not always filled in by an
                 * extended start op after the prologue
                 */
                le = new LocalEntry(0, true, curReg, -1, 0, 0);
            } else {
                // TODO: Final 0 should be idx of paramType.getDescriptor().
                le = new LocalEntry(0, true, curReg, nameIdx, 0, 0);
            }

            locals.add(le);
            lastEntryForReg[curReg] = le;
            curReg += paramType.getCategory();
        }

        for (;;) {
            int opcode = bs.readByte() & 0xff;

            switch (opcode) {
                case DBG_START_LOCAL: {
                    int reg = Leb128.readUnsignedLeb128(bs);
                    int nameIdx = readStringIndex(bs);
                    int typeIdx = readStringIndex(bs);
                    LocalEntry le = new LocalEntry(
                            address, true, reg, nameIdx, typeIdx, 0);

                    locals.add(le);
                    lastEntryForReg[reg] = le;
                }
                break;

                case DBG_START_LOCAL_EXTENDED: {
                    int reg = Leb128.readUnsignedLeb128(bs);
                    int nameIdx = readStringIndex(bs);
                    int typeIdx = readStringIndex(bs);
                    int sigIdx = readStringIndex(bs);
                    LocalEntry le = new LocalEntry(
                            address, true, reg, nameIdx, typeIdx, sigIdx);

                    locals.add(le);
                    lastEntryForReg[reg] = le;
                }
                break;

                case DBG_RESTART_LOCAL: {
                    int reg = Leb128.readUnsignedLeb128(bs);
                    LocalEntry prevle;
                    LocalEntry le;

                    try {
                        prevle = lastEntryForReg[reg];

                        if (prevle.isStart) {
                            throw new RuntimeException("nonsensical "
                                    + "RESTART_LOCAL on live register v"
                                    + reg);
                        }

                        le = new LocalEntry(address, true, reg,
                                prevle.nameIndex, prevle.typeIndex, 0);
                    } catch (NullPointerException ex) {
                        throw new RuntimeException(
                                "Encountered RESTART_LOCAL on new v" + reg);
                    }

                    locals.add(le);
                    lastEntryForReg[reg] = le;
                }
                break;

                case DBG_END_LOCAL: {
                    int reg = Leb128.readUnsignedLeb128(bs);
                    LocalEntry prevle;
                    LocalEntry le;

                    try {
                        prevle = lastEntryForReg[reg];

                        if (!prevle.isStart) {
                            throw new RuntimeException("nonsensical "
                                    + "END_LOCAL on dead register v" + reg);
                        }

                        le = new LocalEntry(address, false, reg,
                                prevle.nameIndex, prevle.typeIndex,
                                prevle.signatureIndex);
                    } catch (NullPointerException ex) {
                        throw new RuntimeException(
                                "Encountered END_LOCAL on new v" + reg);
                    }

                    locals.add(le);
                    lastEntryForReg[reg] = le;
                }
                break;

                case DBG_END_SEQUENCE:
                    // all done
                return;

                case DBG_ADVANCE_PC:
                    address += Leb128.readUnsignedLeb128(bs);
                break;

                case DBG_ADVANCE_LINE:
                    line += Leb128.readSignedLeb128(bs);
                break;

                case DBG_SET_PROLOGUE_END:
                    //TODO do something with this.
                break;

                case DBG_SET_EPILOGUE_BEGIN:
                    //TODO do something with this.
                break;

                case DBG_SET_FILE:
                    //TODO do something with this.
                break;

                default:
                    if (opcode < DBG_FIRST_SPECIAL) {
                        throw new RuntimeException(
                                "Invalid extended opcode encountered "
                                        + opcode);
                    }

                    int adjopcode = opcode - DBG_FIRST_SPECIAL;

                    address += adjopcode / DBG_LINE_RANGE;
                    line += DBG_LINE_BASE + (adjopcode % DBG_LINE_RANGE);

                    positions.add(new PositionEntry(address, line));
                break;

            }
        }
    }

    /**
     * Validates an encoded debug info stream against data used to encode it,
     * throwing an exception if they do not match. Used to validate the
     * encoder.
     *
     * @param info encoded debug info
     * @param file {@code non-null;} file to refer to during decoding
     * @param ref {@code non-null;} method whose info is being decoded
     * @param code {@code non-null;} original code object that was encoded
     * @param isStatic whether the method is static
     */
    public static void validateEncode(byte[] info, DexFile file,
            CstMethodRef ref, DalvCode code, boolean isStatic) {
        PositionList pl = code.getPositions();
        LocalList ll = code.getLocals();
        DalvInsnList insns = code.getInsns();
        int codeSize = insns.codeSize();
        int countRegisters = insns.getRegistersSize();

        try {
            validateEncode0(info, codeSize, countRegisters,
                    isStatic, ref, file, pl, ll);
        } catch (RuntimeException ex) {
            System.err.println("instructions:");
            insns.debugPrint(System.err, "  ", true);
            System.err.println("local list:");
            ll.debugPrint(System.err, "  ");
            throw ExceptionWithContext.withContext(ex,
                    "while processing " + ref.toHuman());
        }
    }

    private static void validateEncode0(byte[] info, int codeSize,
            int countRegisters, boolean isStatic, CstMethodRef ref,
            DexFile file, PositionList pl, LocalList ll) {
        DebugInfoDecoder decoder
                = new DebugInfoDecoder(info, codeSize, countRegisters,
                    isStatic, ref, file);

        decoder.decode();

        /*
         * Go through the decoded position entries, matching up
         * with original entries.
         */

        List<PositionEntry> decodedEntries = decoder.getPositionList();

        if (decodedEntries.size() != pl.size()) {
            throw new RuntimeException(
                    "Decoded positions table not same size was "
                    + decodedEntries.size() + " expected " + pl.size());
        }

        for (PositionEntry entry : decodedEntries) {
            boolean found = false;
            for (int i = pl.size() - 1; i >= 0; i--) {
                PositionList.Entry ple = pl.get(i);

                if (entry.line == ple.getPosition().getLine()
                        && entry.address == ple.getAddress()) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new RuntimeException ("Could not match position entry: "
                        + entry.address + ", " + entry.line);
            }
        }

        /*
         * Go through the original local list, in order, matching up
         * with decoded entries.
         */

        List<LocalEntry> decodedLocals = decoder.getLocals();
        int thisStringIdx = decoder.thisStringIdx;
        int decodedSz = decodedLocals.size();
        int paramBase = decoder.getParamBase();

        /*
         * Preflight to fill in any parameters that were skipped in
         * the prologue (including an implied "this") but then
         * identified by full signature.
         */
        for (int i = 0; i < decodedSz; i++) {
            LocalEntry entry = decodedLocals.get(i);
            int idx = entry.nameIndex;

            if ((idx < 0) || (idx == thisStringIdx)) {
                for (int j = i + 1; j < decodedSz; j++) {
                    LocalEntry e2 = decodedLocals.get(j);
                    if (e2.address != 0) {
                        break;
                    }
                    if ((entry.reg == e2.reg) && e2.isStart) {
                        decodedLocals.set(i, e2);
                        decodedLocals.remove(j);
                        decodedSz--;
                        break;
                    }
                }
            }
        }

        int origSz = ll.size();
        int decodeAt = 0;
        boolean problem = false;

        for (int i = 0; i < origSz; i++) {
            LocalList.Entry origEntry = ll.get(i);

            if (origEntry.getDisposition()
                    == LocalList.Disposition.END_REPLACED) {
                /*
                 * The encoded list doesn't represent replacements, so
                 * ignore them for the sake of comparison.
                 */
                continue;
            }

            LocalEntry decodedEntry;

            do {
                decodedEntry = decodedLocals.get(decodeAt);
                if (decodedEntry.nameIndex >= 0) {
                    break;
                }
                /*
                 * A negative name index means this is an anonymous
                 * parameter, and we shouldn't expect to see it in the
                 * original list. So, skip it.
                 */
                decodeAt++;
            } while (decodeAt < decodedSz);

            int decodedAddress = decodedEntry.address;

            if (decodedEntry.reg != origEntry.getRegister()) {
                System.err.println("local register mismatch at orig " + i +
                        " / decoded " + decodeAt);
                problem = true;
                break;
            }

            if (decodedEntry.isStart != origEntry.isStart()) {
                System.err.println("local start/end mismatch at orig " + i +
                        " / decoded " + decodeAt);
                problem = true;
                break;
            }

            /*
             * The secondary check here accounts for the fact that a
             * parameter might not be marked as starting at 0 in the
             * original list.
             */
            if ((decodedAddress != origEntry.getAddress())
                    && !((decodedAddress == 0)
                            && (decodedEntry.reg >= paramBase))) {
                System.err.println("local address mismatch at orig " + i +
                        " / decoded " + decodeAt);
                problem = true;
                break;
            }

            decodeAt++;
        }

        if (problem) {
            System.err.println("decoded locals:");
            for (LocalEntry e : decodedLocals) {
                System.err.println("  " + e);
            }
            throw new RuntimeException("local table problem");
        }
    }
}

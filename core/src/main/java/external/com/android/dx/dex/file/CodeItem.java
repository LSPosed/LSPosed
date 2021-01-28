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

import external.com.android.dex.util.ExceptionWithContext;
import external.com.android.dx.dex.code.DalvCode;
import external.com.android.dx.dex.code.DalvInsnList;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.io.PrintWriter;

/**
 * Representation of all the parts needed for concrete methods in a
 * {@code dex} file.
 */
public final class CodeItem extends OffsettedItem {
    /** file alignment of this class, in bytes */
    private static final int ALIGNMENT = 4;

    /** write size of the header of this class, in bytes */
    private static final int HEADER_SIZE = 16;

    /** {@code non-null;} method that this code implements */
    private final CstMethodRef ref;

    /** {@code non-null;} the bytecode instructions and associated data */
    private final DalvCode code;

    /** {@code null-ok;} the catches, if needed; set in {@link #addContents} */
    private CatchStructs catches;

    /** whether this instance is for a {@code static} method */
    private final boolean isStatic;

    /**
     * {@code non-null;} list of possibly-thrown exceptions; just used in
     * generating debugging output (listings)
     */
    private final TypeList throwsList;

    /**
     * {@code null-ok;} the debug info or {@code null} if there is none;
     * set in {@link #addContents}
     */
    private DebugInfoItem debugInfo;

    /**
     * Constructs an instance.
     *
     * @param ref {@code non-null;} method that this code implements
     * @param code {@code non-null;} the underlying code
     * @param isStatic whether this instance is for a {@code static}
     * method
     * @param throwsList {@code non-null;} list of possibly-thrown exceptions,
     * just used in generating debugging output (listings)
     */
    public CodeItem(CstMethodRef ref, DalvCode code, boolean isStatic,
            TypeList throwsList) {
        super(ALIGNMENT, -1);

        if (ref == null) {
            throw new NullPointerException("ref == null");
        }

        if (code == null) {
            throw new NullPointerException("code == null");
        }

        if (throwsList == null) {
            throw new NullPointerException("throwsList == null");
        }

        this.ref = ref;
        this.code = code;
        this.isStatic = isStatic;
        this.throwsList = throwsList;
        this.catches = null;
        this.debugInfo = null;
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_CODE_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        MixedItemSection byteData = file.getByteData();
        TypeIdsSection typeIds = file.getTypeIds();

        if (code.hasPositions() || code.hasLocals()) {
            debugInfo = new DebugInfoItem(code, isStatic, ref);
            byteData.add(debugInfo);
        }

        if (code.hasAnyCatches()) {
            for (Type type : code.getCatchTypes()) {
                typeIds.intern(type);
            }
            catches = new CatchStructs(code);
        }

        for (Constant c : code.getInsnConstants()) {
            file.internIfAppropriate(c);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "CodeItem{" + toHuman() + "}";
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return ref.toHuman();
    }

    /**
     * Gets the reference to the method this instance implements.
     *
     * @return {@code non-null;} the method reference
     */
    public CstMethodRef getRef() {
        return ref;
    }

    /**
     * Does a human-friendly dump of this instance.
     *
     * @param out {@code non-null;} where to dump
     * @param prefix {@code non-null;} per-line prefix to use
     * @param verbose whether to be verbose with the output
     */
    public void debugPrint(PrintWriter out, String prefix, boolean verbose) {
        out.println(ref.toHuman() + ":");

        DalvInsnList insns = code.getInsns();
        out.println("regs: " + Hex.u2(getRegistersSize()) +
                "; ins: " + Hex.u2(getInsSize()) + "; outs: " +
                Hex.u2(getOutsSize()));

        insns.debugPrint(out, prefix, verbose);

        String prefix2 = prefix + "  ";

        if (catches != null) {
            out.print(prefix);
            out.println("catches");
            catches.debugPrint(out, prefix2);
        }

        if (debugInfo != null) {
            out.print(prefix);
            out.println("debug info");
            debugInfo.debugPrint(out, prefix2);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void place0(Section addedTo, int offset) {
        final DexFile file = addedTo.getFile();
        int catchesSize;

        /*
         * In order to get the catches and insns, all the code's
         * constants need to be assigned indices.
         */
        code.assignIndices(new DalvCode.AssignIndicesCallback() {
                @Override
                public int getIndex(Constant cst) {
                    IndexedItem item = file.findItemOrNull(cst);
                    if (item == null) {
                        return -1;
                    }
                    return item.getIndex();
                }
            });

        if (catches != null) {
            catches.encode(file);
            catchesSize = catches.writeSize();
        } else {
            catchesSize = 0;
        }

        /*
         * The write size includes the header, two bytes per code
         * unit, post-code padding if necessary, and however much
         * space the catches need.
         */

        int insnsSize = code.getInsns().codeSize();
        if ((insnsSize & 1) != 0) {
            insnsSize++;
        }

        setWriteSize(HEADER_SIZE + (insnsSize * 2) + catchesSize);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        boolean annotates = out.annotates();
        int regSz = getRegistersSize();
        int outsSz = getOutsSize();
        int insSz = getInsSize();
        int insnsSz = code.getInsns().codeSize();
        boolean needPadding = (insnsSz & 1) != 0;
        int triesSz = (catches == null) ? 0 : catches.triesSize();
        int debugOff = (debugInfo == null) ? 0 : debugInfo.getAbsoluteOffset();

        if (annotates) {
            out.annotate(0, offsetString() + ' ' + ref.toHuman());
            out.annotate(2, "  registers_size: " + Hex.u2(regSz));
            out.annotate(2, "  ins_size:       " + Hex.u2(insSz));
            out.annotate(2, "  outs_size:      " + Hex.u2(outsSz));
            out.annotate(2, "  tries_size:     " + Hex.u2(triesSz));
            out.annotate(4, "  debug_off:      " + Hex.u4(debugOff));
            out.annotate(4, "  insns_size:     " + Hex.u4(insnsSz));

            // This isn't represented directly here, but it is useful to see.
            int size = throwsList.size();
            if (size != 0) {
                out.annotate(0, "  throws " + StdTypeList.toHuman(throwsList));
            }
        }

        out.writeShort(regSz);
        out.writeShort(insSz);
        out.writeShort(outsSz);
        out.writeShort(triesSz);
        out.writeInt(debugOff);
        out.writeInt(insnsSz);

        writeCodes(file, out);

        if (catches != null) {
            if (needPadding) {
                if (annotates) {
                    out.annotate(2, "  padding: 0");
                }
                out.writeShort(0);
            }

            catches.writeTo(file, out);
        }

        if (annotates) {
            /*
             * These are pointed at in the code header (above), but it's less
             * distracting to expand on them at the bottom of the code.
             */
            if (debugInfo != null) {
                out.annotate(0, "  debug info");
                debugInfo.annotateTo(file, out, "    ");
            }
        }
    }

    /**
     * Helper for {@link #writeTo0} which writes out the actual bytecode.
     *
     * @param file {@code non-null;} file we are part of
     * @param out {@code non-null;} where to write to
     */
    private void writeCodes(DexFile file, AnnotatedOutput out) {
        DalvInsnList insns = code.getInsns();

        try {
            insns.writeTo(out);
        } catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex, "...while writing " +
                    "instructions for " + ref.toHuman());
        }
    }

    /**
     * Get the in registers count.
     *
     * @return the count
     */
    private int getInsSize() {
        return ref.getParameterWordCount(isStatic);
    }

    /**
     * Get the out registers count.
     *
     * @return the count
     */
    private int getOutsSize() {
        return code.getInsns().getOutsSize();
    }

    /**
     * Get the total registers count.
     *
     * @return the count
     */
    private int getRegistersSize() {
        return code.getInsns().getRegistersSize();
    }
}

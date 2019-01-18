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
import external.com.android.dx.dex.code.LocalList;
import external.com.android.dx.dex.code.PositionList;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.util.AnnotatedOutput;
import java.io.PrintWriter;

public class DebugInfoItem extends OffsettedItem {
    /** the required alignment for instances of this class */
    private static final int ALIGNMENT = 1;

    private static final boolean ENABLE_ENCODER_SELF_CHECK = false;

    /** {@code non-null;} the code this item represents */
    private final DalvCode code;

    private byte[] encoded;

    private final boolean isStatic;
    private final CstMethodRef ref;

    public DebugInfoItem(DalvCode code, boolean isStatic, CstMethodRef ref) {
        // We don't know the write size yet.
        super (ALIGNMENT, -1);

        if (code == null) {
            throw new NullPointerException("code == null");
        }

        this.code = code;
        this.isStatic = isStatic;
        this.ref = ref;
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_DEBUG_INFO_ITEM;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        // No contents to add.
    }

    /** {@inheritDoc} */
    @Override
    protected void place0(Section addedTo, int offset) {
        // Encode the data and note the size.

        try {
            encoded = encode(addedTo.getFile(), null, null, null, false);
            setWriteSize(encoded.length);
        } catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex,
                    "...while placing debug info for " + ref.toHuman());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        throw new RuntimeException("unsupported");
    }

    /**
     * Writes annotations for the elements of this list, as
     * zero-length. This is meant to be used for dumping this instance
     * directly after a code dump (with the real local list actually
     * existing elsewhere in the output).
     *
     * @param file {@code non-null;} the file to use for referencing other sections
     * @param out {@code non-null;} where to annotate to
     * @param prefix {@code null-ok;} prefix to attach to each line of output
     */
    public void annotateTo(DexFile file, AnnotatedOutput out, String prefix) {
        encode(file, prefix, null, out, false);
    }

    /**
     * Does a human-friendly dump of this instance.
     *
     * @param out {@code non-null;} where to dump
     * @param prefix {@code non-null;} prefix to attach to each line of output
     */
    public void debugPrint(PrintWriter out, String prefix) {
        encode(null, prefix, out, null, false);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        if (out.annotates()) {
            /*
             * Re-run the encoder to generate the annotations,
             * but write the bits from the original encode
             */

            out.annotate(offsetString() + " debug info");
            encode(file, null, null, out, true);
        }

        out.write(encoded);
    }

    /**
     * Performs debug info encoding.
     *
     * @param file {@code null-ok;} file to refer to during encoding
     * @param prefix {@code null-ok;} prefix to attach to each line of output
     * @param debugPrint {@code null-ok;} if specified, an alternate output for
     * annotations
     * @param out {@code null-ok;} if specified, where annotations should go
     * @param consume whether to claim to have consumed output for
     * {@code out}
     * @return {@code non-null;} the encoded array
     */
    private byte[] encode(DexFile file, String prefix, PrintWriter debugPrint,
            AnnotatedOutput out, boolean consume) {
        byte[] result = encode0(file, prefix, debugPrint, out, consume);

        if (ENABLE_ENCODER_SELF_CHECK && (file != null)) {
            try {
                DebugInfoDecoder.validateEncode(result, file, ref, code,
                        isStatic);
            } catch (RuntimeException ex) {
                // Reconvert, annotating to System.err.
                encode0(file, "", new PrintWriter(System.err, true), null,
                        false);
                throw ex;
            }
        }

        return result;
    }

    /**
     * Helper for {@link #encode} to do most of the work.
     *
     * @param file {@code null-ok;} file to refer to during encoding
     * @param prefix {@code null-ok;} prefix to attach to each line of output
     * @param debugPrint {@code null-ok;} if specified, an alternate output for
     * annotations
     * @param out {@code null-ok;} if specified, where annotations should go
     * @param consume whether to claim to have consumed output for
     * {@code out}
     * @return {@code non-null;} the encoded array
     */
    private byte[] encode0(DexFile file, String prefix, PrintWriter debugPrint,
            AnnotatedOutput out, boolean consume) {
        PositionList positions = code.getPositions();
        LocalList locals = code.getLocals();
        DalvInsnList insns = code.getInsns();
        int codeSize = insns.codeSize();
        int regSize = insns.getRegistersSize();

        DebugInfoEncoder encoder =
            new DebugInfoEncoder(positions, locals,
                    file, codeSize, regSize, isStatic, ref);

        byte[] result;

        if ((debugPrint == null) && (out == null)) {
            result = encoder.convert();
        } else {
            result = encoder.convertAndAnnotate(prefix, debugPrint, out,
                    consume);
        }

        return result;
    }
}

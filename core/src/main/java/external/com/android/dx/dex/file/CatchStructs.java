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

package external.com.android.dx.dex.file;

import external.com.android.dx.dex.code.CatchHandlerList;
import external.com.android.dx.dex.code.CatchTable;
import external.com.android.dx.dex.code.DalvCode;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.ByteArrayAnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

/**
 * List of exception handlers (tuples of covered range, catch type,
 * handler address) for a particular piece of code. Instances of this
 * class correspond to a {@code try_item[]} and a
 * {@code catch_handler_item[]}.
 */
public final class CatchStructs {
    /**
     * the size of a {@code try_item}: a {@code uint}
     * and two {@code ushort}s
     */
    private static final int TRY_ITEM_WRITE_SIZE = 4 + (2 * 2);

    /** {@code non-null;} code that contains the catches */
    private final DalvCode code;

    /**
     * {@code null-ok;} the underlying table; set in
     * {@link #finishProcessingIfNecessary}
     */
    private CatchTable table;

    /**
     * {@code null-ok;} the encoded handler list, if calculated; set in
     * {@link #encode}
     */
    private byte[] encodedHandlers;

    /**
     * length of the handlers header (encoded size), if known; used for
     * annotation
     */
    private int encodedHandlerHeaderSize;

    /**
     * {@code null-ok;} map from handler lists to byte offsets, if calculated; set in
     * {@link #encode}
     */
    private TreeMap<CatchHandlerList, Integer> handlerOffsets;

    /**
     * Constructs an instance.
     *
     * @param code {@code non-null;} code that contains the catches
     */
    public CatchStructs(DalvCode code) {
        this.code = code;
        this.table = null;
        this.encodedHandlers = null;
        this.encodedHandlerHeaderSize = 0;
        this.handlerOffsets = null;
    }

    /**
     * Finish processing the catches, if necessary.
     */
    private void finishProcessingIfNecessary() {
        if (table == null) {
            table = code.getCatches();
        }
    }

    /**
     * Gets the size of the tries list, in entries.
     *
     * @return {@code >= 0;} the tries list size
     */
    public int triesSize() {
        finishProcessingIfNecessary();
        return table.size();
    }

    /**
     * Does a human-friendly dump of this instance.
     *
     * @param out {@code non-null;} where to dump
     * @param prefix {@code non-null;} prefix to attach to each line of output
     */
    public void debugPrint(PrintWriter out, String prefix) {
        annotateEntries(prefix, out, null);
    }

    /**
     * Encodes the handler lists.
     *
     * @param file {@code non-null;} file this instance is part of
     */
    public void encode(DexFile file) {
        finishProcessingIfNecessary();

        TypeIdsSection typeIds = file.getTypeIds();
        int size = table.size();

        handlerOffsets = new TreeMap<CatchHandlerList, Integer>();

        /*
         * First add a map entry for each unique list. The tree structure
         * will ensure they are sorted when we reiterate later.
         */
        for (int i = 0; i < size; i++) {
            handlerOffsets.put(table.get(i).getHandlers(), null);
        }

        if (handlerOffsets.size() > 65535) {
            throw new UnsupportedOperationException(
                    "too many catch handlers");
        }

        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();

        // Write out the handlers "header" consisting of its size in entries.
        encodedHandlerHeaderSize =
            out.writeUleb128(handlerOffsets.size());

        // Now write the lists out in order, noting the offset of each.
        for (Map.Entry<CatchHandlerList, Integer> mapping :
                 handlerOffsets.entrySet()) {
            CatchHandlerList list = mapping.getKey();
            int listSize = list.size();
            boolean catchesAll = list.catchesAll();

            // Set the offset before we do any writing.
            mapping.setValue(out.getCursor());

            if (catchesAll) {
                // A size <= 0 means that the list ends with a catch-all.
                out.writeSleb128(-(listSize - 1));
                listSize--;
            } else {
                out.writeSleb128(listSize);
            }

            for (int i = 0; i < listSize; i++) {
                CatchHandlerList.Entry entry = list.get(i);
                out.writeUleb128(
                        typeIds.indexOf(entry.getExceptionType()));
                out.writeUleb128(entry.getHandler());
            }

            if (catchesAll) {
                out.writeUleb128(list.get(listSize).getHandler());
            }
        }

        encodedHandlers = out.toByteArray();
    }

    /**
     * Gets the write size of this instance, in bytes.
     *
     * @return {@code >= 0;} the write size
     */
    public int writeSize() {
        return (triesSize() * TRY_ITEM_WRITE_SIZE) +
                + encodedHandlers.length;
    }

    /**
     * Writes this instance to the given stream.
     *
     * @param file {@code non-null;} file this instance is part of
     * @param out {@code non-null;} where to write to
     */
    public void writeTo(DexFile file, AnnotatedOutput out) {
        finishProcessingIfNecessary();

        if (out.annotates()) {
            annotateEntries("  ", null, out);
        }

        int tableSize = table.size();
        for (int i = 0; i < tableSize; i++) {
            CatchTable.Entry one = table.get(i);
            int start = one.getStart();
            int end = one.getEnd();
            int insnCount = end - start;

            if (insnCount >= 65536) {
                throw new UnsupportedOperationException(
                        "bogus exception range: " + Hex.u4(start) + ".." +
                        Hex.u4(end));
            }

            out.writeInt(start);
            out.writeShort(insnCount);
            out.writeShort(handlerOffsets.get(one.getHandlers()));
        }

        out.write(encodedHandlers);
    }

    /**
     * Helper method to annotate or simply print the exception handlers.
     * Only one of {@code printTo} or {@code annotateTo} should
     * be non-null.
     *
     * @param prefix {@code non-null;} prefix for each line
     * @param printTo {@code null-ok;} where to print to
     * @param annotateTo {@code null-ok;} where to consume bytes and annotate to
     */
    private void annotateEntries(String prefix, PrintWriter printTo,
            AnnotatedOutput annotateTo) {
        finishProcessingIfNecessary();

        boolean consume = (annotateTo != null);
        int amt1 = consume ? 6 : 0;
        int amt2 = consume ? 2 : 0;
        int size = table.size();
        String subPrefix = prefix + "  ";

        if (consume) {
            annotateTo.annotate(0, prefix + "tries:");
        } else {
            printTo.println(prefix + "tries:");
        }

        for (int i = 0; i < size; i++) {
            CatchTable.Entry entry = table.get(i);
            CatchHandlerList handlers = entry.getHandlers();
            String s1 = subPrefix + "try " + Hex.u2or4(entry.getStart())
                + ".." + Hex.u2or4(entry.getEnd());
            String s2 = handlers.toHuman(subPrefix, "");

            if (consume) {
                annotateTo.annotate(amt1, s1);
                annotateTo.annotate(amt2, s2);
            } else {
                printTo.println(s1);
                printTo.println(s2);
            }
        }

        if (! consume) {
            // Only emit the handler lists if we are consuming bytes.
            return;
        }

        annotateTo.annotate(0, prefix + "handlers:");
        annotateTo.annotate(encodedHandlerHeaderSize,
                subPrefix + "size: " + Hex.u2(handlerOffsets.size()));

        int lastOffset = 0;
        CatchHandlerList lastList = null;

        for (Map.Entry<CatchHandlerList, Integer> mapping :
                 handlerOffsets.entrySet()) {
            CatchHandlerList list = mapping.getKey();
            int offset = mapping.getValue();

            if (lastList != null) {
                annotateAndConsumeHandlers(lastList, lastOffset,
                        offset - lastOffset, subPrefix, printTo, annotateTo);
            }

            lastList = list;
            lastOffset = offset;
        }

        annotateAndConsumeHandlers(lastList, lastOffset,
                encodedHandlers.length - lastOffset,
                subPrefix, printTo, annotateTo);
    }

    /**
     * Helper for {@link #annotateEntries} to annotate a catch handler list
     * while consuming it.
     *
     * @param handlers {@code non-null;} handlers to annotate
     * @param offset {@code >= 0;} the offset of this handler
     * @param size {@code >= 1;} the number of bytes the handlers consume
     * @param prefix {@code non-null;} prefix for each line
     * @param printTo {@code null-ok;} where to print to
     * @param annotateTo {@code non-null;} where to annotate to
     */
    private static void annotateAndConsumeHandlers(CatchHandlerList handlers,
            int offset, int size, String prefix, PrintWriter printTo,
            AnnotatedOutput annotateTo) {
        String s = handlers.toHuman(prefix, Hex.u2(offset) + ": ");

        if (printTo != null) {
            printTo.println(s);
        }

        annotateTo.annotate(size, s);
    }
}

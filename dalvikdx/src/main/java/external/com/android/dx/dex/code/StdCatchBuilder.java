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

import external.com.android.dx.rop.code.BasicBlock;
import external.com.android.dx.rop.code.BasicBlockList;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Constructor of {@link CatchTable} instances from {@link RopMethod}
 * and associated data.
 */
public final class StdCatchBuilder implements CatchBuilder {
    /** the maximum range of a single catch handler, in code units */
    private static final int MAX_CATCH_RANGE = 65535;

    /** {@code non-null;} method to build the list for */
    private final RopMethod method;

    /** {@code non-null;} block output order */
    private final int[] order;

    /** {@code non-null;} address objects for each block */
    private final BlockAddresses addresses;

    /**
     * Constructs an instance. It merely holds onto its parameters for
     * a subsequent call to {@link #build}.
     *
     * @param method {@code non-null;} method to build the list for
     * @param order {@code non-null;} block output order
     * @param addresses {@code non-null;} address objects for each block
     */
    public StdCatchBuilder(RopMethod method, int[] order,
            BlockAddresses addresses) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        if (order == null) {
            throw new NullPointerException("order == null");
        }

        if (addresses == null) {
            throw new NullPointerException("addresses == null");
        }

        this.method = method;
        this.order = order;
        this.addresses = addresses;
    }

    /** {@inheritDoc} */
    @Override
    public CatchTable build() {
        return build(method, order, addresses);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasAnyCatches() {
        BasicBlockList blocks = method.getBlocks();
        int size = blocks.size();

        for (int i = 0; i < size; i++) {
            BasicBlock block = blocks.get(i);
            TypeList catches = block.getLastInsn().getCatches();
            if (catches.size() != 0) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public HashSet<Type> getCatchTypes() {
        HashSet<Type> result = new HashSet<Type>(20);
        BasicBlockList blocks = method.getBlocks();
        int size = blocks.size();

        for (int i = 0; i < size; i++) {
            BasicBlock block = blocks.get(i);
            TypeList catches = block.getLastInsn().getCatches();
            int catchSize = catches.size();

            for (int j = 0; j < catchSize; j++) {
                result.add(catches.getType(j));
            }
        }

        return result;
    }

    /**
     * Builds and returns the catch table for a given method.
     *
     * @param method {@code non-null;} method to build the list for
     * @param order {@code non-null;} block output order
     * @param addresses {@code non-null;} address objects for each block
     * @return {@code non-null;} the constructed table
     */
    public static CatchTable build(RopMethod method, int[] order,
            BlockAddresses addresses) {
        int len = order.length;
        BasicBlockList blocks = method.getBlocks();
        ArrayList<CatchTable.Entry> resultList =
            new ArrayList<CatchTable.Entry>(len);
        CatchHandlerList currentHandlers = CatchHandlerList.EMPTY;
        BasicBlock currentStartBlock = null;
        BasicBlock currentEndBlock = null;

        for (int i = 0; i < len; i++) {
            BasicBlock block = blocks.labelToBlock(order[i]);

            if (!block.canThrow()) {
                /*
                 * There is no need to concern ourselves with the
                 * placement of blocks that can't throw with respect
                 * to the blocks that *can* throw.
                 */
                continue;
            }

            CatchHandlerList handlers = handlersFor(block, addresses);

            if (currentHandlers.size() == 0) {
                // This is the start of a new catch range.
                currentStartBlock = block;
                currentEndBlock = block;
                currentHandlers = handlers;
                continue;
            }

            if (currentHandlers.equals(handlers)
                    && rangeIsValid(currentStartBlock, block, addresses)) {
                /*
                 * The block we are looking at now has the same handlers
                 * as the block that started the currently open catch
                 * range, and adding it to the currently open range won't
                 * cause it to be too long.
                 */
                currentEndBlock = block;
                continue;
            }

            /*
             * The block we are looking at now has incompatible handlers,
             * so we need to finish off the last entry and start a new
             * one. Note: We only emit an entry if it has associated handlers.
             */
            if (currentHandlers.size() != 0) {
                CatchTable.Entry entry =
                    makeEntry(currentStartBlock, currentEndBlock,
                            currentHandlers, addresses);
                resultList.add(entry);
            }

            currentStartBlock = block;
            currentEndBlock = block;
            currentHandlers = handlers;
        }

        if (currentHandlers.size() != 0) {
            // Emit an entry for the range that was left hanging.
            CatchTable.Entry entry =
                makeEntry(currentStartBlock, currentEndBlock,
                        currentHandlers, addresses);
            resultList.add(entry);
        }

        // Construct the final result.

        int resultSz = resultList.size();

        if (resultSz == 0) {
            return CatchTable.EMPTY;
        }

        CatchTable result = new CatchTable(resultSz);

        for (int i = 0; i < resultSz; i++) {
            result.set(i, resultList.get(i));
        }

        result.setImmutable();
        return result;
    }

    /**
     * Makes the {@link CatchHandlerList} for the given basic block.
     *
     * @param block {@code non-null;} block to get entries for
     * @param addresses {@code non-null;} address objects for each block
     * @return {@code non-null;} array of entries
     */
    private static CatchHandlerList handlersFor(BasicBlock block,
            BlockAddresses addresses) {
        IntList successors = block.getSuccessors();
        int succSize = successors.size();
        int primary = block.getPrimarySuccessor();
        TypeList catches = block.getLastInsn().getCatches();
        int catchSize = catches.size();

        if (catchSize == 0) {
            return CatchHandlerList.EMPTY;
        }

        if (((primary == -1) && (succSize != catchSize))
                || ((primary != -1) &&
                        ((succSize != (catchSize + 1))
                                || (primary != successors.get(catchSize))))) {
            /*
             * Blocks that throw are supposed to list their primary
             * successor -- if any -- last in the successors list, but
             * that constraint appears to be violated here.
             */
            throw new RuntimeException(
                    "shouldn't happen: weird successors list");
        }

        /*
         * Reduce the effective catchSize if we spot a catch-all that
         * isn't at the end.
         */
        for (int i = 0; i < catchSize; i++) {
            Type type = catches.getType(i);
            if (type.equals(Type.OBJECT)) {
                catchSize = i + 1;
                break;
            }
        }

        CatchHandlerList result = new CatchHandlerList(catchSize);

        for (int i = 0; i < catchSize; i++) {
            CstType oneType = new CstType(catches.getType(i));
            CodeAddress oneHandler = addresses.getStart(successors.get(i));
            result.set(i, oneType, oneHandler.getAddress());
        }

        result.setImmutable();
        return result;
    }

    /**
     * Makes a {@link CatchTable.Entry} for the given block range and
     * handlers.
     *
     * @param start {@code non-null;} the start block for the range (inclusive)
     * @param end {@code non-null;} the start block for the range (also inclusive)
     * @param handlers {@code non-null;} the handlers for the range
     * @param addresses {@code non-null;} address objects for each block
     */
    private static CatchTable.Entry makeEntry(BasicBlock start,
            BasicBlock end, CatchHandlerList handlers,
            BlockAddresses addresses) {
        /*
         * We start at the *last* instruction of the start block, since
         * that's the instruction that can throw...
         */
        CodeAddress startAddress = addresses.getLast(start);

        // ...And we end *after* the last instruction of the end block.
        CodeAddress endAddress = addresses.getEnd(end);

        return new CatchTable.Entry(startAddress.getAddress(),
                endAddress.getAddress(), handlers);
    }

    /**
     * Gets whether the address range for the given two blocks is valid
     * for a catch handler. This is true as long as the covered range is
     * under 65536 code units.
     *
     * @param start {@code non-null;} the start block for the range (inclusive)
     * @param end {@code non-null;} the start block for the range (also inclusive)
     * @param addresses {@code non-null;} address objects for each block
     * @return {@code true} if the range is valid as a catch range
     */
    private static boolean rangeIsValid(BasicBlock start, BasicBlock end,
            BlockAddresses addresses) {
        if (start == null) {
            throw new NullPointerException("start == null");
        }

        if (end == null) {
            throw new NullPointerException("end == null");
        }

        // See above about selection of instructions.
        int startAddress = addresses.getLast(start).getAddress();
        int endAddress = addresses.getEnd(end).getAddress();

        return (endAddress - startAddress) <= MAX_CATCH_RANGE;
    }
}

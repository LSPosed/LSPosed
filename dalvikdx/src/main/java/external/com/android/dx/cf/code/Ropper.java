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

package external.com.android.dx.cf.code;

import external.com.android.dx.cf.iface.MethodList;
import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.code.BasicBlock;
import external.com.android.dx.rop.code.BasicBlockList;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.InsnList;
import external.com.android.dx.rop.code.PlainCstInsn;
import external.com.android.dx.rop.code.PlainInsn;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.Rops;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.code.ThrowingCstInsn;
import external.com.android.dx.rop.code.ThrowingInsn;
import external.com.android.dx.rop.code.TranslationAdvice;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.Bits;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility that converts a basic block list into a list of register-oriented
 * blocks.
 */
public final class Ropper {
    /** label offset for the parameter assignment block */
    private static final int PARAM_ASSIGNMENT = -1;

    /** label offset for the return block */
    private static final int RETURN = -2;

    /** label offset for the synchronized method final return block */
    private static final int SYNCH_RETURN = -3;

    /** label offset for the first synchronized method setup block */
    private static final int SYNCH_SETUP_1 = -4;

    /** label offset for the second synchronized method setup block */
    private static final int SYNCH_SETUP_2 = -5;

    /**
     * label offset for the first synchronized method exception
     * handler block
     */
    private static final int SYNCH_CATCH_1 = -6;

    /**
     * label offset for the second synchronized method exception
     * handler block
     */
    private static final int SYNCH_CATCH_2 = -7;

    /** number of special label offsets */
    private static final int SPECIAL_LABEL_COUNT = 7;

    /** {@code non-null;} method being converted */
    private final ConcreteMethod method;

    /** {@code non-null;} original block list */
    private final ByteBlockList blocks;

    /** max locals of the method */
    private final int maxLocals;

    /** max label (exclusive) of any original bytecode block */
    private final int maxLabel;

    /** {@code non-null;} simulation machine to use */
    private final RopperMachine machine;

    /** {@code non-null;} simulator to use */
    private final Simulator sim;

    /**
     * {@code non-null;} sparse array mapping block labels to initial frame
     * contents, if known
     */
    private final Frame[] startFrames;

    /** {@code non-null;} output block list in-progress */
    private final ArrayList<BasicBlock> result;

    /**
     * {@code non-null;} list of subroutine-nest labels
     * (See {@link Frame#getSubroutines} associated with each result block.
     * Parallel to {@link Ropper#result}.
     */
    private final ArrayList<IntList> resultSubroutines;

    /**
     * {@code non-null;} for each block (by label) that is used as an exception
     * handler in the input, the exception handling info in Rop.
     */
    private final CatchInfo[] catchInfos;

    /**
     * whether an exception-handler block for a synchronized method was
     * ever required
     */
    private boolean synchNeedsExceptionHandler;

    /**
     * {@code non-null;} list of subroutines indexed by label of start
     * address */
    private final Subroutine[] subroutines;

    /** true if {@code subroutines} is non-empty */
    private boolean hasSubroutines;

    /** Allocates labels of exception handler setup blocks. */
    private final ExceptionSetupLabelAllocator exceptionSetupLabelAllocator;

    /**
     * Keeps mapping of an input exception handler target code and how it is generated/targeted in
     * Rop.
     */
    private class CatchInfo {
        /**
         * {@code non-null;} map of ExceptionHandlerSetup by the type they handle */
        private final Map<Type, ExceptionHandlerSetup> setups =
                new HashMap<Type, ExceptionHandlerSetup>();

        /**
         * Get the {@link ExceptionHandlerSetup} corresponding to the given type. The
         * ExceptionHandlerSetup is created if this the first request for the given type.
         *
         * @param caughtType {@code non-null;}  the type catch by the requested setup
         * @return {@code non-null;} the handler setup block info for the given type
         */
        ExceptionHandlerSetup getSetup(Type caughtType) {
            ExceptionHandlerSetup handler = setups.get(caughtType);
            if (handler == null) {
                int handlerSetupLabel = exceptionSetupLabelAllocator.getNextLabel();
                handler = new ExceptionHandlerSetup(caughtType, handlerSetupLabel);
                setups.put(caughtType, handler);
            }
            return handler;
        }

        /**
         * Get all {@link ExceptionHandlerSetup} of this handler.
         *
         * @return {@code non-null;}
         */
       Collection<ExceptionHandlerSetup> getSetups() {
            return setups.values();
        }
    }

    /**
     * Keeps track of an exception handler setup.
     */
    private static class ExceptionHandlerSetup {
        /**
         * {@code non-null;} The caught type. */
        private Type caughtType;
        /**
         * {@code >= 0;} The label of the exception setup block. */
        private int label;

        /**
         * Constructs instance.
         *
         * @param caughtType {@code non-null;} the caught type
         * @param label {@code >= 0;} the label
         */
        ExceptionHandlerSetup(Type caughtType, int label) {
            this.caughtType = caughtType;
            this.label = label;
        }

        /**
         * @return {@code non-null;} the caught type
         */
        Type getCaughtType() {
            return caughtType;
        }

        /**
         * @return {@code >= 0;} the label
         */
        public int getLabel() {
            return label;
        }
    }

    /**
     * Keeps track of subroutines that exist in java form and are inlined in
     * Rop form.
     */
    private class Subroutine {
        /** list of all blocks that jsr to this subroutine */
        private BitSet callerBlocks;
        /** List of all blocks that return from this subroutine */
        private BitSet retBlocks;
        /** first block in this subroutine */
        private int startBlock;

        /**
         * Constructs instance.
         *
         * @param startBlock First block of the subroutine.
         */
        Subroutine(int startBlock) {
            this.startBlock = startBlock;
            retBlocks = new BitSet(maxLabel);
            callerBlocks = new BitSet(maxLabel);
            hasSubroutines = true;
        }

        /**
         * Constructs instance.
         *
         * @param startBlock First block of the subroutine.
         * @param retBlock one of the ret blocks (final blocks) of this
         * subroutine.
         */
        Subroutine(int startBlock, int retBlock) {
            this(startBlock);
            addRetBlock(retBlock);
        }

        /**
         * @return {@code >= 0;} the label of the subroutine's start block.
         */
        int getStartBlock() {
            return startBlock;
        }

        /**
         * Adds a label to the list of ret blocks (final blocks) for this
         * subroutine.
         *
         * @param retBlock ret block label
         */
        void addRetBlock(int retBlock) {
            retBlocks.set(retBlock);
        }

        /**
         * Adds a label to the list of caller blocks for this subroutine.
         *
         * @param label a block that invokes this subroutine.
         */
        void addCallerBlock(int label) {
            callerBlocks.set(label);
        }

        /**
         * Generates a list of subroutine successors. Note: successor blocks
         * could be listed more than once. This is ok, because this successor
         * list (and the block it's associated with) will be copied and inlined
         * before we leave the ropper. Redundent successors will result in
         * redundent (no-op) merges.
         *
         * @return all currently known successors
         * (return destinations) for that subroutine
         */
        IntList getSuccessors() {
            IntList successors = new IntList(callerBlocks.size());

            /*
             * For each subroutine caller, get it's target. If the
             * target is us, add the ret target (subroutine successor)
             * to our list
             */

            for (int label = callerBlocks.nextSetBit(0); label >= 0;
                 label = callerBlocks.nextSetBit(label+1)) {
                BasicBlock subCaller = labelToBlock(label);
                successors.add(subCaller.getSuccessors().get(0));
            }

            successors.setImmutable();

            return successors;
        }

        /**
         * Merges the specified frame into this subroutine's successors,
         * setting {@code workSet} as appropriate. To be called with
         * the frame of a subroutine ret block.
         *
         * @param frame {@code non-null;} frame from ret block to merge
         * @param workSet {@code non-null;} workset to update
         */
        void mergeToSuccessors(Frame frame, int[] workSet) {
            for (int label = callerBlocks.nextSetBit(0); label >= 0;
                 label = callerBlocks.nextSetBit(label+1)) {
                BasicBlock subCaller = labelToBlock(label);
                int succLabel = subCaller.getSuccessors().get(0);

                Frame subFrame = frame.subFrameForLabel(startBlock, label);

                if (subFrame != null) {
                    mergeAndWorkAsNecessary(succLabel, -1, null,
                            subFrame, workSet);
                } else {
                    Bits.set(workSet, label);
                }
            }
        }
    }

    /**
     * Converts a {@link ConcreteMethod} to a {@link RopMethod}.
     *
     * @param method {@code non-null;} method to convert
     * @param advice {@code non-null;} translation advice to use
     * @param methods {@code non-null;} list of methods defined by the class
     *     that defines {@code method}.
     * @return {@code non-null;} the converted instance
     */
    public static RopMethod convert(ConcreteMethod method,
            TranslationAdvice advice, MethodList methods, DexOptions dexOptions) {
        try {
            Ropper r = new Ropper(method, advice, methods, dexOptions);
            r.doit();
            return r.getRopMethod();
        } catch (SimException ex) {
            ex.addContext("...while working on method " +
                          method.getNat().toHuman());
            throw ex;
        }
    }

    /**
     * Constructs an instance. This class is not publicly instantiable; use
     * {@link #convert}.
     *
     * @param method {@code non-null;} method to convert
     * @param advice {@code non-null;} translation advice to use
     * @param methods {@code non-null;} list of methods defined by the class
     *     that defines {@code method}.
     * @param dexOptions {@code non-null;} options for dex output
     */
    private Ropper(ConcreteMethod method, TranslationAdvice advice, MethodList methods,
            DexOptions dexOptions) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        if (advice == null) {
            throw new NullPointerException("advice == null");
        }

        this.method = method;
        this.blocks = BasicBlocker.identifyBlocks(method);
        this.maxLabel = blocks.getMaxLabel();
        this.maxLocals = method.getMaxLocals();
        this.machine = new RopperMachine(this, method, advice, methods);
        this.sim = new Simulator(machine, method, dexOptions);
        this.startFrames = new Frame[maxLabel];
        this.subroutines = new Subroutine[maxLabel];

        /*
         * The "* 2 + 10" below is to conservatively believe that every
         * block is an exception handler target and should also
         * take care of enough other possible extra overhead such that
         * the underlying array is unlikely to need resizing.
         */
        this.result = new ArrayList<BasicBlock>(blocks.size() * 2 + 10);
        this.resultSubroutines =
            new ArrayList<IntList>(blocks.size() * 2 + 10);

        this.catchInfos = new CatchInfo[maxLabel];
        this.synchNeedsExceptionHandler = false;

        /*
         * Set up the first stack frame with the right limits, but leave it
         * empty here (to be filled in outside of the constructor).
         */
        startFrames[0] = new Frame(maxLocals, method.getMaxStack());
        exceptionSetupLabelAllocator = new ExceptionSetupLabelAllocator();
    }

    /**
     * Gets the first (lowest) register number to use as the temporary
     * area when unwinding stack manipulation ops.
     *
     * @return {@code >= 0;} the first register to use
     */
    /*package*/ int getFirstTempStackReg() {
        /*
         * We use the register that is just past the deepest possible
         * stack element, plus one if the method is synchronized to
         * avoid overlapping with the synch register. We don't need to
         * do anything else special at this level, since later passes
         * will merely notice the highest register used by explicit
         * inspection.
         */
        int regCount = getNormalRegCount();
        return isSynchronized() ? regCount + 1 : regCount;
    }

    /**
     * Gets the label for the given special-purpose block. The given label
     * should be one of the static constants defined by this class.
     *
     * @param label {@code < 0;} the special label constant
     * @return {@code >= 0;} the actual label value to use
     */
    private int getSpecialLabel(int label) {
        /*
         * The label is bitwise-complemented so that mistakes where
         * LABEL is used instead of getSpecialLabel(LABEL) cause a
         * failure at block construction time, since negative labels
         * are illegal. 0..maxLabel (exclusive) are the original blocks and
         * maxLabel..(maxLabel + method.getCatches().size()) are reserved for exception handler
         * setup blocks (see getAvailableLabel(), exceptionSetupLabelAllocator).
         */
        return maxLabel + method.getCatches().size() + ~label;
    }

    /**
     * Gets the minimum label for unreserved use.
     *
     * @return {@code >= 0;} the minimum label
     */
    private int getMinimumUnreservedLabel() {
        /*
         * The labels below (maxLabel + method.getCatches().size() + SPECIAL_LABEL_COUNT) are
         * reserved for particular uses.
         */

        return maxLabel + method.getCatches().size() + SPECIAL_LABEL_COUNT;
    }

    /**
     * Gets an unreserved and available label.
     * Labels are distributed this way:
     * <ul>
     * <li>[0, maxLabel[ are the labels of the blocks directly
     * corresponding to the input bytecode.</li>
     * <li>[maxLabel, maxLabel + method.getCatches().size()[ are reserved for exception setup
     * blocks.</li>
     * <li>[maxLabel + method.getCatches().size(),
     * maxLabel + method.getCatches().size() + SPECIAL_LABEL_COUNT[ are reserved for special blocks,
     * ie param assignement, return and synch blocks.</li>
     * <li>[maxLabel method.getCatches().size() + SPECIAL_LABEL_COUNT, getAvailableLabel()[ assigned
     *  labels. Note that some
     * of the assigned labels may not be used any more if they were assigned to a block that was
     * deleted since.</li>
     * </ul>
     *
     * @return {@code >= 0;} an available label with the guaranty that all greater labels are
     * also available.
     */
    private int getAvailableLabel() {
        int candidate = getMinimumUnreservedLabel();

        for (BasicBlock bb : result) {
            int label = bb.getLabel();
            if (label >= candidate) {
                candidate = label + 1;
            }
        }

        return candidate;
    }

    /**
     * Gets whether the method being translated is synchronized.
     *
     * @return whether the method being translated is synchronized
     */
    private boolean isSynchronized() {
        int accessFlags = method.getAccessFlags();
        return (accessFlags & AccessFlags.ACC_SYNCHRONIZED) != 0;
    }

    /**
     * Gets whether the method being translated is static.
     *
     * @return whether the method being translated is static
     */
    private boolean isStatic() {
        int accessFlags = method.getAccessFlags();
        return (accessFlags & AccessFlags.ACC_STATIC) != 0;
    }

    /**
     * Gets the total number of registers used for "normal" purposes (i.e.,
     * for the straightforward translation from the original Java).
     *
     * @return {@code >= 0;} the total number of registers used
     */
    private int getNormalRegCount() {
        return maxLocals + method.getMaxStack();
    }

    /**
     * Gets the register spec to use to hold the object to synchronize on,
     * for a synchronized method.
     *
     * @return {@code non-null;} the register spec
     */
    private RegisterSpec getSynchReg() {
        /*
         * We use the register that is just past the deepest possible
         * stack element, with a minimum of v1 since v0 is what's
         * always used to hold the caught exception when unwinding. We
         * don't need to do anything else special at this level, since
         * later passes will merely notice the highest register used
         * by explicit inspection.
         */
        int reg = getNormalRegCount();
        return RegisterSpec.make((reg < 1) ? 1 : reg, Type.OBJECT);
    }

    /**
     * Searches {@link #result} for a block with the given label. Returns its
     * index if found, or returns {@code -1} if there is no such block.
     *
     * @param label the label to look for
     * @return {@code >= -1;} the index for the block with the given label or
     * {@code -1} if there is no such block
     */
    private int labelToResultIndex(int label) {
        int sz = result.size();
        for (int i = 0; i < sz; i++) {
            BasicBlock one = result.get(i);
            if (one.getLabel() == label) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Searches {@link #result} for a block with the given label. Returns it if
     * found, or throws an exception if there is no such block.
     *
     * @param label the label to look for
     * @return {@code non-null;} the block with the given label
     */
    private BasicBlock labelToBlock(int label) {
        int idx = labelToResultIndex(label);

        if (idx < 0) {
            throw new IllegalArgumentException("no such label " +
                    Hex.u2(label));
        }

        return result.get(idx);
    }

    /**
     * Adds a block to the output result.
     *
     * @param block {@code non-null;} the block to add
     * @param subroutines {@code non-null;} subroutine label list
     * as described in {@link Frame#getSubroutines}
     */
    private void addBlock(BasicBlock block, IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }

        result.add(block);
        subroutines.throwIfMutable();
        resultSubroutines.add(subroutines);
    }

    /**
     * Adds or replace a block in the output result. If this is a
     * replacement, then any extra blocks that got added with the
     * original get removed as a result of calling this method.
     *
     * @param block {@code non-null;} the block to add or replace
     * @param subroutines {@code non-null;} subroutine label list
     * as described in {@link Frame#getSubroutines}
     * @return {@code true} if the block was replaced or
     * {@code false} if it was added for the first time
     */
    private boolean addOrReplaceBlock(BasicBlock block, IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }

        int idx = labelToResultIndex(block.getLabel());
        boolean ret;

        if (idx < 0) {
            ret = false;
        } else {
            /*
             * We are replacing a pre-existing block, so find any
             * blocks that got added as part of the original and
             * remove those too. Such blocks are (possibly indirect)
             * successors of this block which are out of the range of
             * normally-translated blocks.
             */
            removeBlockAndSpecialSuccessors(idx);
            ret = true;
        }

        result.add(block);
        subroutines.throwIfMutable();
        resultSubroutines.add(subroutines);
        return ret;
    }

    /**
     * Adds or replaces a block in the output result. Do not delete
     * any successors.
     *
     * @param block {@code non-null;} the block to add or replace
     * @param subroutines {@code non-null;} subroutine label list
     * as described in {@link Frame#getSubroutines}
     * @return {@code true} if the block was replaced or
     * {@code false} if it was added for the first time
     */
    private boolean addOrReplaceBlockNoDelete(BasicBlock block,
            IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }

        int idx = labelToResultIndex(block.getLabel());
        boolean ret;

        if (idx < 0) {
            ret = false;
        } else {
            result.remove(idx);
            resultSubroutines.remove(idx);
            ret = true;
        }

        result.add(block);
        subroutines.throwIfMutable();
        resultSubroutines.add(subroutines);
        return ret;
    }

    /**
     * Helper for {@link #addOrReplaceBlock} which recursively removes
     * the given block and all blocks that are (direct and indirect)
     * successors of it whose labels indicate that they are not in the
     * normally-translated range.
     *
     * @param idx {@code non-null;} block to remove (etc.)
     */
    private void removeBlockAndSpecialSuccessors(int idx) {
        int minLabel = getMinimumUnreservedLabel();
        BasicBlock block = result.get(idx);
        IntList successors = block.getSuccessors();
        int sz = successors.size();

        result.remove(idx);
        resultSubroutines.remove(idx);

        for (int i = 0; i < sz; i++) {
            int label = successors.get(i);
            if (label >= minLabel) {
                idx = labelToResultIndex(label);
                if (idx < 0) {
                    throw new RuntimeException("Invalid label "
                            + Hex.u2(label));
                }
                removeBlockAndSpecialSuccessors(idx);
            }
        }
    }

    /**
     * Extracts the resulting {@link RopMethod} from the instance.
     *
     * @return {@code non-null;} the method object
     */
    private RopMethod getRopMethod() {

        // Construct the final list of blocks.

        int sz = result.size();
        BasicBlockList bbl = new BasicBlockList(sz);
        for (int i = 0; i < sz; i++) {
            bbl.set(i, result.get(i));
        }
        bbl.setImmutable();

        // Construct the method object to wrap it all up.

        /*
         * Note: The parameter assignment block is always the first
         * that should be executed, hence the second argument to the
         * constructor.
         */
        return new RopMethod(bbl, getSpecialLabel(PARAM_ASSIGNMENT));
    }

    /**
     * Does the conversion.
     */
    private void doit() {
        int[] workSet = Bits.makeBitSet(maxLabel);

        Bits.set(workSet, 0);
        addSetupBlocks();
        setFirstFrame();

        for (;;) {
            int offset = Bits.findFirst(workSet, 0);
            if (offset < 0) {
                break;
            }
            Bits.clear(workSet, offset);
            ByteBlock block = blocks.labelToBlock(offset);
            Frame frame = startFrames[offset];
            try {
                processBlock(block, frame, workSet);
            } catch (SimException ex) {
                ex.addContext("...while working on block " + Hex.u2(offset));
                throw ex;
            }
        }

        addReturnBlock();
        addSynchExceptionHandlerBlock();
        addExceptionSetupBlocks();

        if (hasSubroutines) {
            // Subroutines are very rare, so skip this step if it's n/a
            inlineSubroutines();
        }
    }

    /**
     * Sets up the first frame to contain all the incoming parameters in
     * locals.
     */
    private void setFirstFrame() {
        Prototype desc = method.getEffectiveDescriptor();
        startFrames[0].initializeWithParameters(desc.getParameterTypes());
        startFrames[0].setImmutable();
    }

    /**
     * Processes the given block.
     *
     * @param block {@code non-null;} block to process
     * @param frame {@code non-null;} start frame for the block
     * @param workSet {@code non-null;} bits representing work to do,
     * which this method may add to
     */
    private void processBlock(ByteBlock block, Frame frame, int[] workSet) {
        // Prepare the list of caught exceptions for this block.
        ByteCatchList catches = block.getCatches();
        machine.startBlock(catches.toRopCatchList());

        /*
         * Using a copy of the given frame, simulate each instruction,
         * calling into machine for each.
         */
        frame = frame.copy();
        sim.simulate(block, frame);
        frame.setImmutable();

        int extraBlockCount = machine.getExtraBlockCount();
        ArrayList<Insn> insns = machine.getInsns();
        int insnSz = insns.size();

        /*
         * Merge the frame into each possible non-exceptional
         * successor.
         */

        int catchSz = catches.size();
        IntList successors = block.getSuccessors();

        int startSuccessorIndex;

        Subroutine calledSubroutine = null;
        if (machine.hasJsr()) {
            /*
             * If this frame ends in a JSR, only merge our frame with
             * the subroutine start, not the subroutine's return target.
             */
            startSuccessorIndex = 1;

            int subroutineLabel = successors.get(1);

            if (subroutines[subroutineLabel] == null) {
                subroutines[subroutineLabel] =
                    new Subroutine (subroutineLabel);
            }

            subroutines[subroutineLabel].addCallerBlock(block.getLabel());

            calledSubroutine = subroutines[subroutineLabel];
        } else if (machine.hasRet()) {
            /*
             * This block ends in a ret, which means it's the final block
             * in some subroutine. Ultimately, this block will be copied
             * and inlined for each call and then disposed of.
             */

            ReturnAddress ra = machine.getReturnAddress();
            int subroutineLabel = ra.getSubroutineAddress();

            if (subroutines[subroutineLabel] == null) {
                subroutines[subroutineLabel]
                        = new Subroutine (subroutineLabel, block.getLabel());
            } else {
                subroutines[subroutineLabel].addRetBlock(block.getLabel());
            }

            successors = subroutines[subroutineLabel].getSuccessors();
            subroutines[subroutineLabel]
                    .mergeToSuccessors(frame, workSet);
            // Skip processing below since we just did it.
            startSuccessorIndex = successors.size();
        } else if (machine.wereCatchesUsed()) {
            /*
             * If there are catches, then the first successors
             * (which will either be all of them or all but the last one)
             * are catch targets.
             */
            startSuccessorIndex = catchSz;
        } else {
            startSuccessorIndex = 0;
        }

        int succSz = successors.size();
        for (int i = startSuccessorIndex; i < succSz;
             i++) {
            int succ = successors.get(i);
            try {
                mergeAndWorkAsNecessary(succ, block.getLabel(),
                        calledSubroutine, frame, workSet);
            } catch (SimException ex) {
                ex.addContext("...while merging to block " + Hex.u2(succ));
                throw ex;
            }
        }

        if ((succSz == 0) && machine.returns()) {
            /*
             * The block originally contained a return, but it has
             * been made to instead end with a goto, and we need to
             * tell it at this point that its sole successor is the
             * return block. This has to happen after the merge loop
             * above, since, at this point, the return block doesn't
             * actually exist; it gets synthesized at the end of
             * processing the original blocks.
             */
            successors = IntList.makeImmutable(getSpecialLabel(RETURN));
            succSz = 1;
        }

        int primarySucc;

        if (succSz == 0) {
            primarySucc = -1;
        } else {
            primarySucc = machine.getPrimarySuccessorIndex();
            if (primarySucc >= 0) {
                primarySucc = successors.get(primarySucc);
            }
        }

        /*
         * This variable is true only when the method is synchronized and
         * the block being processed can possibly throw an exception.
         */
        boolean synch = isSynchronized() && machine.canThrow();

        if (synch || (catchSz != 0)) {
            /*
             * Deal with exception handlers: Merge an exception-catch
             * frame into each possible exception handler, and
             * construct a new set of successors to point at the
             * exception handler setup blocks (which get synthesized
             * at the very end of processing).
             */
            boolean catchesAny = false;
            IntList newSucc = new IntList(succSz);
            for (int i = 0; i < catchSz; i++) {
                ByteCatchList.Item one = catches.get(i);
                CstType exceptionClass = one.getExceptionClass();
                int targ = one.getHandlerPc();

                catchesAny |= (exceptionClass == CstType.OBJECT);

                Frame f = frame.makeExceptionHandlerStartFrame(exceptionClass);

                try {
                    mergeAndWorkAsNecessary(targ, block.getLabel(),
                            null, f, workSet);
                } catch (SimException ex) {
                    ex.addContext("...while merging exception to block " +
                                  Hex.u2(targ));
                    throw ex;
                }

                /*
                 * Set up the exception handler type.
                 */
                CatchInfo handlers = catchInfos[targ];
                if (handlers == null) {
                    handlers = new CatchInfo();
                    catchInfos[targ] = handlers;
                }
                ExceptionHandlerSetup handler = handlers.getSetup(exceptionClass.getClassType());

                /*
                 * The synthesized exception setup block will have the label given by handler.
                 */
                newSucc.add(handler.getLabel());
            }

            if (synch && !catchesAny) {
                /*
                 * The method is synchronized and this block doesn't
                 * already have a catch-all handler, so add one to the
                 * end, both in the successors and in the throwing
                 * instruction(s) at the end of the block (which is where
                 * the caught classes live).
                 */
                newSucc.add(getSpecialLabel(SYNCH_CATCH_1));
                synchNeedsExceptionHandler = true;

                for (int i = insnSz - extraBlockCount - 1; i < insnSz; i++) {
                    Insn insn = insns.get(i);
                    if (insn.canThrow()) {
                        insn = insn.withAddedCatch(Type.OBJECT);
                        insns.set(i, insn);
                    }
                }
            }

            if (primarySucc >= 0) {
                newSucc.add(primarySucc);
            }

            newSucc.setImmutable();
            successors = newSucc;
        }

        // Construct the final resulting block(s), and store it (them).

        int primarySuccListIndex = successors.indexOf(primarySucc);

        /*
         * If there are any extra blocks, work backwards through the
         * list of instructions, adding single-instruction blocks, and
         * resetting the successors variables as appropriate.
         */
        for (/*extraBlockCount*/; extraBlockCount > 0; extraBlockCount--) {
            /*
             * Some of the blocks that the RopperMachine wants added
             * are for move-result insns, and these need goto insns as well.
             */
            Insn extraInsn = insns.get(--insnSz);
            boolean needsGoto
                    = extraInsn.getOpcode().getBranchingness()
                        == Rop.BRANCH_NONE;
            InsnList il = new InsnList(needsGoto ? 2 : 1);
            IntList extraBlockSuccessors = successors;

            il.set(0, extraInsn);

            if (needsGoto) {
                il.set(1, new PlainInsn(Rops.GOTO,
                        extraInsn.getPosition(), null,
                        RegisterSpecList.EMPTY));
                /*
                 * Obviously, this block won't be throwing an exception
                 * so it should only have one successor.
                 */
                extraBlockSuccessors = IntList.makeImmutable(primarySucc);
            }
            il.setImmutable();

            int label = getAvailableLabel();
            BasicBlock bb = new BasicBlock(label, il, extraBlockSuccessors,
                    primarySucc);
            // All of these extra blocks will be in the same subroutine
            addBlock(bb, frame.getSubroutines());

            successors = successors.mutableCopy();
            successors.set(primarySuccListIndex, label);
            successors.setImmutable();
            primarySucc = label;
        }

        Insn lastInsn = (insnSz == 0) ? null : insns.get(insnSz - 1);

        /*
         * Add a goto to the end of the block if it doesn't already
         * end with a branch, to maintain the invariant that all
         * blocks end with a branch of some sort or other. Note that
         * it is possible for there to be blocks for which no
         * instructions were ever output (e.g., only consist of pop*
         * in the original Java bytecode).
         */
        if ((lastInsn == null) ||
            (lastInsn.getOpcode().getBranchingness() == Rop.BRANCH_NONE)) {
            SourcePosition pos = (lastInsn == null) ? SourcePosition.NO_INFO :
                lastInsn.getPosition();
            insns.add(new PlainInsn(Rops.GOTO, pos, null,
                                    RegisterSpecList.EMPTY));
            insnSz++;
        }

        /*
         * Construct a block for the remaining instructions (which in
         * the usual case is all of them).
         */

        InsnList il = new InsnList(insnSz);
        for (int i = 0; i < insnSz; i++) {
            il.set(i, insns.get(i));
        }
        il.setImmutable();

        BasicBlock bb =
            new BasicBlock(block.getLabel(), il, successors, primarySucc);
        addOrReplaceBlock(bb, frame.getSubroutines());
    }

    /**
     * Helper for {@link #processBlock}, which merges frames and
     * adds to the work set, as necessary.
     *
     * @param label {@code >= 0;} label to work on
     * @param pred  predecessor label; must be {@code >= 0} when
     * {@code label} is a subroutine start block and calledSubroutine
     * is non-null. Otherwise, may be -1.
     * @param calledSubroutine {@code null-ok;} a Subroutine instance if
     * {@code label} is the first block in a subroutine.
     * @param frame {@code non-null;} new frame for the labelled block
     * @param workSet {@code non-null;} bits representing work to do,
     * which this method may add to
     */
    private void mergeAndWorkAsNecessary(int label, int pred,
            Subroutine calledSubroutine, Frame frame, int[] workSet) {
        Frame existing = startFrames[label];
        Frame merged;

        if (existing != null) {
            /*
             * Some other block also continues at this label. Merge
             * the frames, and re-set the bit in the work set if there
             * was a change.
             */
            if (calledSubroutine != null) {
                merged = existing.mergeWithSubroutineCaller(frame,
                        calledSubroutine.getStartBlock(), pred);
            } else {
                merged = existing.mergeWith(frame);
            }
            if (merged != existing) {
                startFrames[label] = merged;
                Bits.set(workSet, label);
            }
        } else {
            // This is the first time this label has been encountered.
            if (calledSubroutine != null) {
                startFrames[label]
                        = frame.makeNewSubroutineStartFrame(label, pred);
            } else {
                startFrames[label] = frame;
            }
            Bits.set(workSet, label);
        }
    }

    /**
     * Constructs and adds the blocks that perform setup for the rest of
     * the method. This includes a first block which merely contains
     * assignments from parameters to the same-numbered registers and
     * a possible second block which deals with synchronization.
     */
    private void addSetupBlocks() {
        LocalVariableList localVariables = method.getLocalVariables();
        SourcePosition pos = method.makeSourcePosistion(0);
        Prototype desc = method.getEffectiveDescriptor();
        StdTypeList params = desc.getParameterTypes();
        int sz = params.size();
        InsnList insns = new InsnList(sz + 1);
        int at = 0;

        for (int i = 0; i < sz; i++) {
            Type one = params.get(i);
            LocalVariableList.Item local =
                localVariables.pcAndIndexToLocal(0, at);
            RegisterSpec result = (local == null) ?
                RegisterSpec.make(at, one) :
                RegisterSpec.makeLocalOptional(at, one, local.getLocalItem());

            Insn insn = new PlainCstInsn(Rops.opMoveParam(one), pos, result,
                                         RegisterSpecList.EMPTY,
                                         CstInteger.make(at));
            insns.set(i, insn);
            at += one.getCategory();
        }

        insns.set(sz, new PlainInsn(Rops.GOTO, pos, null,
                                    RegisterSpecList.EMPTY));
        insns.setImmutable();

        boolean synch = isSynchronized();
        int label = synch ? getSpecialLabel(SYNCH_SETUP_1) : 0;
        BasicBlock bb =
            new BasicBlock(getSpecialLabel(PARAM_ASSIGNMENT), insns,
                           IntList.makeImmutable(label), label);
        addBlock(bb, IntList.EMPTY);

        if (synch) {
            RegisterSpec synchReg = getSynchReg();
            Insn insn;
            if (isStatic()) {
                insn = new ThrowingCstInsn(Rops.CONST_OBJECT, pos,
                                           RegisterSpecList.EMPTY,
                                           StdTypeList.EMPTY,
                                           method.getDefiningClass());
                insns = new InsnList(1);
                insns.set(0, insn);
            } else {
                insns = new InsnList(2);
                insn = new PlainCstInsn(Rops.MOVE_PARAM_OBJECT, pos,
                                        synchReg, RegisterSpecList.EMPTY,
                                        CstInteger.VALUE_0);
                insns.set(0, insn);
                insns.set(1, new PlainInsn(Rops.GOTO, pos, null,
                                           RegisterSpecList.EMPTY));
            }

            int label2 = getSpecialLabel(SYNCH_SETUP_2);
            insns.setImmutable();
            bb = new BasicBlock(label, insns,
                                IntList.makeImmutable(label2), label2);
            addBlock(bb, IntList.EMPTY);

            insns = new InsnList(isStatic() ? 2 : 1);

            if (isStatic()) {
                insns.set(0, new PlainInsn(Rops.opMoveResultPseudo(synchReg),
                        pos, synchReg, RegisterSpecList.EMPTY));
            }

            insn = new ThrowingInsn(Rops.MONITOR_ENTER, pos,
                                    RegisterSpecList.make(synchReg),
                                    StdTypeList.EMPTY);
            insns.set(isStatic() ? 1 :0, insn);
            insns.setImmutable();
            bb = new BasicBlock(label2, insns, IntList.makeImmutable(0), 0);
            addBlock(bb, IntList.EMPTY);
        }
    }

    /**
     * Constructs and adds the return block, if necessary. The return
     * block merely contains an appropriate {@code return}
     * instruction.
     */
    private void addReturnBlock() {
        Rop returnOp = machine.getReturnOp();

        if (returnOp == null) {
            /*
             * The method being converted never returns normally, so there's
             * no need for a return block.
             */
            return;
        }

        SourcePosition returnPos = machine.getReturnPosition();
        int label = getSpecialLabel(RETURN);

        if (isSynchronized()) {
            InsnList insns = new InsnList(1);
            Insn insn = new ThrowingInsn(Rops.MONITOR_EXIT, returnPos,
                                         RegisterSpecList.make(getSynchReg()),
                                         StdTypeList.EMPTY);
            insns.set(0, insn);
            insns.setImmutable();

            int nextLabel = getSpecialLabel(SYNCH_RETURN);
            BasicBlock bb =
                new BasicBlock(label, insns,
                               IntList.makeImmutable(nextLabel), nextLabel);
            addBlock(bb, IntList.EMPTY);

            label = nextLabel;
        }

        InsnList insns = new InsnList(1);
        TypeList sourceTypes = returnOp.getSources();
        RegisterSpecList sources;

        if (sourceTypes.size() == 0) {
            sources = RegisterSpecList.EMPTY;
        } else {
            RegisterSpec source = RegisterSpec.make(0, sourceTypes.getType(0));
            sources = RegisterSpecList.make(source);
        }

        Insn insn = new PlainInsn(returnOp, returnPos, null, sources);
        insns.set(0, insn);
        insns.setImmutable();

        BasicBlock bb = new BasicBlock(label, insns, IntList.EMPTY, -1);
        addBlock(bb, IntList.EMPTY);
    }

    /**
     * Constructs and adds, if necessary, the catch-all exception handler
     * block to deal with unwinding the lock taken on entry to a synchronized
     * method.
     */
    private void addSynchExceptionHandlerBlock() {
        if (!synchNeedsExceptionHandler) {
            /*
             * The method being converted either isn't synchronized or
             * can't possibly throw exceptions in its main body, so
             * there's no need for a synchronized method exception
             * handler.
             */
            return;
        }

        SourcePosition pos = method.makeSourcePosistion(0);
        RegisterSpec exReg = RegisterSpec.make(0, Type.THROWABLE);
        BasicBlock bb;
        Insn insn;

        InsnList insns = new InsnList(2);
        insn = new PlainInsn(Rops.opMoveException(Type.THROWABLE), pos,
                             exReg, RegisterSpecList.EMPTY);
        insns.set(0, insn);
        insn = new ThrowingInsn(Rops.MONITOR_EXIT, pos,
                                RegisterSpecList.make(getSynchReg()),
                                StdTypeList.EMPTY);
        insns.set(1, insn);
        insns.setImmutable();

        int label2 = getSpecialLabel(SYNCH_CATCH_2);
        bb = new BasicBlock(getSpecialLabel(SYNCH_CATCH_1), insns,
                            IntList.makeImmutable(label2), label2);
        addBlock(bb, IntList.EMPTY);

        insns = new InsnList(1);
        insn = new ThrowingInsn(Rops.THROW, pos,
                                RegisterSpecList.make(exReg),
                                StdTypeList.EMPTY);
        insns.set(0, insn);
        insns.setImmutable();

        bb = new BasicBlock(label2, insns, IntList.EMPTY, -1);
        addBlock(bb, IntList.EMPTY);
    }

    /**
     * Creates the exception handler setup blocks. "maxLocals"
     * below is because that's the register number corresponding
     * to the sole element on a one-deep stack (which is the
     * situation at the start of an exception handler block).
     */
    private void addExceptionSetupBlocks() {

        int len = catchInfos.length;
        for (int i = 0; i < len; i++) {
            CatchInfo catches = catchInfos[i];
            if (catches != null) {
                for (ExceptionHandlerSetup one : catches.getSetups()) {
                    Insn proto = labelToBlock(i).getFirstInsn();
                    SourcePosition pos = proto.getPosition();
                    InsnList il = new InsnList(2);

                    Insn insn = new PlainInsn(Rops.opMoveException(one.getCaughtType()),
                            pos,
                            RegisterSpec.make(maxLocals, one.getCaughtType()),
                            RegisterSpecList.EMPTY);
                    il.set(0, insn);

                    insn = new PlainInsn(Rops.GOTO, pos, null,
                            RegisterSpecList.EMPTY);
                    il.set(1, insn);
                    il.setImmutable();

                    BasicBlock bb = new BasicBlock(one.getLabel(),
                            il,
                            IntList.makeImmutable(i),
                            i);
                    addBlock(bb, startFrames[i].getSubroutines());
                }
            }
        }
    }

    /**
     * Checks to see if the basic block is a subroutine caller block.
     *
     * @param bb {@code non-null;} the basic block in question
     * @return true if this block calls a subroutine
     */
    private boolean isSubroutineCaller(BasicBlock bb) {
        IntList successors = bb.getSuccessors();
        if (successors.size() < 2) return false;

        int subLabel = successors.get(1);

        return (subLabel < subroutines.length)
                && (subroutines[subLabel] != null);
    }

    /**
     * Inlines any subroutine calls.
     */
    private void inlineSubroutines() {
        final IntList reachableSubroutineCallerLabels = new IntList(4);

        /*
         * Compile a list of all subroutine calls reachable
         * through the normal (non-subroutine) flow.  We do this first, since
         * we'll be affecting the call flow as we go.
         *
         * Start at label 0 --  the param assignment block has nothing for us
         */
        forEachNonSubBlockDepthFirst(0, new BasicBlock.Visitor() {
            @Override
            public void visitBlock(BasicBlock b) {
                if (isSubroutineCaller(b)) {
                    reachableSubroutineCallerLabels.add(b.getLabel());
                }
            }
        });

        /*
         * Convert the resultSubroutines list, indexed by block index,
         * to a label-to-subroutines mapping used by the inliner.
         */
        int largestAllocedLabel = getAvailableLabel();
        ArrayList<IntList> labelToSubroutines
                = new ArrayList<IntList>(largestAllocedLabel);
        for (int i = 0; i < largestAllocedLabel; i++) {
            labelToSubroutines.add(null);
        }

        for (int i = 0; i < result.size(); i++) {
            BasicBlock b = result.get(i);
            if (b == null) {
                continue;
            }
            IntList subroutineList = resultSubroutines.get(i);
            labelToSubroutines.set(b.getLabel(), subroutineList);
        }

        /*
         * Inline all reachable subroutines.
         * Inner subroutines will be inlined as they are encountered.
         */
        int sz = reachableSubroutineCallerLabels.size();
        for (int i = 0 ; i < sz ; i++) {
            int label = reachableSubroutineCallerLabels.get(i);
            new SubroutineInliner(
                    new LabelAllocator(getAvailableLabel()),
                    labelToSubroutines)
                    .inlineSubroutineCalledFrom(labelToBlock(label));
        }

        // Now find the blocks that aren't reachable and remove them
        deleteUnreachableBlocks();
    }

    /**
     * Deletes all blocks that cannot be reached. This is run to delete
     * original subroutine blocks after subroutine inlining.
     */
    private void deleteUnreachableBlocks() {
        final IntList reachableLabels = new IntList(result.size());

        // subroutine inlining is done now and we won't update this list here
        resultSubroutines.clear();

        forEachNonSubBlockDepthFirst(getSpecialLabel(PARAM_ASSIGNMENT),
                new BasicBlock.Visitor() {

            @Override
            public void visitBlock(BasicBlock b) {
                reachableLabels.add(b.getLabel());
            }
        });

        reachableLabels.sort();

        for (int i = result.size() - 1 ; i >= 0 ; i--) {
            if (reachableLabels.indexOf(result.get(i).getLabel()) < 0) {
                result.remove(i);
                // unnecessary here really, since subroutine inlining is done
                //resultSubroutines.remove(i);
            }
        }
    }

    /**
     * Allocates labels, without requiring previously allocated labels
     * to have been added to the blocks list.
     */
    private static class LabelAllocator {
        int nextAvailableLabel;

        /**
         * @param startLabel available label to start allocating from
         */
        LabelAllocator(int startLabel) {
            nextAvailableLabel = startLabel;
        }

        /**
         * @return next available label
         */
        int getNextLabel() {
            return nextAvailableLabel++;
        }
    }

    /**
     * Allocates labels for exception setup blocks.
     */
    private class ExceptionSetupLabelAllocator extends LabelAllocator {
        int maxSetupLabel;

        ExceptionSetupLabelAllocator() {
            super(maxLabel);
            maxSetupLabel = maxLabel + method.getCatches().size();
        }

        @Override
        int getNextLabel() {
            if (nextAvailableLabel >= maxSetupLabel) {
                throw new IndexOutOfBoundsException();
            }
            return nextAvailableLabel ++;
        }
    }

    /**
     * Inlines a subroutine. Start by calling
     * {@link #inlineSubroutineCalledFrom}.
     */
    private class SubroutineInliner {
        /**
         * maps original label to the label that will be used by the
         * inlined version
         */
        private final HashMap<Integer, Integer> origLabelToCopiedLabel;

        /** set of original labels that need to be copied */
        private final BitSet workList;

        /** the label of the original start block for this subroutine */
        private int subroutineStart;

        /** the label of the ultimate return block */
        private int subroutineSuccessor;

        /** used for generating new labels for copied blocks */
        private final LabelAllocator labelAllocator;

        /**
         * A mapping, indexed by label, to subroutine nesting list.
         * The subroutine nest list is as returned by
         * {@link Frame#getSubroutines}.
         */
        private final ArrayList<IntList> labelToSubroutines;

        SubroutineInliner(final LabelAllocator labelAllocator,
                ArrayList<IntList> labelToSubroutines) {
            origLabelToCopiedLabel = new HashMap<Integer, Integer>();

            workList = new BitSet(maxLabel);

            this.labelAllocator = labelAllocator;
            this.labelToSubroutines = labelToSubroutines;
        }

        /**
         * Inlines a subroutine.
         *
         * @param b block where {@code jsr} occurred in the original bytecode
         */
        void inlineSubroutineCalledFrom(final BasicBlock b) {
            /*
             * The 0th successor of a subroutine caller block is where
             * the subroutine should return to. The 1st successor is
             * the start block of the subroutine.
             */
            subroutineSuccessor = b.getSuccessors().get(0);
            subroutineStart = b.getSuccessors().get(1);

            /*
             * This allocates an initial label and adds the first
             * block to the worklist.
             */
            int newSubStartLabel = mapOrAllocateLabel(subroutineStart);

            for (int label = workList.nextSetBit(0); label >= 0;
                 label = workList.nextSetBit(0)) {
                workList.clear(label);
                int newLabel = origLabelToCopiedLabel.get(label);

                copyBlock(label, newLabel);

                if (isSubroutineCaller(labelToBlock(label))) {
                    new SubroutineInliner(labelAllocator, labelToSubroutines)
                        .inlineSubroutineCalledFrom(labelToBlock(newLabel));
                }
            }

            /*
             * Replace the original caller block, since we now have a
             * new successor
             */

            addOrReplaceBlockNoDelete(
                new BasicBlock(b.getLabel(), b.getInsns(),
                    IntList.makeImmutable (newSubStartLabel),
                            newSubStartLabel),
                labelToSubroutines.get(b.getLabel()));
        }

        /**
         * Copies a basic block, mapping its successors along the way.
         *
         * @param origLabel original block label
         * @param newLabel label that the new block should have
         */
        private void copyBlock(int origLabel, int newLabel) {

            BasicBlock origBlock = labelToBlock(origLabel);

            final IntList origSuccessors = origBlock.getSuccessors();
            IntList successors;
            int primarySuccessor = -1;
            Subroutine subroutine;

            if (isSubroutineCaller(origBlock)) {
                /*
                 * A subroutine call inside a subroutine call.
                 * Set up so we can recurse. The caller block should have
                 * it's first successor be a copied block that will be
                 * the subroutine's return point. It's second successor will
                 * be copied when we recurse, and remains as the original
                 * label of the start of the inner subroutine.
                 */

                successors = IntList.makeImmutable(
                        mapOrAllocateLabel(origSuccessors.get(0)),
                        origSuccessors.get(1));
                // primary successor will be set when this block is replaced
            } else if (null
                    != (subroutine = subroutineFromRetBlock(origLabel))) {
                /*
                 * this is a ret block -- its successor
                 * should be subroutineSuccessor
                 */

                // Sanity check
                if (subroutine.startBlock != subroutineStart) {
                    throw new RuntimeException (
                            "ret instruction returns to label "
                            + Hex.u2 (subroutine.startBlock)
                            + " expected: " + Hex.u2(subroutineStart));
                }

                successors = IntList.makeImmutable(subroutineSuccessor);
                primarySuccessor = subroutineSuccessor;
            } else {
                // Map all the successor labels

                int origPrimary = origBlock.getPrimarySuccessor();
                int sz = origSuccessors.size();

                successors = new IntList(sz);

                for (int i = 0 ; i < sz ; i++) {
                    int origSuccLabel = origSuccessors.get(i);
                    int newSuccLabel =  mapOrAllocateLabel(origSuccLabel);

                    successors.add(newSuccLabel);

                    if (origPrimary == origSuccLabel) {
                        primarySuccessor = newSuccLabel;
                    }
                }

                successors.setImmutable();
            }

            addBlock (
                new BasicBlock(newLabel,
                    filterMoveReturnAddressInsns(origBlock.getInsns()),
                    successors, primarySuccessor),
                    labelToSubroutines.get(newLabel));
        }

        /**
         * Checks to see if a specified label is involved in a specified
         * subroutine.
         *
         * @param label {@code >= 0;} a basic block label
         * @param subroutineStart {@code >= 0;} a subroutine as identified
         * by the label of its start block
         * @return true if the block is dominated by the subroutine call
         */
        private boolean involvedInSubroutine(int label, int subroutineStart) {
            IntList subroutinesList = labelToSubroutines.get(label);
            return (subroutinesList != null && subroutinesList.size() > 0
                    && subroutinesList.top() == subroutineStart);
        }

        /**
         * Maps the label of a pre-copied block to the label of the inlined
         * block, allocating a new label and adding it to the worklist
         * if necessary.  If the origLabel is a "special" label, it
         * is returned exactly and not scheduled for duplication: copying
         * never proceeds past a special label, which likely is the function
         * return block or an immediate predecessor.
         *
         * @param origLabel label of original, pre-copied block
         * @return label for new, inlined block
         */
        private int mapOrAllocateLabel(int origLabel) {
            int resultLabel;
            Integer mappedLabel = origLabelToCopiedLabel.get(origLabel);

            if (mappedLabel != null) {
                resultLabel = mappedLabel;
            } else if (!involvedInSubroutine(origLabel,subroutineStart)) {
                /*
                 * A subroutine has ended by some means other than a "ret"
                 * (which really means a throw caught later).
                 */
                resultLabel = origLabel;
            } else {
                resultLabel = labelAllocator.getNextLabel();
                workList.set(origLabel);
                origLabelToCopiedLabel.put(origLabel, resultLabel);

                // The new label has the same frame as the original label
                while (labelToSubroutines.size() <= resultLabel) {
                    labelToSubroutines.add(null);
                }
                labelToSubroutines.set(resultLabel,
                        labelToSubroutines.get(origLabel));
            }

            return resultLabel;
        }
    }

    /**
     * Finds a {@code Subroutine} that is returned from by a {@code ret} in
     * a given block.
     *
     * @param label A block that originally contained a {@code ret} instruction
     * @return {@code null-ok;} found subroutine or {@code null} if none
     * was found
     */
    private Subroutine subroutineFromRetBlock(int label) {
        for (int i = subroutines.length - 1 ; i >= 0 ; i--) {
            if (subroutines[i] != null) {
                Subroutine subroutine = subroutines[i];

                if (subroutine.retBlocks.get(label)) {
                    return subroutine;
                }
            }
        }

        return null;
    }


    /**
     * Removes all {@code move-return-address} instructions, returning a new
     * {@code InsnList} if necessary. The {@code move-return-address}
     * insns are dead code after subroutines have been inlined.
     *
     * @param insns {@code InsnList} that may contain
     * {@code move-return-address} insns
     * @return {@code InsnList} with {@code move-return-address} removed
     */
    private InsnList filterMoveReturnAddressInsns(InsnList insns) {
        int sz;
        int newSz = 0;

        // First see if we need to filter, and if so what the new size will be
        sz = insns.size();
        for (int i = 0; i < sz; i++) {
            if (insns.get(i).getOpcode() != Rops.MOVE_RETURN_ADDRESS) {
                newSz++;
            }
        }

        if (newSz == sz) {
            return insns;
        }

        // Make a new list without the MOVE_RETURN_ADDRESS insns
        InsnList newInsns = new InsnList(newSz);

        int newIndex = 0;
        for (int i = 0; i < sz; i++) {
            Insn insn = insns.get(i);
            if (insn.getOpcode() != Rops.MOVE_RETURN_ADDRESS) {
                newInsns.set(newIndex++, insn);
            }
        }

        newInsns.setImmutable();
        return newInsns;
    }

    /**
     * Visits each non-subroutine block once in depth-first successor order.
     *
     * @param firstLabel label of start block
     * @param v callback interface
     */
    private void forEachNonSubBlockDepthFirst(int firstLabel,
            BasicBlock.Visitor v) {
        forEachNonSubBlockDepthFirst0(labelToBlock(firstLabel),
                v, new BitSet(maxLabel));
    }

    /**
     * Visits each block once in depth-first successor order, ignoring
     * {@code jsr} targets. Worker for {@link #forEachNonSubBlockDepthFirst}.
     *
     * @param next next block to visit
     * @param v callback interface
     * @param visited set of blocks already visited
     */
    private void forEachNonSubBlockDepthFirst0(
            BasicBlock next, BasicBlock.Visitor v, BitSet visited) {
        v.visitBlock(next);
        visited.set(next.getLabel());

        IntList successors = next.getSuccessors();
        int sz = successors.size();

        for (int i = 0; i < sz; i++) {
            int succ = successors.get(i);

            if (visited.get(succ)) {
                continue;
            }

            if (isSubroutineCaller(next) && i > 0) {
                // ignore jsr targets
                continue;
            }

            /*
             * Ignore missing labels: they're successors of
             * subroutines that never invoke a ret.
             */
            int idx = labelToResultIndex(succ);
            if (idx >= 0) {
                forEachNonSubBlockDepthFirst0(result.get(idx), v, visited);
            }
        }
    }
}

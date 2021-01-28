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

import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.io.Opcodes;
import external.com.android.dx.rop.code.BasicBlock;
import external.com.android.dx.rop.code.BasicBlockList;
import external.com.android.dx.rop.code.FillArrayDataInsn;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.InvokePolymorphicInsn;
import external.com.android.dx.rop.code.LocalVariableInfo;
import external.com.android.dx.rop.code.PlainCstInsn;
import external.com.android.dx.rop.code.PlainInsn;
import external.com.android.dx.rop.code.RegOps;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.RegisterSpecSet;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.code.SwitchInsn;
import external.com.android.dx.rop.code.ThrowingCstInsn;
import external.com.android.dx.rop.code.ThrowingInsn;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.util.Bits;
import external.com.android.dx.util.IntList;
import java.util.ArrayList;

/**
 * Translator from {@link RopMethod} to {@link DalvCode}. The {@link
 * #translate} method is the thing to call on this class.
 */
public final class RopTranslator {
    /** {@code non-null;} options for dex output */
    private final DexOptions dexOptions;

    /** {@code non-null;} method to translate */
    private final RopMethod method;

    /**
     * how much position info to preserve; one of the static
     * constants in {@link PositionList}
     */
    private final int positionInfo;

    /** {@code null-ok;} local variable info to use */
    private final LocalVariableInfo locals;

    /** {@code non-null;} container for all the address objects for the method */
    private final BlockAddresses addresses;

    /** {@code non-null;} list of output instructions in-progress */
    private final OutputCollector output;

    /** {@code non-null;} visitor to use during translation */
    private final TranslationVisitor translationVisitor;

    /** {@code >= 0;} register count for the method */
    private final int regCount;

    /** {@code null-ok;} block output order; becomes non-null in {@link #pickOrder} */
    private int[] order;

    /** size, in register units, of all the parameters to this method */
    private final int paramSize;

    /**
     * true if the parameters to this method happen to be in proper order
     * at the end of the frame (as the optimizer emits them)
     */
    private final boolean paramsAreInOrder;

    /**
     * Translates a {@link RopMethod}. This may modify the given
     * input.
     *
     * @param method {@code non-null;} the original method
     * @param positionInfo how much position info to preserve; one of the
     * static constants in {@link PositionList}
     * @param locals {@code null-ok;} local variable information to use
     * @param paramSize size, in register units, of all the parameters to
     * this method
     * @param dexOptions {@code non-null;} options for dex output
     * @return {@code non-null;} the translated version
     */
    public static DalvCode translate(RopMethod method, int positionInfo,
            LocalVariableInfo locals, int paramSize, DexOptions dexOptions) {
        RopTranslator translator =
            new RopTranslator(method, positionInfo, locals, paramSize, dexOptions);
        return translator.translateAndGetResult();
    }

    /**
     * Constructs an instance. This method is private. Use {@link #translate}.
     *
     * @param method {@code non-null;} the original method
     * @param positionInfo how much position info to preserve; one of the
     * static constants in {@link PositionList}
     * @param locals {@code null-ok;} local variable information to use
     * @param paramSize size, in register units, of all the parameters to
     * this method
     * @param dexOptions {@code non-null;} options for dex output
     */
    private RopTranslator(RopMethod method, int positionInfo, LocalVariableInfo locals,
            int paramSize, DexOptions dexOptions) {
        this.dexOptions = dexOptions;
        this.method = method;
        this.positionInfo = positionInfo;
        this.locals = locals;
        this.addresses = new BlockAddresses(method);
        this.paramSize = paramSize;
        this.order = null;
        this.paramsAreInOrder = calculateParamsAreInOrder(method, paramSize);

        BasicBlockList blocks = method.getBlocks();
        int bsz = blocks.size();

        /*
         * Max possible instructions includes three code address
         * objects per basic block (to the first and last instruction,
         * and just past the end of the block), and the possibility of
         * an extra goto at the end of each basic block.
         */
        int maxInsns = (bsz * 3) + blocks.getInstructionCount();

        if (locals != null) {
            /*
             * If we're tracking locals, then there's could be another
             * extra instruction per block (for the locals state at the
             * start of the block) as well as one for each interblock
             * local introduction.
             */
            maxInsns += bsz + locals.getAssignmentCount();
        }

        /*
         * If params are not in order, we will need register space
         * for them before this is all over...
         */
        this.regCount = blocks.getRegCount()
                + (paramsAreInOrder ? 0 : this.paramSize);

        this.output = new OutputCollector(dexOptions, maxInsns, bsz * 3, regCount, paramSize);

        if (locals != null) {
            this.translationVisitor =
                new LocalVariableAwareTranslationVisitor(output, locals);
        } else {
            this.translationVisitor = new TranslationVisitor(output);
        }
    }

    /**
     * Checks to see if the move-param instructions that occur in this
     * method happen to slot the params in an order at the top of the
     * stack frame that matches dalvik's calling conventions. This will
     * alway result in "true" for methods that have run through the
     * SSA optimizer.
     *
     * @param paramSize size, in register units, of all the parameters
     * to this method
     */
    private static boolean calculateParamsAreInOrder(RopMethod method,
            final int paramSize) {
        final boolean[] paramsAreInOrder = { true };
        final int initialRegCount = method.getBlocks().getRegCount();

        /*
         * We almost could just check the first block here, but the
         * {@code cf} layer will put in a second move-param in a
         * subsequent block in the case of synchronized methods.
         */
        method.getBlocks().forEachInsn(new Insn.BaseVisitor() {
            @Override
            public void visitPlainCstInsn(PlainCstInsn insn) {
                if (insn.getOpcode().getOpcode()== RegOps.MOVE_PARAM) {
                    int param =
                        ((CstInteger) insn.getConstant()).getValue();

                    paramsAreInOrder[0] = paramsAreInOrder[0]
                            && ((initialRegCount - paramSize + param)
                                == insn.getResult().getReg());
                }
            }
        });

        return paramsAreInOrder[0];
    }

    /**
     * Does the translation and returns the result.
     *
     * @return {@code non-null;} the result
     */
    private DalvCode translateAndGetResult() {
        pickOrder();
        outputInstructions();

        StdCatchBuilder catches =
            new StdCatchBuilder(method, order, addresses);

        return new DalvCode(positionInfo, output.getFinisher(), catches);
    }

    /**
     * Performs initial creation of output instructions based on the
     * original blocks.
     */
    private void outputInstructions() {
        BasicBlockList blocks = method.getBlocks();
        int[] order = this.order;
        int len = order.length;

        // Process the blocks in output order.
        for (int i = 0; i < len; i++) {
            int nextI = i + 1;
            int nextLabel = (nextI == order.length) ? -1 : order[nextI];
            outputBlock(blocks.labelToBlock(order[i]), nextLabel);
        }
    }

    /**
     * Helper for {@link #outputInstructions}, which does the processing
     * and output of one block.
     *
     * @param block {@code non-null;} the block to process and output
     * @param nextLabel {@code >= -1;} the next block that will be processed, or
     * {@code -1} if there is no next block
     */
    private void outputBlock(BasicBlock block, int nextLabel) {
        // Append the code address for this block.
        CodeAddress startAddress = addresses.getStart(block);
        output.add(startAddress);

        // Append the local variable state for the block.
        if (locals != null) {
            RegisterSpecSet starts = locals.getStarts(block);
            output.add(new LocalSnapshot(startAddress.getPosition(),
                                         starts));
        }

        /*
         * Choose and append an output instruction for each original
         * instruction.
         */
        translationVisitor.setBlock(block, addresses.getLast(block));
        block.getInsns().forEach(translationVisitor);

        // Insert the block end code address.
        output.add(addresses.getEnd(block));

        // Set up for end-of-block activities.

        int succ = block.getPrimarySuccessor();
        Insn lastInsn = block.getLastInsn();

        /*
         * Check for (and possibly correct for) a non-optimal choice of
         * which block will get output next.
         */

        if ((succ >= 0) && (succ != nextLabel)) {
            /*
             * The block has a "primary successor" and that primary
             * successor isn't the next block to be output.
             */
            Rop lastRop = lastInsn.getOpcode();
            if ((lastRop.getBranchingness() == Rop.BRANCH_IF) &&
                    (block.getSecondarySuccessor() == nextLabel)) {
                /*
                 * The block ends with an "if" of some sort, and its
                 * secondary successor (the "then") is in fact the
                 * next block to output. So, reverse the sense of
                 * the test, so that we can just emit the next block
                 * without an interstitial goto.
                 */
                output.reverseBranch(1, addresses.getStart(succ));
            } else {
                /*
                 * Our only recourse is to add a goto here to get the
                 * flow to be correct.
                 */
                TargetInsn insn =
                    new TargetInsn(Dops.GOTO, lastInsn.getPosition(),
                            RegisterSpecList.EMPTY,
                            addresses.getStart(succ));
                output.add(insn);
            }
        }
    }

    /**
     * Picks an order for the blocks by doing "trace" analysis.
     */
    private void pickOrder() {
        BasicBlockList blocks = method.getBlocks();
        int sz = blocks.size();
        int maxLabel = blocks.getMaxLabel();
        int[] workSet = Bits.makeBitSet(maxLabel);
        int[] tracebackSet = Bits.makeBitSet(maxLabel);

        for (int i = 0; i < sz; i++) {
            BasicBlock one = blocks.get(i);
            Bits.set(workSet, one.getLabel());
        }

        int[] order = new int[sz];
        int at = 0;

        /*
         * Starting with the designated "first label" (that is, the
         * first block of the method), add that label to the order,
         * and then pick its first as-yet unordered successor to
         * immediately follow it, giving top priority to the primary
         * (aka default) successor (if any). Keep following successors
         * until the trace runs out of possibilities. Then, continue
         * by finding an unordered chain containing the first as-yet
         * unordered block, and adding it to the order, and so on.
         */
        for (int label = method.getFirstLabel();
             label != -1;
             label = Bits.findFirst(workSet, 0)) {

            /*
             * Attempt to trace backward from the chosen block to an
             * as-yet unordered predecessor which lists the chosen
             * block as its primary successor, and so on, until we
             * fail to find such an unordered predecessor. Start the
             * trace with that block. Note that the first block in the
             * method has no predecessors, so in that case this loop
             * will simply terminate with zero iterations and without
             * picking a new starter block.
             */
            traceBack:
            for (;;) {
                IntList preds = method.labelToPredecessors(label);
                int psz = preds.size();

                for (int i = 0; i < psz; i++) {
                    int predLabel = preds.get(i);

                    if (Bits.get(tracebackSet, predLabel)) {
                        /*
                         * We found a predecessor loop; stop tracing back
                         * from here.
                         */
                        break;
                    }

                    if (!Bits.get(workSet, predLabel)) {
                        // This one's already ordered.
                        continue;
                    }

                    BasicBlock pred = blocks.labelToBlock(predLabel);
                    if (pred.getPrimarySuccessor() == label) {
                        // Found one!
                        label = predLabel;
                        Bits.set(tracebackSet, label);
                        continue traceBack;
                    }
                }

                // Failed to find a better block to start the trace.
                break;
            }

            /*
             * Trace a path from the chosen block to one of its
             * unordered successors (hopefully the primary), and so
             * on, until we run out of unordered successors.
             */
            while (label != -1) {
                Bits.clear(workSet, label);
                Bits.clear(tracebackSet, label);
                order[at] = label;
                at++;

                BasicBlock one = blocks.labelToBlock(label);
                BasicBlock preferredBlock = blocks.preferredSuccessorOf(one);

                if (preferredBlock == null) {
                    break;
                }

                int preferred = preferredBlock.getLabel();
                int primary = one.getPrimarySuccessor();

                if (Bits.get(workSet, preferred)) {
                    /*
                     * Order the current block's preferred successor
                     * next, as it has yet to be scheduled.
                     */
                    label = preferred;
                } else if ((primary != preferred) && (primary >= 0)
                        && Bits.get(workSet, primary)) {
                    /*
                     * The primary is available, so use that.
                     */
                    label = primary;
                } else {
                    /*
                     * There's no obvious candidate, so pick the first
                     * one that's available, if any.
                     */
                    IntList successors = one.getSuccessors();
                    int ssz = successors.size();
                    label = -1;
                    for (int i = 0; i < ssz; i++) {
                        int candidate = successors.get(i);
                        if (Bits.get(workSet, candidate)) {
                            label = candidate;
                            break;
                        }
                    }
                }
            }
        }

        if (at != sz) {
            // There was a duplicate block label.
            throw new RuntimeException("shouldn't happen");
        }

        this.order = order;
    }

    /**
     * Gets the complete register list (result and sources) out of a
     * given rop instruction. For insns that are commutative, have
     * two register sources, and have a source equal to the result,
     * place that source first.
     *
     * @param insn {@code non-null;} instruction in question
     * @return {@code non-null;} the instruction's complete register list
     */
    private static RegisterSpecList getRegs(Insn insn) {
        return getRegs(insn, insn.getResult());
    }

    /**
     * Gets the complete register list (result and sources) out of a
     * given rop instruction. For insns that are commutative, have
     * two register sources, and have a source equal to the result,
     * place that source first.
     *
     * @param insn {@code non-null;} instruction in question
     * @param resultReg {@code null-ok;} the real result to use (ignore the insn's)
     * @return {@code non-null;} the instruction's complete register list
     */
    private static RegisterSpecList getRegs(Insn insn,
            RegisterSpec resultReg) {
        RegisterSpecList regs = insn.getSources();

        if (insn.getOpcode().isCommutative()
                && (regs.size() == 2)
                && (resultReg.getReg() == regs.get(1).getReg())) {

            /*
             * For commutative ops which have two register sources,
             * if the second source is the same register as the result,
             * swap the sources so that an opcode of form 12x can be selected
             * instead of one of form 23x
             */

            regs = RegisterSpecList.make(regs.get(1), regs.get(0));
        }

        if (resultReg == null) {
            return regs;
        }

        return regs.withFirst(resultReg);
    }

    /**
     * Instruction visitor class for doing the instruction translation per se.
     */
    private class TranslationVisitor implements Insn.Visitor {
        /** {@code non-null;} list of output instructions in-progress */
        private final OutputCollector output;

        /** {@code non-null;} basic block being worked on */
        private BasicBlock block;

        /**
         * {@code null-ok;} code address for the salient last instruction of the
         * block (used before switches and throwing instructions)
         */
        private CodeAddress lastAddress;

        /**
         * Constructs an instance.
         *
         * @param output {@code non-null;} destination for instruction output
         */
        public TranslationVisitor(OutputCollector output) {
            this.output = output;
        }

        /**
         * Sets the block currently being worked on.
         *
         * @param block {@code non-null;} the block
         * @param lastAddress {@code non-null;} code address for the salient
         * last instruction of the block
         */
        public void setBlock(BasicBlock block, CodeAddress lastAddress) {
            this.block = block;
            this.lastAddress = lastAddress;
        }

        /** {@inheritDoc} */
        @Override
        public void visitPlainInsn(PlainInsn insn) {
            Rop rop = insn.getOpcode();
            if (rop.getOpcode() == RegOps.MARK_LOCAL) {
                /*
                 * Ignore these. They're dealt with by
                 * the LocalVariableAwareTranslationVisitor
                 */
                return;
            }
            if (rop.getOpcode() == RegOps.MOVE_RESULT_PSEUDO) {
                // These get skipped
                return;
            }

            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            DalvInsn di;

            switch (rop.getBranchingness()) {
                case Rop.BRANCH_NONE:
                case Rop.BRANCH_RETURN:
                case Rop.BRANCH_THROW: {
                    di = new SimpleInsn(opcode, pos, getRegs(insn));
                    break;
                }
                case Rop.BRANCH_GOTO: {
                    /*
                     * Code in the main translation loop will emit a
                     * goto if necessary (if the branch isn't to the
                     * immediately subsequent block).
                     */
                    return;
                }
                case Rop.BRANCH_IF: {
                    int target = block.getSuccessors().get(1);
                    di = new TargetInsn(opcode, pos, getRegs(insn),
                                        addresses.getStart(target));
                    break;
                }
                default: {
                    throw new RuntimeException("shouldn't happen");
                }
            }

            addOutput(di);
        }

        /** {@inheritDoc} */
        @Override
        public void visitPlainCstInsn(PlainCstInsn insn) {
            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            Rop rop = insn.getOpcode();
            int ropOpcode = rop.getOpcode();
            DalvInsn di;

            if (rop.getBranchingness() != Rop.BRANCH_NONE) {
                throw new RuntimeException("shouldn't happen");
            }

            if (ropOpcode == RegOps.MOVE_PARAM) {
                if (!paramsAreInOrder) {
                    /*
                     * Parameters are not in order at the top of the reg space.
                     * We need to add moves.
                     */

                    RegisterSpec dest = insn.getResult();
                    int param =
                        ((CstInteger) insn.getConstant()).getValue();
                    RegisterSpec source =
                        RegisterSpec.make(regCount - paramSize + param,
                                dest.getType());
                    di = new SimpleInsn(opcode, pos,
                                        RegisterSpecList.make(dest, source));
                    addOutput(di);
                }
            } else {
                // No moves required for the parameters
                RegisterSpecList regs = getRegs(insn);
                di = new CstInsn(opcode, pos, regs, insn.getConstant());
                addOutput(di);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitSwitchInsn(SwitchInsn insn) {
            SourcePosition pos = insn.getPosition();
            IntList cases = insn.getCases();
            IntList successors = block.getSuccessors();
            int casesSz = cases.size();
            int succSz = successors.size();
            int primarySuccessor = block.getPrimarySuccessor();

            /*
             * Check the assumptions that the number of cases is one
             * less than the number of successors and that the last
             * successor in the list is the primary (in this case, the
             * default). This test is here to guard against forgetting
             * to change this code if the way switch instructions are
             * constructed also gets changed.
             */
            if ((casesSz != (succSz - 1)) ||
                (primarySuccessor != successors.get(casesSz))) {
                throw new RuntimeException("shouldn't happen");
            }

            CodeAddress[] switchTargets = new CodeAddress[casesSz];

            for (int i = 0; i < casesSz; i++) {
                int label = successors.get(i);
                switchTargets[i] = addresses.getStart(label);
            }

            CodeAddress dataAddress = new CodeAddress(pos);
            // make a new address that binds closely to the switch instruction
            CodeAddress switchAddress =
                new CodeAddress(lastAddress.getPosition(), true);
            SwitchData dataInsn =
                new SwitchData(pos, switchAddress, cases, switchTargets);
            Dop opcode = dataInsn.isPacked() ?
                Dops.PACKED_SWITCH : Dops.SPARSE_SWITCH;
            TargetInsn switchInsn =
                new TargetInsn(opcode, pos, getRegs(insn), dataAddress);

            addOutput(switchAddress);
            addOutput(switchInsn);

            addOutputSuffix(new OddSpacer(pos));
            addOutputSuffix(dataAddress);
            addOutputSuffix(dataInsn);
        }

        /**
         * Looks forward to the current block's primary successor, returning
         * the RegisterSpec of the result of the move-result-pseudo at the
         * top of that block or null if none.
         *
         * @return {@code null-ok;} result of move-result-pseudo at the beginning of
         * primary successor
         */
        private RegisterSpec getNextMoveResultPseudo()
        {
            int label = block.getPrimarySuccessor();

            if (label < 0) {
                return null;
            }

            Insn insn
                    = method.getBlocks().labelToBlock(label).getInsns().get(0);

            if (insn.getOpcode().getOpcode() != RegOps.MOVE_RESULT_PSEUDO) {
                return null;
            } else {
                return insn.getResult();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitInvokePolymorphicInsn(InvokePolymorphicInsn insn) {
            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            Rop rop = insn.getOpcode();

            if (rop.getBranchingness() != Rop.BRANCH_THROW) {
                throw new RuntimeException("Expected BRANCH_THROW got " + rop.getBranchingness());
            } else if (!rop.isCallLike()) {
                throw new RuntimeException("Expected call-like operation");
            }

            addOutput(lastAddress);

            RegisterSpecList regs = insn.getSources();
            Constant[] constants = new Constant[] {
                insn.getPolymorphicMethod(),
                insn.getCallSiteProto()
                };
            DalvInsn di = new MultiCstInsn(opcode, pos, regs, constants);

            addOutput(di);
        }

        /** {@inheritDoc} */
        @Override
        public void visitThrowingCstInsn(ThrowingCstInsn insn) {
            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            Rop rop = insn.getOpcode();
            Constant cst = insn.getConstant();

            if (rop.getBranchingness() != Rop.BRANCH_THROW) {
                throw new RuntimeException("Expected BRANCH_THROW got " + rop.getBranchingness());
            }

            addOutput(lastAddress);

            if (rop.isCallLike()) {
                RegisterSpecList regs = insn.getSources();
                DalvInsn di = new CstInsn(opcode, pos, regs, cst);

                addOutput(di);
            } else {
                RegisterSpec realResult = getNextMoveResultPseudo();

                RegisterSpecList regs = getRegs(insn, realResult);
                DalvInsn di;

                boolean hasResult = opcode.hasResult()
                        || (rop.getOpcode() == RegOps.CHECK_CAST);

                if (hasResult != (realResult != null)) {
                    throw new RuntimeException(
                            "Insn with result/move-result-pseudo mismatch " +
                            insn);
                }

                if ((rop.getOpcode() == RegOps.NEW_ARRAY) &&
                    (opcode.getOpcode() != Opcodes.NEW_ARRAY)) {
                    /*
                     * It's a type-specific new-array-<primitive>, and
                     * so it should be turned into a SimpleInsn (no
                     * constant ref as it's implicit).
                     */
                    di = new SimpleInsn(opcode, pos, regs);
                } else {
                    /*
                     * This is the general case for constant-bearing
                     * instructions.
                     */
                    di = new CstInsn(opcode, pos, regs, cst);
                }

                addOutput(di);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitThrowingInsn(ThrowingInsn insn) {
            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            Rop rop = insn.getOpcode();
            RegisterSpec realResult;

            if (rop.getBranchingness() != Rop.BRANCH_THROW) {
                throw new RuntimeException("shouldn't happen");
            }

            realResult = getNextMoveResultPseudo();

            if (opcode.hasResult() != (realResult != null)) {
                throw new RuntimeException(
                        "Insn with result/move-result-pseudo mismatch" + insn);
            }

            addOutput(lastAddress);

            DalvInsn di = new SimpleInsn(opcode, pos,
                    getRegs(insn, realResult));

            addOutput(di);
        }

        /** {@inheritDoc} */
        @Override
        public void visitFillArrayDataInsn(FillArrayDataInsn insn) {
            SourcePosition pos = insn.getPosition();
            Constant cst = insn.getConstant();
            ArrayList<Constant> values = insn.getInitValues();
            Rop rop = insn.getOpcode();

            if (rop.getBranchingness() != Rop.BRANCH_NONE) {
                throw new RuntimeException("shouldn't happen");
            }
            CodeAddress dataAddress = new CodeAddress(pos);
            ArrayData dataInsn =
                new ArrayData(pos, lastAddress, values, cst);

            TargetInsn fillArrayDataInsn =
                new TargetInsn(Dops.FILL_ARRAY_DATA, pos, getRegs(insn),
                        dataAddress);

            addOutput(lastAddress);
            addOutput(fillArrayDataInsn);

            addOutputSuffix(new OddSpacer(pos));
            addOutputSuffix(dataAddress);
            addOutputSuffix(dataInsn);
        }

        /**
         * Adds to the output.
         *
         * @param insn {@code non-null;} instruction to add
         */
        protected void addOutput(DalvInsn insn) {
            output.add(insn);
        }

        /**
         * Adds to the output suffix.
         *
         * @param insn {@code non-null;} instruction to add
         */
        protected void addOutputSuffix(DalvInsn insn) {
            output.addSuffix(insn);
        }
    }

    /**
     * Instruction visitor class for doing instruction translation with
     * local variable tracking
     */
    private class LocalVariableAwareTranslationVisitor
            extends TranslationVisitor {
        /** {@code non-null;} local variable info */
        private final LocalVariableInfo locals;

        /**
         * Constructs an instance.
         *
         * @param output {@code non-null;} destination for instruction output
         * @param locals {@code non-null;} the local variable info
         */
        public LocalVariableAwareTranslationVisitor(OutputCollector output,
                                                    LocalVariableInfo locals) {
            super(output);
            this.locals = locals;
        }

        /** {@inheritDoc} */
        @Override
        public void visitPlainInsn(PlainInsn insn) {
            super.visitPlainInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitPlainCstInsn(PlainCstInsn insn) {
            super.visitPlainCstInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitSwitchInsn(SwitchInsn insn) {
            super.visitSwitchInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitThrowingCstInsn(ThrowingCstInsn insn) {
            super.visitThrowingCstInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        /** {@inheritDoc} */
        @Override
        public void visitThrowingInsn(ThrowingInsn insn) {
            super.visitThrowingInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        /**
         * Adds a {@link LocalStart} to the output if the given
         * instruction in fact introduces a local variable.
         *
         * @param insn {@code non-null;} instruction in question
         */
        public void addIntroductionIfNecessary(Insn insn) {
            RegisterSpec spec = locals.getAssignment(insn);

            if (spec != null) {
                addOutput(new LocalStart(insn.getPosition(), spec));
            }
        }
    }
}

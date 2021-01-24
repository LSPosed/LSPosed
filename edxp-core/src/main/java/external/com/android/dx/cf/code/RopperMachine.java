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

import external.com.android.dx.cf.iface.Method;
import external.com.android.dx.cf.iface.MethodList;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.code.FillArrayDataInsn;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.InvokePolymorphicInsn;
import external.com.android.dx.rop.code.PlainCstInsn;
import external.com.android.dx.rop.code.PlainInsn;
import external.com.android.dx.rop.code.RegOps;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.code.Rops;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.code.SwitchInsn;
import external.com.android.dx.rop.code.ThrowingCstInsn;
import external.com.android.dx.rop.code.ThrowingInsn;
import external.com.android.dx.rop.code.TranslationAdvice;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstCallSiteRef;
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.IntList;
import java.util.ArrayList;

/**
 * Machine implementation for use by {@link Ropper}.
 */
/*package*/ final class RopperMachine extends ValueAwareMachine {
    /** {@code non-null;} array reflection class */
    private static final CstType ARRAY_REFLECT_TYPE =
        new CstType(Type.internClassName("java/lang/reflect/Array"));

    /**
     * {@code non-null;} method constant for use in converting
     * {@code multianewarray} instructions
     */
    private static final CstMethodRef MULTIANEWARRAY_METHOD =
        new CstMethodRef(ARRAY_REFLECT_TYPE,
                         new CstNat(new CstString("newInstance"),
                                    new CstString("(Ljava/lang/Class;[I)" +
                                                "Ljava/lang/Object;")));

    /** {@code non-null;} {@link Ropper} controlling this instance */
    private final Ropper ropper;

    /** {@code non-null;} method being converted */
    private final ConcreteMethod method;

    /** {@code non-null:} list of methods from the class whose method is being converted */
    private final MethodList methods;

    /** {@code non-null;} translation advice */
    private final TranslationAdvice advice;

    /** max locals of the method */
    private final int maxLocals;

    /** {@code non-null;} instructions for the rop basic block in-progress */
    private final ArrayList<Insn> insns;

    /** {@code non-null;} catches for the block currently being processed */
    private TypeList catches;

    /** whether the catches have been used in an instruction */
    private boolean catchesUsed;

    /** whether the block contains a {@code return} */
    private boolean returns;

    /** primary successor index */
    private int primarySuccessorIndex;

    /** {@code >= 0;} number of extra basic blocks required */
    private int extraBlockCount;

    /** true if last processed block ends with a jsr or jsr_W*/
    private boolean hasJsr;

    /** true if an exception can be thrown by the last block processed */
    private boolean blockCanThrow;

    /**
     * If non-null, the ReturnAddress that was used by the terminating ret
     * instruction. If null, there was no ret instruction encountered.
     */

    private ReturnAddress returnAddress;

    /**
     * {@code null-ok;} the appropriate {@code return} op or {@code null}
     * if it is not yet known
     */
    private Rop returnOp;

    /**
     * {@code null-ok;} the source position for the return block or {@code null}
     * if it is not yet known
     */
    private SourcePosition returnPosition;

    /**
     * Constructs an instance.
     *
     * @param ropper {@code non-null;} ropper controlling this instance
     * @param method {@code non-null;} method being converted
     * @param advice {@code non-null;} translation advice to use
     * @param methods {@code non-null;} list of methods defined by the class
     *     that defines {@code method}.
     */
    public RopperMachine(Ropper ropper, ConcreteMethod method,
            TranslationAdvice advice, MethodList methods) {
        super(method.getEffectiveDescriptor());

        if (methods == null) {
            throw new NullPointerException("methods == null");
        }

        if (ropper == null) {
            throw new NullPointerException("ropper == null");
        }

        if (advice == null) {
            throw new NullPointerException("advice == null");
        }

        this.ropper = ropper;
        this.method = method;
        this.methods = methods;
        this.advice = advice;
        this.maxLocals = method.getMaxLocals();
        this.insns = new ArrayList<Insn>(25);
        this.catches = null;
        this.catchesUsed = false;
        this.returns = false;
        this.primarySuccessorIndex = -1;
        this.extraBlockCount = 0;
        this.blockCanThrow = false;
        this.returnOp = null;
        this.returnPosition = null;
    }

    /**
     * Gets the instructions array. It is shared and gets modified by
     * subsequent calls to this instance.
     *
     * @return {@code non-null;} the instructions array
     */
    public ArrayList<Insn> getInsns() {
        return insns;
    }

    /**
     * Gets the return opcode encountered, if any.
     *
     * @return {@code null-ok;} the return opcode
     */
    public Rop getReturnOp() {
        return returnOp;
    }

    /**
     * Gets the return position, if known.
     *
     * @return {@code null-ok;} the return position
     */
    public SourcePosition getReturnPosition() {
        return returnPosition;
    }

    /**
     * Gets ready to start working on a new block. This will clear the
     * {@link #insns} list, set {@link #catches}, reset whether it has
     * been used, reset whether the block contains a
     * {@code return}, and reset {@link #primarySuccessorIndex}.
     */
    public void startBlock(TypeList catches) {
        this.catches = catches;

        insns.clear();
        catchesUsed = false;
        returns = false;
        primarySuccessorIndex = 0;
        extraBlockCount = 0;
        blockCanThrow = false;
        hasJsr = false;
        returnAddress = null;
    }

    /**
     * Gets whether {@link #catches} was used. This indicates that the
     * last instruction in the block is one of the ones that can throw.
     *
     * @return whether {@code catches} has been used
     */
    public boolean wereCatchesUsed() {
        return catchesUsed;
    }

    /**
     * Gets whether the block just processed ended with a
     * {@code return}.
     *
     * @return whether the block returns
     */
    public boolean returns() {
        return returns;
    }

    /**
     * Gets the primary successor index. This is the index into the
     * successors list where the primary may be found or
     * {@code -1} if there are successors but no primary
     * successor. This may return something other than
     * {@code -1} in the case of an instruction with no
     * successors at all (primary or otherwise).
     *
     * @return {@code >= -1;} the primary successor index
     */
    public int getPrimarySuccessorIndex() {
        return primarySuccessorIndex;
    }

    /**
     * Gets how many extra blocks will be needed to represent the
     * block currently being translated. Each extra block should consist
     * of one instruction from the end of the original block.
     *
     * @return {@code >= 0;} the number of extra blocks needed
     */
    public int getExtraBlockCount() {
        return extraBlockCount;
    }

    /**
     * @return true if at least one of the insn processed since the last
     * call to startBlock() can throw.
     */
    public boolean canThrow() {
        return blockCanThrow;
    }

    /**
     * @return true if a JSR has ben encountered since the last call to
     * startBlock()
     */
    public boolean hasJsr() {
        return hasJsr;
    }

    /**
     * @return {@code true} if a {@code ret} has ben encountered since
     * the last call to {@code startBlock()}
     */
    public boolean hasRet() {
        return returnAddress != null;
    }

    /**
     * @return {@code null-ok;} return address of a {@code ret}
     * instruction if encountered since last call to startBlock().
     * {@code null} if no ret instruction encountered.
     */
    public ReturnAddress getReturnAddress() {
        return returnAddress;
    }

    /** {@inheritDoc} */
    @Override
    public void run(Frame frame, int offset, int opcode) {
        /*
         * This is the stack pointer after the opcode's arguments have been
         * popped.
         */
        int stackPointer = maxLocals + frame.getStack().size();

        // The sources have to be retrieved before super.run() gets called.
        RegisterSpecList sources = getSources(opcode, stackPointer);
        int sourceCount = sources.size();

        super.run(frame, offset, opcode);

        SourcePosition pos = method.makeSourcePosistion(offset);
        RegisterSpec localTarget = getLocalTarget(opcode == ByteOps.ISTORE);
        int destCount = resultCount();
        RegisterSpec dest;

        if (destCount == 0) {
            dest = null;
            switch (opcode) {
                case ByteOps.POP:
                case ByteOps.POP2: {
                    // These simply don't appear in the rop form.
                    return;
                }
            }
        } else if (localTarget != null) {
            dest = localTarget;
        } else if (destCount == 1) {
            dest = RegisterSpec.make(stackPointer, result(0));
        } else {
            /*
             * This clause only ever applies to the stack manipulation
             * ops that have results (that is, dup* and swap but not
             * pop*).
             *
             * What we do is first move all the source registers into
             * the "temporary stack" area defined for the method, and
             * then move stuff back down onto the main "stack" in the
             * arrangement specified by the stack op pattern.
             *
             * Note: This code ends up emitting a lot of what will
             * turn out to be superfluous moves (e.g., moving back and
             * forth to the same local when doing a dup); however,
             * that makes this code a bit easier (and goodness knows
             * it doesn't need any extra complexity), and all the SSA
             * stuff is going to want to deal with this sort of
             * superfluous assignment anyway, so it should be a wash
             * in the end.
             */
            int scratchAt = ropper.getFirstTempStackReg();
            RegisterSpec[] scratchRegs = new RegisterSpec[sourceCount];

            for (int i = 0; i < sourceCount; i++) {
                RegisterSpec src = sources.get(i);
                TypeBearer type = src.getTypeBearer();
                RegisterSpec scratch = src.withReg(scratchAt);
                insns.add(new PlainInsn(Rops.opMove(type), pos, scratch, src));
                scratchRegs[i] = scratch;
                scratchAt += src.getCategory();
            }

            for (int pattern = getAuxInt(); pattern != 0; pattern >>= 4) {
                int which = (pattern & 0x0f) - 1;
                RegisterSpec scratch = scratchRegs[which];
                TypeBearer type = scratch.getTypeBearer();
                insns.add(new PlainInsn(Rops.opMove(type), pos,
                                        scratch.withReg(stackPointer),
                                        scratch));
                stackPointer += type.getType().getCategory();
            }
            return;
        }

        TypeBearer destType = (dest != null) ? dest : Type.VOID;
        Constant cst = getAuxCst();
        int ropOpcode;
        Rop rop;
        Insn insn;

        if (opcode == ByteOps.MULTIANEWARRAY) {
            blockCanThrow = true;

            // Add the extra instructions for handling multianewarray.

            extraBlockCount = 6;

            /*
             * Add an array constructor for the int[] containing all the
             * dimensions.
             */
            RegisterSpec dimsReg =
                RegisterSpec.make(dest.getNextReg(), Type.INT_ARRAY);
            rop = Rops.opFilledNewArray(Type.INT_ARRAY, sourceCount);
            insn = new ThrowingCstInsn(rop, pos, sources, catches,
                    CstType.INT_ARRAY);
            insns.add(insn);

            // Add a move-result for the new-filled-array
            rop = Rops.opMoveResult(Type.INT_ARRAY);
            insn = new PlainInsn(rop, pos, dimsReg, RegisterSpecList.EMPTY);
            insns.add(insn);

            /*
             * Add a const-class instruction for the specified array
             * class.
             */

            /*
             * Remove as many dimensions from the originally specified
             * class as are given in the explicit list of dimensions,
             * so as to pass the right component class to the standard
             * Java library array constructor.
             */
            Type componentType = ((CstType) cst).getClassType();
            for (int i = 0; i < sourceCount; i++) {
                componentType = componentType.getComponentType();
            }

            RegisterSpec classReg =
                RegisterSpec.make(dest.getReg(), Type.CLASS);

            if (componentType.isPrimitive()) {
                /*
                 * The component type is primitive (e.g., int as opposed
                 * to Integer), so we have to fetch the corresponding
                 * TYPE class.
                 */
                CstFieldRef typeField =
                    CstFieldRef.forPrimitiveType(componentType);
                insn = new ThrowingCstInsn(Rops.GET_STATIC_OBJECT, pos,
                                           RegisterSpecList.EMPTY,
                                           catches, typeField);
            } else {
                /*
                 * The component type is an object type, so just make a
                 * normal class reference.
                 */
                insn = new ThrowingCstInsn(Rops.CONST_OBJECT, pos,
                                           RegisterSpecList.EMPTY, catches,
                                           new CstType(componentType));
            }

            insns.add(insn);

            // Add a move-result-pseudo for the get-static or const
            rop = Rops.opMoveResultPseudo(classReg.getType());
            insn = new PlainInsn(rop, pos, classReg, RegisterSpecList.EMPTY);
            insns.add(insn);

            /*
             * Add a call to the "multianewarray method," that is,
             * Array.newInstance(class, dims). Note: The result type
             * of newInstance() is Object, which is why the last
             * instruction in this sequence is a cast to the right
             * type for the original instruction.
             */

            RegisterSpec objectReg =
                RegisterSpec.make(dest.getReg(), Type.OBJECT);

            insn = new ThrowingCstInsn(
                    Rops.opInvokeStatic(MULTIANEWARRAY_METHOD.getPrototype()),
                    pos, RegisterSpecList.make(classReg, dimsReg),
                    catches, MULTIANEWARRAY_METHOD);
            insns.add(insn);

            // Add a move-result.
            rop = Rops.opMoveResult(MULTIANEWARRAY_METHOD.getPrototype()
                    .getReturnType());
            insn = new PlainInsn(rop, pos, objectReg, RegisterSpecList.EMPTY);
            insns.add(insn);

            /*
             * And finally, set up for the remainder of this method to
             * add an appropriate cast.
             */

            opcode = ByteOps.CHECKCAST;
            sources = RegisterSpecList.make(objectReg);
        } else if (opcode == ByteOps.JSR) {
            // JSR has no Rop instruction
            hasJsr = true;
            return;
        } else if (opcode == ByteOps.RET) {
            try {
                returnAddress = (ReturnAddress)arg(0);
            } catch (ClassCastException ex) {
                throw new RuntimeException(
                        "Argument to RET was not a ReturnAddress", ex);
            }
            // RET has no Rop instruction.
            return;
        }

        ropOpcode = jopToRopOpcode(opcode, cst);
        rop = Rops.ropFor(ropOpcode, destType, sources, cst);

        Insn moveResult = null;
        if (dest != null && rop.isCallLike()) {
            /*
             * We're going to want to have a move-result in the next
             * basic block.
             */
            extraBlockCount++;

            Type returnType;
            if (rop.getOpcode() == RegOps.INVOKE_CUSTOM) {
                returnType = ((CstCallSiteRef) cst).getReturnType();
            } else {
                returnType = ((CstMethodRef) cst).getPrototype().getReturnType();
            }
            moveResult = new PlainInsn(Rops.opMoveResult(returnType),
                                       pos, dest, RegisterSpecList.EMPTY);

            dest = null;
        } else if (dest != null && rop.canThrow()) {
            /*
             * We're going to want to have a move-result-pseudo in the
             * next basic block.
             */
            extraBlockCount++;

            moveResult = new PlainInsn(
                    Rops.opMoveResultPseudo(dest.getTypeBearer()),
                    pos, dest, RegisterSpecList.EMPTY);

            dest = null;
        }
        if (ropOpcode == RegOps.NEW_ARRAY) {
            /*
             * In the original bytecode, this was either a primitive
             * array constructor "newarray" or an object array
             * constructor "anewarray". In the former case, there is
             * no explicit constant, and in the latter, the constant
             * is for the element type and not the array type. The rop
             * instruction form for both of these is supposed to be
             * the resulting array type, so we initialize / alter
             * "cst" here, accordingly. Conveniently enough, the rop
             * opcode already gets constructed with the proper array
             * type.
             */
            cst = CstType.intern(rop.getResult());
        } else if ((cst == null) && (sourceCount == 2)) {
            TypeBearer firstType = sources.get(0).getTypeBearer();
            TypeBearer lastType = sources.get(1).getTypeBearer();

            if ((lastType.isConstant() || firstType.isConstant()) &&
                 advice.hasConstantOperation(rop, sources.get(0),
                                             sources.get(1))) {

                if (lastType.isConstant()) {
                    /*
                     * The target architecture has an instruction that can
                     * build in the constant found in the second argument,
                     * so pull it out of the sources and just use it as a
                     * constant here.
                     */
                    cst = (Constant) lastType;
                    sources = sources.withoutLast();

                    // For subtraction, change to addition and invert constant
                    if (rop.getOpcode() == RegOps.SUB) {
                        ropOpcode = RegOps.ADD;
                        CstInteger cstInt = (CstInteger) lastType;
                        cst = CstInteger.make(-cstInt.getValue());
                    }
                } else {
                    /*
                     * The target architecture has an instruction that can
                     * build in the constant found in the first argument,
                     * so pull it out of the sources and just use it as a
                     * constant here.
                     */
                    cst = (Constant) firstType;
                    sources = sources.withoutFirst();
                }

                rop = Rops.ropFor(ropOpcode, destType, sources, cst);
            }
        }

        SwitchList cases = getAuxCases();
        ArrayList<Constant> initValues = getInitValues();
        boolean canThrow = rop.canThrow();

        blockCanThrow |= canThrow;

        if (cases != null) {
            if (cases.size() == 0) {
                // It's a default-only switch statement. It can happen!
                insn = new PlainInsn(Rops.GOTO, pos, null,
                                     RegisterSpecList.EMPTY);
                primarySuccessorIndex = 0;
            } else {
                IntList values = cases.getValues();
                insn = new SwitchInsn(rop, pos, dest, sources, values);
                primarySuccessorIndex = values.size();
            }
        } else if (ropOpcode == RegOps.RETURN) {
            /*
             * Returns get turned into the combination of a move (if
             * non-void and if the return doesn't already mention
             * register 0) and a goto (to the return block).
             */
            if (sources.size() != 0) {
                RegisterSpec source = sources.get(0);
                TypeBearer type = source.getTypeBearer();
                if (source.getReg() != 0) {
                    insns.add(new PlainInsn(Rops.opMove(type), pos,
                                            RegisterSpec.make(0, type),
                                            source));
                }
            }
            insn = new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY);
            primarySuccessorIndex = 0;
            updateReturnOp(rop, pos);
            returns = true;
        } else if (cst != null) {
            if (canThrow) {
                if (rop.getOpcode() == RegOps.INVOKE_POLYMORPHIC) {
                    insn = makeInvokePolymorphicInsn(rop, pos, sources, catches, cst);
                } else {
                    insn = new ThrowingCstInsn(rop, pos, sources, catches, cst);
                }
                catchesUsed = true;
                primarySuccessorIndex = catches.size();
            } else {
                insn = new PlainCstInsn(rop, pos, dest, sources, cst);
            }
        } else if (canThrow) {
            insn = new ThrowingInsn(rop, pos, sources, catches);
            catchesUsed = true;
            if (opcode == ByteOps.ATHROW) {
                /*
                 * The op athrow is the only one where it's possible
                 * to have non-empty successors and yet not have a
                 * primary successor.
                 */
                primarySuccessorIndex = -1;
            } else {
                primarySuccessorIndex = catches.size();
            }
        } else {
            insn = new PlainInsn(rop, pos, dest, sources);
        }

        insns.add(insn);

        if (moveResult != null) {
            insns.add(moveResult);
        }

        /*
         * If initValues is non-null, it means that the parser has
         * seen a group of compatible constant initialization
         * bytecodes that are applied to the current newarray. The
         * action we take here is to convert these initialization
         * bytecodes into a single fill-array-data ROP which lays out
         * all the constant values in a table.
         */
        if (initValues != null) {
            extraBlockCount++;
            insn = new FillArrayDataInsn(Rops.FILL_ARRAY_DATA, pos,
                    RegisterSpecList.make(moveResult.getResult()), initValues,
                    cst);
            insns.add(insn);
        }
    }

    /**
     * Helper for {@link #run}, which gets the list of sources for the.
     * instruction.
     *
     * @param opcode the opcode being translated
     * @param stackPointer {@code >= 0;} the stack pointer after the
     * instruction's arguments have been popped
     * @return {@code non-null;} the sources
     */
    private RegisterSpecList getSources(int opcode, int stackPointer) {
        int count = argCount();

        if (count == 0) {
            // We get an easy out if there aren't any sources.
            return RegisterSpecList.EMPTY;
        }

        int localIndex = getLocalIndex();
        RegisterSpecList sources;

        if (localIndex >= 0) {
            // The instruction is operating on a local variable.
            sources = new RegisterSpecList(1);
            sources.set(0, RegisterSpec.make(localIndex, arg(0)));
        } else {
            sources = new RegisterSpecList(count);
            int regAt = stackPointer;
            for (int i = 0; i < count; i++) {
                RegisterSpec spec = RegisterSpec.make(regAt, arg(i));
                sources.set(i, spec);
                regAt += spec.getCategory();
            }

            switch (opcode) {
                case ByteOps.IASTORE: {
                    /*
                     * The Java argument order for array stores is
                     * (array, index, value), but the rop argument
                     * order is (value, array, index). The following
                     * code gets the right arguments in the right
                     * places.
                     */
                    if (count != 3) {
                        throw new RuntimeException("shouldn't happen");
                    }
                    RegisterSpec array = sources.get(0);
                    RegisterSpec index = sources.get(1);
                    RegisterSpec value = sources.get(2);
                    sources.set(0, value);
                    sources.set(1, array);
                    sources.set(2, index);
                    break;
                }
                case ByteOps.PUTFIELD: {
                    /*
                     * Similar to above: The Java argument order for
                     * putfield is (object, value), but the rop
                     * argument order is (value, object).
                     */
                    if (count != 2) {
                        throw new RuntimeException("shouldn't happen");
                    }
                    RegisterSpec obj = sources.get(0);
                    RegisterSpec value = sources.get(1);
                    sources.set(0, value);
                    sources.set(1, obj);
                    break;
                }
            }
        }

        sources.setImmutable();
        return sources;
    }

    /**
     * Sets or updates the information about the return block.
     *
     * @param op {@code non-null;} the opcode to use
     * @param pos {@code non-null;} the position to use
     */
    private void updateReturnOp(Rop op, SourcePosition pos) {
        if (op == null) {
            throw new NullPointerException("op == null");
        }

        if (pos == null) {
            throw new NullPointerException("pos == null");
        }

        if (returnOp == null) {
            returnOp = op;
            returnPosition = pos;
        } else {
            if (returnOp != op) {
                throw new SimException("return op mismatch: " + op + ", " +
                                       returnOp);
            }

            if (pos.getLine() > returnPosition.getLine()) {
                // Pick the largest line number to be the "canonical" return.
                returnPosition = pos;
            }
        }
    }

    /**
     * Gets the register opcode for the given Java opcode.
     *
     * @param jop {@code jop >= 0;} the Java opcode
     * @param cst {@code null-ok;} the constant argument, if any
     * @return {@code >= 0;} the corresponding register opcode
     */
    private int jopToRopOpcode(int jop, Constant cst) {
        switch (jop) {
            case ByteOps.POP:
            case ByteOps.POP2:
            case ByteOps.DUP:
            case ByteOps.DUP_X1:
            case ByteOps.DUP_X2:
            case ByteOps.DUP2:
            case ByteOps.DUP2_X1:
            case ByteOps.DUP2_X2:
            case ByteOps.SWAP:
            case ByteOps.JSR:
            case ByteOps.RET:
            case ByteOps.MULTIANEWARRAY: {
                // These need to be taken care of specially.
                break;
            }
            case ByteOps.NOP: {
                return RegOps.NOP;
            }
            case ByteOps.LDC:
            case ByteOps.LDC2_W: {
                return RegOps.CONST;
            }
            case ByteOps.ILOAD:
            case ByteOps.ISTORE: {
                return RegOps.MOVE;
            }
            case ByteOps.IALOAD: {
                return RegOps.AGET;
            }
            case ByteOps.IASTORE: {
                return RegOps.APUT;
            }
            case ByteOps.IADD:
            case ByteOps.IINC: {
                return RegOps.ADD;
            }
            case ByteOps.ISUB: {
                return RegOps.SUB;
            }
            case ByteOps.IMUL: {
                return RegOps.MUL;
            }
            case ByteOps.IDIV: {
                return RegOps.DIV;
            }
            case ByteOps.IREM: {
                return RegOps.REM;
            }
            case ByteOps.INEG: {
                return RegOps.NEG;
            }
            case ByteOps.ISHL: {
                return RegOps.SHL;
            }
            case ByteOps.ISHR: {
                return RegOps.SHR;
            }
            case ByteOps.IUSHR: {
                return RegOps.USHR;
            }
            case ByteOps.IAND: {
                return RegOps.AND;
            }
            case ByteOps.IOR: {
                return RegOps.OR;
            }
            case ByteOps.IXOR: {
                return RegOps.XOR;
            }
            case ByteOps.I2L:
            case ByteOps.I2F:
            case ByteOps.I2D:
            case ByteOps.L2I:
            case ByteOps.L2F:
            case ByteOps.L2D:
            case ByteOps.F2I:
            case ByteOps.F2L:
            case ByteOps.F2D:
            case ByteOps.D2I:
            case ByteOps.D2L:
            case ByteOps.D2F: {
                return RegOps.CONV;
            }
            case ByteOps.I2B: {
                return RegOps.TO_BYTE;
            }
            case ByteOps.I2C: {
                return RegOps.TO_CHAR;
            }
            case ByteOps.I2S: {
                return RegOps.TO_SHORT;
            }
            case ByteOps.LCMP:
            case ByteOps.FCMPL:
            case ByteOps.DCMPL: {
                return RegOps.CMPL;
            }
            case ByteOps.FCMPG:
            case ByteOps.DCMPG: {
                return RegOps.CMPG;
            }
            case ByteOps.IFEQ:
            case ByteOps.IF_ICMPEQ:
            case ByteOps.IF_ACMPEQ:
            case ByteOps.IFNULL: {
                return RegOps.IF_EQ;
            }
            case ByteOps.IFNE:
            case ByteOps.IF_ICMPNE:
            case ByteOps.IF_ACMPNE:
            case ByteOps.IFNONNULL: {
                return RegOps.IF_NE;
            }
            case ByteOps.IFLT:
            case ByteOps.IF_ICMPLT: {
                return RegOps.IF_LT;
            }
            case ByteOps.IFGE:
            case ByteOps.IF_ICMPGE: {
                return RegOps.IF_GE;
            }
            case ByteOps.IFGT:
            case ByteOps.IF_ICMPGT: {
                return RegOps.IF_GT;
            }
            case ByteOps.IFLE:
            case ByteOps.IF_ICMPLE: {
                return RegOps.IF_LE;
            }
            case ByteOps.GOTO: {
                return RegOps.GOTO;
            }
            case ByteOps.LOOKUPSWITCH: {
                return RegOps.SWITCH;
            }
            case ByteOps.IRETURN:
            case ByteOps.RETURN: {
                return RegOps.RETURN;
            }
            case ByteOps.GETSTATIC: {
                return RegOps.GET_STATIC;
            }
            case ByteOps.PUTSTATIC: {
                return RegOps.PUT_STATIC;
            }
            case ByteOps.GETFIELD: {
                return RegOps.GET_FIELD;
            }
            case ByteOps.PUTFIELD: {
                return RegOps.PUT_FIELD;
            }
            case ByteOps.INVOKEVIRTUAL: {
                CstMethodRef ref = (CstMethodRef) cst;
                // The java bytecode specification does not explicitly disallow
                // invokevirtual calls to any instance method, though it
                // specifies that instance methods and private methods "should" be
                // called using "invokespecial" instead of "invokevirtual".
                // Several bytecode tools generate "invokevirtual" instructions for
                // invocation of private methods.
                //
                // The dalvik opcode specification on the other hand allows
                // invoke-virtual to be used only with "normal" virtual methods,
                // i.e, ones that are not private, static, final or constructors.
                // We therefore need to transform invoke-virtual calls to private
                // instance methods to invoke-direct opcodes.
                //
                // Note that it assumes that all methods for a given class are
                // defined in the same dex file.
                //
                // NOTE: This is a slow O(n) loop, and can be replaced with a
                // faster implementation (at the cost of higher memory usage)
                // if it proves to be a hot area of code.
                if (ref.getDefiningClass().equals(method.getDefiningClass())) {
                    for (int i = 0; i < methods.size(); ++i) {
                        final Method m = methods.get(i);
                        if (AccessFlags.isPrivate(m.getAccessFlags()) &&
                                ref.getNat().equals(m.getNat())) {
                            return RegOps.INVOKE_DIRECT;
                        }
                    }
                }
                // If the method reference is a signature polymorphic method
                // substitute invoke-polymorphic for invoke-virtual. This only
                // affects MethodHandle.invoke and MethodHandle.invokeExact.
                if (ref.isSignaturePolymorphic()) {
                    return RegOps.INVOKE_POLYMORPHIC;
                }
                return RegOps.INVOKE_VIRTUAL;
            }
            case ByteOps.INVOKESPECIAL: {
                /*
                 * Determine whether the opcode should be
                 * INVOKE_DIRECT or INVOKE_SUPER. See vmspec-2 section 6
                 * on "invokespecial" as well as section 4.8.2 (7th
                 * bullet point) for the gory details.
                 */
                /* TODO: Consider checking that invoke-special target
                 * method is private, or constructor since otherwise ART
                 * verifier will reject it.
                 */
                CstMethodRef ref = (CstMethodRef) cst;
                if (ref.isInstanceInit() ||
                    (ref.getDefiningClass().equals(method.getDefiningClass()))) {
                    return RegOps.INVOKE_DIRECT;
                }
                return RegOps.INVOKE_SUPER;
            }
            case ByteOps.INVOKESTATIC: {
                return RegOps.INVOKE_STATIC;
            }
            case ByteOps.INVOKEINTERFACE: {
                return RegOps.INVOKE_INTERFACE;
            }
            case ByteOps.INVOKEDYNAMIC: {
                return RegOps.INVOKE_CUSTOM;
            }
            case ByteOps.NEW: {
                return RegOps.NEW_INSTANCE;
            }
            case ByteOps.NEWARRAY:
            case ByteOps.ANEWARRAY: {
                return RegOps.NEW_ARRAY;
            }
            case ByteOps.ARRAYLENGTH: {
                return RegOps.ARRAY_LENGTH;
            }
            case ByteOps.ATHROW: {
                return RegOps.THROW;
            }
            case ByteOps.CHECKCAST: {
                return RegOps.CHECK_CAST;
            }
            case ByteOps.INSTANCEOF: {
                return RegOps.INSTANCE_OF;
            }
            case ByteOps.MONITORENTER: {
                return RegOps.MONITOR_ENTER;
            }
            case ByteOps.MONITOREXIT: {
                return RegOps.MONITOR_EXIT;
            }
        }

        throw new RuntimeException("shouldn't happen");
    }

    private Insn makeInvokePolymorphicInsn(Rop rop, SourcePosition pos, RegisterSpecList sources,
        TypeList catches, Constant cst) {
        CstMethodRef cstMethodRef = (CstMethodRef) cst;
        return new InvokePolymorphicInsn(rop, pos, sources, catches, cstMethodRef);
    }
}

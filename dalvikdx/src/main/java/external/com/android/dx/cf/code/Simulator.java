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

import external.com.android.dex.DexFormat;
import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.rop.code.LocalItem;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.cst.CstInterfaceMethodRef;
import external.com.android.dx.rop.cst.CstInvokeDynamic;
import external.com.android.dx.rop.cst.CstMethodHandle;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstProtoRef;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.Hex;
import java.util.ArrayList;

/**
 * Class which knows how to simulate the effects of executing bytecode.
 *
 * <p><b>Note:</b> This class is not thread-safe. If multiple threads
 * need to use a single instance, they must synchronize access explicitly
 * between themselves.</p>
 */
public class Simulator {
    /**
     * {@code non-null;} canned error message for local variable
     * table mismatches
     */
    private static final String LOCAL_MISMATCH_ERROR =
        "This is symptomatic of .class transformation tools that ignore " +
        "local variable information.";

    /** {@code non-null;} machine to use when simulating */
    private final Machine machine;

    /** {@code non-null;} array of bytecode */
    private final BytecodeArray code;

    /** {@code non-null;} the method being simulated */
    private ConcreteMethod method;

    /** {@code non-null;} local variable information */
    private final LocalVariableList localVariables;

    /** {@code non-null;} visitor instance to use */
    private final SimVisitor visitor;

    /** {@code non-null;} options for dex output */
    private final DexOptions dexOptions;

    /**
     * Constructs an instance.
     *
     * @param machine {@code non-null;} machine to use when simulating
     * @param method {@code non-null;} method data to use
     * @param dexOptions {@code non-null;} options for dex output
     */
    public Simulator(Machine machine, ConcreteMethod method, DexOptions dexOptions) {
        if (machine == null) {
            throw new NullPointerException("machine == null");
        }

        if (method == null) {
            throw new NullPointerException("method == null");
        }

        if (dexOptions == null) {
            throw new NullPointerException("dexOptions == null");
        }

        this.machine = machine;
        this.code = method.getCode();
        this.method = method;
        this.localVariables = method.getLocalVariables();
        this.visitor = new SimVisitor();
        this.dexOptions = dexOptions;

        // This check assumes class is initialized (accesses dexOptions).
        if (method.isDefaultOrStaticInterfaceMethod()) {
            checkInterfaceMethodDeclaration(method);
        }
    }

    /**
     * Simulates the effect of executing the given basic block. This modifies
     * the passed-in frame to represent the end result.
     *
     * @param bb {@code non-null;} the basic block
     * @param frame {@code non-null;} frame to operate on
     */
    public void simulate(ByteBlock bb, Frame frame) {
        int end = bb.getEnd();

        visitor.setFrame(frame);

        try {
            for (int off = bb.getStart(); off < end; /*off*/) {
                int length = code.parseInstruction(off, visitor);
                visitor.setPreviousOffset(off);
                off += length;
            }
        } catch (SimException ex) {
            frame.annotate(ex);
            throw ex;
        }
    }

    /**
     * Simulates the effect of the instruction at the given offset, by
     * making appropriate calls on the given frame.
     *
     * @param offset {@code offset >= 0;} offset of the instruction to simulate
     * @param frame {@code non-null;} frame to operate on
     * @return the length of the instruction, in bytes
     */
    public int simulate(int offset, Frame frame) {
        visitor.setFrame(frame);
        return code.parseInstruction(offset, visitor);
    }

    /**
     * Constructs an "illegal top-of-stack" exception, for the stack
     * manipulation opcodes.
     */
    private static SimException illegalTos() {
        return new SimException("stack mismatch: illegal " +
                "top-of-stack for opcode");
    }

    /**
     * Returns the required array type for an array load or store
     * instruction, based on a given implied type and an observed
     * actual array type.
     *
     * <p>The interesting cases here have to do with object arrays,
     * <code>byte[]</code>s, <code>boolean[]</code>s, and
     * known-nulls.</p>
     *
     * <p>In the case of arrays of objects, we want to narrow the type
     * to the actual array present on the stack, as long as what is
     * present is an object type. Similarly, due to a quirk of the
     * original bytecode representation, the instructions for dealing
     * with <code>byte[]</code> and <code>boolean[]</code> are
     * undifferentiated, and we aim here to return whichever one was
     * actually present on the stack.</p>
     *
     * <p>In the case where there is a known-null on the stack where
     * an array is expected, our behavior depends on the implied type
     * of the instruction. When the implied type is a reference, we
     * don't attempt to infer anything, as we don't know the dimension
     * of the null constant and thus any explicit inferred type could
     * be wrong. When the implied type is a primitive, we fall back to
     * the implied type of the instruction. Due to the quirk described
     * above, this means that source code that uses
     * <code>boolean[]</code> might get translated surprisingly -- but
     * correctly -- into an instruction that specifies a
     * <code>byte[]</code>. It will be correct, because should the
     * code actually execute, it will necessarily throw a
     * <code>NullPointerException</code>, and it won't matter what
     * opcode variant is used to achieve that result.</p>
     *
     * @param impliedType {@code non-null;} type implied by the
     * instruction; is <i>not</i> an array type
     * @param foundArrayType {@code non-null;} type found on the
     * stack; is either an array type or a known-null
     * @return {@code non-null;} the array type that should be
     * required in this context
     */
    private static Type requiredArrayTypeFor(Type impliedType,
            Type foundArrayType) {
        if (foundArrayType == Type.KNOWN_NULL) {
            return impliedType.isReference()
                ? Type.KNOWN_NULL
                : impliedType.getArrayType();
        }

        if ((impliedType == Type.OBJECT)
                && foundArrayType.isArray()
                && foundArrayType.getComponentType().isReference()) {
            return foundArrayType;
        }

        if ((impliedType == Type.BYTE)
                && (foundArrayType == Type.BOOLEAN_ARRAY)) {
            /*
             * Per above, an instruction with implied byte[] is also
             * allowed to be used on boolean[].
             */
            return Type.BOOLEAN_ARRAY;
        }

        return impliedType.getArrayType();
    }

    /**
     * Bytecode visitor used during simulation.
     */
    private class SimVisitor implements BytecodeArray.Visitor {
        /**
         * {@code non-null;} machine instance to use (just to avoid excessive
         * cross-object field access)
         */
        private final Machine machine;

        /**
         * {@code null-ok;} frame to use; set with each call to
         * {@link Simulator#simulate}
         */
        private Frame frame;

        /** offset of the previous bytecode */
        private int previousOffset;

        /**
         * Constructs an instance.
         */
        public SimVisitor() {
            this.machine = Simulator.this.machine;
            this.frame = null;
        }

        /**
         * Sets the frame to act on.
         *
         * @param frame {@code non-null;} the frame
         */
        public void setFrame(Frame frame) {
            if (frame == null) {
                throw new NullPointerException("frame == null");
            }

            this.frame = frame;
        }

        /** {@inheritDoc} */
        @Override
        public void visitInvalid(int opcode, int offset, int length) {
            throw new SimException("invalid opcode " + Hex.u1(opcode));
        }

        /** {@inheritDoc} */
        @Override
        public void visitNoArgs(int opcode, int offset, int length,
                Type type) {
            switch (opcode) {
                case ByteOps.NOP: {
                    machine.clearArgs();
                    break;
                }
                case ByteOps.INEG: {
                    machine.popArgs(frame, type);
                    break;
                }
                case ByteOps.I2L:
                case ByteOps.I2F:
                case ByteOps.I2D:
                case ByteOps.I2B:
                case ByteOps.I2C:
                case ByteOps.I2S: {
                    machine.popArgs(frame, Type.INT);
                    break;
                }
                case ByteOps.L2I:
                case ByteOps.L2F:
                case ByteOps.L2D: {
                    machine.popArgs(frame, Type.LONG);
                    break;
                }
                case ByteOps.F2I:
                case ByteOps.F2L:
                case ByteOps.F2D: {
                    machine.popArgs(frame, Type.FLOAT);
                    break;
                }
                case ByteOps.D2I:
                case ByteOps.D2L:
                case ByteOps.D2F: {
                    machine.popArgs(frame, Type.DOUBLE);
                    break;
                }
                case ByteOps.RETURN: {
                    machine.clearArgs();
                    checkReturnType(Type.VOID);
                    break;
                }
                case ByteOps.IRETURN: {
                    Type checkType = type;
                    if (type == Type.OBJECT) {
                        /*
                         * For an object return, use the best-known
                         * type of the popped value.
                         */
                        checkType = frame.getStack().peekType(0);
                    }
                    machine.popArgs(frame, type);
                    checkReturnType(checkType);
                    break;
                }
                case ByteOps.POP: {
                    Type peekType = frame.getStack().peekType(0);
                    if (peekType.isCategory2()) {
                        throw illegalTos();
                    }
                    machine.popArgs(frame, 1);
                    break;
                }
                case ByteOps.ARRAYLENGTH: {
                    Type arrayType = frame.getStack().peekType(0);
                    if (!arrayType.isArrayOrKnownNull()) {
                        fail("type mismatch: expected array type but encountered " +
                             arrayType.toHuman());
                    }
                    machine.popArgs(frame, Type.OBJECT);
                    break;
                }
                case ByteOps.ATHROW:
                case ByteOps.MONITORENTER:
                case ByteOps.MONITOREXIT: {
                    machine.popArgs(frame, Type.OBJECT);
                    break;
                }
                case ByteOps.IALOAD: {
                    /*
                     * See comment on requiredArrayTypeFor() for explanation
                     * about what's going on here.
                     */
                    Type foundArrayType = frame.getStack().peekType(1);
                    Type requiredArrayType =
                        requiredArrayTypeFor(type, foundArrayType);

                    // Make type agree with the discovered requiredArrayType.
                    type = (requiredArrayType == Type.KNOWN_NULL)
                        ? Type.KNOWN_NULL
                        : requiredArrayType.getComponentType();

                    machine.popArgs(frame, requiredArrayType, Type.INT);
                    break;
                }
                case ByteOps.IADD:
                case ByteOps.ISUB:
                case ByteOps.IMUL:
                case ByteOps.IDIV:
                case ByteOps.IREM:
                case ByteOps.IAND:
                case ByteOps.IOR:
                case ByteOps.IXOR: {
                    machine.popArgs(frame, type, type);
                    break;
                }
                case ByteOps.ISHL:
                case ByteOps.ISHR:
                case ByteOps.IUSHR: {
                    machine.popArgs(frame, type, Type.INT);
                    break;
                }
                case ByteOps.LCMP: {
                    machine.popArgs(frame, Type.LONG, Type.LONG);
                    break;
                }
                case ByteOps.FCMPL:
                case ByteOps.FCMPG: {
                    machine.popArgs(frame, Type.FLOAT, Type.FLOAT);
                    break;
                }
                case ByteOps.DCMPL:
                case ByteOps.DCMPG: {
                    machine.popArgs(frame, Type.DOUBLE, Type.DOUBLE);
                    break;
                }
                case ByteOps.IASTORE: {
                    /*
                     * See comment on requiredArrayTypeFor() for
                     * explanation about what's going on here. In
                     * addition to that, the category 1 vs. 2 thing
                     * below is to deal with the fact that, if the
                     * element type is category 2, we have to skip
                     * over one extra stack slot to find the array.
                     */
                    ExecutionStack stack = frame.getStack();
                    int peekDepth = type.isCategory1() ? 2 : 3;
                    Type foundArrayType = stack.peekType(peekDepth);
                    boolean foundArrayLocal = stack.peekLocal(peekDepth);

                    Type requiredArrayType =
                        requiredArrayTypeFor(type, foundArrayType);

                    /*
                     * Make type agree with the discovered requiredArrayType
                     * if it has local info.
                     */
                    if (foundArrayLocal) {
                        type = (requiredArrayType == Type.KNOWN_NULL)
                            ? Type.KNOWN_NULL
                            : requiredArrayType.getComponentType();
                    }

                    machine.popArgs(frame, requiredArrayType, Type.INT, type);
                    break;
                }
                case ByteOps.POP2:
                case ByteOps.DUP2: {
                    ExecutionStack stack = frame.getStack();
                    int pattern;

                    if (stack.peekType(0).isCategory2()) {
                        // "form 2" in vmspec-2
                        machine.popArgs(frame, 1);
                        pattern = 0x11;
                    } else if (stack.peekType(1).isCategory1()) {
                        // "form 1"
                        machine.popArgs(frame, 2);
                        pattern = 0x2121;
                    } else {
                        throw illegalTos();
                    }

                    if (opcode == ByteOps.DUP2) {
                        machine.auxIntArg(pattern);
                    }
                    break;
                }
                case ByteOps.DUP: {
                    Type peekType = frame.getStack().peekType(0);

                    if (peekType.isCategory2()) {
                        throw illegalTos();
                    }

                    machine.popArgs(frame, 1);
                    machine.auxIntArg(0x11);
                    break;
                }
                case ByteOps.DUP_X1: {
                    ExecutionStack stack = frame.getStack();

                    if (!(stack.peekType(0).isCategory1() &&
                          stack.peekType(1).isCategory1())) {
                        throw illegalTos();
                    }

                    machine.popArgs(frame, 2);
                    machine.auxIntArg(0x212);
                    break;
                }
                case ByteOps.DUP_X2: {
                    ExecutionStack stack = frame.getStack();

                    if (stack.peekType(0).isCategory2()) {
                        throw illegalTos();
                    }

                    if (stack.peekType(1).isCategory2()) {
                        // "form 2" in vmspec-2
                        machine.popArgs(frame, 2);
                        machine.auxIntArg(0x212);
                    } else if (stack.peekType(2).isCategory1()) {
                        // "form 1"
                        machine.popArgs(frame, 3);
                        machine.auxIntArg(0x3213);
                    } else {
                        throw illegalTos();
                    }
                    break;
                }
                case ByteOps.DUP2_X1: {
                    ExecutionStack stack = frame.getStack();

                    if (stack.peekType(0).isCategory2()) {
                        // "form 2" in vmspec-2
                        if (stack.peekType(2).isCategory2()) {
                            throw illegalTos();
                        }
                        machine.popArgs(frame, 2);
                        machine.auxIntArg(0x212);
                    } else {
                        // "form 1"
                        if (stack.peekType(1).isCategory2() ||
                            stack.peekType(2).isCategory2()) {
                            throw illegalTos();
                        }
                        machine.popArgs(frame, 3);
                        machine.auxIntArg(0x32132);
                    }
                    break;
                }
                case ByteOps.DUP2_X2: {
                    ExecutionStack stack = frame.getStack();

                    if (stack.peekType(0).isCategory2()) {
                        if (stack.peekType(2).isCategory2()) {
                            // "form 4" in vmspec-2
                            machine.popArgs(frame, 2);
                            machine.auxIntArg(0x212);
                        } else if (stack.peekType(3).isCategory1()) {
                            // "form 2"
                            machine.popArgs(frame, 3);
                            machine.auxIntArg(0x3213);
                        } else {
                            throw illegalTos();
                        }
                    } else if (stack.peekType(1).isCategory1()) {
                        if (stack.peekType(2).isCategory2()) {
                            // "form 3"
                            machine.popArgs(frame, 3);
                            machine.auxIntArg(0x32132);
                        } else if (stack.peekType(3).isCategory1()) {
                            // "form 1"
                            machine.popArgs(frame, 4);
                            machine.auxIntArg(0x432143);
                        } else {
                            throw illegalTos();
                        }
                    } else {
                        throw illegalTos();
                    }
                    break;
                }
                case ByteOps.SWAP: {
                    ExecutionStack stack = frame.getStack();

                    if (!(stack.peekType(0).isCategory1() &&
                          stack.peekType(1).isCategory1())) {
                        throw illegalTos();
                    }

                    machine.popArgs(frame, 2);
                    machine.auxIntArg(0x12);
                    break;
                }
                default: {
                    visitInvalid(opcode, offset, length);
                    return;
                }
            }

            machine.auxType(type);
            machine.run(frame, offset, opcode);
        }

        /**
         * Checks whether the prototype is compatible with returning the
         * given type, and throws if not.
         *
         * @param encountered {@code non-null;} the encountered return type
         */
        private void checkReturnType(Type encountered) {
            Type returnType = machine.getPrototype().getReturnType();

            /*
             * Check to see if the prototype's return type is
             * possibly assignable from the type we encountered. This
             * takes care of all the salient cases (types are the same,
             * they're compatible primitive types, etc.).
             */
            if (!Merger.isPossiblyAssignableFrom(returnType, encountered)) {
                fail("return type mismatch: prototype " +
                     "indicates " + returnType.toHuman() +
                     ", but encountered type " + encountered.toHuman());
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitLocal(int opcode, int offset, int length,
                int idx, Type type, int value) {
            /*
             * Note that the "type" parameter is always the simplest
             * type based on the original opcode, e.g., "int" for
             * "iload" (per se) and "Object" for "aload". So, when
             * possible, we replace the type with the one indicated in
             * the local variable table, though we still need to check
             * to make sure it's valid for the opcode.
             *
             * The reason we use (offset + length) for the localOffset
             * for a store is because it is only after the store that
             * the local type becomes valid. On the other hand, the
             * type associated with a load is valid at the start of
             * the instruction.
             */
            int localOffset =
                (opcode == ByteOps.ISTORE) ? (offset + length) : offset;
            LocalVariableList.Item local =
                localVariables.pcAndIndexToLocal(localOffset, idx);
            Type localType;

            if (local != null) {
                localType = local.getType();
                if (localType.getBasicFrameType() !=
                        type.getBasicFrameType()) {
                    // wrong type, ignore local variable info
                    local = null;
                    localType = type;
                }
            } else {
                localType = type;
            }

            switch (opcode) {
                case ByteOps.ILOAD:
                case ByteOps.RET: {
                    machine.localArg(frame, idx);
                    machine.localInfo(local != null);
                    machine.auxType(type);
                    break;
                }
                case ByteOps.ISTORE: {
                    LocalItem item
                            = (local == null) ? null : local.getLocalItem();
                    machine.popArgs(frame, type);
                    machine.auxType(type);
                    machine.localTarget(idx, localType, item);
                    break;
                }
                case ByteOps.IINC: {
                    LocalItem item
                            = (local == null) ? null : local.getLocalItem();
                    machine.localArg(frame, idx);
                    machine.localTarget(idx, localType, item);
                    machine.auxType(type);
                    machine.auxIntArg(value);
                    machine.auxCstArg(CstInteger.make(value));
                    break;
                }
                default: {
                    visitInvalid(opcode, offset, length);
                    return;
                }
            }

            machine.run(frame, offset, opcode);
        }

        /** {@inheritDoc} */
        @Override
        public void visitConstant(int opcode, int offset, int length,
                Constant cst, int value) {
            switch (opcode) {
                case ByteOps.ANEWARRAY: {
                    machine.popArgs(frame, Type.INT);
                    break;
                }
                case ByteOps.PUTSTATIC: {
                    Type fieldType = ((CstFieldRef) cst).getType();
                    machine.popArgs(frame, fieldType);
                    break;
                }
                case ByteOps.GETFIELD:
                case ByteOps.CHECKCAST:
                case ByteOps.INSTANCEOF: {
                    machine.popArgs(frame, Type.OBJECT);
                    break;
                }
                case ByteOps.PUTFIELD: {
                    Type fieldType = ((CstFieldRef) cst).getType();
                    machine.popArgs(frame, Type.OBJECT, fieldType);
                    break;
                }
                case ByteOps.INVOKEINTERFACE:
                case ByteOps.INVOKEVIRTUAL:
                case ByteOps.INVOKESPECIAL:
                case ByteOps.INVOKESTATIC: {
                    /*
                     * Convert the interface method ref into a normal
                     * method ref if necessary.
                     */
                    if (cst instanceof CstInterfaceMethodRef) {
                        cst = ((CstInterfaceMethodRef) cst).toMethodRef();
                        checkInvokeInterfaceSupported(opcode, (CstMethodRef) cst);
                    }

                    /*
                     * Check whether invoke-polymorphic is required and supported.
                     */
                    if (cst instanceof CstMethodRef) {
                        CstMethodRef methodRef = (CstMethodRef) cst;
                        if (methodRef.isSignaturePolymorphic()) {
                            checkInvokeSignaturePolymorphic(opcode);
                        }
                    }

                    /*
                     * Get the instance or static prototype, and use it to
                     * direct the machine.
                     */
                    boolean staticMethod = (opcode == ByteOps.INVOKESTATIC);
                    Prototype prototype
                        = ((CstMethodRef) cst).getPrototype(staticMethod);
                    machine.popArgs(frame, prototype);
                    break;
                }
                case ByteOps.INVOKEDYNAMIC: {
                    checkInvokeDynamicSupported(opcode);
                    CstInvokeDynamic invokeDynamicRef = (CstInvokeDynamic) cst;
                    Prototype prototype = invokeDynamicRef.getPrototype();
                    machine.popArgs(frame, prototype);
                    // Change the constant to be associated with instruction to
                    // a call site reference.
                    cst = invokeDynamicRef.addReference();
                    break;
                }
                case ByteOps.MULTIANEWARRAY: {
                    /*
                     * The "value" here is the count of dimensions to
                     * create. Make a prototype of that many "int"
                     * types, and tell the machine to pop them. This
                     * isn't the most efficient way in the world to do
                     * this, but then again, multianewarray is pretty
                     * darn rare and so not worth much effort
                     * optimizing for.
                     */
                    Prototype prototype =
                        Prototype.internInts(Type.VOID, value);
                    machine.popArgs(frame, prototype);
                    break;
                }
                case ByteOps.LDC:
                case ByteOps.LDC_W: {
                    if ((cst instanceof CstMethodHandle || cst instanceof CstProtoRef)) {
                        checkConstMethodHandleSupported(cst);
                    }
                    machine.clearArgs();
                    break;
                }
                default: {
                    machine.clearArgs();
                    break;
                }
            }

            machine.auxIntArg(value);
            machine.auxCstArg(cst);
            machine.run(frame, offset, opcode);
        }

        /** {@inheritDoc} */
        @Override
        public void visitBranch(int opcode, int offset, int length,
                int target) {
            switch (opcode) {
                case ByteOps.IFEQ:
                case ByteOps.IFNE:
                case ByteOps.IFLT:
                case ByteOps.IFGE:
                case ByteOps.IFGT:
                case ByteOps.IFLE: {
                    machine.popArgs(frame, Type.INT);
                    break;
                }
                case ByteOps.IFNULL:
                case ByteOps.IFNONNULL: {
                    machine.popArgs(frame, Type.OBJECT);
                    break;
                }
                case ByteOps.IF_ICMPEQ:
                case ByteOps.IF_ICMPNE:
                case ByteOps.IF_ICMPLT:
                case ByteOps.IF_ICMPGE:
                case ByteOps.IF_ICMPGT:
                case ByteOps.IF_ICMPLE: {
                    machine.popArgs(frame, Type.INT, Type.INT);
                    break;
                }
                case ByteOps.IF_ACMPEQ:
                case ByteOps.IF_ACMPNE: {
                    machine.popArgs(frame, Type.OBJECT, Type.OBJECT);
                    break;
                }
                case ByteOps.GOTO:
                case ByteOps.JSR:
                case ByteOps.GOTO_W:
                case ByteOps.JSR_W: {
                    machine.clearArgs();
                    break;
                }
                default: {
                    visitInvalid(opcode, offset, length);
                    return;
                }
            }

            machine.auxTargetArg(target);
            machine.run(frame, offset, opcode);
        }

        /** {@inheritDoc} */
        @Override
        public void visitSwitch(int opcode, int offset, int length,
                SwitchList cases, int padding) {
            machine.popArgs(frame, Type.INT);
            machine.auxIntArg(padding);
            machine.auxSwitchArg(cases);
            machine.run(frame, offset, opcode);
        }

        /** {@inheritDoc} */
        @Override
        public void visitNewarray(int offset, int length, CstType type,
                ArrayList<Constant> initValues) {
            machine.popArgs(frame, Type.INT);
            machine.auxInitValues(initValues);
            machine.auxCstArg(type);
            machine.run(frame, offset, ByteOps.NEWARRAY);
        }

        /** {@inheritDoc} */
        @Override
        public void setPreviousOffset(int offset) {
            previousOffset = offset;
        }

        /** {@inheritDoc} */
        @Override
        public int getPreviousOffset() {
            return previousOffset;
        }
    }

    private void checkConstMethodHandleSupported(Constant cst) throws SimException {
        if (!dexOptions.apiIsSupported(DexFormat.API_CONST_METHOD_HANDLE)) {
            fail(String.format("invalid constant type %s requires --min-sdk-version >= %d " +
                               "(currently %d)",
                               cst.typeName(), DexFormat.API_CONST_METHOD_HANDLE,
                               dexOptions.minSdkVersion));
        }
    }

    private void checkInvokeDynamicSupported(int opcode) throws SimException {
        if (!dexOptions.apiIsSupported(DexFormat.API_METHOD_HANDLES)) {
            fail(String.format("invalid opcode %02x - invokedynamic requires " +
                               "--min-sdk-version >= %d (currently %d)",
                               opcode, DexFormat.API_METHOD_HANDLES, dexOptions.minSdkVersion));
        }
    }

    private void checkInvokeInterfaceSupported(final int opcode, CstMethodRef callee) {
        if (opcode == ByteOps.INVOKEINTERFACE) {
            // Invoked in the tranditional way, this is fine.
            return;
        }

        if (dexOptions.apiIsSupported(DexFormat.API_INVOKE_INTERFACE_METHODS)) {
            // Running at the officially support API level for default
            // and static interface methods.
            return;
        }

        //
        // One might expect a hard API level for invoking interface
        // methods. It's either okay to have code invoking static (and
        // default) interface methods or not. Reality asks to be
        // prepared for a little compromise here because the
        // traditional guidance to Android developers when producing a
        // multi-API level DEX file is to guard the use of the newer
        // feature with an API level check, e.g.
        //
        // int x = (android.os.Build.VERSION.SDK_INT >= 038) ?
        //         DoJava8Thing() : Do JavaOtherThing();
        //
        // This is fine advice if the bytecodes and VM semantics never
        // change. Unfortunately, changes like Java 8 support
        // introduce new bytecodes and also additional semantics to
        // existing bytecodes. Invoking static and default interface
        // methods is one of these awkward VM transitions.
        //
        // Experimentally invoke-static of static interface methods
        // breaks on VMs running below API level 21. Invocations of
        // default interface methods may soft-fail verification but so
        // long as they are not called that's okay.
        //
        boolean softFail = dexOptions.allowAllInterfaceMethodInvokes;
        if (opcode == ByteOps.INVOKESTATIC) {
            softFail &= dexOptions.apiIsSupported(DexFormat.API_INVOKE_STATIC_INTERFACE_METHODS);
        } else {
            assert (opcode == ByteOps.INVOKESPECIAL) || (opcode == ByteOps.INVOKEVIRTUAL);
        }

        // Running below the officially supported API level. Fail hard
        // unless the user has explicitly allowed this with
        // "--allow-all-interface-method-invokes".
        String invokeKind = (opcode == ByteOps.INVOKESTATIC) ? "static" : "default";
        if (softFail) {
            // The code we are warning about here should have an API check
            // that protects it being used on API version < API_INVOKE_INTERFACE_METHODS.
            String reason =
                    String.format(
                        "invoking a %s interface method %s.%s strictly requires " +
                        "--min-sdk-version >= %d (experimental at current API level %d)",
                        invokeKind, callee.getDefiningClass().toHuman(), callee.getNat().toHuman(),
                        DexFormat.API_INVOKE_INTERFACE_METHODS, dexOptions.minSdkVersion);
            warn(reason);
        } else {
            String reason =
                    String.format(
                        "invoking a %s interface method %s.%s strictly requires " +
                        "--min-sdk-version >= %d (blocked at current API level %d)",
                    invokeKind, callee.getDefiningClass().toHuman(), callee.getNat().toHuman(),
                    DexFormat.API_INVOKE_INTERFACE_METHODS, dexOptions.minSdkVersion);
            fail(reason);
        }
    }

    private void checkInterfaceMethodDeclaration(ConcreteMethod declaredMethod) {
        if (!dexOptions.apiIsSupported(DexFormat.API_DEFINE_INTERFACE_METHODS)) {
            String reason
                = String.format(
                    "defining a %s interface method requires --min-sdk-version >= %d (currently %d)"
                    + " for interface methods: %s.%s",
                    declaredMethod.isStaticMethod() ? "static" : "default",
                    DexFormat.API_DEFINE_INTERFACE_METHODS, dexOptions.minSdkVersion,
                    declaredMethod.getDefiningClass().toHuman(), declaredMethod.getNat().toHuman());
            warn(reason);
        }
    }

    private void checkInvokeSignaturePolymorphic(final int opcode) {
        if (!dexOptions.apiIsSupported(DexFormat.API_METHOD_HANDLES)) {
            fail(String.format(
                "invoking a signature-polymorphic requires --min-sdk-version >= %d (currently %d)",
                DexFormat.API_METHOD_HANDLES, dexOptions.minSdkVersion));
        } else if (opcode != ByteOps.INVOKEVIRTUAL) {
            fail("Unsupported signature polymorphic invocation (" + ByteOps.opName(opcode) + ")");
        }
    }

    private void fail(String reason) {
        String message = String.format("ERROR in %s.%s: %s", method.getDefiningClass().toHuman(),
                                       method.getNat().toHuman(), reason);
        throw new SimException(message);
    }

    private void warn(String reason) {
        String warning = String.format("WARNING in %s.%s: %s", method.getDefiningClass().toHuman(),
                                       method.getNat().toHuman(), reason);
        dexOptions.err.println(warning);
    }
}

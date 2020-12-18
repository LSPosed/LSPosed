/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx;

import external.com.android.dx.rop.code.BasicBlockList;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.PlainCstInsn;
import external.com.android.dx.rop.code.PlainInsn;
import external.com.android.dx.rop.code.RegOps;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.code.RegisterSpecList;
import external.com.android.dx.rop.code.Rop;
import external.com.android.dx.rop.code.Rops;
import external.com.android.dx.rop.code.SourcePosition;
import external.com.android.dx.rop.code.ThrowingCstInsn;
import external.com.android.dx.rop.code.ThrowingInsn;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static external.com.android.dx.rop.code.Rop.BRANCH_GOTO;
import static external.com.android.dx.rop.code.Rop.BRANCH_NONE;
import static external.com.android.dx.rop.code.Rop.BRANCH_RETURN;
import static external.com.android.dx.rop.type.Type.BT_BYTE;
import static external.com.android.dx.rop.type.Type.BT_CHAR;
import static external.com.android.dx.rop.type.Type.BT_INT;
import static external.com.android.dx.rop.type.Type.BT_SHORT;

/**
 * Builds a sequence of instructions.
 *
 * <h3>Locals</h3>
 * All data manipulation takes place in local variables. Each parameter gets its
 * own local by default; access these using {@link #getParameter
 * getParameter()}. Non-static methods and constructors also have a {@code this}
 * parameter; it's available as {@link #getThis getThis()}. Allocate a new local
 * variable using {@link #newLocal newLocal()}, and assign a default value to it
 * with {@link #loadConstant loadConstant()}. Copy a value from one local to
 * another with {@link #move move()}.
 *
 * <p>Every local variable has a fixed type. This is either a primitive type (of
 * any size) or a reference type.  This class emits instructions appropriate to
 * the types they operate on. Not all operations are local on all types;
 * attempting to emit such an operation will fail with an unchecked exception.
 *
 * <h3>Math and Bit Operations</h3>
 * Transform a single value into another related value using {@link
 * #op(UnaryOp,Local,Local) op(UnaryOp, Local, Local)}. Transform two values
 * into a third value using {@link #op(BinaryOp,Local,Local,Local) op(BinaryOp,
 * Local, Local, Local)}. In either overload the first {@code Local} parameter
 * is where the result will be sent; the other {@code Local} parameters are the
 * inputs.
 *
 * <h3>Comparisons</h3>
 * There are three different comparison operations each with different
 * constraints:
 * <ul>
 *     <li>{@link #compareLongs compareLongs()} compares two locals each
 *         containing a {@code long} primitive. This is the only operation that
 *         can compare longs. The result of the comparison is written to another
 *         {@code int} local.</li>
 *     <li>{@link #compareFloatingPoint compareFloatingPoint()} compares two
 *         locals; both {@code float} primitives or both {@code double}
 *         primitives. This is the only operation that can compare floating
 *         point values. This comparison takes an extra parameter that sets
 *         the desired result if either parameter is {@code NaN}. The result of
 *         the comparison is wrtten to another {@code int} local.
 *     <li>{@link #compare compare()} compares two locals. The {@link
 *         Comparison#EQ} and {@link Comparison#NE} options compare either
 *         {@code int} primitives or references. The other options compare only
 *         {@code int} primitives. This comparison takes a {@link Label} that
 *         will be jumped to if the comparison is true. If the comparison is
 *         false the next instruction in sequence will be executed.
 * </ul>
 * There's no single operation to compare longs and jump, or to compare ints and
 * store the result in a local. Accomplish these goals by chaining multiple
 * operations together.
 *
 * <h3>Branches, Labels and Returns</h3>
 * Basic control flow is expressed using jumps and labels. Each label must be
 * marked exactly once and may be jumped to any number of times. Create a label
 * using its constructor: {@code new Label()}, and mark it using {@link #mark
 * mark(Label)}. All jumps to a label will execute instructions starting from
 * that label. You can jump to a label that hasn't yet been marked (jumping
 * forward) or to a label that has already been marked (jumping backward). Jump
 * unconditionally with {@link #jump jump(Label)} or conditionally based on a
 * comparison using {@link #compare compare()}.
 *
 * <p>Most methods should contain a return instruction. Void methods
 * should use {@link #returnVoid()}; non-void methods should use {@link
 * #returnValue returnValue()} with a local whose return type matches the
 * method's return type. Constructors are considered void methods and should
 * call {@link #returnVoid()}. Methods may make multiple returns. Methods
 * containing no return statements must either loop infinitely or throw
 * unconditionally; it is not legal to end a sequence of instructions without a
 * jump, return or throw.
 *
 * <h3>Throwing and Catching</h3>
 * This API uses labels to handle thrown exceptions, errors and throwables. Call
 * {@link #addCatchClause addCatchClause()} to register the target label and
 * throwable class. All statements that follow will jump to that catch clause if
 * they throw a {@link Throwable} assignable to that type. Use {@link
 * #removeCatchClause removeCatchClause()} to unregister the throwable class.
 *
 * <p>Throw an throwable by first assigning it to a local and then calling
 * {@link #throwValue throwValue()}. Control flow will jump to the nearest label
 * assigned to a type assignable to the thrown type. In this context, "nearest"
 * means the label requiring the fewest stack frames to be popped.
 *
 * <h3>Calling methods</h3>
 * A method's caller must know its return type, name, parameters, and invoke
 * kind. Lookup a method on a type using {@link TypeId#getMethod
 * TypeId.getMethod()}. This is more onerous than Java language invokes, which
 * can infer the target method using the target object and parameters. There are
 * four invoke kinds:
 * <ul>
 *     <li>{@link #invokeStatic invokeStatic()} is used for static methods.</li>
 *     <li>{@link #invokeDirect invokeDirect()} is used for private instance
 *         methods and for constructors to call their superclass's
 *         constructor.</li>
 *     <li>{@link #invokeInterface invokeInterface()} is used to invoke a method
 *         whose declaring type is an interface.</li>
 *     <li>{@link #invokeVirtual invokeVirtual()} is used to invoke any other
 *         method. The target must not be static, private, a constructor, or an
 *         interface method.</li>
 *     <li>{@link #invokeSuper invokeSuper()} is used to invoke the closest
 *         superclass's virtual method. The target must not be static, private,
 *         a constructor method, or an interface method.</li>
 *     <li>{@link #newInstance newInstance()} is used to invoke a
 *         constructor.</li>
 * </ul>
 * All invoke methods take a local for the return value. For void methods this
 * local is unused and may be null.
 *
 * <h3>Field Access</h3>
 * Read static fields using {@link #sget sget()}; write them using {@link
 * #sput sput()}. For instance values you'll need to specify the declaring
 * instance; use {@link #getThis getThis()} in an instance method to use {@code
 * this}. Read instance values using {@link #iget iget()} and write them with
 * {@link #iput iput()}.
 *
 * <h3>Array Access</h3>
 * Allocate an array using {@link #newArray newArray()}. Read an array's length
 * with {@link #arrayLength arrayLength()} and its elements with {@link #aget
 * aget()}. Write an array's elements with {@link #aput aput()}.
 *
 * <h3>Types</h3>
 * Use {@link #cast cast()} to perform either a <strong>numeric cast</strong> or
 * a <strong>type cast</strong>. Interrogate the type of a value in a local
 * using {@link #instanceOfType instanceOfType()}.
 *
 * <h3>Synchronization</h3>
 * Acquire a monitor using {@link #monitorEnter monitorEnter()}; release it with
 * {@link #monitorExit monitorExit()}. It is the caller's responsibility to
 * guarantee that enter and exit calls are balanced, even in the presence of
 * exceptions thrown.
 *
 * <strong>Warning:</strong> Even if a method has the {@code synchronized} flag,
 * dex requires instructions to acquire and release monitors manually. A method
 * declared with {@link java.lang.reflect.Modifier#SYNCHRONIZED SYNCHRONIZED}
 * but without manual calls to {@code monitorEnter()} and {@code monitorExit()}
 * will not be synchronized when executed.
 */
public final class Code {
    private final MethodId<?, ?> method;
    /**
     * All allocated labels. Although the order of the labels in this list
     * shouldn't impact behavior, it is used to determine basic block indices.
     */
    private final List<Label> labels = new ArrayList<Label>();

    /**
     * The label currently receiving instructions. This is null if the most
     * recent instruction was a return or goto.
     */
    private Label currentLabel;

    /** true once we've fixed the positions of the parameter registers */
    private boolean localsInitialized;

    private final Local<?> thisLocal;

    /**
     * The parameters on this method. If this is non-static, the first parameter
     * is 'thisLocal' and we have to offset the user's indices by one.
     */
    private final List<Local<?>> parameters = new ArrayList<Local<?>>();
    private final List<Local<?>> locals = new ArrayList<Local<?>>();
    private SourcePosition sourcePosition = SourcePosition.NO_INFO;
    private final List<TypeId<?>> catchTypes = new ArrayList<TypeId<?>>();
    private final List<Label> catchLabels = new ArrayList<Label>();
    private StdTypeList catches = StdTypeList.EMPTY;

    Code(DexMaker.MethodDeclaration methodDeclaration) {
        this.method = methodDeclaration.method;
        if (methodDeclaration.isStatic()) {
            thisLocal = null;
        } else {
            thisLocal = Local.get(this, method.declaringType);
            parameters.add(thisLocal);
        }
        for (TypeId<?> parameter : method.parameters.types) {
            parameters.add(Local.get(this, parameter));
        }
        this.currentLabel = new Label();
        adopt(this.currentLabel);
        this.currentLabel.marked = true;
    }

    /**
     * Allocates a new local variable of type {@code type}. It is an error to
     * allocate a local after instructions have been emitted.
     */
    public <T> Local<T> newLocal(TypeId<T> type) {
        if (localsInitialized) {
            throw new IllegalStateException("Cannot allocate locals after adding instructions");
        }
        Local<T> result = Local.get(this, type);
        locals.add(result);
        return result;
    }

    /**
     * Returns the local for the parameter at index {@code index} and of type
     * {@code type}.
     */
    public <T> Local<T> getParameter(int index, TypeId<T> type) {
        if (thisLocal != null) {
            index++; // adjust for the hidden 'this' parameter
        }
        return coerce(parameters.get(index), type);
    }

    /**
     * Returns the local for {@code this} of type {@code type}. It is an error
     * to call {@code getThis()} if this is a static method.
     */
    public <T> Local<T> getThis(TypeId<T> type) {
        if (thisLocal == null) {
            throw new IllegalStateException("static methods cannot access 'this'");
        }
        return coerce(thisLocal, type);
    }

    @SuppressWarnings("unchecked") // guarded by an equals check
    private <T> Local<T> coerce(Local<?> local, TypeId<T> expectedType) {
        if (!local.type.equals(expectedType)) {
            throw new IllegalArgumentException(
                    "requested " + expectedType + " but was " + local.type);
        }
        return (Local<T>) local;
    }

    /**
     * Assigns registers to locals. From the spec:
     *  "the N arguments to a method land in the last N registers of the
     *   method's invocation frame, in order. Wide arguments consume two
     *   registers. Instance methods are passed a this reference as their
     *   first argument."
     *
     * In addition to assigning registers to each of the locals, this creates
     * instructions to move parameters into their initial registers. These
     * instructions are inserted before the code's first real instruction.
     */
    void initializeLocals() {
        if (localsInitialized) {
            throw new AssertionError();
        }
        localsInitialized = true;

        int reg = 0;
        for (Local<?> local : locals) {
            reg += local.initialize(reg);
        }
        int firstParamReg = reg;
        List<Insn> moveParameterInstructions = new ArrayList<Insn>();
        for (Local<?> local : parameters) {
            CstInteger paramConstant = CstInteger.make(reg - firstParamReg);
            reg += local.initialize(reg);
            moveParameterInstructions.add(new PlainCstInsn(Rops.opMoveParam(local.type.ropType),
                    sourcePosition, local.spec(), RegisterSpecList.EMPTY, paramConstant));
        }
        labels.get(0).instructions.addAll(0, moveParameterInstructions);
    }

    /**
     * Returns the number of registers to hold the parameters. This includes the
     * 'this' parameter if it exists.
     */
    int paramSize() {
        int result = 0;
        for (Local<?> local : parameters) {
            result += local.size();
        }
        return result;
    }

    // labels

    /**
     * Assigns {@code target} to this code.
     */
    private void adopt(Label target) {
        if (target.code == this) {
            return; // already adopted
        }
        if (target.code != null) {
            throw new IllegalArgumentException("Cannot adopt label; it belongs to another Code");
        }
        target.code = this;
        labels.add(target);
    }

    /**
     * Start defining instructions for the named label.
     */
    public void mark(Label label) {
        adopt(label);
        if (label.marked) {
            throw new IllegalStateException("already marked");
        }
        label.marked = true;
        if (currentLabel != null) {
            jump(label); // blocks must end with a branch, return or throw
        }
        currentLabel = label;
    }

    /**
     * Transfers flow control to the instructions at {@code target}. It is an
     * error to jump to a label not marked on this {@code Code}.
     */
    public void jump(Label target) {
        adopt(target);
        addInstruction(new PlainInsn(Rops.GOTO, sourcePosition, null, RegisterSpecList.EMPTY),
                target);
    }

    /**
     * Registers {@code catchClause} as a branch target for all instructions
     * in this frame that throw a class assignable to {@code toCatch}. This
     * includes methods invoked from this frame. Deregister the clause using
     * {@link #removeCatchClause removeCatchClause()}. It is an error to
     * register a catch clause without also {@link #mark marking it} in the same
     * {@code Code} instance.
     */
    public void addCatchClause(TypeId<? extends Throwable> toCatch, Label catchClause) {
        if (catchTypes.contains(toCatch)) {
            throw new IllegalArgumentException("Already caught: " + toCatch);
        }
        adopt(catchClause);
        catchTypes.add(toCatch);
        catches = toTypeList(catchTypes);
        catchLabels.add(catchClause);
    }

    /**
     * Deregisters the catch clause label for {@code toCatch} and returns it.
     */
    public Label removeCatchClause(TypeId<? extends Throwable> toCatch) {
        int index = catchTypes.indexOf(toCatch);
        if (index == -1) {
            throw new IllegalArgumentException("No catch clause: " + toCatch);
        }
        catchTypes.remove(index);
        catches = toTypeList(catchTypes);
        return catchLabels.remove(index);
    }

    public void moveException(Local<?> result) {
        addInstruction(new PlainInsn(Rops.opMoveException(Type.THROWABLE),
                SourcePosition.NO_INFO, result.spec(), RegisterSpecList.EMPTY));
    }

    /**
     * Throws the throwable in {@code toThrow}.
     */
    public void throwValue(Local<? extends Throwable> toThrow) {
        addInstruction(new ThrowingInsn(Rops.THROW, sourcePosition,
                RegisterSpecList.make(toThrow.spec()), catches));
    }

    private StdTypeList toTypeList(List<TypeId<?>> types) {
        StdTypeList result = new StdTypeList(types.size());
        for (int i = 0; i < types.size(); i++) {
            result.set(i, types.get(i).ropType);
        }
        return result;
    }

    private void addInstruction(Insn insn) {
        addInstruction(insn, null);
    }

    /**
     * @param branch the branches to follow; interpretation depends on the
     *     instruction's branchingness.
     */
    private void addInstruction(Insn insn, Label branch) {
        if (currentLabel == null || !currentLabel.marked) {
            throw new IllegalStateException("no current label");
        }
        currentLabel.instructions.add(insn);

        switch (insn.getOpcode().getBranchingness()) {
        case BRANCH_NONE:
            if (branch != null) {
                throw new IllegalArgumentException("unexpected branch: " + branch);
            }
            return;

        case BRANCH_RETURN:
            if (branch != null) {
                throw new IllegalArgumentException("unexpected branch: " + branch);
            }
            currentLabel = null;
            break;

        case BRANCH_GOTO:
            if (branch == null) {
                throw new IllegalArgumentException("branch == null");
            }
            currentLabel.primarySuccessor = branch;
            currentLabel = null;
            break;

        case Rop.BRANCH_IF:
            if (branch == null) {
                throw new IllegalArgumentException("branch == null");
            }
            splitCurrentLabel(branch, Collections.<Label>emptyList());
            break;

        case Rop.BRANCH_THROW:
            if (branch != null) {
                throw new IllegalArgumentException("unexpected branch: " + branch);
            }
            splitCurrentLabel(null, new ArrayList<Label>(catchLabels));
            break;

        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Closes the current label and starts a new one.
     *
     * @param catchLabels an immutable list of catch labels
     */
    private void splitCurrentLabel(Label alternateSuccessor, List<Label> catchLabels) {
        Label newLabel = new Label();
        adopt(newLabel);
        currentLabel.primarySuccessor = newLabel;
        currentLabel.alternateSuccessor = alternateSuccessor;
        currentLabel.catchLabels = catchLabels;
        currentLabel = newLabel;
        currentLabel.marked = true;
    }

    // instructions: locals

    /**
     * Copies the constant value {@code value} to {@code target}. The constant
     * must be a primitive, String, Class, TypeId, or null.
     */
    public <T> void loadConstant(Local<T> target, T value) {
        loadConstantInternal(target, value);
    }

    /**
     * Copies a class type in {@code target}. The benefit to using this method vs {@link Code#loadConstant(Local, Object)}
     * is that the {@code value} can itself be a generated type - {@link TypeId} allows for deferred referencing of class types.
     */
    public void loadDeferredClassConstant(Local<Class> target, TypeId value) {
        loadConstantInternal(target, value);
    }

    private void loadConstantInternal(Local target, Object value) {
        Rop rop = value == null
                  ? Rops.CONST_OBJECT_NOTHROW
                  : Rops.opConst(target.type.ropType);
        if (rop.getBranchingness() == BRANCH_NONE) {
            addInstruction(new PlainCstInsn(rop, sourcePosition, target.spec(),
                                            RegisterSpecList.EMPTY, Constants.getConstant(value)));
        } else {
            addInstruction(new ThrowingCstInsn(rop, sourcePosition,
                                               RegisterSpecList.EMPTY, catches, Constants.getConstant(value)));
            moveResult(target, true);
        }
    }

    /**
     * Copies the value in {@code source} to {@code target}.
     */
    public <T> void move(Local<T> target, Local<T> source) {
        addInstruction(new PlainInsn(Rops.opMove(source.type.ropType),
                sourcePosition, target.spec(), source.spec()));
    }

    // instructions: unary and binary

    /**
     * Executes {@code op} and sets {@code target} to the result.
     */
    public <T> void op(UnaryOp op, Local<T> target, Local<T> source) {
        addInstruction(new PlainInsn(op.rop(source.type), sourcePosition,
                target.spec(), source.spec()));
    }

    /**
     * Executes {@code op} and sets {@code target} to the result. For most
     * binary operations, the types of {@code a} and {@code b} must be the same.
     * Shift operations (like {@link BinaryOp#SHIFT_LEFT}) require {@code b} to
     * be an {@code int}, even when {@code a} is a {@code long}.
     */
    public <T1, T2> void op(BinaryOp op, Local<T1> target, Local<T1> a, Local<T2> b) {
        Rop rop = op.rop(StdTypeList.make(a.type.ropType, b.type.ropType));
        RegisterSpecList sources = RegisterSpecList.make(a.spec(), b.spec());

        if (rop.getBranchingness() == BRANCH_NONE) {
            addInstruction(new PlainInsn(rop, sourcePosition, target.spec(), sources));
        } else {
            addInstruction(new ThrowingInsn(rop, sourcePosition, sources, catches));
            moveResult(target, true);
        }
    }

    // instructions: branches

    /**
     * Compare ints or references. If the comparison is true, execution jumps to
     * {@code trueLabel}. If it is false, execution continues to the next
     * instruction.
     */
    public <T> void compare(Comparison comparison, Label trueLabel, Local<T> a, Local<T> b) {
        adopt(trueLabel);
        Rop rop = comparison.rop(StdTypeList.make(a.type.ropType, b.type.ropType));
        addInstruction(new PlainInsn(rop, sourcePosition, null,
                RegisterSpecList.make(a.spec(), b.spec())), trueLabel);
    }

    /**
     * Check if an int or reference equals to zero. If the comparison is true,
     * execution jumps to {@code trueLabel}. If it is false, execution continues to
     * the next instruction.
     */
    public <T> void compareZ(Comparison comparison, Label trueLabel, Local<?> a) {
        adopt(trueLabel);
        Rop rop = comparison.rop(StdTypeList.make(a.type.ropType));
        addInstruction(new PlainInsn(rop, sourcePosition, null,
                RegisterSpecList.make(a.spec())), trueLabel);
    }

    /**
     * Compare floats or doubles. This stores -1 in {@code target} if {@code
     * a < b}, 0 in {@code target} if {@code a == b} and 1 in target if {@code
     * a > b}. This stores {@code nanValue} in {@code target} if either value
     * is {@code NaN}.
     */
    public <T extends Number> void compareFloatingPoint(
            Local<Integer> target, Local<T> a, Local<T> b, int nanValue) {
        Rop rop;
        if (nanValue == 1) {
            rop = Rops.opCmpg(a.type.ropType);
        } else if (nanValue == -1) {
            rop = Rops.opCmpl(a.type.ropType);
        } else {
            throw new IllegalArgumentException("expected 1 or -1 but was " + nanValue);
        }
        addInstruction(new PlainInsn(rop, sourcePosition, target.spec(),
                RegisterSpecList.make(a.spec(), b.spec())));
    }

    /**
     * Compare longs. This stores -1 in {@code target} if {@code
     * a < b}, 0 in {@code target} if {@code a == b} and 1 in target if {@code
     * a > b}.
     */
    public void compareLongs(Local<Integer> target, Local<Long> a, Local<Long> b) {
        addInstruction(new PlainInsn(Rops.CMPL_LONG, sourcePosition, target.spec(),
                RegisterSpecList.make(a.spec(), b.spec())));
    }

    // instructions: fields

    /**
     * Copies the value in instance field {@code fieldId} of {@code instance} to
     * {@code target}.
     */
    public <D, V> void iget(FieldId<D, ? extends V> fieldId, Local<V> target, Local<D> instance) {
        addInstruction(new ThrowingCstInsn(Rops.opGetField(target.type.ropType), sourcePosition,
                RegisterSpecList.make(instance.spec()), catches, fieldId.constant));
        moveResult(target, true);
    }

    /**
     * Copies the value in {@code source} to the instance field {@code fieldId}
     * of {@code instance}.
     */
   public <D, V> void iput(FieldId<D, V> fieldId, Local<? extends D> instance, Local<? extends V> source) {
        addInstruction(new ThrowingCstInsn(Rops.opPutField(source.type.ropType), sourcePosition,
                RegisterSpecList.make(source.spec(), instance.spec()), catches, fieldId.constant));
    }

    /**
     * Copies the value in the static field {@code fieldId} to {@code target}.
     */
    public <V> void sget(FieldId<?, ? extends V> fieldId, Local<V> target) {
        addInstruction(new ThrowingCstInsn(Rops.opGetStatic(target.type.ropType), sourcePosition,
                RegisterSpecList.EMPTY, catches, fieldId.constant));
        moveResult(target, true);
    }

    /**
     * Copies the value in {@code source} to the static field {@code fieldId}.
     */
    public <V> void sput(FieldId<?, V> fieldId, Local<? extends V> source) {
        addInstruction(new ThrowingCstInsn(Rops.opPutStatic(source.type.ropType), sourcePosition,
                RegisterSpecList.make(source.spec()), catches, fieldId.constant));
    }

    // instructions: invoke

    /**
     * Calls the constructor {@code constructor} using {@code args} and assigns
     * the new instance to {@code target}.
     */
    public <T> void newInstance(Local<T> target, MethodId<T, Void> constructor, Local<?>... args) {
        if (target == null) {
            throw new IllegalArgumentException();
        }
        addInstruction(new ThrowingCstInsn(Rops.NEW_INSTANCE, sourcePosition,
                RegisterSpecList.EMPTY, catches, constructor.declaringType.constant));
        moveResult(target, true);
        invokeDirect(constructor, null, target, args);
    }

    /**
     * Calls the static method {@code method} using {@code args} and assigns the
     * result to {@code target}.
     *
     * @param target the local to receive the method's return value, or {@code
     *     null} if the return type is {@code void} or if its value not needed.
     */
    public <R> void invokeStatic(MethodId<?, R> method, Local<? super R> target, Local<?>... args) {
        invoke(Rops.opInvokeStatic(method.prototype(true)), method, target, null, args);
    }

    /**
     * Calls the non-private instance method {@code method} of {@code instance}
     * using {@code args} and assigns the result to {@code target}.
     *
     * @param method a non-private, non-static, method declared on a class. May
     *     not be an interface method or a constructor.
     * @param target the local to receive the method's return value, or {@code
     *     null} if the return type is {@code void} or if its value not needed.
     */
    public <D, R> void invokeVirtual(MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> instance, Local<?>... args) {
        invoke(Rops.opInvokeVirtual(method.prototype(true)), method, target, instance, args);
    }

    /**
     * Calls {@code method} of {@code instance} using {@code args} and assigns
     * the result to {@code target}.
     *
     * @param method either a private method or the superclass's constructor in
     *     a constructor's call to {@code super()}.
     * @param target the local to receive the method's return value, or {@code
     *     null} if the return type is {@code void} or if its value not needed.
     */
    public <D, R> void invokeDirect(MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> instance, Local<?>... args) {
        invoke(Rops.opInvokeDirect(method.prototype(true)), method, target, instance, args);
    }

    /**
     * Calls the closest superclass's virtual method {@code method} of {@code
     * instance} using {@code args} and assigns the result to {@code target}.
     *
     * @param target the local to receive the method's return value, or {@code
     *     null} if the return type is {@code void} or if its value not needed.
     */
    public <D, R> void invokeSuper(MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> instance, Local<?>... args) {
        invoke(Rops.opInvokeSuper(method.prototype(true)), method, target, instance, args);
    }

    /**
     * Calls the interface method {@code method} of {@code instance} using
     * {@code args} and assigns the result to {@code target}.
     *
     * @param method a method declared on an interface.
     * @param target the local to receive the method's return value, or {@code
     *     null} if the return type is {@code void} or if its value not needed.
     */
    public <D, R> void invokeInterface(MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> instance, Local<?>... args) {
        invoke(Rops.opInvokeInterface(method.prototype(true)), method, target, instance, args);
    }

    private <D, R> void invoke(Rop rop, MethodId<D, R> method, Local<? super R> target,
            Local<? extends D> object, Local<?>... args) {
        addInstruction(new ThrowingCstInsn(rop, sourcePosition, concatenate(object, args),
                catches, method.constant));
        if (target != null) {
            moveResult(target, false);
        }
    }

    // instructions: types

    /**
     * Tests if the value in {@code source} is assignable to {@code type}. If it
     * is, {@code target} is assigned to 1; otherwise {@code target} is assigned
     * to 0.
     */
    public void instanceOfType(Local<?> target, Local<?> source, TypeId<?> type) {
        addInstruction(new ThrowingCstInsn(Rops.INSTANCE_OF, sourcePosition,
                RegisterSpecList.make(source.spec()), catches, type.constant));
        moveResult(target, true);
    }

    /**
     * Performs either a numeric cast or a type cast.
     *
     * <h3>Numeric Casts</h3>
     * Converts a primitive to a different representation. Numeric casts may
     * be lossy. For example, converting the double {@code 1.8d} to an integer
     * yields {@code 1}, losing the fractional part. Converting the integer
     * {@code 0x12345678} to a short yields {@code 0x5678}, losing the high
     * bytes. The following numeric casts are supported:
     *
     * <p><table border="1" summary="Supported Numeric Casts">
     * <tr><th>From</th><th>To</th></tr>
     * <tr><td>int</td><td>byte, char, short, long, float, double</td></tr>
     * <tr><td>long</td><td>int, float, double</td></tr>
     * <tr><td>float</td><td>int, long, double</td></tr>
     * <tr><td>double</td><td>int, long, float</td></tr>
     * </table>
     *
     * <p>For some primitive conversions it will be necessary to chain multiple
     * cast operations. For example, to go from float to short one would first
     * cast float to int and then int to short.
     *
     * <p>Numeric casts never throw {@link ClassCastException}.
     *
     * <h3>Type Casts</h3>
     * Checks that a reference value is assignable to the target type. If it is
     * assignable it is copied to the target local. If it is not assignable a
     * {@link ClassCastException} is thrown.
     */
    public void cast(Local<?> target, Local<?> source) {
        if (source.getType().ropType.isReference()) {
            addInstruction(new ThrowingCstInsn(Rops.CHECK_CAST, sourcePosition,
                    RegisterSpecList.make(source.spec()), catches, target.type.constant));
            moveResult(target, true);
        } else {
            addInstruction(new PlainInsn(getCastRop(source.type.ropType, target.type.ropType),
                    sourcePosition, target.spec(), source.spec()));
        }
    }

    private Rop getCastRop(external.com.android.dx.rop.type.Type sourceType,
            external.com.android.dx.rop.type.Type targetType) {
        if (sourceType.getBasicType() == BT_INT) {
            switch (targetType.getBasicType()) {
            case BT_SHORT:
                return Rops.TO_SHORT;
            case BT_CHAR:
                return Rops.TO_CHAR;
            case BT_BYTE:
                return Rops.TO_BYTE;
            }
        }
        return Rops.opConv(targetType, sourceType);
    }

    // instructions: arrays

    /**
     * Sets {@code target} to the length of the array in {@code array}.
     */
    public <T> void arrayLength(Local<Integer> target, Local<T> array) {
        addInstruction(new ThrowingInsn(Rops.ARRAY_LENGTH, sourcePosition,
                RegisterSpecList.make(array.spec()), catches));
        moveResult(target, true);
    }

    /**
     * Assigns {@code target} to a newly allocated array of length {@code
     * length}. The array's type is the same as {@code target}'s type.
     */
    public <T> void newArray(Local<T> target, Local<Integer> length) {
        addInstruction(new ThrowingCstInsn(Rops.opNewArray(target.type.ropType), sourcePosition,
                RegisterSpecList.make(length.spec()), catches, target.type.constant));
        moveResult(target, true);
    }

    /**
     * Assigns the element at {@code index} in {@code array} to {@code target}.
     */
    public void aget(Local<?> target, Local<?> array, Local<Integer> index) {
        addInstruction(new ThrowingInsn(Rops.opAget(target.type.ropType), sourcePosition,
                RegisterSpecList.make(array.spec(), index.spec()), catches));
        moveResult(target, true);
    }

    /**
     * Assigns {@code source} to the element at {@code index} in {@code array}.
     */
    public void aput(Local<?> array, Local<Integer> index, Local<?> source) {
        addInstruction(new ThrowingInsn(Rops.opAput(source.type.ropType), sourcePosition,
                RegisterSpecList.make(source.spec(), array.spec(), index.spec()), catches));
    }

    // instructions: return

    /**
     * Returns from a {@code void} method. After a return it is an error to
     * define further instructions after a return without first {@link #mark
     * marking} an existing unmarked label.
     */
    public void returnVoid() {
        if (!method.returnType.equals(TypeId.VOID)) {
            throw new IllegalArgumentException("declared " + method.returnType
                    + " but returned void");
        }
        addInstruction(new PlainInsn(Rops.RETURN_VOID, sourcePosition, null,
                RegisterSpecList.EMPTY));
    }

    /**
     * Returns the value in {@code result} to the calling method. After a return
     * it is an error to define further instructions after a return without
     * first {@link #mark marking} an existing unmarked label.
     */
    public void returnValue(Local<?> result) {
        if (!result.type.equals(method.returnType)) {
            // TODO: this is probably too strict.
            throw new IllegalArgumentException("declared " + method.returnType
                    + " but returned " + result.type);
        }
        addInstruction(new PlainInsn(Rops.opReturn(result.type.ropType), sourcePosition,
                null, RegisterSpecList.make(result.spec())));
    }

    private void moveResult(Local<?> target, boolean afterNonInvokeThrowingInsn) {
        Rop rop = afterNonInvokeThrowingInsn
                ? Rops.opMoveResultPseudo(target.type.ropType)
                : Rops.opMoveResult(target.type.ropType);
        addInstruction(new PlainInsn(rop, sourcePosition, target.spec(), RegisterSpecList.EMPTY));
    }

    // instructions; synchronized

    /**
     * Awaits the lock on {@code monitor}, and acquires it.
     */
    public void monitorEnter(Local<?> monitor) {
        addInstruction(new ThrowingInsn(Rops.MONITOR_ENTER, sourcePosition,
                RegisterSpecList.make(monitor.spec()), catches));
    }

    /**
     * Releases the held lock on {@code monitor}.
     */
    public void monitorExit(Local<?> monitor) {
        addInstruction(new ThrowingInsn(Rops.MONITOR_EXIT, sourcePosition,
                RegisterSpecList.make(monitor.spec()), catches));
    }

    // produce BasicBlocks for dex

    BasicBlockList toBasicBlocks() {
        if (!localsInitialized) {
            initializeLocals();
        }

        cleanUpLabels();

        BasicBlockList result = new BasicBlockList(labels.size());
        for (int i = 0; i < labels.size(); i++) {
            result.set(i, labels.get(i).toBasicBlock());
        }
        return result;
    }

    /**
     * Removes empty labels and assigns IDs to non-empty labels.
     */
    private void cleanUpLabels() {
        int id = 0;
        for (Iterator<Label> i = labels.iterator(); i.hasNext();) {
            Label label = i.next();
            if (label.isEmpty()) {
                i.remove();
            } else {
                label.compact();
                label.id = id++;
            }
        }
    }

    private static RegisterSpecList concatenate(Local<?> first, Local<?>[] rest) {
        int offset = (first != null) ? 1 : 0;
        RegisterSpecList result = new RegisterSpecList(offset + rest.length);
        if (first != null) {
            result.set(0, first.spec());
        }
        for (int i = 0; i < rest.length; i++) {
            result.set(i + offset, rest[i].spec());
        }
        return result;
    }
}

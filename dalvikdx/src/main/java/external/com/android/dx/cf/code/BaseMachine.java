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

import external.com.android.dx.rop.code.LocalItem;
import external.com.android.dx.rop.code.RegisterSpec;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import java.util.ArrayList;

/**
 * Base implementation of {@link Machine}.
 *
 * <p><b>Note:</b> For the most part, the documentation for this class
 * ignores the distinction between {@link Type} and {@link
 * TypeBearer}.</p>
 */
public abstract class BaseMachine implements Machine {
    /* {@code non-null;} the prototype for the associated method */
    private final Prototype prototype;

    /** {@code non-null;} primary arguments */
    private TypeBearer[] args;

    /** {@code >= 0;} number of primary arguments */
    private int argCount;

    /** {@code null-ok;} type of the operation, if salient */
    private Type auxType;

    /** auxiliary {@code int} argument */
    private int auxInt;

    /** {@code null-ok;} auxiliary constant argument */
    private Constant auxCst;

    /** auxiliary branch target argument */
    private int auxTarget;

    /** {@code null-ok;} auxiliary switch cases argument */
    private SwitchList auxCases;

    /** {@code null-ok;} auxiliary initial value list for newarray */
    private ArrayList<Constant> auxInitValues;

    /** {@code >= -1;} last local accessed */
    private int localIndex;

    /** specifies if local has info in the local variable table */
    private boolean localInfo;

    /** {@code null-ok;} local target spec, if salient and calculated */
    private RegisterSpec localTarget;

    /** {@code non-null;} results */
    private TypeBearer[] results;

    /**
     * {@code >= -1;} count of the results, or {@code -1} if no results
     * have been set
     */
    private int resultCount;

    /**
     * Constructs an instance.
     *
     * @param prototype {@code non-null;} the prototype for the
     * associated method
     */
    public BaseMachine(Prototype prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }

        this.prototype = prototype;
        args = new TypeBearer[10];
        results = new TypeBearer[6];
        clearArgs();
    }

    /** {@inheritDoc} */
    @Override
    public Prototype getPrototype() {
        return prototype;
    }

    /** {@inheritDoc} */
    @Override
    public final void clearArgs() {
        argCount = 0;
        auxType = null;
        auxInt = 0;
        auxCst = null;
        auxTarget = 0;
        auxCases = null;
        auxInitValues = null;
        localIndex = -1;
        localInfo = false;
        localTarget = null;
        resultCount = -1;
    }

    /** {@inheritDoc} */
    @Override
    public final void popArgs(Frame frame, int count) {
        ExecutionStack stack = frame.getStack();

        clearArgs();

        if (count > args.length) {
            // Grow args, and add a little extra room to grow even more.
            args = new TypeBearer[count + 10];
        }

        for (int i = count - 1; i >= 0; i--) {
            args[i] = stack.pop();
        }

        argCount = count;
    }

    /** {@inheritDoc} */
    @Override
    public void popArgs(Frame frame, Prototype prototype) {
        StdTypeList types = prototype.getParameterTypes();
        int size = types.size();

        // Use the above method to do the actual popping...
        popArgs(frame, size);

        // ...and then verify the popped types.

        for (int i = 0; i < size; i++) {
            if (! Merger.isPossiblyAssignableFrom(types.getType(i), args[i])) {
                throw new SimException("at stack depth " + (size - 1 - i) +
                        ", expected type " + types.getType(i).toHuman() +
                        " but found " + args[i].getType().toHuman());
            }
        }
    }

    @Override
    public final void popArgs(Frame frame, Type type) {
        // Use the above method to do the actual popping...
        popArgs(frame, 1);

        // ...and then verify the popped type.
        if (! Merger.isPossiblyAssignableFrom(type, args[0])) {
            throw new SimException("expected type " + type.toHuman() +
                    " but found " + args[0].getType().toHuman());
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void popArgs(Frame frame, Type type1, Type type2) {
        // Use the above method to do the actual popping...
        popArgs(frame, 2);

        // ...and then verify the popped types.

        if (! Merger.isPossiblyAssignableFrom(type1, args[0])) {
            throw new SimException("expected type " + type1.toHuman() +
                    " but found " + args[0].getType().toHuman());
        }

        if (! Merger.isPossiblyAssignableFrom(type2, args[1])) {
            throw new SimException("expected type " + type2.toHuman() +
                    " but found " + args[1].getType().toHuman());
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void popArgs(Frame frame, Type type1, Type type2,
            Type type3) {
        // Use the above method to do the actual popping...
        popArgs(frame, 3);

        // ...and then verify the popped types.

        if (! Merger.isPossiblyAssignableFrom(type1, args[0])) {
            throw new SimException("expected type " + type1.toHuman() +
                    " but found " + args[0].getType().toHuman());
        }

        if (! Merger.isPossiblyAssignableFrom(type2, args[1])) {
            throw new SimException("expected type " + type2.toHuman() +
                    " but found " + args[1].getType().toHuman());
        }

        if (! Merger.isPossiblyAssignableFrom(type3, args[2])) {
            throw new SimException("expected type " + type3.toHuman() +
                    " but found " + args[2].getType().toHuman());
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void localArg(Frame frame, int idx) {
        clearArgs();
        args[0] = frame.getLocals().get(idx);
        argCount = 1;
        localIndex = idx;
    }

    /** {@inheritDoc} */
    @Override
    public final void localInfo(boolean local) {
        localInfo = local;
    }

    /** {@inheritDoc} */
    @Override
    public final void auxType(Type type) {
        auxType = type;
    }

    /** {@inheritDoc} */
    @Override
    public final void auxIntArg(int value) {
        auxInt = value;
    }

    /** {@inheritDoc} */
    @Override
    public final void auxCstArg(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }

        auxCst = cst;
    }

    /** {@inheritDoc} */
    @Override
    public final void auxTargetArg(int target) {
        auxTarget = target;
    }

    /** {@inheritDoc} */
    @Override
    public final void auxSwitchArg(SwitchList cases) {
        if (cases == null) {
            throw new NullPointerException("cases == null");
        }

        auxCases = cases;
    }

    /** {@inheritDoc} */
    @Override
    public final void auxInitValues(ArrayList<Constant> initValues) {
        auxInitValues = initValues;
    }

    /** {@inheritDoc} */
    @Override
    public final void localTarget(int idx, Type type, LocalItem local) {
        localTarget = RegisterSpec.makeLocalOptional(idx, type, local);
    }

    /**
     * Gets the number of primary arguments.
     *
     * @return {@code >= 0;} the number of primary arguments
     */
    protected final int argCount() {
        return argCount;
    }

    /**
     * Gets the width of the arguments (where a category-2 value counts as
     * two).
     *
     * @return {@code >= 0;} the argument width
     */
    protected final int argWidth() {
        int result = 0;

        for (int i = 0; i < argCount; i++) {
            result += args[i].getType().getCategory();
        }

        return result;
    }

    /**
     * Gets the {@code n}th primary argument.
     *
     * @param n {@code >= 0, < argCount();} which argument
     * @return {@code non-null;} the indicated argument
     */
    protected final TypeBearer arg(int n) {
        if (n >= argCount) {
            throw new IllegalArgumentException("n >= argCount");
        }

        try {
            return args[n];
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("n < 0");
        }
    }

    /**
     * Gets the type auxiliary argument.
     *
     * @return {@code null-ok;} the salient type
     */
    protected final Type getAuxType() {
        return auxType;
    }

    /**
     * Gets the {@code int} auxiliary argument.
     *
     * @return the argument value
     */
    protected final int getAuxInt() {
        return auxInt;
    }

    /**
     * Gets the constant auxiliary argument.
     *
     * @return {@code null-ok;} the argument value
     */
    protected final Constant getAuxCst() {
        return auxCst;
    }

    /**
     * Gets the branch target auxiliary argument.
     *
     * @return the argument value
     */
    protected final int getAuxTarget() {
        return auxTarget;
    }

    /**
     * Gets the switch cases auxiliary argument.
     *
     * @return {@code null-ok;} the argument value
     */
    protected final SwitchList getAuxCases() {
        return auxCases;
    }

    /**
     * Gets the init values auxiliary argument.
     *
     * @return {@code null-ok;} the argument value
     */
    protected final ArrayList<Constant> getInitValues() {
        return auxInitValues;
    }
    /**
     * Gets the last local index accessed.
     *
     * @return {@code >= -1;} the salient local index or {@code -1} if none
     * was set since the last time {@link #clearArgs} was called
     */
    protected final int getLocalIndex() {
        return localIndex;
    }

    /**
     * Gets whether the loaded local has info in the local variable table.
     *
     * @return {@code true} if local arg has info in the local variable table
     */
    protected final boolean getLocalInfo() {
        return localInfo;
    }

    /**
     * Gets the target local register spec of the current operation, if any.
     * The local target spec is the combination of the values indicated
     * by a previous call to {@link #localTarget} with the type of what
     * should be the sole result set by a call to {@link #setResult} (or
     * the combination {@link #clearResult} then {@link #addResult}.
     *
     * @param isMove {@code true} if the operation being performed on the
     * local is a move. This will cause constant values to be propagated
     * to the returned local
     * @return {@code null-ok;} the salient register spec or {@code null} if no
     * local target was set since the last time {@link #clearArgs} was
     * called
     */
    protected final RegisterSpec getLocalTarget(boolean isMove) {
        if (localTarget == null) {
            return null;
        }

        if (resultCount != 1) {
            throw new SimException("local target with " +
                    ((resultCount == 0) ? "no" : "multiple") + " results");
        }

        TypeBearer result = results[0];
        Type resultType = result.getType();
        Type localType = localTarget.getType();

        if (resultType == localType) {
            /*
             * If this is to be a move operation and the result is a
             * known value, make the returned localTarget embody that
             * value.
             */
            if (isMove) {
                return localTarget.withType(result);
            } else {
                return localTarget;
            }
        }

        if (! Merger.isPossiblyAssignableFrom(localType, resultType)) {
            // The result and local types are inconsistent. Complain!
            throwLocalMismatch(resultType, localType);
            return null;
        }

        if (localType == Type.OBJECT) {
            /*
             * The result type is more specific than the local type,
             * so use that instead.
             */
            localTarget = localTarget.withType(result);
        }

        return localTarget;
    }

    /**
     * Clears the results.
     */
    protected final void clearResult() {
        resultCount = 0;
    }

    /**
     * Sets the results list to be the given single value.
     *
     * <p><b>Note:</b> If there is more than one result value, the
     * others may be added by using {@link #addResult}.</p>
     *
     * @param result {@code non-null;} result value
     */
    protected final void setResult(TypeBearer result) {
        if (result == null) {
            throw new NullPointerException("result == null");
        }

        results[0] = result;
        resultCount = 1;
    }

    /**
     * Adds an additional element to the list of results.
     *
     * @see #setResult
     *
     * @param result {@code non-null;} result value
     */
    protected final void addResult(TypeBearer result) {
        if (result == null) {
            throw new NullPointerException("result == null");
        }

        results[resultCount] = result;
        resultCount++;
    }

    /**
     * Gets the count of results. This throws an exception if results were
     * never set. (Explicitly clearing the results counts as setting them.)
     *
     * @return {@code >= 0;} the count
     */
    protected final int resultCount() {
        if (resultCount < 0) {
            throw new SimException("results never set");
        }

        return resultCount;
    }

    /**
     * Gets the width of the results (where a category-2 value counts as
     * two).
     *
     * @return {@code >= 0;} the result width
     */
    protected final int resultWidth() {
        int width = 0;

        for (int i = 0; i < resultCount; i++) {
            width += results[i].getType().getCategory();
        }

        return width;
    }

    /**
     * Gets the {@code n}th result value.
     *
     * @param n {@code >= 0, < resultCount();} which result
     * @return {@code non-null;} the indicated result value
     */
    protected final TypeBearer result(int n) {
        if (n >= resultCount) {
            throw new IllegalArgumentException("n >= resultCount");
        }

        try {
            return results[n];
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Translate the exception.
            throw new IllegalArgumentException("n < 0");
        }
    }

    /**
     * Stores the results of the latest operation into the given frame. If
     * there is a local target (see {@link #localTarget}), then the sole
     * result is stored to that target; otherwise any results are pushed
     * onto the stack.
     *
     * @param frame {@code non-null;} frame to operate on
     */
    protected final void storeResults(Frame frame) {
        if (resultCount < 0) {
            throw new SimException("results never set");
        }

        if (resultCount == 0) {
            // Nothing to do.
            return;
        }

        if (localTarget != null) {
            /*
             * Note: getLocalTarget() doesn't necessarily return
             * localTarget directly.
             */
            frame.getLocals().set(getLocalTarget(false));
        } else {
            ExecutionStack stack = frame.getStack();
            for (int i = 0; i < resultCount; i++) {
                if (localInfo) {
                    stack.setLocal();
                }
                stack.push(results[i]);
            }
        }
    }

    /**
     * Throws an exception that indicates a mismatch in local variable
     * types.
     *
     * @param found {@code non-null;} the encountered type
     * @param local {@code non-null;} the local variable's claimed type
     */
    public static void throwLocalMismatch(TypeBearer found,
            TypeBearer local) {
        throw new SimException("local variable type mismatch: " +
                "attempt to set or access a value of type " +
                found.toHuman() +
                " using a local variable of type " +
                local.toHuman() +
                ". This is symptomatic of .class transformation tools " +
                "that ignore local variable information.");
    }
}

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

import external.com.android.dex.util.ExceptionWithContext;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.IntList;

/**
 * Representation of a Java method execution frame. A frame consists
 * of a set of locals and a value stack, and it can be told to act on
 * them to load and store values between them and an "arguments /
 * results" area.
 */
public final class Frame {
    /** {@code non-null;} the locals */
    private final LocalsArray locals;

    /** {@code non-null;} the stack */
    private final ExecutionStack stack;

    /** {@code null-ok;} stack of labels of subroutines that this block is nested in */
    private final IntList subroutines;

    /**
     * Constructs an instance.
     *
     * @param locals {@code non-null;} the locals array to use
     * @param stack {@code non-null;} the execution stack to use
     */
    private Frame(LocalsArray locals, ExecutionStack stack) {
        this(locals, stack, IntList.EMPTY);
    }

    /**
     * Constructs an instance.
     *
     * @param locals {@code non-null;} the locals array to use
     * @param stack {@code non-null;} the execution stack to use
     * @param subroutines {@code non-null;} list of subroutine start labels for
     * subroutines this frame is nested in
     */
    private Frame(LocalsArray locals,
            ExecutionStack stack, IntList subroutines) {
        if (locals == null) {
            throw new NullPointerException("locals == null");
        }

        if (stack == null) {
            throw new NullPointerException("stack == null");
        }

        subroutines.throwIfMutable();

        this.locals = locals;
        this.stack = stack;
        this.subroutines = subroutines;
    }

    /**
     * Constructs an instance. The locals array initially consists of
     * all-uninitialized values (represented as {@code null}s) and
     * the stack starts out empty.
     *
     * @param maxLocals {@code >= 0;} the maximum number of locals this instance
     * can refer to
     * @param maxStack {@code >= 0;} the maximum size of the stack for this
     * instance
     */
    public Frame(int maxLocals, int maxStack) {
        this(new OneLocalsArray(maxLocals), new ExecutionStack(maxStack));
    }

    /**
     * Makes and returns a mutable copy of this instance. The copy
     * contains copies of the locals and stack (that is, it doesn't
     * share them with the original).
     *
     * @return {@code non-null;} the copy
     */
    public Frame copy() {
        return new Frame(locals.copy(), stack.copy(), subroutines);
    }

    /**
     * Makes this instance immutable.
     */
    public void setImmutable() {
        locals.setImmutable();
        stack.setImmutable();
        // "subroutines" is always immutable
    }

    /**
     * Replaces all the occurrences of the given uninitialized type in
     * this frame with its initialized equivalent.
     *
     * @param type {@code non-null;} type to replace
     */
    public void makeInitialized(Type type) {
        locals.makeInitialized(type);
        stack.makeInitialized(type);
    }

    /**
     * Gets the locals array for this instance.
     *
     * @return {@code non-null;} the locals array
     */
    public LocalsArray getLocals() {
        return locals;
    }

    /**
     * Gets the execution stack for this instance.
     *
     * @return {@code non-null;} the execution stack
     */
    public ExecutionStack getStack() {
        return stack;
    }

    /**
     * Returns the largest subroutine nesting this block may be in. An
     * empty list is returned if this block is not in any subroutine.
     * Subroutines are identified by the label of their start block. The
     * list is ordered such that the deepest nesting (the actual subroutine
     * this block is in) is the last label in the list.
     *
     * @return {@code non-null;} list as noted above
     */
    public IntList getSubroutines() {
        return subroutines;
    }

    /**
     * Initialize this frame with the method's parameters. Used for the first
     * frame.
     *
     * @param params Type list of method parameters.
     */
    public void initializeWithParameters(StdTypeList params) {
        int at = 0;
        int sz = params.size();

        for (int i = 0; i < sz; i++) {
             Type one = params.get(i);
             locals.set(at, one);
             at += one.getCategory();
        }
    }

    /**
     * Returns a Frame instance representing the frame state that should
     * be used when returning from a subroutine. The stack state of all
     * subroutine invocations is identical, but the locals state may differ.
     *
     * @param startLabel {@code >=0;} The label of the returning subroutine's
     * start block
     * @param subLabel {@code >=0;} A calling label of a subroutine
     * @return {@code null-ok;} an appropriatly-constructed instance, or null
     * if label is not in the set
     */
    public Frame subFrameForLabel(int startLabel, int subLabel) {
        LocalsArray subLocals = null;

        if (locals instanceof LocalsArraySet) {
            subLocals = ((LocalsArraySet)locals).subArrayForLabel(subLabel);
        }

        IntList newSubroutines;
        try {
            newSubroutines = subroutines.mutableCopy();

            if (newSubroutines.pop() != startLabel) {
                throw new RuntimeException("returning from invalid subroutine");
            }
            newSubroutines.setImmutable();
        } catch (IndexOutOfBoundsException ex) {
            throw new RuntimeException("returning from invalid subroutine");
        } catch (NullPointerException ex) {
            throw new NullPointerException("can't return from non-subroutine");
        }

        return (subLocals == null) ? null
                : new Frame(subLocals, stack, newSubroutines);
    }

    /**
     * Merges two frames. If the merged result is the same as this frame,
     * then this instance is returned.
     *
     * @param other {@code non-null;} another frame
     * @return {@code non-null;} the result of merging the two frames
     */
    public Frame mergeWith(Frame other) {
        LocalsArray resultLocals;
        ExecutionStack resultStack;
        IntList resultSubroutines;

        resultLocals = getLocals().merge(other.getLocals());
        resultStack = getStack().merge(other.getStack());
        resultSubroutines = mergeSubroutineLists(other.subroutines);

        resultLocals = adjustLocalsForSubroutines(
                resultLocals, resultSubroutines);

        if ((resultLocals == getLocals())
                && (resultStack == getStack())
                && subroutines == resultSubroutines) {
            return this;
        }

        return new Frame(resultLocals, resultStack, resultSubroutines);
    }

    /**
     * Merges this frame's subroutine lists with another. The result
     * is the deepest common nesting (effectively, the common prefix of the
     * two lists).
     *
     * @param otherSubroutines label list of subroutine start blocks, from
     * least-nested to most-nested.
     * @return {@code non-null;} merged subroutine nest list as described above
     */
    private IntList mergeSubroutineLists(IntList otherSubroutines) {
        if (subroutines.equals(otherSubroutines)) {
            return subroutines;
        }

        IntList resultSubroutines = new IntList();

        int szSubroutines = subroutines.size();
        int szOthers = otherSubroutines.size();
        for (int i = 0; i < szSubroutines && i < szOthers
                && (subroutines.get(i) == otherSubroutines.get(i)); i++) {
            resultSubroutines.add(i);
        }

        resultSubroutines.setImmutable();

        return resultSubroutines;
    }

    /**
     * Adjusts a locals array to account for a merged subroutines list.
     * If a frame merge results in, effectively, a subroutine return through
     * a throw then the current locals will be a LocalsArraySet that will
     * need to be trimmed of all OneLocalsArray elements that relevent to
     * the subroutine that is returning.
     *
     * @param locals {@code non-null;} LocalsArray from before a merge
     * @param subroutines {@code non-null;} a label list of subroutine start blocks
     * representing the subroutine nesting of the block being merged into.
     * @return {@code non-null;} locals set appropriate for merge
     */
    private static LocalsArray adjustLocalsForSubroutines(
            LocalsArray locals, IntList subroutines) {
        if (! (locals instanceof LocalsArraySet)) {
            // nothing to see here
            return locals;
        }

        LocalsArraySet laSet = (LocalsArraySet)locals;

        if (subroutines.size() == 0) {
            /*
             * We've merged from a subroutine context to a non-subroutine
             * context, likely via a throw. Our successor will only need
             * to consider the primary locals state, not the state of
             * all possible subroutine paths.
             */

            return laSet.getPrimary();
        }

        /*
         * It's unclear to me if the locals set needs to be trimmed here.
         * If it does, then I believe it is all of the calling blocks
         * in the subroutine at the end of "subroutines" passed into
         * this method that should be removed.
         */
        return laSet;
    }

    /**
     * Merges this frame with the frame of a subroutine caller at
     * {@code predLabel}. Only called on the frame at the first
     * block of a subroutine.
     *
     * @param other {@code non-null;} another frame
     * @param subLabel label of subroutine start block
     * @param predLabel label of calling block
     * @return {@code non-null;} the result of merging the two frames
     */
    public Frame mergeWithSubroutineCaller(Frame other, int subLabel,
            int predLabel) {
        LocalsArray resultLocals;
        ExecutionStack resultStack;

        resultLocals = getLocals().mergeWithSubroutineCaller(
                other.getLocals(), predLabel);
        resultStack = getStack().merge(other.getStack());

        IntList newOtherSubroutines = other.subroutines.mutableCopy();
        newOtherSubroutines.add(subLabel);
        newOtherSubroutines.setImmutable();

        if ((resultLocals == getLocals())
                && (resultStack == getStack())
                && subroutines.equals(newOtherSubroutines)) {
            return this;
        }

        IntList resultSubroutines;

        if (subroutines.equals(newOtherSubroutines)) {
            resultSubroutines = subroutines;
        } else {
            /*
             * The new subroutines list should be the deepest of the two
             * lists being merged, but the postfix of the resultant list
             * must be equal to the shorter list.
             */
            IntList nonResultSubroutines;

            if (subroutines.size() > newOtherSubroutines.size()) {
                resultSubroutines = subroutines;
                nonResultSubroutines = newOtherSubroutines;
            } else {
                resultSubroutines = newOtherSubroutines;
                nonResultSubroutines = subroutines;
            }

            int szResult = resultSubroutines.size();
            int szNonResult = nonResultSubroutines.size();

            for (int i = szNonResult - 1; i >=0; i-- ) {
                if (nonResultSubroutines.get(i)
                        != resultSubroutines.get(
                        i + (szResult - szNonResult))) {
                    throw new
                            RuntimeException("Incompatible merged subroutines");
                }
            }

        }

        return new Frame(resultLocals, resultStack, resultSubroutines);
    }

    /**
     * Makes a frame for a subroutine start block, given that this is the
     * ending frame of one of the subroutine's calling blocks. Subroutine
     * calls may be nested and thus may have nested locals state, so we
     * start with an initial state as seen by the subroutine, but keep track
     * of the individual locals states that will be expected when the individual
     * subroutine calls return.
     *
     * @param subLabel label of subroutine start block
     * @param callerLabel {@code >=0;} label of the caller block where this frame
     * came from.
     * @return a new instance to begin a called subroutine.
     */
    public Frame makeNewSubroutineStartFrame(int subLabel, int callerLabel) {
        IntList newSubroutines = subroutines.mutableCopy();
        newSubroutines.add(subLabel);
        Frame newFrame = new Frame(locals.getPrimary(), stack,
                IntList.makeImmutable(subLabel));
        return newFrame.mergeWithSubroutineCaller(this, subLabel, callerLabel);
    }

    /**
     * Makes a new frame for an exception handler block invoked from this
     * frame.
     *
     * @param exceptionClass exception that the handler block will handle
     * @return new frame
     */
    public Frame makeExceptionHandlerStartFrame(CstType exceptionClass) {
        ExecutionStack newStack = getStack().copy();

        newStack.clear();
        newStack.push(exceptionClass);

        return new Frame(getLocals(), newStack, subroutines);
    }

    /**
     * Annotates (adds context to) the given exception with information
     * about this frame.
     *
     * @param ex {@code non-null;} the exception to annotate
     */
    public void annotate(ExceptionWithContext ex) {
        locals.annotate(ex);
        stack.annotate(ex);
    }
}

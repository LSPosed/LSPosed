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
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.MutabilityControl;

/**
 * Representation of a Java method execution stack.
 *
 * <p><b>Note:</b> For the most part, the documentation for this class
 * ignores the distinction between {@link Type} and {@link
 * TypeBearer}.</p>
 */
public final class ExecutionStack extends MutabilityControl {
    /** {@code non-null;} array of stack contents */
    private final TypeBearer[] stack;

    /**
     * {@code non-null;} array specifying whether stack contents have entries
     * in the local variable table
     */
    private final boolean[] local;
    /**
     * {@code >= 0;} stack pointer (points one past the end) / current stack
     * size
     */
    private int stackPtr;

    /**
     * Constructs an instance.
     *
     * @param maxStack {@code >= 0;} the maximum size of the stack for this
     * instance
     */
    public ExecutionStack(int maxStack) {
        super(maxStack != 0);
        stack = new TypeBearer[maxStack];
        local = new boolean[maxStack];
        stackPtr = 0;
    }

    /**
     * Makes and returns a mutable copy of this instance.
     *
     * @return {@code non-null;} the copy
     */
    public ExecutionStack copy() {
        ExecutionStack result = new ExecutionStack(stack.length);

        System.arraycopy(stack, 0, result.stack, 0, stack.length);
        System.arraycopy(local, 0, result.local, 0, local.length);
        result.stackPtr = stackPtr;

        return result;
    }

    /**
     * Annotates (adds context to) the given exception with information
     * about this instance.
     *
     * @param ex {@code non-null;} the exception to annotate
     */
    public void annotate(ExceptionWithContext ex) {
        int limit = stackPtr - 1;

        for (int i = 0; i <= limit; i++) {
            String idx = (i == limit) ? "top0" : Hex.u2(limit - i);

            ex.addContext("stack[" + idx + "]: " +
                          stackElementString(stack[i]));
        }
    }

    /**
     * Replaces all the occurrences of the given uninitialized type in
     * this stack with its initialized equivalent.
     *
     * @param type {@code non-null;} type to replace
     */
    public void makeInitialized(Type type) {
        if (stackPtr == 0) {
            // We have to check for this before checking for immutability.
            return;
        }

        throwIfImmutable();

        Type initializedType = type.getInitializedType();

        for (int i = 0; i < stackPtr; i++) {
            if (stack[i] == type) {
                stack[i] = initializedType;
            }
        }
    }

    /**
     * Gets the maximum stack size for this instance.
     *
     * @return {@code >= 0;} the max stack size
     */
    public int getMaxStack() {
        return stack.length;
    }

    /**
     * Gets the current stack size.
     *
     * @return {@code >= 0, < getMaxStack();} the current stack size
     */
    public int size() {
        return stackPtr;
    }

    /**
     * Clears the stack. (That is, this method pops everything off.)
     */
    public void clear() {
        throwIfImmutable();

        for (int i = 0; i < stackPtr; i++) {
            stack[i] = null;
            local[i] = false;
        }

        stackPtr = 0;
    }

    /**
     * Pushes a value of the given type onto the stack.
     *
     * @param type {@code non-null;} type of the value
     * @throws SimException thrown if there is insufficient room on the
     * stack for the value
     */
    public void push(TypeBearer type) {
        throwIfImmutable();

        int category;

        try {
            type = type.getFrameType();
            category = type.getType().getCategory();
        } catch (NullPointerException ex) {
            // Elucidate the exception.
            throw new NullPointerException("type == null");
        }

        if ((stackPtr + category) > stack.length) {
            throwSimException("overflow");
            return;
        }

        if (category == 2) {
            stack[stackPtr] = null;
            stackPtr++;
        }

        stack[stackPtr] = type;
        stackPtr++;
    }

    /**
     * Flags the next value pushed onto the stack as having local info.
     */
    public void setLocal() {
        throwIfImmutable();

        local[stackPtr] = true;
    }

    /**
     * Peeks at the {@code n}th element down from the top of the stack.
     * {@code n == 0} means to peek at the top of the stack. Note that
     * this will return {@code null} if the indicated element is the
     * deeper half of a category-2 value.
     *
     * @param n {@code >= 0;} which element to peek at
     * @return {@code null-ok;} the type of value stored at that element
     * @throws SimException thrown if {@code n >= size()}
     */
    public TypeBearer peek(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        }

        if (n >= stackPtr) {
            return throwSimException("underflow");
        }

        return stack[stackPtr - n - 1];
    }

    /**
     * Peeks at the {@code n}th element down from the top of the
     * stack, returning whether or not it has local info.
     *
     * @param n {@code >= 0;} which element to peek at
     * @return {@code true} if the value has local info, {@code false} otherwise
     * @throws SimException thrown if {@code n >= size()}
     */
    public boolean peekLocal(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        }

        if (n >= stackPtr) {
            throw new SimException("stack: underflow");
        }

        return local[stackPtr - n - 1];
    }

    /**
     * Peeks at the {@code n}th element down from the top of the
     * stack, returning the type per se, as opposed to the
     * <i>type-bearer</i>.  This method is just a convenient shorthand
     * for {@code peek(n).getType()}.
     *
     * @see #peek
     */
    public Type peekType(int n) {
        return peek(n).getType();
    }

    /**
     * Pops the top element off of the stack.
     *
     * @return {@code non-null;} the type formerly on the top of the stack
     * @throws SimException thrown if the stack is empty
     */
    public TypeBearer pop() {
        throwIfImmutable();

        TypeBearer result = peek(0);

        stack[stackPtr - 1] = null;
        local[stackPtr - 1] = false;
        stackPtr -= result.getType().getCategory();

        return result;
    }

    /**
     * Changes an element already on a stack. This method is useful in limited
     * contexts, particularly when merging two instances. As such, it places
     * the following restriction on its behavior: You may only replace
     * values with other values of the same category.
     *
     * @param n {@code >= 0;} which element to change, where {@code 0} is
     * the top element of the stack
     * @param type {@code non-null;} type of the new value
     * @throws SimException thrown if {@code n >= size()} or
     * the action is otherwise prohibited
     */
    public void change(int n, TypeBearer type) {
        throwIfImmutable();

        try {
            type = type.getFrameType();
        } catch (NullPointerException ex) {
            // Elucidate the exception.
            throw new NullPointerException("type == null");
        }

        int idx = stackPtr - n - 1;
        TypeBearer orig = stack[idx];

        if ((orig == null) ||
            (orig.getType().getCategory() != type.getType().getCategory())) {
            throwSimException("incompatible substitution: " +
                              stackElementString(orig) + " -> " +
                              stackElementString(type));
        }

        stack[idx] = type;
    }

    /**
     * Merges this stack with another stack. A new instance is returned if
     * this merge results in a change. If no change results, this instance is
     * returned.  See {@link Merger#mergeStack(ExecutionStack,ExecutionStack)
     * Merger.mergeStack()}
     *
     * @param other {@code non-null;} a stack to merge with
     * @return {@code non-null;} the result of the merge
     */
    public ExecutionStack merge(ExecutionStack other) {
        try {
            return Merger.mergeStack(this, other);
        } catch (SimException ex) {
            ex.addContext("underlay stack:");
            this.annotate(ex);
            ex.addContext("overlay stack:");
            other.annotate(ex);
            throw ex;
        }
    }

    /**
     * Gets the string form for a stack element. This is the same as
     * {@code toString()} except that {@code null} is converted
     * to {@code "<invalid>"}.
     *
     * @param type {@code null-ok;} the stack element
     * @return {@code non-null;} the string form
     */
    private static String stackElementString(TypeBearer type) {
        if (type == null) {
            return "<invalid>";
        }

        return type.toString();
    }

    /**
     * Throws a properly-formatted exception.
     *
     * @param msg {@code non-null;} useful message
     * @return never (keeps compiler happy)
     */
    private static TypeBearer throwSimException(String msg) {
        throw new SimException("stack: " + msg);
    }
}

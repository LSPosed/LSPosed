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

package external.com.android.dx.rop.code;

import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.Hex;

/**
 * Class that describes all the immutable parts of register-based operations.
 */
public final class Rop {
    /** minimum {@code BRANCH_*} value */
    public static final int BRANCH_MIN = 1;

    /** indicates a non-branching op */
    public static final int BRANCH_NONE = 1;

    /** indicates a function/method return */
    public static final int BRANCH_RETURN = 2;

    /** indicates an unconditional goto */
    public static final int BRANCH_GOTO = 3;

    /** indicates a two-way branch */
    public static final int BRANCH_IF = 4;

    /** indicates a switch-style branch */
    public static final int BRANCH_SWITCH = 5;

    /** indicates a throw-style branch (both always-throws and may-throw) */
    public static final int BRANCH_THROW = 6;

    /** maximum {@code BRANCH_*} value */
    public static final int BRANCH_MAX = 6;

    /** the opcode; one of the constants in {@link RegOps} */
    private final int opcode;

    /**
     * {@code non-null;} result type of this operation; {@link Type#VOID} for
     * no-result operations
     */
    private final Type result;

    /** {@code non-null;} types of all the sources of this operation */
    private final TypeList sources;

    /** {@code non-null;} list of possible types thrown by this operation */
    private final TypeList exceptions;

    /**
     * the branchingness of this op; one of the {@code BRANCH_*}
     * constants in this class
     */
    private final int branchingness;

    /** whether this is a function/method call op or similar */
    private final boolean isCallLike;

    /** {@code null-ok;} nickname, if specified (used for debugging) */
    private final String nickname;

    /**
     * Constructs an instance. This method is private. Use one of the
     * public constructors.
     *
     * @param opcode the opcode; one of the constants in {@link RegOps}
     * @param result {@code non-null;} result type of this operation; {@link
     * Type#VOID} for no-result operations
     * @param sources {@code non-null;} types of all the sources of this operation
     * @param exceptions {@code non-null;} list of possible types thrown by this
     * operation
     * @param branchingness the branchingness of this op; one of the
     * {@code BRANCH_*} constants
     * @param isCallLike whether the op is a function/method call or similar
     * @param nickname {@code null-ok;} optional nickname (used for debugging)
     */
    public Rop(int opcode, Type result, TypeList sources,
               TypeList exceptions, int branchingness, boolean isCallLike,
               String nickname) {
        if (result == null) {
            throw new NullPointerException("result == null");
        }

        if (sources == null) {
            throw new NullPointerException("sources == null");
        }

        if (exceptions == null) {
            throw new NullPointerException("exceptions == null");
        }

        if ((branchingness < BRANCH_MIN) || (branchingness > BRANCH_MAX)) {
            throw new IllegalArgumentException("invalid branchingness: " + branchingness);
        }

        if ((exceptions.size() != 0) && (branchingness != BRANCH_THROW)) {
            throw new IllegalArgumentException("exceptions / branchingness " +
                                               "mismatch");
        }

        this.opcode = opcode;
        this.result = result;
        this.sources = sources;
        this.exceptions = exceptions;
        this.branchingness = branchingness;
        this.isCallLike = isCallLike;
        this.nickname = nickname;
    }

    /**
     * Constructs an instance. The constructed instance is never a
     * call-like op (see {@link #isCallLike}).
     *
     * @param opcode the opcode; one of the constants in {@link RegOps}
     * @param result {@code non-null;} result type of this operation; {@link
     * Type#VOID} for no-result operations
     * @param sources {@code non-null;} types of all the sources of this operation
     * @param exceptions {@code non-null;} list of possible types thrown by this
     * operation
     * @param branchingness the branchingness of this op; one of the
     * {@code BRANCH_*} constants
     * @param nickname {@code null-ok;} optional nickname (used for debugging)
     */
    public Rop(int opcode, Type result, TypeList sources,
               TypeList exceptions, int branchingness, String nickname) {
        this(opcode, result, sources, exceptions, branchingness, false,
             nickname);
    }

    /**
     * Constructs a no-exception instance. The constructed instance is never a
     * call-like op (see {@link #isCallLike}).
     *
     * @param opcode the opcode; one of the constants in {@link RegOps}
     * @param result {@code non-null;} result type of this operation; {@link
     * Type#VOID} for no-result operations
     * @param sources {@code non-null;} types of all the sources of this operation
     * @param branchingness the branchingness of this op; one of the
     * {@code BRANCH_*} constants
     * @param nickname {@code null-ok;} optional nickname (used for debugging)
     */
    public Rop(int opcode, Type result, TypeList sources, int branchingness,
               String nickname) {
        this(opcode, result, sources, StdTypeList.EMPTY, branchingness, false,
             nickname);
    }

    /**
     * Constructs a non-branching no-exception instance. The
     * {@code branchingness} is always {@code BRANCH_NONE},
     * and it is never a call-like op (see {@link #isCallLike}).
     *
     * @param opcode the opcode; one of the constants in {@link RegOps}
     * @param result {@code non-null;} result type of this operation; {@link
     * Type#VOID} for no-result operations
     * @param sources {@code non-null;} types of all the sources of this operation
     * @param nickname {@code null-ok;} optional nickname (used for debugging)
     */
    public Rop(int opcode, Type result, TypeList sources, String nickname) {
        this(opcode, result, sources, StdTypeList.EMPTY, Rop.BRANCH_NONE,
             false, nickname);
    }

    /**
     * Constructs a non-empty exceptions instance. Its
     * {@code branchingness} is always {@code BRANCH_THROW},
     * but it is never a call-like op (see {@link #isCallLike}).
     *
     * @param opcode the opcode; one of the constants in {@link RegOps}
     * @param result {@code non-null;} result type of this operation; {@link
     * Type#VOID} for no-result operations
     * @param sources {@code non-null;} types of all the sources of this operation
     * @param exceptions {@code non-null;} list of possible types thrown by this
     * operation
     * @param nickname {@code null-ok;} optional nickname (used for debugging)
     */
    public Rop(int opcode, Type result, TypeList sources, TypeList exceptions,
               String nickname) {
        this(opcode, result, sources, exceptions, Rop.BRANCH_THROW, false,
             nickname);
    }

    /**
     * Constructs a non-nicknamed instance with non-empty exceptions, which
     * is always a call-like op (see {@link #isCallLike}). Its
     * {@code branchingness} is always {@code BRANCH_THROW}.
     *
     * @param opcode the opcode; one of the constants in {@link RegOps}
     * @param sources {@code non-null;} types of all the sources of this operation
     * @param exceptions {@code non-null;} list of possible types thrown by this
     * operation
     */
    public Rop(int opcode, TypeList sources, TypeList exceptions) {
        this(opcode, Type.VOID, sources, exceptions, Rop.BRANCH_THROW, true,
             null);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // Easy out.
            return true;
        }

        if (!(other instanceof Rop)) {
            return false;
        }

        Rop rop = (Rop) other;

        return (opcode == rop.opcode) &&
            (branchingness == rop.branchingness) &&
            (result == rop.result) &&
            sources.equals(rop.sources) &&
            exceptions.equals(rop.exceptions);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int h = (opcode * 31) + branchingness;
        h = (h * 31) + result.hashCode();
        h = (h * 31) + sources.hashCode();
        h = (h * 31) + exceptions.hashCode();

        return h;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(40);

        sb.append("Rop{");

        sb.append(RegOps.opName(opcode));

        if (result != Type.VOID) {
            sb.append(" ");
            sb.append(result);
        } else {
            sb.append(" .");
        }

        sb.append(" <-");

        int sz = sources.size();
        if (sz == 0) {
            sb.append(" .");
        } else {
            for (int i = 0; i < sz; i++) {
                sb.append(' ');
                sb.append(sources.getType(i));
            }
        }

        if (isCallLike) {
            sb.append(" call");
        }

        sz = exceptions.size();
        if (sz != 0) {
            sb.append(" throws");
            for (int i = 0; i < sz; i++) {
                sb.append(' ');
                Type one = exceptions.getType(i);
                if (one == Type.THROWABLE) {
                    sb.append("<any>");
                } else {
                    sb.append(exceptions.getType(i));
                }
            }
        } else {
            switch (branchingness) {
                case BRANCH_NONE:   sb.append(" flows"); break;
                case BRANCH_RETURN: sb.append(" returns"); break;
                case BRANCH_GOTO:   sb.append(" gotos"); break;
                case BRANCH_IF:     sb.append(" ifs"); break;
                case BRANCH_SWITCH: sb.append(" switches"); break;
                default: sb.append(" " + Hex.u1(branchingness)); break;
            }
        }

        sb.append('}');

        return sb.toString();
    }

    /**
     * Gets the opcode.
     *
     * @return the opcode
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * Gets the result type. A return value of {@link Type#VOID}
     * means this operation returns nothing.
     *
     * @return {@code null-ok;} the result spec
     */
    public Type getResult() {
        return result;
    }

    /**
     * Gets the source types.
     *
     * @return {@code non-null;} the source types
     */
    public TypeList getSources() {
        return sources;
    }

    /**
     * Gets the list of exception types that might be thrown.
     *
     * @return {@code non-null;} the list of exception types
     */
    public TypeList getExceptions() {
        return exceptions;
    }

    /**
     * Gets the branchingness of this instance.
     *
     * @return the branchingness
     */
    public int getBranchingness() {
        return branchingness;
    }

    /**
     * Gets whether this opcode is a function/method call or similar.
     *
     * @return {@code true} iff this opcode is call-like
     */
    public boolean isCallLike() {
        return isCallLike;
    }


    /**
     * Gets whether this opcode is commutative (the order of its sources are
     * unimportant) or not. All commutative Rops have exactly two sources and
     * have no branchiness.
     *
     * @return true if rop is commutative
     */
    public boolean isCommutative() {
        switch (opcode) {
            case RegOps.AND:
            case RegOps.OR:
            case RegOps.XOR:
            case RegOps.ADD:
            case RegOps.MUL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets the nickname. If this instance has no nickname, this returns
     * the result of calling {@link #toString}.
     *
     * @return {@code non-null;} the nickname
     */
    public String getNickname() {
        if (nickname != null) {
            return nickname;
        }

        return toString();
    }

    /**
     * Gets whether this operation can possibly throw an exception. This
     * is just a convenient wrapper for
     * {@code getExceptions().size() != 0}.
     *
     * @return {@code true} iff this operation can possibly throw
     */
    public final boolean canThrow() {
        return (exceptions.size() != 0);
    }
}

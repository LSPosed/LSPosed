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

import external.com.android.dx.io.OpcodeInfo;
import external.com.android.dx.io.Opcodes;

/**
 * Representation of an opcode.
 */
public final class Dop {
    /** {@code Opcodes.isValid();} the opcode value itself */
    private final int opcode;

    /** {@code Opcodes.isValid();} the opcode family */
    private final int family;

    /**
     * {@code Opcodes.isValid();} what opcode (by number) to try next
     * when attempting to match an opcode to particular arguments;
     * {@code Opcodes.NO_NEXT} to indicate that this is the last
     * opcode to try in a particular chain
     */
    private final int nextOpcode;

    /** {@code non-null;} the instruction format */
    private final InsnFormat format;

    /** whether this opcode uses a result register */
    private final boolean hasResult;

    /**
     * Constructs an instance.
     *
     * @param opcode {@code Opcodes.isValid();} the opcode value
     * itself
     * @param family {@code Opcodes.isValid();} the opcode family
     * @param nextOpcode {@code Opcodes.isValid();} what opcode (by
     * number) to try next when attempting to match an opcode to
     * particular arguments; {@code Opcodes.NO_NEXT} to indicate that
     * this is the last opcode to try in a particular chain
     * @param format {@code non-null;} the instruction format
     * @param hasResult whether the opcode has a result register; if so it
     * is always the first register
     */
    public Dop(int opcode, int family, int nextOpcode, InsnFormat format,
            boolean hasResult) {
        if (!Opcodes.isValidShape(opcode)) {
            throw new IllegalArgumentException("bogus opcode");
        }

        if (!Opcodes.isValidShape(family)) {
            throw new IllegalArgumentException("bogus family");
        }

        if (!Opcodes.isValidShape(nextOpcode)) {
            throw new IllegalArgumentException("bogus nextOpcode");
        }

        if (format == null) {
            throw new NullPointerException("format == null");
        }

        this.opcode = opcode;
        this.family = family;
        this.nextOpcode = nextOpcode;
        this.format = format;
        this.hasResult = hasResult;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Gets the opcode value.
     *
     * @return {@code Opcodes.MIN_VALUE..Opcodes.MAX_VALUE;} the opcode value
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * Gets the opcode family. The opcode family is the unmarked (no
     * "/...") opcode that has equivalent semantics to this one.
     *
     * @return {@code Opcodes.MIN_VALUE..Opcodes.MAX_VALUE;} the opcode family
     */
    public int getFamily() {
        return family;
    }

    /**
     * Gets the instruction format.
     *
     * @return {@code non-null;} the instruction format
     */
    public InsnFormat getFormat() {
        return format;
    }

    /**
     * Returns whether this opcode uses a result register.
     *
     * @return {@code true} iff this opcode uses a result register
     */
    public boolean hasResult() {
        return hasResult;
    }

    /**
     * Gets the opcode name.
     *
     * @return {@code non-null;} the opcode name
     */
    public String getName() {
        return OpcodeInfo.getName(opcode);
    }

    /**
     * Gets the opcode value to try next when attempting to match an
     * opcode to particular arguments. This returns {@code
     * Opcodes.NO_NEXT} to indicate that this is the last opcode to
     * try in a particular chain.
     *
     * @return {@code Opcodes.MIN_VALUE..Opcodes.MAX_VALUE;} the opcode value
     */
    public int getNextOpcode() {
        return nextOpcode;
    }

    /**
     * Gets the opcode for the opposite test of this instance. This is only
     * valid for opcodes which are in fact tests.
     *
     * @return {@code non-null;} the opposite test
     */
    public Dop getOppositeTest() {
        switch (opcode) {
            case Opcodes.IF_EQ:  return Dops.IF_NE;
            case Opcodes.IF_NE:  return Dops.IF_EQ;
            case Opcodes.IF_LT:  return Dops.IF_GE;
            case Opcodes.IF_GE:  return Dops.IF_LT;
            case Opcodes.IF_GT:  return Dops.IF_LE;
            case Opcodes.IF_LE:  return Dops.IF_GT;
            case Opcodes.IF_EQZ: return Dops.IF_NEZ;
            case Opcodes.IF_NEZ: return Dops.IF_EQZ;
            case Opcodes.IF_LTZ: return Dops.IF_GEZ;
            case Opcodes.IF_GEZ: return Dops.IF_LTZ;
            case Opcodes.IF_GTZ: return Dops.IF_LEZ;
            case Opcodes.IF_LEZ: return Dops.IF_GTZ;
        }

        throw new IllegalArgumentException("bogus opcode: " + this);
    }
}

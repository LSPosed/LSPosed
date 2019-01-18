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
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.rop.type.Type;
import java.util.ArrayList;

/**
 * Interface for machines capable of executing bytecode by acting
 * upon a {@link Frame}. A machine conceptually contains four arbitrary-value
 * argument slots, slots for several literal-value arguments, and slots for
 * branch target information.
 */
public interface Machine {
    /**
     * Gets the effective prototype of the method that this instance is
     * being used for. The <i>effective</i> prototype includes an initial
     * {@code this} argument for instance methods.
     *
     * @return {@code non-null;} the method prototype
     */
    public Prototype getPrototype();

    /**
     * Clears the regular and auxiliary arguments area.
     */
    public void clearArgs();

    /**
     * Pops the given number of values from the stack (of either category),
     * and store them in the arguments area, indicating that there are now
     * that many arguments. Also, clear the auxiliary arguments.
     *
     * @param frame {@code non-null;} frame to operate on
     * @param count {@code >= 0;} number of values to pop
     */
    public void popArgs(Frame frame, int count);

    /**
     * Pops values from the stack of the types indicated by the given
     * {@code Prototype} (popped in reverse of the argument
     * order, so the first prototype argument type is for the deepest
     * element of the stack), and store them in the arguments area,
     * indicating that there are now that many arguments. Also, clear
     * the auxiliary arguments.
     *
     * @param frame {@code non-null;} frame to operate on
     * @param prototype {@code non-null;} prototype indicating arguments to pop
     */
    public void popArgs(Frame frame, Prototype prototype);

    /**
     * Pops a value from the stack of the indicated type, and store it
     * in the arguments area, indicating that there are now that many
     * arguments. Also, clear the auxiliary arguments.
     *
     * @param frame {@code non-null;} frame to operate on
     * @param type {@code non-null;} type of the argument
     */
    public void popArgs(Frame frame, Type type);

    /**
     * Pops values from the stack of the indicated types (popped in
     * reverse argument order, so the first indicated type is for the
     * deepest element of the stack), and store them in the arguments
     * area, indicating that there are now that many arguments. Also,
     * clear the auxiliary arguments.
     *
     * @param frame {@code non-null;} frame to operate on
     * @param type1 {@code non-null;} type of the first argument
     * @param type2 {@code non-null;} type of the second argument
     */
    public void popArgs(Frame frame, Type type1, Type type2);

    /**
     * Pops values from the stack of the indicated types (popped in
     * reverse argument order, so the first indicated type is for the
     * deepest element of the stack), and store them in the arguments
     * area, indicating that there are now that many arguments. Also,
     * clear the auxiliary arguments.
     *
     * @param frame {@code non-null;} frame to operate on
     * @param type1 {@code non-null;} type of the first argument
     * @param type2 {@code non-null;} type of the second argument
     * @param type3 {@code non-null;} type of the third argument
     */
    public void popArgs(Frame frame, Type type1, Type type2, Type type3);

    /**
     * Loads the local variable with the given index as the sole argument in
     * the arguments area. Also, clear the auxiliary arguments.
     *
     * @param frame {@code non-null;} frame to operate on
     * @param idx {@code >= 0;} the local variable index
     */
    public void localArg(Frame frame, int idx);

    /**
     * Used to specify if a loaded local variable has info in the local
     * variable table.
     *
     * @param local {@code true} if local arg has info in local variable table
     */
    public void localInfo(boolean local);

    /**
     * Indicates that the salient type of this operation is as
     * given. This differentiates between, for example, the various
     * arithmetic opcodes, which, by the time they hit a
     * {@code Machine} are collapsed to the {@code int}
     * variant. (See {@link BytecodeArray#parseInstruction} for
     * details.)
     *
     * @param type {@code non-null;} the salient type of the upcoming operation
     */
    public void auxType(Type type);

    /**
     * Indicates that there is an auxiliary (inline, not stack)
     * argument of type {@code int}, with the given value.
     *
     * <p><b>Note:</b> Perhaps unintuitively, the stack manipulation
     * ops (e.g., {@code dup} and {@code swap}) use this to
     * indicate the result stack pattern with a straightforward hex
     * encoding of the push order starting with least-significant
     * nibbles getting pushed first). For example, an all-category-1
     * {@code dup2_x1} sets this to {@code 0x12312}, and the
     * other form of that op sets this to
     * {@code 0x121}.</p>
     *
     * <p><b>Also Note:</b> For {@code switch*} instructions, this is
     * used to indicate the padding value (which is only useful for
     * verification).</p>
     *
     * @param value the argument value
     */
    public void auxIntArg(int value);

    /**
     * Indicates that there is an auxiliary (inline, not stack) object
     * argument, with the value based on the given constant.
     *
     * <p><b>Note:</b> Some opcodes use both {@code int} and
     * constant auxiliary arguments.</p>
     *
     * @param cst {@code non-null;} the constant containing / referencing
     * the value
     */
    public void auxCstArg(Constant cst);

    /**
     * Indicates that there is an auxiliary (inline, not stack) argument
     * indicating a branch target.
     *
     * @param target the argument value
     */
    public void auxTargetArg(int target);

    /**
     * Indicates that there is an auxiliary (inline, not stack) argument
     * consisting of a {@code switch*} table.
     *
     * <p><b>Note:</b> This is generally used in conjunction with
     * {@link #auxIntArg} (which holds the padding).</p>
     *
     * @param cases {@code non-null;} the list of key-target pairs, plus the default
     * target
     */
    public void auxSwitchArg(SwitchList cases);

    /**
     * Indicates that there is an auxiliary (inline, not stack) argument
     * consisting of a list of initial values for a newly created array.
     *
     * @param initValues {@code non-null;} the list of constant values to initialize
     * the array
     */
    public void auxInitValues(ArrayList<Constant> initValues);

    /**
     * Indicates that the target of this operation is the given local.
     *
     * @param idx {@code >= 0;} the local variable index
     * @param type {@code non-null;} the type of the local
     * @param local {@code null-ok;} the name and signature of the local, if known
     */
    public void localTarget(int idx, Type type, LocalItem local);

    /**
     * "Runs" the indicated opcode in an appropriate way, using the arguments
     * area as appropriate, and modifying the given frame in response.
     *
     * @param frame {@code non-null;} frame to operate on
     * @param offset {@code >= 0;} byte offset in the method to the opcode being
     * run
     * @param opcode {@code >= 0;} the opcode to run
     */
    public void run(Frame frame, int offset, int opcode);
}

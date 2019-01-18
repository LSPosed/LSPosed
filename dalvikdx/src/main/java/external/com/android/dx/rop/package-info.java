/*
 * Copyright (C) 2008 The Android Open Source Project
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

package external.com.android.dx.rop;

/**
 * <h1>An Introduction to Rop Form</h1>
 *
 * This package contains classes associated with dx's {@code Rop}
 * intermediate form.<p>
 *
 * The Rop form is intended to represent the instructions and the control-flow
 * graph in a reasonably programmatically useful form while closely mirroring
 * the dex instruction set.<p>
 *
 * <h2>Key Classes</h2>
 *
 * <ul>
 * <li> {@link RopMethod}, the representation of an individual method
 * <li> {@link BasicBlock} and its per-method container, {@link BasicBlockList},
 * the representation of control flow elements.
 * <li> {@link Insn} and its subclasses along with its per-basic block
 * container {@link InsnList}. {@code Insn} instances represent
 * individual instructions in the abstract register machine.
 * <li> {@link RegisterSpec} and its container {@link RegisterSpecList}. A
 * register spec encodes register number, register width, type information,
 * and potentially local variable information as well for instruction sources
 * and results.
 * <li> {@link Rop} instances represent opcodes in the abstract machine. Many
 * {@code Rop} instances are singletons defined in static fields in
 * {@link Rops}. The rest are constructed dynamically using static methods
 * in {@code Rops}
 * <li> {@link RegOps} lists numeric constants for the opcodes
 * <li> {@link Constant} and its subclasses represent constant data values
 * that opcodes may refer to.
 * <li> {@link Type} instances represent the core data types that can be
 * handled by the abstract machine.
 * <li> The {@link TypeBearer} interface is implemented by classes that
 * represent a core data type, but may also have secondary information
 * (such as constant value) associated with them.
 * <ul>
 *
 * <h2>Control-Flow Graph</h2>
 *
 * Each method is separated into a list of basic blocks. For the most part,
 * basic blocks are referred to by a positive integer
 * {@link BasicBlock#getLabel label}, which is always unique per method. The
 * label value is typically derived from a bytecode address from the source
 * bytecode. Blocks that don't originate directly from source bytecode have
 * labels generated for them in a mostly arbitrary order.<p>
 *
 * Blocks are referred to by their label, for the most part, because
 * {@code BasicBlock} instances are immutable and thus any modification to
 * the control flow graph or the instruction list results in replacement
 * instances (with identical labels) being created.<p>
 *
 * A method has a single {@link RopMethod#getFirstLabel entry block} and 0
 * to N {@link RopMethod#getExitPredecessors exit predecessor blocks} which
 * will return. All blocks that are not the entry block will have at least
 * one predecessor (or are unreachable and should be removed from the block
 * list). All blocks that are not exit predecessors must have at least one
 * successor.<p>
 *
 * Since all blocks must branch, all blocks must have, as their final
 * instruction, an instruction whose opcode has a {@link Rop#getBranchingness
 * branchingness} other than {@link Rop.BRANCH_NONE}. Furthermore, branching
 * instructions may only be the final instruction in any basic block. If
 * no other terminating opcode is appropriate, use a {@link Rops#GOTO GOTO}.<p>
 *
 * Typically a block will have a {@link BasicBlock#getPrimarySuccessor
 * primary successor} which distinguishes a particular control flow path.
 * For {Rops#isCallLike}call or call-like} opcodes, this is the path taken
 * in the non-exceptional case, where all other successors represent
 * various exception paths. For comparison operators such as
 * {@link Rops#IF_EQZ_INT}, the primary successor represents the path taken
 * if the <b>condition evaluates to false</b>. For {@link SwitchInsn switch
 * instructions}, the primary successor is the default case.<p>
 *
 * A basic block's successor list is ordered and may refer to unique labels
 * multiple times. For example, if a switch statement contains multiple case
 * statements for the same code path, a single basic block will likely
 * appear in the successor list multiple times. In general, the
 * significance of the successor list's order (like the significance of
 * the primary successor) is a property of the final instruction of the basic
 * block. A basic block containing a {@link ThrowingInsn}, for example, has
 * its successor list in an order identical to the
 * {@link ThrowingInsn#getCatches} instruction's catches list, with the
 * primary successor (the no-exception case) listed at the end.
 *
 * It is legal for a basic block to have no primary successor. An obvious
 * example of this is a block that terminates in a {@link Rops#THROW throw}
 * instruction where a catch block exists inside the current method for that
 * exception class. Since the only possible path is the exception path, only
 * the exception path (which cannot be a primary successor) is a successor.
 * An example of this is shown in {@code dx/tests/092-ssa-cfg-edge-cases}.
 *
 * <h2>Rop Instructions</h2>
 *
 * <h3>move-result and move-result-pseudo</h3>
 *
 * An instruction that may throw an exception may not specify a result. This
 * is necessary because the result register will not be assigned to if an
 * exception occurs while processing said instruction and a result assignment
 * may not occur. Since result assignments only occur in the non-exceptional
 * case,  the result assignments for throwing instructions can be said to occur
 * at the beginning of the primary successor block rather than at the end of
 * the current block. The Rop form represents the result assignments this way.
 * Throwing instructions may not directly specify results. Instead, result
 * assignments are represented by {@link
 * Rops#MOVE_RESULT move-result} or {@link Rops#MOVE_RESULT_PSEUDO
 * move-result-pseudo} instructions at the top of the primary successor block.
 *
 * Only a single {@code move-result} or {@code move-result-pseudo}
 * may exist in any block and it must be exactly the first instruction in the
 * block.
 *
 * A {@code move-result} instruction is used for the results of call-like
 * instructions. If the value produced by a {@code move-result} is not
 * used by the method, it may be eliminated as dead code.
 *
 * A {@code move-result-pseudo} instruction is used for the results of
 * non-call-like throwing instructions. It may never be considered dead code
 * since the final dex instruction will always indicate a result register.
 * If a required {@code move-result-pseudo} instruction is not found
 * during conversion to dex bytecode, an exception will be thrown.
 *
 * <h3>move-exception</h3>
 *
 * A {@link RegOps.MOVE_EXCEPTION move-exception} instruction may appear at
 * the start of a catch block, and represents the obtaining of the thrown
 * exception instance. It may only occur as the first instruction in a
 * basic block, and any basic block in which it occurs must be reachable only
 * as an exception successor.
 *
 * <h3>move-param</h3>
 *
 * A {@link RegOps.MOVE_PARAM move-param} instruction represents a method
 * parameter. Every {@code move-param} instruction is a
 * {@link PlainCstInsn}. The index of the method parameter they refer to is
 * carried as the {@link CstInteger integer constant} associated with the
 * instruction.
 *
 * Any number of {@code move-param} instructions referring to the same
 * parameter index may be included in a method's instruction lists. They
 * have no restrictions on placement beyond those of any other
 * {@link Rop.BRANCH_NONE} instruction. Note that the SSA optimizer arranges the
 * parameter assignments to align with the dex bytecode calling conventions.
 * With parameter assignments so arranged, the
 * {@link external.com.android.dx.dex.code.RopTranslator} sees Rop {@code move-param}
 * instructions as unnecessary in dex form and eliminates them.
 *
 * <h3>mark-local</h3>
 *
 * A {@link RegOps.MARK_LOCAL mark-local} instruction indicates that a local
 * variable becomes live in a specified register specified register for the
 * purposes of debug information. A {@code mark-local} instruction has
 * a single source (the register which will now be considered a local variable)
 * and no results. The instruction has no side effect.<p>
 *
 * In a typical case, a local variable's lifetime begins with an
 * assignment. The local variable whose information is present in a result's
 * {@link RegisterSpec#getLocalItem LocalItem} is considered to begin (or move
 * between registers) when the instruction is executed.<p>
 *
 * However, sometimes a local variable can begin its life or move without
 * an assignment occurring. A common example of this is occurs in the Rop
 * representation of the following code:<p>
 *
 * <pre>
 * try {
 *     Object foo = null;
 *     foo = new Object();
 * } catch (Throwable ex) { }
 * </pre>
 *
 * An object's initialization occurs in two steps. First, a
 * {@code new-instance} instruction is executed, whose result is stored in a
 * register. However, that register can not yet be considered to contain
 * "foo". That's because the instance's constructor method must be called
 * via an {@code invoke} instruction. The constructor method, however, may
 * throw an exception. And if an exception occurs, then "foo" should remain
 * null. So "foo" becomes the value of the result of the {@code new-instance}
 * instruction after the (void) constructor method is invoked and
 * returns successfully. In such a case, a {@code mark-local} will
 * typically occur at the beginning of the primary successor block following
 * the invocation to the constructor.
 */

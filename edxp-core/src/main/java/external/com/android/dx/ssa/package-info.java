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

package external.com.android.dx.ssa;

/**
 * <h1>An introduction to SSA Form</h1>
 *
 * This package contains classes associated with dx's {@code SSA}
 * intermediate form. This form is a static-single-assignment representation of
 * Rop-form a method with Rop-form-like instructions (with the addition of a
 * {@link PhiInsn phi instriction}. This form is intended to make it easy to
 * implement basic optimization steps and register allocation so that a
 * reasonably efficient register machine representation can be produced from a
 * stack machine source bytecode.<p>
 *
 * <h2>Key Classes</h2>
 *
 * <h3>Classes related to conversion and lifetime</h3>
 * <ul>
 * <li> {@link Optimizer} is a singleton class containing methods for
 * converting, optimizing, and then back-converting Rop-form methods. It's the
 * typical gateway into the rest of the package.
 * <li> {@link SsaConverter} converts a Rop-form method to SSA form.
 * <li> {@link SsaToRop} converts an SSA-form method back to Rop form.
 * </ul>
 *
 * <h3>Classes related to method representation</h3>
 * <ul>
 * <li> A {@link SsaMethod} instance represents a method.
 * <li> A {@link SsaBasicBlock} instance represents a basic block, whose
 * semantics are quite similar to basic blocks in
 * {@link external.com.android.dx.rop Rop form}.
 * <li> {@link PhiInsn} instances represent "phi" operators defined in SSA
 * literature. They must be the first N instructions in a basic block.
 * <li> {@link NormalSsaInsn} instances represent instructions that directly
 * correspond to {@code Rop} form.
 * </ul>
 *
 * <h3>Classes related to optimization steps</h3>
 * <ul>
 * <li> {@link MoveParamCombiner} is a simple step that ensures each method
 * parameter is represented by at most one SSA register.
 * <li> {@link SCCP} is a (partially implemented) sparse-conditional
 * constant propogator.
 * <li> {@link LiteralOpUpgrader} is a step that attempts to use constant
 * information to convert math and comparison instructions into
 * constant-bearing "literal ops" in cases where they can be represented in the
 * output form (see {@link TranslationAdvice#hasConstantOperation}).
 * <li> {@link ConstCollector} is a step that attempts to trade (modest)
 * increased register space for decreased instruction count in cases where
 * the same constant value is used repeatedly in a single method.
 * <li> {@link DeadCodeRemover} is a dead code remover. This phase must
 * always be run to remove unused phi instructions.
 * </ul>
 *
 * <h2>SSA Lifetime</h2>
 * The representation of a method in SSA form obeys slightly different
 * constraints depending upon whether it is in the process of being converted
 * into or out of SSA form.
 *
 * <h3>Conversion into SSA Form</h3>
 *
 * {@link SsaConverter#convertToSsaMethod} takes a {@code RopMethod} and
 * returns a fully-converted {@code SsaMethod}. The conversion process
 * is roughly as follows:
 *
 * <ol>
 * <li> The Rop-form method, its blocks and their instructions are directly
 * wrapped in {@code SsaMethod}, {@code SsaBasicBlock} and
 * {@code SsaInsn} instances. Nothing else changes.
 * <li> Critical control-flow graph edges are {@link SsaConverter#edgeSplit
 * split} and new basic blocks inserted as required to meet the constraints
 * necessary for the ultimate SSA representation.
 * <li> A {@link LocalVariableExtractor} is run to produce a table of
 * Rop registers to local variables necessary during phi placement. This
 * step could also be done in Rop form and then updated through the preceding
 * steps.
 * <li> {@code Phi} instructions are {link SsaConverter#placePhiFunctions}
 * placed in a semi-pruned fashion, which requires computation of {@link
 * Dominators dominance graph} and each node's {@link DomFront
 * dominance-frontier set}.
 * <li> Finally, source and result registers for all instructions are {@link
 * SsaRenamer renamed} such that each assignment is given a unique register
 * number (register categories or widths, significant in Rop form, do not
 * exist in SSA). Move instructions are eliminated except where necessary
 * to preserve local variable assignments.
 * </ol>
 *
 */

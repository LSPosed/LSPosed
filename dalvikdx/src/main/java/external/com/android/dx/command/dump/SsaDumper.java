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

package external.com.android.dx.command.dump;

import external.com.android.dx.cf.code.ConcreteMethod;
import external.com.android.dx.cf.code.Ropper;
import external.com.android.dx.cf.iface.Member;
import external.com.android.dx.cf.iface.Method;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.code.DexTranslationAdvice;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.TranslationAdvice;
import external.com.android.dx.ssa.Optimizer;
import external.com.android.dx.ssa.SsaBasicBlock;
import external.com.android.dx.ssa.SsaInsn;
import external.com.android.dx.ssa.SsaMethod;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;

/**
 * Dumper for the SSA-translated blocks of a method.
 */
public class SsaDumper extends BlockDumper {
    /**
     * Does the dump.
     *
     * @param bytes {@code non-null;} bytes of the original class file
     * @param out {@code non-null;} where to dump to
     * @param filePath the file path for the class, excluding any base
     * directory specification
     * @param args commandline parsedArgs
     */
    public static void dump(byte[] bytes, PrintStream out,
            String filePath, Args args) {
        SsaDumper sd = new SsaDumper(bytes, out, filePath, args);
        sd.dump();
    }

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} bytes of the original class file
     * @param out {@code non-null;} where to dump to
     * @param filePath the file path for the class, excluding any base
     * directory specification
     * @param args commandline parsedArgs
     */
    private SsaDumper(byte[] bytes, PrintStream out, String filePath,
            Args args) {
        super(bytes, out, filePath, true, args);
    }

    /** {@inheritDoc} */
    @Override
    public void endParsingMember(ByteArray bytes, int offset, String name,
            String descriptor, Member member) {
        if (!(member instanceof Method)) {
            return;
        }

        if (!shouldDumpMethod(name)) {
            return;
        }

        if ((member.getAccessFlags() & (AccessFlags.ACC_ABSTRACT |
                AccessFlags.ACC_NATIVE)) != 0) {
            return;
        }

        ConcreteMethod meth =
            new ConcreteMethod((Method) member, classFile, true, true);
        TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
        RopMethod rmeth = Ropper.convert(meth, advice, classFile.getMethods(), dexOptions);
        SsaMethod ssaMeth = null;
        boolean isStatic = AccessFlags.isStatic(meth.getAccessFlags());
        int paramWidth = computeParamWidth(meth, isStatic);

        if (args.ssaStep == null) {
            ssaMeth = Optimizer.debugNoRegisterAllocation(rmeth,
                    paramWidth, isStatic, true, advice,
                    EnumSet.allOf(Optimizer.OptionalStep.class));
        } else if ("edge-split".equals(args.ssaStep)) {
            ssaMeth = Optimizer.debugEdgeSplit(rmeth, paramWidth,
                    isStatic, true, advice);
        } else if ("phi-placement".equals(args.ssaStep)) {
            ssaMeth = Optimizer.debugPhiPlacement(
                    rmeth, paramWidth, isStatic, true, advice);
        } else if ("renaming".equals(args.ssaStep)) {
            ssaMeth = Optimizer.debugRenaming(
                    rmeth, paramWidth, isStatic, true, advice);
        } else if ("dead-code".equals(args.ssaStep)) {
            ssaMeth = Optimizer.debugDeadCodeRemover(
                    rmeth, paramWidth, isStatic,true, advice);
        }

        StringBuilder sb = new StringBuilder(2000);

        sb.append("first ");
        sb.append(Hex.u2(
                ssaMeth.blockIndexToRopLabel(ssaMeth.getEntryBlockIndex())));
        sb.append('\n');

        ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();
        ArrayList<SsaBasicBlock> sortedBlocks =
            (ArrayList<SsaBasicBlock>) blocks.clone();
        Collections.sort(sortedBlocks, SsaBasicBlock.LABEL_COMPARATOR);

        for (SsaBasicBlock block : sortedBlocks) {
            sb.append("block ")
                    .append(Hex.u2(block.getRopLabel())).append('\n');

            BitSet preds = block.getPredecessors();

            for (int i = preds.nextSetBit(0); i >= 0;
                 i = preds.nextSetBit(i+1)) {
                sb.append("  pred ");
                sb.append(Hex.u2(ssaMeth.blockIndexToRopLabel(i)));
                sb.append('\n');
            }

            sb.append("  live in:" + block.getLiveInRegs());
            sb.append("\n");

            for (SsaInsn insn : block.getInsns()) {
                sb.append("  ");
                sb.append(insn.toHuman());
                sb.append('\n');
            }

            if (block.getSuccessors().cardinality() == 0) {
                sb.append("  returns\n");
            } else {
                int primary = block.getPrimarySuccessorRopLabel();

                IntList succLabelList = block.getRopLabelSuccessorList();

                int szSuccLabels = succLabelList.size();

                for (int i = 0; i < szSuccLabels; i++) {
                    sb.append("  next ");
                    sb.append(Hex.u2(succLabelList.get(i)));

                    if (szSuccLabels != 1 && primary == succLabelList.get(i)) {
                        sb.append(" *");
                    }
                    sb.append('\n');
                }
            }

            sb.append("  live out:" + block.getLiveOutRegs());
            sb.append("\n");
        }

        suppressDump = false;
        parsed(bytes, 0, bytes.size(), sb.toString());
        suppressDump = true;
    }
}

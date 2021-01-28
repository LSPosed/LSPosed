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

import external.com.android.dx.cf.code.BasicBlocker;
import external.com.android.dx.cf.code.ByteBlock;
import external.com.android.dx.cf.code.ByteBlockList;
import external.com.android.dx.cf.code.ByteCatchList;
import external.com.android.dx.cf.code.BytecodeArray;
import external.com.android.dx.cf.code.ConcreteMethod;
import external.com.android.dx.cf.code.Ropper;
import external.com.android.dx.cf.direct.CodeObserver;
import external.com.android.dx.cf.direct.DirectClassFile;
import external.com.android.dx.cf.direct.StdAttributeFactory;
import external.com.android.dx.cf.iface.Member;
import external.com.android.dx.cf.iface.Method;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.code.BasicBlock;
import external.com.android.dx.rop.code.BasicBlockList;
import external.com.android.dx.rop.code.DexTranslationAdvice;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.InsnList;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.TranslationAdvice;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.ssa.Optimizer;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;
import java.io.PrintStream;

/**
 * Utility to dump basic block info from methods in a human-friendly form.
 */
public class BlockDumper
        extends BaseDumper {
    /** whether or not to registerize (make rop blocks) */
    private final boolean rop;

    /**
     * {@code null-ok;} the class file object being constructed;
     * becomes non-null during {@link #dump}
     */
    protected DirectClassFile classFile;

    /** whether or not to suppress dumping */
    protected boolean suppressDump;

    /** whether this is the first method being dumped */
    private boolean first;

    /** whether or not to run the ssa optimziations */
    private final boolean optimize;

    /**
     * Dumps the given array, interpreting it as a class file and dumping
     * methods with indications of block-level stuff.
     *
     * @param bytes {@code non-null;} bytes of the (alleged) class file
     * @param out {@code non-null;} where to dump to
     * @param filePath the file path for the class, excluding any base
     * directory specification
     * @param rop whether or not to registerize (make rop blocks)
     * @param args commandline parsedArgs
     */
    public static void dump(byte[] bytes, PrintStream out,
            String filePath, boolean rop, Args args) {
        BlockDumper bd = new BlockDumper(bytes, out, filePath,
                rop, args);
        bd.dump();
    }

    /**
     * Constructs an instance. This class is not publicly instantiable.
     * Use {@link #dump}.
     */
    BlockDumper(byte[] bytes, PrintStream out, String filePath,
            boolean rop, Args args) {
        super(bytes, out, filePath, args);

        this.rop = rop;
        this.classFile = null;
        this.suppressDump = true;
        this.first = true;
        this.optimize = args.optimize;
    }

    /**
     * Does the dumping.
     */
    public void dump() {
        byte[] bytes = getBytes();
        ByteArray ba = new ByteArray(bytes);

        /*
         * First, parse the file completely, so we can safely refer to
         * attributes, etc.
         */
        classFile = new DirectClassFile(ba, getFilePath(), getStrictParse());
        classFile.setAttributeFactory(StdAttributeFactory.THE_ONE);
        classFile.getMagic(); // Force parsing to happen.

        // Next, reparse it and observe the process.
        DirectClassFile liveCf =
            new DirectClassFile(ba, getFilePath(), getStrictParse());
        liveCf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        liveCf.setObserver(this);
        liveCf.getMagic(); // Force parsing to happen.
    }

    /** {@inheritDoc} */
    @Override
    public void changeIndent(int indentDelta) {
        if (!suppressDump) {
            super.changeIndent(indentDelta);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parsed(ByteArray bytes, int offset, int len, String human) {
        if (!suppressDump) {
            super.parsed(bytes, offset, len, human);
        }
    }

    /**
     * @param name method name
     * @return true if this method should be dumped
     */
    protected boolean shouldDumpMethod(String name) {
        return args.method == null || args.method.equals(name);
    }

    /** {@inheritDoc} */
    @Override
    public void startParsingMember(ByteArray bytes, int offset, String name,
            String descriptor) {
        if (descriptor.indexOf('(') < 0) {
            // It's a field, not a method
            return;
        }

        if (!shouldDumpMethod(name)) {
            return;
        }

        suppressDump = false;

        if (first) {
            first = false;
        } else {
            parsed(bytes, offset, 0, "\n");
        }

        parsed(bytes, offset, 0, "method " + name + " " + descriptor);
        suppressDump = true;
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

        if (rop) {
            ropDump(meth);
        } else {
            regularDump(meth);
        }
    }

    /**
     * Does a regular basic block dump.
     *
     * @param meth {@code non-null;} method data to dump
     */
    private void regularDump(ConcreteMethod meth) {
        BytecodeArray code = meth.getCode();
        ByteArray bytes = code.getBytes();
        ByteBlockList list = BasicBlocker.identifyBlocks(meth);
        int sz = list.size();
        CodeObserver codeObserver = new CodeObserver(bytes, BlockDumper.this);

        suppressDump = false;

        int byteAt = 0;
        for (int i = 0; i < sz; i++) {
            ByteBlock bb = list.get(i);
            int start = bb.getStart();
            int end = bb.getEnd();

            if (byteAt < start) {
                parsed(bytes, byteAt, start - byteAt,
                       "dead code " + Hex.u2(byteAt) + ".." + Hex.u2(start));
            }

            parsed(bytes, start, 0,
                    "block " + Hex.u2(bb.getLabel()) + ": " +
                    Hex.u2(start) + ".." + Hex.u2(end));
            changeIndent(1);

            int len;
            for (int j = start; j < end; j += len) {
                len = code.parseInstruction(j, codeObserver);
                codeObserver.setPreviousOffset(j);
            }

            IntList successors = bb.getSuccessors();
            int ssz = successors.size();
            if (ssz == 0) {
                parsed(bytes, end, 0, "returns");
            } else {
                for (int j = 0; j < ssz; j++) {
                    int succ = successors.get(j);
                    parsed(bytes, end, 0, "next " + Hex.u2(succ));
                }
            }

            ByteCatchList catches = bb.getCatches();
            int csz = catches.size();
            for (int j = 0; j < csz; j++) {
                ByteCatchList.Item one = catches.get(j);
                CstType exceptionClass = one.getExceptionClass();
                parsed(bytes, end, 0,
                       "catch " +
                       ((exceptionClass == CstType.OBJECT) ? "<any>" :
                        exceptionClass.toHuman()) + " -> " +
                       Hex.u2(one.getHandlerPc()));
            }

            changeIndent(-1);
            byteAt = end;
        }

        int end = bytes.size();
        if (byteAt < end) {
            parsed(bytes, byteAt, end - byteAt,
                    "dead code " + Hex.u2(byteAt) + ".." + Hex.u2(end));
        }

        suppressDump = true;
    }

    /**
     * Does a registerizing dump.
     *
     * @param meth {@code non-null;} method data to dump
     */
    private void ropDump(ConcreteMethod meth) {
        TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
        BytecodeArray code = meth.getCode();
        ByteArray bytes = code.getBytes();
        RopMethod rmeth = Ropper.convert(meth, advice, classFile.getMethods(), dexOptions);
        StringBuilder sb = new StringBuilder(2000);

        if (optimize) {
            boolean isStatic = AccessFlags.isStatic(meth.getAccessFlags());
            int paramWidth = computeParamWidth(meth, isStatic);
            rmeth =
                Optimizer.optimize(rmeth, paramWidth, isStatic, true, advice);
        }

        BasicBlockList blocks = rmeth.getBlocks();
        int[] order = blocks.getLabelsInOrder();

        sb.append("first " + Hex.u2(rmeth.getFirstLabel()) + "\n");

        for (int label : order) {
            BasicBlock bb = blocks.get(blocks.indexOfLabel(label));
            sb.append("block ");
            sb.append(Hex.u2(label));
            sb.append("\n");

            IntList preds = rmeth.labelToPredecessors(label);
            int psz = preds.size();
            for (int i = 0; i < psz; i++) {
                sb.append("  pred ");
                sb.append(Hex.u2(preds.get(i)));
                sb.append("\n");
            }

            InsnList il = bb.getInsns();
            int ilsz = il.size();
            for (int i = 0; i < ilsz; i++) {
                Insn one = il.get(i);
                sb.append("  ");
                sb.append(il.get(i).toHuman());
                sb.append("\n");
            }

            IntList successors = bb.getSuccessors();
            int ssz = successors.size();
            if (ssz == 0) {
                sb.append("  returns\n");
            } else {
                int primary = bb.getPrimarySuccessor();
                for (int i = 0; i < ssz; i++) {
                    int succ = successors.get(i);
                    sb.append("  next ");
                    sb.append(Hex.u2(succ));

                    if ((ssz != 1) && (succ == primary)) {
                        sb.append(" *");
                    }

                    sb.append("\n");
                }
            }
        }

        suppressDump = false;
        parsed(bytes, 0, bytes.size(), sb.toString());
        suppressDump = true;
    }
}

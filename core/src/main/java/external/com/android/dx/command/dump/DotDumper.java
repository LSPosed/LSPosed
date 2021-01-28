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
import external.com.android.dx.cf.direct.DirectClassFile;
import external.com.android.dx.cf.direct.StdAttributeFactory;
import external.com.android.dx.cf.iface.Member;
import external.com.android.dx.cf.iface.Method;
import external.com.android.dx.cf.iface.ParseObserver;
import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.code.BasicBlock;
import external.com.android.dx.rop.code.BasicBlockList;
import external.com.android.dx.rop.code.DexTranslationAdvice;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.TranslationAdvice;
import external.com.android.dx.ssa.Optimizer;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IntList;

/**
 * Dumps the pred/succ graph of methods into a format compatible
 * with the popular graph utility "dot".
 */
public class DotDumper implements ParseObserver {
    private DirectClassFile classFile;

    private final byte[] bytes;
    private final String filePath;
    private final boolean strictParse;
    private final boolean optimize;
    private final Args args;
    private final DexOptions dexOptions;

    static void dump(byte[] bytes, String filePath, Args args) {
        new DotDumper(bytes, filePath, args).run();
    }

    DotDumper(byte[] bytes, String filePath, Args args) {
        this.bytes = bytes;
        this.filePath = filePath;
        this.strictParse = args.strictParse;
        this.optimize = args.optimize;
        this.args = args;
        this.dexOptions = new DexOptions();
    }

    private void run() {
        ByteArray ba = new ByteArray(bytes);

        /*
         * First, parse the file completely, so we can safely refer to
         * attributes, etc.
         */
        classFile = new DirectClassFile(ba, filePath, strictParse);
        classFile.setAttributeFactory(StdAttributeFactory.THE_ONE);
        classFile.getMagic(); // Force parsing to happen.

        // Next, reparse it and observe the process.
        DirectClassFile liveCf =
            new DirectClassFile(ba, filePath, strictParse);
        liveCf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        liveCf.setObserver(this);
        liveCf.getMagic(); // Force parsing to happen.
    }

    /**
     * @param name method name
     * @return true if this method should be dumped
     */
    protected boolean shouldDumpMethod(String name) {
        return args.method == null || args.method.equals(name);
    }

    @Override
    public void changeIndent(int indentDelta) {
        // This space intentionally left blank.
    }

    @Override
    public void parsed(ByteArray bytes, int offset, int len, String human) {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public void startParsingMember(ByteArray bytes, int offset, String name,
                                   String descriptor) {
        // This space intentionally left blank.
    }

    @Override
    public void endParsingMember(ByteArray bytes, int offset, String name,
                                 String descriptor, Member member) {
        if (!(member instanceof Method)) {
            return;
        }

        if (!shouldDumpMethod(name)) {
            return;
        }

        ConcreteMethod meth = new ConcreteMethod((Method) member, classFile,
                                                 true, true);

        TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
        RopMethod rmeth =
            Ropper.convert(meth, advice, classFile.getMethods(), dexOptions);

        if (optimize) {
            boolean isStatic = AccessFlags.isStatic(meth.getAccessFlags());
            rmeth = Optimizer.optimize(rmeth,
                    BaseDumper.computeParamWidth(meth, isStatic), isStatic,
                    true, advice);
        }

        System.out.println("digraph "  + name + "{");

        System.out.println("\tfirst -> n"
                + Hex.u2(rmeth.getFirstLabel()) + ";");

        BasicBlockList blocks = rmeth.getBlocks();

        int sz = blocks.size();
        for (int i = 0; i < sz; i++) {
            BasicBlock bb = blocks.get(i);
            int label = bb.getLabel();
            IntList successors = bb.getSuccessors();

            if (successors.size() == 0) {
                System.out.println("\tn" + Hex.u2(label) + " -> returns;");
            } else if (successors.size() == 1) {
                System.out.println("\tn" + Hex.u2(label) + " -> n"
                        + Hex.u2(successors.get(0)) + ";");
            } else {
                System.out.print("\tn" + Hex.u2(label) + " -> {");
                for (int j = 0; j < successors.size(); j++ ) {
                    int successor = successors.get(j);

                    if (successor != bb.getPrimarySuccessor()) {
                        System.out.print(" n" + Hex.u2(successor) + " ");
                    }

                }
                System.out.println("};");

                System.out.println("\tn" + Hex.u2(label) + " -> n"
                        + Hex.u2(bb.getPrimarySuccessor())
                        + " [label=\"primary\"];");


            }
        }

        System.out.println("}");
    }
}

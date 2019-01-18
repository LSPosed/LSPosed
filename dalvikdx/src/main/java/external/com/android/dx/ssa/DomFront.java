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

package external.com.android.dx.ssa;

import external.com.android.dx.util.IntSet;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * Calculates the dominance-frontiers of a method's basic blocks.
 * Algorithm from "A Simple, Fast Dominance Algorithm" by Cooper,
 * Harvey, and Kennedy; transliterated to Java.
 */
public class DomFront {
    /** local debug flag */
    private static final boolean DEBUG = false;

    /** {@code non-null;} method being processed */
    private final SsaMethod meth;

    private final ArrayList<SsaBasicBlock> nodes;

    private final DomInfo[] domInfos;

    /**
     * Dominance-frontier information for a single basic block.
     */
    public static class DomInfo {
        /**
         * {@code null-ok;} the dominance frontier set indexed by
         * block index
         */
        public IntSet dominanceFrontiers;

        /** {@code >= 0 after run();} the index of the immediate dominator */
        public int idom = -1;
    }

    /**
     * Constructs instance. Call {@link DomFront#run} to process.
     *
     * @param meth {@code non-null;} method to process
     */
    public DomFront(SsaMethod meth) {
        this.meth = meth;
        nodes = meth.getBlocks();

        int szNodes = nodes.size();
        domInfos = new DomInfo[szNodes];

        for (int i = 0; i < szNodes; i++) {
            domInfos[i] = new DomInfo();
        }
    }

    /**
     * Calculates the dominance frontier information for the method.
     *
     * @return {@code non-null;} an array of DomInfo structures
     */
    public DomInfo[] run() {
        int szNodes = nodes.size();

        if (DEBUG) {
            for (int i = 0; i < szNodes; i++) {
                SsaBasicBlock node = nodes.get(i);
                System.out.println("pred[" + i + "]: "
                        + node.getPredecessors());
            }
        }

        Dominators methDom = Dominators.make(meth, domInfos, false);

        if (DEBUG) {
            for (int i = 0; i < szNodes; i++) {
                DomInfo info = domInfos[i];
                System.out.println("idom[" + i + "]: "
                        + info.idom);
            }
        }

        buildDomTree();

        if (DEBUG) {
            debugPrintDomChildren();
        }

        for (int i = 0; i < szNodes; i++) {
            domInfos[i].dominanceFrontiers
                    = SetFactory.makeDomFrontSet(szNodes);
        }

        calcDomFronts();

        if (DEBUG) {
            for (int i = 0; i < szNodes; i++) {
                System.out.println("df[" + i + "]: "
                        + domInfos[i].dominanceFrontiers);
            }
        }

        return domInfos;
    }

    private void debugPrintDomChildren() {
        int szNodes = nodes.size();

        for (int i = 0; i < szNodes; i++) {
            SsaBasicBlock node = nodes.get(i);
            StringBuffer sb = new StringBuffer();

            sb.append('{');
            boolean comma = false;
            for (SsaBasicBlock child : node.getDomChildren()) {
                if (comma) {
                    sb.append(',');
                }
                sb.append(child);
                comma = true;
            }
            sb.append('}');

            System.out.println("domChildren[" + node + "]: "
                    + sb);
        }
    }

    /**
     * The dominators algorithm leaves us knowing who the immediate dominator
     * is for each node. This sweeps the node list and builds the proper
     * dominance tree.
     */
    private void buildDomTree() {
        int szNodes = nodes.size();

        for (int i = 0; i < szNodes; i++) {
            DomInfo info = domInfos[i];

            if (info.idom == -1) continue;

            SsaBasicBlock domParent = nodes.get(info.idom);
            domParent.addDomChild(nodes.get(i));
        }
    }

    /**
     * Calculates the dominance-frontier set.
     * from "A Simple, Fast Dominance Algorithm" by Cooper,
     * Harvey, and Kennedy; transliterated to Java.
     */
    private void calcDomFronts() {
        int szNodes = nodes.size();

        for (int b = 0; b < szNodes; b++) {
            SsaBasicBlock nb = nodes.get(b);
            DomInfo nbInfo = domInfos[b];
            BitSet pred = nb.getPredecessors();

            if (pred.cardinality() > 1) {
                for (int i = pred.nextSetBit(0); i >= 0;
                     i = pred.nextSetBit(i + 1)) {

                    for (int runnerIndex = i;
                         runnerIndex != nbInfo.idom; /* empty */) {
                        /*
                         * We can stop if we hit a block we already
                         * added label to, since we must be at a part
                         * of the dom tree we have seen before.
                         */
                        if (runnerIndex == -1) break;

                        DomInfo runnerInfo = domInfos[runnerIndex];

                        if (runnerInfo.dominanceFrontiers.has(b)) {
                            break;
                        }

                        // Add b to runner's dominance frontier set.
                        runnerInfo.dominanceFrontiers.add(b);
                        runnerIndex = runnerInfo.idom;
                    }
                }
            }
        }
    }
}

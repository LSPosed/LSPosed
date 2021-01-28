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

package external.com.android.dx.dex.cf;

import external.com.android.dx.dex.code.DalvCode;
import external.com.android.dx.rop.code.RopMethod;
import java.io.PrintStream;

/**
 * Static methods and variables for collecting statistics on generated
 * code.
 */
public final class CodeStatistics {
    /** set to {@code true} to enable development-time debugging code */
    private static final boolean DEBUG = false;

    /**
     * running sum of the number of registers added/removed in
     * SSA form by the optimizer
     */
    public int runningDeltaRegisters = 0;

    /**
     * running sum of the number of insns added/removed in
     * SSA form by the optimizer
     */
    public int runningDeltaInsns = 0;

    /** running sum of the total number of Rop insns processed */
    public int runningTotalInsns = 0;

    /**
     * running sum of the number of dex-form registers added/removed in
     * SSA form by the optimizer. Only valid if args.statistics is true.
     */
    public int dexRunningDeltaRegisters = 0;

    /**
     * running sum of the number of dex-form insns (actually code
     * units) added/removed in SSA form by the optimizer. Only valid
     * if args.statistics is true.
     */
    public int dexRunningDeltaInsns = 0;

    /**
     * running sum of the total number of dex insns (actually code
     * units) processed
     */
    public int dexRunningTotalInsns = 0;

    /** running sum of original class bytecode bytes */
    public int runningOriginalBytes = 0;

    /**
     * Updates the number of original bytecode bytes processed.
     *
     * @param count {@code >= 0;} the number of bytes to add
     */
    public void updateOriginalByteCount(int count) {
        runningOriginalBytes += count;
    }

    /**
     * Updates the dex statistics.
     *
     * @param nonOptCode non-optimized code block
     * @param code optimized code block
     */
    public void updateDexStatistics(DalvCode nonOptCode,
            DalvCode code) {
        if (DEBUG) {
            System.err.println("dex insns (old/new) "
                    + nonOptCode.getInsns().codeSize()
                    + "/" + code.getInsns().codeSize()
                    + " regs (o/n) "
                    + nonOptCode.getInsns().getRegistersSize()
                    + "/" + code.getInsns().getRegistersSize()
            );
        }

        dexRunningDeltaInsns
            += (code.getInsns().codeSize()
                - nonOptCode.getInsns().codeSize());

        dexRunningDeltaRegisters
            += (code.getInsns().getRegistersSize()
                - nonOptCode.getInsns().getRegistersSize());

        dexRunningTotalInsns += code.getInsns().codeSize();
    }

    /**
     * Updates the ROP statistics.
     *
     * @param nonOptRmeth non-optimized method
     * @param rmeth optimized method
     */
    public void updateRopStatistics(RopMethod nonOptRmeth,
            RopMethod rmeth) {
        int oldCountInsns
                = nonOptRmeth.getBlocks().getEffectiveInstructionCount();
        int oldCountRegs = nonOptRmeth.getBlocks().getRegCount();

        if (DEBUG) {
            System.err.println("insns (old/new): "
                    + oldCountInsns + "/"
                    + rmeth.getBlocks().getEffectiveInstructionCount()
                    + " regs (o/n):" + oldCountRegs
                    + "/"  +  rmeth.getBlocks().getRegCount());
        }

        int newCountInsns
                = rmeth.getBlocks().getEffectiveInstructionCount();

        runningDeltaInsns
            += (newCountInsns - oldCountInsns);

        runningDeltaRegisters
            += (rmeth.getBlocks().getRegCount() - oldCountRegs);

        runningTotalInsns += newCountInsns;
    }

    /**
     * Prints out the collected statistics.
     *
     * @param out {@code non-null;} where to output to
     */
    public void dumpStatistics(PrintStream out) {
        out.printf("Optimizer Delta Rop Insns: %d total: %d "
                + "(%.2f%%) Delta Registers: %d\n",
                runningDeltaInsns,
                runningTotalInsns,
                (100.0 * (((float) runningDeltaInsns)
                        / (runningTotalInsns + Math.abs(runningDeltaInsns)))),
                runningDeltaRegisters);

        out.printf("Optimizer Delta Dex Insns: Insns: %d total: %d "
                + "(%.2f%%) Delta Registers: %d\n",
                dexRunningDeltaInsns,
                dexRunningTotalInsns,
                (100.0 * (((float) dexRunningDeltaInsns)
                        / (dexRunningTotalInsns
                                + Math.abs(dexRunningDeltaInsns)))),
                dexRunningDeltaRegisters);

        out.printf("Original bytecode byte count: %d\n",
                runningOriginalBytes);
    }
}

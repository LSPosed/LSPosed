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

import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.TranslationAdvice;
import external.com.android.dx.ssa.Optimizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;

/**
 * Settings for optimization of code.
 */
public class OptimizerOptions {
    /**
     * {@code null-ok;} hash set of class name + method names that
     * should be optimized. {@code null} if this constraint was not
     * specified on the command line
     */
    private HashSet<String> optimizeList;

    /**
     * {@code null-ok;} hash set of class name + method names that should NOT
     * be optimized.  null if this constraint was not specified on the
     * command line
     */
    private HashSet<String> dontOptimizeList;

    /** true if the above lists have been loaded */
    private boolean optimizeListsLoaded;


    /**
     * Loads the optimize/don't optimize lists from files.
     *
     * @param optimizeListFile Pathname
     * @param dontOptimizeListFile Pathname
     */
    public void loadOptimizeLists(String optimizeListFile,
            String dontOptimizeListFile) {
        if (optimizeListsLoaded) {
            return;
        }

        if (optimizeListFile != null && dontOptimizeListFile != null) {
            /*
             * We shouldn't get this far. The condition should have
             * been caught in the arg processor.
             */
            throw new RuntimeException("optimize and don't optimize lists "
                    + " are mutually exclusive.");
        }

        if (optimizeListFile != null) {
            optimizeList = loadStringsFromFile(optimizeListFile);
        }

        if (dontOptimizeListFile != null) {
            dontOptimizeList = loadStringsFromFile(dontOptimizeListFile);
        }

        optimizeListsLoaded = true;
    }

    /**
     * Loads a list of newline-separated strings into a new HashSet and returns
     * the HashSet.
     *
     * @param filename filename to process
     * @return set of all unique lines in the file
     */
    private static HashSet<String> loadStringsFromFile(String filename) {
        HashSet<String> result = new HashSet<String>();

        try {
            FileReader fr = new FileReader(filename);
            BufferedReader bfr = new BufferedReader(fr);

            String line;

            while (null != (line = bfr.readLine())) {
                result.add(line);
            }

            fr.close();
        } catch (IOException ex) {
            // Let the exception percolate up as a RuntimeException.
            throw new RuntimeException("Error with optimize list: " +
                    filename, ex);
        }

        return result;
    }

    /**
     * Compares the output of the optimizer run normally with a run skipping
     * some optional steps. Results are printed to stderr.
     *
     * @param nonOptRmeth {@code non-null;} origional rop method
     * @param paramSize {@code >= 0;} parameter size of method
     * @param isStatic true if this method has no 'this' pointer argument.
     * @param args {@code non-null;} translator arguments
     * @param advice {@code non-null;} translation advice
     * @param rmeth {@code non-null;} method with all optimization steps run.
     */
    public void compareOptimizerStep(RopMethod nonOptRmeth,
            int paramSize, boolean isStatic, CfOptions args,
            TranslationAdvice advice, RopMethod rmeth) {
        EnumSet<Optimizer.OptionalStep> steps;

        steps = EnumSet.allOf(Optimizer.OptionalStep.class);

        // This is the step to skip.
        steps.remove(Optimizer.OptionalStep.CONST_COLLECTOR);

        RopMethod skipRopMethod
                = Optimizer.optimize(nonOptRmeth,
                        paramSize, isStatic, args.localInfo, advice, steps);

        int normalInsns
                = rmeth.getBlocks().getEffectiveInstructionCount();
        int skipInsns
                = skipRopMethod.getBlocks().getEffectiveInstructionCount();

        System.err.printf(
                "optimize step regs:(%d/%d/%.2f%%)"
                + " insns:(%d/%d/%.2f%%)\n",
                rmeth.getBlocks().getRegCount(),
                skipRopMethod.getBlocks().getRegCount(),
                100.0 * ((skipRopMethod.getBlocks().getRegCount()
                        - rmeth.getBlocks().getRegCount())
                        / (float) skipRopMethod.getBlocks().getRegCount()),
                normalInsns, skipInsns,
                100.0 * ((skipInsns - normalInsns) / (float) skipInsns));
    }

    /**
     * Checks whether the specified method should be optimized
     *
     * @param canonicalMethodName name of method being considered
     * @return true if it should be optimized
     */
    public boolean shouldOptimize(String canonicalMethodName) {
        // Optimize only what's in the optimize list.
        if (optimizeList != null) {
            return optimizeList.contains(canonicalMethodName);
        }

        /*
         * Or don't optimize what's listed here. (The two lists are
         * mutually exclusive.
         */

        if (dontOptimizeList != null) {
            return !dontOptimizeList.contains(canonicalMethodName);
        }

        // If neither list has been specified, then optimize everything.
        return true;
    }
}

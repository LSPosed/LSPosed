/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx.dex;

import external.com.android.dex.DexFormat;
import external.com.android.dx.dex.code.DalvInsnList;
import java.io.PrintStream;

/**
 * Container for options used to control details of dex file generation.
 */
public final class DexOptions {

    /**
     * Enable alignment support of 64-bit registers on Dalvik even registers. This is a temporary
     * configuration flag allowing to quickly go back on the default behavior to face up to problem.
     */
    public static final boolean ALIGN_64BIT_REGS_SUPPORT = true;

   /**
    * Does final processing of 64-bit alignment into output finisher to gets output as
    * {@link DalvInsnList} with 64-bit registers aligned at best. Disabled the final processing is
    * required for tools such as Dasm to avoid modifying user inputs.
    */
    public boolean ALIGN_64BIT_REGS_IN_OUTPUT_FINISHER = ALIGN_64BIT_REGS_SUPPORT;

    /** minimum SDK version targeted */
    public int minSdkVersion = DexFormat.API_NO_EXTENDED_OPCODES;

    /** force generation of jumbo opcodes */
    public boolean forceJumbo = false;

    /** Enable user override for default and static interface method invocation. */
    public boolean allowAllInterfaceMethodInvokes = false;

    /** output stream for reporting warnings */
    public final PrintStream err;

    public DexOptions() {
        err = System.err;
    }

    public DexOptions(PrintStream stream) {
        err = stream;
    }

    /**
     * Gets the dex file magic number corresponding to this instance.
     * @return string representing the dex file magic number
     */
    public String getMagic() {
        return DexFormat.apiToMagic(minSdkVersion);
    }

    /**
     * Checks whether an API feature is supported.
     * @param apiLevel the API level to test
     * @return returns true if the current API level is at least sdkVersion
     */
    public boolean apiIsSupported(int apiLevel) {
        // TODO: the naming here is awkward. Tooling may rely on the minSdkVersion,
        // but it is referred to as API in DexFormat. Currently indistinguishable.
        return minSdkVersion >= apiLevel;
    }
}

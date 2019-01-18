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

import external.com.android.dx.dex.code.PositionList;
import java.io.PrintStream;

/**
 * A class to contain options passed into dex.cf
 */
public class CfOptions {
    /** how much source position info to preserve */
    public int positionInfo = PositionList.LINES;

    /** whether to keep local variable information */
    public boolean localInfo = false;

    /** whether strict file-name-vs-class-name checking should be done */
    public boolean strictNameCheck = true;

    /** whether to do SSA/register optimization */
    public boolean optimize = false;

    /** filename containing list of methods to optimize */
    public String optimizeListFile = null;

    /** filename containing list of methods <i>not</i> to optimize */
    public String dontOptimizeListFile = null;

    /** whether to print statistics to stdout at end of compile cycle */
    public boolean statistics;

    /** where to issue warnings to */
    public PrintStream warn = System.err;
}

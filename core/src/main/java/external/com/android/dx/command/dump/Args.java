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

/**
 * contains command line parsedArgs values
 */
class Args {
    /** whether to run in debug mode */
    boolean debug = false;

    /** whether to dump raw bytes where salient */
    boolean rawBytes = false;

    /** whether to dump information about basic blocks */
    boolean basicBlocks = false;

    /** whether to dump regiserized blocks */
    boolean ropBlocks = false;

    /** whether to dump SSA-form blocks */
    boolean ssaBlocks = false;

    /** Step in SSA processing to stop at, or null for all */
    String ssaStep = null;

    /** whether to run SSA optimizations */
    boolean optimize = false;

    /** whether to be strict about parsing classfiles*/
    boolean strictParse = false;

    /** max width for columnar output */
    int width = 0;

    /** whether to dump flow-graph in "dot" format */
    boolean dotDump = false;

    /** if non-null, an explicit method to dump */
    String method;

}

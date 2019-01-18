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

import external.com.android.dx.cf.direct.DirectClassFile;
import external.com.android.dx.cf.direct.StdAttributeFactory;
import external.com.android.dx.util.ByteArray;
import java.io.PrintStream;

/**
 * Utility to dump the contents of class files in a human-friendly form.
 */
public final class ClassDumper
        extends BaseDumper {
    /**
     * Dumps the given array, interpreting it as a class file.
     *
     * @param bytes {@code non-null;} bytes of the (alleged) class file
     * @param out {@code non-null;} where to dump to
     * passed in as &lt;= 0
     * @param filePath the file path for the class, excluding any base
     * directory specification
     * @param args bag of commandline arguments
     */
    public static void dump(byte[] bytes, PrintStream out,
                            String filePath, Args args) {
        ClassDumper cd =
            new ClassDumper(bytes, out, filePath, args);
        cd.dump();
    }

    /**
     * Constructs an instance. This class is not publicly instantiable.
     * Use {@link #dump}.
     */
    private ClassDumper(byte[] bytes, PrintStream out,
                        String filePath, Args args) {
        super(bytes, out, filePath, args);
    }

    /**
     * Does the dumping.
     */
    public void dump() {
        byte[] bytes = getBytes();
        ByteArray ba = new ByteArray(bytes);
        DirectClassFile cf =
            new DirectClassFile(ba, getFilePath(), getStrictParse());

        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        cf.setObserver(this);
        cf.getMagic(); // Force parsing to happen.

        int readBytes = getReadBytes();
        if (readBytes != bytes.length) {
            parsed(ba, readBytes, bytes.length - readBytes, "<extra data at end of file>");
        }
    }
}

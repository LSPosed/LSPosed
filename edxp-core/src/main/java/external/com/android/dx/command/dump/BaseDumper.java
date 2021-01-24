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
import external.com.android.dx.cf.iface.Member;
import external.com.android.dx.cf.iface.ParseObserver;
import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;
import external.com.android.dx.util.IndentingWriter;
import external.com.android.dx.util.TwoColumnOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;

/**
 * Base class for the various human-friendly dumpers.
 */
public abstract class BaseDumper
        implements ParseObserver {
    /** {@code non-null;} array of data being dumped */
    private final byte[] bytes;

    /** whether or not to include the raw bytes (in a column on the left) */
    private final boolean rawBytes;

    /** {@code non-null;} where to dump to */
    private final PrintStream out;

    /** width of the output in columns */
    private final int width;

    /**
     * {@code non-null;} the file path for the class, excluding any base
     * directory specification
     */
    private final String filePath;

    /** whether to be strict about parsing */
    private final boolean strictParse;

     /** number of bytes per line in hex dumps */
    private final int hexCols;

    /** the current level of indentation */
    private int indent;

    /** {@code non-null;} the current column separator string */
    private String separator;

    /** the number of read bytes */
    private int readBytes;

    /** commandline parsedArgs */
    protected Args args;

    /** {@code non-null;} options for dex output, always set to the defaults for now */
    protected final DexOptions dexOptions;

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} bytes of the (alleged) class file
     * on the left)
     * @param out {@code non-null;} where to dump to
     * @param filePath the file path for the class, excluding any base
     * directory specification
     */
    public BaseDumper(byte[] bytes, PrintStream out,
                      String filePath, Args args) {
        this.bytes = bytes;
        this.rawBytes = args.rawBytes;
        this.out = out;
        this.width = (args.width <= 0) ? 79 : args.width;
        this.filePath = filePath;
        this.strictParse = args.strictParse;
        this.indent = 0;
        this.separator = rawBytes ? "|" : "";
        this.readBytes = 0;
        this.args = args;

        this.dexOptions = new DexOptions();

        int hexCols = (((width - 5) / 15) + 1) & ~1;
        if (hexCols < 6) {
            hexCols = 6;
        } else if (hexCols > 10) {
            hexCols = 10;
        }
        this.hexCols = hexCols;
    }

    /**
     * Computes the total width, in register-units, of the parameters for
     * this method.
     * @param meth method to process
     * @return width in register-units
     */
    static int computeParamWidth(ConcreteMethod meth, boolean isStatic) {
        return meth.getEffectiveDescriptor().getParameterTypes().
            getWordCount();
    }

    /** {@inheritDoc} */
    @Override
    public void changeIndent(int indentDelta) {
        indent += indentDelta;

        separator = rawBytes ? "|" : "";
        for (int i = 0; i < indent; i++) {
            separator += "  ";
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parsed(ByteArray bytes, int offset, int len, String human) {
        offset = bytes.underlyingOffset(offset);

        boolean rawBytes = getRawBytes();

        String hex = rawBytes ? hexDump(offset, len) : "";
        print(twoColumns(hex, human));
        readBytes += len;
    }

    /** {@inheritDoc} */
    @Override
    public void startParsingMember(ByteArray bytes, int offset, String name,
                                   String descriptor) {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    public void endParsingMember(ByteArray bytes, int offset, String name,
                                 String descriptor, Member member) {
        // This space intentionally left blank.
    }

    /**
     * Gets the current number of read bytes.
     *
     * @return {@code >= 0;} the dump cursor
     */
    protected final int getReadBytes() {
        return readBytes;
    }

    /**
     * Gets the array of {@code byte}s to process.
     *
     * @return {@code non-null;} the bytes
     */
    protected final byte[] getBytes() {
        return bytes;
    }

    /**
     * Gets the filesystem/jar path of the file being dumped.
     *
     * @return {@code non-null;} the path
     */
    protected final String getFilePath() {
        return filePath;
    }

    /**
     * Gets whether to be strict about parsing.
     *
     * @return whether to be strict about parsing
     */
    protected final boolean getStrictParse() {
        return strictParse;
    }

    /**
     * Prints the given string to this instance's output stream.
     *
     * @param s {@code null-ok;} string to print
     */
    protected final void print(String s) {
        out.print(s);
    }

    /**
     * Prints the given string to this instance's output stream, followed
     * by a newline.
     *
     * @param s {@code null-ok;} string to print
     */
    protected final void println(String s) {
        out.println(s);
    }

    /**
     * Gets whether this dump is to include raw bytes.
     *
     * @return the raw bytes flag
     */
    protected final boolean getRawBytes() {
        return rawBytes;
    }

    /**
     * Gets the width of the first column of output. This is {@code 0}
     * unless raw bytes are being included in the output.
     *
     * @return {@code >= 0;} the width of the first column
     */
    protected final int getWidth1() {
        if (rawBytes) {
            return 5 + (hexCols * 2) + (hexCols / 2);
        }

        return 0;
    }

    /**
     * Gets the width of the second column of output.
     *
     * @return {@code >= 0;} the width of the second column
     */
    protected final int getWidth2() {
        int w1 = rawBytes ? (getWidth1() + 1) : 0;
        return width - w1 - (indent * 2);
    }

    /**
     * Constructs a hex data dump of the given portion of {@link #bytes}.
     *
     * @param offset offset to start dumping at
     * @param len length to dump
     * @return {@code non-null;} the dump
     */
    protected final String hexDump(int offset, int len) {
        return Hex.dump(bytes, offset, len, offset, hexCols, 4);
    }

    /**
     * Combines a pair of strings as two columns, or if this is one-column
     * output, format the otherwise-second column.
     *
     * @param s1 {@code non-null;} the first column's string
     * @param s2 {@code non-null;} the second column's string
     * @return {@code non-null;} the combined output
     */
    protected final String twoColumns(String s1, String s2) {
        int w1 = getWidth1();
        int w2 = getWidth2();

        try {
            if (w1 == 0) {
                int len2 = s2.length();
                StringWriter sw = new StringWriter(len2 * 2);
                IndentingWriter iw = new IndentingWriter(sw, w2, separator);

                iw.write(s2);
                if ((len2 == 0) || (s2.charAt(len2 - 1) != '\n')) {
                    iw.write('\n');
                }
                iw.flush();

                return sw.toString();
            } else {
                return TwoColumnOutput.toString(s1, w1, separator, s2, w2);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

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

package external.com.android.dx.util;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Writer that wraps another writer and passes width-limited and
 * optionally-prefixed output to its subordinate. When lines are
 * wrapped they are automatically indented based on the start of the
 * line.
 */
public final class IndentingWriter extends FilterWriter {
    /** {@code null-ok;} optional prefix for every line */
    private final String prefix;

    /** {@code > 0;} the maximum output width */
    private final int width;

    /** {@code > 0;} the maximum indent */
    private final int maxIndent;

    /** {@code >= 0;} current output column (zero-based) */
    private int column;

    /** whether indent spaces are currently being collected */
    private boolean collectingIndent;

    /** {@code >= 0;} current indent amount */
    private int indent;

    /**
     * Constructs an instance.
     *
     * @param out {@code non-null;} writer to send final output to
     * @param width {@code >= 0;} the maximum output width (not including
     * {@code prefix}), or {@code 0} for no maximum
     * @param prefix {@code non-null;} the prefix for each line
     */
    public IndentingWriter(Writer out, int width, String prefix) {
        super(out);

        if (out == null) {
            throw new NullPointerException("out == null");
        }

        if (width < 0) {
            throw new IllegalArgumentException("width < 0");
        }

        if (prefix == null) {
            throw new NullPointerException("prefix == null");
        }

        this.width = (width != 0) ? width : Integer.MAX_VALUE;
        this.maxIndent = width >> 1;
        this.prefix = (prefix.length() == 0) ? null : prefix;

        bol();
    }

    /**
     * Constructs a no-prefix instance.
     *
     * @param out {@code non-null;} writer to send final output to
     * @param width {@code >= 0;} the maximum output width (not including
     * {@code prefix}), or {@code 0} for no maximum
     */
    public IndentingWriter(Writer out, int width) {
        this(out, width, "");
    }

    /** {@inheritDoc} */
    @Override
    public void write(int c) throws IOException {
        synchronized (lock) {
            if (collectingIndent) {
                if (c == ' ') {
                    indent++;
                    if (indent >= maxIndent) {
                        indent = maxIndent;
                        collectingIndent = false;
                    }
                } else {
                    collectingIndent = false;
                }
            }

            if ((column == width) && (c != '\n')) {
                out.write('\n');
                column = 0;
                /*
                 * Note: No else, so this should fall through to the next
                 * if statement.
                 */
            }

            if (column == 0) {
                if (prefix != null) {
                    out.write(prefix);
                }

                if (!collectingIndent) {
                    for (int i = 0; i < indent; i++) {
                        out.write(' ');
                    }
                    column = indent;
                }
            }

            out.write(c);

            if (c == '\n') {
                bol();
            } else {
                column++;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            while (len > 0) {
                write(cbuf[off]);
                off++;
                len--;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            while (len > 0) {
                write(str.charAt(off));
                off++;
                len--;
            }
        }
    }

    /**
     * Indicates that output is at the beginning of a line.
     */
    private void bol() {
        column = 0;
        collectingIndent = (maxIndent != 0);
        indent = 0;
    }
}

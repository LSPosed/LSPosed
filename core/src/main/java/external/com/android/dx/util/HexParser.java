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

/**
 * Utilities for parsing hexadecimal text.
 */
public final class HexParser {
    /**
     * This class is uninstantiable.
     */
    private HexParser() {
        // This space intentionally left blank.
    }

    /**
     * Parses the given text as hex, returning a {@code byte[]}
     * corresponding to the text. The format is simple: Each line may
     * start with a hex offset followed by a colon (which is verified
     * and presumably used just as a comment), and then consists of
     * hex digits freely interspersed with whitespace. If a pound sign
     * is encountered, it and the rest of the line are ignored as a
     * comment. If a double quote is encountered, then the ASCII value
     * of the subsequent characters is used, until the next double
     * quote. Quoted strings may not span multiple lines.
     *
     * @param src {@code non-null;} the source string
     * @return {@code non-null;} the parsed form
     */
    public static byte[] parse(String src) {
        int len = src.length();
        byte[] result = new byte[len / 2];
        int at = 0;
        int outAt = 0;

        while (at < len) {
            int nlAt = src.indexOf('\n', at);
            if (nlAt < 0) {
                nlAt = len;
            }
            int poundAt = src.indexOf('#', at);

            String line;
            if ((poundAt >= 0) && (poundAt < nlAt)) {
                line = src.substring(at, poundAt);
            } else {
                line = src.substring(at, nlAt);
            }
            at = nlAt + 1;

            int colonAt = line.indexOf(':');

            atCheck:
            if (colonAt != -1) {
                int quoteAt = line.indexOf('\"');
                if ((quoteAt != -1) && (quoteAt < colonAt)) {
                    break atCheck;
                }

                String atStr = line.substring(0, colonAt).trim();
                line = line.substring(colonAt + 1);
                int alleged = Integer.parseInt(atStr, 16);
                if (alleged != outAt) {
                    throw new RuntimeException("bogus offset marker: " +
                                               atStr);
                }
            }

            int lineLen = line.length();
            int value = -1;
            boolean quoteMode = false;

            for (int i = 0; i < lineLen; i++) {
                char c = line.charAt(i);

                if (quoteMode) {
                    if (c == '\"') {
                        quoteMode = false;
                    } else {
                        result[outAt] = (byte) c;
                        outAt++;
                    }
                    continue;
                }

                if (c <= ' ') {
                    continue;
                }
                if (c == '\"') {
                    if (value != -1) {
                        throw new RuntimeException("spare digit around " +
                                                   "offset " + Hex.u4(outAt));
                    }
                    quoteMode = true;
                    continue;
                }

                int digVal = Character.digit(c, 16);
                if (digVal == -1) {
                    throw new RuntimeException("bogus digit character: \"" +
                                               c + "\"");
                }
                if (value == -1) {
                    value = digVal;
                } else {
                    result[outAt] = (byte) ((value << 4) | digVal);
                    outAt++;
                    value = -1;
                }
            }

            if (value != -1) {
                throw new RuntimeException("spare digit around offset " +
                                           Hex.u4(outAt));
            }

            if (quoteMode) {
                throw new RuntimeException("unterminated quote around " +
                                           "offset " + Hex.u4(outAt));
            }
        }

        if (outAt < result.length) {
            byte[] newr = new byte[outAt];
            System.arraycopy(result, 0, newr, 0, outAt);
            result = newr;
        }

        return result;
    }
}

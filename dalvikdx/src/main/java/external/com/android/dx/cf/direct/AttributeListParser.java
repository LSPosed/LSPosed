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

package external.com.android.dx.cf.direct;

import external.com.android.dx.cf.iface.Attribute;
import external.com.android.dx.cf.iface.ParseException;
import external.com.android.dx.cf.iface.ParseObserver;
import external.com.android.dx.cf.iface.StdAttributeList;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;

/**
 * Parser for lists of attributes.
 */
final /*package*/ class AttributeListParser {
    /** {@code non-null;} the class file to parse from */
    private final DirectClassFile cf;

    /** attribute parsing context */
    private final int context;

    /** offset in the byte array of the classfile to the start of the list */
    private final int offset;

    /** {@code non-null;} attribute factory to use */
    private final AttributeFactory attributeFactory;

    /** {@code non-null;} list of parsed attributes */
    private final StdAttributeList list;

    /** {@code >= -1;} the end offset of this list in the byte array of the
     * classfile, or {@code -1} if not yet parsed */
    private int endOffset;

    /** {@code null-ok;} parse observer, if any */
    private ParseObserver observer;

    /**
     * Constructs an instance.
     *
     * @param cf {@code non-null;} class file to parse from
     * @param context attribute parsing context (see {@link AttributeFactory})
     * @param offset offset in {@code bytes} to the start of the list
     * @param attributeFactory {@code non-null;} attribute factory to use
     */
    public AttributeListParser(DirectClassFile cf, int context, int offset,
                               AttributeFactory attributeFactory) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }

        if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        }

        int size = cf.getBytes().getUnsignedShort(offset);

        this.cf = cf;
        this.context = context;
        this.offset = offset;
        this.attributeFactory = attributeFactory;
        this.list = new StdAttributeList(size);
        this.endOffset = -1;
    }

    /**
     * Sets the parse observer for this instance.
     *
     * @param observer {@code null-ok;} the observer
     */
    public void setObserver(ParseObserver observer) {
        this.observer = observer;
    }

    /**
     * Gets the end offset of this constant pool in the {@code byte[]}
     * which it came from.
     *
     * @return {@code >= 0;} the end offset
     */
    public int getEndOffset() {
        parseIfNecessary();
        return endOffset;
    }

    /**
     * Gets the parsed list.
     *
     * @return {@code non-null;} the list
     */
    public StdAttributeList getList() {
        parseIfNecessary();
        return list;
    }

    /**
     * Runs {@link #parse} if it has not yet been run successfully.
     */
    private void parseIfNecessary() {
        if (endOffset < 0) {
            parse();
        }
    }

    /**
     * Does the actual parsing.
     */
    private void parse() {
        int sz = list.size();
        int at = offset + 2; // Skip the count.

        ByteArray bytes = cf.getBytes();

        if (observer != null) {
            observer.parsed(bytes, offset, 2,
                            "attributes_count: " + Hex.u2(sz));
        }

        for (int i = 0; i < sz; i++) {
            try {
                if (observer != null) {
                    observer.parsed(bytes, at, 0,
                                    "\nattributes[" + i + "]:\n");
                    observer.changeIndent(1);
                }

                Attribute attrib =
                    attributeFactory.parse(cf, context, at, observer);

                at += attrib.byteLength();
                list.set(i, attrib);

                if (observer != null) {
                    observer.changeIndent(-1);
                    observer.parsed(bytes, at, 0,
                                    "end attributes[" + i + "]\n");
                }
            } catch (ParseException ex) {
                ex.addContext("...while parsing attributes[" + i + "]");
                throw ex;
            } catch (RuntimeException ex) {
                ParseException pe = new ParseException(ex);
                pe.addContext("...while parsing attributes[" + i + "]");
                throw pe;
            }
        }

        endOffset = at;
    }
}

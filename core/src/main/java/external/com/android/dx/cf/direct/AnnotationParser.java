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

import external.com.android.dx.cf.iface.ParseException;
import external.com.android.dx.cf.iface.ParseObserver;
import external.com.android.dx.rop.annotation.Annotation;
import external.com.android.dx.rop.annotation.AnnotationVisibility;
import external.com.android.dx.rop.annotation.Annotations;
import external.com.android.dx.rop.annotation.AnnotationsList;
import external.com.android.dx.rop.annotation.NameValuePair;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.ConstantPool;
import external.com.android.dx.rop.cst.CstAnnotation;
import external.com.android.dx.rop.cst.CstArray;
import external.com.android.dx.rop.cst.CstBoolean;
import external.com.android.dx.rop.cst.CstByte;
import external.com.android.dx.rop.cst.CstChar;
import external.com.android.dx.rop.cst.CstDouble;
import external.com.android.dx.rop.cst.CstEnumRef;
import external.com.android.dx.rop.cst.CstFloat;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.cst.CstLong;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstShort;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;
import java.io.IOException;

/**
 * Parser for annotations.
 */
public final class AnnotationParser {
    /** {@code non-null;} class file being parsed */
    private final DirectClassFile cf;

    /** {@code non-null;} constant pool to use */
    private final ConstantPool pool;

    /** {@code non-null;} bytes of the attribute data */
    private final ByteArray bytes;

    /** {@code null-ok;} parse observer, if any */
    private final ParseObserver observer;

    /** {@code non-null;} input stream to parse from */
    private final ByteArray.MyDataInputStream input;

    /**
     * {@code non-null;} cursor for use when informing the observer of what
     * was parsed
     */
    private int parseCursor;

    /**
     * Constructs an instance.
     *
     * @param cf {@code non-null;} class file to parse from
     * @param offset {@code >= 0;} offset into the class file data to parse at
     * @param length {@code >= 0;} number of bytes left in the attribute data
     * @param observer {@code null-ok;} parse observer to notify, if any
     */
    public AnnotationParser(DirectClassFile cf, int offset, int length,
            ParseObserver observer) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }

        this.cf = cf;
        this.pool = cf.getConstantPool();
        this.observer = observer;
        this.bytes = cf.getBytes().slice(offset, offset + length);
        this.input = bytes.makeDataInputStream();
        this.parseCursor = 0;
    }

    /**
     * Parses an annotation value ({@code element_value}) attribute.
     *
     * @return {@code non-null;} the parsed constant value
     */
    public Constant parseValueAttribute() {
        Constant result;

        try {
            result = parseValue();

            if (input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        } catch (IOException ex) {
            // ByteArray.MyDataInputStream should never throw.
            throw new RuntimeException("shouldn't happen", ex);
        }

        return result;
    }

    /**
     * Parses a parameter annotation attribute.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotations
     * @return {@code non-null;} the parsed list of lists of annotations
     */
    public AnnotationsList parseParameterAttribute(
            AnnotationVisibility visibility) {
        AnnotationsList result;

        try {
            result = parseAnnotationsList(visibility);

            if (input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        } catch (IOException ex) {
            // ByteArray.MyDataInputStream should never throw.
            throw new RuntimeException("shouldn't happen", ex);
        }

        return result;
    }

    /**
     * Parses an annotation attribute, per se.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotations
     * @return {@code non-null;} the list of annotations read from the attribute
     * data
     */
    public Annotations parseAnnotationAttribute(
            AnnotationVisibility visibility) {
        Annotations result;

        try {
            result = parseAnnotations(visibility);

            if (input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        } catch (IOException ex) {
            // ByteArray.MyDataInputStream should never throw.
            throw new RuntimeException("shouldn't happen", ex);
        }

        return result;
    }

    /**
     * Parses a list of annotation lists.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotations
     * @return {@code non-null;} the list of annotation lists read from the attribute
     * data
     */
    private AnnotationsList parseAnnotationsList(
            AnnotationVisibility visibility) throws IOException {
        int count = input.readUnsignedByte();

        if (observer != null) {
            parsed(1, "num_parameters: " + Hex.u1(count));
        }

        AnnotationsList outerList = new AnnotationsList(count);

        for (int i = 0; i < count; i++) {
            if (observer != null) {
                parsed(0, "parameter_annotations[" + i + "]:");
                changeIndent(1);
            }

            Annotations annotations = parseAnnotations(visibility);
            outerList.set(i, annotations);

            if (observer != null) {
                observer.changeIndent(-1);
            }
        }

        outerList.setImmutable();
        return outerList;
    }

    /**
     * Parses an annotation list.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotations
     * @return {@code non-null;} the list of annotations read from the attribute
     * data
     */
    private Annotations parseAnnotations(AnnotationVisibility visibility)
            throws IOException {
        int count = input.readUnsignedShort();

        if (observer != null) {
            parsed(2, "num_annotations: " + Hex.u2(count));
        }

        Annotations annotations = new Annotations();

        for (int i = 0; i < count; i++) {
            if (observer != null) {
                parsed(0, "annotations[" + i + "]:");
                changeIndent(1);
            }

            Annotation annotation = parseAnnotation(visibility);
            annotations.add(annotation);

            if (observer != null) {
                observer.changeIndent(-1);
            }
        }

        annotations.setImmutable();
        return annotations;
    }

    /**
     * Parses a single annotation.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotation
     * @return {@code non-null;} the parsed annotation
     */
    private Annotation parseAnnotation(AnnotationVisibility visibility)
            throws IOException {
        requireLength(4);

        int typeIndex = input.readUnsignedShort();
        int numElements = input.readUnsignedShort();
        CstString typeString = (CstString) pool.get(typeIndex);
        CstType type = new CstType(Type.intern(typeString.getString()));

        if (observer != null) {
            parsed(2, "type: " + type.toHuman());
            parsed(2, "num_elements: " + numElements);
        }

        Annotation annotation = new Annotation(type, visibility);

        for (int i = 0; i < numElements; i++) {
            if (observer != null) {
                parsed(0, "elements[" + i + "]:");
                changeIndent(1);
            }

            NameValuePair element = parseElement();
            annotation.add(element);

            if (observer != null) {
                changeIndent(-1);
            }
        }

        annotation.setImmutable();
        return annotation;
    }

    /**
     * Parses a {@link NameValuePair}.
     *
     * @return {@code non-null;} the parsed element
     */
    private NameValuePair parseElement() throws IOException {
        requireLength(5);

        int elementNameIndex = input.readUnsignedShort();
        CstString elementName = (CstString) pool.get(elementNameIndex);

        if (observer != null) {
            parsed(2, "element_name: " + elementName.toHuman());
            parsed(0, "value: ");
            changeIndent(1);
        }

        Constant value = parseValue();

        if (observer != null) {
            changeIndent(-1);
        }

        return new NameValuePair(elementName, value);
    }

    /**
     * Parses an annotation value.
     *
     * @return {@code non-null;} the parsed value
     */
    private Constant parseValue() throws IOException {
        int tag = input.readUnsignedByte();

        if (observer != null) {
            CstString humanTag = new CstString(Character.toString((char) tag));
            parsed(1, "tag: " + humanTag.toQuoted());
        }

        switch (tag) {
            case 'B': {
                CstInteger value = (CstInteger) parseConstant();
                return CstByte.make(value.getValue());
            }
            case 'C': {
                CstInteger value = (CstInteger) parseConstant();
                int intValue = value.getValue();
                return CstChar.make(value.getValue());
            }
            case 'D': {
                CstDouble value = (CstDouble) parseConstant();
                return value;
            }
            case 'F': {
                CstFloat value = (CstFloat) parseConstant();
                return value;
            }
            case 'I': {
                CstInteger value = (CstInteger) parseConstant();
                return value;
            }
            case 'J': {
                CstLong value = (CstLong) parseConstant();
                return value;
            }
            case 'S': {
                CstInteger value = (CstInteger) parseConstant();
                return CstShort.make(value.getValue());
            }
            case 'Z': {
                CstInteger value = (CstInteger) parseConstant();
                return CstBoolean.make(value.getValue());
            }
            case 'c': {
                int classInfoIndex = input.readUnsignedShort();
                CstString value = (CstString) pool.get(classInfoIndex);
                Type type = Type.internReturnType(value.getString());

                if (observer != null) {
                    parsed(2, "class_info: " + type.toHuman());
                }

                return new CstType(type);
            }
            case 's': {
                return parseConstant();
            }
            case 'e': {
                requireLength(4);

                int typeNameIndex = input.readUnsignedShort();
                int constNameIndex = input.readUnsignedShort();
                CstString typeName = (CstString) pool.get(typeNameIndex);
                CstString constName = (CstString) pool.get(constNameIndex);

                if (observer != null) {
                    parsed(2, "type_name: " + typeName.toHuman());
                    parsed(2, "const_name: " + constName.toHuman());
                }

                return new CstEnumRef(new CstNat(constName, typeName));
            }
            case '@': {
                Annotation annotation =
                    parseAnnotation(AnnotationVisibility.EMBEDDED);
                return new CstAnnotation(annotation);
            }
            case '[': {
                requireLength(2);

                int numValues = input.readUnsignedShort();
                CstArray.List list = new CstArray.List(numValues);

                if (observer != null) {
                    parsed(2, "num_values: " + numValues);
                    changeIndent(1);
                }

                for (int i = 0; i < numValues; i++) {
                    if (observer != null) {
                        changeIndent(-1);
                        parsed(0, "element_value[" + i + "]:");
                        changeIndent(1);
                    }
                    list.set(i, parseValue());
                }

                if (observer != null) {
                    changeIndent(-1);
                }

                list.setImmutable();
                return new CstArray(list);
            }
            default: {
                throw new ParseException("unknown annotation tag: " +
                        Hex.u1(tag));
            }
        }
    }

    /**
     * Helper for {@link #parseValue}, which parses a constant reference
     * and returns the referred-to constant value.
     *
     * @return {@code non-null;} the parsed value
     */
    private Constant parseConstant() throws IOException {
        int constValueIndex = input.readUnsignedShort();
        Constant value = (Constant) pool.get(constValueIndex);

        if (observer != null) {
            String human = (value instanceof CstString)
                ? ((CstString) value).toQuoted()
                : value.toHuman();
            parsed(2, "constant_value: " + human);
        }

        return value;
    }

    /**
     * Helper which will throw an exception if the given number of bytes
     * is not available to be read.
     *
     * @param requiredLength the number of required bytes
     */
    private void requireLength(int requiredLength) throws IOException {
        if (input.available() < requiredLength) {
            throw new ParseException("truncated annotation attribute");
        }
    }

    /**
     * Helper which indicates that some bytes were just parsed. This should
     * only be used (for efficiency sake) if the parse is known to be
     * observed.
     *
     * @param length {@code >= 0;} number of bytes parsed
     * @param message {@code non-null;} associated message
     */
    private void parsed(int length, String message) {
        observer.parsed(bytes, parseCursor, length, message);
        parseCursor += length;
    }

    /**
     * Convenience wrapper that simply calls through to
     * {@code observer.changeIndent()}.
     *
     * @param indent the amount to change the indent by
     */
    private void changeIndent(int indent) {
        observer.changeIndent(indent);
    }
}

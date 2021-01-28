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

package external.com.android.dx.merge;

import external.com.android.dex.Annotation;
import external.com.android.dex.CallSiteId;
import external.com.android.dex.ClassDef;
import external.com.android.dex.Dex;
import external.com.android.dex.DexException;
import external.com.android.dex.EncodedValue;
import external.com.android.dex.EncodedValueCodec;
import external.com.android.dex.EncodedValueReader;
import static external.com.android.dex.EncodedValueReader.ENCODED_ANNOTATION;
import static external.com.android.dex.EncodedValueReader.ENCODED_ARRAY;
import static external.com.android.dex.EncodedValueReader.ENCODED_BOOLEAN;
import static external.com.android.dex.EncodedValueReader.ENCODED_BYTE;
import static external.com.android.dex.EncodedValueReader.ENCODED_CHAR;
import static external.com.android.dex.EncodedValueReader.ENCODED_DOUBLE;
import static external.com.android.dex.EncodedValueReader.ENCODED_ENUM;
import static external.com.android.dex.EncodedValueReader.ENCODED_FIELD;
import static external.com.android.dex.EncodedValueReader.ENCODED_FLOAT;
import static external.com.android.dex.EncodedValueReader.ENCODED_INT;
import static external.com.android.dex.EncodedValueReader.ENCODED_LONG;
import static external.com.android.dex.EncodedValueReader.ENCODED_METHOD;
import static external.com.android.dex.EncodedValueReader.ENCODED_METHOD_HANDLE;
import static external.com.android.dex.EncodedValueReader.ENCODED_METHOD_TYPE;
import static external.com.android.dex.EncodedValueReader.ENCODED_NULL;
import static external.com.android.dex.EncodedValueReader.ENCODED_SHORT;
import static external.com.android.dex.EncodedValueReader.ENCODED_STRING;
import static external.com.android.dex.EncodedValueReader.ENCODED_TYPE;
import external.com.android.dex.FieldId;
import external.com.android.dex.Leb128;
import external.com.android.dex.MethodHandle;
import external.com.android.dex.MethodId;
import external.com.android.dex.ProtoId;
import external.com.android.dex.TableOfContents;
import external.com.android.dex.TypeList;
import external.com.android.dex.util.ByteOutput;
import external.com.android.dx.util.ByteArrayAnnotatedOutput;
import java.util.HashMap;

/**
 * Maps the index offsets from one dex file to those in another. For example, if
 * you have string #5 in the old dex file, its position in the new dex file is
 * {@code strings[5]}.
 */
public final class IndexMap {
    private final Dex target;
    public final int[] stringIds;
    public final short[] typeIds;
    public final short[] protoIds;
    public final short[] fieldIds;
    public final short[] methodIds;
    public final int[] callSiteIds;
    public final HashMap<Integer, Integer> methodHandleIds;
    private final HashMap<Integer, Integer> typeListOffsets;
    private final HashMap<Integer, Integer> annotationOffsets;
    private final HashMap<Integer, Integer> annotationSetOffsets;
    private final HashMap<Integer, Integer> annotationSetRefListOffsets;
    private final HashMap<Integer, Integer> annotationDirectoryOffsets;
    private final HashMap<Integer, Integer> encodedArrayValueOffset;

    public IndexMap(Dex target, TableOfContents tableOfContents) {
        this.target = target;
        this.stringIds = new int[tableOfContents.stringIds.size];
        this.typeIds = new short[tableOfContents.typeIds.size];
        this.protoIds = new short[tableOfContents.protoIds.size];
        this.fieldIds = new short[tableOfContents.fieldIds.size];
        this.methodIds = new short[tableOfContents.methodIds.size];
        this.callSiteIds = new int[tableOfContents.callSiteIds.size];
        this.methodHandleIds = new HashMap<Integer, Integer>();
        this.typeListOffsets = new HashMap<Integer, Integer>();
        this.annotationOffsets = new HashMap<Integer, Integer>();
        this.annotationSetOffsets = new HashMap<Integer, Integer>();
        this.annotationSetRefListOffsets = new HashMap<Integer, Integer>();
        this.annotationDirectoryOffsets = new HashMap<Integer, Integer>();
        this.encodedArrayValueOffset = new HashMap<Integer, Integer>();

        /*
         * A type list, annotation set, annotation directory, or static value at
         * offset 0 is always empty. Always map offset 0 to 0.
         */
        this.typeListOffsets.put(0, 0);
        this.annotationSetOffsets.put(0, 0);
        this.annotationDirectoryOffsets.put(0, 0);
        this.encodedArrayValueOffset.put(0, 0);
    }

    public void putTypeListOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        typeListOffsets.put(oldOffset, newOffset);
    }

    public void putAnnotationOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        annotationOffsets.put(oldOffset, newOffset);
    }

    public void putAnnotationSetOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        annotationSetOffsets.put(oldOffset, newOffset);
    }

    public void putAnnotationSetRefListOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        annotationSetRefListOffsets.put(oldOffset, newOffset);
    }

    public void putAnnotationDirectoryOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        annotationDirectoryOffsets.put(oldOffset, newOffset);
    }

    public void putEncodedArrayValueOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        encodedArrayValueOffset.put(oldOffset, newOffset);
    }

    public int adjustString(int stringIndex) {
        return stringIndex == ClassDef.NO_INDEX ? ClassDef.NO_INDEX : stringIds[stringIndex];
    }

    public int adjustType(int typeIndex) {
        return (typeIndex == ClassDef.NO_INDEX) ? ClassDef.NO_INDEX : (typeIds[typeIndex] & 0xffff);
    }

    public TypeList adjustTypeList(TypeList typeList) {
        if (typeList == TypeList.EMPTY) {
            return typeList;
        }
        short[] types = typeList.getTypes().clone();
        for (int i = 0; i < types.length; i++) {
            types[i] = (short) adjustType(types[i]);
        }
        return new TypeList(target, types);
    }

    public int adjustProto(int protoIndex) {
        return protoIds[protoIndex] & 0xffff;
    }

    public int adjustField(int fieldIndex) {
        return fieldIds[fieldIndex] & 0xffff;
    }

    public int adjustMethod(int methodIndex) {
        return methodIds[methodIndex] & 0xffff;
    }

    public int adjustTypeListOffset(int typeListOffset) {
        return typeListOffsets.get(typeListOffset);
    }

    public int adjustAnnotation(int annotationOffset) {
        return annotationOffsets.get(annotationOffset);
    }

    public int adjustAnnotationSet(int annotationSetOffset) {
        return annotationSetOffsets.get(annotationSetOffset);
    }

    public int adjustAnnotationSetRefList(int annotationSetRefListOffset) {
        return annotationSetRefListOffsets.get(annotationSetRefListOffset);
    }

    public int adjustAnnotationDirectory(int annotationDirectoryOffset) {
        return annotationDirectoryOffsets.get(annotationDirectoryOffset);
    }

    public int adjustEncodedArray(int encodedArrayAttribute) {
        return encodedArrayValueOffset.get(encodedArrayAttribute);
    }

    public int adjustCallSite(int callSiteIndex) {
        return callSiteIds[callSiteIndex];
    }

    public int adjustMethodHandle(int methodHandleIndex) {
        return methodHandleIds.get(methodHandleIndex);
    }

    public MethodId adjust(MethodId methodId) {
        return new MethodId(target,
                adjustType(methodId.getDeclaringClassIndex()),
                adjustProto(methodId.getProtoIndex()),
                adjustString(methodId.getNameIndex()));
    }

    public CallSiteId adjust(CallSiteId callSiteId) {
        return new CallSiteId(target, adjustEncodedArray(callSiteId.getCallSiteOffset()));
    }

    public MethodHandle adjust(MethodHandle methodHandle) {
        return new MethodHandle(
                target,
                methodHandle.getMethodHandleType(),
                methodHandle.getUnused1(),
                methodHandle.getMethodHandleType().isField()
                        ? adjustField(methodHandle.getFieldOrMethodId())
                        : adjustMethod(methodHandle.getFieldOrMethodId()),
                methodHandle.getUnused2());
    }

    public FieldId adjust(FieldId fieldId) {
        return new FieldId(target,
                adjustType(fieldId.getDeclaringClassIndex()),
                adjustType(fieldId.getTypeIndex()),
                adjustString(fieldId.getNameIndex()));

    }

    public ProtoId adjust(ProtoId protoId) {
        return new ProtoId(target,
                adjustString(protoId.getShortyIndex()),
                adjustType(protoId.getReturnTypeIndex()),
                adjustTypeListOffset(protoId.getParametersOffset()));
    }

    public ClassDef adjust(ClassDef classDef) {
        return new ClassDef(target, classDef.getOffset(), adjustType(classDef.getTypeIndex()),
                classDef.getAccessFlags(), adjustType(classDef.getSupertypeIndex()),
                adjustTypeListOffset(classDef.getInterfacesOffset()), classDef.getSourceFileIndex(),
                classDef.getAnnotationsOffset(), classDef.getClassDataOffset(),
                classDef.getStaticValuesOffset());
    }

    public SortableType adjust(SortableType sortableType) {
        return new SortableType(sortableType.getDex(),
                sortableType.getIndexMap(), adjust(sortableType.getClassDef()));
    }

    public EncodedValue adjustEncodedValue(EncodedValue encodedValue) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transform(new EncodedValueReader(encodedValue));
        return new EncodedValue(out.toByteArray());
    }

    public EncodedValue adjustEncodedArray(EncodedValue encodedArray) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transformArray(
                new EncodedValueReader(encodedArray, ENCODED_ARRAY));
        return new EncodedValue(out.toByteArray());
    }

    public Annotation adjust(Annotation annotation) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transformAnnotation(
                annotation.getReader());
        return new Annotation(target, annotation.getVisibility(),
                new EncodedValue(out.toByteArray()));
    }

    /**
     * Adjust an encoded value or array.
     */
    private final class EncodedValueTransformer {
        private final ByteOutput out;

        public EncodedValueTransformer(ByteOutput out) {
            this.out = out;
        }

        public void transform(EncodedValueReader reader) {
            // TODO: extract this into a helper class, EncodedValueWriter
            switch (reader.peek()) {
            case ENCODED_BYTE:
                EncodedValueCodec.writeSignedIntegralValue(out, ENCODED_BYTE, reader.readByte());
                break;
            case ENCODED_SHORT:
                EncodedValueCodec.writeSignedIntegralValue(out, ENCODED_SHORT, reader.readShort());
                break;
            case ENCODED_INT:
                EncodedValueCodec.writeSignedIntegralValue(out, ENCODED_INT, reader.readInt());
                break;
            case ENCODED_LONG:
                EncodedValueCodec.writeSignedIntegralValue(out, ENCODED_LONG, reader.readLong());
                break;
            case ENCODED_CHAR:
                EncodedValueCodec.writeUnsignedIntegralValue(out, ENCODED_CHAR, reader.readChar());
                break;
            case ENCODED_FLOAT:
                // Shift value left 32 so that right-zero-extension works.
                long longBits = ((long) Float.floatToIntBits(reader.readFloat())) << 32;
                EncodedValueCodec.writeRightZeroExtendedValue(out, ENCODED_FLOAT, longBits);
                break;
            case ENCODED_DOUBLE:
                EncodedValueCodec.writeRightZeroExtendedValue(
                        out, ENCODED_DOUBLE, Double.doubleToLongBits(reader.readDouble()));
                break;
            case ENCODED_METHOD_TYPE:
                EncodedValueCodec.writeUnsignedIntegralValue(
                        out, ENCODED_METHOD_TYPE, adjustProto(reader.readMethodType()));
                break;
            case ENCODED_METHOD_HANDLE:
                EncodedValueCodec.writeUnsignedIntegralValue(
                        out,
                        ENCODED_METHOD_HANDLE,
                        adjustMethodHandle(reader.readMethodHandle()));
                break;
            case ENCODED_STRING:
                EncodedValueCodec.writeUnsignedIntegralValue(
                        out, ENCODED_STRING, adjustString(reader.readString()));
                break;
            case ENCODED_TYPE:
                EncodedValueCodec.writeUnsignedIntegralValue(
                        out, ENCODED_TYPE, adjustType(reader.readType()));
                break;
            case ENCODED_FIELD:
                EncodedValueCodec.writeUnsignedIntegralValue(
                        out, ENCODED_FIELD, adjustField(reader.readField()));
                break;
            case ENCODED_ENUM:
                EncodedValueCodec.writeUnsignedIntegralValue(
                        out, ENCODED_ENUM, adjustField(reader.readEnum()));
                break;
            case ENCODED_METHOD:
                EncodedValueCodec.writeUnsignedIntegralValue(
                        out, ENCODED_METHOD, adjustMethod(reader.readMethod()));
                break;
            case ENCODED_ARRAY:
                writeTypeAndArg(ENCODED_ARRAY, 0);
                transformArray(reader);
                break;
            case ENCODED_ANNOTATION:
                writeTypeAndArg(ENCODED_ANNOTATION, 0);
                transformAnnotation(reader);
                break;
            case ENCODED_NULL:
                reader.readNull();
                writeTypeAndArg(ENCODED_NULL, 0);
                break;
            case ENCODED_BOOLEAN:
                boolean value = reader.readBoolean();
                writeTypeAndArg(ENCODED_BOOLEAN, value ? 1 : 0);
                break;
            default:
                throw new DexException("Unexpected type: " + Integer.toHexString(reader.peek()));
            }
        }

        private void transformAnnotation(EncodedValueReader reader) {
            int fieldCount = reader.readAnnotation();
            Leb128.writeUnsignedLeb128(out, adjustType(reader.getAnnotationType()));
            Leb128.writeUnsignedLeb128(out, fieldCount);
            for (int i = 0; i < fieldCount; i++) {
                Leb128.writeUnsignedLeb128(out, adjustString(reader.readAnnotationName()));
                transform(reader);
            }
        }

        private void transformArray(EncodedValueReader reader) {
            int size = reader.readArray();
            Leb128.writeUnsignedLeb128(out, size);
            for (int i = 0; i < size; i++) {
                transform(reader);
            }
        }

        private void writeTypeAndArg(int type, int arg) {
            out.writeByte((arg << 5) | type);
        }
    }
}

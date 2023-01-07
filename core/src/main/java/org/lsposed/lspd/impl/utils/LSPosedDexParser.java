package org.lsposed.lspd.impl.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.nativebridge.DexParserBridge;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.github.libxposed.utils.DexParser;

public class LSPosedDexParser implements DexParser {
    long cookie;

    final ByteBuffer data;
    final StringId[] strings;
    final TypeId[] typeIds;
    final ProtoId[] protoIds;
    final FieldId[] fieldIds;
    final MethodId[] methodIds;
    final Annotation[] annotations;

    public LSPosedDexParser(ByteBuffer buffer) throws IOException {
        if (!buffer.isDirect() || !buffer.asReadOnlyBuffer().hasArray()) {
            data = ByteBuffer.allocateDirect(buffer.capacity());
            data.put(buffer);
        } else {
            data = buffer;
        }
        try {
            long[] args = new long[1];
            var out = (Object[]) DexParserBridge.openDex(buffer, args);
            cookie = args[0];
            // out[0]: String[]
            // out[1]: int[]
            // out[2]: int[][]
            // out[3]: int[]
            // out[4]: int[]
            // out[6]: Object[]
            var strings = (Object[]) out[0];
            this.strings = new StringId[strings.length];
            for (int i = 0; i < strings.length; ++i) {
                this.strings[i] = new LSPosedStringId(i, strings[i]);
            }

            var typeIds = (int[]) out[1];
            this.typeIds = new TypeId[typeIds.length];
            for (int i = 0; i < typeIds.length; ++i) {
                this.typeIds[i] = new LSPosedTypeId(i, typeIds[i]);
            }

            var protoIds = (int[][]) out[2];
            this.protoIds = new ProtoId[protoIds.length];
            for (int i = 0; i < protoIds.length; ++i) {
                this.protoIds[i] = new LSPosedProtoId(i, protoIds[i]);
            }

            var fieldIds = (int[]) out[3];
            this.fieldIds = new FieldId[fieldIds.length / 3];
            for (int i = 0; i < this.fieldIds.length; ++i) {
                this.fieldIds[i] = new LSPosedFieldId(i, fieldIds[3 * i], fieldIds[3 * i + 1], fieldIds[3 * i + 2]);
            }

            var methodIds = (int[]) out[4];
            this.methodIds = new MethodId[methodIds.length / 3];
            for (int i = 0; i < this.methodIds.length / 3; ++i) {
                this.methodIds[i] = new LSPosedMethodId(i, methodIds[3 * i], methodIds[3 * i + 1], methodIds[3 * i + 2]);
            }

            if (out[5] != null && out[6] != null) {
                var a = (int[]) out[5];
                var b = (Object[]) out[6];
                this.annotations = new Annotation[a.length / 2];
                for (int i = 0; i < this.annotations.length; ++i) {
                    this.annotations[i] = new LSPosedAnnotation(a[2 * i], a[2 * i + 1], (int[]) b[2 * i], (Object[]) b[2 * i + 1]);
                }
            } else {
                this.annotations = new Annotation[0];
            }
        } catch (Throwable e) {
            throw new IOException("Invalid dex file", e);
        }
    }

    @Override
    synchronized public void close() {
        if (cookie != 0) {
            DexParserBridge.closeDex(cookie);
            cookie = 0;
        }
    }

    static class LSPosedId implements Id {
        final int id;

        LSPosedId(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }
    }

    static class LSPosedStringId extends LSPosedId implements StringId {
        @NonNull
        final String string;

        LSPosedStringId(int id, @NonNull Object string) {
            super(id);
            this.string = (String) string;
        }

        @NonNull
        @Override
        public String getString() {
            return string;
        }
    }

    class LSPosedTypeId extends LSPosedId implements TypeId {
        @NonNull
        final StringId descriptor;

        LSPosedTypeId(int id, int descriptor) {
            super(id);
            this.descriptor = strings[descriptor];
        }

        @NonNull
        @Override
        public StringId getDescriptor() {
            return descriptor;
        }
    }

    class LSPosedProtoId extends LSPosedId implements ProtoId {
        @NonNull
        final StringId shorty;
        @NonNull
        final TypeId returnType;
        @Nullable
        final TypeId[] parameters;

        LSPosedProtoId(int id, @NonNull int[] protoId) {
            super(id);
            this.shorty = strings[protoId[0]];
            this.returnType = typeIds[protoId[1]];
            if (protoId.length > 2) {
                this.parameters = new TypeId[protoId.length - 2];
                for (int i = 2; i < parameters.length; ++i) {
                    this.parameters[i] = typeIds[protoId[i]];
                }
            } else {
                this.parameters = null;
            }
        }

        @NonNull
        @Override
        public StringId getShorty() {
            return shorty;
        }

        @NonNull
        @Override
        public TypeId getReturnType() {
            return returnType;
        }

        @Nullable
        @Override
        public TypeId[] getParameters() {
            return parameters;
        }
    }

    class LSPosedFieldId extends LSPosedId implements FieldId {
        @NonNull
        final TypeId type;
        @NonNull
        final TypeId declaringClass;
        @NonNull
        final StringId name;

        LSPosedFieldId(int id, int type, int declaringClass, int name) {
            super(id);
            this.type = typeIds[type];
            this.declaringClass = typeIds[declaringClass];
            this.name = strings[name];
        }

        @NonNull
        @Override
        public TypeId getType() {
            return type;
        }

        @NonNull
        @Override
        public TypeId getDeclaringClass() {
            return declaringClass;
        }

        @NonNull
        @Override
        public StringId getName() {
            return name;
        }
    }

    class LSPosedMethodId extends LSPosedId implements MethodId {
        @NonNull
        final TypeId declaringClass;
        @NonNull
        final ProtoId prototype;
        @NonNull
        final StringId name;

        LSPosedMethodId(int id, int declaringClass, int prototype, int name) {
            super(id);
            this.declaringClass = typeIds[declaringClass];
            this.prototype = protoIds[prototype];
            this.name = strings[name];
        }

        @NonNull
        @Override
        public TypeId getDeclaringClass() {
            return declaringClass;
        }

        @NonNull
        @Override
        public ProtoId getPrototype() {
            return prototype;
        }

        @NonNull
        @Override
        public StringId getName() {
            return name;
        }
    }

    class LSPosedAnnotation implements Annotation {
        int visibility;
        @NonNull
        final TypeId type;
        @Nullable
        final AnnotationElement[] elements;

        LSPosedAnnotation(int visibility, int type, @NonNull int[] elements, @NonNull Object[] elementValues) {
            this.visibility = visibility;
            this.type = typeIds[type];
            this.elements = new AnnotationElement[elementValues.length];
            for (int i = 0; i < elementValues.length; ++i) {
                this.elements[i] = new LSPosedAnnotationElement(elements[i * 2], elements[i * 2 + 1], (ByteBuffer) elementValues[i]);
            }
        }

        @Override
        public int getVisibility() {
            return visibility;
        }

        @NonNull
        @Override
        public TypeId getType() {
            return type;
        }

        @Nullable
        @Override
        public AnnotationElement[] getElements() {
            return elements;
        }
    }

    class LSPosedAnnotationElement implements AnnotationElement {
        @NonNull
        final StringId name;
        final int valueType;
        @Nullable
        final byte[] value;

        LSPosedAnnotationElement(int name, int valueType, @Nullable ByteBuffer value) {
            this.name = strings[name];
            this.valueType = valueType;
            if (value != null) {
                this.value = new byte[value.remaining()];
                value.get(this.value);
            } else {
                this.value = null;
            }
        }

        @NonNull
        @Override
        public StringId getName() {
            return name;
        }

        @Override
        public int getValueType() {
            return valueType;
        }

        @Nullable
        @Override
        public byte[] getValue() {
            return value;
        }
    }

    @NonNull
    @Override
    public StringId[] getStringId() {
        return strings;
    }

    @NonNull
    @Override
    public TypeId[] getTypeId() {
        return typeIds;
    }

    @NonNull
    @Override
    public FieldId[] getFieldId() {
        return fieldIds;
    }

    @NonNull
    @Override
    public MethodId[] getMethodId() {
        return methodIds;
    }

    @NonNull
    @Override
    public ProtoId[] getProtoId() {
        return protoIds;
    }

    @NonNull
    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    synchronized public void visitDefinedClasses(@NonNull ClassVisitor visitor) {
        if (cookie == 0) {
            throw new IllegalStateException("Closed");
        }
        var classVisitMethod = ClassVisitor.class.getDeclaredMethods()[0];
        var fieldVisitMethod = FieldVisitor.class.getDeclaredMethods()[0];
        var methodVisitMethod = MethodVisitor.class.getDeclaredMethods()[0];
        var methodBodyVisitMethod = MethodBodyVisitor.class.getDeclaredMethods()[0];
        var stopMethod = EarlyStopVisitor.class.getDeclaredMethods()[0];

        DexParserBridge.visitClass(cookie, visitor, FieldVisitor.class, MethodVisitor.class, classVisitMethod, fieldVisitMethod, methodVisitMethod, methodBodyVisitMethod, stopMethod);
    }
}

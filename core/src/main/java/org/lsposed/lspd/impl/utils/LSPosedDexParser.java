package org.lsposed.lspd.impl.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.nativebridge.DexParserBridge;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.github.libxposed.utils.DexParser;

public class LSPosedDexParser implements DexParser {
    ReadWriteLock lock = new ReentrantReadWriteLock();
    long cookie;
    StringId[] strings;
    TypeId[] typeIds;
    ProtoId[] protoIds;
    FieldId[] fieldIds;
    MethodId[] methodIds;
    ClassDef[] classDefs;

    public LSPosedDexParser(ByteBuffer buffer) throws IOException {
        lock.writeLock().lock();
        if (!buffer.isDirect() || !buffer.asReadOnlyBuffer().hasArray()) {
            var newBuffer = ByteBuffer.allocateDirect(buffer.capacity());
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
        try {
            var out = (Object[]) DexParserBridge.parseDex(buffer);
            // out[0]: Long
            // out[1]: String[]
            // out[2]: int[][]
            // out[3]: int[][3]
            // out[4]: int[][3]
            // out[5]: int[][3]
            // out[6]: int[][]
            // out[7]: Object[][][][]
            cookie = (Long) out[0];
            var strings = (Object[]) out[1];
            this.strings = new StringId[strings.length];
            for (int i = 0; i < strings.length; ++i) {
                this.strings[i] = new LSPosedStringId(i, strings[i]);
            }

            var typeIds = (int[]) out[2];
            this.typeIds = new TypeId[typeIds.length];
            for (int i = 0; i < typeIds.length; ++i) {
                this.typeIds[i] = new LSPosedTypeId(i, typeIds[i]);
            }

            var protoIds = (int[][]) out[3];
            this.protoIds = new ProtoId[protoIds.length];
            for (int i = 0; i < protoIds.length; ++i) {
                this.protoIds[i] = new LSPosedProtoId(i, protoIds[i]);
            }

            var fieldIds = (int[][]) out[4];
            this.fieldIds = new FieldId[fieldIds.length];
            for (int i = 0; i < fieldIds.length; ++i) {
                this.fieldIds[i] = new LSPosedFieldId(i, fieldIds[i]);
            }

            var methodIds = (int[][]) out[5];
            this.methodIds = new MethodId[methodIds.length];
            for (int i = 0; i < methodIds.length; ++i) {
                this.methodIds[i] = new LSPosedMethodId(i, methodIds[i]);
            }

            var classDefs = (int[][]) out[6];
            this.classDefs = new ClassDef[classDefs.length];
            var annotations = (Object[][][][]) out[7];
            for (int i = 0; i < classDefs.length; ++i) {
                this.classDefs[i] = new LSPosedClassDef(classDefs[i], annotations[i]);
            }
        } catch (IOException e) {
            close();
            throw e;
        } catch (Throwable e) {
            close();
            throw new IOException("Invalid dex file", e);
        } finally {
            lock.writeLock().unlock();
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

        LSPosedFieldId(int id, @NonNull int[] fieldId) {
            super(id);
            this.type = typeIds[fieldId[0]];
            this.declaringClass = typeIds[fieldId[1]];
            this.name = strings[fieldId[2]];
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

        LSPosedMethodId(int id, @NonNull int[] methodId) {
            super(id);
            declaringClass = typeIds[methodId[0]];
            prototype = protoIds[methodId[1]];
            name = strings[methodId[2]];
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

    class LSPosedEncodedField implements EncodedField {
        @NonNull
        final FieldId field;
        final int accessFlags;

        LSPosedEncodedField(int field, int accessFlags) {
            this.field = fieldIds[field];
            this.accessFlags = accessFlags;
        }

        @NonNull
        @Override
        public FieldId getField() {
            return field;
        }

        @Override
        public int getAccessFlags() {
            return accessFlags;
        }
    }

    class LSPosedEncodedMethod implements EncodedMethod {
        @NonNull
        final MethodId method;
        final int accessFlags;
        final int code;
        MethodId[] invokedMethods = null;
        FieldId[] accessedFields = null;
        FieldId[] assignedFields = null;
        StringId[] referredStrings = null;
        int[] opcodes = null;

        LSPosedEncodedMethod(int method, int accessFlags, int code) {
            this.method = methodIds[method];
            this.accessFlags = accessFlags;
            this.code = code;
        }

        synchronized void parseMethod() {
            if (invokedMethods != null) return;
            lock.readLock().lock();
            try {
                if (cookie == 0) throw new IllegalStateException("DexParser is closed");
                var out = (Object[]) DexParserBridge.parseMethod(cookie, code);
                var invokedMethods = (int[]) out[0];
                this.invokedMethods = new MethodId[invokedMethods.length];
                for (int i = 0; i < invokedMethods.length; ++i) {
                    this.invokedMethods[i] = methodIds[invokedMethods[i]];
                }
                var accessedFields = (int[]) out[1];
                this.accessedFields = new FieldId[accessedFields.length];
                for (int i = 0; i < accessedFields.length; ++i) {
                    this.accessedFields[i] = fieldIds[accessedFields[i]];
                }
                var assignedFields = (int[]) out[2];
                this.assignedFields = new FieldId[assignedFields.length];
                for (int i = 0; i < assignedFields.length; ++i) {
                    this.assignedFields[i] = fieldIds[assignedFields[i]];
                }
                var referredStrings = (int[]) out[3];
                this.referredStrings = new StringId[referredStrings.length];
                for (int i = 0; i < referredStrings.length; ++i) {
                    this.referredStrings[i] = strings[referredStrings[i]];
                }
                opcodes = (int[]) out[4];
            } finally {
                lock.readLock().unlock();
            }
        }

        @NonNull
        @Override
        public MethodId getMethod() {
            return method;
        }

        @Override
        public int getAccessFlags() {
            return accessFlags;
        }

        @NonNull
        @Override
        public MethodId[] getInvokedMethods() {
            if (invokedMethods == null) {
                parseMethod();
            }
            return invokedMethods;
        }

        @NonNull
        @Override
        public FieldId[] getAccessedFields() {
            if (accessedFields == null) {
                parseMethod();
            }
            return accessedFields;
        }

        @NonNull
        @Override
        public FieldId[] getAssignedFields() {
            if (assignedFields == null) {
                parseMethod();
            }
            return assignedFields;
        }

        @NonNull
        @Override
        public int[] getOpcodes() {
            if (opcodes == null) {
                parseMethod();
            }
            return opcodes;
        }

        @NonNull
        @Override
        public StringId[] getReferredString() {
            if (referredStrings == null) {
                parseMethod();
            }
            return referredStrings;
        }

    }

    class LSPosedAnnotation implements Annotation {
        int visibility;
        @NonNull
        final TypeId type;
        @Nullable
        final AnnotationElement[] elements;

        LSPosedAnnotation(int visibility, int type, @NonNull Object[] elements) {
            this.visibility = visibility;
            this.type = typeIds[type];
            var a = (int[]) elements[0];
            var b = (ByteBuffer[]) elements[1];
            this.elements = new AnnotationElement[b.length];
            for (int i = 0; i < b.length; ++i) {
                this.elements[i] = new LSPosedAnnotationElement(a[i * 2], a[i * 2 + 1], b[i]);
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
                this.value = value.array();
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

    class LSPosedFieldAnnotation implements FieldAnnotation {
        @NonNull
        FieldId field;
        @NonNull
        Annotation[] annotations;

        LSPosedFieldAnnotation(int field, @NonNull Object[] annotations) {
            this.field = fieldIds[field];
            var a = (int[]) annotations[0];
            var b = (Object[][]) annotations[1];
            this.annotations = new Annotation[b.length];
            for (int i = 0; i < b.length; ++i) {
                this.annotations[i] = new LSPosedAnnotation(a[2 * i], a[2 * i + 1], b[i]);
            }
        }

        @NonNull
        @Override
        public FieldId getField() {
            return field;
        }

        @NonNull
        @Override
        public Annotation[] getAnnotations() {
            return annotations;
        }
    }

    class LSPosedMethodAnnotation implements MethodAnnotation {
        @NonNull
        MethodId method;
        @NonNull
        Annotation[] annotations;

        LSPosedMethodAnnotation(int method, @NonNull Object[] annotations) {
            this.method = methodIds[method];
            var a = (int[]) annotations[0];
            var b = (Object[][]) annotations[1];
            this.annotations = new Annotation[b.length];
            for (int i = 0; i < b.length; ++i) {
                this.annotations[i] = new LSPosedAnnotation(a[2 * i], a[2 * i + 1], b[i]);
            }
        }

        @NonNull
        @Override
        public MethodId getMethod() {
            return method;
        }

        @NonNull
        @Override
        public Annotation[] getAnnotations() {
            return annotations;
        }
    }

    class LSPosedParameterAnnotation implements ParameterAnnotation {
        @NonNull
        MethodId method;
        @NonNull
        Annotation[][] annotations;

        LSPosedParameterAnnotation(int method, @NonNull Object[][] annotations) {
            this.method = methodIds[method];
            this.annotations = new Annotation[annotations.length][];
            for (int i = 0; i < annotations.length; ++i) {
                var a = (int[]) annotations[i][0];
                var b = (Object[][]) annotations[i][1];
                this.annotations[i] = new Annotation[b.length];
                for (int j = 0; j < b.length; ++j) {
                    this.annotations[i][j] = new LSPosedAnnotation(a[2 * j], a[2 * j + 1], b[j]);
                }
            }
        }

        @NonNull
        @Override
        public MethodId getMethod() {
            return method;
        }

        @NonNull
        @Override
        public Annotation[][] getAnnotations() {
            return annotations;
        }
    }

    class LSPosedClassDef implements ClassDef {
        @NonNull
        final TypeId type;
        final int accessFlags;
        @Nullable
        final TypeId superClass;
        @Nullable
        final TypeId[] interfaces;
        @Nullable
        final StringId sourceFile;
        @NonNull
        final EncodedField[] staticFields;
        @NonNull
        final EncodedField[] instanceFields;
        @NonNull
        final EncodedMethod[] directMethods;
        @NonNull
        final EncodedMethod[] virtualMethods;

        @NonNull
        final FieldAnnotation[] fieldAnnotations;
        @NonNull
        final MethodAnnotation[] methodAnnotations;
        @NonNull
        final ParameterAnnotation[] parameterAnnotations;

        LSPosedClassDef(int[] classDef, Object[][][] annotation) {
            // annotation[0]: int[]
            // annotation[1]: int[]
            var iter = 0;
            type = typeIds[classDef[iter++]];
            accessFlags = classDef[iter++];
            var superClass = classDef[iter++];
            this.superClass = superClass == NO_INDEX ? null : typeIds[superClass];
            var sourceFile = classDef[iter++];
            this.sourceFile = sourceFile == NO_INDEX ? null : strings[sourceFile];
            var num_interfaces = classDef[iter++];
            interfaces = new TypeId[num_interfaces];
            for (int i = 0; i < num_interfaces; ++i) {
                interfaces[i] = typeIds[classDef[iter++]];
            }
            var num_static_fields = classDef[iter++];
            staticFields = new EncodedField[num_static_fields];
            for (int i = 0; i < num_static_fields; ++i) {
                var field = classDef[iter++];
                var accessFlags = classDef[iter++];
                staticFields[i] = new LSPosedEncodedField(field, accessFlags);
            }
            var num_instance_fields = classDef[iter++];
            instanceFields = new EncodedField[num_instance_fields];
            for (int i = 0; i < num_instance_fields; ++i) {
                var field = classDef[iter++];
                var accessFlags = classDef[iter++];
                instanceFields[i] = new LSPosedEncodedField(field, accessFlags);
            }
            var num_direct_methods = classDef[iter++];
            directMethods = new EncodedMethod[num_direct_methods];
            for (int i = 0; i < num_direct_methods; ++i) {
                var method = classDef[iter++];
                var accessFlags = classDef[iter++];
                var code = classDef[iter++];
                directMethods[i] = new LSPosedEncodedMethod(method, accessFlags, code);
            }
            var num_virtual_methods = classDef[iter++];
            virtualMethods = new EncodedMethod[num_virtual_methods];
            for (int i = 0; i < num_virtual_methods; ++i) {
                var method = classDef[iter++];
                var accessFlags = classDef[iter++];
                var code = classDef[iter++];
                virtualMethods[i] = new LSPosedEncodedMethod(method, accessFlags, code);
            }

            var num_field_annotations = classDef[iter++];
            fieldAnnotations = new FieldAnnotation[num_field_annotations];
            for (int i = 0; i < num_field_annotations; ++i) {
                var field = classDef[iter++];
                fieldAnnotations[i] = new LSPosedFieldAnnotation(field, annotation[0][i]);
            }

            var num_method_annotations = classDef[iter++];
            methodAnnotations = new MethodAnnotation[num_method_annotations];
            for (int i = 0; i < num_method_annotations; ++i) {
                var method = classDef[iter++];
                methodAnnotations[i] = new LSPosedMethodAnnotation(method, annotation[1][i]);
            }

            var num_parameter_annotations = classDef[iter++];
            parameterAnnotations = new ParameterAnnotation[num_parameter_annotations];
            for (int i = 0; i < num_parameter_annotations; ++i) {
                var method = classDef[iter++];
                parameterAnnotations[i] = new LSPosedParameterAnnotation(method, (Object[][]) annotation[2][i]);
            }
        }

        @NonNull
        @Override
        public TypeId getType() {
            return type;
        }

        @Override
        public int getAccessFlags() {
            return accessFlags;
        }

        @Nullable
        @Override
        public TypeId getSuperClass() {
            return superClass;
        }

        @Nullable
        @Override
        public TypeId[] getInterfaces() {
            return interfaces;
        }

        @Nullable
        @Override
        public StringId getSourceFile() {
            return sourceFile;
        }

        @NonNull
        @Override
        public EncodedField[] getStaticFields() {
            return staticFields;
        }

        @NonNull
        @Override
        public EncodedField[] getInstanceFields() {
            return instanceFields;
        }

        @NonNull
        @Override
        public EncodedMethod[] getDirectMethods() {
            return directMethods;
        }

        @NonNull
        @Override
        public EncodedMethod[] getVirtualMethods() {
            return virtualMethods;
        }

        @Nullable
        @Override
        public FieldAnnotation[] getFieldAnnotations() {
            return fieldAnnotations;
        }

        @Nullable
        @Override
        public MethodAnnotation[] getMethodAnnotations() {
            return methodAnnotations;
        }

        @Nullable
        @Override
        public ParameterAnnotation[] getParameterAnnotations() {
            return parameterAnnotations;
        }
    }

    @NonNull
    @Override
    public ClassDef[] getClassDef() {
        return classDefs;
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

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (cookie == 0) return;
            DexParserBridge.closeDex(cookie);
            cookie = 0L;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected void finalize() {
        close();
    }
}

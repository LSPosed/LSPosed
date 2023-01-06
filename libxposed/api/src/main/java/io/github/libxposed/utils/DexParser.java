package io.github.libxposed.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface DexParser {
    int NO_INDEX = 0xffffffff;

    interface ClassDef {
        @NonNull
        TypeId getType();

        int getAccessFlags();

        @Nullable
        TypeId getSuperClass();

        @Nullable
        TypeId[] getInterfaces();

        @Nullable
        StringId getSourceFile();

        @NonNull
        EncodedField[] getStaticFields();

        @NonNull
        EncodedField[] getInstanceFields();

        @NonNull
        EncodedMethod[] getDirectMethods();

        @NonNull
        EncodedMethod[] getVirtualMethods();

        @Nullable
        Annotation[] getClassAnnotations();

        @Nullable
        FieldAnnotation[] getFieldAnnotations();

        @Nullable
        MethodAnnotation[] getMethodAnnotations();

        @Nullable
        ParameterAnnotation[] getParameterAnnotations();
    }

    interface FieldAnnotation {
        @NonNull
        FieldId getField();

        @NonNull
        Annotation[] getAnnotations();
    }

    interface MethodAnnotation {
        @NonNull
        MethodId getMethod();

        @NonNull
        Annotation[] getAnnotations();
    }

    interface ParameterAnnotation {
        @NonNull
        MethodId getMethod();

        @NonNull
        Annotation[][] getAnnotations();
    }

    interface Annotation {
        int getVisibility();

        @NonNull
        TypeId getType();

        @Nullable
        AnnotationElement[] getElements();
    }

    interface AnnotationElement {
        @NonNull
        StringId getName();

        @NonNull
        int getValueType();

        @Nullable
        byte[] getValue();
    }

    interface TypeId {
        @NonNull
        StringId getDescriptor();
    }

    interface EncodedField {
        @NonNull
        FieldId getField();

        int getAccessFlags();
    }

    interface EncodedMethod {
        @NonNull
        MethodId getMethod();

        int getAccessFlags();

        @NonNull
        MethodId[] getInvokedMethods();

        @NonNull
        FieldId[] getAccessedFields();

        @NonNull
        FieldId[] getAssignedFields();

        @NonNull
        byte[] getOpcodes();

        @NonNull
        StringId[] getReferredString();
    }

    interface Id {
        int getId();
    }

    interface StringId extends Id {
        @NonNull
        String getString();
    }

    interface FieldId extends Id {
        @NonNull
        TypeId getType();

        @NonNull
        TypeId getDeclaringClass();

        @NonNull
        StringId getName();
    }

    interface MethodId extends Id {
        @NonNull
        TypeId getDeclaringClass();

        @NonNull
        ProtoId getPrototype();

        @NonNull
        StringId getName();
    }

    interface ProtoId extends Id {
        @NonNull
        StringId getShorty();

        @NonNull
        TypeId getReturnType();

        @Nullable
        TypeId[] getParameters();
    }

    @NonNull
    ClassDef[] getClassDef();

    @NonNull
    StringId[] getStringId();

    @NonNull
    TypeId[] getTypeId();

    @NonNull
    FieldId[] getFieldId();

    @NonNull
    MethodId[] getMethodId();

    @NonNull
    ProtoId[] getProtoId();
}

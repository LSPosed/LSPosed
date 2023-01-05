package io.github.libxposed.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

public interface DexParser extends AutoCloseable {

    interface ClassDef {
        @NonNull
        TypeId getType();

        int getAccessFlags();

        @Nullable
        TypeId getSuperClass();

        @Nullable
        TypeList getInterfaces();

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
        AnnotationItem[] getAnnotations();
    }

    interface MethodAnnotation {
        @NonNull
        MethodId getMethod();

        @NonNull
        AnnotationItem[] getAnnotations();
    }

    interface ParameterAnnotation {
        @NonNull
        MethodId getMethod();

        @NonNull
        AnnotationList getAnnotations();
    }

    interface AnnotationList {
        @NonNull
        AnnotationItem[] getAnnotations();
    }

    interface AnnotationItem {
        int getVisibility();

        @NonNull
        Annotation getAnnotation();
    }

    interface Annotation {
        @NonNull
        TypeId getType();

        @Nullable
        AnnotationElement[] getElements();
    }

    interface AnnotationElement {
        int getType();

        ByteBuffer value();
    }

    interface TypeList {
        @NonNull
        TypeId[] getTypes();
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
        int[] getOpcodes();

        @NonNull
        StringId getReferencedString();
    }

    interface Id {
        int getIndex();
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
        ProtoId getProtoType();

        @NonNull
        StringId getName();
    }

    interface ProtoId extends Id {
        @NonNull
        StringId getShorty();

        @NonNull
        TypeId getReturnType();
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

    @NonNull
    TypeList[] getTypeList();
}

package io.github.libxposed.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;

public interface DexParser extends Closeable {
    int NO_INDEX = 0xffffffff;

    interface Array {
        @NonNull
        Value[] getValues();
    }

    interface Annotation {
        int getVisibility();

        @NonNull
        TypeId getType();

        @NonNull
        Element[] getElements();
    }

    interface Value {

        @Nullable
        byte[] getValue();

        int getValueType();
    }

    interface Element extends Value {
        @NonNull
        StringId getName();
    }

    interface TypeId {
        @NonNull
        StringId getDescriptor();
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
    Annotation[] getAnnotations();

    @NonNull
    Array[] getArrays();

    interface EarlyStopVisitor {
        boolean stop();
    }

    interface MemberVisitor extends EarlyStopVisitor {
    }

    interface ClassVisitor extends EarlyStopVisitor {
        @Nullable
        MemberVisitor visit(int clazz, int accessFlags, int superClass, @NonNull int[] interfaces, int sourceFile, @NonNull int[] staticFields, @NonNull int[] staticFieldsAccessFlags, @NonNull int[] instanceFields, @NonNull int[] instanceFieldsAccessFlags, @NonNull int[] directMethods, @NonNull int[] directMethodsAccessFlags, @NonNull int[] virtualMethods, @NonNull int[] virtualMethodsAccessFlags, @NonNull int[] annotations);
    }

    interface FieldVisitor extends MemberVisitor {
        void visit(int field, int accessFlags, @NonNull int[] annotations);
    }

    interface MethodVisitor extends MemberVisitor {
        @Nullable
        MethodBodyVisitor visit(int method, int accessFlags, boolean hasBody, @NonNull int[] annotations, @NonNull int[] parameterAnnotations);
    }

    interface MethodBodyVisitor {
        void visit(int method, int accessFlags, @NonNull int[] referredStrings, @NonNull int[] invokedMethods, @NonNull int[] accessedFields, @NonNull int[] assignedFields, @NonNull byte[] opcodes);
    }

    void visitDefinedClasses(@NonNull ClassVisitor visitor) throws IllegalStateException;
}

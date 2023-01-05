package io.github.libxposed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;

public interface XposedInterface {
    int API = 100;

    interface BeforeHookCallback<T> {
        @NonNull
        T getOrigin();

        @Nullable
        Object getThis();

        @NonNull
        Object[] getArgs();

        @Nullable
        <U> U getArg(int index);

        <U> void setArg(int index, U value);

        void returnAndSkip(@Nullable Object returnValue);

        void throwAndSkip(@Nullable Throwable throwable);

        @Nullable
        Object invokeOrigin(@Nullable Object thisObject, Object[] args) throws InvocationTargetException, IllegalAccessException;

        @Nullable
        Object invokeOrigin() throws InvocationTargetException, IllegalAccessException;

        <U> void setExtra(@NonNull String key, @Nullable U value) throws ConcurrentModificationException;
    }

    interface AfterHookCallback<T> {
        @NonNull
        T getOrigin();

        @Nullable
        Object getThis();

        @NonNull
        Object[] getArgs();

        @Nullable
        Object getResult();

        @Nullable
        Throwable getThrowable();

        boolean isSkipped();

        void setResult(@Nullable Object result);

        void setThrowable(@Nullable Throwable throwable);

        @Nullable
        Object invokeOrigin(@Nullable Object thisObject, Object[] args) throws InvocationTargetException, IllegalAccessException;

        @Nullable
        Object invokeOrigin() throws InvocationTargetException, IllegalAccessException;

        @Nullable
        <U> U getExtra(@NonNull String key);
    }

    interface BeforeHooker<T> {
        void before(@NonNull BeforeHookCallback<T> callback);
    }

    interface AfterHooker<T> {
        void after(@NonNull AfterHookCallback<T> callback);
    }

    interface Hooker<T> extends BeforeHooker<T>, AfterHooker<T> {
    }

    interface MethodUnhooker<T, U> {
        @NonNull
        U getOrigin();

        @NonNull
        T getHooker();

        void unhook();
    }

    @NonNull
    String getFrameworkName();

    @NonNull
    String getFrameworkVersion();

    long getFrameworkVersionCode();

    @Nullable
    MethodUnhooker<BeforeHooker<Method>, Method> hookBefore(@NonNull Method origin, @NonNull BeforeHooker<Method> hooker);

    @Nullable
    MethodUnhooker<AfterHooker<Method>, Method> hookAfter(@NonNull Method origin, @NonNull AfterHooker<Method> hooker);

    @Nullable
    MethodUnhooker<Hooker<Method>, Method> hook(@NonNull Method origin, @NonNull Hooker<Method> hooker);

    @Nullable
    MethodUnhooker<BeforeHooker<Method>, Method> hookBefore(@NonNull Method origin, int priority, @NonNull BeforeHooker<Method> hooker);

    @Nullable
    MethodUnhooker<AfterHooker<Method>, Method> hookAfter(@NonNull Method origin, int priority, @NonNull AfterHooker<Method> hooker);

    @Nullable
    MethodUnhooker<Hooker<Method>, Method> hook(@NonNull Method origin, int priority, @NonNull Hooker<Method> hooker);

    @Nullable
    <T> MethodUnhooker<BeforeHooker<Constructor<T>>, Constructor<T>> hookBefore(@NonNull Constructor<T> origin, @NonNull BeforeHooker<Constructor<T>> hooker);

    @Nullable
    <T> MethodUnhooker<AfterHooker<Constructor<T>>, Constructor<T>> hookAfter(@NonNull Constructor<T> origin, @NonNull AfterHooker<Constructor<T>> hooker);

    @Nullable
    <T> MethodUnhooker<Hooker<Constructor<T>>, Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull Hooker<Constructor<T>> hooker);

    @Nullable
    <T> MethodUnhooker<BeforeHooker<Constructor<T>>, Constructor<T>> hookBefore(@NonNull Constructor<T> origin, int priority, @NonNull BeforeHooker<Constructor<T>> hooker);

    @Nullable
    <T> MethodUnhooker<AfterHooker<Constructor<T>>, Constructor<T>> hookAfter(@NonNull Constructor<T> origin, int priority, @NonNull AfterHooker<Constructor<T>> hooker);

    @Nullable
    <T> MethodUnhooker<Hooker<Constructor<T>>, Constructor<T>> hook(@NonNull Constructor<T> origin, int priority, @NonNull Hooker<Constructor<T>> hooker);

    boolean deoptimize(@NonNull Method method);

    <T> boolean deoptimize(@NonNull Constructor<T> constructor);

    void log(@NonNull String message);

    void log(@NonNull String message, @NonNull Throwable throwable);

    @Nullable
    DexFile openDexFile(ByteBuffer dexData) throws IOException;

    interface DexFile extends AutoCloseable {
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
            long getIndex();
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
}

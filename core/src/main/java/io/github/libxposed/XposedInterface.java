package io.github.libxposed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;

public interface XposedInterface {
    interface BeforeHookCallback<T> {
        @NonNull
        T getOrigin();

        @Nullable
        Object getThis();

        @NonNull
        Object[] getArgs();

        void returnAndSkip(@Nullable Object returnValue);

        void throwAndSkip(@Nullable Throwable throwable);

        @Nullable
        Object invokeOrigin(@Nullable Object thisObject, Object[] args);

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
        Object invokeOrigin(@Nullable Object thisObject, Object[] args);

        @Nullable
        <U> U getExtra(@NonNull String key);
    }

    interface PriorityMethodHooker {
        int PRIORITY_DEFAULT = 50;

        default int getPriority() {
            return PRIORITY_DEFAULT;
        }
    }

    interface BeforeMethodHooker<T> extends PriorityMethodHooker {
        void before(@NonNull BeforeHookCallback<T> callback);
    }

    interface AfterMethodHooker<T> extends PriorityMethodHooker {
        void after(@NonNull AfterHookCallback<T> callback);
    }

    interface MethodHooker<T> extends BeforeMethodHooker<T>, AfterMethodHooker<T> {
    }

    interface MethodUnhooker<T, U> {
        @NonNull
        U getOrigin();

        @NonNull
        T getHooker();

        void unhook();
    }

    @NonNull
    String implementationName();

    @NonNull
    String implementationVersion();

    long implementationVersionCode();

    MethodUnhooker<BeforeMethodHooker<Method>, Method> hookBefore(@NonNull Method origin, @NonNull BeforeMethodHooker<Method> hooker);

    MethodUnhooker<AfterMethodHooker<Method>, Method> hookAfter(@NonNull Method origin, @NonNull AfterMethodHooker<Method> hooker);

    MethodUnhooker<MethodHooker<Method>, Method> hook(@NonNull Method origin, @NonNull MethodHooker<Method> hooker);

    <T> MethodUnhooker<BeforeMethodHooker<Constructor<T>>, Constructor<T>> hookBefore(@NonNull Constructor<T> origin, @NonNull BeforeMethodHooker<Constructor<T>> hooker);

    <T> MethodUnhooker<AfterMethodHooker<Constructor<T>>, Constructor<T>> hookAfter(@NonNull Constructor<T> origin, @NonNull AfterMethodHooker<Constructor<T>> hooker);

    <T> MethodUnhooker<MethodHooker<Constructor<T>>, Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull MethodHooker<Constructor<T>> hooker);

    boolean deoptimize(@Nullable Method method);

    <T> boolean deoptimize(@Nullable Constructor<T> constructor);

    @Nullable XposedUtils getUtils();

    void log(@NonNull String message);

    void log(@NonNull String message, @NonNull Throwable throwable);
}

package io.github.libxposed;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class XposedContextWrapper extends ContextWrapper implements XposedInterface {

    XposedContextWrapper(XposedContext base) {
        super(base);
    }

    public XposedContextWrapper(XposedContextWrapper base) {
        super(base);
    }

    final long getAPIVersion() {
        return 100;
    }

    @Override
    final public XposedContext getBaseContext() {
        return (XposedContext) super.getBaseContext();
    }

    @NonNull
    @Override
    final public String implementationName() {
        return getBaseContext().implementationVersion();
    }

    @NonNull
    @Override
    final public String implementationVersion() {
        return getBaseContext().implementationVersion();
    }

    @Override
    final public long implementationVersionCode() {
        return getBaseContext().implementationVersionCode();
    }

    @Override
    public MethodUnhooker<BeforeMethodHooker<Method>, Method> hookBefore(@NonNull Method origin, @NonNull BeforeMethodHooker<Method> hooker) {
        return getBaseContext().hookBefore(origin, hooker);
    }

    @Override
    public MethodUnhooker<AfterMethodHooker<Method>, Method> hookAfter(@NonNull Method origin, @NonNull AfterMethodHooker<Method> hooker) {
        return getBaseContext().hookAfter(origin, hooker);
    }

    @Override
    public MethodUnhooker<MethodHooker<Method>, Method> hook(@NonNull Method origin, @NonNull MethodHooker<Method> hooker) {
        return getBaseContext().hook(origin, hooker);
    }

    @Override
    public <T> MethodUnhooker<BeforeMethodHooker<Constructor<T>>, Constructor<T>> hookBefore(@NonNull Constructor<T> origin, @NonNull BeforeMethodHooker<Constructor<T>> hooker) {
        return getBaseContext().hookBefore(origin, hooker);
    }

    @Override
    public <T> MethodUnhooker<AfterMethodHooker<Constructor<T>>, Constructor<T>> hookAfter(@NonNull Constructor<T> origin, @NonNull AfterMethodHooker<Constructor<T>> hooker) {
        return getBaseContext().hookAfter(origin, hooker);
    }

    @Override
    public <T> MethodUnhooker<MethodHooker<Constructor<T>>, Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull MethodHooker<Constructor<T>> hooker) {
        return getBaseContext().hook(origin, hooker);
    }

    @Override
    final public void log(@NonNull String message) {
        getBaseContext().log(message);
    }

    @Override
    final public void log(@NonNull String message, @NonNull Throwable throwable) {
        getBaseContext().log(message, throwable);
    }

    @Override
    final protected void attachBaseContext(Context base) {
        if (base instanceof XposedContext || base instanceof XposedContextWrapper) {
            super.attachBaseContext(base);
        } else {
            throw new IllegalArgumentException();
        }
    }
}

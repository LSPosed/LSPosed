package io.github.libxposed;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import io.github.libxposed.utils.DexParser;

public class XposedContextWrapper extends ContextWrapper implements XposedInterface {

    XposedContextWrapper(XposedContext base) {
        super(base);
    }

    public XposedContextWrapper(XposedContextWrapper base) {
        super(base);
    }

    final public int getAPIVersion() {
        return API;
    }

    @Override
    final public XposedContext getBaseContext() {
        return (XposedContext) super.getBaseContext();
    }

    @NonNull
    @Override
    final public String getFrameworkName() {
        return getBaseContext().getFrameworkName();
    }

    @NonNull
    @Override
    final public String getFrameworkVersion() {
        return getBaseContext().getFrameworkVersion();
    }

    @Override
    final public long getFrameworkVersionCode() {
        return getBaseContext().getFrameworkVersionCode();
    }

    @Override
    public MethodUnhooker<BeforeHooker<Method>, Method> hookBefore(@NonNull Method origin, @NonNull BeforeHooker<Method> hooker) {
        return getBaseContext().hookBefore(origin, hooker);
    }

    @Override
    public MethodUnhooker<AfterHooker<Method>, Method> hookAfter(@NonNull Method origin, @NonNull AfterHooker<Method> hooker) {
        return getBaseContext().hookAfter(origin, hooker);
    }

    @Override
    public MethodUnhooker<Hooker<Method>, Method> hook(@NonNull Method origin, @NonNull Hooker<Method> hooker) {
        return getBaseContext().hook(origin, hooker);
    }

    @Override
    public MethodUnhooker<BeforeHooker<Method>, Method> hookBefore(@NonNull Method origin, int priority, @NonNull BeforeHooker<Method> hooker) {
        return getBaseContext().hookBefore(origin, priority, hooker);
    }

    @Override
    public MethodUnhooker<AfterHooker<Method>, Method> hookAfter(@NonNull Method origin, int priority, @NonNull AfterHooker<Method> hooker) {
        return getBaseContext().hookAfter(origin, priority, hooker);
    }

    @Override
    public MethodUnhooker<Hooker<Method>, Method> hook(@NonNull Method origin, int priority, @NonNull Hooker<Method> hooker) {
        return getBaseContext().hook(origin, priority, hooker);
    }

    @Override
    public <T> MethodUnhooker<BeforeHooker<Constructor<T>>, Constructor<T>> hookBefore(@NonNull Constructor<T> origin, @NonNull BeforeHooker<Constructor<T>> hooker) {
        return getBaseContext().hookBefore(origin, hooker);
    }

    @Override
    public <T> MethodUnhooker<AfterHooker<Constructor<T>>, Constructor<T>> hookAfter(@NonNull Constructor<T> origin, @NonNull AfterHooker<Constructor<T>> hooker) {
        return getBaseContext().hookAfter(origin, hooker);
    }

    @Override
    public <T> MethodUnhooker<Hooker<Constructor<T>>, Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull Hooker<Constructor<T>> hooker) {
        return getBaseContext().hook(origin, hooker);
    }

    @Override
    public <T> MethodUnhooker<BeforeHooker<Constructor<T>>, Constructor<T>> hookBefore(@NonNull Constructor<T> origin, int priority, @NonNull BeforeHooker<Constructor<T>> hooker) {
        return getBaseContext().hookBefore(origin, priority, hooker);
    }

    @Override
    public <T> MethodUnhooker<AfterHooker<Constructor<T>>, Constructor<T>> hookAfter(@NonNull Constructor<T> origin, int priority, @NonNull AfterHooker<Constructor<T>> hooker) {
        return getBaseContext().hookAfter(origin, priority, hooker);
    }

    @Override
    public <T> MethodUnhooker<Hooker<Constructor<T>>, Constructor<T>> hook(@NonNull Constructor<T> origin, int priority, @NonNull Hooker<Constructor<T>> hooker) {
        return getBaseContext().hook(origin, priority, hooker);
    }

    @Override
    public boolean deoptimize(@NonNull Method method) {
        return getBaseContext().deoptimize(method);
    }

    @Override
    public <T> boolean deoptimize(@NonNull Constructor<T> constructor) {
        return getBaseContext().deoptimize(constructor);
    }

    @Override
    final public void log(@NonNull String message) {
        getBaseContext().log(message);
    }

    @Override
    final public void log(@NonNull String message, @NonNull Throwable throwable) {
        getBaseContext().log(message, throwable);
    }

    @Nullable
    @Override
    public DexParser parseDex(ByteBuffer dexData) throws IOException {
        return getBaseContext().parseDex(dexData);
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

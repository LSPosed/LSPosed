package io.github.libxposed;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.annotation.NonNull;

public class XposedContextWrapper extends ContextWrapper implements XposedInterface {

    XposedContextWrapper(XposedContext base) {
        super(base);
    }

    public XposedContextWrapper(XposedContextWrapper base) {
        super(base);
    }

    @Override
    final public XposedContext getBaseContext() {
        return (XposedContext) super.getBaseContext();
    }

    @Override
    final public void hook() {
        getBaseContext().hook();
    }

    @NonNull
    @Override
    public String implementationName() {
        return getBaseContext().implementationVersion();
    }

    @NonNull
    @Override
    public String implementationVersion() {
        return getBaseContext().implementationVersion();
    }

    @Override
    public long implementationVersionCode() {
        return getBaseContext().implementationVersionCode();
    }

    @Override
    final public void log(String message) {
        getBaseContext().log(message);
    }

    @Override
    final public void log(String message, Throwable throwable) {
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

package io.github.libxposed;

import android.content.Context;
import android.content.ContextWrapper;

public class XposedContextWrapper extends ContextWrapper implements XposedInterface {

    public XposedContextWrapper(XposedContext base) {
        super(base);
    }

    @Override
    public XposedContext getBaseContext() {
        return (XposedContext) super.getBaseContext();
    }

    @Override
    public void hook() {
        getBaseContext().hook();
    }

    @Override
    final protected void attachBaseContext(Context base) {
        if (base instanceof XposedContext) {
            super.attachBaseContext(base);
        } else {
            throw new IllegalArgumentException();
        }
    }
}

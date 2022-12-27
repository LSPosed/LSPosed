package io.github.libxposed;

import xposed.dummy.XResourcesSuperClass;

public abstract class XposedResources extends XResourcesSuperClass {
    /** Dummy, will never be called (objects are transferred to this class only). */
    protected XposedResources() {
        super();
        throw new UnsupportedOperationException();
    }

    protected XposedResources(ClassLoader classLoader) {
        super(classLoader);
        throw new UnsupportedOperationException();
    }
}

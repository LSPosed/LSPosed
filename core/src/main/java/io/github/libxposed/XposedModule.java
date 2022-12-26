package io.github.libxposed;

public abstract class XposedModule extends XposedContextWrapper implements XposedModuleInterface {
    public XposedModule(XposedContext base) {
        super(base);
    }
}

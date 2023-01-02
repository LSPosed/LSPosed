package io.github.libxposed;

import androidx.annotation.NonNull;

public abstract class XposedModule extends XposedContextWrapper implements XposedModuleInterface {
    public XposedModule(XposedContext base, @SuppressWarnings("unused") @NonNull ModuleLoadedParam param) {
        super(base);
    }
}

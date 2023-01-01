package io.github.libxposed;

import android.os.Bundle;

import androidx.annotation.Nullable;

public abstract class XposedModule extends XposedContextWrapper implements XposedModuleInterface {
    public XposedModule(XposedContext base, @SuppressWarnings("unused") boolean isSystemServer, @SuppressWarnings("unused") String processName, @SuppressWarnings("unused") String appDir, @SuppressWarnings("unused") @Nullable Bundle extras) {
        super(base);
    }
}

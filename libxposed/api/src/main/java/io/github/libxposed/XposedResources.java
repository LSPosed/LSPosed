package io.github.libxposed;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public abstract class XposedResources extends Resources {
    public XposedResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        super(assets, metrics, config);
    }

    public XposedResources(ClassLoader classLoader) {
        super(classLoader);
    }
}

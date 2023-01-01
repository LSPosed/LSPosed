package android.content.res;

import android.util.DisplayMetrics;

public abstract class XposedResources extends Resources {
    @SuppressWarnings("deprecation")
    public XposedResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        super(assets, metrics, config);
    }

    public XposedResources(ClassLoader classLoader) {
        super(null, null, null);
    }
}

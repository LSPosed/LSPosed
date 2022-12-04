package org.lsposed.manager.util;

import android.content.Context;
import android.content.ContextWrapper;

public class FakeContext extends ContextWrapper {
    private final String packageName;

    public FakeContext(Context context, String packageName) {
        super(context);
        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }
}

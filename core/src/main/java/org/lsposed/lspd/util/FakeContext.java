package org.lsposed.lspd.util;

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.content.ContentResolver;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.Nullable;

import org.lsposed.lspd.service.ConfigFileManager;
import org.lsposed.lspd.service.PackageService;

public class FakeContext extends ContextWrapper {
    static ApplicationInfo systemApplicationInfo = null;
    static Resources.Theme theme = null;

    public FakeContext() {
        super(null);
    }

    @Override
    public String getPackageName() {
        return "android";
    }

    @Override
    public Resources getResources() {
        return ConfigFileManager.getResources();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        try {
            if (systemApplicationInfo == null)
                systemApplicationInfo = PackageService.getApplicationInfo("android", 0, 0);
        } catch (Throwable e) {
            Log.e(TAG, "getApplicationInfo", e);
        }
        return systemApplicationInfo;
    }

    @Override
    public ContentResolver getContentResolver() {
        return null;
    }

    public int getUserId() {
        return 0;
    }

    @Override
    public Resources.Theme getTheme() {
        if (theme == null) theme = getResources().newTheme();
        return theme;
    }

    @Nullable
    @Override
    public String getAttributionTag() {
        return null;
    }
}

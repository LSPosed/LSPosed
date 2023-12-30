package org.lsposed.lspd.util;

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.Nullable;

import org.lsposed.lspd.service.ConfigFileManager;
import org.lsposed.lspd.service.PackageService;

import hidden.HiddenApiBridge;

public class FakeContext extends ContextWrapper {
    static ApplicationInfo systemApplicationInfo = null;
    static Resources.Theme theme = null;
    private String packageName = "android";
    public FakeContext() {
        super(null);
    }

    public FakeContext(String packageName) {
        super(null);
        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public Resources getResources() {
        return ConfigFileManager.getResources();
    }

    @Override
    public String getOpPackageName() {
        return "android";
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
        return new ContentResolver(this) {
        };
    }

    public int getUserId() {
        return 0;
    }

    public UserHandle getUser() {
        return HiddenApiBridge.UserHandle(0);
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

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        throw new PackageManager.NameNotFoundException(packageName);
    }
}

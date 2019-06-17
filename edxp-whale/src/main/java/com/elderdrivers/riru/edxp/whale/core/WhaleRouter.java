package com.elderdrivers.riru.edxp.whale.core;

import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

import com.elderdrivers.riru.edxp._hooker.impl.HandleBindApp;
import com.elderdrivers.riru.edxp._hooker.impl.LoadedApkCstr;
import com.elderdrivers.riru.edxp._hooker.yahfa.HandleBindAppHooker;
import com.elderdrivers.riru.edxp._hooker.yahfa.LoadedApkConstructorHooker;
import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;
import com.elderdrivers.riru.edxp.framework.Zygote;
import com.elderdrivers.riru.edxp.proxy.BaseRouter;
import com.elderdrivers.riru.edxp.whale.config.WhaleEdxpConfig;
import com.elderdrivers.riru.edxp.whale.config.WhaleHookProvider;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class WhaleRouter extends BaseRouter {

    public void onEnterChildProcess() {

    }

    public void injectConfig() {
        BaseRouter.useXposedApi = true;
        EdXpConfigGlobal.sConfig = new WhaleEdxpConfig();
        EdXpConfigGlobal.sHookProvider = new WhaleHookProvider();
        XposedBridge.log("using HookProvider: " + EdXpConfigGlobal.sHookProvider.getClass().getName());
        Zygote.allowFileAcrossFork("/system/lib/libwhale.edxp.so");
        Zygote.allowFileAcrossFork("/system/lib64/libwhale.edxp.so");
        Zygote.allowFileAcrossFork("/system/lib/libart.so");
        Zygote.allowFileAcrossFork("/system/lib64/libart.so");
    }

}

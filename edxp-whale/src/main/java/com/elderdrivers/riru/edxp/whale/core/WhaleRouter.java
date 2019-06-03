package com.elderdrivers.riru.edxp.whale.core;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;
import com.elderdrivers.riru.edxp.proxy.BaseRouter;
import com.elderdrivers.riru.edxp.whale.config.WhaleEdxpConfig;
import com.elderdrivers.riru.edxp.whale.config.WhaleHookProvider;

import de.robv.android.xposed.XposedBridge;

public class WhaleRouter extends BaseRouter {

    public void onEnterChildProcess() {

    }

    public void injectConfig() {
        EdXpConfigGlobal.sConfig = new WhaleEdxpConfig();
        EdXpConfigGlobal.sHookProvider = new WhaleHookProvider();
        XposedBridge.log("using HookProvider: " + EdXpConfigGlobal.sHookProvider.getClass().getName());
    }

}

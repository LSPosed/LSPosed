package com.elderdrivers.riru.edxp.yahfa.core;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;
import com.elderdrivers.riru.edxp.proxy.BaseRouter;
import com.elderdrivers.riru.edxp.yahfa.config.YahfaHookProvider;
import com.elderdrivers.riru.edxp.yahfa.dexmaker.DynamicBridge;

public class YahfaRouter extends BaseRouter {

    YahfaRouter() {
    }

    public void onEnterChildProcess() {
        DynamicBridge.onForkPost();
    }

    public void injectConfig() {
        EdXpConfigGlobal.sHookProvider = new YahfaHookProvider();
    }

}

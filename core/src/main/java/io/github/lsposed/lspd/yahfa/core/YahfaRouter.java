package io.github.lsposed.lspd.yahfa.core;

import io.github.lsposed.lspd.config.LSPdConfigGlobal;
import io.github.lsposed.lspd.proxy.BaseRouter;
import io.github.lsposed.lspd.yahfa.config.YahfaHookProvider;
import io.github.lsposed.lspd.yahfa.dexmaker.DynamicBridge;

public class YahfaRouter extends BaseRouter {

    YahfaRouter() {
    }

    public void onEnterChildProcess() {
        DynamicBridge.onForkPost();
    }

    public void injectConfig() {
        LSPdConfigGlobal.sHookProvider = new YahfaHookProvider();
    }

}

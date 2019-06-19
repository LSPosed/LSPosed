package com.elderdrivers.riru.edxp.whale.core;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;
import com.elderdrivers.riru.edxp.framework.Zygote;
import com.elderdrivers.riru.edxp.proxy.BaseRouter;
import com.elderdrivers.riru.edxp.whale.config.WhaleEdxpConfig;
import com.elderdrivers.riru.edxp.whale.config.WhaleHookProvider;

public class WhaleRouter extends BaseRouter {

    public void onEnterChildProcess() {

    }

    public void injectConfig() {
        BaseRouter.useXposedApi = true;
        EdXpConfigGlobal.sConfig = new WhaleEdxpConfig();
        EdXpConfigGlobal.sHookProvider = new WhaleHookProvider();
        Zygote.allowFileAcrossFork("/system/lib/libwhale.edxp.so");
        Zygote.allowFileAcrossFork("/system/lib64/libwhale.edxp.so");
        Zygote.allowFileAcrossFork("/system/lib/libart.so");
        Zygote.allowFileAcrossFork("/system/lib64/libart.so");
    }

}

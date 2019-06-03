package com.elderdrivers.riru.edxp.whale.core;

import android.os.Build;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.config.InstallerChooser;
import com.elderdrivers.riru.edxp.core.BaseEdxpImpl;
import com.elderdrivers.riru.edxp.core.Yahfa;
import com.elderdrivers.riru.edxp.core.yahfa.HookMethodResolver;

public class WhaleEdxpImpl extends BaseEdxpImpl {

    @Override
    protected com.elderdrivers.riru.edxp.proxy.Router createRouter() {
        return new WhaleRouter();
    }

    @Override
    public int getVariant() {
        return WHALE;
    }

    @Override
    public void init() {
        Yahfa.init(Build.VERSION.SDK_INT);
        HookMethodResolver.init();
        getRouter().injectConfig();
        InstallerChooser.setInstallerPackageName(ConfigManager.getInstallerPackageName());

        setInitialized();
    }
}

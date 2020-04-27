package com.elderdrivers.riru.edxp.whale.core;

import android.os.Build;

import com.elderdrivers.riru.edxp.core.BaseEdxpImpl;
import com.elderdrivers.riru.edxp.core.EdxpImpl;
import com.elderdrivers.riru.edxp.core.Main;
import com.elderdrivers.riru.edxp.core.Yahfa;
import com.elderdrivers.riru.edxp.core.yahfa.HookMethodResolver;
import com.elderdrivers.riru.edxp.proxy.Router;

public class WhaleEdxpImpl extends BaseEdxpImpl {

    static {
        final EdxpImpl edxpImpl = new WhaleEdxpImpl();
        if (Main.setEdxpImpl(edxpImpl)) {
            edxpImpl.init();
        }
    }

    @Override
    protected Router createRouter() {
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
        setInitialized();
    }

}

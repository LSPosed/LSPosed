package com.elderdrivers.riru.edxp.sandhook.core;

import android.os.Build;

import com.elderdrivers.riru.edxp.core.BaseEdxpImpl;
import com.elderdrivers.riru.edxp.core.EdxpImpl;
import com.elderdrivers.riru.edxp.core.Main;
import com.elderdrivers.riru.edxp.core.Yahfa;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

public class SandHookEdxpImpl extends BaseEdxpImpl {

    static {
        final EdxpImpl edxpImpl = new SandHookEdxpImpl();
        if (Main.setEdxpImpl(edxpImpl)) {
            edxpImpl.init();
        }
    }

    @Override
    protected com.elderdrivers.riru.edxp.proxy.Router createRouter() {
        return new SandHookRouter();
    }

    @Variant
    @Override
    public int getVariant() {
        return SANDHOOK;
    }

    @Override
    public void init() {
        Yahfa.init(Build.VERSION.SDK_INT);
        getRouter().injectConfig();
        SandHookXposedBridge.init();
        setInitialized();
    }
}

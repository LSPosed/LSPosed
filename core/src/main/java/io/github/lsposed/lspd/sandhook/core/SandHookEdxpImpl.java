package io.github.lsposed.lspd.sandhook.core;

import android.os.Build;

import io.github.lsposed.lspd.core.BaseEdxpImpl;
import io.github.lsposed.lspd.core.EdxpImpl;
import io.github.lsposed.lspd.core.Main;
import io.github.lsposed.lspd.nativebridge.Yahfa;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

public class SandHookEdxpImpl extends BaseEdxpImpl {

    static {
        final EdxpImpl lspdImpl = new SandHookEdxpImpl();
        if (Main.setEdxpImpl(lspdImpl)) {
            lspdImpl.init();
        }
    }

    @Override
    protected io.github.lsposed.lspd.proxy.Router createRouter() {
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

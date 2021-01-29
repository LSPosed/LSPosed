package io.github.lsposed.lspd.yahfa.core;

import android.os.Build;

import io.github.lsposed.lspd.core.BaseEdxpImpl;
import io.github.lsposed.lspd.core.EdxpImpl;
import io.github.lsposed.lspd.core.Main;
import io.github.lsposed.lspd.core.Proxy;
import io.github.lsposed.lspd.nativebridge.Yahfa;
import io.github.lsposed.lspd.proxy.NormalProxy;
import io.github.lsposed.lspd.proxy.Router;

public class YahfaEdxpImpl extends BaseEdxpImpl {

    static {
        final EdxpImpl lspdImpl = new YahfaEdxpImpl();
        if (Main.setEdxpImpl(lspdImpl)) {
            lspdImpl.init();
        }
    }

    @Variant
    @Override
    public int getVariant() {
        return YAHFA;
    }

    @Override
    public void init() {
        Yahfa.init(Build.VERSION.SDK_INT);
        getRouter().injectConfig();
        setInitialized();
    }

    @Override
    protected Proxy createNormalProxy() {
        return new NormalProxy(getRouter());
    }

    @Override
    protected Router createRouter() {
        return new YahfaRouter();
    }
}

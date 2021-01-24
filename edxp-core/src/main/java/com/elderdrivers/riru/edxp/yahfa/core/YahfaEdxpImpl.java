package com.elderdrivers.riru.edxp.yahfa.core;

import android.os.Build;

import com.elderdrivers.riru.edxp.core.BaseEdxpImpl;
import com.elderdrivers.riru.edxp.core.EdxpImpl;
import com.elderdrivers.riru.edxp.core.Main;
import com.elderdrivers.riru.edxp.core.Proxy;
import com.elderdrivers.riru.edxp.core.Yahfa;
import com.elderdrivers.riru.edxp.proxy.NormalProxy;
import com.elderdrivers.riru.edxp.proxy.Router;

public class YahfaEdxpImpl extends BaseEdxpImpl {

    static {
        final EdxpImpl edxpImpl = new YahfaEdxpImpl();
        if (Main.setEdxpImpl(edxpImpl)) {
            edxpImpl.init();
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

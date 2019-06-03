package com.elderdrivers.riru.edxp.proxy;

import com.elderdrivers.riru.edxp.core.Proxy;

public abstract class BaseProxy implements Proxy {

    protected Router mRouter;

    public BaseProxy(Router router) {
        mRouter = router;
    }

    @Override
    public boolean init() {
        return true;
    }
}

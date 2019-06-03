package com.elderdrivers.riru.edxp.core;

import androidx.annotation.NonNull;

import com.elderdrivers.riru.edxp.proxy.BlackWhiteListProxy;
import com.elderdrivers.riru.edxp.proxy.NormalProxy;
import com.elderdrivers.riru.edxp.proxy.Router;

public abstract class BaseEdxpImpl implements EdxpImpl {

    protected Proxy mBlackWhiteListProxy;
    protected Proxy mNormalProxy;
    protected Router mRouter;

    protected boolean mInitialized = false;

    protected void setInitialized() {
        mInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    @NonNull
    @Override
    public Proxy getBlackWhiteListProxy() {
        if (mBlackWhiteListProxy == null) {
            mBlackWhiteListProxy = createBlackWhiteListProxy();
        }
        return mBlackWhiteListProxy;
    }

    @NonNull
    @Override
    public Proxy getNormalProxy() {
        if (mNormalProxy == null) {
            mNormalProxy = createNormalProxy();
        }
        return mNormalProxy;
    }

    @NonNull
    @Override
    public Router getRouter() {
        if (mRouter == null) {
            mRouter = createRouter();
        }
        return mRouter;
    }

    protected Proxy createBlackWhiteListProxy() {
        return new BlackWhiteListProxy(getRouter());
    }

    protected Proxy createNormalProxy() {
        return new NormalProxy(getRouter());
    }

    protected abstract Router createRouter();
}

package io.github.lsposed.lspd.proxy;

import io.github.lsposed.lspd.core.Proxy;

import de.robv.android.xposed.XposedBridge;

public abstract class BaseProxy implements Proxy {

    protected Router mRouter;

    public BaseProxy(Router router) {
        mRouter = router;
    }

    @Override
    public boolean init() {
        return true;
    }

    public static void onBlackListed() {
        XposedBridge.clearAllCallbacks();
    }
}

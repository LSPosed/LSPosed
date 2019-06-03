package com.elderdrivers.riru.edxp.core;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.edxp.proxy.Router;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface EdxpImpl extends KeepAll {

    int NONE = 0;
    int YAHFA = 1;
    int SANDHOOK = 2;
    int WHALE = 3;

    @NonNull
    Proxy getNormalProxy();

    @NonNull
    Proxy getBlackWhiteListProxy();

    @NonNull
    Router getRouter();

    @Variant
    int getVariant();

    void init();

    boolean isInitialized();

    @Retention(SOURCE)
    @IntDef({NONE, YAHFA, SANDHOOK, WHALE})
    @interface Variant {
    }
}

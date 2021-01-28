package io.github.lsposed.lspd.core;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import io.github.lsposed.common.KeepAll;
import io.github.lsposed.lspd.proxy.Router;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface EdxpImpl extends KeepAll {

    int NONE = 0;
    int YAHFA = 1;
    int SANDHOOK = 2;

    @NonNull
    Proxy getNormalProxy();

    @NonNull
    Router getRouter();

    @Variant
    int getVariant();

    void init();

    boolean isInitialized();

    @Retention(SOURCE)
    @IntDef({NONE, YAHFA, SANDHOOK})
    @interface Variant {
    }
}

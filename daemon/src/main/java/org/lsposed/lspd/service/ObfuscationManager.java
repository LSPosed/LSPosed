package org.lsposed.lspd.service;

import android.os.SharedMemory;

public class ObfuscationManager {
    static boolean enabled() {
        return ConfigManager.getInstance(false).dexObfuscate();
    }

    static native void init();

    // For module dexes
    static native SharedMemory obfuscateDex(SharedMemory memory);

    // preload lspd dex only, on daemon startup.
    // it will cache the result, so we could obtain it back on startup.
    static native int preloadDex();

    static native long getPreloadedDexSize();

    // generates signature
    static native String getObfuscatedSignature();
}

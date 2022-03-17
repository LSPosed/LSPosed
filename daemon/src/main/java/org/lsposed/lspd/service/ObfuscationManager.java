package org.lsposed.lspd.service;

import android.os.SharedMemory;

public class ObfuscationManager {
    // For module dexes
    static native SharedMemory obfuscateDex(SharedMemory memory);

    // generates signature
    static native String getObfuscatedSignature();
}

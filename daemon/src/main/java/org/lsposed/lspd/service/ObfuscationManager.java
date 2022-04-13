package org.lsposed.lspd.service;

import android.os.SharedMemory;

import java.util.HashMap;

public class ObfuscationManager {
    // For module dexes
    static native SharedMemory obfuscateDex(SharedMemory memory);

    // generates signature
    static native HashMap<String, String> getSignatures();
}

package org.lsposed.lspd.service;

import android.os.SharedMemory;

import java.nio.ByteBuffer;

public class ObfuscationService {
    static native SharedMemory obfuscateDex(SharedMemory memory);
}
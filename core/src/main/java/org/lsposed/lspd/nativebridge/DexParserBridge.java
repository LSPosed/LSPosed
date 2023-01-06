package org.lsposed.lspd.nativebridge;

import java.io.IOException;
import java.nio.ByteBuffer;

import dalvik.annotation.optimization.FastNative;

public class DexParserBridge {
    public static native Object parseDex(ByteBuffer dex) throws IOException;

    @FastNative
    public static native Object parseMethod(ByteBuffer dex, int codeOffset);
}

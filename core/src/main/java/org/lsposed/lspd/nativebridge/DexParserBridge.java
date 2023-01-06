package org.lsposed.lspd.nativebridge;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DexParserBridge {
    public static native Object parseDex(ByteBuffer dex) throws IOException;

    public static native Object parseMethod(ByteBuffer dex, int codeOffset);
}

package org.lsposed.lspd.nativebridge;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DexParserBridge {
    public static native Object parseDex(ByteBuffer byteBuffer) throws IOException;

    public static native Object parseMethod(long cookie, int code);

    public static native void closeDex(long cookie);
}

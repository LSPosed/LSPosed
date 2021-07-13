package hidden;

import java.nio.ByteBuffer;

import dalvik.system.BaseDexClassLoader;

public class ByteBufferDexClassLoader extends BaseDexClassLoader {
    public ByteBufferDexClassLoader(ByteBuffer[] dexFiles, ClassLoader parent) {
        super(dexFiles, parent);
    }
}

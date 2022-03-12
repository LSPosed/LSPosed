package hidden;

import java.nio.ByteBuffer;

import dalvik.system.BaseDexClassLoader;

public class ByteBufferDexClassLoader extends BaseDexClassLoader {

    public ByteBufferDexClassLoader(ByteBuffer[] dexFiles, ClassLoader parent) {
        super(dexFiles, parent);
    }

    public ByteBufferDexClassLoader(ByteBuffer[] dexFiles, String librarySearchPath, ClassLoader parent) {
        super(dexFiles, librarySearchPath, parent);
    }

    public String getLdLibraryPath() {
        return super.getLdLibraryPath();
    }
}

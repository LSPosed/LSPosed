package dalvik.system;

import java.nio.ByteBuffer;

public class BaseDexClassLoader extends ClassLoader {
    public BaseDexClassLoader(ByteBuffer[] dexFiles, ClassLoader parent) {
        throw new RuntimeException("Stub!");
    }

    public BaseDexClassLoader(ByteBuffer[] dexFiles, String librarySearchPath, ClassLoader parent) {
        throw new RuntimeException("Stub!");
    }

    public String getLdLibraryPath() {
        throw new RuntimeException("Stub!");
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package dalvik.system;

import java.io.File;
import java.nio.ByteBuffer;

public class BaseDexClassLoader extends ClassLoader {
    private Object pathList;
    public BaseDexClassLoader(String dexPath, File optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        throw new RuntimeException("Stub!");
    }

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

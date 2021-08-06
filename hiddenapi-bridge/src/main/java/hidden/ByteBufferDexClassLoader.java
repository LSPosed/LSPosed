package hidden;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import dalvik.system.BaseDexClassLoader;

public class ByteBufferDexClassLoader extends BaseDexClassLoader {
    static Field pathListField = null;
    static Field dexElementsField = null;
    static Field dexFileField = null;
    static Field nameField = null;

    static {
        try {
            pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            dexElementsField = pathListField.getType().getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            var elementType = dexElementsField.getType().getComponentType();
            dexFileField = elementType.getDeclaredField("dexFile");
            dexFileField.setAccessible(true);
            nameField = dexFileField.getType().getDeclaredField("mFileName");
            nameField.setAccessible(true);
        } catch (Throwable ignored) {

        }
    }

    public ByteBufferDexClassLoader(ByteBuffer[] dexFiles, ClassLoader parent) {
        super(dexFiles, parent);
    }

    public ByteBufferDexClassLoader(ByteBuffer[] dexFiles, String librarySearchPath, ClassLoader parent) {
        super(dexFiles, librarySearchPath, parent);
    }

    public void fixDexName(String name) throws IllegalAccessException {
        nameField.set(dexFileField.get(((Object[]) dexElementsField.get(pathListField.get(this)))[0]), name);
    }

    public String getLdLibraryPath() {
        return super.getLdLibraryPath();
    }
}

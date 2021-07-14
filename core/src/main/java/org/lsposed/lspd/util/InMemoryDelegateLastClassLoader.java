package org.lsposed.lspd.util;

import static de.robv.android.xposed.XposedBridge.TAG;

import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import hidden.ByteBufferDexClassLoader;

@SuppressWarnings("ConstantConditions")
public final class InMemoryDelegateLastClassLoader extends ByteBufferDexClassLoader {
    private static final String zipSeparator = "!/";
    private final String apk;
    private final List<File> nativeLibraryDirs = new ArrayList<>();

    private static List<File> splitPaths(String searchPath) {
        var result = new ArrayList<File>();
        if (searchPath == null) return result;
        for (var path : searchPath.split(File.pathSeparator)) {
            result.add(new File(path));
        }
        return result;
    }

    private InMemoryDelegateLastClassLoader(ByteBuffer[] dexBuffers,
                                            ClassLoader parent,
                                            String apk) {
        super(dexBuffers, parent);
        this.apk = apk;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private InMemoryDelegateLastClassLoader(ByteBuffer[] dexBuffers,
                                            String librarySearchPath,
                                            ClassLoader parent,
                                            String apk) {
        super(dexBuffers, librarySearchPath, parent);
        initNativeLibraryDirs(librarySearchPath);
        this.apk = apk;
    }

    private void initNativeLibraryDirs(String librarySearchPath) {
        nativeLibraryDirs.addAll(splitPaths(librarySearchPath));
        nativeLibraryDirs.addAll(splitPaths(System.getProperty("java.library.path")));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        var cl = findLoadedClass(name);
        if (cl != null) {
            return cl;
        }
        try {
            return Object.class.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        ClassNotFoundException fromSuper;
        try {
            return findClass(name);
        } catch (ClassNotFoundException ex) {
            fromSuper = ex;
        }
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException cnfe) {
            throw fromSuper;
        }
    }

    @Override
    public String findLibrary(String libraryName) {
        var fileName = System.mapLibraryName(libraryName);
        for (var file : nativeLibraryDirs) {
            var path = file.getPath();
            if (path.contains(zipSeparator)) {
                var split = path.split(zipSeparator, 2);
                try (var jarFile = new JarFile(split[0])) {
                    var entryName = split[1] + '/' + fileName;
                    var entry = jarFile.getEntry(entryName);
                    if (entry != null && entry.getMethod() == ZipEntry.STORED) {
                        return split[0] + zipSeparator + entryName;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Can not open " + split[0], e);
                }
            } else if (file.isDirectory()) {
                var entryPath = new File(file, fileName).getPath();
                try {
                    var fd = Os.open(entryPath, OsConstants.O_RDONLY, 0);
                    Os.close(fd);
                    return entryPath;
                } catch (ErrnoException ignored) {
                }
            }
        }
        return null;
    }

    @Override
    public String getLdLibraryPath() {
        var result = new StringBuilder();
        for (var directory : nativeLibraryDirs) {
            if (result.length() > 0) {
                result.append(':');
            }
            result.append(directory);
        }
        return result.toString();
    }

    @Override
    protected URL findResource(String name) {
        try {
            var urlHandler = new ClassPathURLStreamHandler(apk);
            return urlHandler.getEntryUrlOrNull(name);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        var result = new ArrayList<URL>();
        var url = findResource(name);
        if (url != null) result.add(url);
        return Collections.enumeration(result);
    }

    @Override
    public URL getResource(String name) {
        var resource = Object.class.getClassLoader().getResource(name);
        if (resource != null) return resource;
        resource = findResource(name);
        if (resource != null) return resource;
        final var cl = getParent();
        return (cl == null) ? null : cl.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        @SuppressWarnings("unchecked")
        final var resources = (Enumeration<URL>[]) new Enumeration<?>[]{
                Object.class.getClassLoader().getResources(name),
                findResources(name),
                getParent() == null ? null : getParent().getResources(name)};
        return new CompoundEnumeration<>(resources);
    }

    @NonNull
    @Override
    public String toString() {
        return "LspModuleClassLoader[" +
                "module=" + apk + "," +
                "nativeLibraryDirs=" + Arrays.toString(nativeLibraryDirs.toArray()) + "," +
                super.toString() + "]";
    }

    public static InMemoryDelegateLastClassLoader loadApk(File apk,
                                                          String librarySearchPath,
                                                          ClassLoader parent) {
        var byteBuffers = new ArrayList<ByteBuffer>();
        try (var apkFile = new ZipFile(apk)) {
            int secondary = 2;
            for (var dexFile = apkFile.getEntry("classes.dex"); dexFile != null;
                 dexFile = apkFile.getEntry("classes" + secondary + ".dex"), secondary++) {
                try (var in = apkFile.getInputStream(dexFile)) {
                    var byteBuffer = ByteBuffer.allocate(in.available());
                    byteBuffer.mark();
                    Channels.newChannel(in).read(byteBuffer);
                    byteBuffer.reset();
                    byteBuffers.add(byteBuffer);
                } catch (IOException e) {
                    Log.w(TAG, "Can not read " + dexFile + " in " + apk, e);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can not open " + apk, e);
        }
        var dexBuffers = new ByteBuffer[byteBuffers.size()];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new InMemoryDelegateLastClassLoader(byteBuffers.toArray(dexBuffers),
                    librarySearchPath, parent, apk.getAbsolutePath());
        } else {
            var cl = new InMemoryDelegateLastClassLoader(byteBuffers.toArray(dexBuffers),
                    parent, apk.getAbsolutePath());
            cl.initNativeLibraryDirs(librarySearchPath);
            return cl;
        }
    }
}

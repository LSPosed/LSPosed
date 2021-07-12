package org.lsposed.lspd.util;

import static de.robv.android.xposed.XposedBridge.TAG;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipFile;

import hidden.ByteBufferDexClassLoader;

@SuppressWarnings("ConstantConditions")
public final class InMemoryDelegateLastClassLoader extends ByteBufferDexClassLoader {
    private final String apk;

    private InMemoryDelegateLastClassLoader(ByteBuffer[] dexBuffers,
                                            String librarySearchPath,
                                            ClassLoader parent,
                                            String apk) {
        super(dexBuffers, librarySearchPath, parent);
        this.apk = apk;
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

    public static InMemoryDelegateLastClassLoader loadApk(File apk, String librarySearchPath, ClassLoader parent) {
        var byteBuffers = new ArrayList<ByteBuffer>();
        try (var apkFile = new ZipFile(apk)) {
            int secondaryNumber = 2;
            for (var dexFile = apkFile.getEntry("classes.dex"); dexFile != null;
                 dexFile = apkFile.getEntry("classes" + secondaryNumber + ".dex"), secondaryNumber++) {
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
        return new InMemoryDelegateLastClassLoader(byteBuffers.toArray(dexBuffers),
                librarySearchPath, parent, apk.getAbsolutePath());
    }
}

package org.lsposed.lspd.util;

import static de.robv.android.xposed.XposedBridge.TAG;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipFile;

import dalvik.system.DelegateLastClassLoader;
import hidden.ByteBufferDexClassLoader;

public final class InMemoryDelegateLastClassLoader extends ByteBufferDexClassLoader {
    private final DelegateLastClassLoader resources;

    private InMemoryDelegateLastClassLoader(ByteBuffer[] dexBuffers, DelegateLastClassLoader resourcesClassLoader, String librarySearchPath, ClassLoader parent) {
        super(dexBuffers, librarySearchPath, parent);
        resources = resourcesClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        var cl = findLoadedClass(name);
        if (cl != null) {
            return cl;
        }
        try {
            //noinspection ConstantConditions
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
    public URL getResource(String name) {
        return resources.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return resources.getResources(name);
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
        var resources = new DelegateLastClassLoader(apk.getPath(), librarySearchPath, parent);
        return new InMemoryDelegateLastClassLoader(byteBuffers.toArray(dexBuffers), resources, librarySearchPath, parent);
    }
}

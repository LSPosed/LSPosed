package com.swift.sandhook.xposedcompat.classloaders;

/**
 * Created by weishu on 17/11/30.
 */

public class ComposeClassLoader extends ClassLoader {

    private final ClassLoader mAppClassLoader;
    public ComposeClassLoader(ClassLoader parent, ClassLoader appClassLoader) {
        super(parent);
        mAppClassLoader = appClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = null;

        try {
            clazz = mAppClassLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            // IGNORE.
        }
        if (clazz == null) {
            clazz = super.loadClass(name, resolve);
        }

        if (clazz == null) {
            throw new ClassNotFoundException();
        }

        return clazz;
    }
}

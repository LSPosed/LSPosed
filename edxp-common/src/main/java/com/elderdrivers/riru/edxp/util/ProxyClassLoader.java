package com.elderdrivers.riru.edxp.util;

public class ProxyClassLoader extends ClassLoader {

    private final ClassLoader mClassLoader;

    public ProxyClassLoader(ClassLoader parentCL, ClassLoader appCL) {
        super(parentCL);
        mClassLoader = appCL;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = null;

        try {
            clazz = mClassLoader.loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }

        if (clazz == null) {
            clazz = super.loadClass(name, resolve);
            if (clazz == null) {
                throw new ClassNotFoundException();
            }
        }

        return clazz;
    }
}

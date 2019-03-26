package com.elderdrivers.riru.edxp.config;

import com.elderdrivers.riru.edxp.hook.HookProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

public class EdXpConfigGlobal {

    public static volatile EdXpConfig sConfig;
    public static volatile HookProvider sHookProvider;

    public static EdXpConfig getConfig() {
        if (sConfig == null) {
            return defaultConfig;
        }
        return sConfig;
    }

    public static HookProvider getHookProvider() {
        if (sHookProvider == null) {
            return defaultHookProvider;
        }
        return sHookProvider;
    }


    private static final EdXpConfig defaultConfig = new EdXpConfig() {

        @Override
        public String getInstallerBaseDir() {
            return "";
        }

        @Override
        public String getBlackListModulePackageName() {
            return "";
        }

        @Override
        public boolean isDynamicModulesMode() {
            return false;
        }
    };


    private static final HookProvider defaultHookProvider = new HookProvider() {

        @Override
        public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {

        }

        @Override
        public Object invokeOriginalMethod(Member method, long methodId, Object thisObject, Object[] args)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return null;
        }

        @Override
        public Member findMethodNative(Member hookMethod) {
            return hookMethod;
        }

        @Override
        public void deoptMethods(String packageName, ClassLoader classLoader) {

        }

        @Override
        public long getMethodId(Member member) {
            return 0;
        }

        @Override
        public Object findMethodNative(Class clazz, String methodName, String methodSig) {
            return null;
        }

        @Override
        public void deoptMethodNative(Object method) {

        }
    };
}

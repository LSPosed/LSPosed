package com.swift.sandhook;

import android.os.Build;

import com.swift.sandhook.annotation.HookMode;
import com.swift.sandhook.blacklist.HookBlackList;
import com.swift.sandhook.utils.ClassStatusUtils;
import com.swift.sandhook.utils.FileUtils;
import com.swift.sandhook.utils.ReflectionUtils;
import com.swift.sandhook.utils.Unsafe;
import com.swift.sandhook.wrapper.HookErrorException;
import com.swift.sandhook.wrapper.HookWrapper;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SandHook {

    static Map<Member,HookWrapper.HookEntity> globalHookEntityMap = new ConcurrentHashMap<>();
    static Map<Method,HookWrapper.HookEntity> globalBackupMap = new ConcurrentHashMap<>();

    private static HookModeCallBack hookModeCallBack;
    public static void setHookModeCallBack(HookModeCallBack hookModeCallBack) {
        SandHook.hookModeCallBack = hookModeCallBack;
    }

    private static HookResultCallBack hookResultCallBack;
    public static void setHookResultCallBack(HookResultCallBack hookResultCallBack) {
        SandHook.hookResultCallBack = hookResultCallBack;
    }

    public static Class artMethodClass;

    public static Field nativePeerField;
    public static Method testOffsetMethod1;
    public static Method testOffsetMethod2;
    public static Object testOffsetArtMethod1;
    public static Object testOffsetArtMethod2;

    public static int testAccessFlag;

    static {
        SandHookConfig.libLoader.loadLib();
        init();
    }

    private static boolean init() {
        initTestOffset();
        initThreadPeer();
        SandHookMethodResolver.init();
        return initNative(SandHookConfig.SDK_INT, SandHookConfig.DEBUG);
    }

    private static void initThreadPeer() {
        try {
            nativePeerField = getField(Thread.class, "nativePeer");
        } catch (NoSuchFieldException e) {

        }
    }

    public static void addHookClass(Class... hookWrapperClass) throws HookErrorException {
        HookWrapper.addHookClass(hookWrapperClass);
    }

    public static void addHookClass(ClassLoader classLoader, Class... hookWrapperClass) throws HookErrorException {
        HookWrapper.addHookClass(classLoader, hookWrapperClass);
    }

    public static synchronized void hook(HookWrapper.HookEntity entity) throws HookErrorException {

        if (entity == null)
            throw new HookErrorException("null hook entity");

        Member target = entity.target;
        Method hook = entity.hook;
        Method backup = entity.backup;

        if (target == null || hook == null)
            throw new HookErrorException("null input");

        if (globalHookEntityMap.containsKey(entity.target))
            throw new HookErrorException("method <" + entity.target.toString() + "> has been hooked!");

        if (HookBlackList.canNotHook(target))
            throw new HookErrorException("method <" + entity.target.toString() + "> can not hook, because of in blacklist!");


        if (SandHookConfig.delayHook && PendingHookHandler.canWork() && ClassStatusUtils.isStaticAndNoInited(entity.target)) {
            PendingHookHandler.addPendingHook(entity);
            return;
        } else if (entity.initClass) {
            resolveStaticMethod(target);
        }

        resolveStaticMethod(backup);

        if (backup != null && entity.resolveDexCache) {
            SandHookMethodResolver.resolveMethod(hook, backup);
        }
        if (target instanceof Method) {
            ((Method)target).setAccessible(true);
        }

        int mode = HookMode.AUTO;
        if (hookModeCallBack != null) {
            mode = hookModeCallBack.hookMode(target);
        }

        globalHookEntityMap.put(entity.target, entity);

        int res;
        if (mode != HookMode.AUTO) {
            res = hookMethod(target, hook, backup, mode);
        } else {
            HookMode hookMode = hook.getAnnotation(HookMode.class);
            res = hookMethod(target, hook, backup, hookMode == null ? HookMode.AUTO : hookMode.value());
        }

        if (res > 0 && backup != null) {
            backup.setAccessible(true);
        }

        entity.hookMode = res;

        if (hookResultCallBack != null) {
            hookResultCallBack.hookResult(res > 0, entity);
        }

        if (res < 0) {
            globalHookEntityMap.remove(entity.target);
            throw new HookErrorException("hook method <" + entity.target.toString() + "> error in native!");
        }

        if (entity.backup != null) {
            globalBackupMap.put(entity.backup, entity);
        }

        HookLog.d("method <" + entity.target.toString() + "> hook <" + (res == HookMode.INLINE ? "inline" : "replacement") + "> success!");
    }

    public final static Object callOriginMethod(Member originMethod, Object thiz, Object... args) throws Throwable {
        HookWrapper.HookEntity hookEntity = globalHookEntityMap.get(originMethod);
        if (hookEntity == null || hookEntity.backup == null)
            return null;
        return callOriginMethod(hookEntity.backupIsStub, originMethod, hookEntity.backup, thiz, args);
    }

    public final static Object callOriginByBackup(Method backupMethod, Object thiz, Object... args) throws Throwable {
        HookWrapper.HookEntity hookEntity = globalBackupMap.get(backupMethod);
        if (hookEntity == null)
            return null;
        return callOriginMethod(hookEntity.backupIsStub, hookEntity.target, backupMethod, thiz, args);
    }

    public final static Object callOriginMethod(Member originMethod, Method backupMethod, Object thiz, Object[] args) throws Throwable {
        return callOriginMethod(true, originMethod, backupMethod, thiz, args);
    }

    public final static Object callOriginMethod(boolean backupIsStub, Member originMethod, Method backupMethod, Object thiz, Object[] args) throws Throwable {
        //reset declaring class
        if (!backupIsStub && SandHookConfig.SDK_INT >= Build.VERSION_CODES.N) {
            //holder in stack to avoid moving gc
            Class originClassHolder = originMethod.getDeclaringClass();
            ensureDeclareClass(originMethod, backupMethod);
        }
        if (Modifier.isStatic(originMethod.getModifiers())) {
            try {
                return backupMethod.invoke(null, args);
            } catch (InvocationTargetException throwable) {
                if (throwable.getCause() != null) {
                    throw throwable.getCause();
                } else {
                    throw throwable;
                }
            }
        } else {
            try {
                return backupMethod.invoke(thiz, args);
            } catch (InvocationTargetException throwable) {
                if (throwable.getCause() != null) {
                    throw throwable.getCause();
                } else {
                    throw throwable;
                }
            }
        }
    }

    public final static void ensureBackupMethod(Method backupMethod) {
        if (SandHookConfig.SDK_INT < Build.VERSION_CODES.N)
            return;
        HookWrapper.HookEntity entity = globalBackupMap.get(backupMethod);
        if (entity != null) {
            ensureDeclareClass(entity.target, backupMethod);
        }
    }

    public static boolean resolveStaticMethod(Member method) {
        //ignore result, just call to trigger resolve
        if (method == null)
            return true;
        try {
            if (method instanceof Method && Modifier.isStatic(method.getModifiers())) {
                ((Method) method).setAccessible(true);
                ((Method) method).invoke(new Object(), getFakeArgs((Method) method));
            }
        } catch (ExceptionInInitializerError classInitError) {
            //may need hook later
            return false;
        } catch (Throwable throwable) {
        }
        return true;
    }

    private static Object[] getFakeArgs(Method method) {
        Class[] pars = method.getParameterTypes();
        if (pars == null || pars.length == 0) {
            return new Object[]{new Object()};
        } else {
            return null;
        }
    }

    public static Object getObject(long address) {
        if (address == 0) {
            return null;
        }
        long threadSelf = getThreadId();
        return getObjectNative(threadSelf, address);
    }

    public static boolean canGetObjectAddress() {
        return Unsafe.support();
    }

    public static long getObjectAddress(Object object) {
        return Unsafe.getObjectAddress(object);
    }

    private static void initTestOffset() {
        // make test methods sure resolved!
        ArtMethodSizeTest.method1();
        ArtMethodSizeTest.method2();
        // get test methods
        try {
            testOffsetMethod1 = ArtMethodSizeTest.class.getDeclaredMethod("method1");
            testOffsetMethod2 = ArtMethodSizeTest.class.getDeclaredMethod("method2");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("SandHook init error", e);
        }
        initTestAccessFlag();
    }

    private static void initTestAccessFlag() {
        if (hasJavaArtMethod()) {
            try {
                loadArtMethod();
                Field fieldAccessFlags = getField(artMethodClass, "accessFlags");
                testAccessFlag = (int) fieldAccessFlags.get(testOffsetArtMethod1);
            } catch (Exception e) {
            }
        } else {
            try {
                Field fieldAccessFlags = getField(Method.class, "accessFlags");
                testAccessFlag = (int) fieldAccessFlags.get(testOffsetMethod1);
            } catch (Exception e) {
            }
        }
    }

    private static void loadArtMethod() {
        try {
            Field fieldArtMethod = getField(Method.class, "artMethod");
            testOffsetArtMethod1 = fieldArtMethod.get(testOffsetMethod1);
            testOffsetArtMethod2 = fieldArtMethod.get(testOffsetMethod2);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


    public static boolean hasJavaArtMethod() {
        if (SandHookConfig.SDK_INT >= Build.VERSION_CODES.O)
            return false;
        if (artMethodClass != null)
            return true;
        try {
            if (SandHookConfig.initClassLoader == null) {
                artMethodClass = Class.forName("java.lang.reflect.ArtMethod");
            } else {
                artMethodClass = Class.forName("java.lang.reflect.ArtMethod", true, SandHookConfig.initClassLoader);
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Field getField(Class topClass, String fieldName) throws NoSuchFieldException {
        while (topClass != null && topClass != Object.class) {
            try {
                Field field = topClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (Exception e) {
            }
            topClass = topClass.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }

    public static long getThreadId() {
        if (nativePeerField == null)
            return 0;
        try {
            if (nativePeerField.getType() == int.class) {
                return nativePeerField.getInt(Thread.currentThread());
            } else {
                return nativePeerField.getLong(Thread.currentThread());
            }
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    public static Object getJavaMethod(String className, String methodName) {
        if (className == null)
            return null;
        Class clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static long getArtMethod(Member member) {
        return SandHookMethodResolver.getArtMethod(member);
    }

    public static boolean passApiCheck() {
        return ReflectionUtils.passApiCheck();
    }

    //disable JIT/AOT Profile
    public static boolean tryDisableProfile(String selfPackageName) {
        if (SandHookConfig.SDK_INT < Build.VERSION_CODES.N)
            return false;
        try {
            File profile = new File("/data/misc/profiles/cur/" + SandHookConfig.curUser + "/" + selfPackageName + "/primary.prof");
            if (!profile.getParentFile().exists()) return false;
            try {
                profile.delete();
                profile.createNewFile();
            } catch (Throwable throwable) {}
            FileUtils.chmod(profile.getAbsolutePath(), FileUtils.FileMode.MODE_IRUSR);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static native boolean initNative(int sdk, boolean debug);

    public static native void setHookMode(int hookMode);

    //default on!
    public static native void setInlineSafeCheck(boolean check);
    public static native void skipAllSafeCheck(boolean skip);

    private static native int hookMethod(Member originMethod, Method hookMethod, Method backupMethod, int hookMode);

    public static native void ensureMethodCached(Method hook, Method backup);
    public static native void ensureDeclareClass(Member origin, Method backup);

    public static native boolean compileMethod(Member member);
    public static native boolean deCompileMethod(Member member, boolean disableJit);

    public static native boolean canGetObject();
    public static native Object getObjectNative(long thread, long address);

    public static native boolean is64Bit();

    public static native boolean disableVMInline();

    public static native boolean disableDex2oatInline(boolean disableDex2oat);

    public static native boolean setNativeEntry(Member origin, Member hook, long nativeEntry);

    public static native boolean initForPendingHook();

    @FunctionalInterface
    public interface HookModeCallBack {
        int hookMode(Member originMethod);
    }

    @FunctionalInterface
    public interface HookResultCallBack {
        void hookResult(boolean success, HookWrapper.HookEntity hookEntity);
    }

}

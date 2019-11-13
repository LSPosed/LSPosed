package com.elderdrivers.riru.edxp.core.yahfa;

import com.elderdrivers.riru.edxp.art.Heap;
import com.elderdrivers.riru.edxp.core.Yahfa;
import com.elderdrivers.riru.edxp.util.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import de.robv.android.xposed.XposedHelpers;

public class HookMain {

    private static final Set<String> hookItemWhiteList = new HashSet<String>();

    public static void addHookItemWhiteList(String className) {
        hookItemWhiteList.add(className);
    }

    private static List<Object> hookedList = new CopyOnWriteArrayList();

    public static boolean hooked(Member target) {
        return hookedList.contains(target);
    }

    public static void doHookDefault(ClassLoader patchClassLoader, ClassLoader originClassLoader, String hookInfoClassName) {
        try {
            Class<?> hookInfoClass = Class.forName(hookInfoClassName, true, patchClassLoader);
            String[] hookItemNames = (String[]) hookInfoClass.getField("hookItemNames").get(null);
            for (String hookItemName : hookItemNames) {
                doHookItemDefault(patchClassLoader, hookItemName, originClassLoader);
            }
        } catch (Throwable e) {
            Utils.logE("error when hooking all in: " + hookInfoClassName, e);
        }
    }

    private static void doHookItemDefault(ClassLoader patchClassLoader, String hookItemName, ClassLoader originClassLoader) {
        try {
            Utils.logD("Start hooking with item " + hookItemName);
            Class<?> hookItem = Class.forName(hookItemName, true, patchClassLoader);

            String className = (String) hookItem.getField("className").get(null);
            String methodName = (String) hookItem.getField("methodName").get(null);
            String methodSig = (String) hookItem.getField("methodSig").get(null);

            if (className == null || className.equals("")) {
                Utils.logW("No target class. Skipping...");
                return;
            }
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className, true, originClassLoader);
            } catch (ClassNotFoundException cnfe) {
                Utils.logE(className + " not found in " + originClassLoader);
                return;
            }
            if (Modifier.isAbstract(clazz.getModifiers())) {
                Utils.logW("Hook may fail for abstract class: " + className);
            }

            Method hook = null;
            Method backup = null;
            for (Method method : hookItem.getDeclaredMethods()) {
                if (method.getName().equals("hook") && Modifier.isStatic(method.getModifiers())) {
                    hook = method;
                } else if (method.getName().equals("backup") && Modifier.isStatic(method.getModifiers())) {
                    backup = method;
                }
            }
            if (hook == null) {
                Utils.logE("Cannot find hook for " + methodName);
                return;
            }
            findAndBackupAndHook(clazz, methodName, methodSig, hook, backup);
        } catch (Throwable e) {
            if (!hookItemWhiteList.contains(hookItemName)) {
                Utils.logE("error when hooking " + hookItemName, e);
            }
        }
    }

    public static void findAndHook(Class targetClass, String methodName, String methodSig, Method hook) {
        hook(findMethod(targetClass, methodName, methodSig), hook);
    }

    public static void findAndBackupAndHook(Class targetClass, String methodName, String methodSig,
                                            Method hook, Method backup) {
        backupAndHook(findMethod(targetClass, methodName, methodSig), hook, backup);
    }

    public static void hook(Object target, Method hook) {
        backupAndHook(target, hook, null);
    }

    public static void backupAndHook(Object target, Method hook, Method backup) {
        Utils.logD(String.format("target=%s, hook=%s, backup=%s", target, hook, backup));
        if (target == null) {
            throw new IllegalArgumentException("null target method");
        }
        if (hook == null) {
            throw new IllegalArgumentException("null hook method");
        }

        if (!Modifier.isStatic(hook.getModifiers())) {
            throw new IllegalArgumentException("Hook must be a static method: " + hook);
        }
        checkCompatibleMethods(target, hook, "Original", "Hook");
        if (backup != null) {
            if (!Modifier.isStatic(backup.getModifiers())) {
                throw new IllegalArgumentException("Backup must be a static method: " + backup);
            }
            // backup is just a placeholder and the constraint could be less strict
            checkCompatibleMethods(target, backup, "Original", "Backup");
        }
        if (backup != null) {
            HookMethodResolver.resolveMethod(hook, backup);
        }
        // make sure GC completed before hook
        Thread currentThread = Thread.currentThread();
        int lastGcType = Heap.waitForGcToComplete(
                XposedHelpers.getLongField(currentThread, "nativePeer"));
        if (lastGcType < 0) {
            Utils.logW("waitForGcToComplete failed, using fallback");
            Runtime.getRuntime().gc();
        }
        if (!Yahfa.backupAndHookNative(target, hook, backup)) {
            throw new RuntimeException("Failed to hook " + target + " with " + hook);
        } else {
            hookedList.add(target);
        }
    }

    public static Object findMethod(Class cls, String methodName, String methodSig) {
        if (cls == null) {
            throw new IllegalArgumentException("null class");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("null method name");
        }
        if (methodSig == null) {
            throw new IllegalArgumentException("null method signature");
        }
        return Yahfa.findMethodNative(cls, methodName, methodSig);
    }

    private static void checkCompatibleMethods(Object original, Method replacement, String originalName, String replacementName) {
        ArrayList<Class<?>> originalParams;
        if (original instanceof Method) {
            originalParams = new ArrayList<>(Arrays.asList(((Method) original).getParameterTypes()));
        } else if (original instanceof Constructor) {
            originalParams = new ArrayList<>(Arrays.asList(((Constructor<?>) original).getParameterTypes()));
        } else {
            throw new IllegalArgumentException("Type of target method is wrong");
        }

        ArrayList<Class<?>> replacementParams = new ArrayList<>(Arrays.asList(replacement.getParameterTypes()));

        if (original instanceof Method
                && !Modifier.isStatic(((Method) original).getModifiers())) {
            originalParams.add(0, ((Method) original).getDeclaringClass());
        } else if (original instanceof Constructor) {
            originalParams.add(0, ((Constructor<?>) original).getDeclaringClass());
        }


        if (!Modifier.isStatic(replacement.getModifiers())) {
            replacementParams.add(0, replacement.getDeclaringClass());
        }

        if (original instanceof Method
                && !replacement.getReturnType().isAssignableFrom(((Method) original).getReturnType())) {
            throw new IllegalArgumentException("Incompatible return types. " + originalName + ": " + ((Method) original).getReturnType() + ", " + replacementName + ": " + replacement.getReturnType());
        } else if (original instanceof Constructor) {
            if (replacement.getReturnType().equals(Void.class)) {
                throw new IllegalArgumentException("Incompatible return types. " + "<init>" + ": " + "V" + ", " + replacementName + ": " + replacement.getReturnType());
            }
        }

        if (originalParams.size() != replacementParams.size()) {
            throw new IllegalArgumentException("Number of arguments don't match. " + originalName + ": " + originalParams.size() + ", " + replacementName + ": " + replacementParams.size());
        }

        for (int i = 0; i < originalParams.size(); i++) {
            if (!replacementParams.get(i).isAssignableFrom(originalParams.get(i))) {
                throw new IllegalArgumentException("Incompatible argument #" + i + ": " + originalName + ": " + originalParams.get(i) + ", " + replacementName + ": " + replacementParams.get(i));
            }
        }
    }
}

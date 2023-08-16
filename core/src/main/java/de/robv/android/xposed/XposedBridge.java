/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

package de.robv.android.xposed;

import android.app.ActivityThread;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;

import org.lsposed.lspd.impl.LSPosedBridge;
import org.lsposed.lspd.impl.LSPosedHookCallback;
import org.lsposed.lspd.nativebridge.HookBridge;
import org.lsposed.lspd.nativebridge.ResourcesHook;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedInterface;

/**
 * This class contains most of Xposed's central logic, such as initialization and callbacks used by
 * the native side. It also includes methods to add new hooks.
 */
public final class XposedBridge {
    /**
     * The system class loader which can be used to locate Android framework classes.
     * Application classes cannot be retrieved from it.
     *
     * @see ClassLoader#getSystemClassLoader
     */
    public static final ClassLoader BOOTCLASSLOADER = XposedBridge.class.getClassLoader();

    /**
     * @hide
     */
    public static final String TAG = "LSPosed-Bridge";

    /**
     * @deprecated Use {@link #getXposedVersion()} instead.
     */
    @Deprecated
    public static int XPOSED_BRIDGE_VERSION;

    private static final Object[] EMPTY_ARRAY = new Object[0];

    // built-in handlers
    public static final CopyOnWriteArraySet<XC_LoadPackage> sLoadedPackageCallbacks = new CopyOnWriteArraySet<>();
    /*package*/ static final CopyOnWriteArraySet<XC_InitPackageResources> sInitPackageResourcesCallbacks = new CopyOnWriteArraySet<>();

    private XposedBridge() {
    }

    public static volatile ClassLoader dummyClassLoader = null;

    public static void initXResources() {
        if (dummyClassLoader != null) {
            return;
        }
        try {
            Resources res = Resources.getSystem();
            Class<?> resClass = res.getClass();
            Class<?> taClass = TypedArray.class;
            try {
                try {
                    TypedArray ta = res.obtainTypedArray(res.getIdentifier(
                            "preloaded_drawables", "array", "android"));
                    taClass = ta.getClass();
                    ta.recycle();
                } catch (NullPointerException npe) {
                    // For ZUI devices, the creation of TypedArray needs to check the configuration
                    // from ActivityThread.currentActivityThread. However, we do not have a valid
                    // ActivityThread for now and the call will throw an NPE. Luckily they check the
                    // nullability of the result configuration. So we hereby set a dummy
                    // ActivityThread to bypass such a situation.
                    var fake = XposedHelpers.newInstance(ActivityThread.class);
                    XposedHelpers.setStaticObjectField(ActivityThread.class, "sCurrentActivityThread", fake);
                    try {
                        TypedArray ta = res.obtainTypedArray(res.getIdentifier(
                                "preloaded_drawables", "array", "android"));
                        taClass = ta.getClass();
                        ta.recycle();
                    } finally {
                        XposedHelpers.setStaticObjectField(ActivityThread.class, "sCurrentActivityThread", null);
                    }
                }
            } catch (Resources.NotFoundException nfe) {
                XposedBridge.log(nfe);
            }
            ResourcesHook.makeInheritable(resClass);
            ResourcesHook.makeInheritable(taClass);
            ClassLoader myCL = XposedBridge.class.getClassLoader();
            assert myCL != null;
            dummyClassLoader = ResourcesHook.buildDummyClassLoader(myCL.getParent(), resClass.getName(), taClass.getName());
            dummyClassLoader.loadClass("xposed.dummy.XResourcesSuperClass");
            dummyClassLoader.loadClass("xposed.dummy.XTypedArraySuperClass");
            XposedHelpers.setObjectField(myCL, "parent", dummyClassLoader);
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
            XposedInit.disableResources = true;
        }
    }

    /**
     * Returns the currently installed version of the Xposed framework.
     */
    public static int getXposedVersion() {
        return XposedInterface.API;
    }

    /**
     * Writes a message to the Xposed modules log.
     *
     * <p class="warning"><b>DON'T FLOOD THE LOG!!!</b> This is only meant for error logging.
     * If you want to write information/debug messages, use logcat.
     *
     * @param text The log message.
     */
    public synchronized static void log(String text) {
        Log.i(TAG, text);
    }

    /**
     * Logs a stack trace to the Xposed modules log.
     *
     * <p class="warning"><b>DON'T FLOOD THE LOG!!!</b> This is only meant for error logging.
     * If you want to write information/debug messages, use logcat.
     *
     * @param t The Throwable object for the stack trace.
     */
    public synchronized static void log(Throwable t) {
        String logStr = Log.getStackTraceString(t);
        Log.e(TAG, logStr);
    }

    /**
     * Deoptimize a method to avoid callee being inlined.
     *
     * @param deoptimizedMethod The method to deoptmize. Generally it should be a caller of a method
     *                          that is inlined.
     */
    public static void deoptimizeMethod(Member deoptimizedMethod) {
        if (!(deoptimizedMethod instanceof Executable)) {
            throw new IllegalArgumentException("Only methods and constructors can be deoptimized: " + deoptimizedMethod);
        } else if (Modifier.isAbstract(deoptimizedMethod.getModifiers())) {
            throw new IllegalArgumentException("Cannot deoptimize abstract methods: " + deoptimizedMethod);
        } else if (Proxy.isProxyClass(deoptimizedMethod.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot deoptimize methods from proxy class: " + deoptimizedMethod);
        }
        HookBridge.deoptimizeMethod((Executable) deoptimizedMethod);
    }

    /**
     * Hook any method (or constructor) with the specified callback. See below for some wrappers
     * that make it easier to find a method/constructor in one step.
     *
     * @param hookMethod The method to be hooked.
     * @param callback   The callback to be executed when the hooked method is called.
     * @return An object that can be used to remove the hook.
     * @see XposedHelpers#findAndHookMethod(String, ClassLoader, String, Object...)
     * @see XposedHelpers#findAndHookMethod(Class, String, Object...)
     * @see #hookAllMethods
     * @see XposedHelpers#findAndHookConstructor(String, ClassLoader, Object...)
     * @see XposedHelpers#findAndHookConstructor(Class, Object...)
     * @see #hookAllConstructors
     */
    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        if (!(hookMethod instanceof Executable)) {
            throw new IllegalArgumentException("Only methods and constructors can be hooked: " + hookMethod);
        } else if (Modifier.isAbstract(hookMethod.getModifiers())) {
            throw new IllegalArgumentException("Cannot hook abstract methods: " + hookMethod);
        } else if (hookMethod.getDeclaringClass().getClassLoader() == XposedBridge.class.getClassLoader()) {
            throw new IllegalArgumentException("Do not allow hooking inner methods");
        } else if (hookMethod.getDeclaringClass() == Method.class && hookMethod.getName().equals("invoke")) {
            throw new IllegalArgumentException("Cannot hook Method.invoke");
        }

        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null!");
        }

        if (!HookBridge.hookMethod(false, (Executable) hookMethod, LSPosedBridge.NativeHooker.class, callback.priority, callback)) {
            log("Failed to hook " + hookMethod);
            return null;
        }

        return callback.new Unhook(hookMethod);
    }

    /**
     * Removes the callback for a hooked method/constructor.
     *
     * @param hookMethod The method for which the callback should be removed.
     * @param callback   The reference to the callback as specified in {@link #hookMethod}.
     * @deprecated Use {@link XC_MethodHook.Unhook#unhook} instead. An instance of the {@code Unhook}
     * class is returned when you hook the method.
     */
    @Deprecated
    public static void unhookMethod(Member hookMethod, XC_MethodHook callback) {
        if (hookMethod instanceof Executable) {
            HookBridge.unhookMethod(false, (Executable) hookMethod, callback);
        }
    }

    /**
     * Hooks all methods with a certain name that were declared in the specified class. Inherited
     * methods and constructors are not considered. For constructors, use
     * {@link #hookAllConstructors} instead.
     *
     * @param hookClass  The class to check for declared methods.
     * @param methodName The name of the method(s) to hook.
     * @param callback   The callback to be executed when the hooked methods are called.
     * @return A set containing one object for each found method which can be used to unhook it.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member method : hookClass.getDeclaredMethods())
            if (method.getName().equals(methodName))
                unhooks.add(hookMethod(method, callback));
        return unhooks;
    }

    /**
     * Hook all constructors of the specified class.
     *
     * @param hookClass The class to check for constructors.
     * @param callback  The callback to be executed when the hooked constructors are called.
     * @return A set containing one object for each found constructor which can be used to unhook it.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member constructor : hookClass.getDeclaredConstructors())
            unhooks.add(hookMethod(constructor, callback));
        return unhooks;
    }

    /**
     * Adds a callback to be executed when an app ("Android package") is loaded.
     *
     * <p class="note">You probably don't need to call this. Simply implement {@link IXposedHookLoadPackage}
     * in your module class and Xposed will take care of registering it as a callback.
     *
     * @param callback The callback to be executed.
     * @hide
     */
    public static void hookLoadPackage(XC_LoadPackage callback) {
        synchronized (sLoadedPackageCallbacks) {
            sLoadedPackageCallbacks.add(callback);
        }
    }

    /**
     * Adds a callback to be executed when the resources for an app are initialized.
     *
     * <p class="note">You probably don't need to call this. Simply implement {@link IXposedHookInitPackageResources}
     * in your module class and Xposed will take care of registering it as a callback.
     *
     * @param callback The callback to be executed.
     * @hide
     */
    public static void hookInitPackageResources(XC_InitPackageResources callback) {
        synchronized (sInitPackageResourcesCallbacks) {
            sInitPackageResourcesCallbacks.add(callback);
        }
    }

    /**
     * Basically the same as {@link Method#invoke}, but calls the original method
     * as it was before the interception by Xposed. Also, access permissions are not checked.
     *
     * <p class="caution">There are very few cases where this method is needed. A common mistake is
     * to replace a method and then invoke the original one based on dynamic conditions. This
     * creates overhead and skips further hooks by other modules. Instead, just hook (don't replace)
     * the method and call {@code param.setResult(null)} in {@link XC_MethodHook#beforeHookedMethod}
     * if the original method should be skipped.
     *
     * @param method     The method to be called.
     * @param thisObject For non-static calls, the "this" pointer, otherwise {@code null}.
     * @param args       Arguments for the method call as Object[] array.
     * @return The result returned from the invoked method.
     * @throws NullPointerException      if {@code receiver == null} for a non-static method
     * @throws IllegalAccessException    if this method is not accessible (see {@link AccessibleObject})
     * @throws IllegalArgumentException  if the number of arguments doesn't match the number of parameters, the receiver
     *                                   is incompatible with the declaring class, or an argument could not be unboxed
     *                                   or converted by a widening conversion to the corresponding parameter type
     * @throws InvocationTargetException if an exception was thrown by the invoked method
     */
    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws Throwable {
        if (args == null) {
            args = EMPTY_ARRAY;
        }

        if (!(method instanceof Executable)) {
            throw new IllegalArgumentException("method must be of type Method or Constructor");
        }

        return HookBridge.invokeOriginalMethod((Executable) method, thisObject, args);
    }

    /**
     * @hide
     */
    public static final class CopyOnWriteSortedSet<E> {
        private transient volatile Object[] elements = EMPTY_ARRAY;

        @SuppressWarnings("UnusedReturnValue")
        public synchronized boolean add(E e) {
            int index = indexOf(e);
            if (index >= 0)
                return false;

            Object[] newElements = new Object[elements.length + 1];
            System.arraycopy(elements, 0, newElements, 0, elements.length);
            newElements[elements.length] = e;
            Arrays.sort(newElements);
            elements = newElements;
            return true;
        }

        @SuppressWarnings("UnusedReturnValue")
        public synchronized boolean remove(E e) {
            int index = indexOf(e);
            if (index == -1)
                return false;

            Object[] newElements = new Object[elements.length - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index, elements.length - index - 1);
            elements = newElements;
            return true;
        }

        private int indexOf(Object o) {
            for (int i = 0; i < elements.length; i++) {
                if (o.equals(elements[i]))
                    return i;
            }
            return -1;
        }

        public Object[] getSnapshot() {
            return elements;
        }

        public <T> T[] getSnapshot(T[] a) {
            var snapshot = getSnapshot();
            return (T[]) Arrays.copyOf(snapshot, snapshot.length, a.getClass());
        }

        public synchronized void clear() {
            elements = EMPTY_ARRAY;
        }
    }

    public static class LegacyApiSupport<T extends Executable> {
        private final XC_MethodHook.MethodHookParam<T> param;
        private final LSPosedHookCallback<T> callback;
        private final Object[] snapshot;

        private int beforeIdx;

        public LegacyApiSupport(LSPosedHookCallback<T> callback, Object[] legacySnapshot) {
            this.param = new XC_MethodHook.MethodHookParam<>();
            this.callback = callback;
            this.snapshot = legacySnapshot;
        }

        public void handleBefore() {
            syncronizeApi(param, callback, true);
            for (beforeIdx = 0; beforeIdx < snapshot.length; beforeIdx++) {
                try {
                    var cb = (XC_MethodHook) snapshot[beforeIdx];
                    cb.beforeHookedMethod(param);
                } catch (Throwable t) {
                    XposedBridge.log(t);

                    // reset result (ignoring what the unexpectedly exiting callback did)
                    param.setResult(null);
                    param.returnEarly = false;
                    continue;
                }

                if (param.returnEarly) {
                    // skip remaining "before" callbacks and corresponding "after" callbacks
                    beforeIdx++;
                    break;
                }
            }
            syncronizeApi(param, callback, false);
        }

        public void handleAfter() {
            syncronizeApi(param, callback, true);
            for (int afterIdx = beforeIdx - 1; afterIdx >= 0; afterIdx--) {
                Object lastResult = param.getResult();
                Throwable lastThrowable = param.getThrowable();
                try {
                    var cb = (XC_MethodHook) snapshot[afterIdx];
                    cb.afterHookedMethod(param);
                } catch (Throwable t) {
                    XposedBridge.log(t);

                    // reset to last result (ignoring what the unexpectedly exiting callback did)
                    if (lastThrowable == null) {
                        param.setResult(lastResult);
                    } else {
                        param.setThrowable(lastThrowable);
                    }
                }
            }
            syncronizeApi(param, callback, false);
        }

        private void syncronizeApi(XC_MethodHook.MethodHookParam<T> param, LSPosedHookCallback<T> callback, boolean forward) {
            if (forward) {
                param.method = callback.method;
                param.thisObject = callback.thisObject;
                param.args = callback.args;
                param.result = callback.result;
                param.throwable = callback.throwable;
                param.returnEarly = callback.isSkipped;
            } else {
                callback.method = param.method;
                callback.thisObject = param.thisObject;
                callback.args = param.args;
                callback.result = param.result;
                callback.throwable = param.throwable;
                callback.isSkipped = param.returnEarly;
            }
        }
    }
}

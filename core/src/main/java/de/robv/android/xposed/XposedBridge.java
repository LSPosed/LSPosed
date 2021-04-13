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
 * Copyright (C) 2021 LSPosed Contributors
 */

package de.robv.android.xposed;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;

import org.lsposed.lspd.BuildConfig;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_InitZygote;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.lsposed.lspd.nativebridge.ModuleLogger;
import org.lsposed.lspd.nativebridge.ResourcesHook;
import org.lsposed.lspd.yahfa.dexmaker.DynamicBridge;
import org.lsposed.lspd.yahfa.hooker.YahfaHooker;

import static de.robv.android.xposed.XposedHelpers.setObjectField;

/**
 * This class contains most of Xposed's central logic, such as initialization and callbacks used by
 * the native side. It also includes methods to add new hooks.
 */
@SuppressWarnings("JniMissingFunction")
public final class XposedBridge {
	/**
	 * The system class loader which can be used to locate Android framework classes.
	 * Application classes cannot be retrieved from it.
	 *
	 * @see ClassLoader#getSystemClassLoader
	 */
	public static final ClassLoader BOOTCLASSLOADER = XposedBridge.class.getClassLoader();

	/** @hide */
	public static final String TAG = "LSPosed-Bridge";

	/** @deprecated Use {@link #getXposedVersion()} instead. */
	@Deprecated
	public static int XPOSED_BRIDGE_VERSION;

	/*package*/ static boolean isZygote = true; // ed: RuntimeInit.main() tool process not supported yet

	// This field is set "magically" on MIUI.
	/*package*/ static long BOOT_START_TIME;

	private static final Object[] EMPTY_ARRAY = new Object[0];

	// built-in handlers
	private static final Map<Member, CopyOnWriteSortedSet<XC_MethodHook>> sHookedMethodCallbacks = new NoValuesHashMap<>();
	public static final CopyOnWriteSortedSet<XC_LoadPackage> sLoadedPackageCallbacks = new CopyOnWriteSortedSet<>();
	/*package*/ static final CopyOnWriteSortedSet<XC_InitPackageResources> sInitPackageResourcesCallbacks = new CopyOnWriteSortedSet<>();
	/*package*/ static final CopyOnWriteSortedSet<XC_InitZygote> sInitZygoteCallbacks = new CopyOnWriteSortedSet<>();

	private XposedBridge() {}

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
				TypedArray ta = res.obtainTypedArray(res.getIdentifier(
						"preloaded_drawables", "array", "android"));
				taClass = ta.getClass();
				ta.recycle();
			} catch (Resources.NotFoundException nfe) {
				XposedBridge.log(nfe);
			}
			ResourcesHook.removeFinalFlagNative(resClass);
			ResourcesHook.removeFinalFlagNative(taClass);
			ClassLoader myCL = XposedBridge.class.getClassLoader();
			dummyClassLoader = ResourcesHook.buildDummyClassLoader(myCL.getParent(), resClass, taClass);
			dummyClassLoader.loadClass("xposed.dummy.XResourcesSuperClass");
			dummyClassLoader.loadClass("xposed.dummy.XTypedArraySuperClass");
			setObjectField(myCL, "parent", dummyClassLoader);
		} catch (Throwable throwable) {
			XposedBridge.log(throwable);
			XposedInit.disableResources = true;
		}
	}

	/**
	 * Returns the currently installed version of the Xposed framework.
	 */
	public static int getXposedVersion() {
		return BuildConfig.API_CODE;
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
		ModuleLogger.log(text, false);
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
		ModuleLogger.log(logStr, true);
	}

	/**
	 * Hook any method (or constructor) with the specified callback. See below for some wrappers
	 * that make it easier to find a method/constructor in one step.
	 *
	 * @param hookMethod The method to be hooked.
	 * @param callback The callback to be executed when the hooked method is called.
	 * @return An object that can be used to remove the hook.
	 *
	 * @see XposedHelpers#findAndHookMethod(String, ClassLoader, String, Object...)
	 * @see XposedHelpers#findAndHookMethod(Class, String, Object...)
	 * @see #hookAllMethods
	 * @see XposedHelpers#findAndHookConstructor(String, ClassLoader, Object...)
	 * @see XposedHelpers#findAndHookConstructor(Class, Object...)
	 * @see #hookAllConstructors
	 */
	public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
		if (!(hookMethod instanceof Executable)) {
			throw new IllegalArgumentException("Only methods and constructors can be hooked: " + hookMethod.toString());
		}
		// No check interface because there may be default methods
 		/*else if (hookMethod.getDeclaringClass().isInterface()) {
			throw new IllegalArgumentException("Cannot hook interfaces: " + hookMethod.toString());
		}*/
		else if (Modifier.isAbstract(hookMethod.getModifiers())) {
			throw new IllegalArgumentException("Cannot hook abstract methods: " + hookMethod.toString());
		}

		Executable targetMethod = (Executable) hookMethod;

		if (callback == null) {
			throw new IllegalArgumentException("callback should not be null!");
		}

		boolean newMethod = false;
		CopyOnWriteSortedSet<XC_MethodHook> callbacks;
		synchronized (sHookedMethodCallbacks) {
			callbacks = sHookedMethodCallbacks.get(targetMethod);
			if (callbacks == null) {
				callbacks = new CopyOnWriteSortedSet<>();
				sHookedMethodCallbacks.put(targetMethod, callbacks);
				newMethod = true;
			}
		}
		callbacks.add(callback);

		if (newMethod) {
			AdditionalHookInfo additionalInfo = new AdditionalHookInfo(callbacks);
            if (!YahfaHooker.shouldDelayHook(targetMethod)) {
				YahfaHooker.hookMethod(targetMethod, additionalInfo);
			} else {
				PendingHooks.recordPendingMethod((Method)hookMethod, additionalInfo);
			}
        }

        return callback.new Unhook(hookMethod);
	}

	/**
	 * Removes the callback for a hooked method/constructor.
	 *
	 * @deprecated Use {@link XC_MethodHook.Unhook#unhook} instead. An instance of the {@code Unhook}
	 * class is returned when you hook the method.
	 *
	 * @param hookMethod The method for which the callback should be removed.
	 * @param callback The reference to the callback as specified in {@link #hookMethod}.
	 */
	@Deprecated
	public static void unhookMethod(Member hookMethod, XC_MethodHook callback) {
		CopyOnWriteSortedSet<XC_MethodHook> callbacks;
		synchronized (sHookedMethodCallbacks) {
			callbacks = sHookedMethodCallbacks.get(hookMethod);
			if (callbacks == null)
				return;
		}
		callbacks.remove(callback);
	}

	/**
	 * Hooks all methods with a certain name that were declared in the specified class. Inherited
	 * methods and constructors are not considered. For constructors, use
	 * {@link #hookAllConstructors} instead.
	 *
	 * @param hookClass The class to check for declared methods.
	 * @param methodName The name of the method(s) to hook.
	 * @param callback The callback to be executed when the hooked methods are called.
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
	 * @param callback The callback to be executed when the hooked constructors are called.
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

	public static void clearLoadedPackages() {
		synchronized (sLoadedPackageCallbacks) {
			sLoadedPackageCallbacks.clear();
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

	public static void hookInitZygote(XC_InitZygote callback) {
		synchronized (sInitZygoteCallbacks) {
			sInitZygoteCallbacks.add(callback);
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
	 * @param method The method to be called.
	 * @param thisObject For non-static calls, the "this" pointer, otherwise {@code null}.
	 * @param args Arguments for the method call as Object[] array.
	 * @return The result returned from the invoked method.
	 * @throws NullPointerException
	 *             if {@code receiver == null} for a non-static method
	 * @throws IllegalAccessException
	 *             if this method is not accessible (see {@link AccessibleObject})
	 * @throws IllegalArgumentException
	 *             if the number of arguments doesn't match the number of parameters, the receiver
	 *             is incompatible with the declaring class, or an argument could not be unboxed
	 *             or converted by a widening conversion to the corresponding parameter type
	 * @throws InvocationTargetException
	 *             if an exception was thrown by the invoked method
	 */
	public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
			throws Throwable {
		if (args == null) {
			args = EMPTY_ARRAY;
		}

		if (!(method instanceof Executable)) {
			throw new IllegalArgumentException("method must be of type Method or Constructor");
		}

		return YahfaHooker.invokeOriginalMethod((Executable) method, thisObject, args);
	}

	private static class NoValuesHashMap<K,V> extends HashMap<K,V> {
		@Override
		public Collection values() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public void clear() {

		}

		@Override
		public Set<K> keySet() {
			return Collections.EMPTY_SET;
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return Collections.EMPTY_SET;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}
	}

	/** @hide */
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

		public synchronized void clear() {
			elements = EMPTY_ARRAY;
		}
	}

	public static class AdditionalHookInfo {
		public final CopyOnWriteSortedSet<XC_MethodHook> callbacks;

		private AdditionalHookInfo(CopyOnWriteSortedSet<XC_MethodHook> callbacks) {
			this.callbacks = callbacks;
		}
	}
}

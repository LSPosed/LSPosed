package de.robv.android.xposed;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;

import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dalvik.system.InMemoryDexClassLoader;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_InitZygote;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;
import external.com.android.dx.DexMaker;
import external.com.android.dx.TypeId;

import static de.robv.android.xposed.XposedHelpers.getIntField;
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
	public static final String TAG = "EdXposed-Bridge";

	/** @deprecated Use {@link #getXposedVersion()} instead. */
	@Deprecated
	public static int XPOSED_BRIDGE_VERSION;

	/*package*/ static boolean isZygote = true; // ed: RuntimeInit.main() tool process not supported yet

	private static int runtime = 2; // ed: only support art
	private static final int RUNTIME_DALVIK = 1;
	private static final int RUNTIME_ART = 2;

	public static boolean disableHooks = false;

	// This field is set "magically" on MIUI.
	/*package*/ static long BOOT_START_TIME;

	private static final Object[] EMPTY_ARRAY = new Object[0];

	// built-in handlers
	public static final Map<Member, CopyOnWriteSortedSet<XC_MethodHook>> sHookedMethodCallbacks = new HashMap<>();
	public static final CopyOnWriteSortedSet<XC_LoadPackage> sLoadedPackageCallbacks = new CopyOnWriteSortedSet<>();
	/*package*/ static final CopyOnWriteSortedSet<XC_InitPackageResources> sInitPackageResourcesCallbacks = new CopyOnWriteSortedSet<>();
	/*package*/ static final CopyOnWriteSortedSet<XC_InitZygote> sInitZygoteCallbacks = new CopyOnWriteSortedSet<>();

	private XposedBridge() {}

	/**
	 * Called when native methods and other things are initialized, but before preloading classes etc.
	 * @hide
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		// ed: moved
	}

	/** @hide */
//	protected static final class ToolEntryPoint {
//		protected static void main(String[] args) {
//			isZygote = false;
//			XposedBridge.main(args);
//		}
//	}

	public static volatile ClassLoader dummyClassLoader = null;

	public static void initXResources() {
        if (dummyClassLoader != null) {
        	return;
		}
		try {
			Resources res = Resources.getSystem();
			Class resClass = res.getClass();
			Class taClass = TypedArray.class;
			try {
				TypedArray ta = res.obtainTypedArray(res.getIdentifier(
						"preloaded_drawables", "array", "android"));
				taClass = ta.getClass();
				ta.recycle();
			} catch (Resources.NotFoundException nfe) {
				XposedBridge.log(nfe);
			}
			XposedBridge.removeFinalFlagNative(resClass);
			XposedBridge.removeFinalFlagNative(taClass);
			DexMaker dexMaker = new DexMaker();
			dexMaker.declare(TypeId.get("Lxposed/dummy/XResourcesSuperClass;"),
					"XResourcesSuperClass.java",
					Modifier.PUBLIC, TypeId.get(resClass));
			dexMaker.declare(TypeId.get("Lxposed/dummy/XTypedArraySuperClass;"),
					"XTypedArraySuperClass.java",
					Modifier.PUBLIC, TypeId.get(taClass));
			ClassLoader myCL = XposedBridge.class.getClassLoader();
			dummyClassLoader = new InMemoryDexClassLoader(
					ByteBuffer.wrap(dexMaker.generate()), myCL.getParent());
			dummyClassLoader.loadClass("xposed.dummy.XResourcesSuperClass");
			dummyClassLoader.loadClass("xposed.dummy.XTypedArraySuperClass");
			setObjectField(myCL, "parent", dummyClassLoader);
		} catch (Throwable throwable) {
			XposedBridge.log(throwable);
			XposedInit.disableResources = true;
		}
	}

//	private static boolean hadInitErrors() {
//		// ed: assuming never had errors
//		return false;
//	}
//	private static native int getRuntime();
//	/*package*/ static native boolean startsSystemServer();
//	/*package*/ static native String getStartClassName();
//	/*package*/ native static boolean initXResourcesNative();

	/**
	 * Returns the currently installed version of the Xposed framework.
	 */
	public static int getXposedVersion() {
		// ed: fixed value for now
		return 91;
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
		if (EdXpConfigGlobal.getConfig().isNoModuleLogEnabled()) {
			return;
		}
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
		Log.e(TAG, Log.getStackTraceString(t));
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
		if (!(hookMethod instanceof Method) && !(hookMethod instanceof Constructor<?>)) {
			throw new IllegalArgumentException("Only methods and constructors can be hooked: " + hookMethod.toString());
		} else if (hookMethod.getDeclaringClass().isInterface()) {
			throw new IllegalArgumentException("Cannot hook interfaces: " + hookMethod.toString());
		} else if (Modifier.isAbstract(hookMethod.getModifiers())) {
			throw new IllegalArgumentException("Cannot hook abstract methods: " + hookMethod.toString());
		}

		if (callback == null) {
			throw new IllegalArgumentException("callback should not be null!");
		}

		boolean newMethod = false;
		CopyOnWriteSortedSet<XC_MethodHook> callbacks;
		synchronized (sHookedMethodCallbacks) {
			callbacks = sHookedMethodCallbacks.get(hookMethod);
			if (callbacks == null) {
				callbacks = new CopyOnWriteSortedSet<>();
				sHookedMethodCallbacks.put(hookMethod, callbacks);
				newMethod = true;
			}
		}
		callbacks.add(callback);

		if (newMethod) {
			Class<?> declaringClass = hookMethod.getDeclaringClass();
			int slot;
			Class<?>[] parameterTypes;
			Class<?> returnType;
			if (runtime == RUNTIME_ART) {
				slot = 0;
				parameterTypes = null;
				returnType = null;
			} else if (hookMethod instanceof Method) {
				slot = getIntField(hookMethod, "slot");
				parameterTypes = ((Method) hookMethod).getParameterTypes();
				returnType = ((Method) hookMethod).getReturnType();
			} else {
				slot = getIntField(hookMethod, "slot");
				parameterTypes = ((Constructor<?>) hookMethod).getParameterTypes();
				returnType = null;
			}

            AdditionalHookInfo additionalInfo = new AdditionalHookInfo(callbacks, parameterTypes, returnType);
            Member reflectMethod = EdXpConfigGlobal.getHookProvider().findMethodNative(hookMethod);
            if (reflectMethod != null) {
				hookMethodNative(reflectMethod, declaringClass, slot, additionalInfo);
			} else {
				PendingHooks.recordPendingMethod(hookMethod, additionalInfo);
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
		EdXpConfigGlobal.getHookProvider().unhookMethod(hookMethod);
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
	 * This method is called as a replacement for hooked methods.
	 */
	public static Object handleHookedMethod(Member method, long originalMethodId, Object additionalInfoObj,
			Object thisObject, Object[] args) throws Throwable {
		AdditionalHookInfo additionalInfo = (AdditionalHookInfo) additionalInfoObj;

		if (disableHooks) {
			try {
				return invokeOriginalMethodNative(method, originalMethodId, additionalInfo.parameterTypes,
						additionalInfo.returnType, thisObject, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}

		Object[] callbacksSnapshot = additionalInfo.callbacks.getSnapshot();
		final int callbacksLength = callbacksSnapshot.length;
		if (callbacksLength == 0) {
			try {
				return invokeOriginalMethodNative(method, originalMethodId, additionalInfo.parameterTypes,
						additionalInfo.returnType, thisObject, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}

		MethodHookParam param = new MethodHookParam();
		param.method = method;
		param.thisObject = thisObject;
		param.args = args;

		// call "before method" callbacks
		int beforeIdx = 0;
		do {
			try {
				((XC_MethodHook) callbacksSnapshot[beforeIdx]).beforeHookedMethod(param);
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
		} while (++beforeIdx < callbacksLength);

		// call original method if not requested otherwise
		if (!param.returnEarly) {
			try {
				param.setResult(invokeOriginalMethodNative(method, originalMethodId,
						additionalInfo.parameterTypes, additionalInfo.returnType, param.thisObject, param.args));
			} catch (InvocationTargetException e) {
				param.setThrowable(e.getCause());
			}
		}

		// call "after method" callbacks
		int afterIdx = beforeIdx - 1;
		do {
			Object lastResult =  param.getResult();
			Throwable lastThrowable = param.getThrowable();

			try {
				((XC_MethodHook) callbacksSnapshot[afterIdx]).afterHookedMethod(param);
			} catch (Throwable t) {
				XposedBridge.log(t);

				// reset to last result (ignoring what the unexpectedly exiting callback did)
				if (lastThrowable == null)
					param.setResult(lastResult);
				else
					param.setThrowable(lastThrowable);
			}
		} while (--afterIdx >= 0);

		// return
		if (param.hasThrowable())
			throw param.getThrowable();
		else
			return param.getResult();
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

	public static void clearInitPackageResources() {
		synchronized (sInitPackageResourcesCallbacks) {
			sInitPackageResourcesCallbacks.clear();
		}
	}

	public static void hookInitZygote(XC_InitZygote callback) {
		synchronized (sInitZygoteCallbacks) {
			sInitZygoteCallbacks.add(callback);
		}
	}

	public static void clearInitZygotes() {
		synchronized (sInitZygoteCallbacks) {
			sInitZygoteCallbacks.clear();
		}
	}

	public static void callInitZygotes() {
		XCallback.callAll(new IXposedHookZygoteInit.StartupParam(sInitZygoteCallbacks));
	}

	public static void clearAllCallbacks() {
		clearLoadedPackages();
		clearInitPackageResources();
		clearInitZygotes();
	}

	/**
	 * Intercept every call to the specified method and call a handler function instead.
	 * @param method The method to intercept
	 */
	/*package*/ synchronized static void hookMethodNative(final Member method, Class<?> declaringClass,
                                                      int slot, final Object additionalInfoObj) {
		EdXpConfigGlobal.getHookProvider().hookMethod(method, (AdditionalHookInfo) additionalInfoObj);
	}

    private static Object invokeOriginalMethodNative(Member method, long methodId,
                                                     Class<?>[] parameterTypes,
                                                     Class<?> returnType,
                                                     Object thisObject, Object[] args)
            throws Throwable {
        return EdXpConfigGlobal.getHookProvider().invokeOriginalMethod(method, methodId, thisObject, args);
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

		Class<?>[] parameterTypes;
		Class<?> returnType;
		if (runtime == RUNTIME_ART && (method instanceof Method || method instanceof Constructor)) {
			parameterTypes = null;
			returnType = null;
		} else if (method instanceof Method) {
			parameterTypes = ((Method) method).getParameterTypes();
			returnType = ((Method) method).getReturnType();
		} else if (method instanceof Constructor) {
			parameterTypes = ((Constructor<?>) method).getParameterTypes();
			returnType = null;
		} else {
			throw new IllegalArgumentException("method must be of type Method or Constructor");
		}

		long methodId = EdXpConfigGlobal.getHookProvider().getMethodId(method);
		return invokeOriginalMethodNative(method, methodId, parameterTypes, returnType, thisObject, args);
	}

	/*package*/ static void setObjectClass(Object obj, Class<?> clazz) {
		if (clazz.isAssignableFrom(obj.getClass())) {
			throw new IllegalArgumentException("Cannot transfer object from " + obj.getClass() + " to " + clazz);
		}
		setObjectClassNative(obj, clazz);
	}

	private static native void setObjectClassNative(Object obj, Class<?> clazz);
	/*package*/ static native void dumpObjectNative(Object obj);

	/*package*/ static Object cloneToSubclass(Object obj, Class<?> targetClazz) {
		if (obj == null)
			return null;

		if (!obj.getClass().isAssignableFrom(targetClazz))
			throw new ClassCastException(targetClazz + " doesn't extend " + obj.getClass());

		return cloneToSubclassNative(obj, targetClazz);
	}

	private static native Object cloneToSubclassNative(Object obj, Class<?> targetClazz);

	private static void removeFinalFlagNative(Class clazz) {
		EdXpConfigGlobal.getHookProvider().removeFinalFlagNative(clazz);
	}

//	/*package*/ static native void closeFilesBeforeForkNative();
//	/*package*/ static native void reopenFilesAfterForkNative();
//
//	/*package*/ static native void invalidateCallersNative(Member[] methods);

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
		public final Class<?>[] parameterTypes;
		public final Class<?> returnType;

		private AdditionalHookInfo(CopyOnWriteSortedSet<XC_MethodHook> callbacks, Class<?>[] parameterTypes, Class<?> returnType) {
			this.callbacks = callbacks;
			this.parameterTypes = parameterTypes;
			this.returnType = returnType;
		}
	}
}

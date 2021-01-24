package de.robv.android.xposed;

import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;
import external.org.apache.commons.lang3.ClassUtils;
import external.org.apache.commons.lang3.reflect.MemberUtils;

/**
 * Helpers that simplify hooking and calling methods/constructors, getting and settings fields, ...
 */
public final class XposedHelpers {
	private XposedHelpers() {}

	private static final HashMap<String, Field> fieldCache = new HashMap<>();
	private static final HashMap<String, Method> methodCache = new HashMap<>();
	private static final HashMap<String, Constructor<?>> constructorCache = new HashMap<>();
	private static final WeakHashMap<Object, HashMap<String, Object>> additionalFields = new WeakHashMap<>();
	private static final HashMap<String, ThreadLocal<AtomicInteger>> sMethodDepth = new HashMap<>();

	/**
	 * Look up a class with the specified class loader.
	 *
	 * <p>There are various allowed syntaxes for the class name, but it's recommended to use one of
	 * these:
	 * <ul>
	 *   <li>{@code java.lang.String}
	 *   <li>{@code java.lang.String[]} (array)
	 *   <li>{@code android.app.ActivityThread.ResourcesKey}
	 *   <li>{@code android.app.ActivityThread$ResourcesKey}
	 * </ul>
	 *
	 * @param className The class name in one of the formats mentioned above.
	 * @param classLoader The class loader, or {@code null} for the boot class loader.
	 * @return A reference to the class.
	 * @throws ClassNotFoundError In case the class was not found.
	 */
	public static Class<?> findClass(String className, ClassLoader classLoader) {
		if (classLoader == null)
			classLoader = XposedBridge.BOOTCLASSLOADER;
		try {
			return ClassUtils.getClass(classLoader, className, false);
		} catch (ClassNotFoundException e) {
			throw new ClassNotFoundError(e);
		}
	}

	/**
	 * Look up and return a class if it exists.
	 * Like {@link #findClass}, but doesn't throw an exception if the class doesn't exist.
	 *
	 * @param className The class name.
	 * @param classLoader The class loader, or {@code null} for the boot class loader.
	 * @return A reference to the class, or {@code null} if it doesn't exist.
	 */
	public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
		try {
			return findClass(className, classLoader);
		} catch (ClassNotFoundError e) {
			return null;
		}
	}

	/**
	 * Look up a field in a class and set it to accessible.
	 *
	 * @param clazz The class which either declares or inherits the field.
	 * @param fieldName The field name.
	 * @return A reference to the field.
	 * @throws NoSuchFieldError In case the field was not found.
	 */
	public static Field findField(Class<?> clazz, String fieldName) {
		String fullFieldName = clazz.getName() + '#' + fieldName;

		if (fieldCache.containsKey(fullFieldName)) {
			Field field = fieldCache.get(fullFieldName);
			if (field == null)
				throw new NoSuchFieldError(fullFieldName);
			return field;
		}

		try {
			Field field = findFieldRecursiveImpl(clazz, fieldName);
			field.setAccessible(true);
			fieldCache.put(fullFieldName, field);
			return field;
		} catch (NoSuchFieldException e) {
			fieldCache.put(fullFieldName, null);
			throw new NoSuchFieldError(fullFieldName);
		}
	}

	/**
	 * Look up and return a field if it exists.
	 * Like {@link #findField}, but doesn't throw an exception if the field doesn't exist.
	 *
	 * @param clazz The class which either declares or inherits the field.
	 * @param fieldName The field name.
	 * @return A reference to the field, or {@code null} if it doesn't exist.
	 */
	public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName);
		} catch (NoSuchFieldError e) {
			return null;
		}
	}

	private static Field findFieldRecursiveImpl(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			while (true) {
				clazz = clazz.getSuperclass();
				if (clazz == null || clazz.equals(Object.class))
					break;

				try {
					return clazz.getDeclaredField(fieldName);
				} catch (NoSuchFieldException ignored) {}
			}
			throw e;
		}
	}

	/**
	 * Returns the first field of the given type in a class.
	 * Might be useful for Proguard'ed classes to identify fields with unique types.
	 *
	 * @param clazz The class which either declares or inherits the field.
	 * @param type The type of the field.
	 * @return A reference to the first field of the given type.
	 * @throws NoSuchFieldError In case no matching field was not found.
	 */
	public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
		Class<?> clz = clazz;
		do {
			for (Field field : clz.getDeclaredFields()) {
				if (field.getType() == type) {
					field.setAccessible(true);
					return field;
				}
			}
		} while ((clz = clz.getSuperclass()) != null);

		throw new NoSuchFieldError("Field of type " + type.getName() + " in class " + clazz.getName());
	}

	/**
	 * Look up a method and hook it. See {@link #findAndHookMethod(String, ClassLoader, String, Object...)}
	 * for details.
	 */
	public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
		if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length-1] instanceof XC_MethodHook))
			throw new IllegalArgumentException("no callback defined");

		XC_MethodHook callback = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length-1];
		Method m = findMethodExact(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));

		return XposedBridge.hookMethod(m, callback);
	}

	/**
	 * Look up a method and hook it. The last argument must be the callback for the hook.
	 *
	 * <p>This combines calls to {@link #findMethodExact(Class, String, Object...)} and
	 * {@link XposedBridge#hookMethod}.
	 *
	 * <p class="warning">The method must be declared or overridden in the given class, inherited
	 * methods are not considered! That's because each method implementation exists only once in
	 * the memory, and when classes inherit it, they just get another reference to the implementation.
	 * Hooking a method therefore applies to all classes inheriting the same implementation. You
	 * have to expect that the hook applies to subclasses (unless they override the method), but you
	 * shouldn't have to worry about hooks applying to superclasses, hence this "limitation".
	 * There could be undesired or even dangerous hooks otherwise, e.g. if you hook
	 * {@code SomeClass.equals()} and that class doesn't override the {@code equals()} on some ROMs,
	 * making you hook {@code Object.equals()} instead.
	 *
	 * <p>There are two ways to specify the parameter types. If you already have a reference to the
	 * {@link Class}, use that. For Android framework classes, you can often use something like
	 * {@code String.class}. If you don't have the class reference, you can simply use the
	 * full class name as a string, e.g. {@code java.lang.String} or {@code com.example.MyClass}.
	 * It will be passed to {@link #findClass} with the same class loader that is used for the target
	 * method, see its documentation for the allowed notations.
	 *
	 * <p>Primitive types, such as {@code int}, can be specified using {@code int.class} (recommended)
	 * or {@code Integer.TYPE}. Note that {@code Integer.class} doesn't refer to {@code int} but to
	 * {@code Integer}, which is a normal class (boxed primitive). Therefore it must not be used when
	 * the method expects an {@code int} parameter - it has to be used for {@code Integer} parameters
	 * though, so check the method signature in detail.
	 *
	 * <p>As last argument to this method (after the list of target method parameters), you need
	 * to specify the callback that should be executed when the method is invoked. It's usually
	 * an anonymous subclass of {@link XC_MethodHook} or {@link XC_MethodReplacement}.
	 *
	 * <p><b>Example</b>
	 * <pre class="prettyprint">
	 * // In order to hook this method ...
	 * package com.example;
	 * public class SomeClass {
	 *   public int doSomething(String s, int i, MyClass m) {
	 *     ...
	 *   }
	 * }
	 *
	 * // ... you can use this call:
	 * findAndHookMethod("com.example.SomeClass", lpparam.classLoader, String.class, int.class, "com.example.MyClass", new XC_MethodHook() {
	 *   &#64;Override
	 *   protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
	 *     String oldText = (String) param.args[0];
	 *     Log.d("MyModule", oldText);
	 *
	 *     param.args[0] = "test";
	 *     param.args[1] = 42; // auto-boxing is working here
	 *     setBooleanField(param.args[2], "great", true);
	 *
	 *     // This would not work (as MyClass can't be resolved at compile time):
	 *     //   MyClass myClass = (MyClass) param.args[2];
	 *     //   myClass.great = true;
	 *   }
	 * });
	 * </pre>
	 *
	 * @param className The name of the class which implements the method.
	 * @param classLoader The class loader for resolving the target and parameter classes.
	 * @param methodName The target method name.
	 * @param parameterTypesAndCallback The parameter types of the target method, plus the callback.
	 * @throws NoSuchMethodError In case the method was not found.
	 * @throws ClassNotFoundError In case the target class or one of the parameter types couldn't be resolved.
	 * @return An object which can be used to remove the callback again.
	 */
	public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
		return findAndHookMethod(findClass(className, classLoader), methodName, parameterTypesAndCallback);
	}

	/**
	 * Look up a method in a class and set it to accessible.
	 * See {@link #findMethodExact(String, ClassLoader, String, Object...)} for details.
	 */
	public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) {
		return findMethodExact(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypes));
	}

	/**
	 * Look up and return a method if it exists.
	 * See {@link #findMethodExactIfExists(String, ClassLoader, String, Object...)} for details.
	 */
	public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Object... parameterTypes) {
		try {
			return findMethodExact(clazz, methodName, parameterTypes);
		} catch (ClassNotFoundError | NoSuchMethodError e) {
			return null;
		}
	}

	/**
	 * Look up a method in a class and set it to accessible.
	 * The method must be declared or overridden in the given class.
	 *
	 * <p>See {@link #findAndHookMethod(String, ClassLoader, String, Object...)} for details about
	 * the method and parameter type resolution.
	 *
	 * @param className The name of the class which implements the method.
	 * @param classLoader The class loader for resolving the target and parameter classes.
	 * @param methodName The target method name.
	 * @param parameterTypes The parameter types of the target method.
	 * @throws NoSuchMethodError In case the method was not found.
	 * @throws ClassNotFoundError In case the target class or one of the parameter types couldn't be resolved.
	 * @return A reference to the method.
	 */
	public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
		return findMethodExact(findClass(className, classLoader), methodName, getParameterClasses(classLoader, parameterTypes));
	}

	/**
	 * Look up and return a method if it exists.
	 * Like {@link #findMethodExact(String, ClassLoader, String, Object...)}, but doesn't throw an
	 * exception if the method doesn't exist.
	 *
	 * @param className The name of the class which implements the method.
	 * @param classLoader The class loader for resolving the target and parameter classes.
	 * @param methodName The target method name.
	 * @param parameterTypes The parameter types of the target method.
	 * @return A reference to the method, or {@code null} if it doesn't exist.
	 */
	public static Method findMethodExactIfExists(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
		try {
			return findMethodExact(className, classLoader, methodName, parameterTypes);
		} catch (ClassNotFoundError | NoSuchMethodError e) {
			return null;
		}
	}

	/**
	 * Look up a method in a class and set it to accessible.
	 * See {@link #findMethodExact(String, ClassLoader, String, Object...)} for details.
	 *
	 * <p>This variant requires that you already have reference to all the parameter types.
	 */
	public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		String fullMethodName = clazz.getName() + '#' + methodName + getParametersString(parameterTypes) + "#exact";

		if (methodCache.containsKey(fullMethodName)) {
			Method method = methodCache.get(fullMethodName);
			if (method == null)
				throw new NoSuchMethodError(fullMethodName);
			return method;
		}

		try {
			Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
			method.setAccessible(true);
			methodCache.put(fullMethodName, method);
			return method;
		} catch (NoSuchMethodException e) {
			methodCache.put(fullMethodName, null);
			throw new NoSuchMethodError(fullMethodName);
		}
	}

	/**
	 * Returns an array of all methods declared/overridden in a class with the specified parameter types.
	 *
	 * <p>The return type is optional, it will not be compared if it is {@code null}.
	 * Use {@code void.class} if you want to search for methods returning nothing.
	 *
	 * @param clazz The class to look in.
	 * @param returnType The return type, or {@code null} (see above).
	 * @param parameterTypes The parameter types.
	 * @return An array with matching methods, all set to accessible already.
	 */
	public static Method[] findMethodsByExactParameters(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
		List<Method> result = new LinkedList<>();
		for (Method method : clazz.getDeclaredMethods()) {
			if (returnType != null && returnType != method.getReturnType())
				continue;

			Class<?>[] methodParameterTypes = method.getParameterTypes();
			if (parameterTypes.length != methodParameterTypes.length)
				continue;

			boolean match = true;
			for (int i = 0; i < parameterTypes.length; i++) {
				if (parameterTypes[i] != methodParameterTypes[i]) {
					match = false;
					break;
				}
			}

			if (!match)
				continue;

			method.setAccessible(true);
			result.add(method);
		}
		return result.toArray(new Method[result.size()]);
	}

	/**
	 * Look up a method in a class and set it to accessible.
	 *
	 * <p>This does'nt only look for exact matches, but for the best match. All considered candidates
	 * must be compatible with the given parameter types, i.e. the parameters must be assignable
	 * to the method's formal parameters. Inherited methods are considered here.
	 *
	 * @param clazz The class which declares, inherits or overrides the method.
	 * @param methodName The method name.
	 * @param parameterTypes The types of the method's parameters.
	 * @return A reference to the best-matching method.
	 * @throws NoSuchMethodError In case no suitable method was found.
	 */
	public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		String fullMethodName = clazz.getName() + '#' + methodName + getParametersString(parameterTypes) + "#bestmatch";

		if (methodCache.containsKey(fullMethodName)) {
			Method method = methodCache.get(fullMethodName);
			if (method == null)
				throw new NoSuchMethodError(fullMethodName);
			return method;
		}

		try {
			Method method = findMethodExact(clazz, methodName, parameterTypes);
			methodCache.put(fullMethodName, method);
			return method;
		} catch (NoSuchMethodError ignored) {}

		Method bestMatch = null;
		Class<?> clz = clazz;
		boolean considerPrivateMethods = true;
		do {
			for (Method method : clz.getDeclaredMethods()) {
				// don't consider private methods of superclasses
				if (!considerPrivateMethods && Modifier.isPrivate(method.getModifiers()))
					continue;

				// compare name and parameters
				if (method.getName().equals(methodName) && ClassUtils.isAssignable(parameterTypes, method.getParameterTypes(), true)) {
					// get accessible version of method
					if (bestMatch == null || MemberUtils.compareParameterTypes(
							method.getParameterTypes(),
							bestMatch.getParameterTypes(),
							parameterTypes) < 0) {
						bestMatch = method;
					}
				}
			}
			considerPrivateMethods = false;
		} while ((clz = clz.getSuperclass()) != null);

		if (bestMatch != null) {
			bestMatch.setAccessible(true);
			methodCache.put(fullMethodName, bestMatch);
			return bestMatch;
		} else {
			NoSuchMethodError e = new NoSuchMethodError(fullMethodName);
			methodCache.put(fullMethodName, null);
			throw e;
		}
	}

	/**
	 * Look up a method in a class and set it to accessible.
	 *
	 * <p>See {@link #findMethodBestMatch(Class, String, Class...)} for details. This variant
	 * determines the parameter types from the classes of the given objects.
	 */
	public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
		return findMethodBestMatch(clazz, methodName, getParameterTypes(args));
	}

	/**
	 * Look up a method in a class and set it to accessible.
	 *
	 * <p>See {@link #findMethodBestMatch(Class, String, Class...)} for details. This variant
	 * determines the parameter types from the classes of the given objects. For any item that is
	 * {@code null}, the type is taken from {@code parameterTypes} instead.
	 */
	public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args) {
		Class<?>[] argsClasses = null;
		for (int i = 0; i < parameterTypes.length; i++) {
			if (parameterTypes[i] != null)
				continue;
			if (argsClasses == null)
				argsClasses = getParameterTypes(args);
			parameterTypes[i] = argsClasses[i];
		}
		return findMethodBestMatch(clazz, methodName, parameterTypes);
	}

	/**
	 * Returns an array with the classes of the given objects.
	 */
	public static Class<?>[] getParameterTypes(Object... args) {
		Class<?>[] clazzes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			clazzes[i] = (args[i] != null) ? args[i].getClass() : null;
		}
		return clazzes;
	}

	/**
	 * Retrieve classes from an array, where each element might either be a Class
	 * already, or a String with the full class name.
	 */
	private static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypesAndCallback) {
		Class<?>[] parameterClasses = null;
		for (int i = parameterTypesAndCallback.length - 1; i >= 0; i--) {
			Object type = parameterTypesAndCallback[i];
			if (type == null)
				throw new ClassNotFoundError("parameter type must not be null", null);

			// ignore trailing callback
			if (type instanceof XC_MethodHook)
				continue;

			if (parameterClasses == null)
				parameterClasses = new Class<?>[i+1];

			if (type instanceof Class)
				parameterClasses[i] = (Class<?>) type;
			else if (type instanceof String)
				parameterClasses[i] = findClass((String) type, classLoader);
			else
				throw new ClassNotFoundError("parameter type must either be specified as Class or String", null);
		}

		// if there are no arguments for the method
		if (parameterClasses == null)
			parameterClasses = new Class<?>[0];

		return parameterClasses;
	}

	/**
	 * Returns an array of the given classes.
	 */
	public static Class<?>[] getClassesAsArray(Class<?>... clazzes) {
		return clazzes;
	}

	private static String getParametersString(Class<?>... clazzes) {
		StringBuilder sb = new StringBuilder("(");
		boolean first = true;
		for (Class<?> clazz : clazzes) {
			if (first)
				first = false;
			else
				sb.append(",");

			if (clazz != null)
				sb.append(clazz.getCanonicalName());
			else
				sb.append("null");
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Look up a constructor of a class and set it to accessible.
	 * See {@link #findMethodExact(String, ClassLoader, String, Object...)} for details.
	 */
	public static Constructor<?> findConstructorExact(Class<?> clazz, Object... parameterTypes) {
		return findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypes));
	}

	/**
	 * Look up and return a constructor if it exists.
	 * See {@link #findMethodExactIfExists(String, ClassLoader, String, Object...)} for details.
	 */
	public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, Object... parameterTypes) {
		try {
			return findConstructorExact(clazz, parameterTypes);
		} catch (ClassNotFoundError | NoSuchMethodError e) {
			return null;
		}
	}

	/**
	 * Look up a constructor of a class and set it to accessible.
	 * See {@link #findMethodExact(String, ClassLoader, String, Object...)} for details.
	 */
	public static Constructor<?> findConstructorExact(String className, ClassLoader classLoader, Object... parameterTypes) {
		return findConstructorExact(findClass(className, classLoader), getParameterClasses(classLoader, parameterTypes));
	}

	/**
	 * Look up and return a constructor if it exists.
	 * See {@link #findMethodExactIfExists(String, ClassLoader, String, Object...)} for details.
	 */
	public static Constructor<?> findConstructorExactIfExists(String className, ClassLoader classLoader, Object... parameterTypes) {
		try {
			return findConstructorExact(className, classLoader, parameterTypes);
		} catch (ClassNotFoundError | NoSuchMethodError e) {
			return null;
		}
	}

	/**
	 * Look up a constructor of a class and set it to accessible.
	 * See {@link #findMethodExact(String, ClassLoader, String, Object...)} for details.
	 */
	public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
		String fullConstructorName = clazz.getName() + getParametersString(parameterTypes) + "#exact";

		if (constructorCache.containsKey(fullConstructorName)) {
			Constructor<?> constructor = constructorCache.get(fullConstructorName);
			if (constructor == null)
				throw new NoSuchMethodError(fullConstructorName);
			return constructor;
		}

		try {
			Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);
			constructorCache.put(fullConstructorName, constructor);
			return constructor;
		} catch (NoSuchMethodException e) {
			constructorCache.put(fullConstructorName, null);
			throw new NoSuchMethodError(fullConstructorName);
		}
	}

	/**
	 * Look up a constructor and hook it. See {@link #findAndHookMethod(String, ClassLoader, String, Object...)}
	 * for details.
	 */
	public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
		if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length-1] instanceof XC_MethodHook))
			throw new IllegalArgumentException("no callback defined");

		XC_MethodHook callback = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length-1];
		Constructor<?> m = findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));

		return XposedBridge.hookMethod(m, callback);
	}

	/**
	 * Look up a constructor and hook it. See {@link #findAndHookMethod(String, ClassLoader, String, Object...)}
	 * for details.
	 */
	public static XC_MethodHook.Unhook findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
		return findAndHookConstructor(findClass(className, classLoader), parameterTypesAndCallback);
	}

	/**
	 * Look up a constructor in a class and set it to accessible.
	 *
	 * <p>See {@link #findMethodBestMatch(Class, String, Class...)} for details.
	 */
	public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
		String fullConstructorName = clazz.getName() + getParametersString(parameterTypes) + "#bestmatch";

		if (constructorCache.containsKey(fullConstructorName)) {
			Constructor<?> constructor = constructorCache.get(fullConstructorName);
			if (constructor == null)
				throw new NoSuchMethodError(fullConstructorName);
			return constructor;
		}

		try {
			Constructor<?> constructor = findConstructorExact(clazz, parameterTypes);
			constructorCache.put(fullConstructorName, constructor);
			return constructor;
		} catch (NoSuchMethodError ignored) {}

		Constructor<?> bestMatch = null;
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		for (Constructor<?> constructor : constructors) {
			// compare name and parameters
			if (ClassUtils.isAssignable(parameterTypes, constructor.getParameterTypes(), true)) {
				// get accessible version of method
				if (bestMatch == null || MemberUtils.compareParameterTypes(
						constructor.getParameterTypes(),
						bestMatch.getParameterTypes(),
						parameterTypes) < 0) {
					bestMatch = constructor;
				}
			}
		}

		if (bestMatch != null) {
			bestMatch.setAccessible(true);
			constructorCache.put(fullConstructorName, bestMatch);
			return bestMatch;
		} else {
			NoSuchMethodError e = new NoSuchMethodError(fullConstructorName);
			constructorCache.put(fullConstructorName, null);
			throw e;
		}
	}

	/**
	 * Look up a constructor in a class and set it to accessible.
	 *
	 * <p>See {@link #findMethodBestMatch(Class, String, Class...)} for details. This variant
	 * determines the parameter types from the classes of the given objects.
	 */
	public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
		return findConstructorBestMatch(clazz, getParameterTypes(args));
	}

	/**
	 * Look up a constructor in a class and set it to accessible.
	 *
	 * <p>See {@link #findMethodBestMatch(Class, String, Class...)} for details. This variant
	 * determines the parameter types from the classes of the given objects. For any item that is
	 * {@code null}, the type is taken from {@code parameterTypes} instead.
	 */
	public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>[] parameterTypes, Object[] args) {
		Class<?>[] argsClasses = null;
		for (int i = 0; i < parameterTypes.length; i++) {
			if (parameterTypes[i] != null)
				continue;
			if (argsClasses == null)
				argsClasses = getParameterTypes(args);
			parameterTypes[i] = argsClasses[i];
		}
		return findConstructorBestMatch(clazz, parameterTypes);
	}

	/**
	 * Thrown when a class loader is unable to find a class. Unlike {@link ClassNotFoundException},
	 * callers are not forced to explicitly catch this. If uncaught, the error will be passed to the
	 * next caller in the stack.
	 */
	public static final class ClassNotFoundError extends Error {
		private static final long serialVersionUID = -1070936889459514628L;

		/** @hide */
		public ClassNotFoundError(Throwable cause) {
			super(cause);
		}

		/** @hide */
		public ClassNotFoundError(String detailMessage, Throwable cause) {
			super(detailMessage, cause);
		}
	}

	/**
	 * Returns the index of the first parameter declared with the given type.
	 *
	 * @throws NoSuchFieldError if there is no parameter with that type.
	 * @hide
	 */
	public static int getFirstParameterIndexByType(Member method, Class<?> type) {
		Class<?>[] classes = (method instanceof  Method) ?
				((Method) method).getParameterTypes() : ((Constructor) method).getParameterTypes();
		for (int i = 0 ; i < classes.length; i++) {
			if (classes[i] == type) {
				return i;
			}
		}
		throw new NoSuchFieldError("No parameter of type " + type + " found in " + method);
	}

	/**
	 * Returns the index of the parameter declared with the given type, ensuring that there is exactly one such parameter.
	 *
	 * @throws NoSuchFieldError if there is no or more than one parameter with that type.
	 * @hide
	 */
	public static int getParameterIndexByType(Member method, Class<?> type) {
		Class<?>[] classes = (method instanceof  Method) ?
				((Method) method).getParameterTypes() : ((Constructor) method).getParameterTypes();
		int idx = -1;
		for (int i = 0 ; i < classes.length; i++) {
			if (classes[i] == type) {
				if (idx == -1) {
					idx = i;
				} else {
					throw new NoSuchFieldError("More than one parameter of type " + type + " found in " + method);
				}
			}
		}
		if (idx != -1) {
			return idx;
		} else {
			throw new NoSuchFieldError("No parameter of type " + type + " found in " + method);
		}
	}

	//#################################################################################################
	/** Sets the value of an object field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setObjectField(Object obj, String fieldName, Object value) {
		try {
			findField(obj.getClass(), fieldName).set(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a {@code boolean} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setBooleanField(Object obj, String fieldName, boolean value) {
		try {
			findField(obj.getClass(), fieldName).setBoolean(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a {@code byte} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setByteField(Object obj, String fieldName, byte value) {
		try {
			findField(obj.getClass(), fieldName).setByte(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a {@code char} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setCharField(Object obj, String fieldName, char value) {
		try {
			findField(obj.getClass(), fieldName).setChar(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a {@code double} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setDoubleField(Object obj, String fieldName, double value) {
		try {
			findField(obj.getClass(), fieldName).setDouble(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a {@code float} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setFloatField(Object obj, String fieldName, float value) {
		try {
			findField(obj.getClass(), fieldName).setFloat(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of an {@code int} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setIntField(Object obj, String fieldName, int value) {
		try {
			findField(obj.getClass(), fieldName).setInt(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a {@code long} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setLongField(Object obj, String fieldName, long value) {
		try {
			findField(obj.getClass(), fieldName).setLong(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a {@code short} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static void setShortField(Object obj, String fieldName, short value) {
		try {
			findField(obj.getClass(), fieldName).setShort(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	//#################################################################################################
	/** Returns the value of an object field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static Object getObjectField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).get(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** For inner classes, returns the surrounding instance, i.e. the {@code this} reference of the surrounding class. */
	public static Object getSurroundingThis(Object obj) {
		return getObjectField(obj, "this$0");
	}

	/** Returns the value of a {@code boolean} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean getBooleanField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getBoolean(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Returns the value of a {@code byte} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static byte getByteField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getByte(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Returns the value of a {@code char} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static char getCharField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getChar(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Returns the value of a {@code double} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static double getDoubleField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getDouble(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Returns the value of a {@code float} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static float getFloatField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getFloat(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Returns the value of an {@code int} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static int getIntField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getInt(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Returns the value of a {@code long} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static long getLongField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getLong(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Returns the value of a {@code short} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
	public static short getShortField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getShort(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	//#################################################################################################
	/** Sets the value of a static object field in the given class. See also {@link #findField}. */
	public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
		try {
			findField(clazz, fieldName).set(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code boolean} field in the given class. See also {@link #findField}. */
	public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
		try {
			findField(clazz, fieldName).setBoolean(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code byte} field in the given class. See also {@link #findField}. */
	public static void setStaticByteField(Class<?> clazz, String fieldName, byte value) {
		try {
			findField(clazz, fieldName).setByte(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code char} field in the given class. See also {@link #findField}. */
	public static void setStaticCharField(Class<?> clazz, String fieldName, char value) {
		try {
			findField(clazz, fieldName).setChar(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code double} field in the given class. See also {@link #findField}. */
	public static void setStaticDoubleField(Class<?> clazz, String fieldName, double value) {
		try {
			findField(clazz, fieldName).setDouble(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code float} field in the given class. See also {@link #findField}. */
	public static void setStaticFloatField(Class<?> clazz, String fieldName, float value) {
		try {
			findField(clazz, fieldName).setFloat(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code int} field in the given class. See also {@link #findField}. */
	public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
		try {
			findField(clazz, fieldName).setInt(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code long} field in the given class. See also {@link #findField}. */
	public static void setStaticLongField(Class<?> clazz, String fieldName, long value) {
		try {
			findField(clazz, fieldName).setLong(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code short} field in the given class. See also {@link #findField}. */
	public static void setStaticShortField(Class<?> clazz, String fieldName, short value) {
		try {
			findField(clazz, fieldName).setShort(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	//#################################################################################################
	/** Returns the value of a static object field in the given class. See also {@link #findField}. */
	public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).get(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Returns the value of a static {@code boolean} field in the given class. See also {@link #findField}. */
	public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getBoolean(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code byte} field in the given class. See also {@link #findField}. */
	public static byte getStaticByteField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getByte(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code char} field in the given class. See also {@link #findField}. */
	public static char getStaticCharField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getChar(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code double} field in the given class. See also {@link #findField}. */
	public static double getStaticDoubleField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getDouble(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code float} field in the given class. See also {@link #findField}. */
	public static float getStaticFloatField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getFloat(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code int} field in the given class. See also {@link #findField}. */
	public static int getStaticIntField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getInt(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code long} field in the given class. See also {@link #findField}. */
	public static long getStaticLongField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getLong(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** Sets the value of a static {@code short} field in the given class. See also {@link #findField}. */
	public static short getStaticShortField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getShort(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	//#################################################################################################
	/**
	 * Calls an instance or static method of the given object.
	 * The method is resolved using {@link #findMethodBestMatch(Class, String, Object...)}.
	 *
	 * @param obj The object instance. A class reference is not sufficient!
	 * @param methodName The method name.
	 * @param args The arguments for the method call.
	 * @throws NoSuchMethodError In case no suitable method was found.
	 * @throws InvocationTargetError In case an exception was thrown by the invoked method.
	 */
	public static Object callMethod(Object obj, String methodName, Object... args) {
		try {
			return findMethodBestMatch(obj.getClass(), methodName, args).invoke(obj, args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		}
	}

	/**
	 * Calls an instance or static method of the given object.
	 * See {@link #callMethod(Object, String, Object...)}.
	 *
	 * <p>This variant allows you to specify parameter types, which can help in case there are multiple
	 * methods with the same name, especially if you call it with {@code null} parameters.
	 */
	public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
		try {
			return findMethodBestMatch(obj.getClass(), methodName, parameterTypes, args).invoke(obj, args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		}
	}

	/**
	 * Calls a static method of the given class.
	 * The method is resolved using {@link #findMethodBestMatch(Class, String, Object...)}.
	 *
	 * @param clazz The class reference.
	 * @param methodName The method name.
	 * @param args The arguments for the method call.
	 * @throws NoSuchMethodError In case no suitable method was found.
	 * @throws InvocationTargetError In case an exception was thrown by the invoked method.
	 */
	public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
		try {
			return findMethodBestMatch(clazz, methodName, args).invoke(null, args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		}
	}

	/**
	 * Calls a static method of the given class.
	 * See {@link #callStaticMethod(Class, String, Object...)}.
	 *
	 * <p>This variant allows you to specify parameter types, which can help in case there are multiple
	 * methods with the same name, especially if you call it with {@code null} parameters.
	 */
	public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
		try {
			return findMethodBestMatch(clazz, methodName, parameterTypes, args).invoke(null, args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		}
	}

	/**
	 * This class provides a wrapper for an exception thrown by a method invocation.
	 *
	 * @see #callMethod(Object, String, Object...)
	 * @see #callStaticMethod(Class, String, Object...)
	 * @see #newInstance(Class, Object...)
	 */
	public static final class InvocationTargetError extends Error {
		private static final long serialVersionUID = -1070936889459514628L;

		/** @hide */
		public InvocationTargetError(Throwable cause) {
			super(cause);
		}
	}

	//#################################################################################################
	/**
	 * Creates a new instance of the given class.
	 * The constructor is resolved using {@link #findConstructorBestMatch(Class, Object...)}.
	 *
	 * @param clazz The class reference.
	 * @param args The arguments for the constructor call.
	 * @throws NoSuchMethodError In case no suitable constructor was found.
	 * @throws InvocationTargetError In case an exception was thrown by the invoked method.
	 * @throws InstantiationError In case the class cannot be instantiated.
	 */
	public static Object newInstance(Class<?> clazz, Object... args) {
		try {
			return findConstructorBestMatch(clazz, args).newInstance(args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		} catch (InstantiationException e) {
			throw new InstantiationError(e.getMessage());
		}
	}

	/**
	 * Creates a new instance of the given class.
	 * See {@link #newInstance(Class, Object...)}.
	 *
	 * <p>This variant allows you to specify parameter types, which can help in case there are multiple
	 * constructors with the same name, especially if you call it with {@code null} parameters.
	 */
	public static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object... args) {
		try {
			return findConstructorBestMatch(clazz, parameterTypes, args).newInstance(args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		} catch (InstantiationException e) {
			throw new InstantiationError(e.getMessage());
		}
	}

	//#################################################################################################

	/**
	 * Attaches any value to an object instance. This simulates adding an instance field.
	 * The value can be retrieved again with {@link #getAdditionalInstanceField}.
	 *
	 * @param obj The object instance for which the value should be stored.
	 * @param key The key in the value map for this object instance.
	 * @param value The value to store.
	 * @return The previously stored value for this instance/key combination, or {@code null} if there was none.
	 */
	public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
		if (obj == null)
			throw new NullPointerException("object must not be null");
		if (key == null)
			throw new NullPointerException("key must not be null");

		HashMap<String, Object> objectFields;
		synchronized (additionalFields) {
			objectFields = additionalFields.get(obj);
			if (objectFields == null) {
				objectFields = new HashMap<>();
				additionalFields.put(obj, objectFields);
			}
		}

		synchronized (objectFields) {
			return objectFields.put(key, value);
		}
	}

	/**
	 * Returns a value which was stored with {@link #setAdditionalInstanceField}.
	 *
	 * @param obj The object instance for which the value has been stored.
	 * @param key The key in the value map for this object instance.
	 * @return The stored value for this instance/key combination, or {@code null} if there is none.
	 */
	public static Object getAdditionalInstanceField(Object obj, String key) {
		if (obj == null)
			throw new NullPointerException("object must not be null");
		if (key == null)
			throw new NullPointerException("key must not be null");

		HashMap<String, Object> objectFields;
		synchronized (additionalFields) {
			objectFields = additionalFields.get(obj);
			if (objectFields == null)
				return null;
		}

		synchronized (objectFields) {
			return objectFields.get(key);
		}
	}

	/**
	 * Removes and returns a value which was stored with {@link #setAdditionalInstanceField}.
	 *
	 * @param obj The object instance for which the value has been stored.
	 * @param key The key in the value map for this object instance.
	 * @return The previously stored value for this instance/key combination, or {@code null} if there was none.
	 */
	public static Object removeAdditionalInstanceField(Object obj, String key) {
		if (obj == null)
			throw new NullPointerException("object must not be null");
		if (key == null)
			throw new NullPointerException("key must not be null");

		HashMap<String, Object> objectFields;
		synchronized (additionalFields) {
			objectFields = additionalFields.get(obj);
			if (objectFields == null)
				return null;
		}

		synchronized (objectFields) {
			return objectFields.remove(key);
		}
	}

	/** Like {@link #setAdditionalInstanceField}, but the value is stored for the class of {@code obj}. */
	public static Object setAdditionalStaticField(Object obj, String key, Object value) {
		return setAdditionalInstanceField(obj.getClass(), key, value);
	}

	/** Like {@link #getAdditionalInstanceField}, but the value is returned for the class of {@code obj}. */
	public static Object getAdditionalStaticField(Object obj, String key) {
		return getAdditionalInstanceField(obj.getClass(), key);
	}

	/** Like {@link #removeAdditionalInstanceField}, but the value is removed and returned for the class of {@code obj}. */
	public static Object removeAdditionalStaticField(Object obj, String key) {
		return removeAdditionalInstanceField(obj.getClass(), key);
	}

	/** Like {@link #setAdditionalInstanceField}, but the value is stored for {@code clazz}. */
	public static Object setAdditionalStaticField(Class<?> clazz, String key, Object value) {
		return setAdditionalInstanceField(clazz, key, value);
	}

	/** Like {@link #setAdditionalInstanceField}, but the value is returned for {@code clazz}. */
	public static Object getAdditionalStaticField(Class<?> clazz, String key) {
		return getAdditionalInstanceField(clazz, key);
	}

	/** Like {@link #setAdditionalInstanceField}, but the value is removed and returned for {@code clazz}. */
	public static Object removeAdditionalStaticField(Class<?> clazz, String key) {
		return removeAdditionalInstanceField(clazz, key);
	}

	//#################################################################################################
	/**
	 * Loads an asset from a resource object and returns the content as {@code byte} array.
	 *
	 * @param res The resources from which the asset should be loaded.
	 * @param path The path to the asset, as in {@link AssetManager#open}.
	 * @return The content of the asset.
	 */
	public static byte[] assetAsByteArray(Resources res, String path) throws IOException {
		return inputStreamToByteArray(res.getAssets().open(path));
	}

	/*package*/ static byte[] inputStreamToByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] temp = new byte[1024];
		int read;

		while ((read = is.read(temp)) > 0) {
			buf.write(temp, 0, read);
		}
		is.close();
		return buf.toByteArray();
	}

	/**
	 * Invokes the {@link Closeable#close()} method, ignoring IOExceptions.
	 */
	/*package*/ static void closeSilently(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException ignored) {}
		}
	}

	/**
	 * Invokes the {@link DexFile#close()} method, ignoring IOExceptions.
	 */
	/*package*/ static void closeSilently(DexFile dexFile) {
		if (dexFile != null) {
			try {
				dexFile.close();
			} catch (IOException ignored) {}
		}
	}

	/**
	 * Invokes the {@link ZipFile#close()} method, ignoring IOExceptions.
	 */
	/*package*/ static void closeSilently(ZipFile zipFile) {
		if (zipFile != null) {
			try {
				zipFile.close();
			} catch (IOException ignored) {}
		}
	}

	/**
	 * Returns the lowercase hex string representation of a file's MD5 hash sum.
	 */
	public static String getMD5Sum(String file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			InputStream is = new FileInputStream(file);
			byte[] buffer = new byte[8192];
			int read;
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			is.close();
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			return bigInt.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
	}

	//#################################################################################################
	/**
	 * Increments the depth counter for the given method.
	 *
	 * <p>The intention of the method depth counter is to keep track of the call depth for recursive
	 * methods, e.g. to override parameters only for the outer call. The Xposed framework uses this
	 * to load drawable replacements only once per call, even when multiple
	 * {@link Resources#getDrawable} variants call each other.
	 *
	 * @param method The method name. Should be prefixed with a unique, module-specific string.
	 * @return The updated depth.
	 */
	public static int incrementMethodDepth(String method) {
		return getMethodDepthCounter(method).get().incrementAndGet();
	}

	/**
	 * Decrements the depth counter for the given method.
	 * See {@link #incrementMethodDepth} for details.
	 *
	 * @param method The method name. Should be prefixed with a unique, module-specific string.
	 * @return The updated depth.
	 */
	public static int decrementMethodDepth(String method) {
		return getMethodDepthCounter(method).get().decrementAndGet();
	}

	/**
	 * Returns the current depth counter for the given method.
	 * See {@link #incrementMethodDepth} for details.
	 *
	 * @param method The method name. Should be prefixed with a unique, module-specific string.
	 * @return The updated depth.
	 */
	public static int getMethodDepth(String method) {
		return getMethodDepthCounter(method).get().get();
	}

	private static ThreadLocal<AtomicInteger> getMethodDepthCounter(String method) {
		synchronized (sMethodDepth) {
			ThreadLocal<AtomicInteger> counter = sMethodDepth.get(method);
			if (counter == null) {
				counter = new ThreadLocal<AtomicInteger>() {
					@Override
					protected AtomicInteger initialValue() {
						return new AtomicInteger();
					}
				};
				sMethodDepth.put(method, counter);
			}
			return counter;
		}
	}

	/*package*/ static boolean fileContains(File file, String str) throws IOException {
		// There are certainly more efficient algorithms (e.g. Boyer-Moore used in grep),
		// but the naive approach should be sufficient here.
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				if (line.contains(str)) {
					return true;
				}
			}
			return false;
		} finally {
			closeSilently(in);
		}
	}

	//#################################################################################################

	/**
	 * Returns the method that is overridden by the given method.
	 * It returns {@code null} if the method doesn't override another method or if that method is
	 * abstract, i.e. if this is the first implementation in the hierarchy.
	 */
	/*package*/ static Method getOverriddenMethod(Method method) {
		int modifiers = method.getModifiers();
		if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)) {
			return null;
		}

		String name = method.getName();
		Class<?>[] parameters = method.getParameterTypes();
		Class<?> clazz = method.getDeclaringClass().getSuperclass();
		while (clazz != null) {
			try {
				Method superMethod = clazz.getDeclaredMethod(name, parameters);
				modifiers = superMethod.getModifiers();
				if (!Modifier.isPrivate(modifiers) && !Modifier.isAbstract(modifiers)) {
					return superMethod;
				} else {
					return null;
				}
			} catch (NoSuchMethodException ignored) {
				clazz = clazz.getSuperclass();
			}
		}
		return null;
	}

	/**
	 * Returns all methods which this class overrides.
	 */
	/*package*/ static Set<Method> getOverriddenMethods(Class<?> clazz) {
		Set<Method> methods = new HashSet<>();
		for (Method method : clazz.getDeclaredMethods()) {
			Method overridden = getOverriddenMethod(method);
			if (overridden != null) {
				methods.add(overridden);
			}
		}
		return methods;
	}

	//#################################################################################################
	// TODO helpers for view traversing
	/*To make it easier, I will try and implement some more helpers:
	- add view before/after existing view (I already mentioned that I think)
	- get index of view in its parent
	- get next/previous sibling (maybe with an optional argument "type", that might be ImageView.class and gives you the next sibling that is an ImageView)?
	- get next/previous element (similar to the above, but would also work if the next element has a different parent, it would just go up the hierarchy and then down again until it finds a matching element)
	- find the first child that is an instance of a specified class
	- find all (direct or indirect) children of a specified class
	*/

}

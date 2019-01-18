/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package external.com.android.dx.stock;

import external.com.android.dx.Code;
import external.com.android.dx.Comparison;
import external.com.android.dx.DexMaker;
import external.com.android.dx.FieldId;
import external.com.android.dx.Label;
import external.com.android.dx.Local;
import external.com.android.dx.MethodId;
import external.com.android.dx.TypeId;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

/**
 * Creates dynamic proxies of concrete classes.
 * <p>
 * This is similar to the {@code java.lang.reflect.Proxy} class, but works for classes instead of
 * interfaces.
 * <h3>Example</h3>
 * The following example demonstrates the creation of a dynamic proxy for {@code java.util.Random}
 * which will always return 4 when asked for integers, and which logs method calls to every method.
 * <pre>
 * InvocationHandler handler = new InvocationHandler() {
 *     &#64;Override
 *     public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
 *         if (method.getName().equals("nextInt")) {
 *             // Chosen by fair dice roll, guaranteed to be random.
 *             return 4;
 *         }
 *         Object result = ProxyBuilder.callSuper(proxy, method, args);
 *         System.out.println("Method: " + method.getName() + " args: "
 *                 + Arrays.toString(args) + " result: " + result);
 *         return result;
 *     }
 * };
 * Random debugRandom = ProxyBuilder.forClass(Random.class)
 *         .dexCache(getInstrumentation().getTargetContext().getDir("dx", Context.MODE_PRIVATE))
 *         .handler(handler)
 *         .build();
 * assertEquals(4, debugRandom.nextInt());
 * debugRandom.setSeed(0);
 * assertTrue(debugRandom.nextBoolean());
 * </pre>
 * <h3>Usage</h3>
 * Call {@link #forClass(Class)} for the Class you wish to proxy. Call
 * {@link #handler(InvocationHandler)} passing in an {@link InvocationHandler}, and then call
 * {@link #build()}. The returned instance will be a dynamically generated subclass where all method
 * calls will be delegated to the invocation handler, except as noted below.
 * <p>
 * The static method {@link #callSuper(Object, Method, Object...)} allows you to access the original
 * super method for a given proxy. This allows the invocation handler to selectively override some
 * methods but not others.
 * <p>
 * By default, the {@link #build()} method will call the no-arg constructor belonging to the class
 * being proxied. If you wish to call a different constructor, you must provide arguments for both
 * {@link #constructorArgTypes(Class[])} and {@link #constructorArgValues(Object[])}.
 * <p>
 * This process works only for classes with public and protected level of visibility.
 * <p>
 * You may proxy abstract classes.  You may not proxy final classes.
 * <p>
 * Only non-private, non-final, non-static methods will be dispatched to the invocation handler.
 * Private, static or final methods will always call through to the superclass as normal.
 * <p>
 * The {@link #finalize()} method on {@code Object} will not be proxied.
 * <p>
 * You must provide a dex cache directory via the {@link #dexCache(File)} method. You should take
 * care not to make this a world-writable directory, so that third parties cannot inject code into
 * your application.  A suitable parameter for these output directories would be something like
 * this:
 * <pre>{@code
 *     getApplicationContext().getDir("dx", Context.MODE_PRIVATE);
 * }</pre>
 * <p>
 * If the base class to be proxied leaks the {@code this} pointer in the constructor (bad practice),
 * that is to say calls a non-private non-final method from the constructor, the invocation handler
 * will not be invoked.  As a simple concrete example, when proxying Random we discover that it
 * internally calls setSeed during the constructor.  The proxy will not intercept this call during
 * proxy construction, but will intercept as normal afterwards.  This behaviour may be subject to
 * change in future releases.
 * <p>
 * This class is <b>not thread safe</b>.
 */
public final class ProxyBuilder<T> {
    // Version of ProxyBuilder. It should be updated if the implementation
    // of the generated proxy class changes.
    public static final int VERSION = 1;

    private static final String FIELD_NAME_HANDLER = "$__handler";
    private static final String FIELD_NAME_METHODS = "$__methodArray";

    /**
     * A cache of all proxy classes ever generated. At the time of writing,
     * Android's runtime doesn't support class unloading so there's little
     * value in using weak references.
     */
    private static final Map<ProxiedClass<?>, Class<?>> generatedProxyClasses
            = Collections.synchronizedMap(new HashMap<ProxiedClass<?>, Class<?>>());

    private final Class<T> baseClass;
    private ClassLoader parentClassLoader = ProxyBuilder.class.getClassLoader();
    private InvocationHandler handler;
    private File dexCache;
    private Class<?>[] constructorArgTypes = new Class[0];
    private Object[] constructorArgValues = new Object[0];
    private List<Class<?>> interfaces = new ArrayList<>();
    private Method[] methods;
    private boolean sharedClassLoader;
    private boolean markTrusted;

    private ProxyBuilder(Class<T> clazz) {
        baseClass = clazz;
    }

    public static <T> ProxyBuilder<T> forClass(Class<T> clazz) {
        return new ProxyBuilder<T>(clazz);
    }

    /**
     * Specifies the parent ClassLoader to use when creating the proxy.
     *
     * <p>If null, {@code ProxyBuilder.class.getClassLoader()} will be used.
     */
    public ProxyBuilder<T> parentClassLoader(ClassLoader parent) {
        parentClassLoader = parent;
        return this;
    }

    public ProxyBuilder<T> handler(InvocationHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Sets the directory where executable code is stored. See {@link
     * DexMaker#generateAndLoad DexMaker.generateAndLoad()} for guidance on
     * choosing a secure location for the dex cache.
     */
    public ProxyBuilder<T> dexCache(File dexCacheParent) {
        dexCache = new File(dexCacheParent, "v" + Integer.toString(VERSION));
        dexCache.mkdir();
        return this;
    }

    public ProxyBuilder<T> implementing(Class<?>... interfaces) {
        List<Class<?>> list = this.interfaces;
        for (Class<?> i : interfaces) {
            if (!i.isInterface()) {
                throw new IllegalArgumentException("Not an interface: " + i.getName());
            }
            if (!list.contains(i)) {
                list.add(i);
            }
        }
        return this;
    }

    public ProxyBuilder<T> constructorArgValues(Object... constructorArgValues) {
        this.constructorArgValues = constructorArgValues;
        return this;
    }

    public ProxyBuilder<T> constructorArgTypes(Class<?>... constructorArgTypes) {
        this.constructorArgTypes = constructorArgTypes;
        return this;
    }

    public ProxyBuilder<T> onlyMethods(Method[] methods) {
        this.methods = methods;
        return this;
    }

    public ProxyBuilder<T> withSharedClassLoader() {
        this.sharedClassLoader = true;
        return this;
    }

    public ProxyBuilder<T> markTrusted() {
        this.markTrusted = true;
        return this;
    }

    /**
     * Create a new instance of the class to proxy.
     *
     * @throws UnsupportedOperationException if the class we are trying to create a proxy for is
     *     not accessible.
     * @throws IOException if an exception occurred writing to the {@code dexCache} directory.
     * @throws UndeclaredThrowableException if the constructor for the base class to proxy throws
     *     a declared exception during construction.
     * @throws IllegalArgumentException if the handler is null, if the constructor argument types
     *     do not match the constructor argument values, or if no such constructor exists.
     */
    public T build() throws IOException {
        check(handler != null, "handler == null");
        check(constructorArgTypes.length == constructorArgValues.length,
                "constructorArgValues.length != constructorArgTypes.length");
        Class<? extends T> proxyClass = buildProxyClass();
        Constructor<? extends T> constructor;
        try {
            constructor = proxyClass.getConstructor(constructorArgTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No constructor for " + baseClass.getName()
                    + " with parameter types " + Arrays.toString(constructorArgTypes));
        }
        T result;
        try {
            result = constructor.newInstance(constructorArgValues);
        } catch (InstantiationException e) {
            // Should not be thrown, generated class is not abstract.
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            // Should not be thrown, the generated constructor is accessible.
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            // Thrown when the base class constructor throws an exception.
            throw launderCause(e);
        }
        setInvocationHandler(result, handler);
        return result;
    }

    // TODO: test coverage for this

    /**
     * Generate a proxy class. Note that new instances of this class will not automatically have an
     * an invocation handler, even if {@link #handler(InvocationHandler)} was called. The handler
     * must be set on each instance after it is created, using
     * {@link #setInvocationHandler(Object, InvocationHandler)}.
     */
    public Class<? extends T> buildProxyClass() throws IOException {
        ClassLoader requestedClassloader;
        if (sharedClassLoader) {
            requestedClassloader = baseClass.getClassLoader();
        } else {
            requestedClassloader = parentClassLoader;
        }

        // try the cache to see if we've generated this one before
        // we only populate the map with matching types
        ProxiedClass<T> cacheKey =
                new ProxiedClass<>(baseClass, interfaces, requestedClassloader, sharedClassLoader);
        @SuppressWarnings("unchecked")
        Class<? extends T> proxyClass = (Class) generatedProxyClasses.get(cacheKey);
        if (proxyClass != null) {
            return proxyClass; // cache hit!
        }

        // the cache missed; generate the class
        DexMaker dexMaker = new DexMaker();
        String generatedName = getMethodNameForProxyOf(baseClass, interfaces);
        TypeId<? extends T> generatedType = TypeId.get("L" + generatedName + ";");
        TypeId<T> superType = TypeId.get(baseClass);
        generateConstructorsAndFields(dexMaker, generatedType, superType, baseClass);

        Method[] methodsToProxy;
        if (methods == null) {
            methodsToProxy = getMethodsToProxyRecursive();
        } else {
            methodsToProxy = methods;
        }

        // Sort the results array so that they are in a deterministic fashion.
        //
        // We use the same parameters to sort as used in {@link MethodId#hashCode}. This is needed
        // as e.g. making a method "public" instead of "protected" should not change the id's of the
        // methods. If the id's would change the classes loaded from the cache would be incorrect.
        Arrays.sort(methodsToProxy, new Comparator<Method>() {
            @Override
            public int compare(Method method1, Method method2) {
                String m1Signature = method1.getDeclaringClass() + method1.getName() + Arrays.toString(method1.getParameterTypes()) + method1.getReturnType();
                String m2Signature = method2.getDeclaringClass() + method2.getName() + Arrays.toString(method2.getParameterTypes()) + method2.getReturnType();

                return m1Signature.compareTo(m2Signature);
            }
        });

        generateCodeForAllMethods(dexMaker, generatedType, methodsToProxy, superType);
        dexMaker.declare(generatedType, generatedName + ".generated", PUBLIC, superType, getInterfacesAsTypeIds());
        if (sharedClassLoader) {
            dexMaker.setSharedClassLoader(requestedClassloader);
        }
        if (markTrusted) {
            // The proxied class might have blacklisted methods. Blacklisting methods (and fields)
            // is a new feature of Android P:
            //
            // https://android-developers.googleblog.com/2018/02/
            // improving-stability-by-reducing-usage.html
            //
            // The newly generated class might not be allowed to call methods of the proxied class
            // if it is not trusted. As it is not clear which classes have blacklisted methods, mark
            // all generated classes as trusted.
            dexMaker.markAsTrusted();
        }
        ClassLoader classLoader;
        if (sharedClassLoader) {
            classLoader = dexMaker.generateAndLoad(null, dexCache);
        } else {
            classLoader = dexMaker.generateAndLoad(parentClassLoader, dexCache);
        }
        try {
            proxyClass = loadClass(classLoader, generatedName);
        } catch (IllegalAccessError e) {
            // Thrown when the base class is not accessible.
            throw new UnsupportedOperationException(
                    "cannot proxy inaccessible class " + baseClass, e);
        } catch (ClassNotFoundException e) {
            // Should not be thrown, we're sure to have generated this class.
            throw new AssertionError(e);
        }
        setMethodsStaticField(proxyClass, methodsToProxy);
        generatedProxyClasses.put(cacheKey, proxyClass);
        return proxyClass;
    }

    // The type cast is safe: the generated type will extend the base class type.
    @SuppressWarnings("unchecked")
    private Class<? extends T> loadClass(ClassLoader classLoader, String generatedName)
            throws ClassNotFoundException {
        return (Class<? extends T>) classLoader.loadClass(generatedName);
    }

    private static RuntimeException launderCause(InvocationTargetException e) {
        Throwable cause = e.getCause();
        // Errors should be thrown as they are.
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        // RuntimeException can be thrown as-is.
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        // Declared exceptions will have to be wrapped.
        throw new UndeclaredThrowableException(cause);
    }

    private static void setMethodsStaticField(Class<?> proxyClass, Method[] methodsToProxy) {
        try {
            Field methodArrayField = proxyClass.getDeclaredField(FIELD_NAME_METHODS);
            methodArrayField.setAccessible(true);
            methodArrayField.set(null, methodsToProxy);
        } catch (NoSuchFieldException e) {
            // Should not be thrown, generated proxy class has been generated with this field.
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            // Should not be thrown, we just set the field to accessible.
            throw new AssertionError(e);
        }
    }

    /**
     * Returns the proxy's {@link InvocationHandler}.
     *
     * @throws IllegalArgumentException if the object supplied is not a proxy created by this class.
     */
    public static InvocationHandler getInvocationHandler(Object instance) {
        try {
            Field field = instance.getClass().getDeclaredField(FIELD_NAME_HANDLER);
            field.setAccessible(true);
            return (InvocationHandler) field.get(instance);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Not a valid proxy instance", e);
        } catch (IllegalAccessException e) {
            // Should not be thrown, we just set the field to accessible.
            throw new AssertionError(e);
        }
    }

    /**
     * Sets the proxy's {@link InvocationHandler}.
     * <p>
     * If you create a proxy with {@link #build()}, the proxy will already have a handler set,
     * provided that you configured one with {@link #handler(InvocationHandler)}.
     * <p>
     * If you generate a proxy class with {@link #buildProxyClass()}, instances of the proxy class
     * will not automatically have a handler set, and it is necessary to use this method with each
     * instance.
     *
     * @throws IllegalArgumentException if the object supplied is not a proxy created by this class.
     */
    public static void setInvocationHandler(Object instance, InvocationHandler handler) {
        try {
            Field handlerField = instance.getClass().getDeclaredField(FIELD_NAME_HANDLER);
            handlerField.setAccessible(true);
            handlerField.set(instance, handler);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Not a valid proxy instance", e);
        } catch (IllegalAccessException e) {
            // Should not be thrown, we just set the field to accessible.
            throw new AssertionError(e);
        }
    }

    // TODO: test coverage for isProxyClass

    /**
     * Returns true if {@code c} is a proxy class created by this builder.
     */
    public static boolean isProxyClass(Class<?> c) {
        // TODO: use a marker interface instead?
        try {
            c.getDeclaredField(FIELD_NAME_HANDLER);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * Add
     *
     * <pre>
     *     abstractMethodErrorMessage = method + " cannot be called";
     *     abstractMethodError = new AbstractMethodError(abstractMethodErrorMessage);
     *     throw abstractMethodError;
     * </pre>
     *
     * to the {@code code}.
     *
     * @param code The code to add to
     * @param method The method that is abstract
     * @param abstractMethodErrorMessage The {@link Local} to store the error message
     * @param abstractMethodError The {@link Local} to store the error object
     */
    private static void throwAbstractMethodError(Code code, Method method,
                                                 Local<String> abstractMethodErrorMessage,
                                                 Local<AbstractMethodError> abstractMethodError) {
        TypeId<AbstractMethodError> abstractMethodErrorClass = TypeId.get(AbstractMethodError.class);

        MethodId<AbstractMethodError, Void> abstractMethodErrorConstructor =
                abstractMethodErrorClass.getConstructor(TypeId.STRING);
        code.loadConstant(abstractMethodErrorMessage, "'" + method + "' cannot be called");
        code.newInstance(abstractMethodError, abstractMethodErrorConstructor,
                abstractMethodErrorMessage);

        code.throwValue(abstractMethodError);
    }

    private static <T, G extends T> void generateCodeForAllMethods(DexMaker dexMaker,
            TypeId<G> generatedType, Method[] methodsToProxy, TypeId<T> superclassType) {
        TypeId<InvocationHandler> handlerType = TypeId.get(InvocationHandler.class);
        TypeId<Method[]> methodArrayType = TypeId.get(Method[].class);
        FieldId<G, InvocationHandler> handlerField =
                generatedType.getField(handlerType, FIELD_NAME_HANDLER);
        FieldId<G, Method[]> allMethods =
                generatedType.getField(methodArrayType, FIELD_NAME_METHODS);
        TypeId<Method> methodType = TypeId.get(Method.class);
        TypeId<Object[]> objectArrayType = TypeId.get(Object[].class);
        MethodId<InvocationHandler, Object> methodInvoke = handlerType.getMethod(TypeId.OBJECT,
                "invoke", TypeId.OBJECT, methodType, objectArrayType);
        for (int m = 0; m < methodsToProxy.length; ++m) {
            /*
             * If the 5th method on the superclass Example that can be overridden were to look like
             * this:
             *
             *     public int doSomething(Bar param0, int param1) {
             *         ...
             *     }
             *
             * Then the following dex byte code will generate a method on the proxy that looks
             * something like this (in idiomatic Java):
             *
             *     // if doSomething is not abstract
             *     public int doSomething(Bar param0, int param1) {
             *         if ($__handler == null) {
             *             return super.doSomething(param0, param1);
             *         }
             *         return __handler.invoke(this, __methodArray[4],
             *                 new Object[] { param0, Integer.valueOf(param1) });
             *     }
             *
             *     // if doSomething is abstract
             *     public int doSomething(Bar param0, int param1) {
             *         if ($__handler == null) {
             *             throw new AbstractMethodError("'doSomething' cannot be called");
             *         }
             *         return __handler.invoke(this, __methodArray[4],
             *                 new Object[] { param0, Integer.valueOf(param1) });
             *     }
             */
            Method method = methodsToProxy[m];
            String name = method.getName();
            Class<?>[] argClasses = method.getParameterTypes();
            TypeId<?>[] argTypes = new TypeId<?>[argClasses.length];
            for (int i = 0; i < argTypes.length; ++i) {
                argTypes[i] = TypeId.get(argClasses[i]);
            }
            Class<?> returnType = method.getReturnType();
            TypeId<?> resultType = TypeId.get(returnType);
            MethodId<?, ?> methodId = generatedType.getMethod(resultType, name, argTypes);
            TypeId<AbstractMethodError> abstractMethodErrorClass =
                    TypeId.get(AbstractMethodError.class);
            Code code = dexMaker.declare(methodId, PUBLIC);
            Local<G> localThis = code.getThis(generatedType);
            Local<InvocationHandler> localHandler = code.newLocal(handlerType);
            Local<Object> invokeResult = code.newLocal(TypeId.OBJECT);
            Local<Integer> intValue = code.newLocal(TypeId.INT);
            Local<Object[]> args = code.newLocal(objectArrayType);
            Local<Integer> argsLength = code.newLocal(TypeId.INT);
            Local<Object> temp = code.newLocal(TypeId.OBJECT);
            Local<?> resultHolder = code.newLocal(resultType);
            Local<Method[]> methodArray = code.newLocal(methodArrayType);
            Local<Method> thisMethod = code.newLocal(methodType);
            Local<Integer> methodIndex = code.newLocal(TypeId.INT);
            Class<?> aBoxedClass = PRIMITIVE_TO_BOXED.get(returnType);
            Local<?> aBoxedResult = null;
            if (aBoxedClass != null) {
                aBoxedResult = code.newLocal(TypeId.get(aBoxedClass));
            }
            Local<InvocationHandler> nullHandler = code.newLocal(handlerType);

            Local<?>[] superArgs2 = null;
            Local<?> superResult2 = null;
            MethodId<T, ?> superMethod = null;
            Local<String> abstractMethodErrorMessage = null;
            Local<AbstractMethodError> abstractMethodError = null;
            if ((method.getModifiers() & ABSTRACT) == 0) {
                superArgs2 = new Local<?>[argClasses.length];
                superResult2 = code.newLocal(resultType);
                superMethod = superclassType.getMethod(resultType, name, argTypes);
            } else {
                abstractMethodErrorMessage = code.newLocal(TypeId.STRING);
                abstractMethodError = code.newLocal(abstractMethodErrorClass);
            }

            code.loadConstant(methodIndex, m);
            code.sget(allMethods, methodArray);
            code.aget(thisMethod, methodArray, methodIndex);
            code.loadConstant(argsLength, argTypes.length);
            code.newArray(args, argsLength);
            code.iget(handlerField, localHandler, localThis);

            // if (proxy == null)
            code.loadConstant(nullHandler, null);
            Label handlerNullCase = new Label();
            code.compare(Comparison.EQ, handlerNullCase, nullHandler, localHandler);

            // This code is what we execute when we have a valid proxy: delegate to invocation
            // handler.
            for (int p = 0; p < argTypes.length; ++p) {
                code.loadConstant(intValue, p);
                Local<?> parameter = code.getParameter(p, argTypes[p]);
                Local<?> unboxedIfNecessary = boxIfRequired(code, parameter, temp);
                code.aput(args, intValue, unboxedIfNecessary);
            }
            code.invokeInterface(methodInvoke, invokeResult, localHandler,
                    localThis, thisMethod, args);
            generateCodeForReturnStatement(code, returnType, invokeResult, resultHolder,
                    aBoxedResult);

            // This code is executed if proxy is null: call the original super method.
            // This is required to handle the case of construction of an object which leaks the
            // "this" pointer.
            code.mark(handlerNullCase);

            if ((method.getModifiers() & ABSTRACT) == 0) {
                for (int i = 0; i < superArgs2.length; ++i) {
                    superArgs2[i] = code.getParameter(i, argTypes[i]);
                }
                if (void.class.equals(returnType)) {
                    code.invokeSuper(superMethod, null, localThis, superArgs2);
                    code.returnVoid();
                } else {
                    invokeSuper(superMethod, code, localThis, superArgs2, superResult2);
                    code.returnValue(superResult2);
                }
            } else {
                throwAbstractMethodError(code, method, abstractMethodErrorMessage,
                        abstractMethodError);
            }

            /*
             * And to allow calling the original super method, the following is also generated:
             *
             *     public String super$doSomething$java_lang_String(Bar param0, int param1) {
             *          int result = super.doSomething(param0, param1);
             *          return result;
             *     }
             */
            MethodId<G, ?> callsSuperMethod = generatedType.getMethod(
                    resultType, superMethodName(method), argTypes);
            Code superCode = dexMaker.declare(callsSuperMethod, PUBLIC);
            if ((method.getModifiers() & ABSTRACT) == 0) {
                Local<G> superThis = superCode.getThis(generatedType);
                Local<?>[] superArgs = new Local<?>[argClasses.length];
                for (int i = 0; i < superArgs.length; ++i) {
                    superArgs[i] = superCode.getParameter(i, argTypes[i]);
                }
                if (void.class.equals(returnType)) {
                    superCode.invokeSuper(superMethod, null, superThis, superArgs);
                    superCode.returnVoid();
                } else {
                    Local<?> superResult = superCode.newLocal(resultType);
                    invokeSuper(superMethod, superCode, superThis, superArgs, superResult);
                    superCode.returnValue(superResult);
                }
            } else {
                Local<String> superAbstractMethodErrorMessage = superCode.newLocal(TypeId.STRING);
                Local<AbstractMethodError> superAbstractMethodError = superCode.newLocal
                        (abstractMethodErrorClass);
                throwAbstractMethodError(superCode, method, superAbstractMethodErrorMessage,
                        superAbstractMethodError);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invokeSuper(MethodId superMethod, Code superCode,
            Local superThis, Local[] superArgs, Local superResult) {
        superCode.invokeSuper(superMethod, superResult, superThis, superArgs);
    }

    private static Local<?> boxIfRequired(Code code, Local<?> parameter, Local<Object> temp) {
        MethodId<?, ?> unboxMethod = PRIMITIVE_TYPE_TO_UNBOX_METHOD.get(parameter.getType());
        if (unboxMethod == null) {
            return parameter;
        }
        code.invokeStatic(unboxMethod, temp, parameter);
        return temp;
    }

    public static Object callSuper(Object proxy, Method method, Object... args) throws Throwable {
        try {
            return proxy.getClass()
                    .getMethod(superMethodName(method), method.getParameterTypes())
                    .invoke(proxy, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * The super method must include the return type, otherwise its ambiguous
     * for methods with covariant return types.
     */
    private static String superMethodName(Method method) {
        String returnType = method.getReturnType().getName();
        return "super$" + method.getName() + "$"
                + returnType.replace('.', '_').replace('[', '_').replace(';', '_');
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static <T, G extends T> void generateConstructorsAndFields(DexMaker dexMaker,
            TypeId<G> generatedType, TypeId<T> superType, Class<T> superClass) {
        TypeId<InvocationHandler> handlerType = TypeId.get(InvocationHandler.class);
        TypeId<Method[]> methodArrayType = TypeId.get(Method[].class);
        FieldId<G, InvocationHandler> handlerField = generatedType.getField(
                handlerType, FIELD_NAME_HANDLER);
        dexMaker.declare(handlerField, PRIVATE, null);
        FieldId<G, Method[]> allMethods = generatedType.getField(
                methodArrayType, FIELD_NAME_METHODS);
        dexMaker.declare(allMethods, PRIVATE | STATIC, null);
        for (Constructor<T> constructor : getConstructorsToOverwrite(superClass)) {
            if (constructor.getModifiers() == Modifier.FINAL) {
                continue;
            }
            TypeId<?>[] types = classArrayToTypeArray(constructor.getParameterTypes());
            MethodId<?, ?> method = generatedType.getConstructor(types);
            Code constructorCode = dexMaker.declare(method, PUBLIC);
            Local<G> thisRef = constructorCode.getThis(generatedType);
            Local<?>[] params = new Local[types.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = constructorCode.getParameter(i, types[i]);
            }
            MethodId<T, ?> superConstructor = superType.getConstructor(types);
            constructorCode.invokeDirect(superConstructor, null, thisRef, params);
            constructorCode.returnVoid();
        }
    }

    // The type parameter on Constructor is the class in which the constructor is declared.
    // The getDeclaredConstructors() method gets constructors declared only in the given class,
    // hence this cast is safe.
    @SuppressWarnings("unchecked")
    private static <T> Constructor<T>[] getConstructorsToOverwrite(Class<T> clazz) {
        return (Constructor<T>[]) clazz.getDeclaredConstructors();
    }

    private TypeId<?>[] getInterfacesAsTypeIds() {
        TypeId<?>[] result = new TypeId<?>[interfaces.size()];
        int i = 0;
        for (Class<?> implemented : interfaces) {
            result[i++] = TypeId.get(implemented);
        }
        return result;
    }

    /**
     * Gets all {@link Method} objects we can proxy in the hierarchy of the
     * supplied class.
     */
    private Method[] getMethodsToProxyRecursive() {
        Set<MethodSetEntry> methodsToProxy = new HashSet<>();
        Set<MethodSetEntry> seenFinalMethods = new HashSet<>();
        // Traverse the class hierarchy to ensure that all concrete methods (which could be marked
        // as final) are visited before any abstract methods from interfaces.
        for (Class<?> c = baseClass; c != null; c = c.getSuperclass()) {
            getMethodsToProxy(methodsToProxy, seenFinalMethods, c);
        }
        // Now traverse the interface hierarchy, starting with the ones implemented by the class,
        // followed by any extra interfaces.
        for (Class<?> c = baseClass; c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                getMethodsToProxy(methodsToProxy, seenFinalMethods, i);
            }
        }
        for (Class<?> c : interfaces) {
            getMethodsToProxy(methodsToProxy, seenFinalMethods, c);
        }

        Method[] results = new Method[methodsToProxy.size()];
        int i = 0;
        for (MethodSetEntry entry : methodsToProxy) {
            results[i++] = entry.originalMethod;
        }

        return results;
    }

    private void getMethodsToProxy(Set<MethodSetEntry> sink, Set<MethodSetEntry> seenFinalMethods,
            Class<?> c) {
        for (Method method : c.getDeclaredMethods()) {
            if ((method.getModifiers() & Modifier.FINAL) != 0) {
                // Skip final methods, we can't override them. We
                // also need to remember them, in case the same
                // method exists in a parent class.
                MethodSetEntry entry = new MethodSetEntry(method);
                seenFinalMethods.add(entry);
                // We may have seen this method already, from an interface
                // implemented by a child class. We need to remove it here.
                sink.remove(entry);
                continue;
            }
            if ((method.getModifiers() & STATIC) != 0) {
                // Skip static methods, overriding them has no effect.
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers())
                    && !Modifier.isProtected(method.getModifiers())
                    && (!sharedClassLoader || Modifier.isPrivate(method.getModifiers()))) {
                // Skip private methods, since they are invoked through direct
                // invocation (as opposed to virtual). Therefore, it would not
                // be possible to intercept any private method defined inside
                // the proxy class except through reflection.

                // Skip package-private methods as well (for non-shared class
                // loaders). The proxy class does
                // not actually inherit package-private methods from the parent
                // class because it is not a member of the parent's package.
                // This is even true if the two classes have the same package
                // name, as they use different class loaders.
                continue;
            }
            if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
                // Skip finalize method, it's likely important that it execute as normal.
                continue;
            }
            MethodSetEntry entry = new MethodSetEntry(method);
            if (seenFinalMethods.contains(entry)) {
                // This method is final in a child class.
                // We can't override it.
                continue;
            }
            sink.add(entry);
        }

        // Only visit the interfaces of this class if it is itself an interface. That prevents
        // visiting interfaces of a class before its super classes.
        if (c.isInterface()) {
            for (Class<?> i : c.getInterfaces()) {
                getMethodsToProxy(sink, seenFinalMethods, i);
            }
        }
    }

    private static <T> String getMethodNameForProxyOf(Class<T> clazz, List<Class<?>> interfaces) {
        String interfacesHash = Integer.toHexString(interfaces.hashCode());
        return clazz.getName().replace(".", "/") + "_" + interfacesHash + "_Proxy";
    }

    private static TypeId<?>[] classArrayToTypeArray(Class<?>[] input) {
        TypeId<?>[] result = new TypeId[input.length];
        for (int i = 0; i < input.length; ++i) {
            result[i] = TypeId.get(input[i]);
        }
        return result;
    }

    /**
     * Calculates the correct return statement code for a method.
     * <p>
     * A void method will not return anything.  A method that returns a primitive will need to
     * unbox the boxed result.  Otherwise we will cast the result.
     */
    // This one is tricky to fix, I gave up.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void generateCodeForReturnStatement(Code code, Class methodReturnType,
            Local localForResultOfInvoke, Local localOfMethodReturnType, Local aBoxedResult) {
        if (PRIMITIVE_TO_UNBOX_METHOD.containsKey(methodReturnType)) {
            code.cast(aBoxedResult, localForResultOfInvoke);
            MethodId unboxingMethodFor = getUnboxMethodForPrimitive(methodReturnType);
            code.invokeVirtual(unboxingMethodFor, localOfMethodReturnType, aBoxedResult);
            code.returnValue(localOfMethodReturnType);
        } else if (void.class.equals(methodReturnType)) {
            code.returnVoid();
        } else {
            code.cast(localOfMethodReturnType, localForResultOfInvoke);
            code.returnValue(localOfMethodReturnType);
        }
    }

    private static MethodId<?, ?> getUnboxMethodForPrimitive(Class<?> methodReturnType) {
        return PRIMITIVE_TO_UNBOX_METHOD.get(methodReturnType);
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_BOXED;
    static {
        PRIMITIVE_TO_BOXED = new HashMap<>();
        PRIMITIVE_TO_BOXED.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_BOXED.put(int.class, Integer.class);
        PRIMITIVE_TO_BOXED.put(byte.class, Byte.class);
        PRIMITIVE_TO_BOXED.put(long.class, Long.class);
        PRIMITIVE_TO_BOXED.put(short.class, Short.class);
        PRIMITIVE_TO_BOXED.put(float.class, Float.class);
        PRIMITIVE_TO_BOXED.put(double.class, Double.class);
        PRIMITIVE_TO_BOXED.put(char.class, Character.class);
    }

    private static final Map<TypeId<?>, MethodId<?, ?>> PRIMITIVE_TYPE_TO_UNBOX_METHOD;
    static {
        PRIMITIVE_TYPE_TO_UNBOX_METHOD = new HashMap<>();
        for (Map.Entry<Class<?>, Class<?>> entry : PRIMITIVE_TO_BOXED.entrySet()) {
            TypeId<?> primitiveType = TypeId.get(entry.getKey());
            TypeId<?> boxedType = TypeId.get(entry.getValue());
            MethodId<?, ?> valueOfMethod = boxedType.getMethod(boxedType, "valueOf", primitiveType);
            PRIMITIVE_TYPE_TO_UNBOX_METHOD.put(primitiveType, valueOfMethod);
        }
    }

    /**
     * Map from primitive type to method used to unbox a boxed version of the primitive.
     * <p>
     * This is required for methods whose return type is primitive, since the
     * {@link InvocationHandler} will return us a boxed result, and we'll need to convert it back to
     * primitive value.
     */
    private static final Map<Class<?>, MethodId<?, ?>> PRIMITIVE_TO_UNBOX_METHOD;
    static {
        Map<Class<?>, MethodId<?, ?>> map = new HashMap<>();
        map.put(boolean.class, TypeId.get(Boolean.class).getMethod(TypeId.BOOLEAN, "booleanValue"));
        map.put(int.class, TypeId.get(Integer.class).getMethod(TypeId.INT, "intValue"));
        map.put(byte.class, TypeId.get(Byte.class).getMethod(TypeId.BYTE, "byteValue"));
        map.put(long.class, TypeId.get(Long.class).getMethod(TypeId.LONG, "longValue"));
        map.put(short.class, TypeId.get(Short.class).getMethod(TypeId.SHORT, "shortValue"));
        map.put(float.class, TypeId.get(Float.class).getMethod(TypeId.FLOAT, "floatValue"));
        map.put(double.class, TypeId.get(Double.class).getMethod(TypeId.DOUBLE, "doubleValue"));
        map.put(char.class, TypeId.get(Character.class).getMethod(TypeId.CHAR, "charValue"));
        PRIMITIVE_TO_UNBOX_METHOD = map;
    }

    /**
     * Wrapper class to let us disambiguate {@link Method} objects.
     * <p>
     * The purpose of this class is to override the {@link #equals(Object)} and {@link #hashCode()}
     * methods so we can use a {@link Set} to remove duplicate methods that are overrides of one
     * another. For these purposes, we consider two methods to be equal if they have the same
     * name, return type, and parameter types.
     */
    public static class MethodSetEntry {
        public final String name;
        public final Class<?>[] paramTypes;
        public final Class<?> returnType;
        public final Method originalMethod;

        public MethodSetEntry(Method method) {
            originalMethod = method;
            name = method.getName();
            paramTypes = method.getParameterTypes();
            returnType = method.getReturnType();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MethodSetEntry) {
                MethodSetEntry other = (MethodSetEntry) o;
                return name.equals(other.name)
                        && returnType.equals(other.returnType)
                        && Arrays.equals(paramTypes, other.paramTypes);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result += 31 * result + name.hashCode();
            result += 31 * result + returnType.hashCode();
            result += 31 * result + Arrays.hashCode(paramTypes);
            return result;
        }
    }

    /**
     * A class that was already proxied.
     */
    private static class ProxiedClass<U> {
        final Class<U> clazz;

        final List<Class<?>> interfaces;

        /**
         * Class loader requested when the proxy class was generated. This might not be the
         * class loader of {@code clazz} as not all class loaders can be shared.
         *
         * @see DexMaker#generateClassLoader(File, File, ClassLoader)
         */
        final ClassLoader requestedClassloader;

        final boolean sharedClassLoader;

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            ProxiedClass<?> that = (ProxiedClass<?>) other;
            return clazz == that.clazz
                    && interfaces.equals(that.interfaces)
                    && requestedClassloader == that.requestedClassloader
                    && sharedClassLoader == that.sharedClassLoader;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode() + interfaces.hashCode() + requestedClassloader.hashCode()
                    + (sharedClassLoader ? 1 : 0); 
        }

        private ProxiedClass(Class<U> clazz, List<Class<?>> interfaces,
                             ClassLoader requestedClassloader, boolean sharedClassLoader) {
            this.clazz = clazz;
            this.interfaces = new ArrayList<>(interfaces);
            this.requestedClassloader = requestedClassloader;
            this.sharedClassLoader = sharedClassLoader;
        }
    }
}

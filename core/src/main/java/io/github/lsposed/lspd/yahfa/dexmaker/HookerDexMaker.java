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

package io.github.lsposed.lspd.yahfa.dexmaker;

import android.annotation.TargetApi;
import android.os.Build;

import io.github.lsposed.lspd.BuildConfig;
import io.github.lsposed.lspd.core.yahfa.HookMain;
import io.github.lsposed.lspd.nativebridge.Yahfa;
import io.github.lsposed.lspd.util.ProxyClassLoader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Map;

import dalvik.system.InMemoryDexClassLoader;
import de.robv.android.xposed.LspHooker;
import de.robv.android.xposed.XposedBridge;
import external.com.android.dx.Code;
import external.com.android.dx.DexMaker;
import external.com.android.dx.FieldId;
import external.com.android.dx.Local;
import external.com.android.dx.MethodId;
import external.com.android.dx.TypeId;

import static io.github.lsposed.lspd.config.LSPApplicationServiceClient.serviceClient;
import static io.github.lsposed.lspd.yahfa.dexmaker.DexMakerUtils.autoBoxIfNecessary;
import static io.github.lsposed.lspd.yahfa.dexmaker.DexMakerUtils.autoUnboxIfNecessary;
import static io.github.lsposed.lspd.yahfa.dexmaker.DexMakerUtils.canCache;
import static io.github.lsposed.lspd.yahfa.dexmaker.DexMakerUtils.createResultLocals;
import static io.github.lsposed.lspd.yahfa.dexmaker.DexMakerUtils.getObjTypeIdIfPrimitive;

@SuppressWarnings("rawtypes")
public class HookerDexMaker {

    public static final String METHOD_NAME_BACKUP = "backup";
    public static final String METHOD_NAME_HOOK = "hook";
    public static final String METHOD_NAME_SETUP = "setup";
    public static final TypeId<Object[]> objArrayTypeId = TypeId.get(Object[].class);
    private static final String CLASS_DESC_PREFIX = "L";
    /**
     * Note: this identifier is used in native codes to pass class access verification.
     */
    private static final String CLASS_NAME_PREFIX = "LspHooker_";
    private static final String FIELD_NAME_HOOKER = "hooker";
    private static final TypeId<LspHooker> hookerTypeId = TypeId.get(LspHooker.class);
    private static final MethodId<LspHooker, Object> handleHookedMethodMethodId =
            hookerTypeId.getMethod(TypeId.OBJECT, "handleHookedMethod", objArrayTypeId);

    private FieldId<?, LspHooker> mHookerFieldId;
    private Class<?> mReturnType;
    private Class<?>[] mActualParameterTypes;

    private TypeId<?> mHookerTypeId;
    private TypeId<?>[] mParameterTypeIds;
    private TypeId<?> mReturnTypeId;

    private DexMaker mDexMaker;
    private Member mMember;
    private XposedBridge.AdditionalHookInfo mHookInfo;
    private ClassLoader mAppClassLoader;
    private LspHooker mHooker;

    private static TypeId<?>[] getParameterTypeIds(Class<?>[] parameterTypes, boolean isStatic) {
        int parameterSize = parameterTypes.length;
        int targetParameterSize = isStatic ? parameterSize : parameterSize + 1;
        TypeId<?>[] parameterTypeIds = new TypeId<?>[targetParameterSize];
        int offset = 0;
        if (!isStatic) {
            parameterTypeIds[0] = TypeId.OBJECT;
            offset = 1;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypeIds[i + offset] = TypeId.get(parameterTypes[i]);
        }
        return parameterTypeIds;
    }

    private static Class<?>[] getParameterTypes(Executable method, boolean isStatic) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (isStatic) {
            return parameterTypes;
        }
        int parameterSize = parameterTypes.length;
        int targetParameterSize = parameterSize + 1;
        Class<?>[] newParameterTypes = new Class<?>[targetParameterSize];
        int offset = 1;
        newParameterTypes[0] = method.getDeclaringClass();
        System.arraycopy(parameterTypes, 0, newParameterTypes, offset, parameterTypes.length);
        return newParameterTypes;
    }

    public void start(Member member, XposedBridge.AdditionalHookInfo hookInfo,
                      ClassLoader appClassLoader) throws Exception {
        boolean isStatic;
        if (member instanceof Method) {
            Method method = (Method) member;
            isStatic = Modifier.isStatic(method.getModifiers());
            mReturnType = method.getReturnType();
            if (mReturnType.equals(Void.class) || mReturnType.equals(void.class)
                    || mReturnType.isPrimitive()) {
                mReturnTypeId = TypeId.get(mReturnType);
            } else {
                // all others fallback to plain Object for convenience
                mReturnTypeId = TypeId.OBJECT;
            }
            mParameterTypeIds = getParameterTypeIds(method.getParameterTypes(), isStatic);
            mActualParameterTypes = getParameterTypes(method, isStatic);
        } else if (member instanceof Constructor) {
            Constructor constructor = (Constructor) member;
            isStatic = false;
            mReturnTypeId = TypeId.VOID;
            mReturnType = void.class;
            mParameterTypeIds = getParameterTypeIds(constructor.getParameterTypes(), isStatic);
            mActualParameterTypes = getParameterTypes(constructor, isStatic);
        } else if (member.getDeclaringClass().isInterface()) {
            throw new IllegalArgumentException("Cannot hook interfaces: " + member.toString());
        } else if (Modifier.isAbstract(member.getModifiers())) {
            throw new IllegalArgumentException("Cannot hook abstract methods: " + member.toString());
        } else {
            throw new IllegalArgumentException("Only methods and constructors can be hooked: " + member.toString());
        }
        mMember = member;
        mHookInfo = hookInfo;
        if (appClassLoader == null
                || appClassLoader.getClass().getName().equals("java.lang.BootClassLoader")) {
            mAppClassLoader = getClass().getClassLoader();
        } else {
            mAppClassLoader = appClassLoader;
            mAppClassLoader = new ProxyClassLoader(mAppClassLoader, getClass().getClassLoader());
        }

        long startTime = System.nanoTime();
        doMake(member.getDeclaringClass().getName(), member instanceof Constructor ? "constructor" : member.getName());
        long endTime = System.nanoTime();
        DexLog.d("Hook time: " + (endTime - startTime) / 1e6 + "ms");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @TargetApi(Build.VERSION_CODES.O)
    private void doMake(String hookedClassName, String methodName) throws Exception {
        Class<?> hookClass = null;
        // Generate a Hooker class.
        String className = CLASS_NAME_PREFIX;
        hookClass = Yahfa.buildHooker(mAppClassLoader, mReturnType, mActualParameterTypes, methodName);
        if (canCache && hookClass == null) {
            methodName = METHOD_NAME_HOOK;
            String suffix = DexMakerUtils.getSha1Hex(mMember.toString());
            className = className + suffix;
            String dexFileName = className + ".jar";
            File dexFile = new File(serviceClient.getCachePath(dexFileName));
            DexLog.d("dex builder failed, generating " + dexFileName);
            mDexMaker = new DexMaker();
            // className is also used as dex file name
            // so it should be different from each other
            if (dexFile.exists()) {
                try {
                    // if file exists, reuse it and skip generating
                    DexLog.d("Using cache " + dexFileName);
                    ClassLoader loader = mDexMaker.loadClassDirect(mAppClassLoader, dexFile.getParentFile(), dexFileName);
                    hookClass = Class.forName(className.replace("/", "."), true, loader);
                } catch (Throwable ignored) {
                }
            }
            if (hookClass == null) {
                try {
                    DexLog.d("cache not hit, generating " + dexFileName);
                    doGenerate(className);
                    ClassLoader loader = mDexMaker.generateAndLoad(mAppClassLoader, dexFile.getParentFile(), dexFileName, true);
                    dexFile.setWritable(true, false);
                    dexFile.setReadable(true, false);
                    hookClass = Class.forName(className.replace("/", "."), true, loader);
                } catch (Throwable ignored) {
                }
            }
        }
        if (hookClass == null) {
            try {
                // do everything in memory
                DexLog.d("Falling back to generate in memory");
                if (BuildConfig.DEBUG)
                    className = className + hookedClassName.replace(".", "/");
                mDexMaker = new DexMaker();
                doGenerate(className);
                byte[] dexBytes = mDexMaker.generate();
                ClassLoader loader = new InMemoryDexClassLoader(ByteBuffer.wrap(dexBytes), mAppClassLoader);
                hookClass = Class.forName(className.replace("/", "."), true, loader);
            } catch (Throwable ignored) {
            }
        }

        if (hookClass == null) {
            DexLog.e("Unable to generate hooker class. This should not happen. Skipping...");
            return;
        }
        // Execute our newly-generated code in-process.
        Method backupMethod = hookClass.getMethod(METHOD_NAME_BACKUP, mActualParameterTypes);
        mHooker = new LspHooker(mHookInfo, mMember, backupMethod);
        hookClass.getMethod(METHOD_NAME_SETUP, LspHooker.class).invoke(null, mHooker);
        Method hookMethod = hookClass.getMethod(methodName, mActualParameterTypes);
        HookMain.backupAndHook(mMember, hookMethod, backupMethod);
    }

    private void doGenerate(String className) {
        String classDesc = CLASS_DESC_PREFIX + className + ";";
        mHookerTypeId = TypeId.get(classDesc);
        mDexMaker.declare(mHookerTypeId, className + ".generated", Modifier.PUBLIC, TypeId.OBJECT);
        generateFields();
        generateSetupMethod();
        generateBackupMethod();
        generateHookMethod();
    }

    public LspHooker getHooker() {
        return mHooker;
    }

    private void generateFields() {
        mHookerFieldId = mHookerTypeId.getField(hookerTypeId, FIELD_NAME_HOOKER);
        mDexMaker.declare(mHookerFieldId, Modifier.STATIC, null);
    }

    private void generateSetupMethod() {
        MethodId<?, Void> setupMethodId = mHookerTypeId.getMethod(
                TypeId.VOID, METHOD_NAME_SETUP, hookerTypeId);
        Code code = mDexMaker.declare(setupMethodId, Modifier.PUBLIC | Modifier.STATIC);
        // init logic
        // get parameters
        Local<LspHooker> hooker = code.getParameter(0, hookerTypeId);
        // save params to static
        code.sput(mHookerFieldId, hooker);
        code.returnVoid();
    }

    private void generateBackupMethod() {
        MethodId<?, ?> backupMethodId = mHookerTypeId.getMethod(mReturnTypeId, METHOD_NAME_BACKUP, mParameterTypeIds);
        Code code = mDexMaker.declare(backupMethodId, Modifier.PUBLIC | Modifier.STATIC);
        Map<TypeId, Local> resultLocals = createResultLocals(code);
        // do nothing
        if (mReturnTypeId.equals(TypeId.VOID)) {
            code.returnVoid();
        } else {
            // we have limited the returnType to primitives or Object, so this should be safe
            code.returnValue(resultLocals.get(mReturnTypeId));
        }
    }

    private void generateHookMethod() {
        MethodId<?, ?> hookMethodId = mHookerTypeId.getMethod(mReturnTypeId, METHOD_NAME_HOOK, mParameterTypeIds);
        Code code = mDexMaker.declare(hookMethodId, Modifier.PUBLIC | Modifier.STATIC);

        // code starts
        // prepare locals
        Local<LspHooker> hooker = code.newLocal(hookerTypeId);
        Local<Object> resultObj = code.newLocal(TypeId.OBJECT); // as a temp Local
        Local<Object[]> args = code.newLocal(objArrayTypeId);

        Local<Integer> actualParamSize = code.newLocal(TypeId.INT);
        Local<Integer> argIndex = code.newLocal(TypeId.INT);

        Local[] allArgsLocals = createParameterLocals(code);

        Map<TypeId, Local> resultLocals = createResultLocals(code);

        code.loadConstant(args, null);
        code.loadConstant(argIndex, 0);

        code.sget(mHookerFieldId, hooker);

        // start hooking
        // prepare hooking locals
        int paramsSize = mParameterTypeIds.length;
        code.loadConstant(actualParamSize, paramsSize);
        code.newArray(args, actualParamSize);
        for (int i = 0; i < paramsSize; i++) {
            Local parameter = allArgsLocals[i];
            // save parameter to resultObj as Object
            autoBoxIfNecessary(code, resultObj, parameter);
            code.loadConstant(argIndex, i);
            // save Object to args
            code.aput(args, argIndex, resultObj);
        }
        // handleHookedMethod
        if (mReturnTypeId.equals(TypeId.VOID)) {
            code.invokeVirtual(handleHookedMethodMethodId, null, hooker, args);
            code.returnVoid();
        } else {
            // hooker always return an Object, so save to resultObj
            code.invokeVirtual(handleHookedMethodMethodId, resultObj, hooker, args);
            // have to unbox it if returnType is primitive
            // casting Object
            TypeId objTypeId = getObjTypeIdIfPrimitive(mReturnTypeId);
            Local matchObjLocal = resultLocals.get(objTypeId);
            code.cast(matchObjLocal, resultObj);
            // have to use matching typed Object(Integer, Double ...) to do unboxing
            Local toReturn = resultLocals.get(mReturnTypeId);
            autoUnboxIfNecessary(code, toReturn, matchObjLocal, resultLocals, true);
            // return
            code.returnValue(toReturn);
        }
    }

    private Local[] createParameterLocals(Code code) {
        Local[] paramLocals = new Local[mParameterTypeIds.length];
        for (int i = 0; i < mParameterTypeIds.length; i++) {
            paramLocals[i] = code.getParameter(i, mParameterTypeIds[i]);
        }
        return paramLocals;
    }
}

package com.elderdrivers.riru.edxp.yahfa.dexmaker;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;

import com.elderdrivers.riru.edxp.config.ConfigManager;
import com.elderdrivers.riru.edxp.core.yahfa.HookMain;
import com.elderdrivers.riru.edxp.util.ProxyClassLoader;
import com.elderdrivers.riru.edxp.yahfa.BuildConfig;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import dalvik.system.InMemoryDexClassLoader;
import de.robv.android.xposed.EdHooker;
import de.robv.android.xposed.XposedBridge;
import external.com.android.dx.Code;
import external.com.android.dx.DexMaker;
import external.com.android.dx.FieldId;
import external.com.android.dx.Local;
import external.com.android.dx.MethodId;
import external.com.android.dx.TypeId;
import pxb.android.arsc.Config;

import static com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils.autoBoxIfNecessary;
import static com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils.autoUnboxIfNecessary;
import static com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils.canCache;
import static com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils.createResultLocals;
import static com.elderdrivers.riru.edxp.yahfa.dexmaker.DexMakerUtils.getObjTypeIdIfPrimitive;

public class HookerDexMaker {

    public static final String METHOD_NAME_BACKUP = "backup";
    public static final String METHOD_NAME_HOOK = "hook";
    public static final String METHOD_NAME_SETUP = "setup";
    public static final TypeId<Object[]> objArrayTypeId = TypeId.get(Object[].class);
    private static final String CLASS_DESC_PREFIX = "L";
    /**
     * Note: this identifier is used in native codes to pass class access verification.
     */
    private static final String CLASS_NAME_PREFIX = "EdHooker_";
    private static final String FIELD_NAME_HOOKER = "hooker";
    private static final TypeId<EdHooker> hookerTypeId = TypeId.get(EdHooker.class);
    private static final MethodId<EdHooker, Object> handleHookedMethodMethodId =
            hookerTypeId.getMethod(TypeId.OBJECT, "handleHookedMethod", objArrayTypeId);

    private static AtomicLong sClassNameSuffix = new AtomicLong(1);

    private FieldId<?, EdHooker> mHookerFieldId;
    private MethodId<?, ?> mBackupMethodId;
    private MethodId<?, ?> mCallBackupMethodId;
    private MethodId<?, ?> mHookMethodId;

    private TypeId<?> mHookerTypeId;
    private TypeId<?>[] mParameterTypeIds;
    private Class<?>[] mActualParameterTypes;
    private Class<?> mReturnType;
    private TypeId<?> mReturnTypeId;
    private boolean mIsStatic;

    private DexMaker mDexMaker;
    private Member mMember;
    private XposedBridge.AdditionalHookInfo mHookInfo;
    private ClassLoader mAppClassLoader;
    private Class<?> mHookClass;
    private Method mHookMethod;
    private Method mBackupMethod;
    private EdHooker mHooker;

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

    private static Class<?>[] getParameterTypes(Class<?>[] parameterTypes, boolean isStatic) {
        if (isStatic) {
            return parameterTypes;
        }
        int parameterSize = parameterTypes.length;
        int targetParameterSize = parameterSize + 1;
        Class<?>[] newParameterTypes = new Class<?>[targetParameterSize];
        int offset = 1;
        newParameterTypes[0] = Object.class;
        System.arraycopy(parameterTypes, 0, newParameterTypes, offset, parameterTypes.length);
        return newParameterTypes;
    }

    public void start(Member member, XposedBridge.AdditionalHookInfo hookInfo,
                      ClassLoader appClassLoader) throws Exception {
        if (member instanceof Method) {
            Method method = (Method) member;
            mIsStatic = Modifier.isStatic(method.getModifiers());
            mReturnType = method.getReturnType();
            if (mReturnType.equals(Void.class) || mReturnType.equals(void.class)
                    || mReturnType.isPrimitive()) {
                mReturnTypeId = TypeId.get(mReturnType);
            } else {
                // all others fallback to plain Object for convenience
                mReturnType = Object.class;
                mReturnTypeId = TypeId.OBJECT;
            }
            mParameterTypeIds = getParameterTypeIds(method.getParameterTypes(), mIsStatic);
            mActualParameterTypes = getParameterTypes(method.getParameterTypes(), mIsStatic);
        } else if (member instanceof Constructor) {
            Constructor constructor = (Constructor) member;
            mIsStatic = false;
            mReturnType = void.class;
            mReturnTypeId = TypeId.VOID;
            mParameterTypeIds = getParameterTypeIds(constructor.getParameterTypes(), mIsStatic);
            mActualParameterTypes = getParameterTypes(constructor.getParameterTypes(), mIsStatic);
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
        doMake(member.getDeclaringClass().getName());
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void doMake(String hookedClassName) throws Exception {
        mDexMaker = new DexMaker();
        ClassLoader loader = null;
        // Generate a Hooker class.
        String className = CLASS_NAME_PREFIX;
        boolean usedCache = false;
        if (canCache) {
            try {
                // className is also used as dex file name
                // so it should be different from each other
                String suffix = DexMakerUtils.getSha1Hex(mMember.toString());
                className = className + suffix;
                String dexFileName = className + ".jar";
                File dexFile = new File(ConfigManager.getCachePath(dexFileName));
                if (!dexFile.exists()) {
                    // if file exists, reuse it and skip generating
                    DexLog.d("Generating " + dexFileName);
                    doGenerate(className);
                    loader = mDexMaker.generateAndLoad(mAppClassLoader, new File(ConfigManager.getCachePath("")), dexFileName, false);
                    dexFile.setWritable(true, false);
                    dexFile.setReadable(true, false);
                } else {
                    DexLog.d("Using cache " + dexFileName);
                    loader = mDexMaker.loadClassDirect(mAppClassLoader, new File(ConfigManager.getCachePath("")), dexFileName);
                }
                usedCache = true;
            } catch (Throwable ignored) {}
        }
        if (!usedCache) {
            // do everything in memory
            DexLog.d("Generating in memory");
            if(BuildConfig.DEBUG)
                className = className + hookedClassName.replace(".", "/");
            doGenerate(className);
            byte[] dexBytes = mDexMaker.generate();
            loader = new InMemoryDexClassLoader(ByteBuffer.wrap(dexBytes), mAppClassLoader);
        }

        mHookClass = Class.forName(className.replace("/", "."), true, loader);
        // Execute our newly-generated code in-process.
        mBackupMethod = mHookClass.getMethod(METHOD_NAME_BACKUP, mActualParameterTypes);
        mHooker = new EdHooker(mHookInfo, mMember, mBackupMethod, mIsStatic);
        mHookClass.getMethod(METHOD_NAME_SETUP, EdHooker.class).invoke(null, mHooker);
        mHookMethod = mHookClass.getMethod(METHOD_NAME_HOOK, mActualParameterTypes);
        HookMain.backupAndHook(mMember, mHookMethod, mBackupMethod);
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

    public Method getHookMethod() {
        return mHookMethod;
    }

    public Method getBackupMethod() {
        return mBackupMethod;
    }

    public Class getHookClass() {
        return mHookClass;
    }

    public EdHooker getHooker() {return mHooker;}

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
        Local<EdHooker> hooker = code.getParameter(0, hookerTypeId);
        // save params to static
        code.sput(mHookerFieldId, hooker);
        code.returnVoid();
    }

    private void generateBackupMethod() {
        mBackupMethodId = mHookerTypeId.getMethod(mReturnTypeId, METHOD_NAME_BACKUP, mParameterTypeIds);
        Code code = mDexMaker.declare(mBackupMethodId, Modifier.PUBLIC | Modifier.STATIC);
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
        mHookMethodId = mHookerTypeId.getMethod(mReturnTypeId, METHOD_NAME_HOOK, mParameterTypeIds);
        Code code = mDexMaker.declare(mHookMethodId, Modifier.PUBLIC | Modifier.STATIC);

        // code starts
        // prepare locals
        Local<EdHooker> hooker = code.newLocal(hookerTypeId);
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

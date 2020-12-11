package com.swift.sandhook.xposedcompat.hookstub;

import android.util.Log;

import com.elderdrivers.riru.edxp.sandhook.BuildConfig;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.SandHookMethodResolver;
import com.swift.sandhook.utils.ParamWrapper;
import com.swift.sandhook.wrapper.StubMethodsFactory;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.swift.sandhook.xposedcompat.utils.DexLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HookStubManager {

    public static volatile boolean is64Bit;
    //64bits arg0 - arg7 is in reg x1 - x7 and > 7 is in stack, but can not match
    public final static int MAX_64_ARGS = 7;

    public static int MAX_STUB_ARGS = 0;

    public static int[] stubSizes;

    public static boolean hasStubBackup;

    public static AtomicInteger[] curUseStubIndexes;

    public static int ALL_STUB = 0;

    public static Member[] originMethods;
    public static HookMethodEntity[] hookMethodEntities;
    public static XposedBridge.AdditionalHookInfo[] additionalHookInfos;

    static {
        is64Bit = SandHook.is64Bit();
        Class stubClass = is64Bit ? MethodHookerStubs64.class : MethodHookerStubs32.class;
        stubSizes = (int[]) XposedHelpers.getStaticObjectField(stubClass, "stubSizes");
        Boolean hasBackup = (Boolean) XposedHelpers.getStaticObjectField(stubClass, "hasStubBackup");
        hasStubBackup = hasBackup != null && (hasBackup && !XposedCompat.useNewCallBackup);
        if (stubSizes != null && stubSizes.length > 0) {
            MAX_STUB_ARGS = stubSizes.length - 1;
            curUseStubIndexes = new AtomicInteger[MAX_STUB_ARGS + 1];
            for (int i = 0; i < MAX_STUB_ARGS + 1; i++) {
                curUseStubIndexes[i] = new AtomicInteger(0);
                ALL_STUB += stubSizes[i];
            }
            originMethods = new Member[ALL_STUB];
            hookMethodEntities = new HookMethodEntity[ALL_STUB];
            additionalHookInfos = new XposedBridge.AdditionalHookInfo[ALL_STUB];
        }
    }


    public static HookMethodEntity getHookMethodEntity(Member origin, XposedBridge.AdditionalHookInfo additionalHookInfo) {

        if (!support()) {
            return null;
        }

        Class[] parType;
        Class retType;
        boolean isStatic = Modifier.isStatic(origin.getModifiers());

        if (origin instanceof Method) {
            Method method = (Method) origin;
            retType = method.getReturnType();
            parType = method.getParameterTypes();
        } else if (origin instanceof Constructor) {
            Constructor constructor = (Constructor) origin;
            retType = Void.TYPE;
            parType = constructor.getParameterTypes();
        } else {
            return null;
        }

        if (!ParamWrapper.support(retType))
            return null;

        int needStubArgCount = isStatic ? 0 : 1;

        if (parType != null) {
            needStubArgCount += parType.length;
            if (needStubArgCount > MAX_STUB_ARGS)
                return null;
            if (is64Bit && needStubArgCount > MAX_64_ARGS)
                return null;
            for (Class par:parType) {
                if (!ParamWrapper.support(par))
                    return null;
            }
        } else {
            parType = new Class[0];
        }

        synchronized (HookStubManager.class) {
            StubMethodsInfo stubMethodInfo = getStubMethodPair(is64Bit, needStubArgCount);
            if (stubMethodInfo == null)
                return null;
            HookMethodEntity entity = new HookMethodEntity(origin, stubMethodInfo.hook, stubMethodInfo.backup);
            entity.retType = retType;
            entity.parType = parType;
            if (hasStubBackup && !tryCompileAndResolveCallOriginMethod(entity.backup, stubMethodInfo.args, stubMethodInfo.index)) {
                DexLog.w("internal stub <" + entity.hook.getName() + "> call origin compile failure, skip use internal stub");
                return null;
            } else {
                int id = getMethodId(stubMethodInfo.args, stubMethodInfo.index);
                originMethods[id] = origin;
                hookMethodEntities[id] = entity;
                additionalHookInfos[id] = additionalHookInfo;
                return entity;
            }
        }
    }

    public static int getMethodId(int args, int index) {
        int id = index;
        for (int i = 0;i < args;i++) {
            id += stubSizes[i];
        }
        return id;
    }

    public static String getHookMethodName(int index) {
        return "stub_hook_" + index;
    }

    public static String getBackupMethodName(int index) {
        return "stub_backup_" + index;
    }

    public static String getCallOriginClassName(int args, int index) {
        return "call_origin_" + args + "_" + index;
    }


    static class StubMethodsInfo {
        int args = 0;
        int index = 0;
        Method hook;
        Method backup;

        public StubMethodsInfo(int args, int index, Method hook, Method backup) {
            this.args = args;
            this.index = index;
            this.hook = hook;
            this.backup = backup;
        }
    }

    private static synchronized StubMethodsInfo getStubMethodPair(boolean is64Bit, int stubArgs) {

        stubArgs = getMatchStubArgsCount(stubArgs);

        if (stubArgs < 0)
            return null;

        int curUseStubIndex = curUseStubIndexes[stubArgs].getAndIncrement();
        Class[] pars = getFindMethodParTypes(is64Bit, stubArgs);
        try {
            if (is64Bit) {
                Method hook = MethodHookerStubs64.class.getDeclaredMethod(getHookMethodName(curUseStubIndex), pars);
                Method backup = hasStubBackup ? MethodHookerStubs64.class.getDeclaredMethod(getBackupMethodName(curUseStubIndex), pars) : StubMethodsFactory.getStubMethod();
                if (hook == null || backup == null)
                    return null;
                return new StubMethodsInfo(stubArgs, curUseStubIndex, hook, backup);
            } else {
                Method hook = MethodHookerStubs32.class.getDeclaredMethod(getHookMethodName(curUseStubIndex), pars);
                Method backup = hasStubBackup ? MethodHookerStubs32.class.getDeclaredMethod(getBackupMethodName(curUseStubIndex), pars) : StubMethodsFactory.getStubMethod();
                if (hook == null || backup == null)
                    return null;
                return new StubMethodsInfo(stubArgs, curUseStubIndex, hook, backup);
            }
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static Method getCallOriginMethod(int args, int index) {
        Class stubClass = is64Bit ? MethodHookerStubs64.class : MethodHookerStubs32.class;
        String className = stubClass.getName();
        className += "$";
        className += getCallOriginClassName(args, index);
        try {
            Class callOriginClass = Class.forName(className, true, stubClass.getClassLoader());
            return callOriginClass.getDeclaredMethod("call", long[].class);
        } catch (Throwable e) {
            Log.e("HookStubManager", "load call origin class error!", e);
            return null;
        }
    }

    public static boolean tryCompileAndResolveCallOriginMethod(Method backupMethod, int args, int index) {
        Method method = getCallOriginMethod(args, index);
        if (method != null) {
            SandHookMethodResolver.resolveMethod(method, backupMethod);
            return SandHook.compileMethod(method);
        } else {
            return false;
        }
    }

    public static int getMatchStubArgsCount(int stubArgs) {
        for (int i = stubArgs;i <= MAX_STUB_ARGS;i++) {
            if (curUseStubIndexes[i].get() < stubSizes[i])
                return i;
        }
        return -1;
    }

    public static Class[] getFindMethodParTypes(boolean is64Bit, int stubArgs) {
        if (stubArgs == 0)
            return null;
        Class[] args = new Class[stubArgs];
        if (is64Bit) {
            for (int i = 0;i < stubArgs;i++) {
                args[i] = long.class;
            }
        } else {
            for (int i = 0;i < stubArgs;i++) {
                args[i] = int.class;
            }
        }
        return args;
    }

    public static long hookBridge(int id, CallOriginCallBack callOrigin, long... stubArgs) throws Throwable {

        Member originMethod = originMethods[id];
        HookMethodEntity entity = hookMethodEntities[id];

        Object thiz = null;
        Object[] args = null;

        if (hasArgs(stubArgs)) {
            thiz = entity.getThis(stubArgs[0]);
            args = entity.getArgs(stubArgs);
        }

        if (thiz == null)
        {
            thiz = originMethod.getDeclaringClass();
        }

        if (XposedBridge.disableHooks) {
            if (hasStubBackup) {
                return callOrigin.call(stubArgs);
            } else {
                return callOrigin(entity, originMethod, thiz, args);
            }
        }

        DexLog.printMethodHookIn(originMethod);

        Object[] snapshot = additionalHookInfos[id].callbacks.getSnapshot();

        if (snapshot == null || snapshot.length == 0) {
            if (hasStubBackup) {
                return callOrigin.call(stubArgs);
            } else {
                return callOrigin(entity, originMethod, thiz, args);
            }
        }

        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();

        param.method = originMethod;
        param.thisObject = thiz;
        param.args = args;

        int beforeIdx = 0;
        do {
            try {
                ((XC_MethodHook) snapshot[beforeIdx]).callBeforeHookedMethod(param);
            } catch (Throwable t) {
                // reset result (ignoring what the unexpectedly exiting callback did)
                if( BuildConfig.DEBUG ) XposedBridge.log(t);
                param.setResult(null);
                param.returnEarly = false;
                continue;
            }

            if (param.returnEarly) {
                // skip remaining "before" callbacks and corresponding "after" callbacks
                beforeIdx++;
                break;
            }
        } while (++beforeIdx < snapshot.length);

        // call original method if not requested otherwise
        if (!param.returnEarly) {
            try {
                if (hasStubBackup) {
                    //prepare new args
                    long[] newArgs = entity.getArgsAddress(stubArgs, param.args);
                    param.setResult(entity.getResult(callOrigin.call(newArgs)));
                } else {
                    param.setResult(SandHook.callOriginMethod(originMethod, entity.backup, thiz, param.args));
                }
            } catch (Throwable e) {
                if( BuildConfig.DEBUG ) XposedBridge.log(e);
                param.setThrowable(e);
            }
        }

        // call "after method" callbacks
        int afterIdx = beforeIdx - 1;
        do {
            Object lastResult =  param.getResult();
            Throwable lastThrowable = param.getThrowable();

            try {
                ((XC_MethodHook) snapshot[afterIdx]).callAfterHookedMethod(param);
            } catch (Throwable t) {
                if( BuildConfig.DEBUG ) XposedBridge.log(t);
                if (lastThrowable == null)
                    param.setResult(lastResult);
                else
                    param.setThrowable(lastThrowable);
            }
        } while (--afterIdx >= 0);
        if (!param.hasThrowable()) {
            return entity.getResultAddress(param.getResult());
        } else {
            throw param.getThrowable();
        }
    }

    public static Object hookBridge(Member origin, Method backup, XposedBridge.AdditionalHookInfo additionalHookInfo, Object thiz, Object... args) throws Throwable {


        if (XposedBridge.disableHooks) {
            return SandHook.callOriginMethod(true, origin, backup, thiz, args);
        }

        DexLog.printMethodHookIn(origin);

        Object[] snapshot = additionalHookInfo.callbacks.getSnapshot();

        if (snapshot == null || snapshot.length == 0) {
            return SandHook.callOriginMethod(origin, backup, thiz, args);
        }

        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();

        param.method = origin;
        param.thisObject = thiz;
        param.args = args;

        int beforeIdx = 0;
        do {
            try {
                ((XC_MethodHook) snapshot[beforeIdx]).callBeforeHookedMethod(param);
            } catch (Throwable t) {
                if( BuildConfig.DEBUG ) XposedBridge.log(t);
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
        } while (++beforeIdx < snapshot.length);

        // call original method if not requested otherwise
        if (!param.returnEarly) {
            try {
                param.setResult(SandHook.callOriginMethod(true, origin, backup, thiz, param.args));
            } catch (Throwable e) {
                if( BuildConfig.DEBUG ) XposedBridge.log(e);
                param.setThrowable(e);
            }
        }

        // call "after method" callbacks
        int afterIdx = beforeIdx - 1;
        do {
            Object lastResult =  param.getResult();
            Throwable lastThrowable = param.getThrowable();

            try {
                ((XC_MethodHook) snapshot[afterIdx]).callAfterHookedMethod(param);
            } catch (Throwable t) {
                if( BuildConfig.DEBUG ) XposedBridge.log(t);
                if (lastThrowable == null)
                    param.setResult(lastResult);
                else
                    param.setThrowable(lastThrowable);
            }
        } while (--afterIdx >= 0);
        if (!param.hasThrowable()) {
            return param.getResult();
        } else {
            throw param.getThrowable();
        }
    }

    public final static long callOrigin(HookMethodEntity entity, Member origin, Object thiz, Object[] args) throws Throwable {
        Object res = SandHook.callOriginMethod(true, origin, entity.backup, thiz, args);
        return entity.getResultAddress(res);
    }

    private static boolean hasArgs(long... args) {
        return args != null && args.length > 0;
    }

    public static boolean support() {
        return MAX_STUB_ARGS > 0 && SandHook.canGetObject() && SandHook.canGetObjectAddress();
    }

}

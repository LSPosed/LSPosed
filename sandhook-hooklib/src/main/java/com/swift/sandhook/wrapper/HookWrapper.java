package com.swift.sandhook.wrapper;

import android.text.TextUtils;

import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.HookReflectClass;
import com.swift.sandhook.annotation.MethodParams;
import com.swift.sandhook.annotation.MethodReflectParams;
import com.swift.sandhook.annotation.Param;
import com.swift.sandhook.annotation.SkipParamCheck;
import com.swift.sandhook.annotation.ThisObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class HookWrapper {

    public static void addHookClass(Class<?>... classes) throws HookErrorException {
        addHookClass(null, classes);
    }

    public static void addHookClass(ClassLoader classLoader, Class<?>... classes) throws HookErrorException {
        for (Class clazz:classes) {
            addHookClass(classLoader, clazz);
        }
    }

    public static void addHookClass(ClassLoader classLoader, Class<?> clazz) throws HookErrorException {
        Class targetHookClass = getTargetHookClass(classLoader, clazz);
        if (targetHookClass == null)
            throw new HookErrorException("error hook wrapper class :" + clazz.getName());
        Map<Member,HookEntity> hookEntityMap = getHookMethods(classLoader, targetHookClass, clazz);
        try {
            fillBackupMethod(classLoader, clazz, hookEntityMap);
        } catch (Throwable throwable) {
            throw new HookErrorException("fillBackupMethod error!", throwable);
        }
        for (HookEntity entity:hookEntityMap.values()) {
            SandHook.hook(entity);
        }
    }

    private static void fillBackupMethod(ClassLoader classLoader,Class<?> clazz, Map<Member, HookEntity> hookEntityMap) {
        Field[] fields = null;
        try {
            fields = clazz.getDeclaredFields();
        } catch (Throwable throwable) {}
        if (fields == null || fields.length == 0)
            return;
        if (hookEntityMap.isEmpty())
            return;
        for (Field field:fields) {
            if (!Modifier.isStatic(field.getModifiers()))
                continue;
            HookMethodBackup hookMethodBackup = field.getAnnotation(HookMethodBackup.class);
            if (hookMethodBackup == null)
                continue;
            for (HookEntity hookEntity:hookEntityMap.values()) {
                if (TextUtils.equals(hookEntity.isCtor() ? "<init>" : hookEntity.target.getName(), hookMethodBackup.value()) && samePars(classLoader, field, hookEntity.pars)) {
                    field.setAccessible(true);
                    if (hookEntity.backup == null) {
                        hookEntity.backup = StubMethodsFactory.getStubMethod();
                        hookEntity.hookIsStub = true;
                        hookEntity.resolveDexCache = false;
                    }
                    if (hookEntity.backup == null)
                        continue;
                    try {
                        if (field.getType() == Method.class) {
                            field.set(null, hookEntity.backup);
                        } else if (field.getType() == HookEntity.class) {
                            field.set(null, hookEntity);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static Map<Member, HookEntity> getHookMethods(ClassLoader classLoader, Class targetHookClass, Class<?> hookWrapperClass) throws HookErrorException {
        Map<Member,HookEntity> hookEntityMap = new HashMap<>();
        Method[] methods = null;
        try {
            methods = hookWrapperClass.getDeclaredMethods();
        } catch (Throwable throwable) {
        }
        if (methods == null || methods.length == 0)
            throw new HookErrorException("error hook wrapper class :" + targetHookClass.getName());
        for (Method method:methods) {
            HookMethod hookMethodAnno = method.getAnnotation(HookMethod.class);
            HookMethodBackup hookMethodBackupAnno = method.getAnnotation(HookMethodBackup.class);
            String methodName;
            Member foundMethod;
            Class[] pars;
            if (hookMethodAnno != null) {
                methodName = hookMethodAnno.value();
                pars = parseMethodPars(classLoader, method);
                try {
                    if (methodName.equals("<init>")) {
                        foundMethod = targetHookClass.getConstructor(pars);
                    } else {
                        foundMethod = targetHookClass.getDeclaredMethod(methodName, pars);
                    }
                } catch (NoSuchMethodException e) {
                    throw new HookErrorException("can not find target method: " + methodName, e);
                }
                if (!method.isAnnotationPresent(SkipParamCheck.class)) {
                    checkSignature(foundMethod, method, pars);
                }
                HookEntity entity = hookEntityMap.get(foundMethod);
                if (entity == null) {
                    entity = new HookEntity(foundMethod);
                    hookEntityMap.put(foundMethod, entity);
                }
                entity.pars = pars;
                entity.hook = method;
            } else if (hookMethodBackupAnno != null) {
                methodName = hookMethodBackupAnno.value();
                pars = parseMethodPars(classLoader, method);
                try {
                    if (methodName.equals("<init>")) {
                        foundMethod = targetHookClass.getConstructor(pars);
                    } else {
                        foundMethod = targetHookClass.getDeclaredMethod(methodName, pars);
                    }
                } catch (NoSuchMethodException e) {
                    throw new HookErrorException("can not find target method: " + methodName, e);
                }
                if (!method.isAnnotationPresent(SkipParamCheck.class)) {
                    checkSignature(foundMethod, method, pars);
                }
                HookEntity entity = hookEntityMap.get(foundMethod);
                if (entity == null) {
                    entity = new HookEntity(foundMethod);
                    hookEntityMap.put(foundMethod, entity);
                }
                entity.pars = pars;
                entity.backup = method;
            } else {
                continue;
            }
        }
        return hookEntityMap;
    }

    private static Class[] parseMethodPars(ClassLoader classLoader, Method method) throws HookErrorException {
        MethodParams methodParams = method.getAnnotation(MethodParams.class);
        MethodReflectParams methodReflectParams = method.getAnnotation(MethodReflectParams.class);
        if (methodParams != null) {
            return methodParams.value();
        } else if (methodReflectParams != null) {
            if (methodReflectParams.value().length == 0)
                return null;
            Class[] pars = new Class[methodReflectParams.value().length];
            for (int i = 0;i < methodReflectParams.value().length; i++) {
                try {
                    pars[i] = classNameToClass(methodReflectParams.value()[i], classLoader);
                } catch (ClassNotFoundException e) {
                    throw new HookErrorException("hook method pars error: " + method.getName(), e);
                }
            }
            return pars;
        } else if (getParsCount(method) > 0) {
            if (getParsCount(method) == 1) {
                if (hasThisObject(method)) {
                    return parseMethodParsNew(classLoader, method);
                } else {
                    return null;
                }
            } else {
                return parseMethodParsNew(classLoader, method);
            }
        } else {
            return null;
        }
    }

    private static Class[] parseMethodPars(ClassLoader classLoader, Field field) throws HookErrorException {
        MethodParams methodParams = field.getAnnotation(MethodParams.class);
        MethodReflectParams methodReflectParams = field.getAnnotation(MethodReflectParams.class);
        if (methodParams != null) {
            return methodParams.value();
        } else if (methodReflectParams != null) {
            if (methodReflectParams.value().length == 0)
                return null;
            Class[] pars = new Class[methodReflectParams.value().length];
            for (int i = 0;i < methodReflectParams.value().length; i++) {
                try {
                    pars[i] = classNameToClass(methodReflectParams.value()[i], classLoader);
                } catch (ClassNotFoundException e) {
                    throw new HookErrorException("hook method pars error: " + field.getName(), e);
                }
            }
            return pars;
        } else {
            return null;
        }
    }

    private static Class[] parseMethodParsNew(ClassLoader classLoader, Method method) throws HookErrorException {
        Class[] hookMethodPars = method.getParameterTypes();
        if (hookMethodPars == null || hookMethodPars.length == 0)
            return null;
        Annotation[][] annotations = method.getParameterAnnotations();
        Class[] realPars = null;
        int parIndex = 0;
        for (int i = 0;i < annotations.length;i ++) {
            Class hookPar = hookMethodPars[i];
            Annotation[] methodAnnos = annotations[i];
            if (i == 0) {
                //check thisObject
                if (isThisObject(methodAnnos)) {
                    realPars = new Class[annotations.length - 1];
                    continue;
                } else {
                    //static method
                    realPars = new Class[annotations.length];
                }
            }
            try {
                realPars[parIndex] = getRealParType(classLoader, hookPar, methodAnnos, method.isAnnotationPresent(SkipParamCheck.class));
            } catch (Exception e) {
                throw new HookErrorException("hook method <" + method.getName() + "> parser pars error", e);
            }
            parIndex++;
        }
        return realPars;
    }

    private static Class getRealParType(ClassLoader classLoader, Class hookPar, Annotation[] annotations, boolean skipCheck) throws Exception {
        if (annotations == null || annotations.length == 0)
            return hookPar;
        for (Annotation annotation:annotations) {
            if (annotation instanceof Param) {
                Param param = (Param) annotation;
                if (TextUtils.isEmpty(param.value()))
                    return hookPar;
                Class realPar = classNameToClass(param.value(), classLoader);
                if (skipCheck || realPar.equals(hookPar) || hookPar.isAssignableFrom(realPar)) {
                    return realPar;
                } else {
                    throw new ClassCastException("hook method par cast error!");
                }
            }
        }
        return hookPar;
    }

    private static boolean hasThisObject(Method method) {
        Annotation[][] annotations = method.getParameterAnnotations();
        if (annotations == null || annotations.length == 0)
            return false;
        Annotation[] thisParAnno = annotations[0];
        return isThisObject(thisParAnno);
    }

    private static boolean isThisObject(Annotation[] annotations) {
        if (annotations == null || annotations.length == 0)
            return false;
        for (Annotation annotation:annotations) {
            if (annotation instanceof ThisObject)
                return true;
        }
        return false;
    }

    private static int getParsCount(Method method) {
        Class[] hookMethodPars = method.getParameterTypes();
        return hookMethodPars == null ? 0 : hookMethodPars.length;
    }

    private static Class classNameToClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
        Class clazz;
        switch (name) {
            case MethodReflectParams.BOOLEAN:
                clazz = boolean.class;
                break;
            case MethodReflectParams.BYTE:
                clazz = byte.class;
                break;
            case MethodReflectParams.CHAR:
                clazz = char.class;
                break;
            case MethodReflectParams.DOUBLE:
                clazz = double.class;
                break;
            case MethodReflectParams.FLOAT:
                clazz = float.class;
                break;
            case MethodReflectParams.INT:
                clazz = int.class;
                break;
            case MethodReflectParams.LONG:
                clazz = long.class;
                break;
            case MethodReflectParams.SHORT:
                clazz = short.class;
                break;
            default:
                if (classLoader == null) {
                    clazz = Class.forName(name);
                } else {
                    clazz = Class.forName(name, true, classLoader);
                }
        }
        return clazz;
    }


    private static boolean samePars(ClassLoader classLoader, Field field, Class[] par) {
        try {
            Class[] parsOnField = parseMethodPars(classLoader, field);
            if (parsOnField == null && field.isAnnotationPresent(SkipParamCheck.class))
                return true;
            if (par == null) {
                par = new Class[0];
            }
            if (parsOnField == null)
                parsOnField = new Class[0];
            if (par.length != parsOnField.length)
                return false;
            for (int i = 0;i < par.length;i++) {
                if (par[i] != parsOnField[i])
                    return false;
            }
            return true;
        } catch (HookErrorException e) {
            return false;
        }
    }




    private static Class getTargetHookClass(ClassLoader classLoader, Class<?> hookWrapperClass) {
        HookClass hookClass = hookWrapperClass.getAnnotation(HookClass.class);
        HookReflectClass hookReflectClass = hookWrapperClass.getAnnotation(HookReflectClass.class);
        if (hookClass != null) {
            return hookClass.value();
        } else if (hookReflectClass != null) {
            try {
                if (classLoader == null) {
                    return Class.forName(hookReflectClass.value());
                } else {
                    return Class.forName(hookReflectClass.value(), true, classLoader);
                }
            } catch (ClassNotFoundException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void checkSignature(Member origin, Method fake, Class[] originPars) throws HookErrorException {
        if (!Modifier.isStatic(fake.getModifiers()))
            throw new HookErrorException("hook method must static! - " + fake.getName());
        if (origin instanceof Constructor) {
            if (!fake.getReturnType().equals(Void.TYPE))
                throw new HookErrorException("error return type! - " + fake.getName());
        } else if (origin instanceof Method) {
            Class originRet = ((Method)origin).getReturnType();
            if (originRet != fake.getReturnType() && !originRet.isAssignableFrom(originRet))
                throw new HookErrorException("error return type! - " + fake.getName());
        }
        Class[] fakePars = fake.getParameterTypes();
        if (fakePars == null)
            fakePars = new Class[0];
        if (originPars == null)
            originPars = new Class[0];
        if (originPars.length == 0 && fakePars.length == 0)
            return;
        int parOffset = 0;
        if (!Modifier.isStatic(origin.getModifiers())) {
            parOffset = 1;
            if (fakePars.length == 0)
                throw new HookErrorException("first par must be this! " + fake.getName());
            if (fakePars[0] != origin.getDeclaringClass() && !fakePars[0].isAssignableFrom(origin.getDeclaringClass()))
                throw new HookErrorException("first par must be this! " + fake.getName());
            if (fakePars.length != originPars.length + 1)
                throw new HookErrorException("hook method pars must match the origin method! " + fake.getName());
        } else {
            if (fakePars.length != originPars.length)
                throw new HookErrorException("hook method pars must match the origin method! " + fake.getName());
        }
        for (int i = 0;i < originPars.length;i++) {
            if (fakePars[i + parOffset] != originPars[i] && !fakePars[i + parOffset].isAssignableFrom(originPars[i]))
                throw new HookErrorException("hook method pars must match the origin method! " + fake.getName());
        }
    }

    public static class HookEntity {

        public Member target;
        public Method hook;
        public Method backup;

        public boolean hookIsStub = false;
        public boolean resolveDexCache = true;
        public boolean backupIsStub = true;
        public boolean initClass = true;

        public Class[] pars;
        public int hookMode;

        public HookEntity(Member target) {
            this.target = target;
        }

        public HookEntity(Member target, Method hook, Method backup) {
            this.target = target;
            this.hook = hook;
            this.backup = backup;
        }

        public HookEntity(Member target, Method hook, Method backup, boolean resolveDexCache) {
            this.target = target;
            this.hook = hook;
            this.backup = backup;
            this.resolveDexCache = resolveDexCache;
        }

        public boolean isCtor() {
            return target instanceof Constructor;
        }

        public Object callOrigin(Object thiz, Object... args) throws Throwable {
            return SandHook.callOriginMethod(backupIsStub, target, backup, thiz, args);
        }
    }

}

package com.swift.sandhook.xposedcompat.hookstub;

import com.swift.sandhook.SandHook;
import com.swift.sandhook.utils.ParamWrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class HookMethodEntity {

    public Member origin;
    public Method hook;
    public Method backup;
    public Class[] parType;
    public Class retType;

    public HookMethodEntity(Member origin, Method hook, Method backup) {
        this.origin = origin;
        this.hook = hook;
        this.backup = backup;
    }

    public Object[] getArgs(long... addresses) {
        if (addresses == null || addresses.length == 0)
            return new Object[0];
        if (parType == null || parType.length == 0)
            return new Object[0];
        int argStart = 0;
        if (!isStatic()) {
            argStart = 1;
        }
        Object[] args = new Object[parType.length];
        for (int i = argStart;i < parType.length + argStart;i++) {
            args[i - argStart] = getArg(i - argStart, addresses[i]);
        }
        return args;
    }

    public long[] getArgsAddress(long[] oldAddress, Object... args) {
        if (oldAddress == null || oldAddress.length == 0)
            return new long[0];
        long[] addresses;
        int argStart = 0;
        if (!isStatic()) {
            argStart = 1;
            addresses = new long[oldAddress.length + 1];
            addresses[0] = oldAddress[0];
        } else {
            addresses = new long[oldAddress.length];
        }
        for (int i = 0;i < parType.length;i++) {
            addresses[i + argStart] = ParamWrapper.objectToAddress(parType[i], args[i]);
        }
        return addresses;
    }

    public Object getThis(long address) {
        if (isStatic())
            return null;
        return SandHook.getObject(address);
    }

    public Object getArg(int index, long address) {
        return ParamWrapper.addressToObject(parType[index], address);
    }

    public Object getResult(long address) {
        if (isVoid())
            return null;
        return ParamWrapper.addressToObject(retType, address);
    }

    public long getResultAddress(Object result) {
        if (isVoid())
            return 0;
        return ParamWrapper.objectToAddress(retType, result);
    }

    public boolean isVoid() {
        return retType == null || Void.TYPE.equals(retType);
    }

    public boolean isConstructor() {
        return origin instanceof Constructor;
    }

    public boolean isStatic() {
        return Modifier.isStatic(origin.getModifiers());
    }

}

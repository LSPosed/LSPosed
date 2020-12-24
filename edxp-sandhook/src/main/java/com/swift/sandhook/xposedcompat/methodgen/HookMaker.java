package com.swift.sandhook.xposedcompat.methodgen;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public interface HookMaker {
    void start(Member member, XposedBridge.AdditionalHookInfo hookInfo,
               ClassLoader appClassLoader) throws Exception;
    Method getHookMethod();
    Method getBackupMethod();
    Method getCallBackupMethod();
}

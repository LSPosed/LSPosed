package com.swift.sandhook.xposedcompat.hookstub;

public interface CallOriginCallBack {
    long call(long... args) throws Throwable;
}

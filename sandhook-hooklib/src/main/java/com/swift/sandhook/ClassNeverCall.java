package com.swift.sandhook;

import android.util.Log;

public class ClassNeverCall {
    private void neverCall() {}
    private void neverCall2() {
        Log.e("ClassNeverCall", "ClassNeverCall2");
    }
    private static void neverCallStatic() {}
    private native void neverCallNative();
    private native void neverCallNative2();
}

package com.swift.sandhook.xposedcompat.utils;

import android.util.Log;

import com.elderdrivers.riru.edxp.sandhook.BuildConfig;

import java.lang.reflect.Member;


public class DexLog {

    public static final String TAG = "SandXposed";

    public static volatile boolean DEBUG = BuildConfig.DEBUG;

    public static int v(String s) {
        return Log.v(TAG, s);
    }

    public static int i(String s) {
        return Log.i(TAG, s);
    }

    public static int d(String s) {
        if (DEBUG) {
            return Log.d(TAG, s);
        } else {
            return 0;
        }
    }

    public static void printMethodHookIn(Member member) {
        if (DEBUG && member != null) {
            Log.d("SandHook", "method <" + member.toString() + "> hook in");
        }
    }

    public static void printCallOriginError(Member member) {
        Log.e("SandHook", "method <" + member.toString() + "> call origin error!");
    }

    public static int w(String s) {
        return Log.w(TAG, s);
    }

    public static int e(String s) {
        return Log.e(TAG, s);
    }

    public static int e(String s, Throwable t) {
        return Log.e(TAG, s, t);
    }


}

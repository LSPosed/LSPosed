package com.swift.sandhook.xposedcompat.utils;

import android.util.Log;

import java.lang.reflect.Member;


public class DexLog {

    public static final String TAG = "SandXposed";

    public static volatile boolean DEBUG = true;

    public static int v(String s) {
        if (DEBUG) {
            return Log.v(TAG, s);
        } else {
            return 0;
        }
    }

    public static int i(String s) {
        return Log.i(TAG, s);
    }

    public static int d(String s) {
        return Log.d(TAG, s);
    }

    public static void printMethodHookIn(Member member) {
        if (DEBUG && member != null) {
            Log.d("SandHook", "method <" + member.toString() + "> hook in");
        }
    }

    public static void printCallOriginError(Member member) {
        if (DEBUG && member != null) {
            Log.d("SandHook", "method <" + member.toString() + "> call origin error!");
        }
    }

    public static int w(String s) {
        if (DEBUG) {
            return Log.w(TAG, s);
        } else {
            return 0;
        }
    }

    public static int e(String s) {
        if (DEBUG) {
            return Log.e(TAG, s);
        } else {
            return 0;
        }
    }

    public static int e(String s, Throwable t) {
        if (DEBUG) {
            return Log.e(TAG, s, t);
        } else {
            return 0;
        }
    }


}

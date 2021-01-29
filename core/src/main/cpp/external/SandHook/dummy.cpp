//
// Created by Kotori0 on 2021/1/30.
//
#include "sandhook.h"
#include "includes/log.h"

bool JNI_Load_Ex(JNIEnv* env, jclass classSandHook, jclass classNeverCall) {
    LOGE("Sandhook: Unsupported platform.");
    return false;
}
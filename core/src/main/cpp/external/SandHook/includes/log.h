//
// Created by SwiftGan on 2019/2/15.
//

#ifndef SANDHOOK_LOG_H
#define SANDHOOK_LOG_H

#include "android/log.h"

#define TAG "SandHook-Native"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#endif //SANDHOOK_LOG_H

//
// Created by liuruikai756 on 05/07/2017.
//
#include <android/log.h>

#ifndef YAHFA_COMMON_H
#define YAHFA_COMMON_H

//#define DEBUG
//#define LOG_DISABLED

#ifdef LOG_DISABLED
#define LOGI(...)
#define LOGW(...)
#define LOGE(...)
#else
#define LOG_TAG "EdXposed"
#ifndef NDEBUG
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#define LOGI(...)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#endif // DEBUG
#endif // LOG_DISABLED

#define pointer_size sizeof(void*)
#define roundUpToPtrSize(v) (v + pointer_size - 1 - ((v + pointer_size - 1) & (pointer_size - 1)))

#endif //YAHFA_COMMON_H

/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#ifndef _LOGGING_H
#define _LOGGING_H

#include <android/log.h>
#include <errno.h>

#ifndef LOG_TAG
#define LOG_TAG    "LSPosed"
#endif

#ifdef LOG_DISABLED
#define LOGD(...) 0
#define LOGV(...) 0
#define LOGI(...) 0
#define LOGW(...) 0
#define LOGE(...) 0
#else
#ifndef NDEBUG
#define LOGD(fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%s:%d#%s" ": " fmt, __FILE_NAME__, __LINE__, __PRETTY_FUNCTION__ __VA_OPT__(,) __VA_ARGS__)
#define LOGV(fmt, ...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "%s:%d#%s" ": " fmt, __FILE_NAME__, __LINE__, __PRETTY_FUNCTION__ __VA_OPT__(,) __VA_ARGS__)
#else
#define LOGD(...) 0
#define LOGV(...) 0
#endif
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL, LOG_TAG, __VA_ARGS__)
#define PLOGE(fmt, args...) LOGE(fmt " failed with %d: %s", ##args, errno, strerror(errno))
#endif

#endif // _LOGGING_H

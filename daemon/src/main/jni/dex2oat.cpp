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
 * Copyright (C) 2023 LSPosed Contributors
 */

#include <fcntl.h>
#include <jni.h>
#include <string>
#include <sys/mount.h>
#include <sys/wait.h>
#include <unistd.h>
#include <sched.h>

#include "logging.h"

extern "C"
JNIEXPORT void JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_doMountNative(JNIEnv *env, jobject,
                                                           jboolean enabled,
                                                           jstring r32, jstring d32,
                                                           jstring r64, jstring d64) {
    char dex2oat32[PATH_MAX], dex2oat64[PATH_MAX];
    realpath("bin/dex2oat32", dex2oat32);
    realpath("bin/dex2oat64", dex2oat64);

    if (pid_t pid = fork(); pid > 0) { // parent
        waitpid(pid, nullptr, 0);
    } else { // child
        int ns = open("/proc/1/ns/mnt", O_RDONLY);
        setns(ns, CLONE_NEWNS);
        close(ns);

        const char *r32p, *d32p, *r64p, *d64p;
        if (r32) r32p = env->GetStringUTFChars(r32, nullptr);
        if (d32) d32p = env->GetStringUTFChars(d32, nullptr);
        if (r64) r64p = env->GetStringUTFChars(r64, nullptr);
        if (d64) d64p = env->GetStringUTFChars(d64, nullptr);

        if (enabled) {
            LOGI("Enable dex2oat wrapper");
            if (r32) {
                mount(dex2oat32, r32p, nullptr, MS_BIND, nullptr);
                mount(nullptr, r32p, nullptr, MS_BIND | MS_REMOUNT | MS_RDONLY, nullptr);
            }
            if (d32) {
                mount(dex2oat32, d32p, nullptr, MS_BIND, nullptr);
                mount(nullptr, d32p, nullptr, MS_BIND | MS_REMOUNT | MS_RDONLY, nullptr);
            }
            if (r64) {
                mount(dex2oat64, r64p, nullptr, MS_BIND, nullptr);
                mount(nullptr, r64p, nullptr, MS_BIND | MS_REMOUNT | MS_RDONLY, nullptr);
            }
            if (d64) {
                mount(dex2oat64, d64p, nullptr, MS_BIND, nullptr);
                mount(nullptr, d64p, nullptr, MS_BIND | MS_REMOUNT | MS_RDONLY, nullptr);
            }
            execlp("resetprop", "resetprop", "--delete", "dalvik.vm.dex2oat-flags", nullptr);
        } else {
            LOGI("Disable dex2oat wrapper");
            if (r32) umount(r32p);
            if (d32) umount(d32p);
            if (r64) umount(r64p);
            if (d64) umount(d64p);
            execlp("resetprop", "resetprop", "dalvik.vm.dex2oat-flags", "--inline-max-code-units=0",
                   nullptr);
        }

        PLOGE("Failed to resetprop");
        exit(1);
    }
}

static int setsockcreatecon_raw(const char *context) {
    std::string path = "/proc/self/task/" + std::to_string(gettid()) + "/attr/sockcreate";
    int fd = open(path.c_str(), O_RDWR | O_CLOEXEC);
    if (fd < 0) return -1;
    int ret;
    if (context) {
        do {
            ret = write(fd, context, strlen(context) + 1);
        } while (ret < 0 && errno == EINTR);
    } else {
        do {
            ret = write(fd, nullptr, 0); // clear
        } while (ret < 0 && errno == EINTR);
    }
    close(fd);
    return ret < 0 ? -1 : 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_setSockCreateContext(JNIEnv *env, jclass,
                                                                  jstring contextStr) {
    const char *context = env->GetStringUTFChars(contextStr, nullptr);
    int ret = setsockcreatecon_raw(context);
    env->ReleaseStringUTFChars(contextStr, context);
    return ret == 0;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_getSockPath(JNIEnv *env, jobject) {
    return env->NewStringUTF("5291374ceda0aef7c5d86cd2a4f6a3ac\0");
}

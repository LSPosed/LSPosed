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
 * Copyright (C) 2022 LSPosed Contributors
 */

//
// Created by Nullptr on 2022/4/2.
//

#include <cstdlib>
#include <fcntl.h>
#include <jni.h>
#include <sched.h>
#include <string_view>
#include <sys/inotify.h>
#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <unistd.h>

#include "logging.h"

const char kRootMntBin32[] = "/proc/1/root/apex/com.android.art/bin/dex2oat32";
const char kRootMntBin64[] = "/proc/1/root/apex/com.android.art/bin/dex2oat64";
const char *kMntBin32 = kRootMntBin32 + 12, *kMntBin64 = kRootMntBin64 + 12;

char kFakeBin32[PATH_MAX], kFakeBin64[PATH_MAX];
char kTmpDir[] = "placeholder_/dev/0123456789abcdef";

extern "C"
JNIEXPORT void JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_init(JNIEnv *env, jobject thiz) {
    char magisk_path[PATH_MAX], cwd[PATH_MAX], *module_name;
    FILE *fp = popen("magisk --path", "r");
    fscanf(fp, "%s", magisk_path);
    fclose(fp);
    getcwd(cwd, PATH_MAX);
    module_name = cwd + std::string_view(cwd).find_last_of('/') + 1;
    sprintf(kFakeBin32, "%s/.magisk/modules/%s/bin/dex2oat32", magisk_path, module_name);
    sprintf(kFakeBin64, "%s/.magisk/modules/%s/bin/dex2oat64", magisk_path, module_name);
    auto path_field = env->GetFieldID(env->GetObjectClass(thiz), "devTmpDir", "Ljava/lang/String;");
    env->SetObjectField(thiz, path_field, env->NewStringUTF(kTmpDir + 12));
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_checkMount(JNIEnv *env, jobject thiz) {
    struct stat apex{}, stock{};
    if (0 == stat(kRootMntBin32, &apex)) {
        stat(kFakeBin32, &stock);
        if (apex.st_ino != stock.st_ino) {
            LOGW("Check mount failed for dex2oat32");
            return JNI_FALSE;
        }
    }
    if (0 == stat(kRootMntBin64, &apex)) {
        stat(kFakeBin64, &stock);
        if (apex.st_ino != stock.st_ino) {
            LOGW("Check mount failed for dex2oat64");
            return JNI_FALSE;
        }
    }
    LOGD("Check mount succeeded");
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_inotifySELinuxEnforce(JNIEnv *env, jobject thiz) {
    static int fd = 0;
    if (!fd) {
        fd = inotify_init();
        inotify_add_watch(fd, "/sys/fs/selinux/enforce", IN_MODIFY);
    } else {
        char event_buf[512];
        if (read(fd, event_buf, sizeof(event_buf)) < sizeof(inotify_event)) {
            jclass io_exception_class = env->FindClass("java/io/IOException");
            jmethodID ctor = env->GetMethodID(io_exception_class, "<init>", "(Ljava/lang/String;)V");
            auto exception = (jthrowable) env->NewObject(
                    io_exception_class,
                    ctor,
                    env->NewStringUTF("Failed to read inotify event"));
            env->Throw(exception);
        }
        LOGD("SELinux status changed");
    }
    int enforce;
    FILE *fp = fopen("/sys/fs/selinux/enforce", "r");
    fscanf(fp, "%d", &enforce);
    fclose(fp);
    return enforce;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_setEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    if (vfork() == 0) {
        int ns = open("/proc/1/ns/mnt", O_RDONLY);
        setns(ns, CLONE_NEWNS);
        close(ns);
        if (enabled) {
            LOGI("Enable dex2oat wrapper");
            mount(kFakeBin32, kMntBin32, nullptr, MS_BIND, nullptr);
            mount(kFakeBin64, kMntBin64, nullptr, MS_BIND, nullptr);
            execlp("resetprop", "resetprop", "--delete", "dalvik.vm.dex2oat-flags", nullptr);
        } else {
            LOGI("Disable dex2oat wrapper");
            umount(kMntBin32);
            umount(kMntBin64);
            execlp("resetprop", "resetprop", "dalvik.vm.dex2oat-flags", "--inline-max-code-units=0", nullptr);
        }
        PLOGE("Failed to resetprop");
        exit(0);
    }
}

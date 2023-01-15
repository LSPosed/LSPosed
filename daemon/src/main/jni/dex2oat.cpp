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
#include <string>
#include <string_view>
#include <sys/inotify.h>
#include <sys/mount.h>
#include <sys/sendfile.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <sys/wait.h>
#include <unistd.h>

#include "logging.h"
#include "utils.h"

using namespace std::string_literals;

const char *kDex2oat32Path, *kDex2oatDebug32Path;
const char *kDex2oat64Path, *kDex2oatDebug64Path;

char kFakeBin32[PATH_MAX], kFakeBin64[PATH_MAX];
char kTmpDir[] = "placeholder_/dev/0123456789abcdef\0";

extern "C"
JNIEXPORT void JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_initNative(JNIEnv *env, jobject thiz) {
    char magisk_path[PATH_MAX], cwd[PATH_MAX], *module_name;
    FILE *fp = popen("magisk --path", "r");
    fscanf(fp, "%s", magisk_path);
    fclose(fp);
    getcwd(cwd, PATH_MAX);
    module_name = cwd + std::string_view(cwd).find_last_of('/') + 1;
    sprintf(kFakeBin32, "%s/.magisk/modules/%s/bin/dex2oat32", magisk_path, module_name);
    sprintf(kFakeBin64, "%s/.magisk/modules/%s/bin/dex2oat64", magisk_path, module_name);

    if (GetAndroidApiLevel() == 29) {
        kDex2oat32Path = "/apex/com.android.runtime/bin/dex2oat";
        kDex2oatDebug32Path = "/apex/com.android.runtime/bin/dex2oatd";
    } else {
        kDex2oat32Path = "/apex/com.android.art/bin/dex2oat32";
        kDex2oatDebug32Path = "/apex/com.android.art/bin/dex2oatd32";
        kDex2oat64Path = "/apex/com.android.art/bin/dex2oat64";
        kDex2oatDebug64Path = "/apex/com.android.art/bin/dex2oatd64";
    }

    std::string copy_dir = magisk_path + "/dex2oat"s;
    mkdir(copy_dir.c_str(), 0755);
    auto CopyAndMount = [&](const char *src, const std::string &target) -> bool {
        int stock = open(src, O_RDONLY);
        if (stock == -1) return false;
        struct stat st{};
        fstat(stock, &st);
        int copy = open(target.c_str(), O_WRONLY | O_CREAT, st.st_mode);
        sendfile(copy, stock, nullptr, st.st_size);
        close(stock);
        close(copy);
        mount(target.c_str(), src, nullptr, MS_BIND, nullptr);
        return true;
    };
    bool done[4] = {false};
    done[0] = CopyAndMount(kDex2oat32Path, copy_dir + "/dex2oat32");
    done[1] = CopyAndMount(kDex2oatDebug32Path, copy_dir + "/dex2oatd32");
    if (GetAndroidApiLevel() >= 30) {
        done[2] = CopyAndMount(kDex2oat64Path, copy_dir + "/dex2oat64");
        done[3] = CopyAndMount(kDex2oatDebug64Path, copy_dir + "/dex2oatd64");
    }

    auto clazz = env->GetObjectClass(thiz);
    auto dev_path_field = env->GetFieldID(clazz, "devTmpDir", "Ljava/lang/String;");
    auto magisk_path_field = env->GetFieldID(clazz, "magiskPath", "Ljava/lang/String;");
    auto fake_bin32_field = env->GetFieldID(clazz, "fakeBin32", "Ljava/lang/String;");
    auto fake_bin64_field = env->GetFieldID(clazz, "fakeBin64", "Ljava/lang/String;");
    auto binaries_field = env->GetFieldID(clazz, "dex2oatBinaries", "[Ljava/lang/String;");
    env->SetObjectField(thiz, dev_path_field, env->NewStringUTF(kTmpDir + 12));
    env->SetObjectField(thiz, magisk_path_field, env->NewStringUTF(magisk_path));
    env->SetObjectField(thiz, fake_bin32_field, env->NewStringUTF(kFakeBin32));
    env->SetObjectField(thiz, fake_bin64_field, env->NewStringUTF(kFakeBin64));
    auto arr = env->NewObjectArray(4, env->FindClass("java/lang/String"), nullptr);
    if (done[0]) env->SetObjectArrayElement(arr, 0, env->NewStringUTF(kDex2oat32Path));
    if (done[1]) env->SetObjectArrayElement(arr, 1, env->NewStringUTF(kDex2oatDebug32Path));
    if (GetAndroidApiLevel() >= 30) {
        if (done[2]) env->SetObjectArrayElement(arr, 2, env->NewStringUTF(kDex2oat64Path));
        if (done[3]) env->SetObjectArrayElement(arr, 3, env->NewStringUTF(kDex2oatDebug64Path));
    }
    env->SetObjectField(thiz, binaries_field, arr);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_setEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    if (pid_t pid = fork(); pid > 0) { // parent
        waitpid(pid, nullptr, 0);
    } else { // child
        int ns = open("/proc/1/ns/mnt", O_RDONLY);
        setns(ns, CLONE_NEWNS);
        close(ns);
        if (enabled) {
            LOGI("Enable dex2oat wrapper");
            mount(kFakeBin32, kDex2oat32Path, nullptr, MS_BIND, nullptr);
            mount(kFakeBin32, kDex2oatDebug32Path, nullptr, MS_BIND, nullptr);
            if (GetAndroidApiLevel() >= 30) {
                mount(kFakeBin64, kDex2oat64Path, nullptr, MS_BIND, nullptr);
                mount(kFakeBin64, kDex2oatDebug64Path, nullptr, MS_BIND, nullptr);
            }
            execlp("resetprop", "resetprop", "--delete", "dalvik.vm.dex2oat-flags", nullptr);
        } else {
            LOGI("Disable dex2oat wrapper");
            umount(kDex2oat32Path);
            umount(kDex2oatDebug32Path);
            if (GetAndroidApiLevel() >= 30) {
                umount(kDex2oat64Path);
                umount(kDex2oatDebug64Path);
            }
            execlp("resetprop", "resetprop", "dalvik.vm.dex2oat-flags", "--inline-max-code-units=0", nullptr);
        }
        PLOGE("Failed to resetprop");
        exit(0);
    }
}

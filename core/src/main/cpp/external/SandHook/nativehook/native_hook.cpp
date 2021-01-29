//
// Created by SwiftGan on 2019/4/12.
//

#include <syscall.h>
#include <cstdlib>
#include <cstring>
#include <unistd.h>

#include "../includes/native_hook.h"
#include "../includes/arch.h"
#include "../includes/log.h"


extern int SDK_INT;

int inline getArrayItemCount(char *const array[]) {
    int i;
    for (i = 0; array[i]; ++i);
    return i;
}

bool isSandHooker(char *const args[]) {
    int orig_arg_count = getArrayItemCount(args);

    for (int i = 0; i < orig_arg_count; i++) {
        if (strstr(args[i], "SandHooker")) {
            LOGE("skip dex2oat hooker!");
            return true;
        }
    }

    return false;
}

char **build_new_argv(char *const argv[]) {

    int orig_argv_count = getArrayItemCount(argv);

    int new_argv_count = orig_argv_count + 2;
    char **new_argv = (char **) malloc(new_argv_count * sizeof(char *));
    int cur = 0;
    for (int i = 0; i < orig_argv_count; ++i) {
        new_argv[cur++] = argv[i];
    }

    if (SDK_INT >= ANDROID_L2 && SDK_INT < ANDROID_Q) {
        new_argv[cur++] = (char *) "--compile-pic";
    }
    if (SDK_INT >= ANDROID_M) {
        new_argv[cur++] = (char *) (SDK_INT > ANDROID_N2 ? "--inline-max-code-units=0" : "--inline-depth-limit=0");
    }

    new_argv[cur] = NULL;

    return new_argv;
}

int fake_execve_disable_inline(const char *pathname, char *argv[], char *const envp[]) {
    if (strstr(pathname, "dex2oat")) {
        if (SDK_INT >= ANDROID_N && isSandHooker(argv)) {
            LOGE("skip dex2oat!");
            return -1;
        }
        char **new_args = build_new_argv(argv);
        LOGE("dex2oat by disable inline!");
        int ret = static_cast<int>(syscall(__NR_execve, pathname, new_args, envp));
        free(new_args);
        return ret;
    }
    int ret = static_cast<int>(syscall(__NR_execve, pathname, argv, envp));
    return ret;
}

int fake_execve_disable_oat(const char *pathname, char *argv[], char *const envp[]) {
    if (strstr(pathname, "dex2oat")) {
        LOGE("skip dex2oat!");
        return -1;
    }
    return static_cast<int>(syscall(__NR_execve, pathname, argv, envp));
}

namespace SandHook {

    volatile bool hasHookedDex2oat = false;
    
    bool NativeHook::hookDex2oat(bool disableDex2oat) {
        if (hasHookedDex2oat)
            return false;

        hasHookedDex2oat = true;
        return nativeHookNoBackup(reinterpret_cast<void *>(execve),
                           reinterpret_cast<void *>(disableDex2oat ? fake_execve_disable_oat : fake_execve_disable_inline));
    }
    
}

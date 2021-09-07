//
// Created by VIP on 2021/4/25.
//

#include "bypass_sig.h"
#include "native_api.h"
#include "native_util.h"
#include "jni_helper.h"
#include "logging.h"
#include "symbol_cache.h"

[[gnu::weak]] extern "C" int __openat(int, const char *, int, int);

namespace lspd {

    std::string apkPath;
    std::string redirectPath;

    CREATE_HOOK_STUB_ENTRIES(
            "__openat",
            int, __openat,
            (int fd, const char *pathname, int flag, int mode), {
                if (pathname == apkPath) {
                    return backup(fd, redirectPath.c_str(), flag, mode);
                }
                return backup(fd, pathname, flag, mode);
            });

    LSP_DEF_NATIVE_METHOD(void, SigBypass, enableOpenatHook, jstring origApkPath, jstring cacheApkPath) {
        auto r = HookSymNoHandle(reinterpret_cast<void *>(&::__openat), __openat);
        if (!r) {
            LOGE("Hook __openat fail");
            return;
        }
        JUTFString str1(env, origApkPath);
        JUTFString str2(env, cacheApkPath);
        apkPath = str1.get();
        redirectPath = str2.get();
        LOGD("apkPath %s", apkPath.c_str());
        LOGD("redirectPath %s", redirectPath.c_str());
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(SigBypass, enableOpenatHook, "(Ljava/lang/String;Ljava/lang/String;)V")
    };

    void RegisterBypass(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(SigBypass);
    }
}

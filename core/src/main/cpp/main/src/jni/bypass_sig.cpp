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

    std::string hookedPackageName;
    std::string redirectPath;
    std::string apkPathPre;

    CREATE_HOOK_STUB_ENTRIES(
            "__openat",
            int, __openat,
            (int fd, const char *pathname, int flag, int mode), {
                if (strstr(pathname, apkPathPre.c_str()) && strstr(pathname, "/base.apk")) {
                    return backup(fd, redirectPath.c_str(), flag, mode);
                }
                return backup(fd, pathname, flag, mode);
            });

    LSP_DEF_NATIVE_METHOD(void, SigBypass, enableOpenatHook, jstring packageName) {
        auto r = HookSymNoHandle(reinterpret_cast<void *>(&::__openat), __openat);
        if (!r) {
            LOGE("Hook __openat fail");
            return;
        }
        JUTFString str(env, packageName);
        hookedPackageName = str.get();
        redirectPath = "/data/data/" + hookedPackageName + "/cache/lspatchapk.so";
        apkPathPre = "/data/app/" + hookedPackageName;
        LOGD("redirectPath %s", redirectPath.c_str());
        LOGD("apkPathPre %s", apkPathPre.c_str());
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(SigBypass, enableOpenatHook, "(Ljava/lang/String;)V")
    };

    void RegisterBypass(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(SigBypass);
    }
}

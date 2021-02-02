
#include <config_manager.h>
#include <nativehelper/jni_macros.h>
#include <native_util.h>
#include <sstream>
#include "config_manager.h"

namespace lspd {

    LSP_DEF_NATIVE_METHOD(jboolean, ConfigManager, isResourcesHookEnabled) {
        return (jboolean) ConfigManager::GetInstance()->IsResourcesHookEnabled();
    }

    LSP_DEF_NATIVE_METHOD(jboolean, ConfigManager, isNoModuleLogEnabled) {
        return (jboolean) ConfigManager::GetInstance()->IsNoModuleLogEnabled();
    }

    LSP_DEF_NATIVE_METHOD(jstring, ConfigManager, getInstallerPackageName) {
        return env->NewStringUTF(ConfigManager::GetInstance()->GetInstallerPackageName().c_str());
    }

    LSP_DEF_NATIVE_METHOD(jstring, ConfigManager, getDataPathPrefix) {
        return env->NewStringUTF(ConfigManager::GetInstance()->GetDataPathPrefix().c_str());
    }

    LSP_DEF_NATIVE_METHOD(jstring, ConfigManager, getConfigPath, jstring jSuffix) {
        const char *suffix = env->GetStringUTFChars(jSuffix, JNI_FALSE);
        auto result = ConfigManager::GetInstance()->GetConfigPath(suffix);
        env->ReleaseStringUTFChars(jSuffix, suffix);
        return env->NewStringUTF(result.c_str());
    }

    LSP_DEF_NATIVE_METHOD(jstring, ConfigManager, getPrefsPath, jstring jSuffix) {
        const char *suffix = env->GetStringUTFChars(jSuffix, JNI_FALSE);
        auto result = ConfigManager::GetInstance()->GetPrefsPath(suffix);
        env->ReleaseStringUTFChars(jSuffix, suffix);
        return env->NewStringUTF(result.c_str());
    }

    LSP_DEF_NATIVE_METHOD(jstring, ConfigManager, getCachePath, jstring jSuffix) {
        const char *suffix = env->GetStringUTFChars(jSuffix, JNI_FALSE);
        auto result = ConfigManager::GetCachePath(suffix);
        env->ReleaseStringUTFChars(jSuffix, suffix);
        return env->NewStringUTF(result.c_str());
    }

    LSP_DEF_NATIVE_METHOD(jstring, ConfigManager, getBaseConfigPath) {
        auto result = ConfigManager::GetInstance()->GetBaseConfigPath();
        return env->NewStringUTF(result.c_str());
    }

    LSP_DEF_NATIVE_METHOD(jstring, ConfigManager, getMiscPath) {
        auto result = ConfigManager::GetMiscPath();
        return env->NewStringUTF(result.c_str());
    }

    LSP_DEF_NATIVE_METHOD(jstring, ConfigManager, getModulesList) {
        auto module_list = Context::GetInstance()->GetAppModulesList();
        std::ostringstream join;
        std::copy(module_list.begin(), module_list.end(),
                  std::ostream_iterator<std::string>(join, "\n"));
        const auto &list = join.str();
        LOGD("module list: %s", list.c_str());
        return env->NewStringUTF(list.c_str());
    }

    LSP_DEF_NATIVE_METHOD(jboolean, ConfigManager, isPermissive) {
        return ConfigManager::GetInstance()->IsPermissive();
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(ConfigManager, isResourcesHookEnabled, "()Z"),
            LSP_NATIVE_METHOD(ConfigManager, isNoModuleLogEnabled, "()Z"),
            LSP_NATIVE_METHOD(ConfigManager, getInstallerPackageName, "()Ljava/lang/String;"),
            LSP_NATIVE_METHOD(ConfigManager, getDataPathPrefix, "()Ljava/lang/String;"),
            LSP_NATIVE_METHOD(ConfigManager, getPrefsPath,
                              "(Ljava/lang/String;)Ljava/lang/String;"),
            LSP_NATIVE_METHOD(ConfigManager, getCachePath,
                              "(Ljava/lang/String;)Ljava/lang/String;"),
            LSP_NATIVE_METHOD(ConfigManager, getBaseConfigPath, "()Ljava/lang/String;"),
            LSP_NATIVE_METHOD(ConfigManager, getModulesList, "()Ljava/lang/String;"),
            LSP_NATIVE_METHOD(ConfigManager, isPermissive, "()Z"),
    };

    void RegisterConfigManagerMethods(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("io.github.lsposed.lspd.nativebridge.ConfigManager");
    }

}
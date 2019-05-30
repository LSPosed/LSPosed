
#include <config_manager.h>
#include <nativehelper/jni_macros.h>
#include <native_util.h>
#include "edxp_config_manager.h"

namespace edxp {

    static jboolean ConfigManager_isBlackWhiteListEnabled(JNI_START) {
        return (jboolean) ConfigManager::GetInstance()->IsBlackWhiteListEnabled();
    }

    static jboolean ConfigManager_isDynamicModulesEnabled(JNI_START) {
        return (jboolean) ConfigManager::GetInstance()->IsDynamicModulesEnabled();
    }

    static jboolean ConfigManager_isResourcesHookEnabled(JNI_START) {
        return (jboolean) ConfigManager::GetInstance()->IsResourcesHookEnabled();
    }

    static jboolean ConfigManager_isDeoptBootImageEnabled(JNI_START) {
        return (jboolean) ConfigManager::GetInstance()->IsDeoptBootImageEnabled();
    }

    static jstring ConfigManager_getInstallerPackageName(JNI_START) {
        return env->NewStringUTF(ConfigManager::GetInstance()->GetInstallerPkgName().c_str());
    }

    static jboolean ConfigManager_isAppNeedHook(JNI_START, jstring appDataDir) {
        const char *app_data_dir = env->GetStringUTFChars(appDataDir, JNI_FALSE);
        auto result = (jboolean) ConfigManager::GetInstance()->IsAppNeedHook(app_data_dir);
        env->ReleaseStringUTFChars(appDataDir, app_data_dir);
        return result;
    }

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(ConfigManager, isBlackWhiteListEnabled, "()Z"),
            NATIVE_METHOD(ConfigManager, isDynamicModulesEnabled, "()Z"),
            NATIVE_METHOD(ConfigManager, isResourcesHookEnabled, "()Z"),
            NATIVE_METHOD(ConfigManager, isDeoptBootImageEnabled, "()Z"),
            NATIVE_METHOD(ConfigManager, getInstallerPackageName, "()Ljava/lang/String;"),
            NATIVE_METHOD(ConfigManager, isAppNeedHook, "(Ljava/lang/String;)Z"),
    };

    void RegisterConfigManagerMethods(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("com.elderdrivers.riru.edxp.config.ConfigManager");
    }

}
//
// Created by loves on 2/7/2021.
//

#include <nativehelper/scoped_local_ref.h>
#include <base/object.h>
#include <dobby.h>
#include "service.h"
#include "context.h"

namespace lspd {

    namespace {
        constexpr uint32_t BRIDGE_TRANSACTION_CODE = 1598837584;

        JNINativeInterface native_interface_replace{};
        jmethodID exec_transact_backup_methodID = nullptr;

        jboolean (*call_boolean_method_va_backup)(JNIEnv *env, jobject obj, jmethodID methodId,
                                                  va_list args) = nullptr;

        jclass bridge_service_class = nullptr;
        jmethodID exec_transact_replace_methodID = nullptr;
    }

    static bool exec_transact_replace(jboolean *res, JNIEnv *env, jobject obj, va_list args) {
        jint code;

        va_list copy;
        va_copy(copy, args);
        code = va_arg(copy, jint);
        va_end(copy);

        if (UNLIKELY(code == BRIDGE_TRANSACTION_CODE)) {
            *res = env->CallStaticBooleanMethodV(bridge_service_class,
                                                 exec_transact_replace_methodID,
                                                 args);
            return true;
        }

        return false;
    }

    static jboolean
    call_boolean_method_va_replace(JNIEnv *env, jobject obj, jmethodID methodId, va_list args) {
        if (UNLIKELY(methodId == exec_transact_backup_methodID)) {
            jboolean res = false;
            if (LIKELY(exec_transact_replace(&res, env, obj, args))) return res;
            // else fallback to backup
        }
        return call_boolean_method_va_backup(env, obj, methodId, args);
    }

    void InitService(const Context &context, JNIEnv *env) {
        bridge_service_class = context.FindClassFromCurrentLoader(env, kBridgeServiceClassName);
        if (!bridge_service_class) {
            LOGE("server class not found");
            return;
        }
        bridge_service_class = (jclass) env->NewGlobalRef(bridge_service_class);
        exec_transact_replace_methodID = env->GetStaticMethodID(bridge_service_class,
                                                                "execTransact",
                                                                "(IJJI)Z");
        if (!exec_transact_replace_methodID) {
            LOGE("execTransact class not found");
            return;
        }

        ScopedLocalRef<jclass> binderClass(env, env->FindClass("android/os/Binder"));
        exec_transact_backup_methodID = env->GetMethodID(binderClass.get(), "execTransact",
                                                         "(IJJI)Z");
        auto set_table_override = reinterpret_cast<void (*)(
                JNINativeInterface *)>(DobbySymbolResolver(nullptr,
                                                           "_ZN3art9JNIEnvExt16SetTableOverrideEPK18JNINativeInterface"));
        if (!set_table_override) {
            LOGE("set table override not found");
        }
        memcpy(&native_interface_replace, env->functions, sizeof(JNINativeInterface));

        call_boolean_method_va_backup = env->functions->CallBooleanMethodV;
        native_interface_replace.CallBooleanMethodV = &call_boolean_method_va_replace;

        if (set_table_override != nullptr) {
            set_table_override(&native_interface_replace);
        }

        LOGD("Done InitService");
    }

}
//
// Created by loves on 2/7/2021.
//

#ifndef LSPOSED_SERVICE_H
#define LSPOSED_SERVICE_H

#include <jni.h>
#include "context.h"

using namespace std::literals::string_view_literals;

namespace lspd {
    class Service {
        constexpr static jint BRIDGE_TRANSACTION_CODE = 1598837584;
        constexpr static auto BRIDGE_SERVICE_DESCRIPTOR = "LSPosed"sv;
        constexpr static auto BRIDGE_SERVICE_NAME = "activity"sv;
        constexpr static auto SYSTEM_SERVER_BRIDGE_SERVICE_NAME = "serial"sv;
        constexpr static jint BRIDGE_ACTION_GET_BINDER = 2;

    public:
        inline static Service* instance() {
            return instance_.get();
        }

        inline static std::unique_ptr<Service> ReleaseInstance() {
            return std::move(instance_);
        }

        void InitService(JNIEnv *env);

        void HookBridge(const Context& context, JNIEnv *env);

        jobject RequestBinder(JNIEnv *env);

        jobject RequestBinderForSystemServer(JNIEnv *env);

    private:
        inline static std::unique_ptr<Service> instance_ = std::make_unique<Service>();
        bool initialized_ = false;

        Service() = default;

        static jboolean
        call_boolean_method_va_replace(JNIEnv *env, jobject obj, jmethodID methodId, va_list args);

        static jboolean exec_transact_replace(jboolean *res, JNIEnv *env, jobject obj, va_list args);

        JNINativeInterface native_interface_replace_{};
        jmethodID exec_transact_backup_methodID_ = nullptr;

        jboolean (*call_boolean_method_va_backup_)(JNIEnv *env, jobject obj, jmethodID methodId,
                                                  va_list args) = nullptr;

        jclass bridge_service_class_ = nullptr;
        jmethodID exec_transact_replace_methodID_ = nullptr;

        jclass service_manager_class_ = nullptr;
        jmethodID get_service_method_ = nullptr;

        jmethodID transact_method_ = nullptr;

        jclass parcel_class_ = nullptr;
        jmethodID obtain_method_ = nullptr;
        jmethodID recycleMethod_ = nullptr;
        jmethodID write_interface_token_method_ = nullptr;
        jmethodID write_int_method_ = nullptr;
        jmethodID read_exception_method_ = nullptr;
        jmethodID read_strong_binder_method_ = nullptr;

        jclass deadObjectExceptionClass_ = nullptr;

        friend std::unique_ptr<Service> std::make_unique<Service>();

    };
}


#endif //LSPOSED_SERVICE_H

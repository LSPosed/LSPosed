//
// Created by loves on 2/7/2021.
//

#include <nativehelper/scoped_local_ref.h>
#include <dobby.h>
#include "base/object.h"
#include "service.h"
#include "context.h"
#include "JNIHelper.h"

namespace lspd {
    jboolean Service::exec_transact_replace(jboolean *res, JNIEnv *env, [[maybe_unused]] jobject obj, va_list args) {
        jint code;

        va_list copy;
        va_copy(copy, args);
        code = va_arg(copy, jint);
        va_end(copy);

        if (UNLIKELY(code == BRIDGE_TRANSACTION_CODE)) {
            *res = env->CallStaticBooleanMethodV(instance()->bridge_service_class_,
                                                 instance()->exec_transact_replace_methodID_,
                                                 args);
            return true;
        }

        return false;
    }

    jboolean
    Service::call_boolean_method_va_replace(JNIEnv *env, jobject obj, jmethodID methodId,
                                            va_list args) {
        if (UNLIKELY(methodId == instance()->exec_transact_backup_methodID_)) {
            jboolean res = false;
            if (LIKELY(exec_transact_replace(&res, env, obj, args))) return res;
            // else fallback to backup
        }
        return instance()->call_boolean_method_va_backup_(env, obj, methodId, args);
    }

    void Service::InitService(JNIEnv *env) {
        if (LIKELY(initialized_)) return;
        initialized_ = true;

        // ServiceManager
        serviceManagerClass_ = env->FindClass("android/os/ServiceManager");
        if (serviceManagerClass_) {
            serviceManagerClass_ = (jclass) env->NewGlobalRef(serviceManagerClass_);
        } else {
            env->ExceptionClear();
            return;
        }
        getServiceMethod_ = env->GetStaticMethodID(serviceManagerClass_, "getService",
                                                   "(Ljava/lang/String;)Landroid/os/IBinder;");
        if (!getServiceMethod_) {
            env->ExceptionClear();
            return;
        }

        // IBinder
        jclass iBinderClass = env->FindClass("android/os/IBinder");
        transactMethod_ = env->GetMethodID(iBinderClass, "transact",
                                           "(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z");

        // Parcel
        parcelClass_ = env->FindClass("android/os/Parcel");
        if (parcelClass_) parcelClass_ = (jclass) env->NewGlobalRef(parcelClass_);
        obtainMethod_ = env->GetStaticMethodID(parcelClass_, "obtain", "()Landroid/os/Parcel;");
        recycleMethod_ = env->GetMethodID(parcelClass_, "recycle", "()V");
        writeInterfaceTokenMethod_ = env->GetMethodID(parcelClass_, "writeInterfaceToken",
                                                      "(Ljava/lang/String;)V");
        writeIntMethod_ = env->GetMethodID(parcelClass_, "writeInt", "(I)V");
        writeStringMethod_ = env->GetMethodID(parcelClass_, "writeString", "(Ljava/lang/String;)V");
        readExceptionMethod_ = env->GetMethodID(parcelClass_, "readException", "()V");
        readStrongBinderMethod_ = env->GetMethodID(parcelClass_, "readStrongBinder",
                                                   "()Landroid/os/IBinder;");
        createStringArray_ = env->GetMethodID(parcelClass_, "createStringArray",
                                              "()[Ljava/lang/String;");

        deadObjectExceptionClass_ = env->FindClass("android/os/DeadObjectException");
        if (deadObjectExceptionClass_)
            deadObjectExceptionClass_ = (jclass) env->NewGlobalRef(deadObjectExceptionClass_);
    }

    void Service::HookBridge(const Context &context, JNIEnv *env) {
        static bool hooked = false;
        // This should only be ran once, so unlikely
        if (UNLIKELY(hooked)) return;
        hooked = true;
        bridge_service_class_ = context.FindClassFromCurrentLoader(env, kBridgeServiceClassName);
        if (!bridge_service_class_) {
            LOGE("server class not found");
            return;
        }
        bridge_service_class_ = (jclass) env->NewGlobalRef(bridge_service_class_);
        exec_transact_replace_methodID_ = env->GetStaticMethodID(bridge_service_class_,
                                                                 "execTransact",
                                                                 "(IJJI)Z");
        if (!exec_transact_replace_methodID_) {
            LOGE("execTransact class not found");
            return;
        }

        ScopedLocalRef<jclass> binderClass(env, env->FindClass("android/os/Binder"));
        exec_transact_backup_methodID_ = env->GetMethodID(binderClass.get(), "execTransact",
                                                          "(IJJI)Z");
        auto set_table_override = reinterpret_cast<void (*)(
                JNINativeInterface *)>(DobbySymbolResolver(nullptr,
                                                           "_ZN3art9JNIEnvExt16SetTableOverrideEPK18JNINativeInterface"));
        if (!set_table_override) {
            LOGE("set table override not found");
        }
        memcpy(&native_interface_replace_, env->functions, sizeof(JNINativeInterface));

        call_boolean_method_va_backup_ = env->functions->CallBooleanMethodV;
        native_interface_replace_.CallBooleanMethodV = &call_boolean_method_va_replace;

        if (set_table_override != nullptr) {
            set_table_override(&native_interface_replace_);
        }

        LOGD("Done InitService");
    }

    jobject Service::RequestBinder(JNIEnv *env) {
        if (UNLIKELY(!initialized_)) {
            LOGE("Service not initialized");
            return nullptr;
        }


        auto bridgeServiceName = env->NewStringUTF(BRIDGE_SERVICE_NAME.data());
        auto bridgeService = JNI_CallStaticObjectMethod(env, serviceManagerClass_,
                                                        getServiceMethod_, bridgeServiceName);
        if (!bridgeService) {
            LOGD("can't get %s", BRIDGE_SERVICE_NAME.data());
            return nullptr;
        }

        auto data = JNI_CallStaticObjectMethod(env, parcelClass_, obtainMethod_);
        auto reply = JNI_CallStaticObjectMethod(env, parcelClass_, obtainMethod_);

        auto descriptor = env->NewStringUTF(BRIDGE_SERVICE_DESCRIPTOR.data());
        JNI_CallVoidMethod(env, data, writeInterfaceTokenMethod_, descriptor);
        JNI_CallVoidMethod(env, data, writeIntMethod_, BRIDGE_ACTION_GET_BINDER);

        auto res = JNI_CallBooleanMethod(env, bridgeService, transactMethod_,
                                         BRIDGE_TRANSACTION_CODE,
                                         data,
                                         reply, 0);

        jobject service = nullptr;
        if (res) {
            JNI_CallVoidMethod(env, reply, readExceptionMethod_);
            service = JNI_CallObjectMethod(env, reply, readStrongBinderMethod_);
        } else {
            LOGD("no reply");
        }

        JNI_CallVoidMethod(env, data, recycleMethod_);
        JNI_CallObjectMethod(env, reply, recycleMethod_);

        return service;
    }
}
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
 * Copyright (C) 2021 LSPosed Contributors
 */

//
// Created by loves on 2/7/2021.
//

#include <dobby.h>
#include "base/object.h"
#include "service.h"
#include "context.h"
#include "jni_helper.h"

namespace lspd {
    jboolean
    Service::exec_transact_replace(jboolean *res, JNIEnv *env, [[maybe_unused]] jobject obj,
                                   va_list args) {
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
        service_manager_class_ = env->FindClass("android/os/ServiceManager");
        if (service_manager_class_) {
            service_manager_class_ = (jclass) env->NewGlobalRef(service_manager_class_);
        } else {
            env->ExceptionClear();
            return;
        }
        get_service_method_ = env->GetStaticMethodID(service_manager_class_, "getService",
                                                     "(Ljava/lang/String;)Landroid/os/IBinder;");
        if (!get_service_method_) {
            env->ExceptionClear();
            return;
        }

        // IBinder
        jclass ibinder_class = env->FindClass("android/os/IBinder");
        transact_method_ = env->GetMethodID(ibinder_class, "transact",
                                            "(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z");

        binder_class_ = env->FindClass("android/os/Binder");
        if (binder_class_) binder_class_ = (jclass)env->NewGlobalRef(binder_class_);
        binder_ctor_ = env->GetMethodID(binder_class_, "<init>", "()V");

        // Parcel
        parcel_class_ = env->FindClass("android/os/Parcel");
        if (parcel_class_) parcel_class_ = (jclass) env->NewGlobalRef(parcel_class_);
        obtain_method_ = env->GetStaticMethodID(parcel_class_, "obtain", "()Landroid/os/Parcel;");
        recycleMethod_ = env->GetMethodID(parcel_class_, "recycle", "()V");
        write_interface_token_method_ = env->GetMethodID(parcel_class_, "writeInterfaceToken",
                                                         "(Ljava/lang/String;)V");
        write_int_method_ = env->GetMethodID(parcel_class_, "writeInt", "(I)V");
        write_string_method_ = env->GetMethodID(parcel_class_, "writeString",
                                                "(Ljava/lang/String;)V");
        write_strong_binder_method_ = env->GetMethodID(parcel_class_, "writeStrongBinder",
                                                       "(Landroid/os/IBinder;)V");
        read_exception_method_ = env->GetMethodID(parcel_class_, "readException", "()V");
        read_strong_binder_method_ = env->GetMethodID(parcel_class_, "readStrongBinder",
                                                      "()Landroid/os/IBinder;");
//        createStringArray_ = env->GetMethodID(parcel_class_, "createStringArray",
//                                              "()[Ljava/lang/String;");

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
        exec_transact_replace_methodID_ = JNI_GetStaticMethodID(env, bridge_service_class_,
                                                                "execTransact",
                                                                "(IJJI)Z");
        if (!exec_transact_replace_methodID_) {
            LOGE("execTransact class not found");
            return;
        }

        ScopedLocalRef<jclass> binderClass(env, env->FindClass("android/os/Binder"));
        exec_transact_backup_methodID_ = JNI_GetMethodID(env, binderClass.get(), "execTransact",
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

    jobject Service::RequestBinder(JNIEnv *env, jstring nice_name) {
        if (UNLIKELY(!initialized_)) {
            LOGE("Service not initialized");
            return nullptr;
        }

        auto bridgeServiceName = env->NewStringUTF(BRIDGE_SERVICE_NAME.data());
        auto bridgeService = JNI_CallStaticObjectMethod(env, service_manager_class_,
                                                        get_service_method_, bridgeServiceName);
        if (!bridgeService) {
            LOGD("can't get %s", BRIDGE_SERVICE_NAME.data());
            return nullptr;
        }

        auto heart_beat_binder = JNI_NewObject(env, binder_class_, binder_ctor_);

        auto data = JNI_CallStaticObjectMethod(env, parcel_class_, obtain_method_);
        auto reply = JNI_CallStaticObjectMethod(env, parcel_class_, obtain_method_);

        auto descriptor = env->NewStringUTF(BRIDGE_SERVICE_DESCRIPTOR.data());
        JNI_CallVoidMethod(env, data, write_interface_token_method_, descriptor);
        JNI_CallVoidMethod(env, data, write_int_method_, BRIDGE_ACTION_GET_BINDER);
        JNI_CallVoidMethod(env, data, write_string_method_, nice_name);
        JNI_CallVoidMethod(env, data, write_strong_binder_method_, heart_beat_binder);

        auto res = JNI_CallBooleanMethod(env, bridgeService, transact_method_,
                                         BRIDGE_TRANSACTION_CODE,
                                         data,
                                         reply, 0);

        jobject service = nullptr;
        if (res) {
            env->CallVoidMethod(reply, read_exception_method_);
            if (!ClearException(env)) {
                service = JNI_CallObjectMethod(env, reply, read_strong_binder_method_);
            }
        }
        JNI_CallVoidMethod(env, data, recycleMethod_);
        JNI_CallVoidMethod(env, reply, recycleMethod_);
        if (service) {
            env->NewGlobalRef(heart_beat_binder);
        } else {
            env->DeleteLocalRef(heart_beat_binder);
        }

        return service;
    }

    jobject Service::RequestBinderForSystemServer(JNIEnv *env) {
        if (UNLIKELY(!initialized_)) {
            LOGE("Service not initialized");
            return nullptr;
        }
        auto bridgeServiceName = env->NewStringUTF(SYSTEM_SERVER_BRIDGE_SERVICE_NAME.data());
        auto binder = JNI_CallStaticObjectMethod(env, service_manager_class_,
                                                 get_service_method_, bridgeServiceName);
        if (!binder) {
            LOGD("Fail to get binder for system server");
            return nullptr;
        }
        auto method = JNI_GetStaticMethodID(env, bridge_service_class_,
                                            "getApplicationServiceForSystemServer",
                                            "(Landroid/os/IBinder;Landroid/os/IBinder;)Landroid/os/IBinder;");
        auto heart_beat_binder = JNI_NewObject(env, binder_class_, binder_ctor_);
        auto app_binder = JNI_CallStaticObjectMethod(env, bridge_service_class_, method, binder,
                                                     heart_beat_binder);
        if (app_binder) {
            env->NewGlobalRef(heart_beat_binder);
        } else {
            env->DeleteLocalRef(heart_beat_binder);
        }
        return app_binder;
    }
}

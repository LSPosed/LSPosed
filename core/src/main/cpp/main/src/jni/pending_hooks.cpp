
#include <nativehelper/jni_macros.h>
#include <string>
#include <unordered_set>
#include <shared_mutex>
#include "HookMain.h"
#include "jni.h"
#include "native_util.h"
#include "pending_hooks.h"
#include "art/runtime/thread.h"
#include "art/runtime/mirror/class.h"

namespace lspd {
    namespace {
        std::unordered_set<const void *> pending_classes_;
        std::shared_mutex pending_classes_lock_;

        std::unordered_set<const void *> pending_methods_;
        std::shared_mutex pending_methods_lock_;

        std::unordered_set<const void *> hooked_methods_;
        std::shared_mutex hooked_methods_lock_;
    }

    bool IsClassPending(void *clazz) {
        std::shared_lock lk(pending_classes_lock_);
        return pending_classes_.count(clazz);
    }

    bool IsMethodPending(void *art_method) {
        bool result;
        {
            std::shared_lock lk(pending_methods_lock_);
            result = pending_methods_.count(art_method);
        }
        if (result) {
            std::unique_lock lk(pending_methods_lock_);
            pending_methods_.erase(art_method);
        }
        return result;
    }

    void DonePendingHook(void *clazz) {
        std::unique_lock lk(pending_classes_lock_);
        pending_classes_.erase(clazz);
    }

    static void
    PendingHooks_recordPendingMethodNative(JNI_START, jobject method_ref, jclass class_ref) {
        auto *class_ptr = art::Thread::Current().DecodeJObject(class_ref);
        auto *method = getArtMethodYahfa(env, method_ref);
        art::mirror::Class mirror_class(class_ptr);
        if (auto def = mirror_class.GetClassDef(); LIKELY(def)) {
            LOGD("record pending: %p (%s) with %p", class_ptr, mirror_class.GetDescriptor().c_str(),
                 method);
            // Add it for ShouldUseInterpreterEntrypoint
            {
                std::unique_lock lk(pending_methods_lock_);
                pending_methods_.insert(method);
            }
            {
                std::unique_lock lk(pending_classes_lock_);
                pending_classes_.insert(def);
            }
        } else {
            LOGW("fail to record pending for : %p (%s)", class_ptr,
                 mirror_class.GetDescriptor().c_str());
        }
    }

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(PendingHooks, recordPendingMethodNative,
                          "(Ljava/lang/reflect/Method;Ljava/lang/Class;)V"),
    };

    void RegisterPendingHooks(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("de.robv.android.xposed.PendingHooks");
    }

    bool isHooked(void *art_method) {
        std::shared_lock lk(hooked_methods_lock_);
        return hooked_methods_.count(art_method);
    }

    void recordHooked(void *art_method) {
        std::unique_lock lk(hooked_methods_lock_);
        hooked_methods_.insert(art_method);
    }

}
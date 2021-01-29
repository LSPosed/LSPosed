
#include <nativehelper/jni_macros.h>
#include <string>
#include <unordered_set>
#include "HookMain.h"
#include "jni.h"
#include "native_util.h"
#include "pending_hooks.h"
#include "art/runtime/thread.h"
#include "art/runtime/mirror/class.h"

namespace lspd {
    namespace {
        std::unordered_set<const void *> pending_classes_;
        std::unordered_set<const void *> pending_methods_;

        std::unordered_set<const void *> hooked_methods_;
    }

    bool IsClassPending(void *clazz) {
        return pending_classes_.count(clazz);
    }

    bool IsMethodPending(void* art_method) {
        return pending_methods_.erase(art_method) > 0;
    }

    void DonePendingHook(void *clazz) {
        pending_classes_.erase(clazz);
    }

    static void PendingHooks_recordPendingMethodNative(JNI_START, jobject method_ref, jclass class_ref) {
        auto *class_ptr = art::Thread::Current().DecodeJObject(class_ref);
        auto *method = getArtMethod(env, method_ref);
        art::mirror::Class mirror_class(class_ptr);
        if (auto def = mirror_class.GetClassDef(); LIKELY(def)) {
            LOGD("record pending: %p (%s) with %p", class_ptr, mirror_class.GetDescriptor().c_str(), method);
            // Add it for ShouldUseInterpreterEntrypoint
            pending_methods_.insert(method);
            pending_classes_.insert(def);
        } else {
            LOGW("fail to record pending for : %p (%s)", class_ptr,
                 mirror_class.GetDescriptor().c_str());
        }
    }

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(PendingHooks, recordPendingMethodNative, "(Ljava/lang/reflect/Method;Ljava/lang/Class;)V"),
    };

    void RegisterPendingHooks(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("de.robv.android.xposed.PendingHooks");
    }

    bool isHooked(void *art_method) {
        return hooked_methods_.count(art_method);
    }

    void recordHooked(void *art_method) {
        hooked_methods_.insert(art_method);
    }

}
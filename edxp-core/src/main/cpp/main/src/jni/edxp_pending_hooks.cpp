
#include <nativehelper/jni_macros.h>
#include <string>
#include <unordered_set>
#include "HookMain.h"
#include "jni.h"
#include "native_util.h"
#include "edxp_pending_hooks.h"
#include "art/runtime/thread.h"
#include "art/runtime/mirror/class.h"

namespace edxp {

    static std::unordered_set<const void *> pending_classes_;

    static std::unordered_set<const void *> hooked_methods_;

    bool IsClassPending(void *clazz) {
        return pending_classes_.count(clazz);
    }

    static void PendingHooks_recordPendingMethodNative(JNI_START, jlong thread, jclass class_ref) {
        art::Thread current_thread(reinterpret_cast<void *>(thread));
        auto *class_ptr = current_thread.DecodeJObject(class_ref);
        art::mirror::Class mirror_class(class_ptr);
        if (auto def = mirror_class.GetClassDef(); LIKELY(def)) {
            LOGD("record pending: %p (%s)", class_ptr, mirror_class.GetDescriptor().c_str());
            pending_classes_.insert(def);
        } else {
            LOGW("fail to record pending for : %p (%s)", class_ptr,
                 mirror_class.GetDescriptor().c_str());
        }
    }

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(PendingHooks, recordPendingMethodNative, "(JLjava/lang/Class;)V"),
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
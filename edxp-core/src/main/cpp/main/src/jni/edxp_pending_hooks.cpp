
#include <nativehelper/jni_macros.h>
#include <set>
#include <string>
#include "jni.h"
#include "native_util.h"
#include "edxp_pending_hooks.h"

namespace edxp {

    static std::set<std::string> class_descs_;

    static std::set<void*> hooked_methods_;

    bool IsClassPending(const char *class_desc) {
        return class_descs_.find(class_desc) != class_descs_.end();
    }

    static void PendingHooks_recordPendingMethodNative(JNI_START, jstring class_desc) {
        const char *class_desc_chars = env->GetStringUTFChars(class_desc, JNI_FALSE);
        class_descs_.insert(class_desc_chars);
    }

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(PendingHooks, recordPendingMethodNative, "(Ljava/lang/String;)V"),
    };

    void RegisterPendingHooks(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("de.robv.android.xposed.PendingHooks");
    }

    bool isHooked(void* art_method) {
        return hooked_methods_.count(art_method);
    }

    void recordHooked(void * art_method) {
        hooked_methods_.insert(art_method);
    }

}
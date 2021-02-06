
#include <jni.h>
#include <resource_hook.h>
#include "native_util.h"
#include "nativehelper/jni_macros.h"
#include "resources_hook.h"

namespace lspd {

    LSP_DEF_NATIVE_METHOD(jboolean, ResourcesHook, initXResourcesNative) {
        return XposedBridge_initXResourcesNative(env, clazz);
    }

    // @ApiSensitive(Level.MIDDLE)
    LSP_DEF_NATIVE_METHOD(jboolean, ResourcesHook, removeFinalFlagNative, jclass target_class) {
        if (target_class) {
            jclass class_clazz = JNI_FindClass(env, "java/lang/Class");
            jfieldID java_lang_Class_accessFlags = JNI_GetFieldID(
                    env, class_clazz, "accessFlags", "I");
            jint access_flags = env->GetIntField(target_class, java_lang_Class_accessFlags);
            env->SetIntField(target_class, java_lang_Class_accessFlags, access_flags & ~kAccFinal);
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(ResourcesHook, initXResourcesNative, "()Z"),
            LSP_NATIVE_METHOD(ResourcesHook, removeFinalFlagNative, "(Ljava/lang/Class;)Z"),
    };

    void RegisterEdxpResourcesHook(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(ResourcesHook);
    }

}
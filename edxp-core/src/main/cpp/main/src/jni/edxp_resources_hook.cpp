
#include <jni.h>
#include <native_util.h>
#include <nativehelper/jni_macros.h>
#include <resource_hook.h>
#include "edxp_resources_hook.h"

namespace edxp {

    static jboolean ResourcesHook_initXResourcesNative(JNI_START) {
        return XposedBridge_initXResourcesNative(env, clazz);
    }

    // @ApiSensitive(Level.MIDDLE)
    static jboolean ResourcesHook_removeFinalFlagNative(JNI_START, jclass target_class) {
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
            NATIVE_METHOD(ResourcesHook, initXResourcesNative, "()Z"),
            NATIVE_METHOD(ResourcesHook, removeFinalFlagNative, "(Ljava/lang/Class;)Z"),
    };

    void RegisterEdxpResourcesHook(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("com.elderdrivers.riru.edxp.core.ResourcesHook");
    }

}

#include "HookMain.h"
#include <nativehelper/jni_macros.h>
#include "jni.h"
#include "native_util.h"
#include "yahfa.h"
#include "pending_hooks.h"
#include "art/runtime/class_linker.h"

namespace lspd {

    LSP_DEF_NATIVE_METHOD(void, Yahfa, init, jint sdkVersion) {
        Java_lab_galaxy_yahfa_HookMain_init(env, clazz, sdkVersion);
    }

    LSP_DEF_NATIVE_METHOD(jobject, Yahfa, findMethodNative, jclass targetClass,
                                          jstring methodName, jstring methodSig) {
        return Java_lab_galaxy_yahfa_HookMain_findMethodNative(env, clazz, targetClass, methodName,
                                                               methodSig);
    }

    LSP_DEF_NATIVE_METHOD(jboolean, Yahfa, backupAndHookNative, jobject target,
                                              jobject hook, jobject backup) {
        return Java_lab_galaxy_yahfa_HookMain_backupAndHookNative(env, clazz, target, hook, backup);
    }

    LSP_DEF_NATIVE_METHOD(void, Yahfa, recordHooked, jobject member) {
        lspd::recordHooked(getArtMethodYahfa(env, member));
    }

    LSP_DEF_NATIVE_METHOD(jboolean, Yahfa, isHooked, jobject member) {
        return lspd::isHooked(getArtMethodYahfa(env, member));
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(Yahfa, init, "(I)V"),
            LSP_NATIVE_METHOD(Yahfa, findMethodNative,
                          "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/reflect/Member;"),
            LSP_NATIVE_METHOD(Yahfa, backupAndHookNative,
                          "(Ljava/lang/Object;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)Z"),
            LSP_NATIVE_METHOD(Yahfa, recordHooked, "(Ljava/lang/reflect/Member;)V"),
            LSP_NATIVE_METHOD(Yahfa, isHooked, "(Ljava/lang/reflect/Member;)Z")
    };

    void RegisterEdxpYahfa(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("io.github.lsposed.lspd.nativebridge.Yahfa");
    }

}
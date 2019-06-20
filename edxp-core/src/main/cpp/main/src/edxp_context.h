
#pragma once

#include <unistd.h>
#include <mutex>

namespace edxp {

#define SYSTEM_SERVER_DATA_DIR "/data/user/0/android"

    enum Variant {
        NONE = 0,
        YAHFA = 1,
        SANDHOOK = 2,
        WHALE = 3
    };

    class Context {

    public:
        static Context *GetInstance();

        jobject GetCurrentClassLoader() const;

        void CallOnPreFixupStaticTrampolines(void *class_ptr);

        void CallOnPostFixupStaticTrampolines(void *class_ptr);

        void PrepareJavaEnv(JNIEnv *env);

        void FindAndCall(JNIEnv *env, const char *method_name, const char *method_sig, ...) const;

        JavaVM *GetJavaVM() const;

        void SetAppDataDir(jstring app_data_dir);

        void SetNiceName(jstring nice_name);

        jstring GetAppDataDir() const;

        jstring GetNiceName() const;

        jclass FindClassFromLoader(JNIEnv *env, const char *className) const;

        void OnNativeForkAndSpecializePre(JNIEnv *env, jclass clazz, jint uid, jint gid,
                                          jintArray gids, jint runtime_flags, jobjectArray rlimits,
                                          jint mount_external,
                                          jstring se_info, jstring se_name, jintArray fds_to_close,
                                          jintArray fds_to_ignore, jboolean is_child_zygote,
                                          jstring instruction_set, jstring app_data_dir);

        int OnNativeForkAndSpecializePost(JNIEnv *env, jclass clazz, jint res);

        int OnNativeForkSystemServerPost(JNIEnv *env, jclass clazz, jint res);

        void OnNativeForkSystemServerPre(JNIEnv *env, jclass clazz, uid_t uid, gid_t gid,
                                         jintArray gids, jint runtime_flags, jobjectArray rlimits,
                                         jlong permitted_capabilities,
                                         jlong effective_capabilities);

        bool IsInitialized() const;

        Variant GetVariant() const;

    private:
        static Context *instance_;
        bool initialized_ = false;
        Variant variant_ = NONE;
        jobject inject_class_loader_ = nullptr;
        jclass entry_class_ = nullptr;
        jstring app_data_dir_ = nullptr;
        jstring nice_name_ = nullptr;
        JavaVM *vm_ = nullptr;
        jclass class_linker_class_ = nullptr;
        jmethodID pre_fixup_static_mid_ = nullptr;
        jmethodID post_fixup_static_mid_ = nullptr;

        Context() {}

        ~Context() {}

        void LoadDexAndInit(JNIEnv *env, const char *dex_path);

        jclass FindClassFromLoader(JNIEnv *env, jobject class_loader, const char *class_name) const;

        void CallPostFixupStaticTrampolinesCallback(void *class_ptr, jmethodID mid);
    };

}

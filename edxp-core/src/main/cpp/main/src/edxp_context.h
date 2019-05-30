
#pragma once

#include <unistd.h>

namespace edxp {

#define SYSTEM_SERVER_DATA_DIR "/data/user/0/android"

    class Context {

    public:
        static Context *GetInstance();

        jobject GetCurrentClassLoader() const;

        void PrepareJavaEnv(JNIEnv *env);

        void FindAndCall(JNIEnv *env, const char *method_name, const char *method_sig, ...) const;

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

    private:
        static Context *instance_;
        bool initialized_ = false;
        jobject inject_class_loader_ = nullptr;
        jclass entry_class_ = nullptr;
        jstring app_data_dir_ = nullptr;
        jstring nice_name_ = nullptr;

        Context() {}

        ~Context() {}

        void LoadDexAndInit(JNIEnv *env, const char *dex_path);

        jclass FindClassFromLoader(JNIEnv *env, jobject class_loader, const char *class_name) const;

    };

}

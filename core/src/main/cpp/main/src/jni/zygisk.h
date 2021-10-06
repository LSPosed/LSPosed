// All content of this file is released to the public domain.

// This file is the public API for Zygisk modules, and should always be updated in sync with:
// https://github.com/topjohnwu/zygisk-module-sample/blob/master/module/jni/zygisk.hpp

#pragma once

#include <jni.h>

#define ZYGISK_API_VERSION 1

/*
Define a class and inherit zygisk::ModuleBase to implement the functionality of your module.
Use the macro REGISTER_ZYGISK_MODULE(className) to register that class to Zygisk.
Please note that modules will only be loaded after zygote has forked the child process.
THIS MEANS ALL OF YOUR CODE RUNS IN THE APP/SYSTEM SERVER PROCESS, NOT THE ZYGOTE DAEMON!
Example code:
static jint (*orig_logger_entry_max)(JNIEnv *env);
static jint my_logger_entry_max(JNIEnv *env) { return orig_logger_entry_max(env); }
static void example_handler(int socket) { ... }
class ExampleModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        this->api = api;
    }
    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
        JNINativeMethod methods[] = {
            { "logger_entry_max_payload_native", "()I", (void*) my_logger_entry_max },
        };
        api->hookJniNativeMethods("android/util/Log", methods, 1);
        *(void **) &orig_logger_entry_max = methods[0].fnPtr;
    }
private:
    zygisk::Api *api;
};
REGISTER_ZYGISK_MODULE(ExampleModule)
REGISTER_ZYGISK_COMPANION(example_handler)
*/

namespace zygisk {

    struct Api;
    struct AppSpecializeArgs;
    struct ServerSpecializeArgs;

    class ModuleBase {
    public:

        // This function is called when the module is loaded into the target process.
        // A Zygisk API handle will be sent as an argument; call utility functions or interface
        // with Zygisk through this handle.
        virtual void onLoad(Api *api, JNIEnv *env) {}

        // This function is called before the app process is specialized.
        // At this point, the process just got forked from zygote, but no app specific specialization
        // is applied. This means that the process does not have any sandbox restrictions and
        // still runs with the same privilege of zygote.
        //
        // All the arguments that will be sent and used for app specialization is passed as a single
        // AppSpecializeArgs object. You can read and overwrite these arguments to change how the app
        // process will be specialized.
        //
        // If you need to run some operations as superuser, you can call Api::connectCompanion() to
        // get a socket to do IPC calls with a root companion process.
        // See Api::connectCompanion() for more info.
        virtual void preAppSpecialize(AppSpecializeArgs *args) {}

        // This function is called after the app process is specialized.
        // At this point, the process has all sandbox restrictions enabled for this application.
        // This means that this function runs as the same privilege of the app's own code.
        virtual void postAppSpecialize(const AppSpecializeArgs *args) {}

        // This function is called before the system server process is specialized.
        // See preAppSpecialize(args) for more info.
        virtual void preServerSpecialize(ServerSpecializeArgs *args) {}

        // This function is called after the app process is specialized.
        // At this point, the process runs with the privilege of system_server.
        virtual void postServerSpecialize(const ServerSpecializeArgs *args) {}
    };

    struct AppSpecializeArgs {
        // Required arguments. These arguments are guaranteed to exist on all Android versions.
        jint &uid;
        jint &gid;
        jintArray &gids;
        jint &runtime_flags;
        jint &mount_external;
        jstring &se_info;
        jstring &nice_name;
        jstring &instruction_set;
        jstring &app_data_dir;

        // Optional arguments. Please check whether the pointer is null before de-referencing
        jboolean *const is_child_zygote;
        jboolean *const is_top_app;
        jobjectArray *const pkg_data_info_list;
        jobjectArray *const whitelisted_data_info_list;
        jboolean *const mount_data_dirs;
        jboolean *const mount_storage_dirs;

        AppSpecializeArgs() = delete;
    };

    struct ServerSpecializeArgs {
        jint &uid;
        jint &gid;
        jintArray &gids;
        jint &runtime_flags;
        jlong &permitted_capabilities;
        jlong &effective_capabilities;

        ServerSpecializeArgs() = delete;
    };

    namespace internal {
        struct api_table;
        template <class T> void entry_impl(api_table *, JNIEnv *);
    }

    struct Api {

        // Connect to a root companion process and get a Unix domain socket for IPC.
        //
        // This API only works in the pre[XXX]Specialize functions due to SELinux restrictions.
        //
        // The pre[XXX]Specialize functions run with the same privilege of zygote.
        // If you would like to do some operations with superuser permissions, register a handler
        // function that would be called in the root process with REGISTER_ZYGISK_COMPANION(func).
        // Another good use case for a companion process is that if you want to share some resources
        // across multiple processes, hold the resources in the companion process and pass it over.
        //
        // When this function is called, in the companion process, a socket pair will be created,
        // your module's onCompanionRequest(int) callback will receive one socket, and the other
        // socket will be returned.
        //
        // Returns a file descriptor to a socket that is connected to the socket passed to
        // your module's onCompanionRequest(int). Returns -1 if the connection attempt failed.
        int connectCompanion();

        // Force Magisk's denylist unmount routines to run on this process.
        //
        // This API only works in preAppSpecialize.
        //
        // Processes added to Magisk's denylist will have all Magisk and its modules' files unmounted
        // from its mount namespace. In addition, all Zygisk code will be unloaded from memory, which
        // also implies that no Zygisk modules (including yours) are loaded.
        //
        // However, if for any reason your module still wants the unmount part of the denylist
        // operation to be enabled EVEN IF THE PROCESS IS NOT ON THE DENYLIST, call this function.
        // No code will be unloaded from memory (including your module) because there is no way to
        // guarantee no crashes will occur.
        //
        // The unmounting does not happen immediately after the function is called. It is actually
        // done during app process specialization.
        void forceDenylistUnmount();

        // Hook JNI native methods for a class
        //
        // Lookup all registered JNI native methods and replace it with your own functions.
        // The original function pointer will be saved in each JNINativeMethod's fnPtr.
        // If no matching class, method name, or signature is found, that specific JNINativeMethod.fnPtr
        // will be set to nullptr.
        void hookJniNativeMethods(const char *className, JNINativeMethod *methods, int numMethods);

        // For ELFs loaded in memory matching `regex`, replace function `symbol` with `newFunc`.
        // If `oldFunc` is not nullptr, the original function pointer will be saved to `oldFunc`.
        void pltHookRegister(const char *regex, const char *symbol, void *newFunc, void **oldFunc);

        // For ELFs loaded in memory matching `regex`, exclude hooks registered for `symbol`.
        // If `symbol` is nullptr, then all symbols will be excluded.
        void pltHookExclude(const char *regex, const char *symbol);

        // Commit all the hooks that was previously registered.
        // Returns false if an error occurred.
        bool pltHookCommit();

    private:
        internal::api_table *impl;

        template<typename T>
        friend void internal::entry_impl(internal::api_table *, JNIEnv *);
    };

// Register a class as a Zygisk module

#define REGISTER_ZYGISK_MODULE(clazz) \
void zygisk_module_entry(zygisk::internal::api_table *table, JNIEnv *env) { \
    zygisk::internal::entry_impl<clazz>(table, env);                        \
}

// Register a root companion request handler function for your module
//
// The function runs in a superuser daemon process and handles a root companion request from
// your module running in a target process. The function has to accept an integer value,
// which is a socket that is connected to the target process.
// See Api::connectCompanion() for more info.
//
// NOTE: the function can run concurrently on multiple threads.
// Be aware of race conditions if you have a globally shared resource.

#define REGISTER_ZYGISK_COMPANION(func) \
void zygisk_companion_entry(int client) { func(client); }

/************************************************************************************
 * All the code after this point is internal code used to interface with Zygisk
 * and guarantee ABI stability. You do not have to understand what it is doing.
 ************************************************************************************/

    namespace internal {

        struct module_abi {
            long api_version;
            ModuleBase *_this;

            void (*onLoad)(ModuleBase *, Api *, JNIEnv *);
            void (*preAppSpecialize)(ModuleBase *, AppSpecializeArgs *);
            void (*postAppSpecialize)(ModuleBase *, const AppSpecializeArgs *);
            void (*preServerSpecialize)(ModuleBase *, ServerSpecializeArgs *);
            void (*postServerSpecialize)(ModuleBase *, const ServerSpecializeArgs *);

            module_abi(ModuleBase *module) : api_version(ZYGISK_API_VERSION), _this(module) {
                onLoad = [](auto self, auto api, auto env) { self->onLoad(api, env); };
                preAppSpecialize = [](auto self, auto args) { self->preAppSpecialize(args); };
                postAppSpecialize = [](auto self, auto args) { self->postAppSpecialize(args); };
                preServerSpecialize = [](auto self, auto args) { self->preServerSpecialize(args); };
                postServerSpecialize = [](auto self, auto args) { self->postServerSpecialize(args); };
            }
        };

        struct api_table {
            // These first 2 entries are permanent, shall never change
            void *_this;
            bool (*registerModule)(api_table *, module_abi *);

            // Utility functions
            void (*hookJniNativeMethods)(const char *, JNINativeMethod *, int);
            void (*pltHookRegister)(const char *, const char *, void *, void **);
            void (*pltHookExclude)(const char *, const char *);
            bool (*pltHookCommit)();

            // Zygisk functions
            int  (*connectCompanion)(void * /* _this */);
            void (*forceDenylistUnmount)(void * /* _this */);
        };

        template <class T>
        void entry_impl(api_table *table, JNIEnv *env) {
            ModuleBase* module = new T();
            if (!table->registerModule(table, new module_abi(module)))
                return;
            auto api = new Api();
            api->impl = table;
            module->onLoad(api, env);
        }

    } // namespace internal

    int Api::connectCompanion() {
        return impl->connectCompanion(impl->_this);
    }
    void Api::forceDenylistUnmount() {
        impl->forceDenylistUnmount(impl->_this);
    }
    void Api::hookJniNativeMethods(const char *className, JNINativeMethod *methods, int numMethods) {
        impl->hookJniNativeMethods(className, methods, numMethods);
    }
    void Api::pltHookRegister(const char *regex, const char *symbol, void *newFunc, void **oldFunc) {
        impl->pltHookRegister(regex, symbol, newFunc, oldFunc);
    }
    void Api::pltHookExclude(const char *regex, const char *symbol) {
        impl->pltHookExclude(regex, symbol);
    }
    bool Api::pltHookCommit() {
        return impl->pltHookCommit();
    }

} // namespace zygisk

[[gnu::visibility("default")]] [[gnu::used]]
extern "C" void zygisk_module_entry(zygisk::internal::api_table *, JNIEnv *);

[[gnu::visibility("default")]] [[gnu::used]]
extern "C" void zygisk_companion_entry(int);

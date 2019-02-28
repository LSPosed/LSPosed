
#include <dlfcn.h>
#include <include/android_build.h>
#include <string>
#include <vector>
#include <inject/config_manager.h>

#include "include/logging.h"
#include "native_hook.h"

static bool inlineHooksInstalled = false;

static const char *(*getDesc)(void *, std::string *);

static bool (*isInSamePackageBackup)(void *, void *) = nullptr;

// runtime
void *runtime_ = nullptr;

void (*deoptBootImage)(void *runtime) = nullptr;

bool (*runtimeInitBackup)(void *runtime, void *mapAddr) = nullptr;

// instrumentation
void *instru_ = nullptr;

static void *(*instrCstBackup)(void *instru) = nullptr;

void (*deoptMethod)(void *, void *) = nullptr;

bool my_runtimeInit(void *runtime, void *mapAddr) {
    if (!runtimeInitBackup) {
        LOGE("runtimeInitBackup is null");
        return false;
    }
    LOGI("runtimeInit starts");
    bool result = (*runtimeInitBackup)(runtime, mapAddr);
    if (!deoptBootImage) {
        LOGE("deoptBootImageSym is null, skip deoptBootImage");
    } else {
        LOGI("deoptBootImage starts");
        (*deoptBootImage)(runtime);
        LOGI("deoptBootImage finishes");
    }
    LOGI("runtimeInit finishes");
    return result;
}

static bool onIsInSamePackageCalled(void *thiz, void *that) {
    std::string storage1, storage2;
    const char *thisDesc = (*getDesc)(thiz, &storage1);
    const char *thatDesc = (*getDesc)(that, &storage2);
    if (strstr(thisDesc, "EdHooker") != nullptr
        || strstr(thatDesc, "EdHooker") != nullptr
        || strstr(thisDesc, "com/elderdrivers/riru/") != nullptr
        || strstr(thatDesc, "com/elderdrivers/riru/") != nullptr) {
        return true;
    }
    return (*isInSamePackageBackup)(thiz, that);
}

static bool onInvokeHiddenAPI() {
    return false;
}

/**
 * NOTICE:
 * After Android Q(10.0), GetMemberActionImpl has been renamed to ShouldDenyAccessToMemberImpl,
 * But we don't know the symbols until it's published.
 * @author asLody
 */
static bool disableHiddenAPIPolicyImpl(int api_level, void *artHandle,
                                       void (*hookFun)(void *, void *, void **)) {
    if (api_level < ANDROID_P) {
        return true;
    }
    void *symbol = nullptr;
    // Android P : Preview 1 ~ 4 version
    symbol = dlsym(artHandle,
                   "_ZN3art9hiddenapi25ShouldBlockAccessToMemberINS_8ArtFieldEEEbPT_PNS_6ThreadENSt3__18functionIFbS6_EEENS0_12AccessMethodE");
    if (symbol) {
        hookFun(symbol, reinterpret_cast<void *>(onInvokeHiddenAPI), nullptr);
    }
    symbol = dlsym(artHandle,
                   "_ZN3art9hiddenapi25ShouldBlockAccessToMemberINS_9ArtMethodEEEbPT_PNS_6ThreadENSt3__18functionIFbS6_EEENS0_12AccessMethodE"
    );

    if (symbol) {
        hookFun(symbol, reinterpret_cast<void *>(onInvokeHiddenAPI), nullptr);
        return true;
    }
    // Android P : Release version
    symbol = dlsym(artHandle,
                   "_ZN3art9hiddenapi6detail19GetMemberActionImplINS_8ArtFieldEEENS0_6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE"
    );
    if (symbol) {
        hookFun(symbol, reinterpret_cast<void *>(onInvokeHiddenAPI), nullptr);
    }
    symbol = dlsym(artHandle,
                   "_ZN3art9hiddenapi6detail19GetMemberActionImplINS_9ArtMethodEEENS0_6ActionEPT_NS_20HiddenApiAccessFlags7ApiListES4_NS0_12AccessMethodE"
    );
    if (symbol) {
        hookFun(symbol, reinterpret_cast<void *>(onInvokeHiddenAPI), nullptr);
    }
    return symbol != nullptr;
}

static void hookIsInSamePackage(int api_level, void *artHandle,
                                void (*hookFun)(void *, void *, void **)) {
    // 5.0 - 7.1
    const char *isInSamePackageSym = "_ZN3art6mirror5Class15IsInSamePackageEPS1_";
    const char *getDescriptorSym = "_ZN3art6mirror5Class13GetDescriptorEPNSt3__112basic_stringIcNS2_11char_traitsIcEENS2_9allocatorIcEEEE";
    if (api_level >= ANDROID_O) {
        // 8.0 and later
        isInSamePackageSym = "_ZN3art6mirror5Class15IsInSamePackageENS_6ObjPtrIS1_EE";
    }
    void *original = dlsym(artHandle, isInSamePackageSym);
    if (!original) {
        LOGE("can't get isInSamePackageSym: %s", dlerror());
        return;
    }
    void *getDescSym = dlsym(artHandle, getDescriptorSym);
    if (!getDescSym) {
        LOGE("can't get GetDescriptorSym: %s", dlerror());
        return;
    }
    getDesc = reinterpret_cast<const char *(*)(void *, std::string *)>(getDescSym);
    (*hookFun)(original, reinterpret_cast<void *>(onIsInSamePackageCalled),
               reinterpret_cast<void **>(&isInSamePackageBackup));
}

void *my_instruCst(void *instru) {
    if (!instrCstBackup) {
        LOGE("instrCstBackup is null");
        return instru;
    }
    LOGI("instrCst starts");
    void *result = (*instrCstBackup)(instru);
    LOGI("instrCst finishes");
    if (instru_ != instru) {
        LOGI("instru_ changed from %p to %p", instru_, instru);
        instru_ = instru;
    }
    return result;
}

void hookInstrumentation(int api_level, void *artHandle, void (*hookFun)(void *, void *, void **)) {
    if (api_level < ANDROID_P) {
        // TODO support other api levels
        return;
    }
    void *instruCstSym = dlsym(artHandle,
                               "_ZN3art15instrumentation15InstrumentationC2Ev");
    deoptMethod = reinterpret_cast<void (*)(void *, void *)>(
            dlsym(artHandle,
                  "_ZN3art15instrumentation15Instrumentation40UpdateMethodsCodeToInterpreterEntryPointEPNS_9ArtMethodE"));
    if (!instruCstSym) {
        LOGE("can't get instruCstSym: %s", dlerror());
        return;
    }
    (*hookFun)(instruCstSym, reinterpret_cast<void *>(my_instruCst),
               reinterpret_cast<void **>(&instrCstBackup));
    LOGI("instrCst hooked");
}

std::vector<void *> deoptedMethods;

void deoptimize_method(JNIEnv *env, jclass clazz, jobject method) {
    if (!deoptMethod) {
        LOGE("deoptMethodSym is null, skip deopt");
        return;
    }
    void *reflected_method = env->FromReflectedMethod(method);
    if (std::find(deoptedMethods.begin(), deoptedMethods.end(), reflected_method) !=
        deoptedMethods.end()) {
        LOGD("method %p has been deopted before, skip...", reflected_method);
        return;
    }
    LOGD("deoptimize method: %p", reflected_method);
    (*deoptMethod)(instru_, reflected_method);
    deoptedMethods.push_back(reflected_method);
    LOGD("deoptimize method done: %p");
}

void hookRuntime(int api_level, void *artHandle, void (*hookFun)(void *, void *, void **)) {
    if (!is_deopt_boot_image_enabled()) {
        return;
    }
    void *runtimeInitSym = nullptr;
    if (api_level >= ANDROID_O) {
        // only oreo has deoptBootImageSym in Runtime
        runtime_ = dlsym(artHandle, "_ZN3art7Runtime9instance_E");
        if (!runtime_) { LOGW("runtime instance not found"); }
        runtimeInitSym = dlsym(artHandle, "_ZN3art7Runtime4InitEONS_18RuntimeArgumentMapE");
        if (!runtimeInitSym) {
            LOGE("can't find runtimeInitSym: %s", dlerror());
            return;
        }
        deoptBootImage = reinterpret_cast<void (*)(void *)>(dlsym(artHandle,
                                                                  "_ZN3art7Runtime19DeoptimizeBootImageEv"));
        if (!deoptBootImage) {
            LOGE("can't find deoptBootImageSym: %s", dlerror());
            return;
        }
        LOGI("start to hook runtimeInitSym");
        (*hookFun)(runtimeInitSym, reinterpret_cast<void *>(my_runtimeInit),
                   reinterpret_cast<void **>(&runtimeInitBackup));
        LOGI("runtimeInitSym hooked");
    } else {
        // TODO support deoptBootImage for Android 7.1 and before?
        LOGI("hooking Runtime skipped");
    }
}

void (*suspendAll)(ScopedSuspendAll*, const char*, bool) = nullptr;
void (*resumeAll)(ScopedSuspendAll*) = nullptr;

void getSuspendSyms(int api_level, void *artHandle, void (*hookFun)(void *, void *, void **)) {
    if (api_level < ANDROID_N) {
        return;
    }
    suspendAll = reinterpret_cast<void (*)(ScopedSuspendAll*,const char*, bool)>(dlsym(artHandle,
                                                        "_ZN3art16ScopedSuspendAllC2EPKcb"));
    resumeAll = reinterpret_cast<void (*)(ScopedSuspendAll*)>(dlsym(artHandle,
                                                            "_ZN3art16ScopedSuspendAllD2Ev"));
}

void install_inline_hooks() {
    if (inlineHooksInstalled) {
        LOGI("inline hooks installed, skip");
        return;
    }
    LOGI("start to install inline hooks");
    int api_level = GetAndroidApiLevel();
    if (api_level < ANDROID_LOLLIPOP) {
        LOGE("api level not supported: %d, skip", api_level);
        return;
    }
    LOGI("using api level %d", api_level);
    void *whaleHandle = dlopen(kLibWhalePath, RTLD_LAZY | RTLD_GLOBAL);
    if (!whaleHandle) {
        LOGE("can't open libwhale: %s", dlerror());
        return;
    }
    void *hookFunSym = dlsym(whaleHandle, "WInlineHookFunction");
    if (!hookFunSym) {
        LOGE("can't get WInlineHookFunction: %s", dlerror());
        return;
    }
    void (*hookFun)(void *, void *, void **) = reinterpret_cast<void (*)(void *, void *,
                                                                         void **)>(hookFunSym);
    void *artHandle = dlopen(kLibArtPath, RTLD_LAZY | RTLD_GLOBAL);
    if (!artHandle) {
        LOGE("can't open libart: %s", dlerror());
        return;
    }
    hookRuntime(api_level, artHandle, hookFun);
    hookInstrumentation(api_level, artHandle, hookFun);
    getSuspendSyms(api_level, artHandle, hookFun);
    hookIsInSamePackage(api_level, artHandle, hookFun);
    if (disableHiddenAPIPolicyImpl(api_level, artHandle, hookFun)) {
        LOGI("disableHiddenAPIPolicyImpl done.");
    } else {
        LOGE("disableHiddenAPIPolicyImpl failed.");
    }
    dlclose(whaleHandle);
    dlclose(artHandle);
    LOGI("install inline hooks done");
}


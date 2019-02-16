
#include <dlfcn.h>
#include <include/android_build.h>
#include <string>

#include "include/logging.h"
#include "native_hook.h"

static const char *(*getDesc)(void *, std::string *);

static bool (*isInSamePackageBackup)(void *, void *) = nullptr;

static bool onIsInSamePackageCalled(void *thiz, void *that) {
    std::string storage1, storage2;
    const char *thisDesc = (*getDesc)(thiz, &storage1);
    const char *thatDesc = (*getDesc)(that, &storage2);
    if (strstr(thisDesc, "EdHooker") != nullptr
        || strstr(thatDesc, "EdHooker") != nullptr
        || strstr(thisDesc, "com/elderdrivers/riru/") != nullptr
        || strstr(thatDesc, "com/elderdrivers/riru/") != nullptr) {
//        LOGE("onIsInSamePackageCalled, %s -> %s", thisDesc, thatDesc);
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
static bool disable_HiddenAPIPolicyImpl(int api_level, void *artHandle,
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

static void hook_IsInSamePackage(int api_level, void *artHandle,
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

void install_inline_hooks() {
    int api_level = GetAndroidApiLevel();
    if (api_level < ANDROID_LOLLIPOP) {
        return;
    }
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
    hook_IsInSamePackage(api_level, artHandle, hookFun);
    if (disable_HiddenAPIPolicyImpl(api_level, artHandle, hookFun)) {
        LOGI("disable_HiddenAPIPolicyImpl done.");
    } else {
        LOGE("disable_HiddenAPIPolicyImpl failed.");
    }
    dlclose(whaleHandle);
    dlclose(artHandle);
}



#include <dlfcn.h>
#include <include/android_build.h>
#include <string>

#include "include/logging.h"
#include "native_hook.h"

static const char *(*getDesc)(void *, std::string *);

static bool (*isInSamePackageBackup)(void *, void *) = nullptr;

bool onIsInSamePackageCalled(void *thiz, void *that) {
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

void install_inline_hooks() {
    int api_level = GetAndroidApiLevel();
    if (api_level < ANDROID_LOLLIPOP) {
        return;
    }
    void *whaleHandle = dlopen(kLibWhalePath, RTLD_NOW);
    if (!whaleHandle) {
        LOGE("can't open libwhale");
        return;
    }
    void *hookFunSym = dlsym(whaleHandle, "WInlineHookFunction");
    if (!hookFunSym) {
        LOGE("can't get WInlineHookFunction");
        return;
    }
    void (*hookFun)(void *, void *, void **) = reinterpret_cast<void (*)(void *, void *,
                                                                         void **)>(hookFunSym);
    void *artHandle = dlopen(kLibArtPath, RTLD_NOW);
    if (!artHandle) {
        LOGE("can't open libart");
        return;
    }
    // 5.0 - 7.1
    const char *isInSamePackageSym = "_ZN3art6mirror5Class15IsInSamePackageEPS1_";
    const char *getDescriptorSym = "_ZN3art6mirror5Class13GetDescriptorEPNSt3__112basic_stringIcNS2_11char_traitsIcEENS2_9allocatorIcEEEE";
    if (api_level >= ANDROID_O) {
        // 8.0 and later
        isInSamePackageSym = "_ZN3art6mirror5Class15IsInSamePackageENS_6ObjPtrIS1_EE";
    }
    void *original = dlsym(artHandle, isInSamePackageSym);
    getDesc = reinterpret_cast<const char *(*)(void *, std::string *)>(dlsym(artHandle,
                                                                             getDescriptorSym));
    if (!original) {
        LOGE("can't get isInSamePackageSym");
        return;
    }
    (*hookFun)(original, reinterpret_cast<void *>(onIsInSamePackageCalled),
               reinterpret_cast<void **>(&isInSamePackageBackup));
}
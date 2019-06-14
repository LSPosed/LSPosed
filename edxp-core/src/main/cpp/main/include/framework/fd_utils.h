
#pragma once

#include <android-base/strings.h>
#include "base/object.h"

namespace android {

    // Static whitelist of open paths that the zygote is allowed to keep open.
    static const char *kPathWhitelist[] = {
            "/data/app/",
            "/data/app-private/"
    };

    class FileDescriptorWhitelist : public edxp::HookedObject {

    public:
        FileDescriptorWhitelist(void *thiz) : HookedObject(thiz) {}

        static void Setup(void *handle, HookFunType hook_func) {
            HOOK_FUNC(IsAllowed,
                      "_ZNK23FileDescriptorWhitelist9IsAllowedERKNSt3__112basic_stringIcNS0_11char_traitsIcEENS0_9allocatorIcEEEE");
        }

    private:

        CREATE_HOOK_STUB_ENTRIES(bool, IsAllowed, void *thiz, const std::string &path) {
            for (const auto &whitelist_path : kPathWhitelist) {
                if (android::base::StartsWith(path, whitelist_path))
                    return true;
            }
            return IsAllowedBackup(thiz, path);
        }

    };

}
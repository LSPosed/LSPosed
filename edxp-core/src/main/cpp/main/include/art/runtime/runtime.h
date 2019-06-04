
#pragma once

#include <base/object.h>
#include <config_manager.h>

namespace art {

    class Runtime : public edxp::HookedObject {

    private:
        inline static Runtime *instance_;

        CREATE_FUNC_SYMBOL_ENTRY(void, DeoptimizeBootImage, void *thiz) {
            if (LIKELY(DeoptimizeBootImageSym))
                DeoptimizeBootImageSym(thiz);
        }

        CREATE_HOOK_STUB_ENTRIES(bool, Init, void *thiz, void *runtime_options) {
            if (LIKELY(instance_))
                instance_->Reset(thiz);
            else
                instance_ = new Runtime(thiz);
            bool success = InitBackup(thiz, runtime_options);
            if (edxp::ConfigManager::GetInstance()->IsDeoptBootImageEnabled()) {
                DeoptimizeBootImage(thiz);
                LOGI("DeoptimizeBootImage done");
            }
            return success;
        }

    public:
        Runtime(void *thiz) : HookedObject(thiz) {}

        static Runtime *Current() {
            return instance_;
        }

        static void Setup(void *handle, HookFunType hook_func) {
            HOOK_FUNC(Init, "_ZN3art7Runtime4InitEONS_18RuntimeArgumentMapE");
            RETRIEVE_FUNC_SYMBOL(DeoptimizeBootImage,
                                 "_ZN3art7Runtime19DeoptimizeBootImageEv");
        }

        ALWAYS_INLINE void DeoptimizeBootImage() const {
            if (LIKELY(thiz_))
                DeoptimizeBootImage(thiz_);
        }

    };

}

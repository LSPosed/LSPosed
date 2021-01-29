
#pragma once

#include <base/object.h>
#include <config_manager.h>

namespace art {

    class Runtime : public lspd::HookedObject {
    private:
        inline static Runtime *instance_;

    public:
        Runtime(void *thiz) : HookedObject(thiz) {}

        static Runtime *Current() {
            return instance_;
        }

        // @ApiSensitive(Level.LOW)
        static void Setup(void *handle, HookFunType hook_func) {
            RETRIEVE_FIELD_SYMBOL(instance, "_ZN3art7Runtime9instance_E");
            void * thiz = *reinterpret_cast<void**>(instance);
            LOGD("_ZN3art7Runtime9instance_E = %p", thiz);
            instance_ = new Runtime(thiz);
        }
    };

}

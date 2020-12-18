//
// Created by 双草酸酯 on 12/18/20.
//

#ifndef EDXPOSED_OAT_QUICK_METHOD_HEADER_H
#define EDXPOSED_OAT_QUICK_METHOD_HEADER_H

#include <base/object.h>
#include <config_manager.h>
#include <HookMain.h>
namespace art {
    // https://github.com/ElderDrivers/EdXposed/issues/740
    class OatQuickMethodHeader : public edxp::HookedObject {
    private:
        CREATE_HOOK_STUB_ENTRIES(uint32_t, GetCodeSize, void *thiz) {
            LOGD("OatQuickMethodHeader::GetCodeSize: %p", thiz);
            void* oep = getOriginalEntryPointFromHookedEntryPoint(thiz);
            if (oep) {
                LOGD("OatQuickMethodHeader: Original entry point: %p", oep);
                return GetCodeSizeBackup(oep);
            } else {
                LOGD("OatQuickMethodHeader: Original entry point not found");
                return GetCodeSizeBackup(thiz);
            }
        }

    public:
        static void Setup(void *handle, HookFunType hook_func) {
            if (edxp::GetAndroidApiLevel() >= __ANDROID_API_R__) {
                HOOK_FUNC(GetCodeSize, "_ZNK3art20OatQuickMethodHeader11GetCodeSizeEv");
            }
        }
    };

}


#endif //EDXPOSED_OAT_QUICK_METHOD_HEADER_H

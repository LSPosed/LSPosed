
#pragma once

#include <edxp_context.h>
#include "base/object.h"

namespace art {
    namespace mirror {

        using namespace std;
        using namespace edxp;

        class Class : public HookedObject {

        private:

            CREATE_FUNC_SYMBOL_ENTRY(const char *, GetDescriptor, void *thiz,
                                     std::string *storage) {
                if (GetDescriptorSym)
                    return GetDescriptorSym(thiz, storage);
                else
                    return "";
            }

            CREATE_HOOK_STUB_ENTRIES(bool, IsInSamePackage, void *thiz, void *that) {
                std::string storage1, storage2;
                const char *thisDesc = GetDescriptor(thiz, &storage1);
                const char *thatDesc = GetDescriptor(that, &storage2);
                // Note: these identifiers should be consistent with those in Java layer
                if (strstr(thisDesc, "EdHooker_") != nullptr
                    || strstr(thatDesc, "EdHooker_") != nullptr
                    || strstr(thisDesc, "com/elderdrivers/riru/") != nullptr
                    || strstr(thatDesc, "com/elderdrivers/riru/") != nullptr) {
                    return true;
                }
                // for MIUI resources hooking
                if (strstr(thisDesc, "android/content/res/MiuiTypedArray") != nullptr
                    || strstr(thatDesc, "android/content/res/MiuiTypedArray") != nullptr
                    || strstr(thisDesc, "android/content/res/XResources$XTypedArray") != nullptr
                    || strstr(thatDesc, "android/content/res/XResources$XTypedArray") != nullptr) {
                    return true;
                }
                return IsInSamePackageBackup(thiz, that);
            }

        public:
            Class(void *thiz) : HookedObject(thiz) {}

            static void Setup(void *handle, HookFunType hook_func) {
                RETRIEVE_FUNC_SYMBOL(GetDescriptor, "_ZN3art6mirror5Class13GetDescriptorEPNSt3__112"
                                                    "basic_stringIcNS2_11char_traitsIcEENS2_9allocatorIcEEEE");

//                RETRIEVE_FIELD_SYMBOL(mutator_lock_, "_ZN3art5Locks13mutator_lock_E");
//                LOGE("mutator_lock_: %p", mutator_lock_);

                HOOK_FUNC(IsInSamePackage,
                          "_ZN3art6mirror5Class15IsInSamePackageENS_6ObjPtrIS1_EE", //8.0-
                          "_ZN3art6mirror5Class15IsInSamePackageEPS1_"); //5.0-7.1

//                HOOK_FUNC(ClassForName,
//                          "_ZN3artL18Class_classForNameEP7_JNIEnvP7_jclassP8_jstringhP8_jobject");
            }

            const char *GetDescriptor(std::string *storage) {
                if (thiz_ && GetDescriptorSym) {
                    return GetDescriptor(thiz_, storage);
                }
                return "";
            }
        };

    } // namespace mirror
} // namespace art

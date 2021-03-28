/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#include <cstring>
#include <string>
#include <riru.h>
#include <sys/system_properties.h>
#include <logging.h>
#include "utils.h"
#include "riru_hook.h"
#include "symbol_cache.h"

namespace lspd {

    static int api_level = 0;

    //Max length of property values
    //Ref https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/os/SystemProperties.java
    //static const int PROP_VALUE_MAX = 91;

    CREATE_HOOK_STUB_ENTRIES(
            "__system_property_get",
            int, __system_property_get, (const char *key, char *value), {
                int res = backup(key, value);
                if (key) {
                    if (strcmp(kPropKeyCompilerFilter, key) == 0) {
//                strcpy(value, kPropValueCompilerFilter);
                        LOGI("system_property_get: %s -> %s", key, value);
                    }

                    if (strcmp(kPropKeyCompilerFlags, key) == 0) {
                        if (strcmp(value, "") == 0)
                            strcpy(value, kPropValueCompilerFlags);
                        else if (strstr(value, kPropValueCompilerFlags) == nullptr) {
                            if (strlen(value) + strlen(kPropValueCompilerFlagsWS) >
                                PROP_VALUE_MAX) {
                                //just fallback, why not
                                LOGI("Cannot add option to disable inline opt! Fall back to replace..");
                                strcpy(value, kPropValueCompilerFlags);
                            } else {
                                strcat(value, kPropValueCompilerFlagsWS);
                            }
                        }
                        LOGI("system_property_get: %s -> %s", key, value);
                    }


                    if (api_level == __ANDROID_API_O_MR1__) {
                        // https://android.googlesource.com/platform/art/+/f5516d38736fb97bfd0435ad03bbab17ddabbe4e
                        // Android 8.1 add a fatal check for debugging (removed in Android 9.0),
                        // which will be triggered by LSPosed in cases where target method is hooked
                        // (native flag set) after it has been called several times(getCounter() return positive number)
                        if (strcmp(kPropKeyUseJitProfiles, key) == 0) {
                            strcpy(value, "false");
                        } else if (strcmp(kPropKeyPmBgDexopt, key) == 0) {
                            // use speed as bg-dexopt filter since that speed-profile won't work after
                            // jit profiles is disabled
                            strcpy(value, kPropValuePmBgDexopt);
                        }
                        LOGD("system_property_get: %s -> %s", key, value);
                    }
                }
                return res;
            });

    CREATE_HOOK_STUB_ENTRIES(
            "_ZN7android4base11GetPropertyERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_",
            std::string, GetProperty, (const std::string &key, const std::string &default_value), {
                std::string res = backup(key, default_value);
                if (strcmp(kPropKeyCompilerFilter, key.c_str()) == 0) {
//            res = kPropValueCompilerFilter;
                    LOGI("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
                }

                if (strcmp(kPropKeyCompilerFlags, key.c_str()) == 0) {
                    if (strcmp(res.c_str(), "") == 0)
                        res = kPropValueCompilerFlags;
                    else if (strstr(res.c_str(), kPropValueCompilerFlags) == nullptr) {
                        if (strlen(res.c_str()) + strlen(kPropValueCompilerFlagsWS) >
                            PROP_VALUE_MAX) {
                            //just fallback, why not
                            LOGI("Cannot add option to disable inline opt! Fall back to replace..");
                            res = kPropValueCompilerFlags;
                        } else {
                            res.append(kPropValueCompilerFlagsWS);
                        }
                    }
                    LOGI("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
                }

                if (api_level == __ANDROID_API_O_MR1__) {
                    // see __system_property_get hook above for explanations
                    if (strcmp(kPropKeyUseJitProfiles, key.c_str()) == 0) {
                        res = "false";
                    } else if (strcmp(kPropKeyPmBgDexopt, key.c_str()) == 0) {
                        res = kPropValuePmBgDexopt;
                    }
                    LOGD("android::base::GetProperty: %s -> %s", key.c_str(), res.c_str());
                }
                return res;
            });

    void InstallRiruHooks() {

        LOGI("Start to install Riru hook");

        api_level = GetAndroidApiLevel();

        if (!sym_system_property_get) {
            LOGE("Failed to get symbol of __system_property_get");
            return;
        }
        HookSymNoHandle(sym_system_property_get, __system_property_get);

        if (GetAndroidApiLevel() >= __ANDROID_API_P__) {
            if (!sym_get_property) {
                LOGE("Failed to get symbol of _ZN7android4base11GetPropertyERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_");
                return;
            }
            HookSymNoHandle(sym_get_property, GetProperty);
        }

        LOGI("Riru hooks installed");
    }

}

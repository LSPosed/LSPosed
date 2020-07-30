#ifndef WHALE_ANDROID_ANDROID_BUILD_H_
#define WHALE_ANDROID_ANDROID_BUILD_H_

#include <cstdint>
#include <cstdlib>
#include <sys/system_properties.h>

static inline int32_t GetAndroidApiLevel() {
    char prop_value[PROP_VALUE_MAX];
    __system_property_get("ro.build.version.sdk", prop_value);
    return atoi(prop_value);
}

#endif  // WHALE_ANDROID_ANDROID_BUILD_H_

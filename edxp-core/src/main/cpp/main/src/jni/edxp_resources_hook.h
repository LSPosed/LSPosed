
#pragma once

#include "jni.h"

namespace edxp {

    static constexpr uint32_t kAccFinal = 0x0010;

    void RegisterEdxpResourcesHook(JNIEnv *);

} // namespace edxp

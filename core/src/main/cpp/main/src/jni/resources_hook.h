
#pragma once

#include "jni.h"

namespace lspd {

    static constexpr uint32_t kAccFinal = 0x0010;

    void RegisterEdxpResourcesHook(JNIEnv *);

} // namespace lspd

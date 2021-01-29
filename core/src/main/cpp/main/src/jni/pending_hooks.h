
#pragma once

#include "jni.h"

namespace lspd {

    bool IsClassPending(void *);

    void RegisterPendingHooks(JNIEnv *);

    bool isHooked(void* art_method);

    void recordHooked(void* art_method);

    void DonePendingHook(void *clazz);

    bool IsMethodPending(void* art_method);

} // namespace lspd


#pragma once

#include "jni.h"

namespace edxp {

    bool IsClassPending(void *);

    void RegisterPendingHooks(JNIEnv *);

    bool isHooked(void* art_method);

    void recordHooked(void* art_method);

} // namespace edxp

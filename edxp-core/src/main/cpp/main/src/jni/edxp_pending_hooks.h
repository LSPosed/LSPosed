
#pragma once

#include "jni.h"

namespace edxp {

    bool IsClassPending(const char *);

    void RegisterPendingHooks(JNIEnv *);

    bool isEntryHooked(const void* entry);

    bool isHooked(void* art_method);

    void recordHooked(void* art_method);

} // namespace edxp

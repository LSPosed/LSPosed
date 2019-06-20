
#pragma once

#include "jni.h"

namespace edxp {

    bool IsClassPending(const char *);

    void RegisterPendingHooks(JNIEnv *);

} // namespace edxp

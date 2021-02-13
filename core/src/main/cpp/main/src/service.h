//
// Created by loves on 2/7/2021.
//

#ifndef LSPOSED_SERVICE_H
#define LSPOSED_SERVICE_H

#include <jni.h>
#include "context.h"

namespace lspd {
    void InitService(const Context & context, JNIEnv *env);
}


#endif //LSPOSED_SERVICE_H

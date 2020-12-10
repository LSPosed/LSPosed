/*
 *
 * Copyright (c) 2011 The Android Open Source Project
 * Copyright (c) 2015, alipay.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ART_H
#define ART_H

#include <jni.h>
#include "arch.h"

//7.0 - 10.0
#define GCRoot uint32_t

namespace art {
namespace mirror {
class Object {
public:
};
class Class: public Object {
public:
};

class ArtField {
public:
};

class ArtMethod {
public:

    bool isAbstract();
    bool isNative();
    bool isStatic();
    bool isCompiled();
    bool isThumbCode();

    void setAccessFlags(uint32_t flags);
    void disableCompilable();
    void tryDisableInline();
    void disableInterpreterForO();
    void disableFastInterpreterForQ();
    void setPrivate();
    void setStatic();
    void setNative();

    void setQuickCodeEntry(void* entry);
    void setJniCodeEntry(void* entry);
    void setInterpreterCodeEntry(void* entry);
    void setDexCacheResolveList(void* list);
    void setDexCacheResolveItem(uint32_t index, void* item);
    void setDeclaringClass(GCRoot classPtr);
    void setHotnessCount(uint16_t count);

    void* getQuickCodeEntry();
    void* getInterpreterCodeEntry();
    uint32_t getAccessFlags();
    uint32_t getDexMethodIndex();
    GCRoot getDeclaringClass();
    uint16_t getHotnessCount();

    bool compile(JNIEnv* env);
    bool deCompile();
    void flushCache();
    void backup(ArtMethod* backup);

    static Size size();

};

}

}

#endif //ART_H

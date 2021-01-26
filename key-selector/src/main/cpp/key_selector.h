/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
 * Copyright (C) 2021 LSPosed
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

#ifndef __KEYCHECK_H__
#define __KEYCHECK_H__

// Constants: pressed keys
#define KEYCHECK_CHECK_VOLUMEDOWN 0x01u
#define KEYCHECK_CHECK_VOLUMEUP 0x02u
#define KEYCHECK_PRESSED_VOLUMEDOWN 41u
#define KEYCHECK_PRESSED_VOLUMEUP 42u

enum Variant {
    YAHFA = 0x11,
    SandHook = 0x12,
};

#endif // __KEYCHECK_H__
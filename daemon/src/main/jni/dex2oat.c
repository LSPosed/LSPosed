/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2022 LSPosed Contributors
 */

//
// Created by Nullptr on 2022/4/2.
//

#include <jni.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <stdlib.h>

#include "logging.h"

char kTmpDir[] = "placeholder_/dev/0123456789abcdef";

JNIEXPORT jstring JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_getDevPath(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, kTmpDir + 12);
}

JNIEXPORT void JNICALL
Java_org_lsposed_lspd_service_Dex2OatService_fallback(JNIEnv *env, jclass clazz) {
    LOGI("do fallback");
    system("nsenter -m -t 1 umount /apex/com.android.art/bin/dex2oat*");
    __system_property_set("dalvik.vm.dex2oat-flags", "--inline-max-code-units=0");
}

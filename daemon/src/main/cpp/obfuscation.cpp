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
// Created by Kotori2 on 2021/12/1.
//

#include <jni.h>
#include <unistd.h>
#include <algorithm>
#include <random>
#include <unordered_map>
#include <sys/mman.h>
#include <android/sharedmem.h>
#include <android/sharedmem_jni.h>
#include <slicer/dex_utf8.h>
#include <fcntl.h>
#include "slicer/reader.h"
#include "slicer/writer.h"
#include "obfuscation.h"

namespace {
std::mutex init_lock{};
std::string obfuscated_signature;
const std::string old_signature = "Lde/robv/android/xposed";

jclass class_file_descriptor;
jmethodID method_file_descriptor_ctor;

jclass class_shared_memory;
jmethodID method_shared_memory_ctor;

bool inited = false;
}

void maybeInit(JNIEnv *env) {
    if (inited) [[likely]] return;
    std::lock_guard l(init_lock);
    LOGD("ObfuscationManager.init");
    if (auto file_descriptor = JNI_FindClass(env, "java/io/FileDescriptor")) {
        class_file_descriptor = JNI_NewGlobalRef(env, file_descriptor);
    } else return;

    method_file_descriptor_ctor = JNI_GetMethodID(env, class_file_descriptor, "<init>", "(I)V");

    if (auto shared_memory = JNI_FindClass(env, "android/os/SharedMemory")) {
        class_shared_memory = JNI_NewGlobalRef(env, shared_memory);
    } else return;

    method_shared_memory_ctor = JNI_GetMethodID(env, class_shared_memory, "<init>", "(Ljava/io/FileDescriptor;)V");

    auto regen = []() {
        static auto& chrs = "abcdefghijklmnopqrstuvwxyz"
                            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        thread_local static std::mt19937 rg{std::random_device{}()};
        thread_local static std::uniform_int_distribution<std::string::size_type> pick(0, sizeof(chrs) - 2);
        thread_local static std::uniform_int_distribution<std::string::size_type> choose_slash(0, 10);

        std::string out;
        size_t length = old_signature.size();
        out.reserve(length);
        out += "L";

        for (size_t i = 1; i < length; i++) {
            if (choose_slash(rg) > 8 &&                         // 80% alphabet + 20% slashes
                out[i - 1] != '/' &&                                // slashes could not stick together
                i != 1 &&                                           // the first character should not be slash
                i != length - 1) {                                  // and the last character
                out += "/";
            } else {
                out += chrs[pick(rg)];
            }
        }
        return out;
    };

    auto contains_keyword = [](std::string_view s) -> bool {
        for (const auto &i: {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
                "continue", "const", "default", "do", "double", "else", "enum", "exports", "extends",
                "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
                "int", "interface", "long", "module", "native", "new", "package", "private", "protected",
                "public", "requires", "return", "short", "static", "strictfp", "super", "switch",
                "synchronized", "this", "throw", "throws", "transient", "try", "var", "void", "volatile",
                "while"}) {
            if (s.find(i) != std::string::npos) return true;
        }
        return false;
    };

    [[unlikely]] do {
        obfuscated_signature = regen();
    } while (contains_keyword(obfuscated_signature));

    LOGD("ObfuscationManager.getObfuscatedSignature: %s", obfuscated_signature.c_str());
    LOGD("ObfuscationManager init successfully");
    inited = true;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_lsposed_lspd_service_ObfuscationManager_getObfuscatedSignature(JNIEnv *env, [[maybe_unused]] jclass obfuscation_manager) {
    maybeInit(env);
    return env->NewStringUTF(obfuscated_signature.c_str());
}

static int obfuscateDex(const void *dex, size_t size) {
    const char* new_sig = obfuscated_signature.c_str();
    dex::Reader reader{reinterpret_cast<const dex::u1*>(dex), size};

    reader.CreateFullIr();
    auto ir = reader.GetIr();
    for (auto &i: ir->strings) {
        const char *s = i->c_str();
        char* p = const_cast<char *>(strstr(s, old_signature.c_str()));
        if (p) {
            // NOLINTNEXTLINE bugprone-not-null-terminated-result
            memcpy(p, new_sig, strlen(new_sig));
        }
    }
    dex::Writer writer(ir);

    size_t new_size;
    WA allocator;
    auto *p_dex = writer.CreateImage(&allocator, &new_size);  // allocates memory only once
    return allocator.GetFd(p_dex);
}

extern "C"
JNIEXPORT jobject
Java_org_lsposed_lspd_service_ObfuscationManager_obfuscateDex(JNIEnv *env, [[maybe_unused]] jclass obfuscation_manager,
                                                       jobject memory) {
    maybeInit(env);
    int fd = ASharedMemory_dupFromJava(env, memory);
    auto size = ASharedMemory_getSize(fd);
    LOGD("fd=%d, size=%zu", fd, size);

    const void* mem = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (mem == MAP_FAILED) {
        LOGE("old dex map failed?");
        return nullptr;
    }

    auto new_fd = obfuscateDex(mem, size);

    // construct new shared mem with fd
    auto java_fd = JNI_NewObject(env, class_file_descriptor, method_file_descriptor_ctor, new_fd);
    auto java_sm = JNI_NewObject(env, class_shared_memory, method_shared_memory_ctor, java_fd);

    return java_sm.release();
}

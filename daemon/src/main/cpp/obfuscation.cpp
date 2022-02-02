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
#include "config.h"
#include "obfuscation.h"

extern "C"
JNIEXPORT void JNICALL
Java_org_lsposed_lspd_service_ObfuscationManager_init(JNIEnv *env, jclass ) {
    LOGD("ObfuscationManager.init");
    if (auto file_descriptor = JNI_FindClass(env, "java/io/FileDescriptor")) {
        class_file_descriptor = JNI_NewGlobalRef(env, file_descriptor);
    } else return;

    method_file_descriptor_ctor = JNI_GetMethodID(env, class_file_descriptor, "<init>", "(I)V");

    if (auto shared_memory = JNI_FindClass(env, "android/os/SharedMemory")) {
        class_shared_memory = JNI_NewGlobalRef(env, shared_memory);
    } else return;

    method_shared_memory_ctor = JNI_GetMethodID(env, class_shared_memory, "<init>", "(Ljava/io/FileDescriptor;)V");
    LOGD("ObfuscationManager init successfully");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_lsposed_lspd_service_ObfuscationManager_getObfuscatedSignature(JNIEnv *env, jclass ) {
    if (!obfuscated_signature.empty()) {
        return env->NewStringUTF(obfuscated_signature.c_str());
    }

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
        for (const auto &i: {"do", "if", "for", "int", "new", "try"}) {
            if (s.find(i) != std::string::npos) return true;
        }
        return false;
    };

    do {
        obfuscated_signature = regen();
    } while (contains_keyword(obfuscated_signature));

    LOGD("ObfuscationManager.getObfuscatedSignature: %s", obfuscated_signature.c_str());
    return env->NewStringUTF(obfuscated_signature.c_str());
}

int obfuscateDex(const void *dex, size_t size) {
    const char* new_sig = obfuscated_signature.c_str();
    dex::Reader reader{reinterpret_cast<const dex::u1*>(dex), size};

    reader.CreateFullIr();
    auto ir = reader.GetIr();
    for (auto &i: ir->strings) {
        const char *s = i->c_str();
        char* p = const_cast<char *>(strstr(s, old_signature.c_str()));
        if (p) {
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
JNIEXPORT jint JNICALL
Java_org_lsposed_lspd_service_ObfuscationManager_preloadDex(JNIEnv *, jclass ) {
    using namespace std::string_literals;
    std::lock_guard lg(dex_lock);
    if (lspdDex != -1) return lspdDex;
    std::string dex_path = "/data/adb/modules/"s + lspd::moduleName + "/" + lspd::kDexPath;

    std::unique_ptr<FILE, decltype(&fclose)> f{fopen(dex_path.data(), "rb"), &fclose};

    if (!f) {
        LOGE("Fail to open dex from %s", dex_path.data());
        return -1;
    }
    fseek(f.get(), 0, SEEK_END);
    auto size = ftell(f.get());
    rewind(f.get());

    LOGD("Loaded %s with size %zu", dex_path.data(), size);

    auto *addr = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_PRIVATE, fileno(f.get()), 0);

    if (addr == MAP_FAILED) {
        PLOGE("Read dex");
        return -1;
    }

    auto new_dex = obfuscateDex(addr, size);
    LOGD("LSPApplicationService::preloadDex: %d, size=%zu", new_dex, ASharedMemory_getSize(new_dex));
    lspdDex = new_dex;
    return new_dex;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_lsposed_lspd_service_ObfuscationManager_getPreloadedDexSize(JNIEnv *, jclass ) {
    if (lspdDex != -1) {
        return ASharedMemory_getSize(lspdDex);
    }
    return 0;
}

extern "C"
JNIEXPORT jobject
Java_org_lsposed_lspd_service_ObfuscationManager_obfuscateDex(JNIEnv *env, jclass /*clazz*/,
                                                       jobject memory) {
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

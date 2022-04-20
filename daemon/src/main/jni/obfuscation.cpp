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
#include "logging.h"

using namespace lsplant;
namespace {
std::mutex init_lock{};
std::map<const std::string, std::string> signatures = {
        {"Lde/robv/android/xposed/", ""},
        { "Landroid/app/AndroidApp", ""},
        { "Landroid/content/res/XRes", ""},
        { "Landroid/content/res/XModule", ""},
        { "Lorg/lsposed/lspd/core/", ""},
        { "Lorg/lsposed/lspd/nativebridge/", ""},
        { "Lorg/lsposed/lspd/service/", ""},
};

jclass class_file_descriptor;
jmethodID method_file_descriptor_ctor;

jclass class_shared_memory;
jmethodID method_shared_memory_ctor;

bool inited = false;
}

static std::string to_java(const std::string &signature) {
    std::string java(signature, 1);
    replace(java.begin(), java.end(), '/', '.');
    return java;
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

    auto regen = [](std::string_view original_signature) {
        static auto& chrs = "abcdefghijklmnopqrstuvwxyz"
                            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        thread_local static std::mt19937 rg{std::random_device{}()};
        thread_local static std::uniform_int_distribution<std::string::size_type> pick(0, sizeof(chrs) - 2);
        thread_local static std::uniform_int_distribution<std::string::size_type> choose_slash(0, 10);

        std::string out;
        size_t length = original_signature.size();
        out.reserve(length);
        out += "L";

        for (size_t i = 1; i < length - 1; i++) {
            if (choose_slash(rg) > 8 &&                         // 80% alphabet + 20% slashes
                out[i - 1] != '/' &&                                // slashes could not stick together
                i != 1 &&                                           // the first character should not be slash
                i != length - 2) {                                  // and the last character
                out += "/";
            } else {
                out += chrs[pick(rg)];
            }
        }

        out += "/";
        return out;
    };

    for (auto &i: signatures) {
        i.second = regen(i.first);
        LOGD("%s => %s", i.first.c_str(), i.second.c_str());
    }

    LOGD("ObfuscationManager init successfully");
    inited = true;
}

// https://stackoverflow.com/questions/4844022/jni-create-hashmap with modifications
jobject stringMapToJavaHashMap(JNIEnv *env, const decltype(signatures)& map) {
    jclass mapClass = env->FindClass("java/util/HashMap");
    if(mapClass == nullptr)
        return nullptr;

    jmethodID init = env->GetMethodID(mapClass, "<init>", "()V");
    jobject hashMap = env->NewObject(mapClass, init);
    jmethodID put = env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    auto citr = map.begin();
    for( ; citr != map.end(); ++citr) {
        jstring keyJava = env->NewStringUTF(citr->first.c_str());
        jstring valueJava = env->NewStringUTF(citr->second.c_str());

        env->CallObjectMethod(hashMap, put, keyJava, valueJava);

        env->DeleteLocalRef(keyJava);
        env->DeleteLocalRef(valueJava);
    }

    auto hashMapGobal = static_cast<jobject>(env->NewGlobalRef(hashMap));
    env->DeleteLocalRef(hashMap);
    env->DeleteLocalRef(mapClass);

    return hashMapGobal;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_lsposed_lspd_service_ObfuscationManager_getSignatures(JNIEnv *env, [[maybe_unused]] jclass obfuscation_manager) {
    maybeInit(env);
    static jobject signatures_jni = nullptr;
    if (signatures_jni) return signatures_jni;
    decltype(signatures) signatures_java;
    for (const auto &i: signatures) {
        signatures_java[to_java(i.first)] = to_java(i.second);
    }
    signatures_jni = stringMapToJavaHashMap(env, signatures_java);
    return signatures_jni;
}

static int obfuscateDex(const void *dex, size_t size) {
    // const char* new_sig = obfuscated_signature.c_str();
    dex::Reader reader{reinterpret_cast<const dex::u1*>(dex), size};

    reader.CreateFullIr();
    auto ir = reader.GetIr();
    for (auto &i: ir->strings) {
        const char *s = i->c_str();
        for (const auto &signature: signatures) {
            char* p = const_cast<char *>(strstr(s, signature.first.c_str()));
            if (p) {
                auto new_sig = signature.second.c_str();
                // NOLINTNEXTLINE bugprone-not-null-terminated-result
                memcpy(p, new_sig, strlen(new_sig));
            }
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

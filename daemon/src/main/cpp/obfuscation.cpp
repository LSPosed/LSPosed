//
// Created by Kotori2 on 2021/12/1.
//

#include <jni.h>
#include <unistd.h>
#include <sys/mman.h>
#include <android/sharedmem.h>
#include <android/sharedmem_jni.h>
#include <slicer/dex_utf8.h>
#include <fcntl.h>
#include "slicer/reader.h"
#include "slicer/writer.h"
#include "Obfuscation.h"
// TODO: BAD
#include "../../../../core/src/main/cpp/main/include/config.h"

static jobject lspdDex = nullptr;

jobject new_sharedmem(JNIEnv* env, jint size) {
    jclass clazz = env->FindClass("android/os/SharedMemory");
    auto *ref = env->NewGlobalRef(clazz);
    jmethodID mid = env->GetStaticMethodID(clazz, "create", "(Ljava/lang/String;I)Landroid/os/SharedMemory;");
    jstring empty_str = env->NewStringUTF("");
    jobject new_mem = env->CallStaticObjectMethod(clazz, mid, empty_str, static_cast<jint>(size));
    env->DeleteGlobalRef(ref);
    env->DeleteLocalRef(empty_str);
    return new_mem;
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_lsposed_lspd_service_LSPApplicationService_preloadDex(JNIEnv *env, jclass ) {
    using namespace std::string_literals;
    // TODO: Lock?
    if (lspdDex) return ASharedMemory_dupFromJava(env, lspdDex);
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
        LOGE("Read dex failed: %s", strerror(errno));
        return -1;
    }

    auto new_dex = Obfuscation::obfuscateDex(addr, size);
    LOGD("LSPApplicationService::preloadDex: %p, size=%zu", new_dex.data(), new_dex.size());
    auto new_mem = new_sharedmem(env, new_dex.size());
    lspdDex = env->NewGlobalRef(new_mem);
    auto new_fd = ASharedMemory_dupFromJava(env, lspdDex);
    auto new_addr = mmap(nullptr, new_dex.size(), PROT_READ | PROT_WRITE, MAP_SHARED, new_fd, 0);
    memmove(new_addr, new_dex.data(), new_dex.size());

    return new_fd;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_lsposed_lspd_service_LSPApplicationService_getPreloadedDexSize(JNIEnv *env, jclass ) {
    if (lspdDex) {
        auto fd = ASharedMemory_dupFromJava(env, lspdDex);
        return ASharedMemory_getSize(fd);
    }
    return 0;
}

extern "C"
JNIEXPORT jobject
Java_org_lsposed_lspd_service_LSPApplicationService_obfuscateDex(JNIEnv *env, jclass /*clazz*/,
                                                       jobject memory) {
    int fd = ASharedMemory_dupFromJava(env, memory);
    auto size = ASharedMemory_getSize(fd);
    ustring mem_wrapper;
    mem_wrapper.resize(size);
    read(fd, mem_wrapper.data(), size);

    void *mem = mem_wrapper.data();

    auto new_dex = Obfuscation::obfuscateDex(mem, size);

    // create new SharedMem since it cannot be resized
    auto new_mem = new_sharedmem(env, new_dex.size());
    int new_fd = ASharedMemory_dupFromJava(env, new_mem);

    mem = mmap(nullptr, new_dex.size(), PROT_READ | PROT_WRITE, MAP_SHARED, new_fd, 0);
    if (mem == MAP_FAILED) {
        // LOGE("Failed to map new dex to memory?");
    }
    memcpy(mem, new_dex.data(), new_dex.size());
    ASharedMemory_setProt(fd, PROT_READ);
    return new_mem;
}
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
#include <jni.h>

extern "C"
JNIEXPORT jobject
Java_org_lsposed_lspd_service_ObfuscationService_obfuscateDex(JNIEnv *env, jclass /*clazz*/,
                                                       jobject memory) {
    int fd = ASharedMemory_dupFromJava(env, memory);
    auto size = ASharedMemory_getSize(fd);
    ustring mem_wrapper;
    mem_wrapper.resize(size);
    read(fd, mem_wrapper.data(), size);

    void *mem = mem_wrapper.data();

    auto new_dex = Obfuscation::obfuscateDex(mem, size);

    // create new SharedMem since it cannot be resized
    jclass clazz = env->FindClass("android/os/SharedMemory");
    auto *ref = env->NewGlobalRef(clazz);
    jmethodID mid = env->GetStaticMethodID(clazz, "create", "(Ljava/lang/String;I)Landroid/os/SharedMemory;");
    jstring empty_str = env->NewStringUTF("");
    jobject new_mem = env->CallStaticObjectMethod(clazz, mid, empty_str, static_cast<jint>(new_dex.size()));
    int new_fd = ASharedMemory_dupFromJava(env, new_mem);

    env->DeleteGlobalRef(ref);
    env->DeleteLocalRef(empty_str);

    mem = mmap(nullptr, new_dex.size(), PROT_READ | PROT_WRITE, MAP_SHARED, new_fd, 0);
    if (mem == MAP_FAILED) {
        // LOGE("Failed to map new dex to memory?");
    }
    memcpy(mem, new_dex.data(), new_dex.size());
    ASharedMemory_setProt(fd, PROT_READ);
    return new_mem;
}

#include <jni.h>
#include <native_util.h>
#include <art/runtime/gc/collector/gc_type.h>
#include <art/runtime/gc/heap.h>
#include <nativehelper/jni_macros.h>
#include "art_heap.h"

namespace lspd {


    static jint Heap_waitForGcToComplete(JNI_START) {
        art::gc::collector::GcType gcType = art::gc::Heap::Current()->WaitForGcToComplete(
                art::gc::GcCause::kGcCauseNone, art::Thread::Current().Get());
        return gcType;
    }

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(Heap, waitForGcToComplete, "()I")
    };

    void RegisterArtHeap(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("io.github.lsposed.lspd.art.Heap");
    }

}
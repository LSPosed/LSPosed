
#include <jni.h>
#include <native_util.h>
#include <art/runtime/gc/collector/gc_type.h>
#include <art/runtime/gc/heap.h>
#include <nativehelper/jni_macros.h>
#include "art_heap.h"

namespace lspd {


    LSP_DEF_NATIVE_METHOD(jint, Heap, waitForGcToComplete) {
        art::gc::collector::GcType gcType = art::gc::Heap::Current()->WaitForGcToComplete(
                art::gc::GcCause::kGcCauseNone, art::Thread::Current().Get());
        return gcType;
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(Heap, waitForGcToComplete, "()I")
    };

    void RegisterArtHeap(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(Heap);
    }

}
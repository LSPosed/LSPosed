
#include <jni.h>
#include <native_util.h>
#include <art/runtime/gc/collector/gc_type.h>
#include <art/runtime/gc/heap.h>
#include <nativehelper/jni_macros.h>
#include "art_heap.h"

namespace edxp {


    static jint Heap_waitForGcToComplete(JNI_START, jlong thread) {
        art::gc::collector::GcType gcType = art::gc::Heap::Current()->WaitForGcToComplete(
                art::gc::GcCause::kGcCauseNone, reinterpret_cast<void *>(thread));
        return gcType;
    }

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(Heap, waitForGcToComplete, "(J)I")
    };

    void RegisterArtHeap(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("com.elderdrivers.riru.edxp.art.Heap");
    }

}
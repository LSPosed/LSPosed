/*
 * Copyright (C) 2014 The Android Open Source Project
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

#ifndef ART_RUNTIME_GC_GC_CAUSE_H_
#define ART_RUNTIME_GC_GC_CAUSE_H_

#include <iosfwd>

namespace art {
    namespace gc {
// What caused the GC?
        enum GcCause {
            // Invalid GC cause used as a placeholder.
                    kGcCauseNone,
            // GC triggered by a failed allocation. Thread doing allocation is blocked waiting for GC before
            // retrying allocation.
                    kGcCauseForAlloc,
            // A background GC trying to ensure there is free memory ahead of allocations.
                    kGcCauseBackground,
            // An explicit System.gc() call.
                    kGcCauseExplicit,
            // GC triggered for a native allocation when NativeAllocationGcWatermark is exceeded.
            // (This may be a blocking GC depending on whether we run a non-concurrent collector).
                    kGcCauseForNativeAlloc,
            // GC triggered for a collector transition.
                    kGcCauseCollectorTransition,
            // Not a real GC cause, used when we disable moving GC (currently for GetPrimitiveArrayCritical).
                    kGcCauseDisableMovingGc,
            // Not a real GC cause, used when we trim the heap.
                    kGcCauseTrim,
            // Not a real GC cause, used to implement exclusion between GC and instrumentation.
                    kGcCauseInstrumentation,
            // Not a real GC cause, used to add or remove app image spaces.
                    kGcCauseAddRemoveAppImageSpace,
            // Not a real GC cause, used to implement exclusion between GC and debugger.
                    kGcCauseDebugger,
            // GC triggered for background transition when both foreground and background collector are CMS.
                    kGcCauseHomogeneousSpaceCompact,
            // Class linker cause, used to guard filling art methods with special values.
                    kGcCauseClassLinker,
            // Not a real GC cause, used to implement exclusion between code cache metadata and GC.
                    kGcCauseJitCodeCache,
            // Not a real GC cause, used to add or remove system-weak holders.
                    kGcCauseAddRemoveSystemWeakHolder,
            // Not a real GC cause, used to prevent hprof running in the middle of GC.
                    kGcCauseHprof,
            // Not a real GC cause, used to prevent GetObjectsAllocated running in the middle of GC.
                    kGcCauseGetObjectsAllocated,
            // GC cause for the profile saver.
                    kGcCauseProfileSaver,
        };
    }  // namespace gc
}  // namespace art
#endif  // ART_RUNTIME_GC_GC_CAUSE_H_
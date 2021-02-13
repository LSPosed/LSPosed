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
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
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
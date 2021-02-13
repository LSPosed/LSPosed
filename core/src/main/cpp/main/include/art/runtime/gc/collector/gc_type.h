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

#ifndef ART_RUNTIME_GC_COLLECTOR_GC_TYPE_H_
#define ART_RUNTIME_GC_COLLECTOR_GC_TYPE_H_

#include <iosfwd>

namespace art {
    namespace gc {

// Which types of collections are able to be performed.
        enum CollectorType {
            // No collector selected.
            kCollectorTypeNone,
            // Non concurrent mark-sweep.
            kCollectorTypeMS,
            // Concurrent mark-sweep.
            kCollectorTypeCMS,
            // Semi-space / mark-sweep hybrid, enables compaction.
            kCollectorTypeSS,
            // Heap trimming collector, doesn't do any actual collecting.
            kCollectorTypeHeapTrim,
            // A (mostly) concurrent copying collector.
            kCollectorTypeCC,
            // The background compaction of the concurrent copying collector.
            kCollectorTypeCCBackground,
            // Instrumentation critical section fake collector.
            kCollectorTypeInstrumentation,
            // Fake collector for adding or removing application image spaces.
            kCollectorTypeAddRemoveAppImageSpace,
            // Fake collector used to implement exclusion between GC and debugger.
            kCollectorTypeDebugger,
            // A homogeneous space compaction collector used in background transition
            // when both foreground and background collector are CMS.
            kCollectorTypeHomogeneousSpaceCompact,
            // Class linker fake collector.
            kCollectorTypeClassLinker,
            // JIT Code cache fake collector.
            kCollectorTypeJitCodeCache,
            // Hprof fake collector.
            kCollectorTypeHprof,
            // Fake collector for installing/removing a system-weak holder.
            kCollectorTypeAddRemoveSystemWeakHolder,
            // Fake collector type for GetObjectsAllocated
            kCollectorTypeGetObjectsAllocated,
            // Fake collector type for ScopedGCCriticalSection
            kCollectorTypeCriticalSection,
        };
        std::ostream& operator<<(std::ostream& os, CollectorType collector_type);
    }  // namespace gc
}  // namespace art
#endif  // ART_RUNTIME_GC_COLLECTOR_GC_TYPE_H_
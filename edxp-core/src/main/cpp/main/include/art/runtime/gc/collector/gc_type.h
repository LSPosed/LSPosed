/*
 * Copyright (C) 2011 The Android Open Source Project
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
#ifndef ART_RUNTIME_GC_COLLECTOR_GC_TYPE_H_
#define ART_RUNTIME_GC_COLLECTOR_GC_TYPE_H_

#include <iosfwd>

namespace art {
    namespace gc {
        namespace collector {
// The type of collection to be performed. The ordering of the enum matters, it is used to
// determine which GCs are run first.
            enum GcType {
                // Placeholder for when no GC has been performed.
                        kGcTypeNone,
                // Sticky mark bits GC that attempts to only free objects allocated since the last GC.
                        kGcTypeSticky,
                // Partial GC that marks the application heap but not the Zygote.
                        kGcTypePartial,
                // Full GC that marks and frees in both the application and Zygote heap.
                        kGcTypeFull,
                // Number of different GC types.
                        kGcTypeMax,
            };

            std::ostream &operator<<(std::ostream &os, const GcType &policy);
        }  // namespace collector
    }  // namespace gc
}  // namespace art
#endif  // ART_RUNTIME_GC_COLLECTOR_GC_TYPE_H_
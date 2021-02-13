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
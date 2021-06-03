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
 * Copyright (C) 2021 LSPosed Contributors
 */

#ifndef LSPOSED_SCOPED_GC_CRITICAL_SECTION_H
#define LSPOSED_SCOPED_GC_CRITICAL_SECTION_H

#include "gc_cause.h"
#include "collector_type.h"

namespace art {
    namespace gc {

        class GCCriticalSection {
        private:
            void* self_;
            const char* section_name_;
        };

        class ScopedGCCriticalSection {
            CREATE_MEM_FUNC_SYMBOL_ENTRY(void, constructor, void *thiz, void* self, GcCause cause, CollectorType collector_type) {
                if (UNLIKELY(thiz == nullptr)) return;
                if (LIKELY(constructorSym))
                    return constructorSym(thiz, self, cause, collector_type);
            }
            CREATE_MEM_FUNC_SYMBOL_ENTRY(void, destructor, void *thiz) {
                if (UNLIKELY(thiz == nullptr)) return;
                if (LIKELY(destructorSym))
                    return destructorSym(thiz);
            }
        public:
            ScopedGCCriticalSection(void *self, GcCause cause, CollectorType collector_type) {
                constructor(this, self, cause, collector_type);
            }
            ~ScopedGCCriticalSection() {
                destructor(this);
            }

            static void Setup(const SandHook::ElfImg &handle) {
                RETRIEVE_MEM_FUNC_SYMBOL(constructor, "_ZN3art2gc23ScopedGCCriticalSectionC2EPNS_6ThreadENS0_7GcCauseENS0_13CollectorTypeE");
                RETRIEVE_MEM_FUNC_SYMBOL(destructor, "_ZN3art2gc23ScopedGCCriticalSectionD2Ev");
            }
        private:
            GCCriticalSection critical_section_;
            const char* old_no_suspend_reason_;
        };
    }
}

#endif //LSPOSED_SCOPED_GC_CRITICAL_SECTION_H

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

#ifndef LSPOSED_THREAD_LIST_H
#define LSPOSED_THREAD_LIST_H

namespace art {
    namespace thread_list {

        class ScopedSuspendAll {
            CREATE_MEM_FUNC_SYMBOL_ENTRY(void, constructor, void *thiz, const char * cause, bool long_suspend) {
                if (UNLIKELY(thiz == nullptr)) return;
                if (LIKELY(constructorSym))
                    return constructorSym(thiz, cause, long_suspend);
            }
            CREATE_MEM_FUNC_SYMBOL_ENTRY(void, destructor, void *thiz) {
                if (UNLIKELY(thiz == nullptr)) return;
                if (LIKELY(destructorSym))
                    return destructorSym(thiz);
            }
        public:
            ScopedSuspendAll(const char * cause, bool long_suspend) {
                constructor(this, cause, long_suspend);
            }
            ~ScopedSuspendAll() {
                destructor(this);
            }

            static void Setup(const SandHook::ElfImg &handle) {
                RETRIEVE_MEM_FUNC_SYMBOL(constructor, "_ZN3art16ScopedSuspendAllC2EPKcb");
                RETRIEVE_MEM_FUNC_SYMBOL(destructor, "_ZN3art16ScopedSuspendAllD2Ev");
            }
        };
    }
}

#endif //LSPOSED_THREAD_LIST_H

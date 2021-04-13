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

//
// Created by kotori on 2/7/21.
//

#include "symbol_cache.h"
#include "elf_util.h"
#include <dobby.h>
#include "macros.h"
#include "config.h"
#include <vector>
#include <logging.h>

namespace lspd {
    bool sym_initialized = false;
    void *sym_do_dlopen = nullptr;
    void *sym_system_property_get = nullptr;
    void *sym_get_property = nullptr;
    void *handle_libart = nullptr;

    struct soinfo;

    soinfo *solist = nullptr;
    soinfo *somain = nullptr;

    template<typename T>
    constexpr inline T *getStaticVariable(const SandHook::ElfImg &linker, std::string_view name) {
        auto *addr = reinterpret_cast<T **>(linker.getSymbAddress(name));
        return addr == nullptr ? nullptr : *addr;
    }

    struct soinfo {
        soinfo *next() {
            return *(soinfo **) ((uintptr_t) this + solist_next_offset);
        }

        const char *get_realpath() {
            return get_realpath_sym ? get_realpath_sym(this) : ((std::string *) (
                    (uintptr_t) this +
                    solist_realpath_offset))->c_str();
        }

        const char *get_soname() {
            return get_soname_sym ? get_soname_sym(this) : *((const char **) (
                    (uintptr_t) this +
                    solist_realpath_offset - sizeof(void *)));
        }

        void *to_handle() {
            return to_handle_sym ? to_handle_sym(this) : nullptr;
        }

        static bool setup(const SandHook::ElfImg &linker) {
            get_realpath_sym = reinterpret_cast<decltype(get_realpath_sym)>(linker.getSymbAddress(
                    "__dl__ZNK6soinfo12get_realpathEv"));
            get_soname_sym = reinterpret_cast<decltype(get_soname_sym)>(linker.getSymbAddress(
                    "__dl__ZNK6soinfo10get_sonameEv"));
            to_handle_sym = reinterpret_cast<decltype(to_handle_sym)>(linker.getSymbAddress(
                    "__dl__ZN6soinfo9to_handleEv"));
            auto vsdo = getStaticVariable<soinfo>(linker, "__dl__ZL4vdso");
            for (size_t i = 0; i < 1024 / sizeof(void *); i++) {
                auto *possible_next = *(void **) ((uintptr_t) solist + i * sizeof(void *));
                if (possible_next == somain || (vsdo && possible_next == vsdo)) {
                    solist_next_offset = i * sizeof(void *);
                    return (get_realpath_sym && get_soname_sym && to_handle_sym);
                }
            }
            LOGW("%s", "failed to search next offset");
            // shortcut
            return false;
        }

#ifdef __LP64__
        inline static size_t solist_next_offset = 0x30;
        constexpr static size_t solist_realpath_offset = 0x1a8;
#else
        inline static size_t solist_next_offset = 0xa4;
        constexpr static size_t solist_realpath_offset = 0x174;
#endif

        // since Android 8
        inline static const char *(*get_realpath_sym)(soinfo *) = nullptr;

        inline static const char *(*get_soname_sym)(soinfo *) = nullptr;

        inline static void *(*to_handle_sym)(soinfo *) = nullptr;
    };

    std::vector<soinfo *> linker_get_solist() {
        std::vector<soinfo *> linker_solist{};
        for (auto *iter = solist; iter; iter = iter->next()) {
            linker_solist.push_back(iter);
        }
        return linker_solist;
    }

    void *findLibArt() {
        for (const auto &soinfo : linker_get_solist()) {
            if (const auto &real_path = soinfo->get_realpath(), &soname = soinfo->get_soname();
                    (real_path &&
                     std::string_view(real_path).find(kLibArtName) != std::string_view::npos) ||
                    (soname &&
                     std::string_view(soname).find(kLibArtName) != std::string_view::npos)) {
                return soinfo->to_handle();
            }
        }
        return nullptr;
    }

    void InitSymbolCache() {
        if (UNLIKELY(sym_initialized)) return;
        LOGD("InitSymbolCache");
        auto linker = SandHook::ElfImg(kLinkerPath.c_str());
        auto libc = SandHook::ElfImg(kLibcPath.c_str());
        auto libbase = SandHook::ElfImg(kLibbasePath.c_str());
        sym_initialized = (solist = getStaticVariable<soinfo>(linker, "__dl__ZL6solist")) &&
                          (somain = getStaticVariable<soinfo>(linker, "__dl__ZL6somain")) &&
                          (sym_do_dlopen = reinterpret_cast<void *>(linker.getSymbAddress(
                                  "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv"))) &&
                          (sym_system_property_get = reinterpret_cast<void *>(libc.getSymbAddress(
                                  "__system_property_get"))) &&
                          (sym_get_property = reinterpret_cast<void *>(libbase.getSymbAddress(
                                  "_ZN7android4base11GetPropertyERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_"))) &&
                          soinfo::setup(linker) && (handle_libart = findLibArt());
        if (UNLIKELY(!sym_initialized)) {
            LOGE("Init symbol cache failed");
        }
    }
}

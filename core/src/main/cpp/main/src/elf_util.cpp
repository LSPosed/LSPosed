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
 * Copyright (C) 2019 Swift Gan
 * Copyright (C) 2021 LSPosed Contributors
 */
#include <malloc.h>
#include <cstring>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <cassert>
#include <sys/stat.h>
#include "logging.h"
#include "elf_util.h"

using namespace SandHook;

template<typename T>
inline constexpr auto offsetOf(ElfW(Ehdr) *head, ElfW(Off) off) {
    return reinterpret_cast<std::conditional_t<std::is_pointer_v<T>, T, T *>>(
            reinterpret_cast<uintptr_t>(head) + off);
}

ElfImg::ElfImg(std::string_view elf) : elf(elf) {
    //load elf
    int fd = open(elf.data(), O_RDONLY);
    if (fd < 0) {
        LOGE("failed to open %s", elf.data());
        return;
    }

    size = lseek(fd, 0, SEEK_END);
    if (size <= 0) {
        LOGE("lseek() failed for %s", elf.data());
    }

    header = reinterpret_cast<decltype(header)>(mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0));

    close(fd);

    section_header = offsetOf<decltype(section_header)>(header, header->e_shoff);

    auto shoff = reinterpret_cast<uintptr_t>(section_header);
    char *section_str = offsetOf<char *>(header, section_header[header->e_shstrndx].sh_offset);

    for (int i = 0; i < header->e_shnum; i++, shoff += header->e_shentsize) {
        auto *section_h = (ElfW(Shdr) *) shoff;
        char *sname = section_h->sh_name + section_str;
        auto entsize = section_h->sh_entsize;
        switch (section_h->sh_type) {
            case SHT_DYNSYM: {
                if (bias == -4396) {
                    dynsym = section_h;
                    dynsym_offset = section_h->sh_offset;
                    dynsym_start = offsetOf<decltype(dynsym_start)>(header, dynsym_offset);
                }
                break;
            }
            case SHT_SYMTAB: {
                if (strcmp(sname, ".symtab") == 0) {
                    symtab = section_h;
                    symtab_offset = section_h->sh_offset;
                    symtab_size = section_h->sh_size;
                    symtab_count = symtab_size / entsize;
                    symtab_start = offsetOf<decltype(symtab_start)>(header, symtab_offset);
                }
                break;
            }
            case SHT_STRTAB: {
                if (bias == -4396) {
                    strtab = section_h;
                    symstr_offset = section_h->sh_offset;
                    strtab_start = offsetOf<decltype(strtab_start)>(header, symstr_offset);
                }
                if (strcmp(sname, ".strtab") == 0) {
                    symstr_offset_for_symtab = section_h->sh_offset;
                }
                break;
            }
            case SHT_PROGBITS: {
                if (strtab == nullptr || dynsym == nullptr) break;
                if (bias == -4396) {
                    bias = (off_t) section_h->sh_addr - (off_t) section_h->sh_offset;
                }
                break;
            }
            case SHT_HASH: {
                auto *d_un = offsetOf<ElfW(Word)>(header, section_h->sh_offset);
                nbucket_ = d_un[0];
                bucket_ = d_un + 2;
                chain_ = bucket_ + nbucket_;
                break;
            }
            case SHT_GNU_HASH: {
                auto *d_buf = reinterpret_cast<ElfW(Word) *>(((size_t) header) +
                                                             section_h->sh_offset);
                gnu_nbucket_ = d_buf[0];
                gnu_symndx_ = d_buf[1];
                gnu_bloom_size_ = d_buf[2];
                gnu_shift2_ = d_buf[3];
                gnu_bloom_filter_ = reinterpret_cast<decltype(gnu_bloom_filter_)>(d_buf + 4);
                gnu_bucket_ = reinterpret_cast<decltype(gnu_bucket_)>(gnu_bloom_filter_ +
                                                                      gnu_bloom_size_);
                gnu_chain_ = gnu_bucket_ + gnu_nbucket_ - gnu_symndx_;
                break;
            }
        }
    }

    //load module base
    base = getModuleBase();
}

ElfW(Addr) ElfImg::ElfLookup(std::string_view name, uint32_t hash) const {
    if (nbucket_ == 0) return 0;

    char *strings = (char *) strtab_start;

    for (auto n = bucket_[hash % nbucket_]; n != 0; n = chain_[n]) {
        auto *sym = dynsym_start + n;
        if (name == strings + sym->st_name) {
            return sym->st_value;
        }
    }
    return 0;
}

ElfW(Addr) ElfImg::GnuLookup(std::string_view name, uint32_t hash) const {
    static constexpr auto bloom_mask_bits = sizeof(ElfW(Addr)) * 8;

    if (gnu_nbucket_ == 0 || gnu_bloom_size_ == 0) return 0;

    auto bloom_word = gnu_bloom_filter_[(hash / bloom_mask_bits) % gnu_bloom_size_];
    uintptr_t mask = 0
                     | (uintptr_t) 1 << (hash % bloom_mask_bits)
                     | (uintptr_t) 1 << ((hash >> gnu_shift2_) % bloom_mask_bits);
    if ((mask & bloom_word) == mask) {
        auto sym_index = gnu_bucket_[hash % gnu_nbucket_];
        if (sym_index >= gnu_symndx_) {
            char *strings = (char *) strtab_start;
            do {
                auto *sym = dynsym_start + sym_index;
                if (((gnu_chain_[sym_index] ^ hash) >> 1) == 0
                    && name == strings + sym->st_name) {
                    return sym->st_value;
                }
            } while ((gnu_chain_[sym_index++] & 1) == 0);
        }
    }
    return 0;
}

ElfW(Addr) ElfImg::LinearLookup(std::string_view name) const {
    if (symtabs_.empty()) {
        symtabs_.reserve(symtab_count);
        if (symtab_start != nullptr && symstr_offset_for_symtab != 0) {
            for (int i = 0; i < symtab_count; i++) {
                unsigned int st_type = ELF_ST_TYPE(symtab_start[i].st_info);
                const char *st_name = offsetOf<const char *>(header, symstr_offset_for_symtab +
                                                                     symtab_start[i].st_name);
                if ((st_type == STT_FUNC || st_type == STT_OBJECT) && symtab_start[i].st_size) {
                    symtabs_.emplace(st_name, &symtab_start[i]);
                }
            }
        }
    }
    if (auto i = symtabs_.find(name); i != symtabs_.end()) {
        return i->second->st_value;
    } else {
        return 0;
    }
}


ElfImg::~ElfImg() {
    //open elf file local
    if (buffer) {
        free(buffer);
        buffer = nullptr;
    }
    //use mmap
    if (header) {
        munmap(header, size);
    }
}

ElfW(Addr)
ElfImg::getSymbOffset(std::string_view name, uint32_t gnu_hash, uint32_t elf_hash) const {
    if (auto offset = GnuLookup(name, gnu_hash); offset > 0) {
        LOGD("found %s %p in %s in dynsym by gnuhash", name.data(),
             reinterpret_cast<void *>(offset), elf.data());
        return offset;
    } else if (offset = ElfLookup(name, elf_hash); offset > 0) {
        LOGD("found %s %p in %s in dynsym by elfhash", name.data(),
             reinterpret_cast<void *>(offset), elf.data());
        return offset;
    } else if (offset = LinearLookup(name); offset > 0) {
        LOGD("found %s %p in %s in symtab by linear lookup", name.data(),
             reinterpret_cast<void *>(offset), elf.data());
        return offset;
    } else {
        return 0;
    }

}

void *ElfImg::getModuleBase() const {
    char buff[256];
    off_t load_addr;
    int found = 0;
    FILE *maps = fopen("/proc/self/maps", "r");

    char name[PATH_MAX] = {'\0'};

    strncpy(name, elf.data(), PATH_MAX);
    {
        struct stat buf{};
        while (lstat(name, &buf) == 0 && S_ISLNK(buf.st_mode)) {
            if (auto s = readlink(name, name, PATH_MAX); s >= 0) {
                name[s] = '\0';
            } else {
                fclose(maps);
                LOGE("cannot read link for %s with %s", name, strerror(errno));
                return nullptr;
            }
        }
    }

//    fs::path name(elf);
//    std::error_code ec;
//    while(fs::is_symlink(name, ec) && !ec) {
//        name = fs::read_symlink(name);
//    }

    while (fgets(buff, sizeof(buff), maps)) {
        if ((strstr(buff, "r-xp") || strstr(buff, "r--p")) && strstr(buff, name)) {
            found = 1;
            LOGD("found: %s", buff);
            break;
        }
    }

    if (!found) {
        LOGE("failed to read load address for %s", name);
        fclose(maps);
        return nullptr;
    }

    if (char *next = buff; load_addr = strtoul(buff, &next, 16), next == buff) {
        LOGE("failed to read load address for %s", name);
    }

    fclose(maps);

    LOGD("get module base %s: %lx", name, load_addr);

    return reinterpret_cast<void *>(load_addr);
}


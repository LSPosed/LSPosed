// From https://github.com/ganyao114/SandHook/blob/master/hooklib/src/main/cpp/utils/elf_util.cpp
//
// Created by Swift Gan on 2019/3/14.
//
#include <malloc.h>
#include <cstring>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <cassert>
#include <filesystem>
#include <sys/stat.h>
#include "logging.h"
#include "elf_util.h"

using namespace SandHook;
namespace fs = std::filesystem;

ElfImg::ElfImg(const char *elf) {
    this->elf = elf;
    //load elf
    int fd = open(elf, O_RDONLY);
    if (fd < 0) {
        LOGE("failed to open %s", elf);
        return;
    }

    size = lseek(fd, 0, SEEK_END);
    if (size <= 0) {
        LOGE("lseek() failed for %s", elf);
    }

    header = reinterpret_cast<Elf_Ehdr *>(mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0));

    close(fd);

    section_header = reinterpret_cast<Elf_Shdr *>(((size_t) header) + header->e_shoff);

    auto shoff = reinterpret_cast<size_t>(section_header);
    char *section_str = reinterpret_cast<char *>(section_header[header->e_shstrndx].sh_offset +
                                                 ((size_t) header));

    for (int i = 0; i < header->e_shnum; i++, shoff += header->e_shentsize) {
        auto *section_h = (Elf_Shdr *) shoff;
        char *sname = section_h->sh_name + section_str;
        Elf_Off entsize = section_h->sh_entsize;
        switch (section_h->sh_type) {
            case SHT_DYNSYM:
                if (bias == -4396) {
                    dynsym = section_h;
                    dynsym_offset = section_h->sh_offset;
                    dynsym_size = section_h->sh_size;
                    dynsym_count = dynsym_size / entsize;
                    dynsym_start = reinterpret_cast<Elf_Sym *>(((size_t) header) + dynsym_offset);
                }
                break;
            case SHT_SYMTAB:
                if (strcmp(sname, ".symtab") == 0) {
                    symtab = section_h;
                    symtab_offset = section_h->sh_offset;
                    symtab_size = section_h->sh_size;
                    symtab_count = symtab_size / entsize;
                    symtab_start = reinterpret_cast<Elf_Sym *>(((size_t) header) + symtab_offset);
                }
                break;
            case SHT_STRTAB:
                if (bias == -4396) {
                    strtab = section_h;
                    symstr_offset = section_h->sh_offset;
                    strtab_start = reinterpret_cast<Elf_Sym *>(((size_t) header) + symstr_offset);
                }
                if (strcmp(sname, ".strtab") == 0) {
                    symstr_offset_for_symtab = section_h->sh_offset;
                }
                break;
            case SHT_PROGBITS:
                if (strtab == nullptr || dynsym == nullptr) break;
                if (bias == -4396) {
                    bias = (off_t) section_h->sh_addr - (off_t) section_h->sh_offset;
                }
                break;
        }
    }

    //load module base
    base = getModuleBase();
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

Elf_Addr ElfImg::getSymbOffset(const char *name) const {
    Elf_Addr _offset;

    //search dynmtab
    if (dynsym_start != nullptr && strtab_start != nullptr) {
        Elf_Sym *sym = dynsym_start;
        char *strings = (char *) strtab_start;
        int k;
        for (k = 0; k < dynsym_count; k++, sym++)
            if (strcmp(strings + sym->st_name, name) == 0) {
                _offset = sym->st_value;
                LOGD("find %s: %p", elf, reinterpret_cast<void *>(_offset));
                return _offset;
            }
    }

    //search symtab
    if (symtab_start != nullptr && symstr_offset_for_symtab != 0) {
        for (int i = 0; i < symtab_count; i++) {
            unsigned int st_type = ELF_ST_TYPE(symtab_start[i].st_info);
            char *st_name = reinterpret_cast<char *>(((size_t) header) + symstr_offset_for_symtab +
                                                     symtab_start[i].st_name);
            if ((st_type == STT_FUNC || st_type == STT_OBJECT) && symtab_start[i].st_size) {
                if (strcmp(st_name, name) == 0) {
                    _offset = symtab_start[i].st_value;
                    LOGD("find %s: %p", elf, reinterpret_cast<void *>(_offset));
                    return _offset;
                }
            }
        }
    }
    return 0;
}

Elf_Addr ElfImg::getSymbAddress(const char *name) const {
    Elf_Addr offset = getSymbOffset(name);
    Elf_Addr res;
    if (offset > 0 && base != nullptr) {
        res = static_cast<Elf_Addr>((size_t) base + offset - bias);
    } else {
        res = 0;
    }
    if (res == 0) {
        LOGE("fail to get symbol %s from %s ", name, elf);
    } else {
        LOGD("got symbol %s form %s with %p", name, elf, (void *) res);
    }
    return res;
}

void *ElfImg::getModuleBase() const {
    char buff[256];
    off_t load_addr;
    int found = 0;
    FILE *maps = fopen("/proc/self/maps", "r");

    char name[PATH_MAX] = {'\0'};

    strncpy(name, elf, PATH_MAX);
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

    if (sscanf(buff, "%lx", &load_addr) != 1)
        LOGE("failed to read load address for %s", name);

    fclose(maps);

    LOGD("get module base %s: %lu", name, load_addr);

    return reinterpret_cast<void *>(load_addr);
}

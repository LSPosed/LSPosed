//
// Created by Swift Gan on 2019/3/14.
//
#ifndef SANDHOOK_ELF_UTIL_H
#define SANDHOOK_ELF_UTIL_H

#include <linux/elf.h>

#if defined(__LP64__)
typedef Elf64_Ehdr Elf_Ehdr;
typedef Elf64_Shdr Elf_Shdr;
typedef Elf64_Addr Elf_Addr;
typedef Elf64_Dyn Elf_Dyn;
typedef Elf64_Rela Elf_Rela;
typedef Elf64_Sym Elf_Sym;
typedef Elf64_Off Elf_Off;

#define ELF_R_SYM(i) ELF64_R_SYM(i)
#else
typedef Elf32_Ehdr Elf_Ehdr;
typedef Elf32_Shdr Elf_Shdr;
typedef Elf32_Addr Elf_Addr;
typedef Elf32_Dyn Elf_Dyn;
typedef Elf32_Rel Elf_Rela;
typedef Elf32_Sym Elf_Sym;
typedef Elf32_Off Elf_Off;

#define ELF_R_SYM(i) ELF32_R_SYM(i)
#endif

namespace SandHook {

    class ElfImg {
    public:

        ElfImg(const char* elf);

        Elf_Addr getSymbOffset(const char* name);

        void* getModuleBase(const char* name);

        Elf_Addr getSymbAddress(const char* name);

        ~ElfImg();

    private:
        const char* elf = nullptr;
        void* base = nullptr;
        char* buffer = nullptr;
        off_t size = 0;
        off_t bias = -4396;
        Elf_Ehdr* header = nullptr;
        Elf_Shdr* section_header = nullptr;
        Elf_Shdr* symtab = nullptr;
        Elf_Shdr* strtab = nullptr;
        Elf_Shdr* dynsym = nullptr;
        Elf_Off dynsym_count = 0;
        Elf_Sym* symtab_start = nullptr;
        Elf_Sym* dynsym_start = nullptr;
        Elf_Sym* strtab_start = nullptr;
        Elf_Off symtab_count = 0;
        Elf_Off symstr_offset = 0;
        Elf_Off symstr_offset_for_symtab = 0;
        Elf_Off symtab_offset = 0;
        Elf_Off dynsym_offset = 0;
        Elf_Off symtab_size = 0;
        Elf_Off dynsym_size = 0;
    };

}

#endif //SANDHOOK_ELF_UTIL_H

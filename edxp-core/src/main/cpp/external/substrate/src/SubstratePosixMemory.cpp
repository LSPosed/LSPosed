/* Cydia Substrate - Powerful Code Insertion Platform
 * Copyright (C) 2008-2011  Jay Freeman (saurik)
*/

/* GNU Lesser General Public License, Version 3 {{{ */
/*
 * Substrate is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Substrate is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Substrate.  If not, see <http://www.gnu.org/licenses/>.
**/
/* }}} */

#define SubstrateInternal
#include "CydiaSubstrate.h"
#include "SubstrateLog.hpp"

#include <sys/mman.h>

#include <errno.h>
#include <stdio.h>
#include <unistd.h>

extern "C" void __clear_cache (void *beg, void *end);

struct __SubstrateMemory {
    void *address_;
    size_t width_;

    __SubstrateMemory(void *address, size_t width) :
        address_(address),
        width_(width)
    {
    }
};

extern "C" SubstrateMemoryRef SubstrateMemoryCreate(SubstrateAllocatorRef allocator, SubstrateProcessRef process, void *data, size_t size) {
    if (allocator != NULL) {
        MSLog(MSLogLevelError, "MS:Error:allocator != %d", 0);
        return NULL;
    }

    if (size == 0)
        return NULL;

    long page(sysconf(_SC_PAGESIZE)); // Portable applications should employ sysconf(_SC_PAGESIZE) instead of getpagesize

    uintptr_t base(reinterpret_cast<uintptr_t>(data) / page * page);
    size_t width(((reinterpret_cast<uintptr_t>(data) + size - 1) / page + 1) * page - base);
    void *address(reinterpret_cast<void *>(base));

    if (mprotect(address, width, PROT_READ | PROT_WRITE | PROT_EXEC) == -1) {
        MSLog(MSLogLevelError, "MS:Error:mprotect() = %d", errno);
        return NULL;
    }

    return new __SubstrateMemory(address, width);
}

extern "C" void SubstrateMemoryRelease(SubstrateMemoryRef memory) {
    if (mprotect(memory->address_, memory->width_, PROT_READ | PROT_WRITE | PROT_EXEC) == -1)
        MSLog(MSLogLevelError, "MS:Error:mprotect() = %d", errno);

    __clear_cache(reinterpret_cast<char *>(memory->address_), reinterpret_cast<char *>(memory->address_) + memory->width_);

    delete memory;
}

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
 * Copyright (C) 2022 LSPosed Contributors
 */

#pragma once
#include <string>
#include "jni_helper.h"

static std::string obfuscated_signature;
static const std::string old_signature = "Lde/robv/android/xposed";
static int lspdDex = -1;
static std::mutex dex_lock;

static jclass class_file_descriptor;
static jmethodID method_file_descriptor_ctor;

static jclass class_shared_memory;
static jmethodID method_shared_memory_ctor;

class WA: public dex::Writer::Allocator {
    // addr: {size, fd}
    std::unordered_map<void*, std::pair<size_t, int>> allocated_;
public:
    inline void* Allocate(size_t size) override {
        auto fd = ASharedMemory_create("", size);
        auto *mem = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
        allocated_[mem] = {size, fd};
        return mem;
    }
    inline void Free(void* ptr) override {
        auto alloc_data = allocated_.at(ptr);
        close(alloc_data.second);
        allocated_.erase(ptr);
    }
    inline int GetFd(void* ptr) {
        auto alloc_data = allocated_.find(ptr);
        if (alloc_data == allocated_.end()) return -1;
        return (*alloc_data).second.second;
    }
};
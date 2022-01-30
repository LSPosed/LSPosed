//
// Created by Kotori0 on 2021/12/2.
//

#include <cstddef>
#include <sys/mman.h>
#include <unordered_map>
#include "slicer/reader.h"
#include "slicer/writer.h"
#include "Obfuscation.h"

class WA: public dex::Writer::Allocator {
    std::unordered_map<void*, size_t> allocated_;
public:
    void* Allocate(size_t size) override {
        auto *mem = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        allocated_[mem] = size;
        return mem;
    }
    void Free(void* ptr) override {
        munmap(ptr, allocated_[ptr]);
        allocated_.erase(ptr);
    }
};

ustring Obfuscation::obfuscateDex(void *dex, size_t size) {
    const char* new_sig = "Lac/ksmm/notioss/lspdaa";
    dex::Reader reader{reinterpret_cast<dex::u1*>(dex), size};

    reader.CreateFullIr();
    auto ir = reader.GetIr();
    for (auto &i: ir->strings) {
        const char *s = i->c_str();
        char* p = const_cast<char *>(strstr(s, "Lde/robv/android/xposed"));
        if (p) {
            memcpy(p, new_sig, strlen(new_sig));
        }
    }
    dex::Writer writer(ir);

    size_t new_size;
    WA allocator;
    auto *p_dex = writer.CreateImage(&allocator, &new_size);
    ustring new_dex(p_dex, new_size);
    allocator.Free(p_dex);
    return new_dex;
}
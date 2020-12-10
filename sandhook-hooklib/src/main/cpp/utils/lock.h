//
// Created by SwiftGan on 2019/1/21.
//

#ifndef SANDHOOK_LOCK_H
#define SANDHOOK_LOCK_H

#include "mutex"
#include "../includes/hide_api.h"

namespace SandHook {

    class AutoLock {
    public:
        inline AutoLock(std::mutex& mutex) : mLock(mutex)  { mLock.lock(); }
        inline AutoLock(std::mutex* mutex) : mLock(*mutex) { mLock.lock(); }
        inline ~AutoLock() { mLock.unlock(); }
    private:
        std::mutex& mLock;
    };

    class StopTheWorld {
    public:
        inline StopTheWorld()  { suspendVM(); }
        inline ~StopTheWorld() { resumeVM(); }
    };

}

#endif //SANDHOOK_LOCK_H

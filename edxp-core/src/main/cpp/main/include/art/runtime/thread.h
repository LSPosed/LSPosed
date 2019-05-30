
#pragma once

#include <base/object.h>

namespace art {

class Thread : public edxp::HookedObject {

    public:
        Thread(void *thiz) : HookedObject(thiz) {}

    };
}

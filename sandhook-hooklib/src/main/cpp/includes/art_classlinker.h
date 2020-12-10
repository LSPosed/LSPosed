//
// Created by 双草酸酯 on 11/27/20.
//

#ifndef SANDHOOK_ART_CLASSLINKER_H
#define SANDHOOK_ART_CLASSLINKER_H

#endif //SANDHOOK_ART_CLASSLINKER_H
namespace art {
class ClassLinker {
public:
    void MakeInitializedClassesVisiblyInitialized(void* self, bool wait);
};
}
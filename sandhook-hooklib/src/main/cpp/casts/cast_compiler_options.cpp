//
// Created by 甘尧 on 2019/2/24.
//

#include "../includes/cast_compiler_options.h"
#include "../includes/hide_api.h"

extern int SDK_INT;

namespace SandHook {


    class CastInlineMaxCodeUnits : public IMember<art::CompilerOptions, size_t> {
    protected:
        Size calOffset(JNIEnv *jniEnv, art::CompilerOptions *p) override {
            if (SDK_INT < ANDROID_N)
                return getParentSize() + 1;
            if (SDK_INT >= ANDROID_O) {
                return BYTE_POINT + 5 * sizeof(size_t);
            } else {
                return BYTE_POINT + 6 * sizeof(size_t);
            }
        }
    };

    void CastCompilerOptions::init(JNIEnv *jniEnv) {
        inlineMaxCodeUnits->init(jniEnv, nullptr, sizeof(art::CompilerOptions));
    }

    IMember<art::CompilerOptions, size_t>* CastCompilerOptions::inlineMaxCodeUnits = new CastInlineMaxCodeUnits();

}

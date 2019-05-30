
#pragma once

#include <cstdint>

namespace edxp {

    jboolean XposedBridge_initXResourcesNative(JNIEnv *env, jclass);

    void XResources_rewriteXmlReferencesNative(JNIEnv *env, jclass,
                                               jlong parserPtr, jobject origRes, jobject repRes);
}

//
// Created by solo on 2019/3/24.
//

#include <jni.h>
#include <include/ByteOrder.h>
#include <include/logging.h>
#include <dlfcn.h>
#include <java_hook/java_hook.h>
#include "resource_hook.h"

#define CLASS_XRESOURCES     "android/content/res/XResources"

jclass classXResources;
jmethodID methodXResourcesTranslateAttrId;
jmethodID methodXResourcesTranslateResId;

int32_t (*ResXMLParser_next)(void *);

void (*ResXMLParser_restart)(void *);

int32_t (*ResXMLParser_getAttributeNameID)(void *, int);

char16_t *(*ResStringPool_stringAt)(const void *, int32_t, size_t *);

bool prepareSymbols() {
    void *fwHandle = dlopen(kLibFwPath, RTLD_LAZY | RTLD_GLOBAL);
    if (!fwHandle) {
        LOGE("can't open libandroidfw: %s", dlerror());
        return false;
    }
    ResXMLParser_next = reinterpret_cast<int32_t (*)(void *)>(dlsym(fwHandle,
                                                                    "_ZN7android12ResXMLParser4nextEv"));
    if (!ResXMLParser_next) {
        LOGE("can't get ResXMLParser_next: %s", dlerror());
        return false;
    }
    ResXMLParser_restart = reinterpret_cast<void (*)(void *)>(dlsym(fwHandle,
                                                                    "_ZN7android12ResXMLParser7restartEv"));
    if (!ResXMLParser_restart) {
        LOGE("can't get ResXMLParser_restart: %s", dlerror());
        return false;
    }
    ResXMLParser_getAttributeNameID = reinterpret_cast<int32_t (*)(void *, int)>(dlsym(fwHandle,
#if defined(__LP64__)
            "_ZNK7android12ResXMLParser18getAttributeNameIDEm"
#else
                                                                                       "_ZNK7android12ResXMLParser18getAttributeNameIDEj"
#endif
    ));
    if (!ResXMLParser_getAttributeNameID) {
        LOGE("can't get ResXMLParser_getAttributeNameID: %s", dlerror());
        return false;
    }
    ResStringPool_stringAt = reinterpret_cast<char16_t *(*)(const void *, int32_t, size_t *)>(dlsym(
            fwHandle,
#if defined(__LP64__)
            "_ZNK7android13ResStringPool8stringAtEmPm"
#else
            "_ZNK7android13ResStringPool8stringAtEjPj"
#endif
    ));
    if (!ResStringPool_stringAt) {
        LOGE("can't get ResStringPool_stringAt: %s", dlerror());
        return false;
    }
    return true;
}

int register_natives_XResources(JNIEnv *env, jclass clazz) {
    const JNINativeMethod methods[] = {
            {"rewriteXmlReferencesNative",
                    "(JLandroid/content/res/XResources;Landroid/content/res/Resources;)V",
                    (void *) XResources_rewriteXmlReferencesNative},
    };
    return env->RegisterNatives(clazz, methods, NELEM(methods));
}

jboolean XposedBridge_initXResourcesNative(JNIEnv *env, jclass) {
    classXResources = env->FindClass(CLASS_XRESOURCES);
    if (classXResources == NULL) {
        LOGE("Error while loading XResources class '%s':", CLASS_XRESOURCES);
        env->ExceptionClear();
        return false;
    }
    classXResources = reinterpret_cast<jclass>(env->NewGlobalRef(classXResources));

    if (register_natives_XResources(env, classXResources) != JNI_OK) {
        LOGE("Could not register natives for '%s'", CLASS_XRESOURCES);
        env->ExceptionClear();
        return false;
    }

    methodXResourcesTranslateResId = env->GetStaticMethodID(classXResources, "translateResId",
                                                            "(ILandroid/content/res/XResources;Landroid/content/res/Resources;)I");
    if (methodXResourcesTranslateResId == NULL) {
        LOGE("ERROR: could not find method %s.translateResId(int, XResources, Resources)",
             CLASS_XRESOURCES);
        env->ExceptionClear();
        return false;
    }

    methodXResourcesTranslateAttrId = env->GetStaticMethodID(classXResources, "translateAttrId",
                                                             "(Ljava/lang/String;Landroid/content/res/XResources;)I");
    if (methodXResourcesTranslateAttrId == NULL) {
        LOGE("ERROR: could not find method %s.findAttrId(String, XResources)", CLASS_XRESOURCES);
        env->ExceptionClear();
        return false;
    }

    return prepareSymbols();
}

void XResources_rewriteXmlReferencesNative(JNIEnv *env, jclass,
                                           jlong parserPtr, jobject origRes, jobject repRes) {

    ResXMLParser *parser = (ResXMLParser *) parserPtr;

    if (parser == nullptr)
        return;

    const ResXMLTree &mTree = parser->mTree;
    uint32_t *mResIds = (uint32_t *) mTree.mResIds;
    ResXMLTree_attrExt *tag;
    int attrCount;

    do {
        switch (ResXMLParser_next(parser)) {
            case ResXMLParser::START_TAG:
                tag = (ResXMLTree_attrExt *) parser->mCurExt;
                attrCount = dtohs(tag->attributeCount);
                for (int idx = 0; idx < attrCount; idx++) {
                    ResXMLTree_attribute *attr = (ResXMLTree_attribute *)
                            (((const uint8_t *) tag)
                             + dtohs(tag->attributeStart)
                             + (dtohs(tag->attributeSize) * idx));

                    // find resource IDs for attribute names
                    int32_t attrNameID = ResXMLParser_getAttributeNameID(parser, idx);
                    // only replace attribute name IDs for app packages
                    if (attrNameID >= 0 && (size_t) attrNameID < mTree.mNumResIds &&
                        dtohl(mResIds[attrNameID]) >= 0x7f000000) {
                        size_t attNameLen;
                        const char16_t *attrName = ResStringPool_stringAt(&(mTree.mStrings),
                                                                          attrNameID, &attNameLen);
                        jint attrResID = env->CallStaticIntMethod(classXResources,
                                                                  methodXResourcesTranslateAttrId,
                                                                  env->NewString(
                                                                          (const jchar *) attrName,
                                                                          attNameLen), origRes);
                        if (env->ExceptionCheck())
                            goto leave;

                        mResIds[attrNameID] = htodl(attrResID);
                    }

                    // find original resource IDs for reference values (app packages only)
                    if (attr->typedValue.dataType != Res_value::TYPE_REFERENCE)
                        continue;

                    jint oldValue = dtohl(attr->typedValue.data);
                    if (oldValue < 0x7f000000)
                        continue;

                    jint newValue = env->CallStaticIntMethod(classXResources,
                                                             methodXResourcesTranslateResId,
                                                             oldValue, origRes, repRes);
                    if (env->ExceptionCheck())
                        goto leave;

                    if (newValue != oldValue)
                        attr->typedValue.data = htodl(newValue);
                }
                continue;
            case ResXMLParser::END_DOCUMENT:
            case ResXMLParser::BAD_DOCUMENT:
                goto leave;
            default:
                continue;
        }
    } while (true);

    leave:
    ResXMLParser_restart(parser);
}
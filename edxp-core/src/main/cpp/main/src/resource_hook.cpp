//
// Created by solo on 2019/3/24.
//

#include <jni.h>
#include <ByteOrder.h>
#include <logging.h>
#include <dlfcn.h>
#include <android-base/macros.h>
#include <edxp_context.h>
#include <art/runtime/native/native_util.h>
#include <nativehelper/jni_macros.h>
#include <JNIHelper.h>
#include "framework/androidfw/ResourceTypes.h"
#include "resource_hook.h"
#include "dl_util.h"
#include "config.h"

// @ApiSensitive(Level.HIGH)
namespace edxp {

    static constexpr const char *kXResourcesClassName = "android/content/res/XResources";

    typedef int32_t (*TYPE_GET_ATTR_NAME_ID)(void *, int);

    typedef char16_t *(*TYPE_STRING_AT)(const void *, int32_t, size_t *);

    typedef void(*TYPE_RESTART)(void *);

    typedef int32_t (*TYPE_NEXT)(void *);

    static jclass classXResources;
    static jmethodID methodXResourcesTranslateAttrId;
    static jmethodID methodXResourcesTranslateResId;

    static TYPE_NEXT ResXMLParser_next = nullptr;
    static TYPE_RESTART ResXMLParser_restart = nullptr;
    static TYPE_GET_ATTR_NAME_ID ResXMLParser_getAttributeNameID = nullptr;
    static TYPE_STRING_AT ResStringPool_stringAt = nullptr;

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(XResources, rewriteXmlReferencesNative,
                          "(JLandroid/content/res/XResources;Landroid/content/res/Resources;)V")
    };

    static bool register_natives_XResources(JNIEnv *env, jclass clazz) {
        jint result = JNI_RegisterNatives(env, clazz, gMethods, arraysize(gMethods));
        return result == JNI_OK;
    }

    static bool PrepareSymbols() {
        ScopedDlHandle fw_handle(kLibFwPath.c_str());
        if (!fw_handle.IsValid()) {
            return false;
        };
        if (!(ResXMLParser_next = fw_handle.DlSym<TYPE_NEXT>(
                "_ZN7android12ResXMLParser4nextEv"))) {
            return false;
        }
        if (!(ResXMLParser_restart = fw_handle.DlSym<TYPE_RESTART>(
                "_ZN7android12ResXMLParser7restartEv"))) {
            return false;
        };
        if (!(ResXMLParser_getAttributeNameID = fw_handle.DlSym<TYPE_GET_ATTR_NAME_ID>(
                LP_SELECT("_ZNK7android12ResXMLParser18getAttributeNameIDEj",
                          "_ZNK7android12ResXMLParser18getAttributeNameIDEm")))) {
            return false;
        }
        return (ResStringPool_stringAt = fw_handle.DlSym<TYPE_STRING_AT>(
                LP_SELECT("_ZNK7android13ResStringPool8stringAtEjPj",
                          "_ZNK7android13ResStringPool8stringAtEmPm"))) != nullptr;
    }

    jboolean XposedBridge_initXResourcesNative(JNIEnv *env, jclass) {
        classXResources = Context::GetInstance()->FindClassFromLoader(env, kXResourcesClassName);
        if (!classXResources) {
            LOGE("Error while loading XResources class '%s':", kXResourcesClassName);
            return JNI_FALSE;
        }
        if (!register_natives_XResources(env, classXResources)) {
            return JNI_FALSE;
        }
        methodXResourcesTranslateResId = JNI_GetStaticMethodID(
                env, classXResources, "translateResId",
                "(ILandroid/content/res/XResources;Landroid/content/res/Resources;)I");
        if (!methodXResourcesTranslateResId) {
            return JNI_FALSE;
        }
        methodXResourcesTranslateAttrId = JNI_GetStaticMethodID(
                env, classXResources, "translateAttrId",
                "(Ljava/lang/String;Landroid/content/res/XResources;)I");
        if (!methodXResourcesTranslateAttrId) {
            return JNI_FALSE;
        }
        if (!PrepareSymbols()) {
            return JNI_FALSE;
        }
        classXResources = reinterpret_cast<jclass>(env->NewGlobalRef(classXResources));
        return JNI_TRUE;
    }

    void XResources_rewriteXmlReferencesNative(JNIEnv *env, jclass,
                                               jlong parserPtr, jobject origRes, jobject repRes) {

        auto parser = (android::ResXMLParser *) parserPtr;

        if (parser == nullptr)
            return;

        const android::ResXMLTree &mTree = parser->mTree;
        auto mResIds = (uint32_t *) mTree.mResIds;
        android::ResXMLTree_attrExt *tag;
        int attrCount;

        do {
            switch (ResXMLParser_next(parser)) {
                case android::ResXMLParser::START_TAG:
                    tag = (android::ResXMLTree_attrExt *) parser->mCurExt;
                    attrCount = dtohs(tag->attributeCount);
                    for (int idx = 0; idx < attrCount; idx++) {
                        auto attr = (android::ResXMLTree_attribute *)
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
                                                                              attrNameID,
                                                                              &attNameLen);
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
                        if (attr->typedValue.dataType != android::Res_value::TYPE_REFERENCE)
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
                case android::ResXMLParser::END_DOCUMENT:
                case android::ResXMLParser::BAD_DOCUMENT:
                    goto leave;
                default:
                    continue;
            }
        } while (true);

        leave:
        ResXMLParser_restart(parser);
    }

}
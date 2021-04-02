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
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#include <jni.h>
#include <dex_builder.h>
#include <art/runtime/thread.h>
#include <art/runtime/mirror/class.h>
#include <dl_util.h>
#include <framework/androidfw/resource_types.h>
#include <byte_order.h>
#include "native_util.h"
#include "resources_hook.h"

namespace lspd {
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
        return android::ResStringPool::setup(fw_handle.Get());
    }

    LSP_DEF_NATIVE_METHOD(jboolean, ResourcesHook, initXResourcesNative) {
        if (auto classXResources_ = Context::GetInstance()->FindClassFromCurrentLoader(env,
                                                                                       kXResourcesClassName)) {
            classXResources = JNI_NewGlobalRef(env, classXResources_);
        } else {
            LOGE("Error while loading XResources class '%s':", kXResourcesClassName);
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
        return JNI_TRUE;
    }

    // @ApiSensitive(Level.MIDDLE)
    LSP_DEF_NATIVE_METHOD(jboolean, ResourcesHook, removeFinalFlagNative, jclass target_class) {
        if (target_class) {
            auto class_clazz = JNI_FindClass(env, "java/lang/Class");
            jfieldID java_lang_Class_accessFlags = JNI_GetFieldID(
                    env, class_clazz, "accessFlags", "I");
            jint access_flags = env->GetIntField(target_class, java_lang_Class_accessFlags);
            env->SetIntField(target_class, java_lang_Class_accessFlags, access_flags & ~kAccFinal);
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }

    LSP_DEF_NATIVE_METHOD(jobject, ResourcesHook, buildDummyClassLoader, jobject parent,
                          jobject resource_super_class, jobject typed_array_super_class) {
        using namespace startop::dex;
        static auto in_memory_classloader = JNI_NewGlobalRef(env, JNI_FindClass(env,
                                                                                "dalvik/system/InMemoryDexClassLoader"));
        static jmethodID initMid = JNI_GetMethodID(env, in_memory_classloader, "<init>",
                                                   "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        DexBuilder dex_file;

        std::string storage;
        auto current_thread = art::Thread::Current();
        ClassBuilder xresource_builder{
                dex_file.MakeClass("xposed.dummy.XResourcesSuperClass")};
        xresource_builder.setSuperClass(TypeDescriptor::FromDescriptor(art::mirror::Class(
                current_thread.DecodeJObject(resource_super_class)).GetDescriptor(&storage)));

        ClassBuilder xtypearray_builder{
                dex_file.MakeClass("xposed.dummy.XTypedArraySuperClass")};
        xtypearray_builder.setSuperClass(TypeDescriptor::FromDescriptor(art::mirror::Class(
                current_thread.DecodeJObject(typed_array_super_class)).GetDescriptor(&storage)));

        slicer::MemView image{dex_file.CreateImage()};

        auto dex_buffer = env->NewDirectByteBuffer(const_cast<void *>(image.ptr()), image.size());
        return JNI_NewObject(env, in_memory_classloader, initMid,
                             dex_buffer, parent).release();
    }

    LSP_DEF_NATIVE_METHOD(void, ResourcesHook, rewriteXmlReferencesNative,
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
                            auto attrName = mTree.mStrings.stringAt(attrNameID);
                            jint attrResID = env->CallStaticIntMethod(classXResources,
                                                                      methodXResourcesTranslateAttrId,
                                                                      env->NewString(
                                                                              (const jchar *) attrName.data_,
                                                                              attrName.length_),
                                                                      origRes);
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

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(ResourcesHook, initXResourcesNative, "()Z"),
            LSP_NATIVE_METHOD(ResourcesHook, removeFinalFlagNative, "(Ljava/lang/Class;)Z"),
            LSP_NATIVE_METHOD(ResourcesHook, buildDummyClassLoader,
                              "(Ljava/lang/ClassLoader;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/ClassLoader;"),
            LSP_NATIVE_METHOD(ResourcesHook, rewriteXmlReferencesNative,
                              "(JLandroid/content/res/XResources;Landroid/content/res/Resources;)V")
    };

    void RegisterResourcesHook(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(ResourcesHook);
    }
}

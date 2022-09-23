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
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

#include <jni.h>
#include "dex_builder.h"
#include "framework/androidfw/resource_types.h"
#include "elf_util.h"
#include "native_util.h"
#include "resources_hook.h"
#include "config_bridge.h"

using namespace lsplant;

namespace lspd {
    using TYPE_GET_ATTR_NAME_ID = int32_t (*)(void *, int);

    using TYPE_STRING_AT = char16_t *(*)(const void *, int32_t, size_t *);

    using TYPE_RESTART = void (*)(void *);

    using TYPE_NEXT = int32_t (*)(void *);

    static jclass classXResources;
    static jmethodID methodXResourcesTranslateAttrId;
    static jmethodID methodXResourcesTranslateResId;

    static TYPE_NEXT ResXMLParser_next = nullptr;
    static TYPE_RESTART ResXMLParser_restart = nullptr;
    static TYPE_GET_ATTR_NAME_ID ResXMLParser_getAttributeNameID = nullptr;

    static std::string GetXResourcesClassName() {
        auto &obfs_map = ConfigBridge::GetInstance()->obfuscation_map();
        if (obfs_map.empty()) {
            LOGW("GetXResourcesClassName: obfuscation_map empty?????");
        }
        static auto name = lspd::JavaNameToSignature(
                obfs_map.at("android.content.res.XRes"))  // TODO: kill this hardcoded name
                    .substr(1) + "ources";
        LOGD("{}", name.c_str());
        return name;
    }

    static bool PrepareSymbols() {
        SandHook::ElfImg fw(kLibFwName);
        if (!fw.isValid()) {
            return false;
        };
        if (!(ResXMLParser_next = fw.getSymbAddress<TYPE_NEXT>(
                "_ZN7android12ResXMLParser4nextEv"))) {
            return false;
        }
        if (!(ResXMLParser_restart = fw.getSymbAddress<TYPE_RESTART>(
                "_ZN7android12ResXMLParser7restartEv"))) {
            return false;
        };
        if (!(ResXMLParser_getAttributeNameID = fw.getSymbAddress<TYPE_GET_ATTR_NAME_ID>(
                LP_SELECT("_ZNK7android12ResXMLParser18getAttributeNameIDEj",
                          "_ZNK7android12ResXMLParser18getAttributeNameIDEm")))) {
            return false;
        }
        return android::ResStringPool::setup(HookHandler{
            .art_symbol_resolver = [&](auto s) {
                return fw.template getSymbAddress(s);
            }
        });
    }

    LSP_DEF_NATIVE_METHOD(jboolean, ResourcesHook, initXResourcesNative) {
        const auto x_resources_class_name = GetXResourcesClassName();
        if (auto classXResources_ = Context::GetInstance()->FindClassFromCurrentLoader(env,
                                                                                       x_resources_class_name)) {
            classXResources = JNI_NewGlobalRef(env, classXResources_);
        } else {
            LOGE("Error while loading XResources class '{}':", x_resources_class_name);
            return JNI_FALSE;
        }
        methodXResourcesTranslateResId = JNI_GetStaticMethodID(
                env, classXResources, "translateResId",
                fmt::format("(IL{};Landroid/content/res/Resources;)I", x_resources_class_name));
        if (!methodXResourcesTranslateResId) {
            return JNI_FALSE;
        }
        methodXResourcesTranslateAttrId = JNI_GetStaticMethodID(
                env, classXResources, "translateAttrId",
                fmt::format("(Ljava/lang/String;L{};)I", x_resources_class_name));
        if (!methodXResourcesTranslateAttrId) {
            return JNI_FALSE;
        }
        if (!PrepareSymbols()) {
            return JNI_FALSE;
        }
        return JNI_TRUE;
    }

    // @ApiSensitive(Level.MIDDLE)
    LSP_DEF_NATIVE_METHOD(jboolean, ResourcesHook, makeInheritable, jclass target_class) {
        if (lsplant::MakeClassInheritable(env, target_class)) {
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }

    LSP_DEF_NATIVE_METHOD(jobject, ResourcesHook, buildDummyClassLoader, jobject parent,
                          jstring resource_super_class, jstring typed_array_super_class) {
        using namespace startop::dex;
        static auto in_memory_classloader = JNI_NewGlobalRef(env, JNI_FindClass(env,
                                                                                "dalvik/system/InMemoryDexClassLoader"));
        static jmethodID initMid = JNI_GetMethodID(env, in_memory_classloader, "<init>",
                                                   "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        DexBuilder dex_file;

        ClassBuilder xresource_builder{
                dex_file.MakeClass("xposed.dummy.XResourcesSuperClass")};
        xresource_builder.setSuperClass(TypeDescriptor::FromClassname(JUTFString(env, resource_super_class).get()));

        ClassBuilder xtypearray_builder{
                dex_file.MakeClass("xposed.dummy.XTypedArraySuperClass")};
        xtypearray_builder.setSuperClass(TypeDescriptor::FromClassname(JUTFString(env, typed_array_super_class).get()));

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
                    attrCount = tag->attributeCount;
                    for (int idx = 0; idx < attrCount; idx++) {
                        auto attr = (android::ResXMLTree_attribute *)
                                (((const uint8_t *) tag)
                                 + tag->attributeStart
                                 + tag->attributeSize * idx);

                        // find resource IDs for attribute names
                        int32_t attrNameID = ResXMLParser_getAttributeNameID(parser, idx);
                        // only replace attribute name IDs for app packages
                        if (attrNameID >= 0 && (size_t) attrNameID < mTree.mNumResIds &&
                            mResIds[attrNameID] >= 0x7f000000) {
                            auto attrName = mTree.mStrings.stringAt(attrNameID);
                            jint attrResID = env->CallStaticIntMethod(classXResources,
                                                                      methodXResourcesTranslateAttrId,
                                                                      env->NewString(
                                                                              (const jchar *) attrName.data_,
                                                                              attrName.length_),
                                                                      origRes);
                            if (env->ExceptionCheck())
                                goto leave;

                            mResIds[attrNameID] = attrResID;
                        }

                        // find original resource IDs for reference values (app packages only)
                        if (attr->typedValue.dataType != android::Res_value::TYPE_REFERENCE)
                            continue;

                        jint oldValue = attr->typedValue.data;
                        if (oldValue < 0x7f000000)
                            continue;

                        jint newValue = env->CallStaticIntMethod(classXResources,
                                                                 methodXResourcesTranslateResId,
                                                                 oldValue, origRes, repRes);
                        if (env->ExceptionCheck())
                            goto leave;

                        if (newValue != oldValue)
                            attr->typedValue.data = newValue;
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
            LSP_NATIVE_METHOD(ResourcesHook, makeInheritable,"(Ljava/lang/Class;)Z"),
            LSP_NATIVE_METHOD(ResourcesHook, buildDummyClassLoader,
                              "(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/ClassLoader;"),
            LSP_NATIVE_METHOD(ResourcesHook, rewriteXmlReferencesNative,
                              "(JLandroid/content/res/XResources;Landroid/content/res/Resources;)V")
    };

    void RegisterResourcesHook(JNIEnv *env) {
        auto sign = fmt::format("(JL{};Landroid/content/res/Resources;)V", GetXResourcesClassName());
        gMethods[3].signature = sign.c_str();

        REGISTER_LSP_NATIVE_METHODS(ResourcesHook);
    }
}

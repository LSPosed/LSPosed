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
 * Copyright (C) 2023 LSPosed Contributors
 */

#include "dex_parser.h"
#include "native_util.h"
#include "slicer/reader.h"

namespace {
    jobject ParseAnnotation(JNIEnv *env, dex::Reader &dex, jclass object_class,
                            const dex::AnnotationSetItem *annotation) {
        if (annotation == nullptr) {
            return nullptr;
        }
        auto a = env->NewIntArray(static_cast<jint>(2 * annotation->size));
        auto *a_ptr = env->GetIntArrayElements(a, nullptr);
        auto b = env->NewObjectArray(static_cast<jint>(annotation->size), object_class, nullptr);
        for (size_t i = 0; i < annotation->size; ++i) {
            auto *item = dex.dataPtr<dex::AnnotationItem>(annotation->entries[i]);
            a_ptr[2 * i] = item->visibility;
            auto *annotation_data = item->annotation;
            a_ptr[2 * i + 1] = static_cast<jint>(dex::ReadULeb128(&annotation_data));
            auto size = dex::ReadULeb128(&annotation_data);
            auto bi = env->NewObjectArray(static_cast<jint>(size), object_class, nullptr);
            for (size_t j = 0; j < size; ++j) {
                auto name = dex::ReadULeb128(&annotation_data);
                auto arg_and_type = *annotation_data++;
                auto value_type = arg_and_type & 0x1f;
                auto value_arg = arg_and_type >> 5;
                jobject value = nullptr;
                switch (value_type) {
                    case 0x00: // byte
                    case 0x1f: // boolean
                        value = env->NewDirectByteBuffer(
                                reinterpret_cast<void *>(const_cast<dex::u1 *>(annotation_data)),
                                1);
                        break;
                    case 0x02: // short
                    case 0x03: // char
                    case 0x04: // int
                    case 0x06: // long
                    case 0x10: // float
                    case 0x11: // double
                    case 0x17: // string
                    case 0x18: // type
                    case 0x19: // field
                    case 0x1a: // method
                    case 0x1b: // enum
                        value = env->NewDirectByteBuffer(
                                reinterpret_cast<void *>(const_cast<dex::u1 *>(annotation_data)),
                                value_arg);
                        break;
                    case 0x1c: // array
                    case 0x1d: // annotation
                    case 0x1e: // null
                        // not supported
                        value = nullptr;
                        break;
                    default:
                        break;
                }
                env->SetObjectArrayElement(bi, static_cast<jint>(j), value);
                env->DeleteLocalRef(value);
            }
            env->SetObjectArrayElement(b, static_cast<jint>(i), bi);
            env->DeleteLocalRef(bi);
        }
        env->ReleaseIntArrayElements(a, a_ptr, 0);
        auto res = env->NewObjectArray(2, object_class, nullptr);
        env->SetObjectArrayElement(res, 0, a);
        env->SetObjectArrayElement(res, 1, b);
        env->DeleteLocalRef(a);
        env->DeleteLocalRef(b);
        return res;
    }
}
namespace lspd {
    LSP_DEF_NATIVE_METHOD(jobject, DexParserBridge, parseDex, jobject data) {
        auto dex_size = env->GetDirectBufferCapacity(data);
        if (dex_size == -1) {
            env->ThrowNew(env->FindClass("java/io/IOException"), "Invalid dex data");
            return nullptr;
        }
        auto *dex_data = env->GetDirectBufferAddress(data);

        dex::Reader dex(reinterpret_cast<dex::u1 *>(dex_data), dex_size, nullptr, 0);
        if (dex.IsCompact()) {
            env->ThrowNew(env->FindClass("java/io/IOException"), "Compact dex is not supported");
            return nullptr;
        }
        auto object_class = env->FindClass("java/lang/Object");
        auto string_class = env->FindClass("java/lang/String");
        auto int_array_class = env->FindClass("[I");
        auto out = env->NewObjectArray(7, object_class, nullptr);
        auto out0 = env->NewObjectArray(static_cast<jint>(dex.StringIds().size()),
                                        string_class, nullptr);
        auto strings = dex.StringIds();
        for (size_t i = 0; i < strings.size(); ++i) {
            const auto *ptr = dex.dataPtr<dex::u1>(strings[i].string_data_off);
            size_t len = dex::ReadULeb128(&ptr);
            auto str = env->NewStringUTF(reinterpret_cast<const char *>(ptr));
            env->SetObjectArrayElement(out0, static_cast<jint>(i), str);
            env->DeleteLocalRef(str);
        }
        env->SetObjectArrayElement(out, 0, out0);
        env->DeleteLocalRef(out0);

        auto types = dex.TypeIds();
        auto out1 = env->NewIntArray(static_cast<jint>(types.size()));
        auto *out1_ptr = env->GetIntArrayElements(out1, nullptr);
        for (size_t i = 0; i < types.size(); ++i) {
            out1_ptr[i] = static_cast<jint>(types[i].descriptor_idx);
        }
        env->ReleaseIntArrayElements(out1, out1_ptr, 0);
        env->SetObjectArrayElement(out, 1, out1);
        env->DeleteLocalRef(out1);

        auto protos = dex.ProtoIds();
        auto out2 = env->NewObjectArray(static_cast<jint>(protos.size()),
                                        int_array_class, nullptr);
        auto empty_type_list = dex::TypeList{.size = 0, .list = {}};
        for (size_t i = 0; i < protos.size(); ++i) {
            auto &proto = protos[i];
            const auto &params = proto.parameters_off ? *dex.dataPtr<dex::TypeList>(
                    proto.parameters_off) : empty_type_list;

            auto out2i = env->NewIntArray(static_cast<jint>(2 + params.size));
            auto *out2i_ptr = env->GetIntArrayElements(out2i, nullptr);
            out2i_ptr[0] = static_cast<jint>(proto.shorty_idx);
            out2i_ptr[1] = static_cast<jint>(proto.return_type_idx);
            for (size_t j = 0; j < params.size; ++j) {
                out2i_ptr[2 + j] = static_cast<jint>(params.list[j].type_idx);
            }
            env->ReleaseIntArrayElements(out2i, out2i_ptr, 0);
            env->SetObjectArrayElement(out2, static_cast<jint>(i), out2i);
            env->DeleteLocalRef(out2i);
        }
        env->SetObjectArrayElement(out, 2, out2);
        env->DeleteLocalRef(out2);

        auto fields = dex.FieldIds();
        auto out3 = env->NewIntArray(static_cast<jint>(3 * fields.size()));
        auto *out3_ptr = env->GetIntArrayElements(out3, nullptr);
        for (size_t i = 0; i < fields.size(); ++i) {
            auto &field = fields[i];
            out3_ptr[3 * i] = static_cast<jint>(field.class_idx);
            out3_ptr[3 * i + 1] = static_cast<jint>(field.type_idx);
            out3_ptr[3 * i + 2] = static_cast<jint>(field.name_idx);
        }
        env->ReleaseIntArrayElements(out3, out3_ptr, 0);
        env->SetObjectArrayElement(out, 3, out3);
        env->DeleteLocalRef(out3);

        auto methods = dex.MethodIds();
        auto out4 = env->NewIntArray(static_cast<jint>(3 * methods.size()));
        auto *out4_ptr = env->GetIntArrayElements(out4, nullptr);
        for (size_t i = 0; i < methods.size(); ++i) {
            out4_ptr[3 * i] = static_cast<jint>(methods[i].class_idx);
            out4_ptr[3 * i + 1] = static_cast<jint>(methods[i].proto_idx);
            out4_ptr[3 * i + 2] = static_cast<jint>(methods[i].name_idx);
        }
        env->ReleaseIntArrayElements(out4, out4_ptr, 0);
        env->SetObjectArrayElement(out, 4, out4);
        env->DeleteLocalRef(out4);

        auto classes = dex.ClassDefs();
        auto out5 = env->NewObjectArray(static_cast<jint>(classes.size()),
                                        int_array_class, nullptr);
        auto out6 = env->NewObjectArray(static_cast<jint>(4 * classes.size()),
                                        object_class, nullptr);
        for (size_t i = 0; i < classes.size(); ++i) {
            auto &class_def = classes[i];
            auto &interfaces = class_def.interfaces_off ? *dex.dataPtr<dex::TypeList>(
                    class_def.interfaces_off) : empty_type_list;

            dex::u4 static_fields_count = 0;
            dex::u4 instance_fields_count = 0;
            dex::u4 direct_methods_count = 0;
            dex::u4 virtual_methods_count = 0;
            dex::u4 field_annotations_count = 0;
            dex::u4 method_annotations_count = 0;
            dex::u4 parameter_annotations_count = 0;
            const dex::u1 *class_data = nullptr;
            const dex::AnnotationsDirectoryItem *annotations = nullptr;
            const dex::AnnotationSetItem *class_annotation = nullptr;
            if (class_def.class_data_off != 0) {
                class_data = dex.dataPtr<dex::u1>(class_def.class_data_off);
                static_fields_count = dex::ReadULeb128(&class_data);
                instance_fields_count = dex::ReadULeb128(&class_data);
                direct_methods_count = dex::ReadULeb128(&class_data);
                virtual_methods_count = dex::ReadULeb128(&class_data);
            }

            if (class_def.annotations_off != 0) {
                annotations = dex.dataPtr<dex::AnnotationsDirectoryItem>(class_def.annotations_off);
                if (annotations->class_annotations_off != 0) {
                    class_annotation = dex.dataPtr<dex::AnnotationSetItem>(
                            annotations->class_annotations_off);
                }
                field_annotations_count = annotations->fields_size;
                method_annotations_count = annotations->methods_size;
                parameter_annotations_count = annotations->parameters_size;
            }

            auto array_size = 4 + 1 + interfaces.size + 1 + 2 * static_fields_count +
                              1 + 2 * instance_fields_count + 1 + 3 * direct_methods_count +
                              1 + 3 * virtual_methods_count + 1 +
                              1 + field_annotations_count + 1 + method_annotations_count +
                              1 + parameter_annotations_count;
            auto out5i = env->NewIntArray(static_cast<int>(array_size));
            auto *out5i_ptr = env->GetIntArrayElements(out5i, nullptr);
            size_t j = 0;
            out5i_ptr[j++] = static_cast<jint>(class_def.class_idx);
            out5i_ptr[j++] = static_cast<jint>(class_def.access_flags);
            out5i_ptr[j++] = static_cast<jint>(class_def.superclass_idx);
            out5i_ptr[j++] = static_cast<jint>(class_def.source_file_idx);
            out5i_ptr[j++] = static_cast<jint>(interfaces.size);
            for (size_t k = 0; k < interfaces.size; ++k) {
                out5i_ptr[j++] = static_cast<jint>(interfaces.list[k].type_idx);
            }
            out5i_ptr[j++] = static_cast<jint>(static_fields_count);
            for (size_t k = 0, field_idx = 0; k < static_fields_count; ++k) {
                out5i_ptr[j++] = static_cast<jint>(field_idx += dex::ReadULeb128(&class_data));
                out5i_ptr[j++] = static_cast<jint>(dex::ReadULeb128(&class_data));
            }
            out5i_ptr[j++] = static_cast<jint>(instance_fields_count);
            for (size_t k = 0, field_idx = 0; k < instance_fields_count; ++k) {
                out5i_ptr[j++] = static_cast<jint>(field_idx += dex::ReadULeb128(&class_data));
                out5i_ptr[j++] = static_cast<jint>(dex::ReadULeb128(&class_data));
            }
            out5i_ptr[j++] = static_cast<jint>(direct_methods_count);
            for (size_t k = 0, method_idx = 0; k < direct_methods_count; ++k) {
                out5i_ptr[j++] = static_cast<jint>(method_idx += dex::ReadULeb128(&class_data));
                out5i_ptr[j++] = static_cast<jint>(dex::ReadULeb128(&class_data));
                out5i_ptr[j++] = static_cast<jint>(dex::ReadULeb128(&class_data));
            }
            out5i_ptr[j++] = static_cast<jint>(virtual_methods_count);
            for (size_t k = 0, method_idx = 0; k < virtual_methods_count; ++k) {
                out5i_ptr[j++] = static_cast<jint>(method_idx += dex::ReadULeb128(&class_data));
                out5i_ptr[j++] = static_cast<jint>(dex::ReadULeb128(&class_data));
                out5i_ptr[j++] = static_cast<jint>(dex::ReadULeb128(&class_data));
            }

            auto out6i0 = ParseAnnotation(env, dex, object_class, class_annotation);
            env->SetObjectArrayElement(out6, static_cast<jint>(4 * i), out6i0);
            env->DeleteLocalRef(out6i0);

            out5i_ptr[j++] = static_cast<jint>(class_annotation ? class_annotation->size : 0);

            out5i_ptr[j++] = static_cast<jint>(field_annotations_count);
            auto *field_annotations = annotations
                                      ? reinterpret_cast<const dex::FieldAnnotationsItem *>(
                                              annotations + 1) : nullptr;
            auto out6i1 = env->NewObjectArray(static_cast<jint>(field_annotations_count),
                                              object_class, nullptr);
            for (size_t k = 0; k < field_annotations_count; ++k) {
                out5i_ptr[j++] = static_cast<jint>(field_annotations[k].field_idx);
                auto *field_annotation = dex.dataPtr<dex::AnnotationSetItem>(
                        field_annotations[k].annotations_off);
                auto out6i1i = ParseAnnotation(env, dex, object_class, field_annotation);
                env->SetObjectArrayElement(out6i1, static_cast<jint>(k), out6i1i);
                env->DeleteLocalRef(out6i1i);
            }
            env->SetObjectArrayElement(out6, static_cast<jint>(4 * i + 1), out6i1);
            env->DeleteLocalRef(out6i1);

            out5i_ptr[j++] = static_cast<jint>(method_annotations_count);
            auto *method_annotations = field_annotations
                                       ? reinterpret_cast<const dex::MethodAnnotationsItem *>(
                                               field_annotations + field_annotations_count)
                                       : nullptr;
            auto out6i2 = env->NewObjectArray(static_cast<jint>(method_annotations_count),
                                              object_class, nullptr);
            for (size_t k = 0; k < method_annotations_count; ++k) {
                out5i_ptr[j++] = static_cast<jint>(method_annotations[k].method_idx);
                auto *method_annotation = dex.dataPtr<dex::AnnotationSetItem>(
                        method_annotations[k].annotations_off);
                auto out6i2i = ParseAnnotation(env, dex, object_class, method_annotation);
                env->SetObjectArrayElement(out6i2, static_cast<jint>(k), out6i2i);
                env->DeleteLocalRef(out6i2i);
            }
            env->SetObjectArrayElement(out6, static_cast<jint>(4 * i + 2), out6i2);

            out5i_ptr[j++] = static_cast<jint>(parameter_annotations_count);
            auto *parameter_annotations = method_annotations
                                          ? reinterpret_cast<const dex::ParameterAnnotationsItem *>(
                                                  method_annotations + method_annotations_count)
                                          : nullptr;
            auto out6i3 = env->NewObjectArray(static_cast<jint>(parameter_annotations_count),
                                              object_class, nullptr);
            for (size_t k = 0; k < parameter_annotations_count; ++k) {
                out5i_ptr[j++] = static_cast<jint>(parameter_annotations[k].method_idx);
                auto *parameter_annotation = dex.dataPtr<dex::AnnotationSetRefList>(
                        parameter_annotations[k].annotations_off);
                auto out6i3i = env->NewObjectArray(
                        static_cast<jint>(parameter_annotation->size), object_class, nullptr);
                for (size_t l = 0; l < parameter_annotation->size; ++l) {
                    auto *parameter_annotation_item = dex.dataPtr<dex::AnnotationSetItem>(
                            parameter_annotation->list[l].annotations_off);
                    auto out6i3ii = ParseAnnotation(env, dex, object_class,
                                                    parameter_annotation_item);
                    env->SetObjectArrayElement(out6i3i, static_cast<jint>(l), out6i3ii);
                    env->DeleteLocalRef(out6i3ii);
                }
                env->SetObjectArrayElement(out6i3, static_cast<jint>(k), out6i3i);
                env->DeleteLocalRef(out6i3i);
            }
            env->SetObjectArrayElement(out6, static_cast<jint>(4 * i + 3), out6i3);
            env->DeleteLocalRef(out6i3);

            env->ReleaseIntArrayElements(out5i, out5i_ptr, 0);
            env->SetObjectArrayElement(out5, static_cast<jint>(i), out5i);
            env->DeleteLocalRef(out5i);
        }

        return out;
    }

    LSP_DEF_NATIVE_METHOD(jobject, DexParserBridge, parseMethod, jobject data, jint code_offset) {
        auto dex_size = env->GetDirectBufferCapacity(data);
        auto *dex_data = env->GetDirectBufferAddress(data);
        if (dex_size < 0 || dex_data == nullptr || code_offset >= dex_size) {
            return nullptr;
        }
        auto *code = reinterpret_cast<dex::Code *>(reinterpret_cast<dex::u1 *>(dex_data) +
                                                   code_offset);

        static constexpr dex::u1 kOpcodeMask = 0xff;
        static constexpr dex::u1 kOpcodeNoOp = 0x00;
        static constexpr dex::u1 kOpcodeConstString = 0x1a;
        static constexpr dex::u1 kOpcodeConstStringJumbo = 0x1b;
        static constexpr dex::u1 kOpcodeIGetStart = 0x52;
        static constexpr dex::u1 kOpcodeIGetEnd = 0x58;
        static constexpr dex::u1 kOpcodeSGetStart = 0x60;
        static constexpr dex::u1 kOpcodeSGetEnd = 0x66;
        static constexpr dex::u1 kOpcodeIPutStart = 0x59;
        static constexpr dex::u1 kOpcodeIPutEnd = 0x5f;
        static constexpr dex::u1 kOpcodeSPutStart = 0x67;
        static constexpr dex::u1 kOpcodeSPutEnd = 0x6d;
        static constexpr dex::u1 kOpcodeInvokeStart = 0x6e;
        static constexpr dex::u1 kOpcodeInvokeEnd = 0x72;
        static constexpr dex::u1 kOpcodeInvokeRangeStart = 0x74;
        static constexpr dex::u1 kOpcodeInvokeRangeEnd = 0x78;
        static constexpr dex::u2 kInstPackedSwitchPlayLoad = 0x0100;
        static constexpr dex::u2 kInstSparseSwitchPlayLoad = 0x0200;
        static constexpr dex::u2 kInstFillArrayDataPlayLoad = 0x0300;
        const dex::u2 *inst = code->insns;
        const dex::u2 *end = inst + code->insns_size;
        std::vector<jint> invoked_methods;
        std::vector<jint> referred_strings;
        std::vector<jint> accessed_fields;
        std::vector<jint> assigned_fields;
        std::vector<jbyte> opcodes;
        while (inst < end) {
            dex::u1 opcode = *inst & kOpcodeMask;
            opcodes.push_back(static_cast<jbyte>(opcode));
            if (opcode == kOpcodeConstString) {
                auto str_idx = inst[1];
                referred_strings.push_back(str_idx);
            }
            if (opcode == kOpcodeConstStringJumbo) {
                auto str_idx = *reinterpret_cast<const dex::u4 *>(&inst[1]);
                referred_strings.push_back(static_cast<jint>(str_idx));
            }
            if ((opcode >= kOpcodeIGetStart && opcode <= kOpcodeIGetEnd) ||
                (opcode >= kOpcodeSGetStart && opcode <= kOpcodeSGetEnd)) {
                auto field_idx = inst[1];
                accessed_fields.push_back(field_idx);
            }
            if ((opcode >= kOpcodeIPutStart && opcode <= kOpcodeIPutEnd) ||
                (opcode >= kOpcodeSPutStart && opcode <= kOpcodeSPutEnd)) {
                auto field_idx = inst[1];
                assigned_fields.push_back(field_idx);
            }
            if ((opcode >= kOpcodeInvokeStart && opcode <= kOpcodeInvokeEnd) ||
                (opcode >= kOpcodeInvokeRangeStart && opcode <= kOpcodeInvokeRangeEnd)) {
                auto callee = inst[1];
                invoked_methods.push_back(callee);
            }
            if (opcode == kOpcodeNoOp) {
                if (*inst == kInstPackedSwitchPlayLoad) {
                    inst += inst[1] * 2 + 3;
                } else if (*inst == kInstSparseSwitchPlayLoad) {
                    inst += inst[1] * 4 + 1;
                } else if (*inst == kInstFillArrayDataPlayLoad) {
                    inst += (*reinterpret_cast<const dex::u4 *>(&inst[2]) * inst[1] + 1) / 2 + 3;
                }
            }
            inst += dex::opcode_len[opcode];
        }
        auto res = env->NewObjectArray(5, env->FindClass("java/lang/Object"), nullptr);
        auto res0 = env->NewIntArray(static_cast<jint>(invoked_methods.size()));
        auto res0_ptr = env->GetIntArrayElements(res0, nullptr);
        memcpy(res0_ptr, invoked_methods.data(), invoked_methods.size() * sizeof(jint));
        env->ReleaseIntArrayElements(res0, res0_ptr, 0);
        env->SetObjectArrayElement(res, 0, res0);
        env->DeleteLocalRef(res0);
        auto res1 = env->NewIntArray(static_cast<jint>(accessed_fields.size()));
        auto res1_ptr = env->GetIntArrayElements(res1, nullptr);
        memcpy(res1_ptr, accessed_fields.data(), accessed_fields.size() * sizeof(jint));
        env->ReleaseIntArrayElements(res1, res1_ptr, 0);
        env->SetObjectArrayElement(res, 1, res1);
        env->DeleteLocalRef(res1);
        auto res2 = env->NewIntArray(static_cast<jint>(assigned_fields.size()));
        auto res2_ptr = env->GetIntArrayElements(res2, nullptr);
        memcpy(res2_ptr, assigned_fields.data(), assigned_fields.size() * sizeof(jint));
        env->ReleaseIntArrayElements(res2, res2_ptr, 0);
        env->SetObjectArrayElement(res, 2, res2);
        env->DeleteLocalRef(res2);
        auto res3 = env->NewIntArray(static_cast<jint>(referred_strings.size()));
        auto res3_ptr = env->GetIntArrayElements(res3, nullptr);
        memcpy(res3_ptr, referred_strings.data(), referred_strings.size() * sizeof(jint));
        env->ReleaseIntArrayElements(res3, res3_ptr, 0);
        env->SetObjectArrayElement(res, 3, res3);
        env->DeleteLocalRef(res3);
        auto res4 = env->NewByteArray(static_cast<jint>(opcodes.size()));
        auto res4_ptr = env->GetByteArrayElements(res4, nullptr);
        memcpy(res4_ptr, opcodes.data(), opcodes.size() * sizeof(jbyte));
        env->ReleaseByteArrayElements(res4, res4_ptr, 0);
        env->SetObjectArrayElement(res, 4, res4);
        env->DeleteLocalRef(res4);

        return res;
    }

    static JNINativeMethod gMethods[] = {
            LSP_NATIVE_METHOD(DexParserBridge, parseDex,
                              "(Ljava/nio/buffer/ByteBuffer;)Ljava/lang/Object;"),
            LSP_NATIVE_METHOD(DexParserBridge, parseDex,
                              "(Ljava/nio/buffer/ByteBuffer;I)Ljava/lang/Object;"),
    };


    void RegisterDexParserBridge(JNIEnv *env) {
        REGISTER_LSP_NATIVE_METHODS(DexParserBridge);
    }
}

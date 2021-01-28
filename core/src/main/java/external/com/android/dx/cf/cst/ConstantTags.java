/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package external.com.android.dx.cf.cst;

/**
 * Tags for constant pool constants.
 */
public interface ConstantTags {
    /** tag for a {@code CONSTANT_Utf8_info} */
    int CONSTANT_Utf8 = 1;

    /** tag for a {@code CONSTANT_Integer_info} */
    int CONSTANT_Integer = 3;

    /** tag for a {@code CONSTANT_Float_info} */
    int CONSTANT_Float = 4;

    /** tag for a {@code CONSTANT_Long_info} */
    int CONSTANT_Long = 5;

    /** tag for a {@code CONSTANT_Double_info} */
    int CONSTANT_Double = 6;

    /** tag for a {@code CONSTANT_Class_info} */
    int CONSTANT_Class = 7;

    /** tag for a {@code CONSTANT_String_info} */
    int CONSTANT_String = 8;

    /** tag for a {@code CONSTANT_Fieldref_info} */
    int CONSTANT_Fieldref = 9;

    /** tag for a {@code CONSTANT_Methodref_info} */
    int CONSTANT_Methodref = 10;

    /** tag for a {@code CONSTANT_InterfaceMethodref_info} */
    int CONSTANT_InterfaceMethodref = 11;

    /** tag for a {@code CONSTANT_NameAndType_info} */
    int CONSTANT_NameAndType = 12;

    /** tag for a {@code CONSTANT_MethodHandle} */
    int CONSTANT_MethodHandle = 15;

    /** tag for a {@code CONSTANT_MethodType} */
    int CONSTANT_MethodType = 16;

    /** tag for a {@code CONSTANT_InvokeDynamic} */
    int CONSTANT_InvokeDynamic = 18;
}

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

package external.com.android.dx.dex.code;

import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.dex.code.form.Form10t;
import external.com.android.dx.dex.code.form.Form10x;
import external.com.android.dx.dex.code.form.Form11n;
import external.com.android.dx.dex.code.form.Form11x;
import external.com.android.dx.dex.code.form.Form12x;
import external.com.android.dx.dex.code.form.Form20t;
import external.com.android.dx.dex.code.form.Form21c;
import external.com.android.dx.dex.code.form.Form21h;
import external.com.android.dx.dex.code.form.Form21s;
import external.com.android.dx.dex.code.form.Form21t;
import external.com.android.dx.dex.code.form.Form22b;
import external.com.android.dx.dex.code.form.Form22c;
import external.com.android.dx.dex.code.form.Form22s;
import external.com.android.dx.dex.code.form.Form22t;
import external.com.android.dx.dex.code.form.Form22x;
import external.com.android.dx.dex.code.form.Form23x;
import external.com.android.dx.dex.code.form.Form30t;
import external.com.android.dx.dex.code.form.Form31c;
import external.com.android.dx.dex.code.form.Form31i;
import external.com.android.dx.dex.code.form.Form31t;
import external.com.android.dx.dex.code.form.Form32x;
import external.com.android.dx.dex.code.form.Form35c;
import external.com.android.dx.dex.code.form.Form3rc;
import external.com.android.dx.dex.code.form.Form45cc;
import external.com.android.dx.dex.code.form.Form4rcc;
import external.com.android.dx.dex.code.form.Form51l;
import external.com.android.dx.dex.code.form.SpecialFormat;
import external.com.android.dx.io.Opcodes;

/**
 * Standard instances of {@link Dop} and utility methods for getting
 * them.
 */
public final class Dops {
    /** {@code non-null;} array containing all the standard instances */
    private static final Dop[] DOPS;

    /**
     * pseudo-opcode used for nonstandard formatted "instructions"
     * (which are mostly not actually instructions, though they do
     * appear in instruction lists). TODO: Retire the usage of this
     * constant.
     */
    public static final Dop SPECIAL_FORMAT =
        new Dop(Opcodes.SPECIAL_FORMAT, Opcodes.SPECIAL_FORMAT,
                Opcodes.NO_NEXT, SpecialFormat.THE_ONE, false);

    // BEGIN(dops); GENERATED AUTOMATICALLY BY opcode-gen
    public static final Dop NOP =
        new Dop(Opcodes.NOP, Opcodes.NOP,
            Opcodes.NO_NEXT, Form10x.THE_ONE, false);

    public static final Dop MOVE =
        new Dop(Opcodes.MOVE, Opcodes.MOVE,
            Opcodes.MOVE_FROM16, Form12x.THE_ONE, true);

    public static final Dop MOVE_FROM16 =
        new Dop(Opcodes.MOVE_FROM16, Opcodes.MOVE,
            Opcodes.MOVE_16, Form22x.THE_ONE, true);

    public static final Dop MOVE_16 =
        new Dop(Opcodes.MOVE_16, Opcodes.MOVE,
            Opcodes.NO_NEXT, Form32x.THE_ONE, true);

    public static final Dop MOVE_WIDE =
        new Dop(Opcodes.MOVE_WIDE, Opcodes.MOVE_WIDE,
            Opcodes.MOVE_WIDE_FROM16, Form12x.THE_ONE, true);

    public static final Dop MOVE_WIDE_FROM16 =
        new Dop(Opcodes.MOVE_WIDE_FROM16, Opcodes.MOVE_WIDE,
            Opcodes.MOVE_WIDE_16, Form22x.THE_ONE, true);

    public static final Dop MOVE_WIDE_16 =
        new Dop(Opcodes.MOVE_WIDE_16, Opcodes.MOVE_WIDE,
            Opcodes.NO_NEXT, Form32x.THE_ONE, true);

    public static final Dop MOVE_OBJECT =
        new Dop(Opcodes.MOVE_OBJECT, Opcodes.MOVE_OBJECT,
            Opcodes.MOVE_OBJECT_FROM16, Form12x.THE_ONE, true);

    public static final Dop MOVE_OBJECT_FROM16 =
        new Dop(Opcodes.MOVE_OBJECT_FROM16, Opcodes.MOVE_OBJECT,
            Opcodes.MOVE_OBJECT_16, Form22x.THE_ONE, true);

    public static final Dop MOVE_OBJECT_16 =
        new Dop(Opcodes.MOVE_OBJECT_16, Opcodes.MOVE_OBJECT,
            Opcodes.NO_NEXT, Form32x.THE_ONE, true);

    public static final Dop MOVE_RESULT =
        new Dop(Opcodes.MOVE_RESULT, Opcodes.MOVE_RESULT,
            Opcodes.NO_NEXT, Form11x.THE_ONE, true);

    public static final Dop MOVE_RESULT_WIDE =
        new Dop(Opcodes.MOVE_RESULT_WIDE, Opcodes.MOVE_RESULT_WIDE,
            Opcodes.NO_NEXT, Form11x.THE_ONE, true);

    public static final Dop MOVE_RESULT_OBJECT =
        new Dop(Opcodes.MOVE_RESULT_OBJECT, Opcodes.MOVE_RESULT_OBJECT,
            Opcodes.NO_NEXT, Form11x.THE_ONE, true);

    public static final Dop MOVE_EXCEPTION =
        new Dop(Opcodes.MOVE_EXCEPTION, Opcodes.MOVE_EXCEPTION,
            Opcodes.NO_NEXT, Form11x.THE_ONE, true);

    public static final Dop RETURN_VOID =
        new Dop(Opcodes.RETURN_VOID, Opcodes.RETURN_VOID,
            Opcodes.NO_NEXT, Form10x.THE_ONE, false);

    public static final Dop RETURN =
        new Dop(Opcodes.RETURN, Opcodes.RETURN,
            Opcodes.NO_NEXT, Form11x.THE_ONE, false);

    public static final Dop RETURN_WIDE =
        new Dop(Opcodes.RETURN_WIDE, Opcodes.RETURN_WIDE,
            Opcodes.NO_NEXT, Form11x.THE_ONE, false);

    public static final Dop RETURN_OBJECT =
        new Dop(Opcodes.RETURN_OBJECT, Opcodes.RETURN_OBJECT,
            Opcodes.NO_NEXT, Form11x.THE_ONE, false);

    public static final Dop CONST_4 =
        new Dop(Opcodes.CONST_4, Opcodes.CONST,
            Opcodes.CONST_16, Form11n.THE_ONE, true);

    public static final Dop CONST_16 =
        new Dop(Opcodes.CONST_16, Opcodes.CONST,
            Opcodes.CONST_HIGH16, Form21s.THE_ONE, true);

    public static final Dop CONST =
        new Dop(Opcodes.CONST, Opcodes.CONST,
            Opcodes.NO_NEXT, Form31i.THE_ONE, true);

    public static final Dop CONST_HIGH16 =
        new Dop(Opcodes.CONST_HIGH16, Opcodes.CONST,
            Opcodes.CONST, Form21h.THE_ONE, true);

    public static final Dop CONST_WIDE_16 =
        new Dop(Opcodes.CONST_WIDE_16, Opcodes.CONST_WIDE,
            Opcodes.CONST_WIDE_HIGH16, Form21s.THE_ONE, true);

    public static final Dop CONST_WIDE_32 =
        new Dop(Opcodes.CONST_WIDE_32, Opcodes.CONST_WIDE,
            Opcodes.CONST_WIDE, Form31i.THE_ONE, true);

    public static final Dop CONST_WIDE =
        new Dop(Opcodes.CONST_WIDE, Opcodes.CONST_WIDE,
            Opcodes.NO_NEXT, Form51l.THE_ONE, true);

    public static final Dop CONST_WIDE_HIGH16 =
        new Dop(Opcodes.CONST_WIDE_HIGH16, Opcodes.CONST_WIDE,
            Opcodes.CONST_WIDE_32, Form21h.THE_ONE, true);

    public static final Dop CONST_STRING =
        new Dop(Opcodes.CONST_STRING, Opcodes.CONST_STRING,
            Opcodes.CONST_STRING_JUMBO, Form21c.THE_ONE, true);

    public static final Dop CONST_STRING_JUMBO =
        new Dop(Opcodes.CONST_STRING_JUMBO, Opcodes.CONST_STRING,
            Opcodes.NO_NEXT, Form31c.THE_ONE, true);

    public static final Dop CONST_CLASS =
        new Dop(Opcodes.CONST_CLASS, Opcodes.CONST_CLASS,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop MONITOR_ENTER =
        new Dop(Opcodes.MONITOR_ENTER, Opcodes.MONITOR_ENTER,
            Opcodes.NO_NEXT, Form11x.THE_ONE, false);

    public static final Dop MONITOR_EXIT =
        new Dop(Opcodes.MONITOR_EXIT, Opcodes.MONITOR_EXIT,
            Opcodes.NO_NEXT, Form11x.THE_ONE, false);

    public static final Dop CHECK_CAST =
        new Dop(Opcodes.CHECK_CAST, Opcodes.CHECK_CAST,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop INSTANCE_OF =
        new Dop(Opcodes.INSTANCE_OF, Opcodes.INSTANCE_OF,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop ARRAY_LENGTH =
        new Dop(Opcodes.ARRAY_LENGTH, Opcodes.ARRAY_LENGTH,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop NEW_INSTANCE =
        new Dop(Opcodes.NEW_INSTANCE, Opcodes.NEW_INSTANCE,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop NEW_ARRAY =
        new Dop(Opcodes.NEW_ARRAY, Opcodes.NEW_ARRAY,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop FILLED_NEW_ARRAY =
        new Dop(Opcodes.FILLED_NEW_ARRAY, Opcodes.FILLED_NEW_ARRAY,
            Opcodes.FILLED_NEW_ARRAY_RANGE, Form35c.THE_ONE, false);

    public static final Dop FILLED_NEW_ARRAY_RANGE =
        new Dop(Opcodes.FILLED_NEW_ARRAY_RANGE, Opcodes.FILLED_NEW_ARRAY,
            Opcodes.NO_NEXT, Form3rc.THE_ONE, false);

    public static final Dop FILL_ARRAY_DATA =
        new Dop(Opcodes.FILL_ARRAY_DATA, Opcodes.FILL_ARRAY_DATA,
            Opcodes.NO_NEXT, Form31t.THE_ONE, false);

    public static final Dop THROW =
        new Dop(Opcodes.THROW, Opcodes.THROW,
            Opcodes.NO_NEXT, Form11x.THE_ONE, false);

    public static final Dop GOTO =
        new Dop(Opcodes.GOTO, Opcodes.GOTO,
            Opcodes.GOTO_16, Form10t.THE_ONE, false);

    public static final Dop GOTO_16 =
        new Dop(Opcodes.GOTO_16, Opcodes.GOTO,
            Opcodes.GOTO_32, Form20t.THE_ONE, false);

    public static final Dop GOTO_32 =
        new Dop(Opcodes.GOTO_32, Opcodes.GOTO,
            Opcodes.NO_NEXT, Form30t.THE_ONE, false);

    public static final Dop PACKED_SWITCH =
        new Dop(Opcodes.PACKED_SWITCH, Opcodes.PACKED_SWITCH,
            Opcodes.NO_NEXT, Form31t.THE_ONE, false);

    public static final Dop SPARSE_SWITCH =
        new Dop(Opcodes.SPARSE_SWITCH, Opcodes.SPARSE_SWITCH,
            Opcodes.NO_NEXT, Form31t.THE_ONE, false);

    public static final Dop CMPL_FLOAT =
        new Dop(Opcodes.CMPL_FLOAT, Opcodes.CMPL_FLOAT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop CMPG_FLOAT =
        new Dop(Opcodes.CMPG_FLOAT, Opcodes.CMPG_FLOAT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop CMPL_DOUBLE =
        new Dop(Opcodes.CMPL_DOUBLE, Opcodes.CMPL_DOUBLE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop CMPG_DOUBLE =
        new Dop(Opcodes.CMPG_DOUBLE, Opcodes.CMPG_DOUBLE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop CMP_LONG =
        new Dop(Opcodes.CMP_LONG, Opcodes.CMP_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop IF_EQ =
        new Dop(Opcodes.IF_EQ, Opcodes.IF_EQ,
            Opcodes.NO_NEXT, Form22t.THE_ONE, false);

    public static final Dop IF_NE =
        new Dop(Opcodes.IF_NE, Opcodes.IF_NE,
            Opcodes.NO_NEXT, Form22t.THE_ONE, false);

    public static final Dop IF_LT =
        new Dop(Opcodes.IF_LT, Opcodes.IF_LT,
            Opcodes.NO_NEXT, Form22t.THE_ONE, false);

    public static final Dop IF_GE =
        new Dop(Opcodes.IF_GE, Opcodes.IF_GE,
            Opcodes.NO_NEXT, Form22t.THE_ONE, false);

    public static final Dop IF_GT =
        new Dop(Opcodes.IF_GT, Opcodes.IF_GT,
            Opcodes.NO_NEXT, Form22t.THE_ONE, false);

    public static final Dop IF_LE =
        new Dop(Opcodes.IF_LE, Opcodes.IF_LE,
            Opcodes.NO_NEXT, Form22t.THE_ONE, false);

    public static final Dop IF_EQZ =
        new Dop(Opcodes.IF_EQZ, Opcodes.IF_EQZ,
            Opcodes.NO_NEXT, Form21t.THE_ONE, false);

    public static final Dop IF_NEZ =
        new Dop(Opcodes.IF_NEZ, Opcodes.IF_NEZ,
            Opcodes.NO_NEXT, Form21t.THE_ONE, false);

    public static final Dop IF_LTZ =
        new Dop(Opcodes.IF_LTZ, Opcodes.IF_LTZ,
            Opcodes.NO_NEXT, Form21t.THE_ONE, false);

    public static final Dop IF_GEZ =
        new Dop(Opcodes.IF_GEZ, Opcodes.IF_GEZ,
            Opcodes.NO_NEXT, Form21t.THE_ONE, false);

    public static final Dop IF_GTZ =
        new Dop(Opcodes.IF_GTZ, Opcodes.IF_GTZ,
            Opcodes.NO_NEXT, Form21t.THE_ONE, false);

    public static final Dop IF_LEZ =
        new Dop(Opcodes.IF_LEZ, Opcodes.IF_LEZ,
            Opcodes.NO_NEXT, Form21t.THE_ONE, false);

    public static final Dop AGET =
        new Dop(Opcodes.AGET, Opcodes.AGET,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop AGET_WIDE =
        new Dop(Opcodes.AGET_WIDE, Opcodes.AGET_WIDE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop AGET_OBJECT =
        new Dop(Opcodes.AGET_OBJECT, Opcodes.AGET_OBJECT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop AGET_BOOLEAN =
        new Dop(Opcodes.AGET_BOOLEAN, Opcodes.AGET_BOOLEAN,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop AGET_BYTE =
        new Dop(Opcodes.AGET_BYTE, Opcodes.AGET_BYTE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop AGET_CHAR =
        new Dop(Opcodes.AGET_CHAR, Opcodes.AGET_CHAR,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop AGET_SHORT =
        new Dop(Opcodes.AGET_SHORT, Opcodes.AGET_SHORT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop APUT =
        new Dop(Opcodes.APUT, Opcodes.APUT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, false);

    public static final Dop APUT_WIDE =
        new Dop(Opcodes.APUT_WIDE, Opcodes.APUT_WIDE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, false);

    public static final Dop APUT_OBJECT =
        new Dop(Opcodes.APUT_OBJECT, Opcodes.APUT_OBJECT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, false);

    public static final Dop APUT_BOOLEAN =
        new Dop(Opcodes.APUT_BOOLEAN, Opcodes.APUT_BOOLEAN,
            Opcodes.NO_NEXT, Form23x.THE_ONE, false);

    public static final Dop APUT_BYTE =
        new Dop(Opcodes.APUT_BYTE, Opcodes.APUT_BYTE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, false);

    public static final Dop APUT_CHAR =
        new Dop(Opcodes.APUT_CHAR, Opcodes.APUT_CHAR,
            Opcodes.NO_NEXT, Form23x.THE_ONE, false);

    public static final Dop APUT_SHORT =
        new Dop(Opcodes.APUT_SHORT, Opcodes.APUT_SHORT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, false);

    public static final Dop IGET =
        new Dop(Opcodes.IGET, Opcodes.IGET,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop IGET_WIDE =
        new Dop(Opcodes.IGET_WIDE, Opcodes.IGET_WIDE,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop IGET_OBJECT =
        new Dop(Opcodes.IGET_OBJECT, Opcodes.IGET_OBJECT,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop IGET_BOOLEAN =
        new Dop(Opcodes.IGET_BOOLEAN, Opcodes.IGET_BOOLEAN,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop IGET_BYTE =
        new Dop(Opcodes.IGET_BYTE, Opcodes.IGET_BYTE,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop IGET_CHAR =
        new Dop(Opcodes.IGET_CHAR, Opcodes.IGET_CHAR,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop IGET_SHORT =
        new Dop(Opcodes.IGET_SHORT, Opcodes.IGET_SHORT,
            Opcodes.NO_NEXT, Form22c.THE_ONE, true);

    public static final Dop IPUT =
        new Dop(Opcodes.IPUT, Opcodes.IPUT,
            Opcodes.NO_NEXT, Form22c.THE_ONE, false);

    public static final Dop IPUT_WIDE =
        new Dop(Opcodes.IPUT_WIDE, Opcodes.IPUT_WIDE,
            Opcodes.NO_NEXT, Form22c.THE_ONE, false);

    public static final Dop IPUT_OBJECT =
        new Dop(Opcodes.IPUT_OBJECT, Opcodes.IPUT_OBJECT,
            Opcodes.NO_NEXT, Form22c.THE_ONE, false);

    public static final Dop IPUT_BOOLEAN =
        new Dop(Opcodes.IPUT_BOOLEAN, Opcodes.IPUT_BOOLEAN,
            Opcodes.NO_NEXT, Form22c.THE_ONE, false);

    public static final Dop IPUT_BYTE =
        new Dop(Opcodes.IPUT_BYTE, Opcodes.IPUT_BYTE,
            Opcodes.NO_NEXT, Form22c.THE_ONE, false);

    public static final Dop IPUT_CHAR =
        new Dop(Opcodes.IPUT_CHAR, Opcodes.IPUT_CHAR,
            Opcodes.NO_NEXT, Form22c.THE_ONE, false);

    public static final Dop IPUT_SHORT =
        new Dop(Opcodes.IPUT_SHORT, Opcodes.IPUT_SHORT,
            Opcodes.NO_NEXT, Form22c.THE_ONE, false);

    public static final Dop SGET =
        new Dop(Opcodes.SGET, Opcodes.SGET,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop SGET_WIDE =
        new Dop(Opcodes.SGET_WIDE, Opcodes.SGET_WIDE,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop SGET_OBJECT =
        new Dop(Opcodes.SGET_OBJECT, Opcodes.SGET_OBJECT,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop SGET_BOOLEAN =
        new Dop(Opcodes.SGET_BOOLEAN, Opcodes.SGET_BOOLEAN,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop SGET_BYTE =
        new Dop(Opcodes.SGET_BYTE, Opcodes.SGET_BYTE,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop SGET_CHAR =
        new Dop(Opcodes.SGET_CHAR, Opcodes.SGET_CHAR,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop SGET_SHORT =
        new Dop(Opcodes.SGET_SHORT, Opcodes.SGET_SHORT,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop SPUT =
        new Dop(Opcodes.SPUT, Opcodes.SPUT,
            Opcodes.NO_NEXT, Form21c.THE_ONE, false);

    public static final Dop SPUT_WIDE =
        new Dop(Opcodes.SPUT_WIDE, Opcodes.SPUT_WIDE,
            Opcodes.NO_NEXT, Form21c.THE_ONE, false);

    public static final Dop SPUT_OBJECT =
        new Dop(Opcodes.SPUT_OBJECT, Opcodes.SPUT_OBJECT,
            Opcodes.NO_NEXT, Form21c.THE_ONE, false);

    public static final Dop SPUT_BOOLEAN =
        new Dop(Opcodes.SPUT_BOOLEAN, Opcodes.SPUT_BOOLEAN,
            Opcodes.NO_NEXT, Form21c.THE_ONE, false);

    public static final Dop SPUT_BYTE =
        new Dop(Opcodes.SPUT_BYTE, Opcodes.SPUT_BYTE,
            Opcodes.NO_NEXT, Form21c.THE_ONE, false);

    public static final Dop SPUT_CHAR =
        new Dop(Opcodes.SPUT_CHAR, Opcodes.SPUT_CHAR,
            Opcodes.NO_NEXT, Form21c.THE_ONE, false);

    public static final Dop SPUT_SHORT =
        new Dop(Opcodes.SPUT_SHORT, Opcodes.SPUT_SHORT,
            Opcodes.NO_NEXT, Form21c.THE_ONE, false);

    public static final Dop INVOKE_VIRTUAL =
        new Dop(Opcodes.INVOKE_VIRTUAL, Opcodes.INVOKE_VIRTUAL,
            Opcodes.INVOKE_VIRTUAL_RANGE, Form35c.THE_ONE, false);

    public static final Dop INVOKE_SUPER =
        new Dop(Opcodes.INVOKE_SUPER, Opcodes.INVOKE_SUPER,
            Opcodes.INVOKE_SUPER_RANGE, Form35c.THE_ONE, false);

    public static final Dop INVOKE_DIRECT =
        new Dop(Opcodes.INVOKE_DIRECT, Opcodes.INVOKE_DIRECT,
            Opcodes.INVOKE_DIRECT_RANGE, Form35c.THE_ONE, false);

    public static final Dop INVOKE_STATIC =
        new Dop(Opcodes.INVOKE_STATIC, Opcodes.INVOKE_STATIC,
            Opcodes.INVOKE_STATIC_RANGE, Form35c.THE_ONE, false);

    public static final Dop INVOKE_INTERFACE =
        new Dop(Opcodes.INVOKE_INTERFACE, Opcodes.INVOKE_INTERFACE,
            Opcodes.INVOKE_INTERFACE_RANGE, Form35c.THE_ONE, false);

    public static final Dop INVOKE_VIRTUAL_RANGE =
        new Dop(Opcodes.INVOKE_VIRTUAL_RANGE, Opcodes.INVOKE_VIRTUAL,
            Opcodes.NO_NEXT, Form3rc.THE_ONE, false);

    public static final Dop INVOKE_SUPER_RANGE =
        new Dop(Opcodes.INVOKE_SUPER_RANGE, Opcodes.INVOKE_SUPER,
            Opcodes.NO_NEXT, Form3rc.THE_ONE, false);

    public static final Dop INVOKE_DIRECT_RANGE =
        new Dop(Opcodes.INVOKE_DIRECT_RANGE, Opcodes.INVOKE_DIRECT,
            Opcodes.NO_NEXT, Form3rc.THE_ONE, false);

    public static final Dop INVOKE_STATIC_RANGE =
        new Dop(Opcodes.INVOKE_STATIC_RANGE, Opcodes.INVOKE_STATIC,
            Opcodes.NO_NEXT, Form3rc.THE_ONE, false);

    public static final Dop INVOKE_INTERFACE_RANGE =
        new Dop(Opcodes.INVOKE_INTERFACE_RANGE, Opcodes.INVOKE_INTERFACE,
            Opcodes.NO_NEXT, Form3rc.THE_ONE, false);

    public static final Dop NEG_INT =
        new Dop(Opcodes.NEG_INT, Opcodes.NEG_INT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop NOT_INT =
        new Dop(Opcodes.NOT_INT, Opcodes.NOT_INT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop NEG_LONG =
        new Dop(Opcodes.NEG_LONG, Opcodes.NEG_LONG,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop NOT_LONG =
        new Dop(Opcodes.NOT_LONG, Opcodes.NOT_LONG,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop NEG_FLOAT =
        new Dop(Opcodes.NEG_FLOAT, Opcodes.NEG_FLOAT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop NEG_DOUBLE =
        new Dop(Opcodes.NEG_DOUBLE, Opcodes.NEG_DOUBLE,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop INT_TO_LONG =
        new Dop(Opcodes.INT_TO_LONG, Opcodes.INT_TO_LONG,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop INT_TO_FLOAT =
        new Dop(Opcodes.INT_TO_FLOAT, Opcodes.INT_TO_FLOAT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop INT_TO_DOUBLE =
        new Dop(Opcodes.INT_TO_DOUBLE, Opcodes.INT_TO_DOUBLE,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop LONG_TO_INT =
        new Dop(Opcodes.LONG_TO_INT, Opcodes.LONG_TO_INT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop LONG_TO_FLOAT =
        new Dop(Opcodes.LONG_TO_FLOAT, Opcodes.LONG_TO_FLOAT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop LONG_TO_DOUBLE =
        new Dop(Opcodes.LONG_TO_DOUBLE, Opcodes.LONG_TO_DOUBLE,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop FLOAT_TO_INT =
        new Dop(Opcodes.FLOAT_TO_INT, Opcodes.FLOAT_TO_INT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop FLOAT_TO_LONG =
        new Dop(Opcodes.FLOAT_TO_LONG, Opcodes.FLOAT_TO_LONG,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop FLOAT_TO_DOUBLE =
        new Dop(Opcodes.FLOAT_TO_DOUBLE, Opcodes.FLOAT_TO_DOUBLE,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop DOUBLE_TO_INT =
        new Dop(Opcodes.DOUBLE_TO_INT, Opcodes.DOUBLE_TO_INT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop DOUBLE_TO_LONG =
        new Dop(Opcodes.DOUBLE_TO_LONG, Opcodes.DOUBLE_TO_LONG,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop DOUBLE_TO_FLOAT =
        new Dop(Opcodes.DOUBLE_TO_FLOAT, Opcodes.DOUBLE_TO_FLOAT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop INT_TO_BYTE =
        new Dop(Opcodes.INT_TO_BYTE, Opcodes.INT_TO_BYTE,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop INT_TO_CHAR =
        new Dop(Opcodes.INT_TO_CHAR, Opcodes.INT_TO_CHAR,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop INT_TO_SHORT =
        new Dop(Opcodes.INT_TO_SHORT, Opcodes.INT_TO_SHORT,
            Opcodes.NO_NEXT, Form12x.THE_ONE, true);

    public static final Dop ADD_INT =
        new Dop(Opcodes.ADD_INT, Opcodes.ADD_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop SUB_INT =
        new Dop(Opcodes.SUB_INT, Opcodes.SUB_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop MUL_INT =
        new Dop(Opcodes.MUL_INT, Opcodes.MUL_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop DIV_INT =
        new Dop(Opcodes.DIV_INT, Opcodes.DIV_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop REM_INT =
        new Dop(Opcodes.REM_INT, Opcodes.REM_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop AND_INT =
        new Dop(Opcodes.AND_INT, Opcodes.AND_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop OR_INT =
        new Dop(Opcodes.OR_INT, Opcodes.OR_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop XOR_INT =
        new Dop(Opcodes.XOR_INT, Opcodes.XOR_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop SHL_INT =
        new Dop(Opcodes.SHL_INT, Opcodes.SHL_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop SHR_INT =
        new Dop(Opcodes.SHR_INT, Opcodes.SHR_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop USHR_INT =
        new Dop(Opcodes.USHR_INT, Opcodes.USHR_INT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop ADD_LONG =
        new Dop(Opcodes.ADD_LONG, Opcodes.ADD_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop SUB_LONG =
        new Dop(Opcodes.SUB_LONG, Opcodes.SUB_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop MUL_LONG =
        new Dop(Opcodes.MUL_LONG, Opcodes.MUL_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop DIV_LONG =
        new Dop(Opcodes.DIV_LONG, Opcodes.DIV_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop REM_LONG =
        new Dop(Opcodes.REM_LONG, Opcodes.REM_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop AND_LONG =
        new Dop(Opcodes.AND_LONG, Opcodes.AND_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop OR_LONG =
        new Dop(Opcodes.OR_LONG, Opcodes.OR_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop XOR_LONG =
        new Dop(Opcodes.XOR_LONG, Opcodes.XOR_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop SHL_LONG =
        new Dop(Opcodes.SHL_LONG, Opcodes.SHL_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop SHR_LONG =
        new Dop(Opcodes.SHR_LONG, Opcodes.SHR_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop USHR_LONG =
        new Dop(Opcodes.USHR_LONG, Opcodes.USHR_LONG,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop ADD_FLOAT =
        new Dop(Opcodes.ADD_FLOAT, Opcodes.ADD_FLOAT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop SUB_FLOAT =
        new Dop(Opcodes.SUB_FLOAT, Opcodes.SUB_FLOAT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop MUL_FLOAT =
        new Dop(Opcodes.MUL_FLOAT, Opcodes.MUL_FLOAT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop DIV_FLOAT =
        new Dop(Opcodes.DIV_FLOAT, Opcodes.DIV_FLOAT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop REM_FLOAT =
        new Dop(Opcodes.REM_FLOAT, Opcodes.REM_FLOAT,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop ADD_DOUBLE =
        new Dop(Opcodes.ADD_DOUBLE, Opcodes.ADD_DOUBLE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop SUB_DOUBLE =
        new Dop(Opcodes.SUB_DOUBLE, Opcodes.SUB_DOUBLE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop MUL_DOUBLE =
        new Dop(Opcodes.MUL_DOUBLE, Opcodes.MUL_DOUBLE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop DIV_DOUBLE =
        new Dop(Opcodes.DIV_DOUBLE, Opcodes.DIV_DOUBLE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop REM_DOUBLE =
        new Dop(Opcodes.REM_DOUBLE, Opcodes.REM_DOUBLE,
            Opcodes.NO_NEXT, Form23x.THE_ONE, true);

    public static final Dop ADD_INT_2ADDR =
        new Dop(Opcodes.ADD_INT_2ADDR, Opcodes.ADD_INT,
            Opcodes.ADD_INT, Form12x.THE_ONE, true);

    public static final Dop SUB_INT_2ADDR =
        new Dop(Opcodes.SUB_INT_2ADDR, Opcodes.SUB_INT,
            Opcodes.SUB_INT, Form12x.THE_ONE, true);

    public static final Dop MUL_INT_2ADDR =
        new Dop(Opcodes.MUL_INT_2ADDR, Opcodes.MUL_INT,
            Opcodes.MUL_INT, Form12x.THE_ONE, true);

    public static final Dop DIV_INT_2ADDR =
        new Dop(Opcodes.DIV_INT_2ADDR, Opcodes.DIV_INT,
            Opcodes.DIV_INT, Form12x.THE_ONE, true);

    public static final Dop REM_INT_2ADDR =
        new Dop(Opcodes.REM_INT_2ADDR, Opcodes.REM_INT,
            Opcodes.REM_INT, Form12x.THE_ONE, true);

    public static final Dop AND_INT_2ADDR =
        new Dop(Opcodes.AND_INT_2ADDR, Opcodes.AND_INT,
            Opcodes.AND_INT, Form12x.THE_ONE, true);

    public static final Dop OR_INT_2ADDR =
        new Dop(Opcodes.OR_INT_2ADDR, Opcodes.OR_INT,
            Opcodes.OR_INT, Form12x.THE_ONE, true);

    public static final Dop XOR_INT_2ADDR =
        new Dop(Opcodes.XOR_INT_2ADDR, Opcodes.XOR_INT,
            Opcodes.XOR_INT, Form12x.THE_ONE, true);

    public static final Dop SHL_INT_2ADDR =
        new Dop(Opcodes.SHL_INT_2ADDR, Opcodes.SHL_INT,
            Opcodes.SHL_INT, Form12x.THE_ONE, true);

    public static final Dop SHR_INT_2ADDR =
        new Dop(Opcodes.SHR_INT_2ADDR, Opcodes.SHR_INT,
            Opcodes.SHR_INT, Form12x.THE_ONE, true);

    public static final Dop USHR_INT_2ADDR =
        new Dop(Opcodes.USHR_INT_2ADDR, Opcodes.USHR_INT,
            Opcodes.USHR_INT, Form12x.THE_ONE, true);

    public static final Dop ADD_LONG_2ADDR =
        new Dop(Opcodes.ADD_LONG_2ADDR, Opcodes.ADD_LONG,
            Opcodes.ADD_LONG, Form12x.THE_ONE, true);

    public static final Dop SUB_LONG_2ADDR =
        new Dop(Opcodes.SUB_LONG_2ADDR, Opcodes.SUB_LONG,
            Opcodes.SUB_LONG, Form12x.THE_ONE, true);

    public static final Dop MUL_LONG_2ADDR =
        new Dop(Opcodes.MUL_LONG_2ADDR, Opcodes.MUL_LONG,
            Opcodes.MUL_LONG, Form12x.THE_ONE, true);

    public static final Dop DIV_LONG_2ADDR =
        new Dop(Opcodes.DIV_LONG_2ADDR, Opcodes.DIV_LONG,
            Opcodes.DIV_LONG, Form12x.THE_ONE, true);

    public static final Dop REM_LONG_2ADDR =
        new Dop(Opcodes.REM_LONG_2ADDR, Opcodes.REM_LONG,
            Opcodes.REM_LONG, Form12x.THE_ONE, true);

    public static final Dop AND_LONG_2ADDR =
        new Dop(Opcodes.AND_LONG_2ADDR, Opcodes.AND_LONG,
            Opcodes.AND_LONG, Form12x.THE_ONE, true);

    public static final Dop OR_LONG_2ADDR =
        new Dop(Opcodes.OR_LONG_2ADDR, Opcodes.OR_LONG,
            Opcodes.OR_LONG, Form12x.THE_ONE, true);

    public static final Dop XOR_LONG_2ADDR =
        new Dop(Opcodes.XOR_LONG_2ADDR, Opcodes.XOR_LONG,
            Opcodes.XOR_LONG, Form12x.THE_ONE, true);

    public static final Dop SHL_LONG_2ADDR =
        new Dop(Opcodes.SHL_LONG_2ADDR, Opcodes.SHL_LONG,
            Opcodes.SHL_LONG, Form12x.THE_ONE, true);

    public static final Dop SHR_LONG_2ADDR =
        new Dop(Opcodes.SHR_LONG_2ADDR, Opcodes.SHR_LONG,
            Opcodes.SHR_LONG, Form12x.THE_ONE, true);

    public static final Dop USHR_LONG_2ADDR =
        new Dop(Opcodes.USHR_LONG_2ADDR, Opcodes.USHR_LONG,
            Opcodes.USHR_LONG, Form12x.THE_ONE, true);

    public static final Dop ADD_FLOAT_2ADDR =
        new Dop(Opcodes.ADD_FLOAT_2ADDR, Opcodes.ADD_FLOAT,
            Opcodes.ADD_FLOAT, Form12x.THE_ONE, true);

    public static final Dop SUB_FLOAT_2ADDR =
        new Dop(Opcodes.SUB_FLOAT_2ADDR, Opcodes.SUB_FLOAT,
            Opcodes.SUB_FLOAT, Form12x.THE_ONE, true);

    public static final Dop MUL_FLOAT_2ADDR =
        new Dop(Opcodes.MUL_FLOAT_2ADDR, Opcodes.MUL_FLOAT,
            Opcodes.MUL_FLOAT, Form12x.THE_ONE, true);

    public static final Dop DIV_FLOAT_2ADDR =
        new Dop(Opcodes.DIV_FLOAT_2ADDR, Opcodes.DIV_FLOAT,
            Opcodes.DIV_FLOAT, Form12x.THE_ONE, true);

    public static final Dop REM_FLOAT_2ADDR =
        new Dop(Opcodes.REM_FLOAT_2ADDR, Opcodes.REM_FLOAT,
            Opcodes.REM_FLOAT, Form12x.THE_ONE, true);

    public static final Dop ADD_DOUBLE_2ADDR =
        new Dop(Opcodes.ADD_DOUBLE_2ADDR, Opcodes.ADD_DOUBLE,
            Opcodes.ADD_DOUBLE, Form12x.THE_ONE, true);

    public static final Dop SUB_DOUBLE_2ADDR =
        new Dop(Opcodes.SUB_DOUBLE_2ADDR, Opcodes.SUB_DOUBLE,
            Opcodes.SUB_DOUBLE, Form12x.THE_ONE, true);

    public static final Dop MUL_DOUBLE_2ADDR =
        new Dop(Opcodes.MUL_DOUBLE_2ADDR, Opcodes.MUL_DOUBLE,
            Opcodes.MUL_DOUBLE, Form12x.THE_ONE, true);

    public static final Dop DIV_DOUBLE_2ADDR =
        new Dop(Opcodes.DIV_DOUBLE_2ADDR, Opcodes.DIV_DOUBLE,
            Opcodes.DIV_DOUBLE, Form12x.THE_ONE, true);

    public static final Dop REM_DOUBLE_2ADDR =
        new Dop(Opcodes.REM_DOUBLE_2ADDR, Opcodes.REM_DOUBLE,
            Opcodes.REM_DOUBLE, Form12x.THE_ONE, true);

    public static final Dop ADD_INT_LIT16 =
        new Dop(Opcodes.ADD_INT_LIT16, Opcodes.ADD_INT,
            Opcodes.NO_NEXT, Form22s.THE_ONE, true);

    public static final Dop RSUB_INT =
        new Dop(Opcodes.RSUB_INT, Opcodes.RSUB_INT,
            Opcodes.NO_NEXT, Form22s.THE_ONE, true);

    public static final Dop MUL_INT_LIT16 =
        new Dop(Opcodes.MUL_INT_LIT16, Opcodes.MUL_INT,
            Opcodes.NO_NEXT, Form22s.THE_ONE, true);

    public static final Dop DIV_INT_LIT16 =
        new Dop(Opcodes.DIV_INT_LIT16, Opcodes.DIV_INT,
            Opcodes.NO_NEXT, Form22s.THE_ONE, true);

    public static final Dop REM_INT_LIT16 =
        new Dop(Opcodes.REM_INT_LIT16, Opcodes.REM_INT,
            Opcodes.NO_NEXT, Form22s.THE_ONE, true);

    public static final Dop AND_INT_LIT16 =
        new Dop(Opcodes.AND_INT_LIT16, Opcodes.AND_INT,
            Opcodes.NO_NEXT, Form22s.THE_ONE, true);

    public static final Dop OR_INT_LIT16 =
        new Dop(Opcodes.OR_INT_LIT16, Opcodes.OR_INT,
            Opcodes.NO_NEXT, Form22s.THE_ONE, true);

    public static final Dop XOR_INT_LIT16 =
        new Dop(Opcodes.XOR_INT_LIT16, Opcodes.XOR_INT,
            Opcodes.NO_NEXT, Form22s.THE_ONE, true);

    public static final Dop ADD_INT_LIT8 =
        new Dop(Opcodes.ADD_INT_LIT8, Opcodes.ADD_INT,
            Opcodes.ADD_INT_LIT16, Form22b.THE_ONE, true);

    public static final Dop RSUB_INT_LIT8 =
        new Dop(Opcodes.RSUB_INT_LIT8, Opcodes.RSUB_INT,
            Opcodes.RSUB_INT, Form22b.THE_ONE, true);

    public static final Dop MUL_INT_LIT8 =
        new Dop(Opcodes.MUL_INT_LIT8, Opcodes.MUL_INT,
            Opcodes.MUL_INT_LIT16, Form22b.THE_ONE, true);

    public static final Dop DIV_INT_LIT8 =
        new Dop(Opcodes.DIV_INT_LIT8, Opcodes.DIV_INT,
            Opcodes.DIV_INT_LIT16, Form22b.THE_ONE, true);

    public static final Dop REM_INT_LIT8 =
        new Dop(Opcodes.REM_INT_LIT8, Opcodes.REM_INT,
            Opcodes.REM_INT_LIT16, Form22b.THE_ONE, true);

    public static final Dop AND_INT_LIT8 =
        new Dop(Opcodes.AND_INT_LIT8, Opcodes.AND_INT,
            Opcodes.AND_INT_LIT16, Form22b.THE_ONE, true);

    public static final Dop OR_INT_LIT8 =
        new Dop(Opcodes.OR_INT_LIT8, Opcodes.OR_INT,
            Opcodes.OR_INT_LIT16, Form22b.THE_ONE, true);

    public static final Dop XOR_INT_LIT8 =
        new Dop(Opcodes.XOR_INT_LIT8, Opcodes.XOR_INT,
            Opcodes.XOR_INT_LIT16, Form22b.THE_ONE, true);

    public static final Dop SHL_INT_LIT8 =
        new Dop(Opcodes.SHL_INT_LIT8, Opcodes.SHL_INT,
            Opcodes.NO_NEXT, Form22b.THE_ONE, true);

    public static final Dop SHR_INT_LIT8 =
        new Dop(Opcodes.SHR_INT_LIT8, Opcodes.SHR_INT,
            Opcodes.NO_NEXT, Form22b.THE_ONE, true);

    public static final Dop USHR_INT_LIT8 =
        new Dop(Opcodes.USHR_INT_LIT8, Opcodes.USHR_INT,
            Opcodes.NO_NEXT, Form22b.THE_ONE, true);

    public static final Dop INVOKE_POLYMORPHIC =
        new Dop(Opcodes.INVOKE_POLYMORPHIC, Opcodes.INVOKE_POLYMORPHIC,
            Opcodes.INVOKE_POLYMORPHIC_RANGE, Form45cc.THE_ONE, false);

    public static final Dop INVOKE_POLYMORPHIC_RANGE =
        new Dop(Opcodes.INVOKE_POLYMORPHIC_RANGE, Opcodes.INVOKE_POLYMORPHIC,
            Opcodes.NO_NEXT, Form4rcc.THE_ONE, false);

    public static final Dop INVOKE_CUSTOM =
        new Dop(Opcodes.INVOKE_CUSTOM, Opcodes.INVOKE_CUSTOM,
            Opcodes.INVOKE_CUSTOM_RANGE, Form35c.THE_ONE, false);

    public static final Dop INVOKE_CUSTOM_RANGE =
        new Dop(Opcodes.INVOKE_CUSTOM_RANGE, Opcodes.INVOKE_CUSTOM,
            Opcodes.NO_NEXT, Form3rc.THE_ONE, false);

    public static final Dop CONST_METHOD_HANDLE =
        new Dop(Opcodes.CONST_METHOD_HANDLE, Opcodes.CONST_METHOD_HANDLE,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    public static final Dop CONST_METHOD_TYPE =
        new Dop(Opcodes.CONST_METHOD_TYPE, Opcodes.CONST_METHOD_TYPE,
            Opcodes.NO_NEXT, Form21c.THE_ONE, true);

    // END(dops)

    // Static initialization.
    static {
        DOPS = new Dop[Opcodes.MAX_VALUE - Opcodes.MIN_VALUE + 1];

        set(SPECIAL_FORMAT);

        // BEGIN(dops-init); GENERATED AUTOMATICALLY BY opcode-gen
        set(NOP);
        set(MOVE);
        set(MOVE_FROM16);
        set(MOVE_16);
        set(MOVE_WIDE);
        set(MOVE_WIDE_FROM16);
        set(MOVE_WIDE_16);
        set(MOVE_OBJECT);
        set(MOVE_OBJECT_FROM16);
        set(MOVE_OBJECT_16);
        set(MOVE_RESULT);
        set(MOVE_RESULT_WIDE);
        set(MOVE_RESULT_OBJECT);
        set(MOVE_EXCEPTION);
        set(RETURN_VOID);
        set(RETURN);
        set(RETURN_WIDE);
        set(RETURN_OBJECT);
        set(CONST_4);
        set(CONST_16);
        set(CONST);
        set(CONST_HIGH16);
        set(CONST_WIDE_16);
        set(CONST_WIDE_32);
        set(CONST_WIDE);
        set(CONST_WIDE_HIGH16);
        set(CONST_STRING);
        set(CONST_STRING_JUMBO);
        set(CONST_CLASS);
        set(MONITOR_ENTER);
        set(MONITOR_EXIT);
        set(CHECK_CAST);
        set(INSTANCE_OF);
        set(ARRAY_LENGTH);
        set(NEW_INSTANCE);
        set(NEW_ARRAY);
        set(FILLED_NEW_ARRAY);
        set(FILLED_NEW_ARRAY_RANGE);
        set(FILL_ARRAY_DATA);
        set(THROW);
        set(GOTO);
        set(GOTO_16);
        set(GOTO_32);
        set(PACKED_SWITCH);
        set(SPARSE_SWITCH);
        set(CMPL_FLOAT);
        set(CMPG_FLOAT);
        set(CMPL_DOUBLE);
        set(CMPG_DOUBLE);
        set(CMP_LONG);
        set(IF_EQ);
        set(IF_NE);
        set(IF_LT);
        set(IF_GE);
        set(IF_GT);
        set(IF_LE);
        set(IF_EQZ);
        set(IF_NEZ);
        set(IF_LTZ);
        set(IF_GEZ);
        set(IF_GTZ);
        set(IF_LEZ);
        set(AGET);
        set(AGET_WIDE);
        set(AGET_OBJECT);
        set(AGET_BOOLEAN);
        set(AGET_BYTE);
        set(AGET_CHAR);
        set(AGET_SHORT);
        set(APUT);
        set(APUT_WIDE);
        set(APUT_OBJECT);
        set(APUT_BOOLEAN);
        set(APUT_BYTE);
        set(APUT_CHAR);
        set(APUT_SHORT);
        set(IGET);
        set(IGET_WIDE);
        set(IGET_OBJECT);
        set(IGET_BOOLEAN);
        set(IGET_BYTE);
        set(IGET_CHAR);
        set(IGET_SHORT);
        set(IPUT);
        set(IPUT_WIDE);
        set(IPUT_OBJECT);
        set(IPUT_BOOLEAN);
        set(IPUT_BYTE);
        set(IPUT_CHAR);
        set(IPUT_SHORT);
        set(SGET);
        set(SGET_WIDE);
        set(SGET_OBJECT);
        set(SGET_BOOLEAN);
        set(SGET_BYTE);
        set(SGET_CHAR);
        set(SGET_SHORT);
        set(SPUT);
        set(SPUT_WIDE);
        set(SPUT_OBJECT);
        set(SPUT_BOOLEAN);
        set(SPUT_BYTE);
        set(SPUT_CHAR);
        set(SPUT_SHORT);
        set(INVOKE_VIRTUAL);
        set(INVOKE_SUPER);
        set(INVOKE_DIRECT);
        set(INVOKE_STATIC);
        set(INVOKE_INTERFACE);
        set(INVOKE_VIRTUAL_RANGE);
        set(INVOKE_SUPER_RANGE);
        set(INVOKE_DIRECT_RANGE);
        set(INVOKE_STATIC_RANGE);
        set(INVOKE_INTERFACE_RANGE);
        set(NEG_INT);
        set(NOT_INT);
        set(NEG_LONG);
        set(NOT_LONG);
        set(NEG_FLOAT);
        set(NEG_DOUBLE);
        set(INT_TO_LONG);
        set(INT_TO_FLOAT);
        set(INT_TO_DOUBLE);
        set(LONG_TO_INT);
        set(LONG_TO_FLOAT);
        set(LONG_TO_DOUBLE);
        set(FLOAT_TO_INT);
        set(FLOAT_TO_LONG);
        set(FLOAT_TO_DOUBLE);
        set(DOUBLE_TO_INT);
        set(DOUBLE_TO_LONG);
        set(DOUBLE_TO_FLOAT);
        set(INT_TO_BYTE);
        set(INT_TO_CHAR);
        set(INT_TO_SHORT);
        set(ADD_INT);
        set(SUB_INT);
        set(MUL_INT);
        set(DIV_INT);
        set(REM_INT);
        set(AND_INT);
        set(OR_INT);
        set(XOR_INT);
        set(SHL_INT);
        set(SHR_INT);
        set(USHR_INT);
        set(ADD_LONG);
        set(SUB_LONG);
        set(MUL_LONG);
        set(DIV_LONG);
        set(REM_LONG);
        set(AND_LONG);
        set(OR_LONG);
        set(XOR_LONG);
        set(SHL_LONG);
        set(SHR_LONG);
        set(USHR_LONG);
        set(ADD_FLOAT);
        set(SUB_FLOAT);
        set(MUL_FLOAT);
        set(DIV_FLOAT);
        set(REM_FLOAT);
        set(ADD_DOUBLE);
        set(SUB_DOUBLE);
        set(MUL_DOUBLE);
        set(DIV_DOUBLE);
        set(REM_DOUBLE);
        set(ADD_INT_2ADDR);
        set(SUB_INT_2ADDR);
        set(MUL_INT_2ADDR);
        set(DIV_INT_2ADDR);
        set(REM_INT_2ADDR);
        set(AND_INT_2ADDR);
        set(OR_INT_2ADDR);
        set(XOR_INT_2ADDR);
        set(SHL_INT_2ADDR);
        set(SHR_INT_2ADDR);
        set(USHR_INT_2ADDR);
        set(ADD_LONG_2ADDR);
        set(SUB_LONG_2ADDR);
        set(MUL_LONG_2ADDR);
        set(DIV_LONG_2ADDR);
        set(REM_LONG_2ADDR);
        set(AND_LONG_2ADDR);
        set(OR_LONG_2ADDR);
        set(XOR_LONG_2ADDR);
        set(SHL_LONG_2ADDR);
        set(SHR_LONG_2ADDR);
        set(USHR_LONG_2ADDR);
        set(ADD_FLOAT_2ADDR);
        set(SUB_FLOAT_2ADDR);
        set(MUL_FLOAT_2ADDR);
        set(DIV_FLOAT_2ADDR);
        set(REM_FLOAT_2ADDR);
        set(ADD_DOUBLE_2ADDR);
        set(SUB_DOUBLE_2ADDR);
        set(MUL_DOUBLE_2ADDR);
        set(DIV_DOUBLE_2ADDR);
        set(REM_DOUBLE_2ADDR);
        set(ADD_INT_LIT16);
        set(RSUB_INT);
        set(MUL_INT_LIT16);
        set(DIV_INT_LIT16);
        set(REM_INT_LIT16);
        set(AND_INT_LIT16);
        set(OR_INT_LIT16);
        set(XOR_INT_LIT16);
        set(ADD_INT_LIT8);
        set(RSUB_INT_LIT8);
        set(MUL_INT_LIT8);
        set(DIV_INT_LIT8);
        set(REM_INT_LIT8);
        set(AND_INT_LIT8);
        set(OR_INT_LIT8);
        set(XOR_INT_LIT8);
        set(SHL_INT_LIT8);
        set(SHR_INT_LIT8);
        set(USHR_INT_LIT8);
        set(INVOKE_POLYMORPHIC);
        set(INVOKE_POLYMORPHIC_RANGE);
        set(INVOKE_CUSTOM);
        set(INVOKE_CUSTOM_RANGE);
        set(CONST_METHOD_HANDLE);
        set(CONST_METHOD_TYPE);
        // END(dops-init)
    }

    /**
     * This class is uninstantiable.
     */
    private Dops() {
        // This space intentionally left blank.
    }

    /**
     * Gets the {@link Dop} for the given opcode value.
     *
     * @param opcode {@code Opcodes.MIN_VALUE..Opcodes.MAX_VALUE;} the
     * opcode value
     * @return {@code non-null;} the associated opcode instance
     */
    public static Dop get(int opcode) {
        int idx = opcode - Opcodes.MIN_VALUE;

        try {
            Dop result = DOPS[idx];
            if (result != null) {
                return result;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Fall through.
        }

        throw new IllegalArgumentException("bogus opcode");
    }

    /**
     * Gets the next {@link Dop} in the instruction fitting chain after the
     * given instance, if any.
     *
     * @param opcode {@code non-null;} the opcode
     * @param options {@code non-null;} options, used to determine
     * which opcodes are potentially off-limits
     * @return {@code null-ok;} the next opcode in the same family, in the
     * chain of opcodes to try, or {@code null} if the given opcode is
     * the last in its chain
     */
    public static Dop getNextOrNull(Dop opcode, DexOptions options) {
      int nextOpcode = opcode.getNextOpcode();

      if (nextOpcode == Opcodes.NO_NEXT) {
        return null;
      }

      opcode = get(nextOpcode);

      return opcode;
    }

    /**
     * Puts the given opcode into the table of all ops.
     *
     * @param opcode {@code non-null;} the opcode
     */
    private static void set(Dop opcode) {
        int idx = opcode.getOpcode() - Opcodes.MIN_VALUE;
        DOPS[idx] = opcode;
    }
}

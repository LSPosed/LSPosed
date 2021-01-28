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

package external.com.android.dx.cf.code;

import external.com.android.dx.util.Hex;

/**
 * Constants and utility methods for dealing with bytecode arrays at an
 * opcode level.
 */
public class ByteOps {
    // one constant per opcode
    public static final int NOP = 0x00;
    public static final int ACONST_NULL = 0x01;
    public static final int ICONST_M1 = 0x02;
    public static final int ICONST_0 = 0x03;
    public static final int ICONST_1 = 0x04;
    public static final int ICONST_2 = 0x05;
    public static final int ICONST_3 = 0x06;
    public static final int ICONST_4 = 0x07;
    public static final int ICONST_5 = 0x08;
    public static final int LCONST_0 = 0x09;
    public static final int LCONST_1 = 0x0a;
    public static final int FCONST_0 = 0x0b;
    public static final int FCONST_1 = 0x0c;
    public static final int FCONST_2 = 0x0d;
    public static final int DCONST_0 = 0x0e;
    public static final int DCONST_1 = 0x0f;
    public static final int BIPUSH = 0x10;
    public static final int SIPUSH = 0x11;
    public static final int LDC = 0x12;
    public static final int LDC_W = 0x13;
    public static final int LDC2_W = 0x14;
    public static final int ILOAD = 0x15;
    public static final int LLOAD = 0x16;
    public static final int FLOAD = 0x17;
    public static final int DLOAD = 0x18;
    public static final int ALOAD = 0x19;
    public static final int ILOAD_0 = 0x1a;
    public static final int ILOAD_1 = 0x1b;
    public static final int ILOAD_2 = 0x1c;
    public static final int ILOAD_3 = 0x1d;
    public static final int LLOAD_0 = 0x1e;
    public static final int LLOAD_1 = 0x1f;
    public static final int LLOAD_2 = 0x20;
    public static final int LLOAD_3 = 0x21;
    public static final int FLOAD_0 = 0x22;
    public static final int FLOAD_1 = 0x23;
    public static final int FLOAD_2 = 0x24;
    public static final int FLOAD_3 = 0x25;
    public static final int DLOAD_0 = 0x26;
    public static final int DLOAD_1 = 0x27;
    public static final int DLOAD_2 = 0x28;
    public static final int DLOAD_3 = 0x29;
    public static final int ALOAD_0 = 0x2a;
    public static final int ALOAD_1 = 0x2b;
    public static final int ALOAD_2 = 0x2c;
    public static final int ALOAD_3 = 0x2d;
    public static final int IALOAD = 0x2e;
    public static final int LALOAD = 0x2f;
    public static final int FALOAD = 0x30;
    public static final int DALOAD = 0x31;
    public static final int AALOAD = 0x32;
    public static final int BALOAD = 0x33;
    public static final int CALOAD = 0x34;
    public static final int SALOAD = 0x35;
    public static final int ISTORE = 0x36;
    public static final int LSTORE = 0x37;
    public static final int FSTORE = 0x38;
    public static final int DSTORE = 0x39;
    public static final int ASTORE = 0x3a;
    public static final int ISTORE_0 = 0x3b;
    public static final int ISTORE_1 = 0x3c;
    public static final int ISTORE_2 = 0x3d;
    public static final int ISTORE_3 = 0x3e;
    public static final int LSTORE_0 = 0x3f;
    public static final int LSTORE_1 = 0x40;
    public static final int LSTORE_2 = 0x41;
    public static final int LSTORE_3 = 0x42;
    public static final int FSTORE_0 = 0x43;
    public static final int FSTORE_1 = 0x44;
    public static final int FSTORE_2 = 0x45;
    public static final int FSTORE_3 = 0x46;
    public static final int DSTORE_0 = 0x47;
    public static final int DSTORE_1 = 0x48;
    public static final int DSTORE_2 = 0x49;
    public static final int DSTORE_3 = 0x4a;
    public static final int ASTORE_0 = 0x4b;
    public static final int ASTORE_1 = 0x4c;
    public static final int ASTORE_2 = 0x4d;
    public static final int ASTORE_3 = 0x4e;
    public static final int IASTORE = 0x4f;
    public static final int LASTORE = 0x50;
    public static final int FASTORE = 0x51;
    public static final int DASTORE = 0x52;
    public static final int AASTORE = 0x53;
    public static final int BASTORE = 0x54;
    public static final int CASTORE = 0x55;
    public static final int SASTORE = 0x56;
    public static final int POP = 0x57;
    public static final int POP2 = 0x58;
    public static final int DUP = 0x59;
    public static final int DUP_X1 = 0x5a;
    public static final int DUP_X2 = 0x5b;
    public static final int DUP2 = 0x5c;
    public static final int DUP2_X1 = 0x5d;
    public static final int DUP2_X2 = 0x5e;
    public static final int SWAP = 0x5f;
    public static final int IADD = 0x60;
    public static final int LADD = 0x61;
    public static final int FADD = 0x62;
    public static final int DADD = 0x63;
    public static final int ISUB = 0x64;
    public static final int LSUB = 0x65;
    public static final int FSUB = 0x66;
    public static final int DSUB = 0x67;
    public static final int IMUL = 0x68;
    public static final int LMUL = 0x69;
    public static final int FMUL = 0x6a;
    public static final int DMUL = 0x6b;
    public static final int IDIV = 0x6c;
    public static final int LDIV = 0x6d;
    public static final int FDIV = 0x6e;
    public static final int DDIV = 0x6f;
    public static final int IREM = 0x70;
    public static final int LREM = 0x71;
    public static final int FREM = 0x72;
    public static final int DREM = 0x73;
    public static final int INEG = 0x74;
    public static final int LNEG = 0x75;
    public static final int FNEG = 0x76;
    public static final int DNEG = 0x77;
    public static final int ISHL = 0x78;
    public static final int LSHL = 0x79;
    public static final int ISHR = 0x7a;
    public static final int LSHR = 0x7b;
    public static final int IUSHR = 0x7c;
    public static final int LUSHR = 0x7d;
    public static final int IAND = 0x7e;
    public static final int LAND = 0x7f;
    public static final int IOR = 0x80;
    public static final int LOR = 0x81;
    public static final int IXOR = 0x82;
    public static final int LXOR = 0x83;
    public static final int IINC = 0x84;
    public static final int I2L = 0x85;
    public static final int I2F = 0x86;
    public static final int I2D = 0x87;
    public static final int L2I = 0x88;
    public static final int L2F = 0x89;
    public static final int L2D = 0x8a;
    public static final int F2I = 0x8b;
    public static final int F2L = 0x8c;
    public static final int F2D = 0x8d;
    public static final int D2I = 0x8e;
    public static final int D2L = 0x8f;
    public static final int D2F = 0x90;
    public static final int I2B = 0x91;
    public static final int I2C = 0x92;
    public static final int I2S = 0x93;
    public static final int LCMP = 0x94;
    public static final int FCMPL = 0x95;
    public static final int FCMPG = 0x96;
    public static final int DCMPL = 0x97;
    public static final int DCMPG = 0x98;
    public static final int IFEQ = 0x99;
    public static final int IFNE = 0x9a;
    public static final int IFLT = 0x9b;
    public static final int IFGE = 0x9c;
    public static final int IFGT = 0x9d;
    public static final int IFLE = 0x9e;
    public static final int IF_ICMPEQ = 0x9f;
    public static final int IF_ICMPNE = 0xa0;
    public static final int IF_ICMPLT = 0xa1;
    public static final int IF_ICMPGE = 0xa2;
    public static final int IF_ICMPGT = 0xa3;
    public static final int IF_ICMPLE = 0xa4;
    public static final int IF_ACMPEQ = 0xa5;
    public static final int IF_ACMPNE = 0xa6;
    public static final int GOTO = 0xa7;
    public static final int JSR = 0xa8;
    public static final int RET = 0xa9;
    public static final int TABLESWITCH = 0xaa;
    public static final int LOOKUPSWITCH = 0xab;
    public static final int IRETURN = 0xac;
    public static final int LRETURN = 0xad;
    public static final int FRETURN = 0xae;
    public static final int DRETURN = 0xaf;
    public static final int ARETURN = 0xb0;
    public static final int RETURN = 0xb1;
    public static final int GETSTATIC = 0xb2;
    public static final int PUTSTATIC = 0xb3;
    public static final int GETFIELD = 0xb4;
    public static final int PUTFIELD = 0xb5;
    public static final int INVOKEVIRTUAL = 0xb6;
    public static final int INVOKESPECIAL = 0xb7;
    public static final int INVOKESTATIC = 0xb8;
    public static final int INVOKEINTERFACE = 0xb9;
    public static final int INVOKEDYNAMIC = 0xba;
    public static final int NEW = 0xbb;
    public static final int NEWARRAY = 0xbc;
    public static final int ANEWARRAY = 0xbd;
    public static final int ARRAYLENGTH = 0xbe;
    public static final int ATHROW = 0xbf;
    public static final int CHECKCAST = 0xc0;
    public static final int INSTANCEOF = 0xc1;
    public static final int MONITORENTER = 0xc2;
    public static final int MONITOREXIT = 0xc3;
    public static final int WIDE = 0xc4;
    public static final int MULTIANEWARRAY = 0xc5;
    public static final int IFNULL = 0xc6;
    public static final int IFNONNULL = 0xc7;
    public static final int GOTO_W = 0xc8;
    public static final int JSR_W = 0xc9;

    // a constant for each valid argument to "newarray"

    public static final int NEWARRAY_BOOLEAN = 4;
    public static final int NEWARRAY_CHAR = 5;
    public static final int NEWARRAY_FLOAT = 6;
    public static final int NEWARRAY_DOUBLE = 7;
    public static final int NEWARRAY_BYTE = 8;
    public static final int NEWARRAY_SHORT = 9;
    public static final int NEWARRAY_INT = 10;
    public static final int NEWARRAY_LONG = 11;

    // a constant for each possible instruction format

    /** invalid */
    public static final int FMT_INVALID = 0;

    /** "-": {@code op} */
    public static final int FMT_NO_ARGS = 1;

    /** "0": {@code op}; implies {@code max_locals >= 1} */
    public static final int FMT_NO_ARGS_LOCALS_1 = 2;

    /** "1": {@code op}; implies {@code max_locals >= 2} */
    public static final int FMT_NO_ARGS_LOCALS_2 = 3;

    /** "2": {@code op}; implies {@code max_locals >= 3} */
    public static final int FMT_NO_ARGS_LOCALS_3 = 4;

    /** "3": {@code op}; implies {@code max_locals >= 4} */
    public static final int FMT_NO_ARGS_LOCALS_4 = 5;

    /** "4": {@code op}; implies {@code max_locals >= 5} */
    public static final int FMT_NO_ARGS_LOCALS_5 = 6;

    /** "b": {@code op target target} */
    public static final int FMT_BRANCH = 7;

    /** "c": {@code op target target target target} */
    public static final int FMT_WIDE_BRANCH = 8;

    /** "p": {@code op #cpi #cpi}; constant restricted as specified */
    public static final int FMT_CPI = 9;

    /**
     * "l": {@code op local}; category-1 local; implies
     * {@code max_locals} is at least two more than the given
     * local number
     */
    public static final int FMT_LOCAL_1 = 10;

    /**
     * "m": {@code op local}; category-2 local; implies
     * {@code max_locals} is at least two more than the given
     * local number
     */
    public static final int FMT_LOCAL_2 = 11;

    /**
     * "y": {@code op #byte} ({@code bipush} and
     * {@code newarray})
     */
    public static final int FMT_LITERAL_BYTE = 12;

    /** "I": {@code invokeinterface cpi cpi count 0} */
    public static final int FMT_INVOKEINTERFACE = 13;

    /** "L": {@code ldc #cpi}; constant restricted as specified */
    public static final int FMT_LDC = 14;

    /** "S": {@code sipush #byte #byte} */
    public static final int FMT_SIPUSH = 15;

    /** "T": {@code tableswitch ...} */
    public static final int FMT_TABLESWITCH = 16;

    /** "U": {@code lookupswitch ...} */
    public static final int FMT_LOOKUPSWITCH = 17;

    /** "M": {@code multianewarray cpi cpi dims} */
    public static final int FMT_MULTIANEWARRAY = 18;

    /** "W": {@code wide ...} */
    public static final int FMT_WIDE = 19;

    /** mask for the bits representing the opcode format */
    public static final int FMT_MASK = 0x1f;

    /** "I": flag bit for valid cp type for {@code Integer} */
    public static final int CPOK_Integer = 0x20;

    /** "F": flag bit for valid cp type for {@code Float} */
    public static final int CPOK_Float = 0x40;

    /** "J": flag bit for valid cp type for {@code Long} */
    public static final int CPOK_Long = 0x80;

    /** "D": flag bit for valid cp type for {@code Double} */
    public static final int CPOK_Double = 0x100;

    /** "c": flag bit for valid cp type for {@code Class} */
    public static final int CPOK_Class = 0x200;

    /** "s": flag bit for valid cp type for {@code String} */
    public static final int CPOK_String = 0x400;

    /** "f": flag bit for valid cp type for {@code Fieldref} */
    public static final int CPOK_Fieldref = 0x800;

    /** "m": flag bit for valid cp type for {@code Methodref} */
    public static final int CPOK_Methodref = 0x1000;

    /** "i": flag bit for valid cp type for {@code InterfaceMethodref} */
    public static final int CPOK_InterfaceMethodref = 0x2000;

    /**
     * {@code non-null;} map from opcodes to format or'ed with allowed constant
     * pool types
     */
    private static final int[] OPCODE_INFO = new int[256];

    /** {@code non-null;} map from opcodes to their names */
    private static final String[] OPCODE_NAMES = new String[256];

    /** {@code non-null;} bigass string describing all the opcodes */
    private static final String OPCODE_DETAILS =
        "00 - nop;" +
        "01 - aconst_null;" +
        "02 - iconst_m1;" +
        "03 - iconst_0;" +
        "04 - iconst_1;" +
        "05 - iconst_2;" +
        "06 - iconst_3;" +
        "07 - iconst_4;" +
        "08 - iconst_5;" +
        "09 - lconst_0;" +
        "0a - lconst_1;" +
        "0b - fconst_0;" +
        "0c - fconst_1;" +
        "0d - fconst_2;" +
        "0e - dconst_0;" +
        "0f - dconst_1;" +
        "10 y bipush;" +
        "11 S sipush;" +
        "12 L:IFcs ldc;" +
        "13 p:IFcs ldc_w;" +
        "14 p:DJ ldc2_w;" +
        "15 l iload;" +
        "16 m lload;" +
        "17 l fload;" +
        "18 m dload;" +
        "19 l aload;" +
        "1a 0 iload_0;" +
        "1b 1 iload_1;" +
        "1c 2 iload_2;" +
        "1d 3 iload_3;" +
        "1e 1 lload_0;" +
        "1f 2 lload_1;" +
        "20 3 lload_2;" +
        "21 4 lload_3;" +
        "22 0 fload_0;" +
        "23 1 fload_1;" +
        "24 2 fload_2;" +
        "25 3 fload_3;" +
        "26 1 dload_0;" +
        "27 2 dload_1;" +
        "28 3 dload_2;" +
        "29 4 dload_3;" +
        "2a 0 aload_0;" +
        "2b 1 aload_1;" +
        "2c 2 aload_2;" +
        "2d 3 aload_3;" +
        "2e - iaload;" +
        "2f - laload;" +
        "30 - faload;" +
        "31 - daload;" +
        "32 - aaload;" +
        "33 - baload;" +
        "34 - caload;" +
        "35 - saload;" +
        "36 - istore;" +
        "37 - lstore;" +
        "38 - fstore;" +
        "39 - dstore;" +
        "3a - astore;" +
        "3b 0 istore_0;" +
        "3c 1 istore_1;" +
        "3d 2 istore_2;" +
        "3e 3 istore_3;" +
        "3f 1 lstore_0;" +
        "40 2 lstore_1;" +
        "41 3 lstore_2;" +
        "42 4 lstore_3;" +
        "43 0 fstore_0;" +
        "44 1 fstore_1;" +
        "45 2 fstore_2;" +
        "46 3 fstore_3;" +
        "47 1 dstore_0;" +
        "48 2 dstore_1;" +
        "49 3 dstore_2;" +
        "4a 4 dstore_3;" +
        "4b 0 astore_0;" +
        "4c 1 astore_1;" +
        "4d 2 astore_2;" +
        "4e 3 astore_3;" +
        "4f - iastore;" +
        "50 - lastore;" +
        "51 - fastore;" +
        "52 - dastore;" +
        "53 - aastore;" +
        "54 - bastore;" +
        "55 - castore;" +
        "56 - sastore;" +
        "57 - pop;" +
        "58 - pop2;" +
        "59 - dup;" +
        "5a - dup_x1;" +
        "5b - dup_x2;" +
        "5c - dup2;" +
        "5d - dup2_x1;" +
        "5e - dup2_x2;" +
        "5f - swap;" +
        "60 - iadd;" +
        "61 - ladd;" +
        "62 - fadd;" +
        "63 - dadd;" +
        "64 - isub;" +
        "65 - lsub;" +
        "66 - fsub;" +
        "67 - dsub;" +
        "68 - imul;" +
        "69 - lmul;" +
        "6a - fmul;" +
        "6b - dmul;" +
        "6c - idiv;" +
        "6d - ldiv;" +
        "6e - fdiv;" +
        "6f - ddiv;" +
        "70 - irem;" +
        "71 - lrem;" +
        "72 - frem;" +
        "73 - drem;" +
        "74 - ineg;" +
        "75 - lneg;" +
        "76 - fneg;" +
        "77 - dneg;" +
        "78 - ishl;" +
        "79 - lshl;" +
        "7a - ishr;" +
        "7b - lshr;" +
        "7c - iushr;" +
        "7d - lushr;" +
        "7e - iand;" +
        "7f - land;" +
        "80 - ior;" +
        "81 - lor;" +
        "82 - ixor;" +
        "83 - lxor;" +
        "84 l iinc;" +
        "85 - i2l;" +
        "86 - i2f;" +
        "87 - i2d;" +
        "88 - l2i;" +
        "89 - l2f;" +
        "8a - l2d;" +
        "8b - f2i;" +
        "8c - f2l;" +
        "8d - f2d;" +
        "8e - d2i;" +
        "8f - d2l;" +
        "90 - d2f;" +
        "91 - i2b;" +
        "92 - i2c;" +
        "93 - i2s;" +
        "94 - lcmp;" +
        "95 - fcmpl;" +
        "96 - fcmpg;" +
        "97 - dcmpl;" +
        "98 - dcmpg;" +
        "99 b ifeq;" +
        "9a b ifne;" +
        "9b b iflt;" +
        "9c b ifge;" +
        "9d b ifgt;" +
        "9e b ifle;" +
        "9f b if_icmpeq;" +
        "a0 b if_icmpne;" +
        "a1 b if_icmplt;" +
        "a2 b if_icmpge;" +
        "a3 b if_icmpgt;" +
        "a4 b if_icmple;" +
        "a5 b if_acmpeq;" +
        "a6 b if_acmpne;" +
        "a7 b goto;" +
        "a8 b jsr;" +
        "a9 l ret;" +
        "aa T tableswitch;" +
        "ab U lookupswitch;" +
        "ac - ireturn;" +
        "ad - lreturn;" +
        "ae - freturn;" +
        "af - dreturn;" +
        "b0 - areturn;" +
        "b1 - return;" +
        "b2 p:f getstatic;" +
        "b3 p:f putstatic;" +
        "b4 p:f getfield;" +
        "b5 p:f putfield;" +
        "b6 p:m invokevirtual;" +
        "b7 p:m invokespecial;" +
        "b8 p:m invokestatic;" +
        "b9 I:i invokeinterface;" +
        "bb p:c new;" +
        "bc y newarray;" +
        "bd p:c anewarray;" +
        "be - arraylength;" +
        "bf - athrow;" +
        "c0 p:c checkcast;" +
        "c1 p:c instanceof;" +
        "c2 - monitorenter;" +
        "c3 - monitorexit;" +
        "c4 W wide;" +
        "c5 M:c multianewarray;" +
        "c6 b ifnull;" +
        "c7 b ifnonnull;" +
        "c8 c goto_w;" +
        "c9 c jsr_w;";

    static {
        // Set up OPCODE_INFO and OPCODE_NAMES.
        String s = OPCODE_DETAILS;
        int len = s.length();

        for (int i = 0; i < len; /*i*/) {
            int idx = (Character.digit(s.charAt(i), 16) << 4) |
                Character.digit(s.charAt(i + 1), 16);
            int info;
            switch (s.charAt(i + 3)) {
                case '-': info = FMT_NO_ARGS; break;
                case '0': info = FMT_NO_ARGS_LOCALS_1; break;
                case '1': info = FMT_NO_ARGS_LOCALS_2; break;
                case '2': info = FMT_NO_ARGS_LOCALS_3; break;
                case '3': info = FMT_NO_ARGS_LOCALS_4; break;
                case '4': info = FMT_NO_ARGS_LOCALS_5; break;
                case 'b': info = FMT_BRANCH; break;
                case 'c': info = FMT_WIDE_BRANCH; break;
                case 'p': info = FMT_CPI; break;
                case 'l': info = FMT_LOCAL_1; break;
                case 'm': info = FMT_LOCAL_2; break;
                case 'y': info = FMT_LITERAL_BYTE; break;
                case 'I': info = FMT_INVOKEINTERFACE; break;
                case 'L': info = FMT_LDC; break;
                case 'S': info = FMT_SIPUSH; break;
                case 'T': info = FMT_TABLESWITCH; break;
                case 'U': info = FMT_LOOKUPSWITCH; break;
                case 'M': info = FMT_MULTIANEWARRAY; break;
                case 'W': info = FMT_WIDE; break;
                default: info = FMT_INVALID; break;
            }

            i += 5;
            if (s.charAt(i - 1) == ':') {
                inner:
                for (;;) {
                    switch (s.charAt(i)) {
                        case 'I': info |= CPOK_Integer; break;
                        case 'F': info |= CPOK_Float; break;
                        case 'J': info |= CPOK_Long; break;
                        case 'D': info |= CPOK_Double; break;
                        case 'c': info |= CPOK_Class; break;
                        case 's': info |= CPOK_String; break;
                        case 'f': info |= CPOK_Fieldref; break;
                        case 'm': info |= CPOK_Methodref; break;
                        case 'i': info |= CPOK_InterfaceMethodref; break;
                        default: break inner;
                    }
                    i++;
                }
                i++;
            }

            int endAt = s.indexOf(';', i);
            OPCODE_INFO[idx] = info;
            OPCODE_NAMES[idx] = s.substring(i, endAt);
            i = endAt + 1;
        }
    }

    /**
     * This class is uninstantiable.
     */
    private ByteOps() {
        // This space intentionally left blank.
    }

    /**
     * Gets the name of the given opcode.
     *
     * @param opcode {@code >= 0, <= 255;} the opcode
     * @return {@code non-null;} its name
     */
    public static String opName(int opcode) {
        String result = OPCODE_NAMES[opcode];

        if (result == null) {
            result = "unused_" + Hex.u1(opcode);
            OPCODE_NAMES[opcode] = result;
        }

        return result;
    }

    /**
     * Gets the format and allowed cp types of the given opcode.
     *
     * @param opcode {@code >= 0, <= 255;} the opcode
     * @return its format and allowed cp types
     */
    public static int opInfo(int opcode) {
        return OPCODE_INFO[opcode];
    }
}

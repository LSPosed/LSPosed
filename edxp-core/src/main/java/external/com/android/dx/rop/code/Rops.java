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

package external.com.android.dx.rop.code;

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstBaseMethodRef;
import external.com.android.dx.rop.cst.CstCallSiteRef;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import external.com.android.dx.rop.type.TypeList;

/**
 * Standard instances of {@link Rop}.
 */
public final class Rops {
    /** {@code nop()} */
    public static final Rop NOP =
        new Rop(RegOps.NOP, Type.VOID, StdTypeList.EMPTY, "nop");

    /** {@code r,x: int :: r = x;} */
    public static final Rop MOVE_INT =
        new Rop(RegOps.MOVE, Type.INT, StdTypeList.INT, "move-int");

    /** {@code r,x: long :: r = x;} */
    public static final Rop MOVE_LONG =
        new Rop(RegOps.MOVE, Type.LONG, StdTypeList.LONG, "move-long");

    /** {@code r,x: float :: r = x;} */
    public static final Rop MOVE_FLOAT =
        new Rop(RegOps.MOVE, Type.FLOAT, StdTypeList.FLOAT, "move-float");

    /** {@code r,x: double :: r = x;} */
    public static final Rop MOVE_DOUBLE =
        new Rop(RegOps.MOVE, Type.DOUBLE, StdTypeList.DOUBLE, "move-double");

    /** {@code r,x: Object :: r = x;} */
    public static final Rop MOVE_OBJECT =
        new Rop(RegOps.MOVE, Type.OBJECT, StdTypeList.OBJECT, "move-object");

    /**
     * {@code r,x: ReturnAddress :: r = x;}
     *
     * Note that this rop-form instruction has no dex-form equivilent and
     * must be removed before the dex conversion.
     */
    public static final Rop MOVE_RETURN_ADDRESS =
        new Rop(RegOps.MOVE, Type.RETURN_ADDRESS,
                StdTypeList.RETURN_ADDRESS, "move-return-address");

    /** {@code r,param(x): int :: r = param(x);} */
    public static final Rop MOVE_PARAM_INT =
        new Rop(RegOps.MOVE_PARAM, Type.INT, StdTypeList.EMPTY,
                "move-param-int");

    /** {@code r,param(x): long :: r = param(x);} */
    public static final Rop MOVE_PARAM_LONG =
        new Rop(RegOps.MOVE_PARAM, Type.LONG, StdTypeList.EMPTY,
                "move-param-long");

    /** {@code r,param(x): float :: r = param(x);} */
    public static final Rop MOVE_PARAM_FLOAT =
        new Rop(RegOps.MOVE_PARAM, Type.FLOAT, StdTypeList.EMPTY,
                "move-param-float");

    /** {@code r,param(x): double :: r = param(x);} */
    public static final Rop MOVE_PARAM_DOUBLE =
        new Rop(RegOps.MOVE_PARAM, Type.DOUBLE, StdTypeList.EMPTY,
                "move-param-double");

    /** {@code r,param(x): Object :: r = param(x);} */
    public static final Rop MOVE_PARAM_OBJECT =
        new Rop(RegOps.MOVE_PARAM, Type.OBJECT, StdTypeList.EMPTY,
                "move-param-object");

    /** {@code r, literal: int :: r = literal;} */
    public static final Rop CONST_INT =
        new Rop(RegOps.CONST, Type.INT, StdTypeList.EMPTY, "const-int");

    /** {@code r, literal: long :: r = literal;} */
    public static final Rop CONST_LONG =
        new Rop(RegOps.CONST, Type.LONG, StdTypeList.EMPTY, "const-long");

    /** {@code r, literal: float :: r = literal;} */
    public static final Rop CONST_FLOAT =
        new Rop(RegOps.CONST, Type.FLOAT, StdTypeList.EMPTY, "const-float");

    /** {@code r, literal: double :: r = literal;} */
    public static final Rop CONST_DOUBLE =
        new Rop(RegOps.CONST, Type.DOUBLE, StdTypeList.EMPTY, "const-double");

    /** {@code r, literal: Object :: r = literal;} */
    public static final Rop CONST_OBJECT =
        new Rop(RegOps.CONST, Type.OBJECT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "const-object");

    /** {@code r, literal: Object :: r = literal;} */
    public static final Rop CONST_OBJECT_NOTHROW =
        new Rop(RegOps.CONST, Type.OBJECT, StdTypeList.EMPTY,
                "const-object-nothrow");

    /** {@code goto label} */
    public static final Rop GOTO =
        new Rop(RegOps.GOTO, Type.VOID, StdTypeList.EMPTY, Rop.BRANCH_GOTO,
                "goto");

    /** {@code x: int :: if (x == 0) goto label} */
    public static final Rop IF_EQZ_INT =
        new Rop(RegOps.IF_EQ, Type.VOID, StdTypeList.INT, Rop.BRANCH_IF,
                "if-eqz-int");

    /** {@code x: int :: if (x != 0) goto label} */
    public static final Rop IF_NEZ_INT =
        new Rop(RegOps.IF_NE, Type.VOID, StdTypeList.INT, Rop.BRANCH_IF,
                "if-nez-int");

    /** {@code x: int :: if (x < 0) goto label} */
    public static final Rop IF_LTZ_INT =
        new Rop(RegOps.IF_LT, Type.VOID, StdTypeList.INT, Rop.BRANCH_IF,
                "if-ltz-int");

    /** {@code x: int :: if (x >= 0) goto label} */
    public static final Rop IF_GEZ_INT =
        new Rop(RegOps.IF_GE, Type.VOID, StdTypeList.INT, Rop.BRANCH_IF,
                "if-gez-int");

    /** {@code x: int :: if (x <= 0) goto label} */
    public static final Rop IF_LEZ_INT =
        new Rop(RegOps.IF_LE, Type.VOID, StdTypeList.INT, Rop.BRANCH_IF,
                "if-lez-int");

    /** {@code x: int :: if (x > 0) goto label} */
    public static final Rop IF_GTZ_INT =
        new Rop(RegOps.IF_GT, Type.VOID, StdTypeList.INT, Rop.BRANCH_IF,
                "if-gtz-int");

    /** {@code x: Object :: if (x == null) goto label} */
    public static final Rop IF_EQZ_OBJECT =
        new Rop(RegOps.IF_EQ, Type.VOID, StdTypeList.OBJECT, Rop.BRANCH_IF,
                "if-eqz-object");

    /** {@code x: Object :: if (x != null) goto label} */
    public static final Rop IF_NEZ_OBJECT =
        new Rop(RegOps.IF_NE, Type.VOID, StdTypeList.OBJECT, Rop.BRANCH_IF,
                "if-nez-object");

    /** {@code x,y: int :: if (x == y) goto label} */
    public static final Rop IF_EQ_INT =
        new Rop(RegOps.IF_EQ, Type.VOID, StdTypeList.INT_INT, Rop.BRANCH_IF,
                "if-eq-int");

    /** {@code x,y: int :: if (x != y) goto label} */
    public static final Rop IF_NE_INT =
        new Rop(RegOps.IF_NE, Type.VOID, StdTypeList.INT_INT, Rop.BRANCH_IF,
                "if-ne-int");

    /** {@code x,y: int :: if (x < y) goto label} */
    public static final Rop IF_LT_INT =
        new Rop(RegOps.IF_LT, Type.VOID, StdTypeList.INT_INT, Rop.BRANCH_IF,
                "if-lt-int");

    /** {@code x,y: int :: if (x >= y) goto label} */
    public static final Rop IF_GE_INT =
        new Rop(RegOps.IF_GE, Type.VOID, StdTypeList.INT_INT, Rop.BRANCH_IF,
                "if-ge-int");

    /** {@code x,y: int :: if (x <= y) goto label} */
    public static final Rop IF_LE_INT =
        new Rop(RegOps.IF_LE, Type.VOID, StdTypeList.INT_INT, Rop.BRANCH_IF,
                "if-le-int");

    /** {@code x,y: int :: if (x > y) goto label} */
    public static final Rop IF_GT_INT =
        new Rop(RegOps.IF_GT, Type.VOID, StdTypeList.INT_INT, Rop.BRANCH_IF,
                "if-gt-int");

    /** {@code x,y: Object :: if (x == y) goto label} */
    public static final Rop IF_EQ_OBJECT =
        new Rop(RegOps.IF_EQ, Type.VOID, StdTypeList.OBJECT_OBJECT,
                Rop.BRANCH_IF, "if-eq-object");

    /** {@code x,y: Object :: if (x != y) goto label} */
    public static final Rop IF_NE_OBJECT =
        new Rop(RegOps.IF_NE, Type.VOID, StdTypeList.OBJECT_OBJECT,
                Rop.BRANCH_IF, "if-ne-object");

    /** {@code x: int :: goto switchtable[x]} */
    public static final Rop SWITCH =
        new Rop(RegOps.SWITCH, Type.VOID, StdTypeList.INT, Rop.BRANCH_SWITCH,
                "switch");

    /** {@code r,x,y: int :: r = x + y;} */
    public static final Rop ADD_INT =
        new Rop(RegOps.ADD, Type.INT, StdTypeList.INT_INT, "add-int");

    /** {@code r,x,y: long :: r = x + y;} */
    public static final Rop ADD_LONG =
        new Rop(RegOps.ADD, Type.LONG, StdTypeList.LONG_LONG, "add-long");

    /** {@code r,x,y: float :: r = x + y;} */
    public static final Rop ADD_FLOAT =
        new Rop(RegOps.ADD, Type.FLOAT, StdTypeList.FLOAT_FLOAT, "add-float");

    /** {@code r,x,y: double :: r = x + y;} */
    public static final Rop ADD_DOUBLE =
        new Rop(RegOps.ADD, Type.DOUBLE, StdTypeList.DOUBLE_DOUBLE,
                Rop.BRANCH_NONE, "add-double");

    /** {@code r,x,y: int :: r = x - y;} */
    public static final Rop SUB_INT =
        new Rop(RegOps.SUB, Type.INT, StdTypeList.INT_INT, "sub-int");

    /** {@code r,x,y: long :: r = x - y;} */
    public static final Rop SUB_LONG =
        new Rop(RegOps.SUB, Type.LONG, StdTypeList.LONG_LONG, "sub-long");

    /** {@code r,x,y: float :: r = x - y;} */
    public static final Rop SUB_FLOAT =
        new Rop(RegOps.SUB, Type.FLOAT, StdTypeList.FLOAT_FLOAT, "sub-float");

    /** {@code r,x,y: double :: r = x - y;} */
    public static final Rop SUB_DOUBLE =
        new Rop(RegOps.SUB, Type.DOUBLE, StdTypeList.DOUBLE_DOUBLE,
                Rop.BRANCH_NONE, "sub-double");

    /** {@code r,x,y: int :: r = x * y;} */
    public static final Rop MUL_INT =
        new Rop(RegOps.MUL, Type.INT, StdTypeList.INT_INT, "mul-int");

    /** {@code r,x,y: long :: r = x * y;} */
    public static final Rop MUL_LONG =
        new Rop(RegOps.MUL, Type.LONG, StdTypeList.LONG_LONG, "mul-long");

    /** {@code r,x,y: float :: r = x * y;} */
    public static final Rop MUL_FLOAT =
        new Rop(RegOps.MUL, Type.FLOAT, StdTypeList.FLOAT_FLOAT, "mul-float");

    /** {@code r,x,y: double :: r = x * y;} */
    public static final Rop MUL_DOUBLE =
        new Rop(RegOps.MUL, Type.DOUBLE, StdTypeList.DOUBLE_DOUBLE,
                Rop.BRANCH_NONE, "mul-double");

    /** {@code r,x,y: int :: r = x / y;} */
    public static final Rop DIV_INT =
        new Rop(RegOps.DIV, Type.INT, StdTypeList.INT_INT,
                Exceptions.LIST_Error_ArithmeticException, "div-int");

    /** {@code r,x,y: long :: r = x / y;} */
    public static final Rop DIV_LONG =
        new Rop(RegOps.DIV, Type.LONG, StdTypeList.LONG_LONG,
                Exceptions.LIST_Error_ArithmeticException, "div-long");

    /** {@code r,x,y: float :: r = x / y;} */
    public static final Rop DIV_FLOAT =
        new Rop(RegOps.DIV, Type.FLOAT, StdTypeList.FLOAT_FLOAT, "div-float");

    /** {@code r,x,y: double :: r = x / y;} */
    public static final Rop DIV_DOUBLE =
        new Rop(RegOps.DIV, Type.DOUBLE, StdTypeList.DOUBLE_DOUBLE,
                "div-double");

    /** {@code r,x,y: int :: r = x % y;} */
    public static final Rop REM_INT =
        new Rop(RegOps.REM, Type.INT, StdTypeList.INT_INT,
                Exceptions.LIST_Error_ArithmeticException, "rem-int");

    /** {@code r,x,y: long :: r = x % y;} */
    public static final Rop REM_LONG =
        new Rop(RegOps.REM, Type.LONG, StdTypeList.LONG_LONG,
                Exceptions.LIST_Error_ArithmeticException, "rem-long");

    /** {@code r,x,y: float :: r = x % y;} */
    public static final Rop REM_FLOAT =
        new Rop(RegOps.REM, Type.FLOAT, StdTypeList.FLOAT_FLOAT, "rem-float");

    /** {@code r,x,y: double :: r = x % y;} */
    public static final Rop REM_DOUBLE =
        new Rop(RegOps.REM, Type.DOUBLE, StdTypeList.DOUBLE_DOUBLE,
                "rem-double");

    /** {@code r,x: int :: r = -x;} */
    public static final Rop NEG_INT =
        new Rop(RegOps.NEG, Type.INT, StdTypeList.INT, "neg-int");

    /** {@code r,x: long :: r = -x;} */
    public static final Rop NEG_LONG =
        new Rop(RegOps.NEG, Type.LONG, StdTypeList.LONG, "neg-long");

    /** {@code r,x: float :: r = -x;} */
    public static final Rop NEG_FLOAT =
        new Rop(RegOps.NEG, Type.FLOAT, StdTypeList.FLOAT, "neg-float");

    /** {@code r,x: double :: r = -x;} */
    public static final Rop NEG_DOUBLE =
        new Rop(RegOps.NEG, Type.DOUBLE, StdTypeList.DOUBLE, "neg-double");

    /** {@code r,x,y: int :: r = x & y;} */
    public static final Rop AND_INT =
        new Rop(RegOps.AND, Type.INT, StdTypeList.INT_INT, "and-int");

    /** {@code r,x,y: long :: r = x & y;} */
    public static final Rop AND_LONG =
        new Rop(RegOps.AND, Type.LONG, StdTypeList.LONG_LONG, "and-long");

    /** {@code r,x,y: int :: r = x | y;} */
    public static final Rop OR_INT =
        new Rop(RegOps.OR, Type.INT, StdTypeList.INT_INT, "or-int");

    /** {@code r,x,y: long :: r = x | y;} */
    public static final Rop OR_LONG =
        new Rop(RegOps.OR, Type.LONG, StdTypeList.LONG_LONG, "or-long");

    /** {@code r,x,y: int :: r = x ^ y;} */
    public static final Rop XOR_INT =
        new Rop(RegOps.XOR, Type.INT, StdTypeList.INT_INT, "xor-int");

    /** {@code r,x,y: long :: r = x ^ y;} */
    public static final Rop XOR_LONG =
        new Rop(RegOps.XOR, Type.LONG, StdTypeList.LONG_LONG, "xor-long");

    /** {@code r,x,y: int :: r = x << y;} */
    public static final Rop SHL_INT =
        new Rop(RegOps.SHL, Type.INT, StdTypeList.INT_INT, "shl-int");

    /** {@code r,x: long; y: int :: r = x << y;} */
    public static final Rop SHL_LONG =
        new Rop(RegOps.SHL, Type.LONG, StdTypeList.LONG_INT, "shl-long");

    /** {@code r,x,y: int :: r = x >> y;} */
    public static final Rop SHR_INT =
        new Rop(RegOps.SHR, Type.INT, StdTypeList.INT_INT, "shr-int");

    /** {@code r,x: long; y: int :: r = x >> y;} */
    public static final Rop SHR_LONG =
        new Rop(RegOps.SHR, Type.LONG, StdTypeList.LONG_INT, "shr-long");

    /** {@code r,x,y: int :: r = x >>> y;} */
    public static final Rop USHR_INT =
        new Rop(RegOps.USHR, Type.INT, StdTypeList.INT_INT, "ushr-int");

    /** {@code r,x: long; y: int :: r = x >>> y;} */
    public static final Rop USHR_LONG =
        new Rop(RegOps.USHR, Type.LONG, StdTypeList.LONG_INT, "ushr-long");

    /** {@code r,x: int :: r = ~x;} */
    public static final Rop NOT_INT =
        new Rop(RegOps.NOT, Type.INT, StdTypeList.INT, "not-int");

    /** {@code r,x: long :: r = ~x;} */
    public static final Rop NOT_LONG =
        new Rop(RegOps.NOT, Type.LONG, StdTypeList.LONG, "not-long");

    /** {@code r,x,c: int :: r = x + c;} */
    public static final Rop ADD_CONST_INT =
        new Rop(RegOps.ADD, Type.INT, StdTypeList.INT, "add-const-int");

    /** {@code r,x,c: long :: r = x + c;} */
    public static final Rop ADD_CONST_LONG =
        new Rop(RegOps.ADD, Type.LONG, StdTypeList.LONG, "add-const-long");

    /** {@code r,x,c: float :: r = x + c;} */
    public static final Rop ADD_CONST_FLOAT =
        new Rop(RegOps.ADD, Type.FLOAT, StdTypeList.FLOAT, "add-const-float");

    /** {@code r,x,c: double :: r = x + c;} */
    public static final Rop ADD_CONST_DOUBLE =
        new Rop(RegOps.ADD, Type.DOUBLE, StdTypeList.DOUBLE,
                "add-const-double");

    /** {@code r,x,c: int :: r = x - c;} */
    public static final Rop SUB_CONST_INT =
        new Rop(RegOps.SUB, Type.INT, StdTypeList.INT, "sub-const-int");

    /** {@code r,x,c: long :: r = x - c;} */
    public static final Rop SUB_CONST_LONG =
        new Rop(RegOps.SUB, Type.LONG, StdTypeList.LONG, "sub-const-long");

    /** {@code r,x,c: float :: r = x - c;} */
    public static final Rop SUB_CONST_FLOAT =
        new Rop(RegOps.SUB, Type.FLOAT, StdTypeList.FLOAT, "sub-const-float");

    /** {@code r,x,c: double :: r = x - c;} */
    public static final Rop SUB_CONST_DOUBLE =
        new Rop(RegOps.SUB, Type.DOUBLE, StdTypeList.DOUBLE,
                "sub-const-double");

    /** {@code r,x,c: int :: r = x * c;} */
    public static final Rop MUL_CONST_INT =
        new Rop(RegOps.MUL, Type.INT, StdTypeList.INT, "mul-const-int");

    /** {@code r,x,c: long :: r = x * c;} */
    public static final Rop MUL_CONST_LONG =
        new Rop(RegOps.MUL, Type.LONG, StdTypeList.LONG, "mul-const-long");

    /** {@code r,x,c: float :: r = x * c;} */
    public static final Rop MUL_CONST_FLOAT =
        new Rop(RegOps.MUL, Type.FLOAT, StdTypeList.FLOAT, "mul-const-float");

    /** {@code r,x,c: double :: r = x * c;} */
    public static final Rop MUL_CONST_DOUBLE =
        new Rop(RegOps.MUL, Type.DOUBLE, StdTypeList.DOUBLE,
                "mul-const-double");

    /** {@code r,x,c: int :: r = x / c;} */
    public static final Rop DIV_CONST_INT =
        new Rop(RegOps.DIV, Type.INT, StdTypeList.INT,
                Exceptions.LIST_Error_ArithmeticException, "div-const-int");

    /** {@code r,x,c: long :: r = x / c;} */
    public static final Rop DIV_CONST_LONG =
        new Rop(RegOps.DIV, Type.LONG, StdTypeList.LONG,
                Exceptions.LIST_Error_ArithmeticException, "div-const-long");

    /** {@code r,x,c: float :: r = x / c;} */
    public static final Rop DIV_CONST_FLOAT =
        new Rop(RegOps.DIV, Type.FLOAT, StdTypeList.FLOAT, "div-const-float");

    /** {@code r,x,c: double :: r = x / c;} */
    public static final Rop DIV_CONST_DOUBLE =
        new Rop(RegOps.DIV, Type.DOUBLE, StdTypeList.DOUBLE,
                "div-const-double");

    /** {@code r,x,c: int :: r = x % c;} */
    public static final Rop REM_CONST_INT =
        new Rop(RegOps.REM, Type.INT, StdTypeList.INT,
                Exceptions.LIST_Error_ArithmeticException, "rem-const-int");

    /** {@code r,x,c: long :: r = x % c;} */
    public static final Rop REM_CONST_LONG =
        new Rop(RegOps.REM, Type.LONG, StdTypeList.LONG,
                Exceptions.LIST_Error_ArithmeticException, "rem-const-long");

    /** {@code r,x,c: float :: r = x % c;} */
    public static final Rop REM_CONST_FLOAT =
        new Rop(RegOps.REM, Type.FLOAT, StdTypeList.FLOAT, "rem-const-float");

    /** {@code r,x,c: double :: r = x % c;} */
    public static final Rop REM_CONST_DOUBLE =
        new Rop(RegOps.REM, Type.DOUBLE, StdTypeList.DOUBLE,
                "rem-const-double");

    /** {@code r,x,c: int :: r = x & c;} */
    public static final Rop AND_CONST_INT =
        new Rop(RegOps.AND, Type.INT, StdTypeList.INT, "and-const-int");

    /** {@code r,x,c: long :: r = x & c;} */
    public static final Rop AND_CONST_LONG =
        new Rop(RegOps.AND, Type.LONG, StdTypeList.LONG, "and-const-long");

    /** {@code r,x,c: int :: r = x | c;} */
    public static final Rop OR_CONST_INT =
        new Rop(RegOps.OR, Type.INT, StdTypeList.INT, "or-const-int");

    /** {@code r,x,c: long :: r = x | c;} */
    public static final Rop OR_CONST_LONG =
        new Rop(RegOps.OR, Type.LONG, StdTypeList.LONG, "or-const-long");

    /** {@code r,x,c: int :: r = x ^ c;} */
    public static final Rop XOR_CONST_INT =
        new Rop(RegOps.XOR, Type.INT, StdTypeList.INT, "xor-const-int");

    /** {@code r,x,c: long :: r = x ^ c;} */
    public static final Rop XOR_CONST_LONG =
        new Rop(RegOps.XOR, Type.LONG, StdTypeList.LONG, "xor-const-long");

    /** {@code r,x,c: int :: r = x << c;} */
    public static final Rop SHL_CONST_INT =
        new Rop(RegOps.SHL, Type.INT, StdTypeList.INT, "shl-const-int");

    /** {@code r,x: long; c: int :: r = x << c;} */
    public static final Rop SHL_CONST_LONG =
        new Rop(RegOps.SHL, Type.LONG, StdTypeList.INT, "shl-const-long");

    /** {@code r,x,c: int :: r = x >> c;} */
    public static final Rop SHR_CONST_INT =
        new Rop(RegOps.SHR, Type.INT, StdTypeList.INT, "shr-const-int");

    /** {@code r,x: long; c: int :: r = x >> c;} */
    public static final Rop SHR_CONST_LONG =
        new Rop(RegOps.SHR, Type.LONG, StdTypeList.INT, "shr-const-long");

    /** {@code r,x,c: int :: r = x >>> c;} */
    public static final Rop USHR_CONST_INT =
        new Rop(RegOps.USHR, Type.INT, StdTypeList.INT, "ushr-const-int");

    /** {@code r,x: long; c: int :: r = x >>> c;} */
    public static final Rop USHR_CONST_LONG =
        new Rop(RegOps.USHR, Type.LONG, StdTypeList.INT, "ushr-const-long");

    /** {@code r: int; x,y: long :: r = cmp(x, y);} */
    public static final Rop CMPL_LONG =
        new Rop(RegOps.CMPL, Type.INT, StdTypeList.LONG_LONG, "cmpl-long");

    /** {@code r: int; x,y: float :: r = cmpl(x, y);} */
    public static final Rop CMPL_FLOAT =
        new Rop(RegOps.CMPL, Type.INT, StdTypeList.FLOAT_FLOAT, "cmpl-float");

    /** {@code r: int; x,y: double :: r = cmpl(x, y);} */
    public static final Rop CMPL_DOUBLE =
        new Rop(RegOps.CMPL, Type.INT, StdTypeList.DOUBLE_DOUBLE,
                "cmpl-double");

    /** {@code r: int; x,y: float :: r = cmpg(x, y);} */
    public static final Rop CMPG_FLOAT =
        new Rop(RegOps.CMPG, Type.INT, StdTypeList.FLOAT_FLOAT, "cmpg-float");

    /** {@code r: int; x,y: double :: r = cmpg(x, y);} */
    public static final Rop CMPG_DOUBLE =
        new Rop(RegOps.CMPG, Type.INT, StdTypeList.DOUBLE_DOUBLE,
                "cmpg-double");

    /** {@code r: int; x: long :: r = (int) x} */
    public static final Rop CONV_L2I =
        new Rop(RegOps.CONV, Type.INT, StdTypeList.LONG, "conv-l2i");

    /** {@code r: int; x: float :: r = (int) x} */
    public static final Rop CONV_F2I =
        new Rop(RegOps.CONV, Type.INT, StdTypeList.FLOAT, "conv-f2i");

    /** {@code r: int; x: double :: r = (int) x} */
    public static final Rop CONV_D2I =
        new Rop(RegOps.CONV, Type.INT, StdTypeList.DOUBLE, "conv-d2i");

    /** {@code r: long; x: int :: r = (long) x} */
    public static final Rop CONV_I2L =
        new Rop(RegOps.CONV, Type.LONG, StdTypeList.INT, "conv-i2l");

    /** {@code r: long; x: float :: r = (long) x} */
    public static final Rop CONV_F2L =
        new Rop(RegOps.CONV, Type.LONG, StdTypeList.FLOAT, "conv-f2l");

    /** {@code r: long; x: double :: r = (long) x} */
    public static final Rop CONV_D2L =
        new Rop(RegOps.CONV, Type.LONG, StdTypeList.DOUBLE, "conv-d2l");

    /** {@code r: float; x: int :: r = (float) x} */
    public static final Rop CONV_I2F =
        new Rop(RegOps.CONV, Type.FLOAT, StdTypeList.INT, "conv-i2f");

    /** {@code r: float; x: long :: r = (float) x} */
    public static final Rop CONV_L2F =
        new Rop(RegOps.CONV, Type.FLOAT, StdTypeList.LONG, "conv-l2f");

    /** {@code r: float; x: double :: r = (float) x} */
    public static final Rop CONV_D2F =
        new Rop(RegOps.CONV, Type.FLOAT, StdTypeList.DOUBLE, "conv-d2f");

    /** {@code r: double; x: int :: r = (double) x} */
    public static final Rop CONV_I2D =
        new Rop(RegOps.CONV, Type.DOUBLE, StdTypeList.INT, "conv-i2d");

    /** {@code r: double; x: long :: r = (double) x} */
    public static final Rop CONV_L2D =
        new Rop(RegOps.CONV, Type.DOUBLE, StdTypeList.LONG, "conv-l2d");

    /** {@code r: double; x: float :: r = (double) x} */
    public static final Rop CONV_F2D =
        new Rop(RegOps.CONV, Type.DOUBLE, StdTypeList.FLOAT, "conv-f2d");

    /**
     * {@code r,x: int :: r = (x << 24) >> 24} (Java-style
     * convert int to byte)
     */
    public static final Rop TO_BYTE =
        new Rop(RegOps.TO_BYTE, Type.INT, StdTypeList.INT, "to-byte");

    /**
     * {@code r,x: int :: r = x & 0xffff} (Java-style
     * convert int to char)
     */
    public static final Rop TO_CHAR =
        new Rop(RegOps.TO_CHAR, Type.INT, StdTypeList.INT, "to-char");

    /**
     * {@code r,x: int :: r = (x << 16) >> 16} (Java-style
     * convert int to short)
     */
    public static final Rop TO_SHORT =
        new Rop(RegOps.TO_SHORT, Type.INT, StdTypeList.INT, "to-short");

    /** {@code return void} */
    public static final Rop RETURN_VOID =
        new Rop(RegOps.RETURN, Type.VOID, StdTypeList.EMPTY, Rop.BRANCH_RETURN,
                "return-void");

    /** {@code x: int; return x} */
    public static final Rop RETURN_INT =
        new Rop(RegOps.RETURN, Type.VOID, StdTypeList.INT, Rop.BRANCH_RETURN,
                "return-int");

    /** {@code x: long; return x} */
    public static final Rop RETURN_LONG =
        new Rop(RegOps.RETURN, Type.VOID, StdTypeList.LONG, Rop.BRANCH_RETURN,
                "return-long");

    /** {@code x: float; return x} */
    public static final Rop RETURN_FLOAT =
        new Rop(RegOps.RETURN, Type.VOID, StdTypeList.FLOAT, Rop.BRANCH_RETURN,
                "return-float");

    /** {@code x: double; return x} */
    public static final Rop RETURN_DOUBLE =
        new Rop(RegOps.RETURN, Type.VOID, StdTypeList.DOUBLE,
                Rop.BRANCH_RETURN, "return-double");

    /** {@code x: Object; return x} */
    public static final Rop RETURN_OBJECT =
        new Rop(RegOps.RETURN, Type.VOID, StdTypeList.OBJECT,
                Rop.BRANCH_RETURN, "return-object");

    /** {@code T: any type; r: int; x: T[]; :: r = x.length} */
    public static final Rop ARRAY_LENGTH =
        new Rop(RegOps.ARRAY_LENGTH, Type.INT, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException, "array-length");

    /** {@code x: Throwable :: throw(x)} */
    public static final Rop THROW =
        new Rop(RegOps.THROW, Type.VOID, StdTypeList.THROWABLE,
                StdTypeList.THROWABLE, "throw");

    /** {@code x: Object :: monitorenter(x)} */
    public static final Rop MONITOR_ENTER =
        new Rop(RegOps.MONITOR_ENTER, Type.VOID, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException, "monitor-enter");

    /** {@code x: Object :: monitorexit(x)} */
    public static final Rop MONITOR_EXIT =
        new Rop(RegOps.MONITOR_EXIT, Type.VOID, StdTypeList.OBJECT,
                Exceptions.LIST_Error_Null_IllegalMonitorStateException,
                "monitor-exit");

    /** {@code r,y: int; x: int[] :: r = x[y]} */
    public static final Rop AGET_INT =
        new Rop(RegOps.AGET, Type.INT, StdTypeList.INTARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aget-int");

    /** {@code r: long; x: long[]; y: int :: r = x[y]} */
    public static final Rop AGET_LONG =
        new Rop(RegOps.AGET, Type.LONG, StdTypeList.LONGARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aget-long");

    /** {@code r: float; x: float[]; y: int :: r = x[y]} */
    public static final Rop AGET_FLOAT =
        new Rop(RegOps.AGET, Type.FLOAT, StdTypeList.FLOATARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aget-float");

    /** {@code r: double; x: double[]; y: int :: r = x[y]} */
    public static final Rop AGET_DOUBLE =
        new Rop(RegOps.AGET, Type.DOUBLE, StdTypeList.DOUBLEARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aget-double");

    /** {@code r: Object; x: Object[]; y: int :: r = x[y]} */
    public static final Rop AGET_OBJECT =
        new Rop(RegOps.AGET, Type.OBJECT, StdTypeList.OBJECTARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aget-object");

    /** {@code r: boolean; x: boolean[]; y: int :: r = x[y]} */
    public static final Rop AGET_BOOLEAN =
        new Rop(RegOps.AGET, Type.INT, StdTypeList.BOOLEANARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aget-boolean");

    /** {@code r: byte; x: byte[]; y: int :: r = x[y]} */
    public static final Rop AGET_BYTE =
        new Rop(RegOps.AGET, Type.INT, StdTypeList.BYTEARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds, "aget-byte");

    /** {@code r: char; x: char[]; y: int :: r = x[y]} */
    public static final Rop AGET_CHAR =
        new Rop(RegOps.AGET, Type.INT, StdTypeList.CHARARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds, "aget-char");

    /** {@code r: short; x: short[]; y: int :: r = x[y]} */
    public static final Rop AGET_SHORT =
        new Rop(RegOps.AGET, Type.INT, StdTypeList.SHORTARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aget-short");

    /** {@code x,z: int; y: int[] :: y[z] = x} */
    public static final Rop APUT_INT =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.INT_INTARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds, "aput-int");

    /** {@code x: long; y: long[]; z: int :: y[z] = x} */
    public static final Rop APUT_LONG =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.LONG_LONGARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds, "aput-long");

    /** {@code x: float; y: float[]; z: int :: y[z] = x} */
    public static final Rop APUT_FLOAT =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.FLOAT_FLOATARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aput-float");

    /** {@code x: double; y: double[]; z: int :: y[z] = x} */
    public static final Rop APUT_DOUBLE =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.DOUBLE_DOUBLEARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndexOutOfBounds,
                "aput-double");

    /** {@code x: Object; y: Object[]; z: int :: y[z] = x} */
    public static final Rop APUT_OBJECT =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.OBJECT_OBJECTARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndex_ArrayStore,
                "aput-object");

    /** {@code x: boolean; y: boolean[]; z: int :: y[z] = x} */
    public static final Rop APUT_BOOLEAN =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.INT_BOOLEANARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndex_ArrayStore,
                "aput-boolean");

    /** {@code x: byte; y: byte[]; z: int :: y[z] = x} */
    public static final Rop APUT_BYTE =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.INT_BYTEARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndex_ArrayStore, "aput-byte");

    /** {@code x: char; y: char[]; z: int :: y[z] = x} */
    public static final Rop APUT_CHAR =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.INT_CHARARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndex_ArrayStore, "aput-char");

    /** {@code x: short; y: short[]; z: int :: y[z] = x} */
    public static final Rop APUT_SHORT =
        new Rop(RegOps.APUT, Type.VOID, StdTypeList.INT_SHORTARR_INT,
                Exceptions.LIST_Error_Null_ArrayIndex_ArrayStore,
                "aput-short");

    /**
     * {@code T: any non-array object type :: r =
     * alloc(T)} (allocate heap space for an object)
     */
    public static final Rop NEW_INSTANCE =
        new Rop(RegOps.NEW_INSTANCE, Type.OBJECT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "new-instance");

    /** {@code r: int[]; x: int :: r = new int[x]} */
    public static final Rop NEW_ARRAY_INT =
        new Rop(RegOps.NEW_ARRAY, Type.INT_ARRAY, StdTypeList.INT,
                Exceptions.LIST_Error_NegativeArraySizeException,
                "new-array-int");

    /** {@code r: long[]; x: int :: r = new long[x]} */
    public static final Rop NEW_ARRAY_LONG =
        new Rop(RegOps.NEW_ARRAY, Type.LONG_ARRAY, StdTypeList.INT,
                Exceptions.LIST_Error_NegativeArraySizeException,
                "new-array-long");

    /** {@code r: float[]; x: int :: r = new float[x]} */
    public static final Rop NEW_ARRAY_FLOAT =
        new Rop(RegOps.NEW_ARRAY, Type.FLOAT_ARRAY, StdTypeList.INT,
                Exceptions.LIST_Error_NegativeArraySizeException,
                "new-array-float");

    /** {@code r: double[]; x: int :: r = new double[x]} */
    public static final Rop NEW_ARRAY_DOUBLE =
        new Rop(RegOps.NEW_ARRAY, Type.DOUBLE_ARRAY, StdTypeList.INT,
                Exceptions.LIST_Error_NegativeArraySizeException,
                "new-array-double");

    /** {@code r: boolean[]; x: int :: r = new boolean[x]} */
    public static final Rop NEW_ARRAY_BOOLEAN =
        new Rop(RegOps.NEW_ARRAY, Type.BOOLEAN_ARRAY, StdTypeList.INT,
                Exceptions.LIST_Error_NegativeArraySizeException,
                "new-array-boolean");

    /** {@code r: byte[]; x: int :: r = new byte[x]} */
    public static final Rop NEW_ARRAY_BYTE =
        new Rop(RegOps.NEW_ARRAY, Type.BYTE_ARRAY, StdTypeList.INT,
                Exceptions.LIST_Error_NegativeArraySizeException,
                "new-array-byte");

    /** {@code r: char[]; x: int :: r = new char[x]} */
    public static final Rop NEW_ARRAY_CHAR =
        new Rop(RegOps.NEW_ARRAY, Type.CHAR_ARRAY, StdTypeList.INT,
                Exceptions.LIST_Error_NegativeArraySizeException,
                "new-array-char");

    /** {@code r: short[]; x: int :: r = new short[x]} */
    public static final Rop NEW_ARRAY_SHORT =
        new Rop(RegOps.NEW_ARRAY, Type.SHORT_ARRAY, StdTypeList.INT,
                Exceptions.LIST_Error_NegativeArraySizeException,
                "new-array-short");

    /**
     * {@code T: any non-array object type; x: Object :: (T) x} (can
     * throw {@code ClassCastException})
     */
    public static final Rop CHECK_CAST =
        new Rop(RegOps.CHECK_CAST, Type.VOID, StdTypeList.OBJECT,
                Exceptions.LIST_Error_ClassCastException, "check-cast");

    /**
     * {@code T: any non-array object type; x: Object :: x instanceof
     * T}. Note: This is listed as throwing {@code Error}
     * explicitly because the op <i>can</i> throw, but there are no
     * other predefined exceptions for it.
     */
    public static final Rop INSTANCE_OF =
        new Rop(RegOps.INSTANCE_OF, Type.INT, StdTypeList.OBJECT,
                Exceptions.LIST_Error, "instance-of");

    /**
     * {@code r: int; x: Object; f: instance field spec of
     * type int :: r = x.f}
     */
    public static final Rop GET_FIELD_INT =
        new Rop(RegOps.GET_FIELD, Type.INT, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException, "get-field-int");

    /**
     * {@code r: long; x: Object; f: instance field spec of
     * type long :: r = x.f}
     */
    public static final Rop GET_FIELD_LONG =
        new Rop(RegOps.GET_FIELD, Type.LONG, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException, "get-field-long");

    /**
     * {@code r: float; x: Object; f: instance field spec of
     * type float :: r = x.f}
     */
    public static final Rop GET_FIELD_FLOAT =
        new Rop(RegOps.GET_FIELD, Type.FLOAT, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "get-field-float");

    /**
     * {@code r: double; x: Object; f: instance field spec of
     * type double :: r = x.f}
     */
    public static final Rop GET_FIELD_DOUBLE =
        new Rop(RegOps.GET_FIELD, Type.DOUBLE, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "get-field-double");

    /**
     * {@code r: Object; x: Object; f: instance field spec of
     * type Object :: r = x.f}
     */
    public static final Rop GET_FIELD_OBJECT =
        new Rop(RegOps.GET_FIELD, Type.OBJECT, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "get-field-object");

    /**
     * {@code r: boolean; x: Object; f: instance field spec of
     * type boolean :: r = x.f}
     */
    public static final Rop GET_FIELD_BOOLEAN =
        new Rop(RegOps.GET_FIELD, Type.INT, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "get-field-boolean");

    /**
     * {@code r: byte; x: Object; f: instance field spec of
     * type byte :: r = x.f}
     */
    public static final Rop GET_FIELD_BYTE =
        new Rop(RegOps.GET_FIELD, Type.INT, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "get-field-byte");

    /**
     * {@code r: char; x: Object; f: instance field spec of
     * type char :: r = x.f}
     */
    public static final Rop GET_FIELD_CHAR =
        new Rop(RegOps.GET_FIELD, Type.INT, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "get-field-char");

    /**
     * {@code r: short; x: Object; f: instance field spec of
     * type short :: r = x.f}
     */
    public static final Rop GET_FIELD_SHORT =
        new Rop(RegOps.GET_FIELD, Type.INT, StdTypeList.OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "get-field-short");

    /** {@code r: int; f: static field spec of type int :: r = f} */
    public static final Rop GET_STATIC_INT =
        new Rop(RegOps.GET_STATIC, Type.INT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-static-int");

    /** {@code r: long; f: static field spec of type long :: r = f} */
    public static final Rop GET_STATIC_LONG =
        new Rop(RegOps.GET_STATIC, Type.LONG, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-static-long");

    /** {@code r: float; f: static field spec of type float :: r = f} */
    public static final Rop GET_STATIC_FLOAT =
        new Rop(RegOps.GET_STATIC, Type.FLOAT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-static-float");

    /** {@code r: double; f: static field spec of type double :: r = f} */
    public static final Rop GET_STATIC_DOUBLE =
        new Rop(RegOps.GET_STATIC, Type.DOUBLE, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-static-double");

    /** {@code r: Object; f: static field spec of type Object :: r = f} */
    public static final Rop GET_STATIC_OBJECT =
        new Rop(RegOps.GET_STATIC, Type.OBJECT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-static-object");

    /** {@code r: boolean; f: static field spec of type boolean :: r = f} */
    public static final Rop GET_STATIC_BOOLEAN =
        new Rop(RegOps.GET_STATIC, Type.INT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-field-boolean");

    /** {@code r: byte; f: static field spec of type byte :: r = f} */
    public static final Rop GET_STATIC_BYTE =
        new Rop(RegOps.GET_STATIC, Type.INT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-field-byte");

    /** {@code r: char; f: static field spec of type char :: r = f} */
    public static final Rop GET_STATIC_CHAR =
        new Rop(RegOps.GET_STATIC, Type.INT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-field-char");

    /** {@code r: short; f: static field spec of type short :: r = f} */
    public static final Rop GET_STATIC_SHORT =
        new Rop(RegOps.GET_STATIC, Type.INT, StdTypeList.EMPTY,
                Exceptions.LIST_Error, "get-field-short");

    /**
     * {@code x: int; y: Object; f: instance field spec of type
     * int :: y.f = x}
     */
    public static final Rop PUT_FIELD_INT =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.INT_OBJECT,
                Exceptions.LIST_Error_NullPointerException, "put-field-int");

    /**
     * {@code x: long; y: Object; f: instance field spec of type
     * long :: y.f = x}
     */
    public static final Rop PUT_FIELD_LONG =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.LONG_OBJECT,
                Exceptions.LIST_Error_NullPointerException, "put-field-long");

    /**
     * {@code x: float; y: Object; f: instance field spec of type
     * float :: y.f = x}
     */
    public static final Rop PUT_FIELD_FLOAT =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.FLOAT_OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "put-field-float");

    /**
     * {@code x: double; y: Object; f: instance field spec of type
     * double :: y.f = x}
     */
    public static final Rop PUT_FIELD_DOUBLE =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.DOUBLE_OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "put-field-double");

    /**
     * {@code x: Object; y: Object; f: instance field spec of type
     * Object :: y.f = x}
     */
    public static final Rop PUT_FIELD_OBJECT =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.OBJECT_OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "put-field-object");

    /**
     * {@code x: int; y: Object; f: instance field spec of type
     * boolean :: y.f = x}
     */
    public static final Rop PUT_FIELD_BOOLEAN =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.INT_OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "put-field-boolean");

    /**
     * {@code x: int; y: Object; f: instance field spec of type
     * byte :: y.f = x}
     */
    public static final Rop PUT_FIELD_BYTE =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.INT_OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "put-field-byte");

    /**
     * {@code x: int; y: Object; f: instance field spec of type
     * char :: y.f = x}
     */
    public static final Rop PUT_FIELD_CHAR =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.INT_OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "put-field-char");

    /**
     * {@code x: int; y: Object; f: instance field spec of type
     * short :: y.f = x}
     */
    public static final Rop PUT_FIELD_SHORT =
        new Rop(RegOps.PUT_FIELD, Type.VOID, StdTypeList.INT_OBJECT,
                Exceptions.LIST_Error_NullPointerException,
                "put-field-short");

    /** {@code f: static field spec of type int; x: int :: f = x} */
    public static final Rop PUT_STATIC_INT =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.INT,
                Exceptions.LIST_Error, "put-static-int");

    /** {@code f: static field spec of type long; x: long :: f = x} */
    public static final Rop PUT_STATIC_LONG =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.LONG,
                Exceptions.LIST_Error, "put-static-long");

    /** {@code f: static field spec of type float; x: float :: f = x} */
    public static final Rop PUT_STATIC_FLOAT =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.FLOAT,
                Exceptions.LIST_Error, "put-static-float");

    /** {@code f: static field spec of type double; x: double :: f = x} */
    public static final Rop PUT_STATIC_DOUBLE =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.DOUBLE,
                Exceptions.LIST_Error, "put-static-double");

    /** {@code f: static field spec of type Object; x: Object :: f = x} */
    public static final Rop PUT_STATIC_OBJECT =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.OBJECT,
                Exceptions.LIST_Error, "put-static-object");

    /**
     * {@code f: static field spec of type boolean; x: boolean :: f =
     * x}
     */
    public static final Rop PUT_STATIC_BOOLEAN =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.INT,
                Exceptions.LIST_Error, "put-static-boolean");

    /** {@code f: static field spec of type byte; x: byte :: f = x} */
    public static final Rop PUT_STATIC_BYTE =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.INT,
                Exceptions.LIST_Error, "put-static-byte");

    /** {@code f: static field spec of type char; x: char :: f = x} */
    public static final Rop PUT_STATIC_CHAR =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.INT,
                Exceptions.LIST_Error, "put-static-char");

    /** {@code f: static field spec of type short; x: short :: f = x} */
    public static final Rop PUT_STATIC_SHORT =
        new Rop(RegOps.PUT_STATIC, Type.VOID, StdTypeList.INT,
                Exceptions.LIST_Error, "put-static-short");

    /** {@code x: Int :: local variable begins in x} */
    public static final Rop MARK_LOCAL_INT =
            new Rop (RegOps.MARK_LOCAL, Type.VOID,
                    StdTypeList.INT, "mark-local-int");

    /** {@code x: Long :: local variable begins in x} */
    public static final Rop MARK_LOCAL_LONG =
            new Rop (RegOps.MARK_LOCAL, Type.VOID,
                    StdTypeList.LONG, "mark-local-long");

    /** {@code x: Float :: local variable begins in x} */
    public static final Rop MARK_LOCAL_FLOAT =
            new Rop (RegOps.MARK_LOCAL, Type.VOID,
                    StdTypeList.FLOAT, "mark-local-float");

    /** {@code x: Double :: local variable begins in x} */
    public static final Rop MARK_LOCAL_DOUBLE =
            new Rop (RegOps.MARK_LOCAL, Type.VOID,
                    StdTypeList.DOUBLE, "mark-local-double");

    /** {@code x: Object :: local variable begins in x} */
    public static final Rop MARK_LOCAL_OBJECT =
            new Rop (RegOps.MARK_LOCAL, Type.VOID,
                    StdTypeList.OBJECT, "mark-local-object");

    /** {@code T: Any primitive type; v0..vx: T :: {v0, ..., vx}} */
    public static final Rop FILL_ARRAY_DATA =
        new Rop(RegOps.FILL_ARRAY_DATA, Type.VOID, StdTypeList.EMPTY,
                "fill-array-data");

    /**
     * Returns the appropriate rop for the given opcode, destination,
     * and sources. The result is typically, but not necessarily, a
     * shared instance.
     *
     * <p><b>Note:</b> This method does not do complete error checking on
     * its arguments, and so it may return an instance which seemed "right
     * enough" even though in actuality the passed arguments don't quite
     * match what is returned. TODO: Revisit this issue.</p>
     *
     * @param opcode the opcode
     * @param dest {@code non-null;} destination (result) type, or
     * {@link Type#VOID} if none
     * @param sources {@code non-null;} list of source types
     * @param cst {@code null-ok;} associated constant, if any
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop ropFor(int opcode, TypeBearer dest, TypeList sources,
            Constant cst) {
        switch (opcode) {
            case RegOps.NOP: return NOP;
            case RegOps.MOVE: return opMove(dest);
            case RegOps.MOVE_PARAM: return opMoveParam(dest);
            case RegOps.MOVE_EXCEPTION: return opMoveException(dest);
            case RegOps.CONST: return opConst(dest);
            case RegOps.GOTO: return GOTO;
            case RegOps.IF_EQ: return opIfEq(sources);
            case RegOps.IF_NE: return opIfNe(sources);
            case RegOps.IF_LT: return opIfLt(sources);
            case RegOps.IF_GE: return opIfGe(sources);
            case RegOps.IF_LE: return opIfLe(sources);
            case RegOps.IF_GT: return opIfGt(sources);
            case RegOps.SWITCH: return SWITCH;
            case RegOps.ADD: return opAdd(sources);
            case RegOps.SUB: return opSub(sources);
            case RegOps.MUL: return opMul(sources);
            case RegOps.DIV: return opDiv(sources);
            case RegOps.REM: return opRem(sources);
            case RegOps.NEG: return opNeg(dest);
            case RegOps.AND: return opAnd(sources);
            case RegOps.OR: return opOr(sources);
            case RegOps.XOR: return opXor(sources);
            case RegOps.SHL: return opShl(sources);
            case RegOps.SHR: return opShr(sources);
            case RegOps.USHR: return opUshr(sources);
            case RegOps.NOT: return opNot(dest);
            case RegOps.CMPL: return opCmpl(sources.getType(0));
            case RegOps.CMPG: return opCmpg(sources.getType(0));
            case RegOps.CONV: return opConv(dest, sources.getType(0));
            case RegOps.TO_BYTE: return TO_BYTE;
            case RegOps.TO_CHAR: return TO_CHAR;
            case RegOps.TO_SHORT: return TO_SHORT;
            case RegOps.RETURN: {
                if (sources.size() == 0) {
                    return RETURN_VOID;
                }
                return opReturn(sources.getType(0));
            }
            case RegOps.ARRAY_LENGTH: return ARRAY_LENGTH;
            case RegOps.THROW: return THROW;
            case RegOps.MONITOR_ENTER: return MONITOR_ENTER;
            case RegOps.MONITOR_EXIT: return MONITOR_EXIT;
            case RegOps.AGET: {
                Type source = sources.getType(0);
                Type componentType;
                if (source == Type.KNOWN_NULL) {
                    /*
                     * Treat a known-null as an array of the expected
                     * result type.
                     */
                    componentType = dest.getType();
                } else {
                    componentType = source.getComponentType();
                }
                return opAget(componentType);
            }
            case RegOps.APUT: {
                Type source = sources.getType(1);
                Type componentType;
                if (source == Type.KNOWN_NULL) {
                    /*
                     * Treat a known-null as an array of the type being
                     * stored.
                     */
                    componentType = sources.getType(0);
                } else {
                    componentType = source.getComponentType();
                }
                return opAput(componentType);
            }
            case RegOps.NEW_INSTANCE: return NEW_INSTANCE;
            case RegOps.NEW_ARRAY: return opNewArray(dest.getType());
            case RegOps.CHECK_CAST: return CHECK_CAST;
            case RegOps.INSTANCE_OF: return INSTANCE_OF;
            case RegOps.GET_FIELD: return opGetField(dest);
            case RegOps.GET_STATIC: return opGetStatic(dest);
            case RegOps.PUT_FIELD: return opPutField(sources.getType(0));
            case RegOps.PUT_STATIC: return opPutStatic(sources.getType(0));
            case RegOps.INVOKE_STATIC: {
                return opInvokeStatic(((CstMethodRef) cst).getPrototype());
            }
            case RegOps.INVOKE_VIRTUAL: {
                CstBaseMethodRef cstMeth = (CstMethodRef) cst;
                Prototype meth = cstMeth.getPrototype();
                CstType definer = cstMeth.getDefiningClass();
                meth = meth.withFirstParameter(definer.getClassType());
                return opInvokeVirtual(meth);
            }
            case RegOps.INVOKE_SUPER: {
                CstBaseMethodRef cstMeth = (CstMethodRef) cst;
                Prototype meth = cstMeth.getPrototype();
                CstType definer = cstMeth.getDefiningClass();
                meth = meth.withFirstParameter(definer.getClassType());
                return opInvokeSuper(meth);
            }
            case RegOps.INVOKE_DIRECT: {
                CstBaseMethodRef cstMeth = (CstMethodRef) cst;
                Prototype meth = cstMeth.getPrototype();
                CstType definer = cstMeth.getDefiningClass();
                meth = meth.withFirstParameter(definer.getClassType());
                return opInvokeDirect(meth);
            }
            case RegOps.INVOKE_INTERFACE: {
                CstBaseMethodRef cstMeth = (CstMethodRef) cst;
                Prototype meth = cstMeth.getPrototype();
                CstType definer = cstMeth.getDefiningClass();
                meth = meth.withFirstParameter(definer.getClassType());
                return opInvokeInterface(meth);
            }
            case RegOps.INVOKE_POLYMORPHIC: {
                CstBaseMethodRef cstMeth = (CstMethodRef) cst;
                Prototype proto = cstMeth.getPrototype();
                CstType definer = cstMeth.getDefiningClass();
                Prototype meth = proto.withFirstParameter(definer.getClassType());
                return opInvokePolymorphic(meth);
            }
            case RegOps.INVOKE_CUSTOM: {
                CstCallSiteRef cstInvokeDynamicRef = (CstCallSiteRef) cst;
                Prototype proto = cstInvokeDynamicRef.getPrototype();
                return opInvokeCustom(proto);
            }
        }

        throw new RuntimeException("unknown opcode " + RegOps.opName(opcode));
    }

    /**
     * Returns the appropriate {@code move} rop for the given type. The
     * result is a shared instance.
     *
     * @param type {@code non-null;} type of value being moved
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opMove(TypeBearer type) {
        switch (type.getBasicFrameType()) {
            case Type.BT_INT:    return MOVE_INT;
            case Type.BT_LONG:   return MOVE_LONG;
            case Type.BT_FLOAT:  return MOVE_FLOAT;
            case Type.BT_DOUBLE: return MOVE_DOUBLE;
            case Type.BT_OBJECT: return MOVE_OBJECT;
            case Type.BT_ADDR:   return MOVE_RETURN_ADDRESS;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code move-param} rop for the
     * given type. The result is a shared instance.
     *
     * @param type {@code non-null;} type of value being moved
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opMoveParam(TypeBearer type) {
        switch (type.getBasicFrameType()) {
            case Type.BT_INT:    return MOVE_PARAM_INT;
            case Type.BT_LONG:   return MOVE_PARAM_LONG;
            case Type.BT_FLOAT:  return MOVE_PARAM_FLOAT;
            case Type.BT_DOUBLE: return MOVE_PARAM_DOUBLE;
            case Type.BT_OBJECT: return MOVE_PARAM_OBJECT;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code move-exception} rop for the
     * given type. The result may be a shared instance.
     *
     * @param type {@code non-null;} type of the exception
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opMoveException(TypeBearer type) {
        return new Rop(RegOps.MOVE_EXCEPTION, type.getType(),
                       StdTypeList.EMPTY, (String) null);
    }

    /**
     * Returns the appropriate {@code move-result} rop for the
     * given type. The result may be a shared instance.
     *
     * @param type {@code non-null;} type of the parameter
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opMoveResult(TypeBearer type) {
        return new Rop(RegOps.MOVE_RESULT, type.getType(),
                       StdTypeList.EMPTY, (String) null);
    }

    /**
     * Returns the appropriate {@code move-result-pseudo} rop for the
     * given type. The result may be a shared instance.
     *
     * @param type {@code non-null;} type of the parameter
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opMoveResultPseudo(TypeBearer type) {
        return new Rop(RegOps.MOVE_RESULT_PSEUDO, type.getType(),
                       StdTypeList.EMPTY, (String) null);
    }

    /**
     * Returns the appropriate {@code const} rop for the given
     * type. The result is a shared instance.
     *
     * @param type {@code non-null;} type of the constant
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opConst(TypeBearer type) {
        if (type.getType() == Type.KNOWN_NULL) {
            return CONST_OBJECT_NOTHROW;
        }

        switch (type.getBasicFrameType()) {
            case Type.BT_INT:    return CONST_INT;
            case Type.BT_LONG:   return CONST_LONG;
            case Type.BT_FLOAT:  return CONST_FLOAT;
            case Type.BT_DOUBLE: return CONST_DOUBLE;
            case Type.BT_OBJECT: return CONST_OBJECT;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code if-eq} rop for the given
     * sources. The result is a shared instance.
     *
     * @param types {@code non-null;} source types
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opIfEq(TypeList types) {
        return pickIf(types, IF_EQZ_INT, IF_EQZ_OBJECT,
                      IF_EQ_INT, IF_EQ_OBJECT);
    }

    /**
     * Returns the appropriate {@code if-ne} rop for the given
     * sources. The result is a shared instance.
     *
     * @param types {@code non-null;} source types
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opIfNe(TypeList types) {
        return pickIf(types, IF_NEZ_INT, IF_NEZ_OBJECT,
                      IF_NE_INT, IF_NE_OBJECT);
    }

    /**
     * Returns the appropriate {@code if-lt} rop for the given
     * sources. The result is a shared instance.
     *
     * @param types {@code non-null;} source types
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opIfLt(TypeList types) {
        return pickIf(types, IF_LTZ_INT, null, IF_LT_INT, null);
    }

    /**
     * Returns the appropriate {@code if-ge} rop for the given
     * sources. The result is a shared instance.
     *
     * @param types {@code non-null;} source types
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opIfGe(TypeList types) {
        return pickIf(types, IF_GEZ_INT, null, IF_GE_INT, null);
    }

    /**
     * Returns the appropriate {@code if-gt} rop for the given
     * sources. The result is a shared instance.
     *
     * @param types {@code non-null;} source types
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opIfGt(TypeList types) {
        return pickIf(types, IF_GTZ_INT, null, IF_GT_INT, null);
    }

    /**
     * Returns the appropriate {@code if-le} rop for the given
     * sources. The result is a shared instance.
     *
     * @param types {@code non-null;} source types
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opIfLe(TypeList types) {
        return pickIf(types, IF_LEZ_INT, null, IF_LE_INT, null);
    }

    /**
     * Helper for all the {@code if*}-related methods, which
     * checks types and picks one of the four variants, throwing if
     * there's a problem.
     *
     * @param types {@code non-null;} the types
     * @param intZ {@code non-null;} the int-to-0 comparison
     * @param objZ {@code null-ok;} the object-to-null comparison
     * @param intInt {@code non-null;} the int-to-int comparison
     * @param objObj {@code non-null;} the object-to-object comparison
     * @return {@code non-null;} the appropriate instance
     */
    private static Rop pickIf(TypeList types, Rop intZ, Rop objZ, Rop intInt,
                              Rop objObj) {
        switch(types.size()) {
            case 1: {
                switch (types.getType(0).getBasicFrameType()) {
                    case Type.BT_INT: {
                        return intZ;
                    }
                    case Type.BT_OBJECT: {
                        if (objZ != null) {
                            return objZ;
                        }
                    }
                }
                break;
            }
            case 2: {
                int bt = types.getType(0).getBasicFrameType();
                if (bt == types.getType(1).getBasicFrameType()) {
                    switch (bt) {
                        case Type.BT_INT: {
                            return intInt;
                        }
                        case Type.BT_OBJECT: {
                            if (objObj != null) {
                                return objObj;
                            }
                        }
                    }
                }
                break;
            }
        }

        return throwBadTypes(types);
    }

    /**
     * Returns the appropriate {@code add} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opAdd(TypeList types) {
        return pickBinaryOp(types, ADD_CONST_INT, ADD_CONST_LONG,
                            ADD_CONST_FLOAT, ADD_CONST_DOUBLE, ADD_INT,
                            ADD_LONG, ADD_FLOAT, ADD_DOUBLE);
    }

    /**
     * Returns the appropriate {@code sub} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opSub(TypeList types) {
        return pickBinaryOp(types, SUB_CONST_INT, SUB_CONST_LONG,
                            SUB_CONST_FLOAT, SUB_CONST_DOUBLE, SUB_INT,
                            SUB_LONG, SUB_FLOAT, SUB_DOUBLE);
    }

    /**
     * Returns the appropriate {@code mul} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opMul(TypeList types) {
        return pickBinaryOp(types, MUL_CONST_INT, MUL_CONST_LONG,
                            MUL_CONST_FLOAT, MUL_CONST_DOUBLE, MUL_INT,
                            MUL_LONG, MUL_FLOAT, MUL_DOUBLE);
    }

    /**
     * Returns the appropriate {@code div} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opDiv(TypeList types) {
        return pickBinaryOp(types, DIV_CONST_INT, DIV_CONST_LONG,
                            DIV_CONST_FLOAT, DIV_CONST_DOUBLE, DIV_INT,
                            DIV_LONG, DIV_FLOAT, DIV_DOUBLE);
    }

    /**
     * Returns the appropriate {@code rem} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opRem(TypeList types) {
        return pickBinaryOp(types, REM_CONST_INT, REM_CONST_LONG,
                            REM_CONST_FLOAT, REM_CONST_DOUBLE, REM_INT,
                            REM_LONG, REM_FLOAT, REM_DOUBLE);
    }

    /**
     * Returns the appropriate {@code and} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opAnd(TypeList types) {
        return pickBinaryOp(types, AND_CONST_INT, AND_CONST_LONG, null, null,
                            AND_INT, AND_LONG, null, null);
    }

    /**
     * Returns the appropriate {@code or} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opOr(TypeList types) {
        return pickBinaryOp(types, OR_CONST_INT, OR_CONST_LONG, null, null,
                            OR_INT, OR_LONG, null, null);
    }

    /**
     * Returns the appropriate {@code xor} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opXor(TypeList types) {
        return pickBinaryOp(types, XOR_CONST_INT, XOR_CONST_LONG, null, null,
                            XOR_INT, XOR_LONG, null, null);
    }

    /**
     * Returns the appropriate {@code shl} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opShl(TypeList types) {
        return pickBinaryOp(types, SHL_CONST_INT, SHL_CONST_LONG, null, null,
                            SHL_INT, SHL_LONG, null, null);
    }

    /**
     * Returns the appropriate {@code shr} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opShr(TypeList types) {
        return pickBinaryOp(types, SHR_CONST_INT, SHR_CONST_LONG, null, null,
                            SHR_INT, SHR_LONG, null, null);
    }

    /**
     * Returns the appropriate {@code ushr} rop for the given
     * types. The result is a shared instance.
     *
     * @param types {@code non-null;} types of the sources
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opUshr(TypeList types) {
        return pickBinaryOp(types, USHR_CONST_INT, USHR_CONST_LONG, null, null,
                            USHR_INT, USHR_LONG, null, null);
    }

    /**
     * Returns the appropriate binary arithmetic rop for the given type
     * and arguments. The result is a shared instance.
     *
     * @param types {@code non-null;} sources of the operation
     * @param int1 {@code non-null;} the int-to-constant rop
     * @param long1 {@code non-null;} the long-to-constant rop
     * @param float1 {@code null-ok;} the float-to-constant rop, if any
     * @param double1 {@code null-ok;} the double-to-constant rop, if any
     * @param int2 {@code non-null;} the int-to-int rop
     * @param long2 {@code non-null;} the long-to-long or long-to-int rop
     * @param float2 {@code null-ok;} the float-to-float rop, if any
     * @param double2 {@code null-ok;} the double-to-double rop, if any
     * @return {@code non-null;} an appropriate instance
     */
    private static Rop pickBinaryOp(TypeList types, Rop int1, Rop long1,
                                    Rop float1, Rop double1, Rop int2,
                                    Rop long2, Rop float2, Rop double2) {
        int bt1 = types.getType(0).getBasicFrameType();
        Rop result = null;

        switch (types.size()) {
            case 1: {
                switch(bt1) {
                    case Type.BT_INT:    return int1;
                    case Type.BT_LONG:   return long1;
                    case Type.BT_FLOAT:  result = float1; break;
                    case Type.BT_DOUBLE: result = double1; break;
                }
                break;
            }
            case 2: {
                switch(bt1) {
                    case Type.BT_INT:    return int2;
                    case Type.BT_LONG:   return long2;
                    case Type.BT_FLOAT:  result = float2; break;
                    case Type.BT_DOUBLE: result = double2; break;
                }
                break;
            }
        }

        if (result == null) {
            return throwBadTypes(types);
        }

        return result;
    }

    /**
     * Returns the appropriate {@code neg} rop for the given type. The
     * result is a shared instance.
     *
     * @param type {@code non-null;} type of value being operated on
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opNeg(TypeBearer type) {
        switch (type.getBasicFrameType()) {
            case Type.BT_INT:    return NEG_INT;
            case Type.BT_LONG:   return NEG_LONG;
            case Type.BT_FLOAT:  return NEG_FLOAT;
            case Type.BT_DOUBLE: return NEG_DOUBLE;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code not} rop for the given type. The
     * result is a shared instance.
     *
     * @param type {@code non-null;} type of value being operated on
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opNot(TypeBearer type) {
        switch (type.getBasicFrameType()) {
            case Type.BT_INT:  return NOT_INT;
            case Type.BT_LONG: return NOT_LONG;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code cmpl} rop for the given type. The
     * result is a shared instance.
     *
     * @param type {@code non-null;} type of value being compared
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opCmpl(TypeBearer type) {
        switch (type.getBasicType()) {
            case Type.BT_LONG:   return CMPL_LONG;
            case Type.BT_FLOAT:  return CMPL_FLOAT;
            case Type.BT_DOUBLE: return CMPL_DOUBLE;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code cmpg} rop for the given type. The
     * result is a shared instance.
     *
     * @param type {@code non-null;} type of value being compared
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opCmpg(TypeBearer type) {
        switch (type.getBasicType()) {
            case Type.BT_FLOAT:  return CMPG_FLOAT;
            case Type.BT_DOUBLE: return CMPG_DOUBLE;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code conv} rop for the given types. The
     * result is a shared instance.
     *
     * @param dest {@code non-null;} target value type
     * @param source {@code non-null;} source value type
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opConv(TypeBearer dest, TypeBearer source) {
        int dbt = dest.getBasicFrameType();
        switch (source.getBasicFrameType()) {
            case Type.BT_INT: {
                switch (dbt) {
                    case Type.BT_LONG:   return CONV_I2L;
                    case Type.BT_FLOAT:  return CONV_I2F;
                    case Type.BT_DOUBLE: return CONV_I2D;
                    default:             break;
                }
            }
            case Type.BT_LONG: {
                switch (dbt) {
                    case Type.BT_INT:    return CONV_L2I;
                    case Type.BT_FLOAT:  return CONV_L2F;
                    case Type.BT_DOUBLE: return CONV_L2D;
                    default:             break;
                }
            }
            case Type.BT_FLOAT: {
                switch (dbt) {
                    case Type.BT_INT:    return CONV_F2I;
                    case Type.BT_LONG:   return CONV_F2L;
                    case Type.BT_DOUBLE: return CONV_F2D;
                    default:             break;
                }
            }
            case Type.BT_DOUBLE: {
                switch (dbt) {
                    case Type.BT_INT:    return CONV_D2I;
                    case Type.BT_LONG:   return CONV_D2L;
                    case Type.BT_FLOAT:  return CONV_D2F;
                    default:             break;
                }
            }
        }

        return throwBadTypes(StdTypeList.make(dest.getType(),
                                              source.getType()));
    }

    /**
     * Returns the appropriate {@code return} rop for the given type. The
     * result is a shared instance.
     *
     * @param type {@code non-null;} type of value being returned
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opReturn(TypeBearer type) {
        switch (type.getBasicFrameType()) {
            case Type.BT_INT:    return RETURN_INT;
            case Type.BT_LONG:   return RETURN_LONG;
            case Type.BT_FLOAT:  return RETURN_FLOAT;
            case Type.BT_DOUBLE: return RETURN_DOUBLE;
            case Type.BT_OBJECT: return RETURN_OBJECT;
            case Type.BT_VOID:   return RETURN_VOID;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code aget} rop for the given type. The
     * result is a shared instance.
     *
     * @param type {@code non-null;} element type of array being accessed
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opAget(TypeBearer type) {
        switch (type.getBasicType()) {
            case Type.BT_INT:     return AGET_INT;
            case Type.BT_LONG:    return AGET_LONG;
            case Type.BT_FLOAT:   return AGET_FLOAT;
            case Type.BT_DOUBLE:  return AGET_DOUBLE;
            case Type.BT_OBJECT:  return AGET_OBJECT;
            case Type.BT_BOOLEAN: return AGET_BOOLEAN;
            case Type.BT_BYTE:    return AGET_BYTE;
            case Type.BT_CHAR:    return AGET_CHAR;
            case Type.BT_SHORT:   return AGET_SHORT;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code aput} rop for the given type. The
     * result is a shared instance.
     *
     * @param type {@code non-null;} element type of array being accessed
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opAput(TypeBearer type) {
        switch (type.getBasicType()) {
            case Type.BT_INT:     return APUT_INT;
            case Type.BT_LONG:    return APUT_LONG;
            case Type.BT_FLOAT:   return APUT_FLOAT;
            case Type.BT_DOUBLE:  return APUT_DOUBLE;
            case Type.BT_OBJECT:  return APUT_OBJECT;
            case Type.BT_BOOLEAN: return APUT_BOOLEAN;
            case Type.BT_BYTE:    return APUT_BYTE;
            case Type.BT_CHAR:    return APUT_CHAR;
            case Type.BT_SHORT:   return APUT_SHORT;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code new-array} rop for the given
     * type. The result is a shared instance.
     *
     * @param arrayType {@code non-null;} array type of array being created
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opNewArray(TypeBearer arrayType) {
        Type type = arrayType.getType();
        Type elementType = type.getComponentType();

        switch (elementType.getBasicType()) {
            case Type.BT_INT:     return NEW_ARRAY_INT;
            case Type.BT_LONG:    return NEW_ARRAY_LONG;
            case Type.BT_FLOAT:   return NEW_ARRAY_FLOAT;
            case Type.BT_DOUBLE:  return NEW_ARRAY_DOUBLE;
            case Type.BT_BOOLEAN: return NEW_ARRAY_BOOLEAN;
            case Type.BT_BYTE:    return NEW_ARRAY_BYTE;
            case Type.BT_CHAR:    return NEW_ARRAY_CHAR;
            case Type.BT_SHORT:   return NEW_ARRAY_SHORT;
            case Type.BT_OBJECT: {
                return new Rop(RegOps.NEW_ARRAY, type, StdTypeList.INT,
                        Exceptions.LIST_Error_NegativeArraySizeException,
                        "new-array-object");
            }
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code filled-new-array} rop for the given
     * type. The result may be a shared instance.
     *
     * @param arrayType {@code non-null;} type of array being created
     * @param count {@code count >= 0;} number of elements that the array should have
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opFilledNewArray(TypeBearer arrayType, int count) {
        Type type = arrayType.getType();
        Type elementType = type.getComponentType();

        if (elementType.isCategory2()) {
            return throwBadType(arrayType);
        }

        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }

        StdTypeList sourceTypes = new StdTypeList(count);

        for (int i = 0; i < count; i++) {
            sourceTypes.set(i, elementType);
        }

        // Note: The resulting rop is considered call-like.
        return new Rop(RegOps.FILLED_NEW_ARRAY,
                       sourceTypes,
                       Exceptions.LIST_Error);
    }

    /**
     * Returns the appropriate {@code get-field} rop for the given
     * type. The result is a shared instance.
     *
     * @param type {@code non-null;} type of the field in question
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opGetField(TypeBearer type) {
        switch (type.getBasicType()) {
            case Type.BT_INT:     return GET_FIELD_INT;
            case Type.BT_LONG:    return GET_FIELD_LONG;
            case Type.BT_FLOAT:   return GET_FIELD_FLOAT;
            case Type.BT_DOUBLE:  return GET_FIELD_DOUBLE;
            case Type.BT_OBJECT:  return GET_FIELD_OBJECT;
            case Type.BT_BOOLEAN: return GET_FIELD_BOOLEAN;
            case Type.BT_BYTE:    return GET_FIELD_BYTE;
            case Type.BT_CHAR:    return GET_FIELD_CHAR;
            case Type.BT_SHORT:   return GET_FIELD_SHORT;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code put-field} rop for the given
     * type. The result is a shared instance.
     *
     * @param type {@code non-null;} type of the field in question
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opPutField(TypeBearer type) {
        switch (type.getBasicType()) {
            case Type.BT_INT:     return PUT_FIELD_INT;
            case Type.BT_LONG:    return PUT_FIELD_LONG;
            case Type.BT_FLOAT:   return PUT_FIELD_FLOAT;
            case Type.BT_DOUBLE:  return PUT_FIELD_DOUBLE;
            case Type.BT_OBJECT:  return PUT_FIELD_OBJECT;
            case Type.BT_BOOLEAN: return PUT_FIELD_BOOLEAN;
            case Type.BT_BYTE:    return PUT_FIELD_BYTE;
            case Type.BT_CHAR:    return PUT_FIELD_CHAR;
            case Type.BT_SHORT:   return PUT_FIELD_SHORT;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code get-static} rop for the given
     * type. The result is a shared instance.
     *
     * @param type {@code non-null;} type of the field in question
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opGetStatic(TypeBearer type) {
        switch (type.getBasicType()) {
            case Type.BT_INT:     return GET_STATIC_INT;
            case Type.BT_LONG:    return GET_STATIC_LONG;
            case Type.BT_FLOAT:   return GET_STATIC_FLOAT;
            case Type.BT_DOUBLE:  return GET_STATIC_DOUBLE;
            case Type.BT_OBJECT:  return GET_STATIC_OBJECT;
            case Type.BT_BOOLEAN: return GET_STATIC_BOOLEAN;
            case Type.BT_BYTE:    return GET_STATIC_BYTE;
            case Type.BT_CHAR:    return GET_STATIC_CHAR;
            case Type.BT_SHORT:   return GET_STATIC_SHORT;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code put-static} rop for the given
     * type. The result is a shared instance.
     *
     * @param type {@code non-null;} type of the field in question
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opPutStatic(TypeBearer type) {
        switch (type.getBasicType()) {
            case Type.BT_INT:     return PUT_STATIC_INT;
            case Type.BT_LONG:    return PUT_STATIC_LONG;
            case Type.BT_FLOAT:   return PUT_STATIC_FLOAT;
            case Type.BT_DOUBLE:  return PUT_STATIC_DOUBLE;
            case Type.BT_OBJECT:  return PUT_STATIC_OBJECT;
            case Type.BT_BOOLEAN: return PUT_STATIC_BOOLEAN;
            case Type.BT_BYTE:    return PUT_STATIC_BYTE;
            case Type.BT_CHAR:    return PUT_STATIC_CHAR;
            case Type.BT_SHORT:   return PUT_STATIC_SHORT;
        }

        return throwBadType(type);
    }

    /**
     * Returns the appropriate {@code invoke-static} rop for the
     * given type. The result is typically a newly-allocated instance.
     *
     * @param meth {@code non-null;} descriptor of the method
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opInvokeStatic(Prototype meth) {
        return new Rop(RegOps.INVOKE_STATIC,
                       meth.getParameterFrameTypes(),
                       StdTypeList.THROWABLE);
    }

    /**
     * Returns the appropriate {@code invoke-virtual} rop for the
     * given type. The result is typically a newly-allocated instance.
     *
     * @param meth {@code non-null;} descriptor of the method, including the
     * {@code this} parameter
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opInvokeVirtual(Prototype meth) {
        return new Rop(RegOps.INVOKE_VIRTUAL,
                       meth.getParameterFrameTypes(),
                       StdTypeList.THROWABLE);
    }

    /**
     * Returns the appropriate {@code invoke-super} rop for the
     * given type. The result is typically a newly-allocated instance.
     *
     * @param meth {@code non-null;} descriptor of the method, including the
     * {@code this} parameter
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opInvokeSuper(Prototype meth) {
        return new Rop(RegOps.INVOKE_SUPER,
                       meth.getParameterFrameTypes(),
                       StdTypeList.THROWABLE);
    }

    /**
     * Returns the appropriate {@code invoke-direct} rop for the
     * given type. The result is typically a newly-allocated instance.
     *
     * @param meth {@code non-null;} descriptor of the method, including the
     * {@code this} parameter
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opInvokeDirect(Prototype meth) {
        return new Rop(RegOps.INVOKE_DIRECT,
                       meth.getParameterFrameTypes(),
                       StdTypeList.THROWABLE);
    }

    /**
     * Returns the appropriate {@code invoke-interface} rop for the
     * given type. The result is typically a newly-allocated instance.
     *
     * @param meth {@code non-null;} descriptor of the method, including the
     * {@code this} parameter
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opInvokeInterface(Prototype meth) {
        return new Rop(RegOps.INVOKE_INTERFACE,
                       meth.getParameterFrameTypes(),
                       StdTypeList.THROWABLE);
    }

    /**
     * Returns the appropriate {@code invoke-polymorphic} rop for the
     * given type. The result is typically a newly-allocated instance.
     *
     * @param meth {@code non-null;} descriptor of the method, including the
     * {@code this} parameter
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opInvokePolymorphic(Prototype meth) {
        return new Rop(RegOps.INVOKE_POLYMORPHIC,
                       meth.getParameterFrameTypes(),
                       StdTypeList.THROWABLE);
    }

    /**
     * Returns the appropriate {@code invoke-dynamic} rop for the
     * given type. The result is typically a newly-allocated instance.
     *
     * @param meth {@code non-null;} descriptor of the method, including the
     * {@code this} parameter
     * @return {@code non-null;} an appropriate instance
     */
    private static Rop opInvokeCustom(Prototype meth) {
        return new Rop(RegOps.INVOKE_CUSTOM,
                       meth.getParameterFrameTypes(),
                       StdTypeList.THROWABLE);
    }

    /**
     * Returns the appropriate {@code mark-local} rop for the given type.
     * The result is a shared instance.
     *
     * @param type {@code non-null;} type of value being marked
     * @return {@code non-null;} an appropriate instance
     */
    public static Rop opMarkLocal(TypeBearer type) {
        switch (type.getBasicFrameType()) {
            case Type.BT_INT:    return MARK_LOCAL_INT;
            case Type.BT_LONG:   return MARK_LOCAL_LONG;
            case Type.BT_FLOAT:  return MARK_LOCAL_FLOAT;
            case Type.BT_DOUBLE: return MARK_LOCAL_DOUBLE;
            case Type.BT_OBJECT: return MARK_LOCAL_OBJECT;
        }

        return throwBadType(type);
    }

    /**
     * This class is uninstantiable.
     */
    private Rops() {
        // This space intentionally left blank.
    }

    /**
     * Throws the right exception to complain about a bogus type.
     *
     * @param type {@code non-null;} the bad type
     * @return never
     */
    private static Rop throwBadType(TypeBearer type) {
        throw new IllegalArgumentException("bad type: " + type);
    }

    /**
     * Throws the right exception to complain about a bogus list of types.
     *
     * @param types {@code non-null;} the bad types
     * @return never
     */
    private static Rop throwBadTypes(TypeList types) {
        throw new IllegalArgumentException("bad types: " + types);
    }
}

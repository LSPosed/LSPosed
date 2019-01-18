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

import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_Class;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_Double;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_Fieldref;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_Float;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_Integer;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_InterfaceMethodref;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_InvokeDynamic;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_Long;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_MethodHandle;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_MethodType;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_Methodref;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_NameAndType;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_String;
import static external.com.android.dx.cf.cst.ConstantTags.CONSTANT_Utf8;
import external.com.android.dx.cf.iface.ParseException;
import external.com.android.dx.cf.iface.ParseObserver;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstDouble;
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.rop.cst.CstFloat;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.cst.CstInterfaceMethodRef;
import external.com.android.dx.rop.cst.CstInvokeDynamic;
import external.com.android.dx.rop.cst.CstLong;
import external.com.android.dx.rop.cst.CstMethodHandle;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstProtoRef;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.cst.StdConstantPool;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;
import java.util.BitSet;

/**
 * Parser for a constant pool embedded in a class file.
 */
public final class ConstantPoolParser {
    /** {@code non-null;} the bytes of the constant pool */
    private final ByteArray bytes;

    /** {@code non-null;} actual parsed constant pool contents */
    private final StdConstantPool pool;

    /** {@code non-null;} byte offsets to each cst */
    private final int[] offsets;

    /**
     * -1 || &gt;= 10; the end offset of this constant pool in the
     * {@code byte[]} which it came from or {@code -1} if not
     * yet parsed
     */
    private int endOffset;

    /** {@code null-ok;} parse observer, if any */
    private ParseObserver observer;

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} the bytes of the file
     */
    public ConstantPoolParser(ByteArray bytes) {
        int size = bytes.getUnsignedShort(8); // constant_pool_count

        this.bytes = bytes;
        this.pool = new StdConstantPool(size);
        this.offsets = new int[size];
        this.endOffset = -1;
    }

    /**
     * Sets the parse observer for this instance.
     *
     * @param observer {@code null-ok;} the observer
     */
    public void setObserver(ParseObserver observer) {
        this.observer = observer;
    }

    /**
     * Gets the end offset of this constant pool in the {@code byte[]}
     * which it came from.
     *
     * @return {@code >= 10;} the end offset
     */
    public int getEndOffset() {
        parseIfNecessary();
        return endOffset;
    }

    /**
     * Gets the actual constant pool.
     *
     * @return {@code non-null;} the constant pool
     */
    public StdConstantPool getPool() {
        parseIfNecessary();
        return pool;
    }

    /**
     * Runs {@link #parse} if it has not yet been run successfully.
     */
    private void parseIfNecessary() {
        if (endOffset < 0) {
            parse();
        }
    }

    /**
     * Does the actual parsing.
     */
    private void parse() {
        determineOffsets();

        if (observer != null) {
            observer.parsed(bytes, 8, 2,
                            "constant_pool_count: " + Hex.u2(offsets.length));
            observer.parsed(bytes, 10, 0, "\nconstant_pool:");
            observer.changeIndent(1);
        }

        /*
         * Track the constant value's original string type. True if constants[i] was
         * a CONSTANT_Utf8, false for any other type including CONSTANT_string.
         */
        BitSet wasUtf8 = new BitSet(offsets.length);

        for (int i = 1; i < offsets.length; i++) {
            int offset = offsets[i];
            if ((offset != 0) && (pool.getOrNull(i) == null)) {
                parse0(i, wasUtf8);
            }
        }

        if (observer != null) {
            for (int i = 1; i < offsets.length; i++) {
                Constant cst = pool.getOrNull(i);
                if (cst == null) {
                    continue;
                }
                int offset = offsets[i];
                int nextOffset = endOffset;
                for (int j = i + 1; j < offsets.length; j++) {
                    int off = offsets[j];
                    if (off != 0) {
                        nextOffset = off;
                        break;
                    }
                }
                String human = wasUtf8.get(i)
                        ? Hex.u2(i) + ": utf8{\"" + cst.toHuman() + "\"}"
                        : Hex.u2(i) + ": " + cst.toString();
                observer.parsed(bytes, offset, nextOffset - offset, human);
            }

            observer.changeIndent(-1);
            observer.parsed(bytes, endOffset, 0, "end constant_pool");
        }
    }

    /**
     * Populates {@link #offsets} and also completely parse utf8 constants.
     */
    private void determineOffsets() {
        int at = 10; // offset from the start of the file to the first cst
        int lastCategory;

        for (int i = 1; i < offsets.length; i += lastCategory) {
            offsets[i] = at;
            int tag = bytes.getUnsignedByte(at);
            try {
                switch (tag) {
                    case CONSTANT_Integer:
                    case CONSTANT_Float:
                    case CONSTANT_Fieldref:
                    case CONSTANT_Methodref:
                    case CONSTANT_InterfaceMethodref:
                    case CONSTANT_NameAndType: {
                        lastCategory = 1;
                        at += 5;
                        break;
                    }
                    case CONSTANT_Long:
                    case CONSTANT_Double: {
                        lastCategory = 2;
                        at += 9;
                        break;
                    }
                    case CONSTANT_Class:
                    case CONSTANT_String: {
                        lastCategory = 1;
                        at += 3;
                        break;
                    }
                    case CONSTANT_Utf8: {
                        lastCategory = 1;
                        at += bytes.getUnsignedShort(at + 1) + 3;
                        break;
                    }
                    case CONSTANT_MethodHandle: {
                        lastCategory = 1;
                        at += 4;
                        break;
                    }
                    case CONSTANT_MethodType: {
                        lastCategory = 1;
                        at += 3;
                        break;
                    }
                    case CONSTANT_InvokeDynamic: {
                        lastCategory = 1;
                        at += 5;
                        break;
                    }
                    default: {
                        throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                    }
                }
            } catch (ParseException ex) {
                ex.addContext("...while preparsing cst " + Hex.u2(i) + " at offset " + Hex.u4(at));
                throw ex;
            }
        }

        endOffset = at;
    }

    /**
     * Parses the constant for the given index if it hasn't already been
     * parsed, also storing it in the constant pool. This will also
     * have the side effect of parsing any entries the indicated one
     * depends on.
     *
     * @param idx which constant
     * @return {@code non-null;} the parsed constant
     */
    private Constant parse0(int idx, BitSet wasUtf8) {
        Constant cst = pool.getOrNull(idx);
        if (cst != null) {
            return cst;
        }

        int at = offsets[idx];

        try {
            int tag = bytes.getUnsignedByte(at);
            switch (tag) {
                case CONSTANT_Utf8: {
                    cst = parseUtf8(at);
                    wasUtf8.set(idx);
                    break;
                }
                case CONSTANT_Integer: {
                    int value = bytes.getInt(at + 1);
                    cst = CstInteger.make(value);
                    break;
                }
                case CONSTANT_Float: {
                    int bits = bytes.getInt(at + 1);
                    cst = CstFloat.make(bits);
                    break;
                }
                case CONSTANT_Long: {
                    long value = bytes.getLong(at + 1);
                    cst = CstLong.make(value);
                    break;
                }
                case CONSTANT_Double: {
                    long bits = bytes.getLong(at + 1);
                    cst = CstDouble.make(bits);
                    break;
                }
                case CONSTANT_Class: {
                    int nameIndex = bytes.getUnsignedShort(at + 1);
                    CstString name = (CstString) parse0(nameIndex, wasUtf8);
                    cst = new CstType(Type.internClassName(name.getString()));
                    break;
                }
                case CONSTANT_String: {
                    int stringIndex = bytes.getUnsignedShort(at + 1);
                    cst = parse0(stringIndex, wasUtf8);
                    break;
                }
                case CONSTANT_Fieldref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstFieldRef(type, nat);
                    break;
                }
                case CONSTANT_Methodref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstMethodRef(type, nat);
                    break;
                }
                case CONSTANT_InterfaceMethodref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstInterfaceMethodRef(type, nat);
                    break;
                }
                case CONSTANT_NameAndType: {
                    int nameIndex = bytes.getUnsignedShort(at + 1);
                    CstString name = (CstString) parse0(nameIndex, wasUtf8);
                    int descriptorIndex = bytes.getUnsignedShort(at + 3);
                    CstString descriptor = (CstString) parse0(descriptorIndex, wasUtf8);
                    cst = new CstNat(name, descriptor);
                    break;
                }
                case CONSTANT_MethodHandle: {
                    final int kind = bytes.getUnsignedByte(at + 1);
                    final int constantIndex = bytes.getUnsignedShort(at + 2);
                    final Constant ref;
                    switch (kind) {
                        case MethodHandleKind.REF_getField:
                        case MethodHandleKind.REF_getStatic:
                        case MethodHandleKind.REF_putField:
                        case MethodHandleKind.REF_putStatic:
                            ref = (CstFieldRef) parse0(constantIndex, wasUtf8);
                            break;
                        case MethodHandleKind.REF_invokeVirtual:
                        case MethodHandleKind.REF_newInvokeSpecial:
                            ref = (CstMethodRef) parse0(constantIndex, wasUtf8);
                            break;
                        case MethodHandleKind.REF_invokeStatic:
                        case MethodHandleKind.REF_invokeSpecial:
                            ref = parse0(constantIndex, wasUtf8);
                            if (!(ref instanceof CstMethodRef
                                || ref instanceof CstInterfaceMethodRef)) {
                              throw new ParseException(
                                  "Unsupported ref constant type for MethodHandle "
                                  + ref.getClass());
                            }
                            break;
                        case MethodHandleKind.REF_invokeInterface:
                            ref = (CstInterfaceMethodRef) parse0(constantIndex, wasUtf8);
                            break;
                        default:
                            throw new ParseException("Unsupported MethodHandle kind: " + kind);
                    }

                    final int methodHandleType = getMethodHandleTypeForKind(kind);
                    cst = CstMethodHandle.make(methodHandleType, ref);
                    break;
                }
                case CONSTANT_MethodType: {
                    int descriptorIndex = bytes.getUnsignedShort(at + 1);
                    CstString descriptor = (CstString) parse0(descriptorIndex, wasUtf8);
                    cst = CstProtoRef.make(descriptor);
                    break;
                }
                case CONSTANT_InvokeDynamic: {
                    int bootstrapMethodIndex = bytes.getUnsignedShort(at + 1);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = CstInvokeDynamic.make(bootstrapMethodIndex, nat);
                    break;
                }
                default: {
                    throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                }
            }
        } catch (ParseException ex) {
            ex.addContext("...while parsing cst " + Hex.u2(idx) +
                          " at offset " + Hex.u4(at));
            throw ex;
        } catch (RuntimeException ex) {
            ParseException pe = new ParseException(ex);
            pe.addContext("...while parsing cst " + Hex.u2(idx) +
                          " at offset " + Hex.u4(at));
            throw pe;
        }

        pool.set(idx, cst);
        return cst;
    }

    /**
     * Parses a utf8 constant.
     *
     * @param at offset to the start of the constant (where the tag byte is)
     * @return {@code non-null;} the parsed value
     */
    private CstString parseUtf8(int at) {
        int length = bytes.getUnsignedShort(at + 1);

        at += 3; // Skip to the data.

        ByteArray ubytes = bytes.slice(at, at + length);

        try {
            return new CstString(ubytes);
        } catch (IllegalArgumentException ex) {
            // Translate the exception
            throw new ParseException(ex);
        }
    }

    private static int getMethodHandleTypeForKind(int kind) {
        switch (kind) {
            case MethodHandleKind.REF_getField:
                return CstMethodHandle.METHOD_HANDLE_TYPE_INSTANCE_GET;
            case MethodHandleKind.REF_getStatic:
                return CstMethodHandle.METHOD_HANDLE_TYPE_STATIC_GET;
            case MethodHandleKind.REF_putField:
                return CstMethodHandle.METHOD_HANDLE_TYPE_INSTANCE_PUT;
            case MethodHandleKind.REF_putStatic:
                return CstMethodHandle.METHOD_HANDLE_TYPE_STATIC_PUT;
            case MethodHandleKind.REF_invokeVirtual:
                return CstMethodHandle.METHOD_HANDLE_TYPE_INVOKE_INSTANCE;
            case MethodHandleKind.REF_invokeStatic:
                return CstMethodHandle.METHOD_HANDLE_TYPE_INVOKE_STATIC;
            case MethodHandleKind.REF_invokeSpecial:
                return CstMethodHandle.METHOD_HANDLE_TYPE_INVOKE_DIRECT;
            case MethodHandleKind.REF_newInvokeSpecial:
                return CstMethodHandle.METHOD_HANDLE_TYPE_INVOKE_CONSTRUCTOR;
            case MethodHandleKind.REF_invokeInterface:
                return CstMethodHandle.METHOD_HANDLE_TYPE_INVOKE_INTERFACE;
        }
        throw new IllegalArgumentException("invalid kind: " + kind);
    }
}

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

import external.com.android.dex.util.ExceptionWithContext;
import external.com.android.dx.io.Opcodes;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstBaseMethodRef;
import external.com.android.dx.rop.cst.CstCallSiteRef;
import external.com.android.dx.rop.cst.CstProtoRef;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.FixedSizeList;
import external.com.android.dx.util.IndentingWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * List of {@link DalvInsn} instances.
 */
public final class DalvInsnList extends FixedSizeList {

    /**
     * The amount of register space, in register units, required for this
     * code block. This may be greater than the largest observed register+
     * category because the method this code block exists in may
     * specify arguments that are unused by the method.
     */
    private final int regCount;

    /**
     * Constructs and returns an immutable instance whose elements are
     * identical to the ones in the given list, in the same order.
     *
     * @param list {@code non-null;} the list to use for elements
     * @param regCount count, in register-units, of the number of registers
     * this code block requires.
     * @return {@code non-null;} an appropriately-constructed instance of this
     * class
     */
    public static DalvInsnList makeImmutable(ArrayList<DalvInsn> list,
            int regCount) {
        int size = list.size();
        DalvInsnList result = new DalvInsnList(size, regCount);

        for (int i = 0; i < size; i++) {
            result.set(i, list.get(i));
        }

        result.setImmutable();
        return result;
    }

    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size the size of the list
     * @param regCount count, in register-units, of the number of registers
     * this code block requires.
     */
    public DalvInsnList(int size, int regCount) {
        super(size);
        this.regCount = regCount;
    }

    /**
     * Gets the element at the given index. It is an error to call
     * this with the index for an element which was never set; if you
     * do that, this will throw {@code NullPointerException}.
     *
     * @param n {@code >= 0, < size();} which index
     * @return {@code non-null;} element at that index
     */
    public DalvInsn get(int n) {
        return (DalvInsn) get0(n);
    }

    /**
     * Sets the instruction at the given index.
     *
     * @param n {@code >= 0, < size();} which index
     * @param insn {@code non-null;} the instruction to set at {@code n}
     */
    public void set(int n, DalvInsn insn) {
        set0(n, insn);
    }

    /**
     * Gets the size of this instance, in 16-bit code units. This will only
     * return a meaningful result if the instructions in this instance all
     * have valid addresses.
     *
     * @return {@code >= 0;} the size
     */
    public int codeSize() {
        int sz = size();

        if (sz == 0) {
            return 0;
        }

        DalvInsn last = get(sz - 1);
        return last.getNextAddress();
    }

    /**
     * Writes all the instructions in this instance to the given output
     * destination.
     *
     * @param out {@code non-null;} where to write to
     */
    public void writeTo(AnnotatedOutput out) {
        int startCursor = out.getCursor();
        int sz = size();

        if (out.annotates()) {
            boolean verbose = out.isVerbose();

            for (int i = 0; i < sz; i++) {
                DalvInsn insn = (DalvInsn) get0(i);
                int codeBytes = insn.codeSize() * 2;
                String s;

                if ((codeBytes != 0) || verbose) {
                    s = insn.listingString("  ", out.getAnnotationWidth(),
                            true);
                } else {
                    s = null;
                }

                if (s != null) {
                    out.annotate(codeBytes, s);
                } else if (codeBytes != 0) {
                    out.annotate(codeBytes, "");
                }
            }
        }

        for (int i = 0; i < sz; i++) {
            DalvInsn insn = (DalvInsn) get0(i);
            try {
                insn.writeTo(out);
            } catch (RuntimeException ex) {
                throw ExceptionWithContext.withContext(ex,
                        "...while writing " + insn);
            }
        }

        // Sanity check of the amount written.
        int written = (out.getCursor() - startCursor) / 2;
        if (written != codeSize()) {
            throw new RuntimeException("write length mismatch; expected " +
                    codeSize() + " but actually wrote " + written);
        }
    }

    /**
     * Gets the minimum required register count implied by this
     * instance.  This includes any unused parameters that could
     * potentially be at the top of the register space.
     * @return {@code >= 0;} the required registers size
     */
    public int getRegistersSize() {
        return regCount;
    }

    /**
     * Gets the size of the outgoing arguments area required by this
     * method. This is equal to the largest argument word count of any
     * method referred to by this instance.
     *
     * @return {@code >= 0;} the required outgoing arguments size
     */
    public int getOutsSize() {
        int sz = size();
        int result = 0;

        for (int i = 0; i < sz; i++) {
            DalvInsn insn = (DalvInsn) get0(i);
            int count = 0;

            if (insn instanceof CstInsn) {
                Constant cst = ((CstInsn) insn).getConstant();
                if (cst instanceof CstBaseMethodRef) {
                    CstBaseMethodRef methodRef = (CstBaseMethodRef) cst;
                    boolean isStatic =
                        (insn.getOpcode().getFamily() == Opcodes.INVOKE_STATIC);
                    count = methodRef.getParameterWordCount(isStatic);
                } else if (cst instanceof CstCallSiteRef) {
                    CstCallSiteRef invokeDynamicRef = (CstCallSiteRef) cst;
                    count = invokeDynamicRef.getPrototype().getParameterTypes().getWordCount();
                }
            } else if (insn instanceof MultiCstInsn) {
                if (insn.getOpcode().getFamily() != Opcodes.INVOKE_POLYMORPHIC) {
                    throw new RuntimeException("Expecting invoke-polymorphic");
                }
                MultiCstInsn mci = (MultiCstInsn) insn;
                // Invoke-polymorphic has two constants: [0] method-ref and
                // [1] call site prototype. The number of arguments is based
                // on the call site prototype since these are the arguments
                // presented. The method-ref is always MethodHandle.invoke(Object[])
                // or MethodHandle.invokeExact(Object[]).
                CstProtoRef proto = (CstProtoRef) mci.getConstant(1);
                count = proto.getPrototype().getParameterTypes().getWordCount();
                count = count + 1; // And one for receiver (method handle).
            } else {
                continue;
            }

            if (count > result) {
                result = count;
            }
        }

        return result;
    }

    /**
     * Does a human-friendly dump of this instance.
     *
     * @param out {@code non-null;} where to dump
     * @param prefix {@code non-null;} prefix to attach to each line of output
     * @param verbose whether to be verbose; verbose output includes
     * lines for zero-size instructions and explicit constant pool indices
     */
    public void debugPrint(Writer out, String prefix, boolean verbose) {
        IndentingWriter iw = new IndentingWriter(out, 0, prefix);
        int sz = size();

        try {
            for (int i = 0; i < sz; i++) {
                DalvInsn insn = (DalvInsn) get0(i);
                String s;

                if ((insn.codeSize() != 0) || verbose) {
                    s = insn.listingString("", 0, verbose);
                } else {
                    s = null;
                }

                if (s != null) {
                    iw.write(s);
                }
            }

            iw.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Does a human-friendly dump of this instance.
     *
     * @param out {@code non-null;} where to dump
     * @param prefix {@code non-null;} prefix to attach to each line of output
     * @param verbose whether to be verbose; verbose output includes
     * lines for zero-size instructions
     */
    public void debugPrint(OutputStream out, String prefix, boolean verbose) {
        Writer w = new OutputStreamWriter(out);
        debugPrint(w, prefix, verbose);

        try {
            w.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

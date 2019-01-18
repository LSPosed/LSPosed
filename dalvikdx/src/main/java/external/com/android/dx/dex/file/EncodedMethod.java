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

package external.com.android.dx.dex.file;

import external.com.android.dex.Leb128;
import external.com.android.dx.dex.code.DalvCode;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.io.PrintWriter;

/**
 * Class that representats a method of a class.
 */
public final class EncodedMethod extends EncodedMember
        implements Comparable<EncodedMethod> {
    /** {@code non-null;} constant for the method */
    private final CstMethodRef method;

    /**
     * {@code null-ok;} code for the method, if the method is neither
     * {@code abstract} nor {@code native}
     */
    private final CodeItem code;

    /**
     * Constructs an instance.
     *
     * @param method {@code non-null;} constant for the method
     * @param accessFlags access flags
     * @param code {@code null-ok;} code for the method, if it is neither
     * {@code abstract} nor {@code native}
     * @param throwsList {@code non-null;} list of possibly-thrown exceptions,
     * just used in generating debugging output (listings)
     */
    public EncodedMethod(CstMethodRef method, int accessFlags,
            DalvCode code, TypeList throwsList) {
        super(accessFlags);

        if (method == null) {
            throw new NullPointerException("method == null");
        }

        this.method = method;

        if (code == null) {
            this.code = null;
        } else {
            boolean isStatic = (accessFlags & AccessFlags.ACC_STATIC) != 0;
            this.code = new CodeItem(method, code, isStatic, throwsList);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (! (other instanceof EncodedMethod)) {
            return false;
        }

        return compareTo((EncodedMethod) other) == 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Note:</b> This compares the method constants only,
     * ignoring any associated code, because it should never be the
     * case that two different items with the same method constant
     * ever appear in the same list (or same file, even).</p>
     */
    @Override
    public int compareTo(EncodedMethod other) {
        return method.compareTo(other.method);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);

        sb.append(getClass().getName());
        sb.append('{');
        sb.append(Hex.u2(getAccessFlags()));
        sb.append(' ');
        sb.append(method);

        if (code != null) {
            sb.append(' ');
            sb.append(code);
        }

        sb.append('}');

        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        MethodIdsSection methodIds = file.getMethodIds();
        MixedItemSection wordData = file.getWordData();

        methodIds.intern(method);

        if (code != null) {
            wordData.add(code);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final String toHuman() {
        return method.toHuman();
    }

    /** {@inheritDoc} */
    @Override
    public final CstString getName() {
        return method.getNat().getName();
    }

    /** {@inheritDoc} */
    @Override
    public void debugPrint(PrintWriter out, boolean verbose) {
        if (code == null) {
            out.println(getRef().toHuman() + ": abstract or native");
        } else {
            code.debugPrint(out, "  ", verbose);
        }
    }

    /**
     * Gets the constant for the method.
     *
     * @return {@code non-null;} the constant
     */
    public final CstMethodRef getRef() {
        return method;
    }

    /** {@inheritDoc} */
    @Override
    public int encode(DexFile file, AnnotatedOutput out,
            int lastIndex, int dumpSeq) {
        int methodIdx = file.getMethodIds().indexOf(method);
        int diff = methodIdx - lastIndex;
        int accessFlags = getAccessFlags();
        int codeOff = OffsettedItem.getAbsoluteOffsetOr0(code);
        boolean hasCode = (codeOff != 0);
        boolean shouldHaveCode = (accessFlags &
                (AccessFlags.ACC_ABSTRACT | AccessFlags.ACC_NATIVE)) == 0;

        /*
         * Verify that code appears if and only if a method is
         * declared to have it.
         */
        if (hasCode != shouldHaveCode) {
            throw new UnsupportedOperationException(
                    "code vs. access_flags mismatch");
        }

        if (out.annotates()) {
            out.annotate(0, String.format("  [%x] %s", dumpSeq,
                            method.toHuman()));
            out.annotate(Leb128.unsignedLeb128Size(diff),
                    "    method_idx:   " + Hex.u4(methodIdx));
            out.annotate(Leb128.unsignedLeb128Size(accessFlags),
                    "    access_flags: " +
                    AccessFlags.methodString(accessFlags));
            out.annotate(Leb128.unsignedLeb128Size(codeOff),
                    "    code_off:     " + Hex.u4(codeOff));
        }

        out.writeUleb128(diff);
        out.writeUleb128(accessFlags);
        out.writeUleb128(codeOff);

        return methodIdx;
    }
}

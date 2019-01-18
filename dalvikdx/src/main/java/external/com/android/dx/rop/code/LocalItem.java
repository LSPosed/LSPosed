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

import external.com.android.dx.rop.cst.CstString;

/**
 * A local variable item: either a name or a signature or both.
 */
public class LocalItem implements Comparable<LocalItem> {
    /** {@code null-ok;} local variable name */
    private final CstString name;

    /** {@code null-ok;} local variable signature */
    private final CstString signature;

    /**
     * Make a new item. If both name and signature are null, null is returned.
     *
     * TODO: intern these
     *
     * @param name {@code null-ok;} local variable name
     * @param signature {@code null-ok;} local variable signature
     * @return {@code non-null;} appropriate instance.
     */
    public static LocalItem make(CstString name, CstString signature) {
        if (name == null && signature == null) {
            return null;
        }

        return new LocalItem (name, signature);
    }

    /**
     * Constructs instance.
     *
     * @param name {@code null-ok;} local variable name
     * @param signature {@code null-ok;} local variable signature
     */
    private LocalItem(CstString name, CstString signature) {
        this.name = name;
        this.signature = signature;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LocalItem)) {
            return false;
        }

        LocalItem local = (LocalItem) other;

        return 0 == compareTo(local);
    }

    /**
     * Compares two strings like String.compareTo(), excepts treats a null
     * as the least-possible string value.
     *
     * @return negative integer, zero, or positive integer in accordance
     * with Comparable.compareTo()
     */
    private static int compareHandlesNulls(CstString a, CstString b) {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        } else {
            return a.compareTo(b);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(LocalItem local) {
        int ret;

        ret = compareHandlesNulls(name, local.name);

        if (ret != 0) {
            return ret;
        }

        ret = compareHandlesNulls(signature, local.signature);

        return ret;
    }


    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (name == null ? 0 : name.hashCode()) * 31
                + (signature == null ? 0 : signature.hashCode());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (name != null && signature == null) {
            return name.toQuoted();
        } else if (name == null && signature == null) {
            return "";
        }

        return "[" + (name == null ? "" : name.toQuoted())
                + "|" + (signature == null ? "" : signature.toQuoted());
    }

    /**
     * Gets name.
     *
     * @return {@code null-ok;} name
     */
    public CstString getName() {
        return name;
    }

    /**
     * Gets signature.
     *
     * @return {@code null-ok;} signature
     */
    public CstString getSignature() {
        return signature;
    }
}

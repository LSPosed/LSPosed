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

import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.ToHuman;
import java.io.PrintWriter;

/**
 * Representation of a member (field or method) of a class, for the
 * purposes of encoding it inside a {@link ClassDataItem}.
 */
public abstract class EncodedMember implements ToHuman {
    /** access flags */
    private final int accessFlags;

    /**
     * Constructs an instance.
     *
     * @param accessFlags access flags for the member
     */
    public EncodedMember(int accessFlags) {
        this.accessFlags = accessFlags;
    }

    /**
     * Gets the access flags.
     *
     * @return the access flags
     */
    public final int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Gets the name.
     *
     * @return {@code non-null;} the name
     */
    public abstract CstString getName();

    /**
     * Does a human-friendly dump of this instance.
     *
     * @param out {@code non-null;} where to dump
     * @param verbose whether to be verbose with the output
     */
    public abstract void debugPrint(PrintWriter out, boolean verbose);

    /**
     * Populates a {@link DexFile} with items from within this instance.
     *
     * @param file {@code non-null;} the file to populate
     */
    public abstract void addContents(DexFile file);

    /**
     * Encodes this instance to the given output.
     *
     * @param file {@code non-null;} file this instance is part of
     * @param out {@code non-null;} where to write to
     * @param lastIndex {@code >= 0;} the previous member index value encoded, or
     * {@code 0} if this is the first element to encode
     * @param dumpSeq {@code >= 0;} sequence number of this instance for
     * annotation purposes
     * @return {@code >= 0;} the member index value that was encoded
     */
    public abstract int encode(DexFile file, AnnotatedOutput out,
            int lastIndex, int dumpSeq);
}

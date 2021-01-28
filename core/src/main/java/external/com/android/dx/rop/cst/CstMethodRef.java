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

package external.com.android.dx.rop.cst;

/**
 * Constants of type {@code CONSTANT_Methodref_info}.
 */
public final class CstMethodRef
        extends CstBaseMethodRef {
    /**
     * Constructs an instance.
     *
     * @param definingClass {@code non-null;} the type of the defining class
     * @param nat {@code non-null;} the name-and-type
     */
    public CstMethodRef(CstType definingClass, CstNat nat) {
        super(definingClass, nat);
    }

    /** {@inheritDoc} */
    @Override
    public String typeName() {
        return "method";
    }
}

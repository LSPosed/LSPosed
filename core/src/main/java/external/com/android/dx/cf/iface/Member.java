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

package external.com.android.dx.cf.iface;

import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;

/**
 * Interface representing members of class files (that is, fields and methods).
 */
public interface Member extends HasAttribute {
    /**
     * Get the defining class.
     *
     * @return {@code non-null;} the defining class
     */
    public CstType getDefiningClass();

    /**
     * Get the field {@code access_flags}.
     *
     * @return the access flags
     */
    public int getAccessFlags();

    /**
     * Get the field {@code name_index} of the member. This is
     * just a convenient shorthand for {@code getNat().getName()}.
     *
     * @return {@code non-null;} the name
     */
    public CstString getName();

    /**
     * Get the field {@code descriptor_index} of the member. This is
     * just a convenient shorthand for {@code getNat().getDescriptor()}.
     *
     * @return {@code non-null;} the descriptor
     */
    public CstString getDescriptor();

    /**
     * Get the name and type associated with this member. This is a
     * combination of the fields {@code name_index} and
     * {@code descriptor_index} in the original classfile, interpreted
     * via the constant pool.
     *
     * @return {@code non-null;} the name and type
     */
    public CstNat getNat();

    /**
     * Get the field {@code attributes} (along with
     * {@code attributes_count}).
     *
     * @return {@code non-null;} the constant pool
     */
    @Override
    public AttributeList getAttributes();
}

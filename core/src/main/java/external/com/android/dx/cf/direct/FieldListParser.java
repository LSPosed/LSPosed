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

package external.com.android.dx.cf.direct;

import external.com.android.dx.cf.iface.AttributeList;
import external.com.android.dx.cf.iface.Member;
import external.com.android.dx.cf.iface.StdField;
import external.com.android.dx.cf.iface.StdFieldList;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstType;

/**
 * Parser for lists of fields in a class file.
 */
final /*package*/ class FieldListParser extends MemberListParser {
    /** {@code non-null;} list in progress */
    private final StdFieldList fields;

    /**
     * Constructs an instance.
     *
     * @param cf {@code non-null;} the class file to parse from
     * @param definer {@code non-null;} class being defined
     * @param offset offset in {@code bytes} to the start of the list
     * @param attributeFactory {@code non-null;} attribute factory to use
     */
    public FieldListParser(DirectClassFile cf, CstType definer, int offset,
            AttributeFactory attributeFactory) {
        super(cf, definer, offset, attributeFactory);
        fields = new StdFieldList(getCount());
    }

    /**
     * Gets the parsed list.
     *
     * @return {@code non-null;} the parsed list
     */
    public StdFieldList getList() {
        parseIfNecessary();
        return fields;
    }

    /** {@inheritDoc} */
    @Override
    protected String humanName() {
        return "field";
    }

    /** {@inheritDoc} */
    @Override
    protected String humanAccessFlags(int accessFlags) {
        return AccessFlags.fieldString(accessFlags);
    }

    /** {@inheritDoc} */
    @Override
    protected int getAttributeContext() {
        return AttributeFactory.CTX_FIELD;
    }

    /** {@inheritDoc} */
    @Override
    protected Member set(int n, int accessFlags, CstNat nat,
                         AttributeList attributes) {
        StdField field =
            new StdField(getDefiner(), accessFlags, nat, attributes);

        fields.set(n, field);
        return field;
    }
}

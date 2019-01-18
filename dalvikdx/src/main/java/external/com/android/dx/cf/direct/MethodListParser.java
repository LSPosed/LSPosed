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
import external.com.android.dx.cf.iface.StdMethod;
import external.com.android.dx.cf.iface.StdMethodList;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstType;

/**
 * Parser for lists of methods in a class file.
 */
final /*package*/ class MethodListParser extends MemberListParser {
    /** {@code non-null;} list in progress */
    final private StdMethodList methods;

    /**
     * Constructs an instance.
     *
     * @param cf {@code non-null;} the class file to parse from
     * @param definer {@code non-null;} class being defined
     * @param offset offset in {@code bytes} to the start of the list
     * @param attributeFactory {@code non-null;} attribute factory to use
     */
    public MethodListParser(DirectClassFile cf, CstType definer,
            int offset, AttributeFactory attributeFactory) {
        super(cf, definer, offset, attributeFactory);
        methods = new StdMethodList(getCount());
    }

    /**
     * Gets the parsed list.
     *
     * @return {@code non-null;} the parsed list
     */
    public StdMethodList getList() {
        parseIfNecessary();
        return methods;
    }

    /** {@inheritDoc} */
    @Override
    protected String humanName() {
        return "method";
    }

    /** {@inheritDoc} */
    @Override
    protected String humanAccessFlags(int accessFlags) {
        return AccessFlags.methodString(accessFlags);
    }

    /** {@inheritDoc} */
    @Override
    protected int getAttributeContext() {
        return AttributeFactory.CTX_METHOD;
    }

    /** {@inheritDoc} */
    @Override
    protected Member set(int n, int accessFlags, CstNat nat,
                         AttributeList attributes) {
        StdMethod meth =
            new StdMethod(getDefiner(), accessFlags, nat, attributes);

        methods.set(n, meth);
        return meth;
    }
}

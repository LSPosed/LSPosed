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

import external.com.android.dx.cf.code.BootstrapMethodsList;
import external.com.android.dx.rop.cst.ConstantPool;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.TypeList;

/**
 * Interface for things which purport to be class files or reasonable
 * facsimiles thereof.
 *
 * <p><b>Note:</b> The fields referred to in this documentation are of the
 * {@code ClassFile} structure defined in vmspec-2 sec4.1.
 */
public interface ClassFile extends HasAttribute {
    /**
     * Gets the field {@code magic}.
     *
     * @return the value in question
     */
    public int getMagic();

    /**
     * Gets the field {@code minor_version}.
     *
     * @return the value in question
     */
    public int getMinorVersion();

    /**
     * Gets the field {@code major_version}.
     *
     * @return the value in question
     */
    public int getMajorVersion();

    /**
     * Gets the field {@code access_flags}.
     *
     * @return the value in question
     */
    public int getAccessFlags();

    /**
     * Gets the field {@code this_class}, interpreted as a type constant.
     *
     * @return {@code non-null;} the value in question
     */
    public CstType getThisClass();

    /**
     * Gets the field {@code super_class}, interpreted as a type constant
     * if non-zero.
     *
     * @return {@code null-ok;} the value in question
     */
    public CstType getSuperclass();

    /**
     * Gets the field {@code constant_pool} (along with
     * {@code constant_pool_count}).
     *
     * @return {@code non-null;} the constant pool
     */
    public ConstantPool getConstantPool();

    /**
     * Gets the field {@code interfaces} (along with
     * {@code interfaces_count}).
     *
     * @return {@code non-null;} the list of interfaces
     */
    public TypeList getInterfaces();

    /**
     * Gets the field {@code fields} (along with
     * {@code fields_count}).
     *
     * @return {@code non-null;} the list of fields
     */
    public FieldList getFields();

    /**
     * Gets the field {@code methods} (along with
     * {@code methods_count}).
     *
     * @return {@code non-null;} the list of fields
     */
    public MethodList getMethods();

    /**
     * Gets the field {@code attributes} (along with
     * {@code attributes_count}).
     *
     * @return {@code non-null;} the list of attributes
     */
    @Override
    public AttributeList getAttributes();

    /**
     * Gets the bootstrap method {@code attributes}.
     * @return {@code non-null;} the list of bootstrap methods
     */
    public BootstrapMethodsList getBootstrapMethods();

    /**
     * Gets the name out of the {@code SourceFile} attribute of this
     * file, if any. This is a convenient shorthand for scrounging around
     * the class's attributes.
     *
     * @return {@code non-null;} the constant pool
     */
    public CstString getSourceFile();
}

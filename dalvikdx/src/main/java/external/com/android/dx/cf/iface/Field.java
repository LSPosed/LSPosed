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

import external.com.android.dx.rop.cst.TypedConstant;

/**
 * Interface representing fields of class files.
 */
public interface Field
        extends Member {
    /**
     * Get the constant value for this field, if any. This only returns
     * non-{@code null} for a {@code static final} field which
     * includes a {@code ConstantValue} attribute.
     *
     * @return {@code null-ok;} the constant value, or {@code null} if this
     * field isn't a constant
     */
    public TypedConstant getConstantValue();
}

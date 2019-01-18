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

import external.com.android.dx.rop.type.TypeBearer;

/**
 * Base class for constants which implement {@link TypeBearer}.
 */
public abstract class TypedConstant
        extends Constant implements TypeBearer {
    /**
     * {@inheritDoc}
     *
     * This implementation always returns {@code this}.
     */
    @Override
    public final TypeBearer getFrameType() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public final int getBasicType() {
        return getType().getBasicType();
    }

    /** {@inheritDoc} */
    @Override
    public final int getBasicFrameType() {
        return getType().getBasicFrameType();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isConstant() {
        return true;
    }
}

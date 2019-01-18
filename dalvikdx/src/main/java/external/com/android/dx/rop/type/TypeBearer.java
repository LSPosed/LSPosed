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

package external.com.android.dx.rop.type;

import external.com.android.dx.util.ToHuman;

/**
 * Object which has an associated type, possibly itself.
 */
public interface TypeBearer
        extends ToHuman {
    /**
     * Gets the type associated with this instance.
     *
     * @return {@code non-null;} the type
     */
    public Type getType();

    /**
     * Gets the frame type corresponding to this type. This method returns
     * {@code this}, except if {@link Type#isIntlike} on the underlying
     * type returns {@code true} but the underlying type is not in
     * fact {@link Type#INT}, in which case this method returns an instance
     * whose underlying type <i>is</i> {@code INT}.
     *
     * @return {@code non-null;} the frame type for this instance
     */
    public TypeBearer getFrameType();

    /**
     * Gets the basic type corresponding to this instance.
     *
     * @return the basic type; one of the {@code BT_*} constants
     * defined by {@link Type}
     */
    public int getBasicType();

    /**
     * Gets the basic type corresponding to this instance's frame type. This
     * is equivalent to {@code getFrameType().getBasicType()}, and
     * is the same as calling {@code getFrameType()} unless this
     * instance is an int-like type, in which case this method returns
     * {@code BT_INT}.
     *
     * @see #getBasicType
     * @see #getFrameType
     *
     * @return the basic frame type; one of the {@code BT_*} constants
     * defined by {@link Type}
     */
    public int getBasicFrameType();

    /**
     * Returns whether this instance represents a constant value.
     *
     * @return {@code true} if this instance represents a constant value
     * and {@code false} if not
     */
    public boolean isConstant();
}

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

package external.com.android.dx.util;

/**
 * Very simple base class that implements a flag to control the mutability
 * of instances. This class just provides the flag and a utility to check
 * and throw the right exception, but it is up to subclasses to place calls
 * to the checker in all the right places.
 */
public class MutabilityControl {
    /** whether this instance is mutable */
    private boolean mutable;

    /**
     * Constructs an instance. It is initially mutable.
     */
    public MutabilityControl() {
        mutable = true;
    }

    /**
     * Constructs an instance, explicitly indicating the mutability.
     *
     * @param mutable {@code true} iff this instance is mutable
     */
    public MutabilityControl(boolean mutable) {
        this.mutable = mutable;
    }

    /**
     * Makes this instance immutable.
     */
    public void setImmutable() {
        mutable = false;
    }

    /**
     * Checks to see whether or not this instance is immutable. This is the
     * same as calling {@code !isMutable()}.
     *
     * @return {@code true} iff this instance is immutable
     */
    public final boolean isImmutable() {
        return !mutable;
    }

    /**
     * Checks to see whether or not this instance is mutable.
     *
     * @return {@code true} iff this instance is mutable
     */
    public final boolean isMutable() {
        return mutable;
    }

    /**
     * Throws {@link MutabilityException} if this instance is
     * immutable.
     */
    public final void throwIfImmutable() {
        if (!mutable) {
            throw new MutabilityException("immutable instance");
        }
    }

    /**
     * Throws {@link MutabilityException} if this instance is mutable.
     */
    public final void throwIfMutable() {
        if (mutable) {
            throw new MutabilityException("mutable instance");
        }
    }
}

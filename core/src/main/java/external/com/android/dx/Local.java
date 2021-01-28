/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx;

import external.com.android.dx.rop.code.RegisterSpec;

/**
 * A temporary variable that holds a single value of a known type.
 */
public final class Local<T> {
    private final Code code;
    final TypeId<T> type;
    private int reg = -1;
    private RegisterSpec spec;

    private Local(Code code, TypeId<T> type) {
        this.code = code;
        this.type = type;
    }

    static <T> Local<T> get(Code code, TypeId<T> type) {
        return new Local<T>(code, type);
    }

    /**
     * Assigns registers to this local.
     *
     * @return the number of registers required.
     */
    int initialize(int nextAvailableRegister) {
        this.reg = nextAvailableRegister;
        this.spec = RegisterSpec.make(nextAvailableRegister, type.ropType);
        return size();
    }

    /**
     * Returns the number of registered required to hold this local.
     */
    int size() {
        return type.ropType.getCategory();
    }

    RegisterSpec spec() {
        if (spec == null) {
            code.initializeLocals();
            if (spec == null) {
                throw new AssertionError();
            }
        }
        return spec;
    }

    public TypeId getType() {
        return type;
    }

    @Override
    public String toString() {
        return "v" + reg + "(" + type + ")";
    }
}

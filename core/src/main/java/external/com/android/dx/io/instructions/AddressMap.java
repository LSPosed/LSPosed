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

package external.com.android.dx.io.instructions;

import java.util.HashMap;

/**
 * Map from addresses to addresses, where addresses are all
 * {@code int}s.
 */
public final class AddressMap {
    /** underlying map. TODO: This might be too inefficient. */
    private final HashMap<Integer,Integer> map;

    /**
     * Constructs an instance.
     */
    public AddressMap() {
        map = new HashMap<Integer,Integer>();
    }

    /**
     * Gets the value address corresponding to the given key address. Returns
     * {@code -1} if there is no mapping.
     */
    public int get(int keyAddress) {
        Integer value = map.get(keyAddress);
        return (value == null) ? -1 : value;
    }

    /**
     * Sets the value address associated with the given key address.
     */
    public void put(int keyAddress, int valueAddress) {
        map.put(keyAddress, valueAddress);
    }
}

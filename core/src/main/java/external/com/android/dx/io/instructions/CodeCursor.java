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

/**
 * Cursor over code units, for reading or writing out Dalvik bytecode.
 */
public interface CodeCursor {
    /**
     * Gets the cursor. The cursor is the offset in code units from
     * the start of the input of the next code unit to be read or
     * written, where the input generally consists of the code for a
     * single method.
     */
    public int cursor();

    /**
     * Gets the base address associated with the current cursor. This
     * differs from the cursor value when explicitly set (by {@link
     * #setBaseAddress}). This is used, in particular, to convey base
     * addresses to switch data payload instructions, whose relative
     * addresses are relative to the address of a dependant switch
     * instruction.
     */
    public int baseAddressForCursor();

    /**
     * Sets the base address for the given target address to be as indicated.
     *
     * @see #baseAddressForCursor
     */
    public void setBaseAddress(int targetAddress, int baseAddress);
}

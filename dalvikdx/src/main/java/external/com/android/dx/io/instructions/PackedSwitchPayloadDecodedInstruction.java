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
 * A decoded Dalvik instruction which contains the payload for
 * a {@code packed-switch} instruction.
 */
public final class PackedSwitchPayloadDecodedInstruction
        extends DecodedInstruction {
    /** first key value */
    private final int firstKey;

    /**
     * array of target addresses. These are absolute, not relative,
     * addresses.
     */
    private final int[] targets;

    /**
     * Constructs an instance.
     */
    public PackedSwitchPayloadDecodedInstruction(InstructionCodec format,
            int opcode, int firstKey, int[] targets) {
        super(format, opcode, 0, null, 0, 0L);

        this.firstKey = firstKey;
        this.targets = targets;
    }

    /** {@inheritDoc} */
    @Override
    public int getRegisterCount() {
        return 0;
    }

    public int getFirstKey() {
        return firstKey;
    }

    public int[] getTargets() {
        return targets;
    }

    /** {@inheritDoc} */
    @Override
    public DecodedInstruction withIndex(int newIndex) {
        throw new UnsupportedOperationException("no index in instruction");
    }
}

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
public final class FillArrayDataPayloadDecodedInstruction
        extends DecodedInstruction {
    /** data array */
    private final Object data;

    /** number of elements */
    private final int size;

    /** element width */
    private final int elementWidth;

    /**
     * Constructs an instance. This private instance doesn't check the
     * type of the data array.
     */
    private FillArrayDataPayloadDecodedInstruction(InstructionCodec format,
            int opcode, Object data, int size, int elementWidth) {
        super(format, opcode, 0, null, 0, 0L);

        this.data = data;
        this.size = size;
        this.elementWidth = elementWidth;
    }

    /**
     * Constructs an instance.
     */
    public FillArrayDataPayloadDecodedInstruction(InstructionCodec format,
            int opcode, byte[] data) {
        this(format, opcode, data, data.length, 1);
    }

    /**
     * Constructs an instance.
     */
    public FillArrayDataPayloadDecodedInstruction(InstructionCodec format,
            int opcode, short[] data) {
        this(format, opcode, data, data.length, 2);
    }

    /**
     * Constructs an instance.
     */
    public FillArrayDataPayloadDecodedInstruction(InstructionCodec format,
            int opcode, int[] data) {
        this(format, opcode, data, data.length, 4);
    }

    /**
     * Constructs an instance.
     */
    public FillArrayDataPayloadDecodedInstruction(InstructionCodec format,
            int opcode, long[] data) {
        this(format, opcode, data, data.length, 8);
    }

    /** {@inheritDoc} */
    @Override
    public int getRegisterCount() {
        return 0;
    }

    public short getElementWidthUnit() {
        return (short) elementWidth;
    }

    public int getSize() {
        return size;
    }

    public Object getData() {
        return data;
    }

    /** {@inheritDoc} */
    @Override
    public DecodedInstruction withIndex(int newIndex) {
        throw new UnsupportedOperationException("no index in instruction");
    }
}

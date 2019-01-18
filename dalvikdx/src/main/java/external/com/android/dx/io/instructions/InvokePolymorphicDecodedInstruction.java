/*
 * Copyright (C) 2017 The Android Open Source Project
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

import external.com.android.dx.io.IndexType;

/** A decoded invoke-polymorphic instruction. */
public class InvokePolymorphicDecodedInstruction extends DecodedInstruction {

    private final int protoIndex;
    private final int[] registers;

    public InvokePolymorphicDecodedInstruction(
            InstructionCodec format,
            int opcode,
            int methodIndex,
            IndexType indexType,
            int protoIndex,
            int[] registers) {
        super(format, opcode, methodIndex, indexType, 0, 0);
        if (protoIndex != (short) protoIndex) {
          throw new IllegalArgumentException("protoIndex doesn't fit in a short: " + protoIndex);
        }
        this.protoIndex = protoIndex;
        this.registers = registers;
    }

    @Override
    public int getRegisterCount() {
        return registers.length;
    }

    @Override
    public DecodedInstruction withIndex(int newIndex) {
        throw new UnsupportedOperationException(
                "use withProtoIndex to update both the method and proto indices for"
                        + " invoke-polymorphic");
    }

    @Override
    public DecodedInstruction withProtoIndex(int newIndex, int newProtoIndex) {
        return new InvokePolymorphicDecodedInstruction(
                getFormat(), getOpcode(), newIndex, getIndexType(), newProtoIndex, registers);
    }

    @Override
    public int getC() {
        return registers.length > 0 ? registers[0] : 0;
    }

    @Override
    public int getD() {
        return registers.length > 1 ? registers[1] : 0;
    }

    @Override
    public int getE() {
        return registers.length > 2 ? registers[2] : 0;
    }

    public int getF() {
        return registers.length > 3 ? registers[3] : 0;
    }

    public int getG() {
        return registers.length > 4 ? registers[4] : 0;
    }

    @Override
    public short getProtoIndex() {
        return (short) protoIndex;
    }
}

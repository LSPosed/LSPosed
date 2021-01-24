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

import external.com.android.dex.DexException;
import external.com.android.dx.io.IndexType;
import external.com.android.dx.io.OpcodeInfo;
import external.com.android.dx.io.Opcodes;
import external.com.android.dx.util.Hex;
import java.io.EOFException;
import java.util.Arrays;

/**
 * Representation of an instruction format, which knows how to decode into
 * and encode from instances of {@link DecodedInstruction}.
 */
public enum InstructionCodec {
    FORMAT_00X() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            return new ZeroRegisterDecodedInstruction(
                    this, opcodeUnit, 0, null,
                    0, 0L);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(insn.getOpcodeUnit());
        }
    },

    FORMAT_10X() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int literal = byte1(opcodeUnit); // should be zero
            return new ZeroRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(insn.getOpcodeUnit());
        }
    },

    FORMAT_12X() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = nibble2(opcodeUnit);
            int b = nibble3(opcodeUnit);
            return new TwoRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, 0L,
                    a, b);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcodeUnit(),
                             makeByte(insn.getA(), insn.getB())));
        }
    },

    FORMAT_11N() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = nibble2(opcodeUnit);
            int literal = (nibble3(opcodeUnit) << 28) >> 28; // sign-extend
            return new OneRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcodeUnit(),
                             makeByte(insn.getA(), insn.getLiteralNibble())));
        }
    },

    FORMAT_11X() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            return new OneRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, 0L,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(codeUnit(insn.getOpcode(), insn.getA()));
        }
    },

    FORMAT_10T() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int baseAddress = in.cursor() - 1;
            int opcode = byte0(opcodeUnit);
            int target = (byte) byte1(opcodeUnit); // sign-extend
            return new ZeroRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    baseAddress + target, 0L);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            int relativeTarget = insn.getTargetByte(out.cursor());
            out.write(codeUnit(insn.getOpcode(), relativeTarget));
        }
    },

    FORMAT_20T() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int baseAddress = in.cursor() - 1;
            int opcode = byte0(opcodeUnit);
            int literal = byte1(opcodeUnit); // should be zero
            int target = (short) in.read(); // sign-extend
            return new ZeroRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    baseAddress + target, literal);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            short relativeTarget = insn.getTargetUnit(out.cursor());
            out.write(insn.getOpcodeUnit(), relativeTarget);
        }
    },

    FORMAT_20BC() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            // Note: We use the literal field to hold the decoded AA value.
            int opcode = byte0(opcodeUnit);
            int literal = byte1(opcodeUnit);
            int index = in.read();
            return new ZeroRegisterDecodedInstruction(
                    this, opcode, index, IndexType.VARIES,
                    0, literal);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(), insn.getLiteralByte()),
                    insn.getIndexUnit());
        }
    },

    FORMAT_22X() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int b = in.read();
            return new TwoRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, 0L,
                    a, b);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    insn.getBUnit());
        }
    },

    FORMAT_21T() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int baseAddress = in.cursor() - 1;
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int target = (short) in.read(); // sign-extend
            return new OneRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    baseAddress + target, 0L,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            short relativeTarget = insn.getTargetUnit(out.cursor());
            out.write(codeUnit(insn.getOpcode(), insn.getA()), relativeTarget);
        }
    },

    FORMAT_21S() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int literal = (short) in.read(); // sign-extend
            return new OneRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    insn.getLiteralUnit());
        }
    },

    FORMAT_21H() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            long literal = (short) in.read(); // sign-extend

            /*
             * Format 21h decodes differently depending on the opcode,
             * because the "signed hat" might represent either a 32-
             * or 64- bit value.
             */
            literal <<= (opcode == Opcodes.CONST_HIGH16) ? 16 : 48;

            return new OneRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            // See above.
            int opcode = insn.getOpcode();
            int shift = (opcode == Opcodes.CONST_HIGH16) ? 16 : 48;
            short literal = (short) (insn.getLiteral() >> shift);

            out.write(codeUnit(opcode, insn.getA()), literal);
        }
    },

    FORMAT_21C() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int index = in.read();
            IndexType indexType = OpcodeInfo.getIndexType(opcode);
            return new OneRegisterDecodedInstruction(
                    this, opcode, index, indexType,
                    0, 0L,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    insn.getIndexUnit());
        }
    },

    FORMAT_23X() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int bc = in.read();
            int b = byte0(bc);
            int c = byte1(bc);
            return new ThreeRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, 0L,
                    a, b, c);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    codeUnit(insn.getB(), insn.getC()));
        }
    },

    FORMAT_22B() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int bc = in.read();
            int b = byte0(bc);
            int literal = (byte) byte1(bc); // sign-extend
            return new TwoRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal,
                    a, b);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    codeUnit(insn.getB(),
                             insn.getLiteralByte()));
        }
    },

    FORMAT_22T() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int baseAddress = in.cursor() - 1;
            int opcode = byte0(opcodeUnit);
            int a = nibble2(opcodeUnit);
            int b = nibble3(opcodeUnit);
            int target = (short) in.read(); // sign-extend
            return new TwoRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    baseAddress + target, 0L,
                    a, b);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            short relativeTarget = insn.getTargetUnit(out.cursor());
            out.write(
                    codeUnit(insn.getOpcode(),
                             makeByte(insn.getA(), insn.getB())),
                    relativeTarget);
        }
    },

    FORMAT_22S() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = nibble2(opcodeUnit);
            int b = nibble3(opcodeUnit);
            int literal = (short) in.read(); // sign-extend
            return new TwoRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal,
                    a, b);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(),
                             makeByte(insn.getA(), insn.getB())),
                    insn.getLiteralUnit());
        }
    },

    FORMAT_22C() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = nibble2(opcodeUnit);
            int b = nibble3(opcodeUnit);
            int index = in.read();
            IndexType indexType = OpcodeInfo.getIndexType(opcode);
            return new TwoRegisterDecodedInstruction(
                    this, opcode, index, indexType,
                    0, 0L,
                    a, b);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(),
                             makeByte(insn.getA(), insn.getB())),
                    insn.getIndexUnit());
        }
    },

    FORMAT_22CS() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = nibble2(opcodeUnit);
            int b = nibble3(opcodeUnit);
            int index = in.read();
            return new TwoRegisterDecodedInstruction(
                    this, opcode, index, IndexType.FIELD_OFFSET,
                    0, 0L,
                    a, b);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(),
                             makeByte(insn.getA(), insn.getB())),
                    insn.getIndexUnit());
        }
    },

    FORMAT_30T() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int baseAddress = in.cursor() - 1;
            int opcode = byte0(opcodeUnit);
            int literal = byte1(opcodeUnit); // should be zero
            int target = in.readInt();
            return new ZeroRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    baseAddress + target, literal);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            int relativeTarget = insn.getTarget(out.cursor());
            out.write(insn.getOpcodeUnit(),
                    unit0(relativeTarget), unit1(relativeTarget));
        }
    },

    FORMAT_32X() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int literal = byte1(opcodeUnit); // should be zero
            int a = in.read();
            int b = in.read();
            return new TwoRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal,
                    a, b);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(insn.getOpcodeUnit(), insn.getAUnit(), insn.getBUnit());
        }
    },

    FORMAT_31I() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int literal = in.readInt();
            return new OneRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            int literal = insn.getLiteralInt();
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    unit0(literal),
                    unit1(literal));
        }
    },

    FORMAT_31T() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int baseAddress = in.cursor() - 1;
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int target = baseAddress + in.readInt();

            /*
             * Switch instructions need to "forward" their addresses to their
             * payload target instructions.
             */
            switch (opcode) {
                case Opcodes.PACKED_SWITCH:
                case Opcodes.SPARSE_SWITCH: {
                    in.setBaseAddress(target, baseAddress);
                    break;
                }
                default: // fall out
            }

            return new OneRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    target, 0L,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            int relativeTarget = insn.getTarget(out.cursor());
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    unit0(relativeTarget), unit1(relativeTarget));
        }
    },

    FORMAT_31C() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            int index = in.readInt();
            IndexType indexType = OpcodeInfo.getIndexType(opcode);
            return new OneRegisterDecodedInstruction(
                    this, opcode, index, indexType,
                    0, 0L,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            int index = insn.getIndex();
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    unit0(index),
                    unit1(index));
        }
    },

    FORMAT_35C() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            return decodeRegisterList(this, opcodeUnit, in);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            encodeRegisterList(insn, out);
        }
    },

    FORMAT_35MS() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            return decodeRegisterList(this, opcodeUnit, in);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            encodeRegisterList(insn, out);
        }
    },

    FORMAT_35MI() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            return decodeRegisterList(this, opcodeUnit, in);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            encodeRegisterList(insn, out);
        }
    },

    FORMAT_3RC() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            return decodeRegisterRange(this, opcodeUnit, in);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            encodeRegisterRange(insn, out);
        }
    },

    FORMAT_3RMS() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            return decodeRegisterRange(this, opcodeUnit, in);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            encodeRegisterRange(insn, out);
        }
    },

    FORMAT_3RMI() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            return decodeRegisterRange(this, opcodeUnit, in);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            encodeRegisterRange(insn, out);
        }
    },

    FORMAT_51L() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            int a = byte1(opcodeUnit);
            long literal = in.readLong();
            return new OneRegisterDecodedInstruction(
                    this, opcode, 0, null,
                    0, literal,
                    a);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            long literal = insn.getLiteral();
            out.write(
                    codeUnit(insn.getOpcode(), insn.getA()),
                    unit0(literal),
                    unit1(literal),
                    unit2(literal),
                    unit3(literal));
        }
    },

    FORMAT_45CC() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            if (opcode != Opcodes.INVOKE_POLYMORPHIC) {
              // 45cc isn't currently used for anything other than invoke-polymorphic.
              // If that changes, add a more general DecodedInstruction for this format.
              throw new UnsupportedOperationException(String.valueOf(opcode));
            }
            int g = nibble2(opcodeUnit);
            int registerCount = nibble3(opcodeUnit);
            int methodIndex = in.read();
            int cdef = in.read();
            int c = nibble0(cdef);
            int d = nibble1(cdef);
            int e = nibble2(cdef);
            int f = nibble3(cdef);
            int protoIndex = in.read();
            IndexType indexType = OpcodeInfo.getIndexType(opcode);

            if (registerCount < 1 || registerCount > 5) {
                throw new DexException("bogus registerCount: " + Hex.uNibble(registerCount));
            }
            int[] registers = {c, d, e, f, g};
            registers = Arrays.copyOfRange(registers, 0, registerCount);

            return new InvokePolymorphicDecodedInstruction(
                    this, opcode, methodIndex, indexType, protoIndex, registers);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            InvokePolymorphicDecodedInstruction polyInsn =
                    (InvokePolymorphicDecodedInstruction) insn;
            out.write(codeUnit(polyInsn.getOpcode(),
                            makeByte(polyInsn.getG(), polyInsn.getRegisterCount())),
                    polyInsn.getIndexUnit(),
                    codeUnit(polyInsn.getC(), polyInsn.getD(), polyInsn.getE(), polyInsn.getF()),
                    polyInsn.getProtoIndex());

        }
    },

    FORMAT_4RCC() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int opcode = byte0(opcodeUnit);
            if (opcode != Opcodes.INVOKE_POLYMORPHIC_RANGE) {
              // 4rcc isn't currently used for anything other than invoke-polymorphic.
              // If that changes, add a more general DecodedInstruction for this format.
              throw new UnsupportedOperationException(String.valueOf(opcode));
            }
            int registerCount = byte1(opcodeUnit);
            int methodIndex = in.read();
            int c = in.read();
            int protoIndex = in.read();
            IndexType indexType = OpcodeInfo.getIndexType(opcode);
            return new InvokePolymorphicRangeDecodedInstruction(
                    this, opcode, methodIndex, indexType, c, registerCount, protoIndex);

        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            out.write(
                    codeUnit(insn.getOpcode(), insn.getRegisterCount()),
                    insn.getIndexUnit(),
                    insn.getCUnit(),
                    insn.getProtoIndex());

        }
    },

    FORMAT_PACKED_SWITCH_PAYLOAD() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int baseAddress = in.baseAddressForCursor() - 1; // already read opcode
            int size = in.read();
            int firstKey = in.readInt();
            int[] targets = new int[size];

            for (int i = 0; i < size; i++) {
                targets[i] = baseAddress + in.readInt();
            }

            return new PackedSwitchPayloadDecodedInstruction(
                    this, opcodeUnit, firstKey, targets);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            PackedSwitchPayloadDecodedInstruction payload =
                (PackedSwitchPayloadDecodedInstruction) insn;
            int[] targets = payload.getTargets();
            int baseAddress = out.baseAddressForCursor();

            out.write(payload.getOpcodeUnit());
            out.write(asUnsignedUnit(targets.length));
            out.writeInt(payload.getFirstKey());

            for (int target : targets) {
                out.writeInt(target - baseAddress);
            }
        }
    },

    FORMAT_SPARSE_SWITCH_PAYLOAD() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int baseAddress = in.baseAddressForCursor() - 1; // already read opcode
            int size = in.read();
            int[] keys = new int[size];
            int[] targets = new int[size];

            for (int i = 0; i < size; i++) {
                keys[i] = in.readInt();
            }

            for (int i = 0; i < size; i++) {
                targets[i] = baseAddress + in.readInt();
            }

            return new SparseSwitchPayloadDecodedInstruction(
                    this, opcodeUnit, keys, targets);
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            SparseSwitchPayloadDecodedInstruction payload =
                (SparseSwitchPayloadDecodedInstruction) insn;
            int[] keys = payload.getKeys();
            int[] targets = payload.getTargets();
            int baseAddress = out.baseAddressForCursor();

            out.write(payload.getOpcodeUnit());
            out.write(asUnsignedUnit(targets.length));

            for (int key : keys) {
                out.writeInt(key);
            }

            for (int target : targets) {
                out.writeInt(target - baseAddress);
            }
        }
    },

    FORMAT_FILL_ARRAY_DATA_PAYLOAD() {
        @Override
        public DecodedInstruction decode(int opcodeUnit,
                CodeInput in) throws EOFException {
            int elementWidth = in.read();
            int size = in.readInt();

            switch (elementWidth) {
                case 1: {
                    byte[] array = new byte[size];
                    boolean even = true;
                    for (int i = 0, value = 0; i < size; i++, even = !even) {
                        if (even) {
                            value = in.read();
                        }
                        array[i] = (byte) (value & 0xff);
                        value >>= 8;
                    }
                    return new FillArrayDataPayloadDecodedInstruction(
                            this, opcodeUnit, array);
                }
                case 2: {
                    short[] array = new short[size];
                    for (int i = 0; i < size; i++) {
                        array[i] = (short) in.read();
                    }
                    return new FillArrayDataPayloadDecodedInstruction(
                            this, opcodeUnit, array);
                }
                case 4: {
                    int[] array = new int[size];
                    for (int i = 0; i < size; i++) {
                        array[i] = in.readInt();
                    }
                    return new FillArrayDataPayloadDecodedInstruction(
                            this, opcodeUnit, array);
                }
                case 8: {
                    long[] array = new long[size];
                    for (int i = 0; i < size; i++) {
                        array[i] = in.readLong();
                    }
                    return new FillArrayDataPayloadDecodedInstruction(
                            this, opcodeUnit, array);
                }
                default: // fall out
            }

            throw new DexException("bogus element_width: "
                    + Hex.u2(elementWidth));
        }

        @Override
        public void encode(DecodedInstruction insn, CodeOutput out) {
            FillArrayDataPayloadDecodedInstruction payload =
                (FillArrayDataPayloadDecodedInstruction) insn;
            short elementWidth = payload.getElementWidthUnit();
            Object data = payload.getData();

            out.write(payload.getOpcodeUnit());
            out.write(elementWidth);
            out.writeInt(payload.getSize());

            switch (elementWidth) {
                case 1: out.write((byte[]) data);  break;
                case 2: out.write((short[]) data); break;
                case 4: out.write((int[]) data);   break;
                case 8: out.write((long[]) data);  break;
                default: {
                    throw new DexException("bogus element_width: "
                            + Hex.u2(elementWidth));
                }
            }
        }
    };

    /**
     * Decodes an instruction specified by the given opcode unit, reading
     * any required additional code units from the given input source.
     */
    public abstract DecodedInstruction decode(int opcodeUnit, CodeInput in)
        throws EOFException;

    /**
     * Encodes the given instruction.
     */
    public abstract void encode(DecodedInstruction insn, CodeOutput out);

    /**
     * Helper method that decodes any of the register-list formats.
     */
    private static DecodedInstruction decodeRegisterList(
            InstructionCodec format, int opcodeUnit, CodeInput in)
            throws EOFException {
        int opcode = byte0(opcodeUnit);
        int e = nibble2(opcodeUnit);
        int registerCount = nibble3(opcodeUnit);
        int index = in.read();
        int abcd = in.read();
        int a = nibble0(abcd);
        int b = nibble1(abcd);
        int c = nibble2(abcd);
        int d = nibble3(abcd);
        IndexType indexType = OpcodeInfo.getIndexType(opcode);

        // TODO: Having to switch like this is less than ideal.
        switch (registerCount) {
            case 0:
                return new ZeroRegisterDecodedInstruction(
                        format, opcode, index, indexType,
                        0, 0L);
            case 1:
                return new OneRegisterDecodedInstruction(
                        format, opcode, index, indexType,
                        0, 0L,
                        a);
            case 2:
                return new TwoRegisterDecodedInstruction(
                        format, opcode, index, indexType,
                        0, 0L,
                        a, b);
            case 3:
                return new ThreeRegisterDecodedInstruction(
                        format, opcode, index, indexType,
                        0, 0L,
                        a, b, c);
            case 4:
                return new FourRegisterDecodedInstruction(
                        format, opcode, index, indexType,
                        0, 0L,
                        a, b, c, d);
            case 5:
                return new FiveRegisterDecodedInstruction(
                        format, opcode, index, indexType,
                        0, 0L,
                        a, b, c, d, e);
            default: // fall out
        }

        throw new DexException("bogus registerCount: "
                + Hex.uNibble(registerCount));
    }

    /**
     * Helper method that encodes any of the register-list formats.
     */
    private static void encodeRegisterList(DecodedInstruction insn,
            CodeOutput out) {
        out.write(codeUnit(insn.getOpcode(),
                        makeByte(insn.getE(), insn.getRegisterCount())),
                insn.getIndexUnit(),
                codeUnit(insn.getA(), insn.getB(), insn.getC(), insn.getD()));
    }

    /**
     * Helper method that decodes any of the three-unit register-range formats.
     */
    private static DecodedInstruction decodeRegisterRange(
            InstructionCodec format, int opcodeUnit, CodeInput in)
            throws EOFException {
        int opcode = byte0(opcodeUnit);
        int registerCount = byte1(opcodeUnit);
        int index = in.read();
        int a = in.read();
        IndexType indexType = OpcodeInfo.getIndexType(opcode);
        return new RegisterRangeDecodedInstruction(
                format, opcode, index, indexType,
                0, 0L,
                a, registerCount);
    }

    /**
     * Helper method that encodes any of the three-unit register-range formats.
     */
    private static void encodeRegisterRange(DecodedInstruction insn,
            CodeOutput out) {
        out.write(codeUnit(insn.getOpcode(), insn.getRegisterCount()),
                insn.getIndexUnit(),
                insn.getAUnit());
    }

    private static short codeUnit(int lowByte, int highByte) {
        if ((lowByte & ~0xff) != 0) {
            throw new IllegalArgumentException("bogus lowByte");
        }

        if ((highByte & ~0xff) != 0) {
            throw new IllegalArgumentException("bogus highByte");
        }

        return (short) (lowByte | (highByte << 8));
    }

    private static short codeUnit(int nibble0, int nibble1, int nibble2,
            int nibble3) {
        if ((nibble0 & ~0xf) != 0) {
            throw new IllegalArgumentException("bogus nibble0");
        }

        if ((nibble1 & ~0xf) != 0) {
            throw new IllegalArgumentException("bogus nibble1");
        }

        if ((nibble2 & ~0xf) != 0) {
            throw new IllegalArgumentException("bogus nibble2");
        }

        if ((nibble3 & ~0xf) != 0) {
            throw new IllegalArgumentException("bogus nibble3");
        }

        return (short) (nibble0 | (nibble1 << 4)
                | (nibble2 << 8) | (nibble3 << 12));
    }

    private static int makeByte(int lowNibble, int highNibble) {
        if ((lowNibble & ~0xf) != 0) {
            throw new IllegalArgumentException("bogus lowNibble");
        }

        if ((highNibble & ~0xf) != 0) {
            throw new IllegalArgumentException("bogus highNibble");
        }

        return lowNibble | (highNibble << 4);
    }

    private static short asUnsignedUnit(int value) {
        if ((value & ~0xffff) != 0) {
            throw new IllegalArgumentException("bogus unsigned code unit");
        }

        return (short) value;
    }

    private static short unit0(int value) {
        return (short) value;
    }

    private static short unit1(int value) {
        return (short) (value >> 16);
    }

    private static short unit0(long value) {
        return (short) value;
    }

    private static short unit1(long value) {
        return (short) (value >> 16);
    }

    private static short unit2(long value) {
        return (short) (value >> 32);
    }

    private static short unit3(long value) {
        return (short) (value >> 48);
    }

    private static int byte0(int value) {
        return value & 0xff;
    }

    private static int byte1(int value) {
        return (value >> 8) & 0xff;
    }

    private static int byte2(int value) {
        return (value >> 16) & 0xff;
    }

    private static int byte3(int value) {
        return value >>> 24;
    }

    private static int nibble0(int value) {
        return value & 0xf;
    }

    private static int nibble1(int value) {
        return (value >> 4) & 0xf;
    }

    private static int nibble2(int value) {
        return (value >> 8) & 0xf;
    }

    private static int nibble3(int value) {
        return (value >> 12) & 0xf;
    }
}

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

package external.com.android.dx.cf.code;

import external.com.android.dx.rop.cst.CstCallSiteRef;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Prototype;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeBearer;
import external.com.android.dx.util.Hex;

/**
 * {@link Machine} which keeps track of known values but does not do
 * smart/realistic reference type calculations.
 */
public class ValueAwareMachine extends BaseMachine {
    /**
     * Constructs an instance.
     *
     * @param prototype {@code non-null;} the prototype for the associated
     * method
     */
    public ValueAwareMachine(Prototype prototype) {
        super(prototype);
    }

    /** {@inheritDoc} */
    @Override
    public void run(Frame frame, int offset, int opcode) {
        switch (opcode) {
            case ByteOps.NOP:
            case ByteOps.IASTORE:
            case ByteOps.POP:
            case ByteOps.POP2:
            case ByteOps.IFEQ:
            case ByteOps.IFNE:
            case ByteOps.IFLT:
            case ByteOps.IFGE:
            case ByteOps.IFGT:
            case ByteOps.IFLE:
            case ByteOps.IF_ICMPEQ:
            case ByteOps.IF_ICMPNE:
            case ByteOps.IF_ICMPLT:
            case ByteOps.IF_ICMPGE:
            case ByteOps.IF_ICMPGT:
            case ByteOps.IF_ICMPLE:
            case ByteOps.IF_ACMPEQ:
            case ByteOps.IF_ACMPNE:
            case ByteOps.GOTO:
            case ByteOps.RET:
            case ByteOps.LOOKUPSWITCH:
            case ByteOps.IRETURN:
            case ByteOps.RETURN:
            case ByteOps.PUTSTATIC:
            case ByteOps.PUTFIELD:
            case ByteOps.ATHROW:
            case ByteOps.MONITORENTER:
            case ByteOps.MONITOREXIT:
            case ByteOps.IFNULL:
            case ByteOps.IFNONNULL: {
                // Nothing to do for these ops in this class.
                clearResult();
                break;
            }
            case ByteOps.LDC:
            case ByteOps.LDC2_W: {
                setResult((TypeBearer) getAuxCst());
                break;
            }
            case ByteOps.ILOAD:
            case ByteOps.ISTORE: {
                setResult(arg(0));
                break;
            }
            case ByteOps.IALOAD:
            case ByteOps.IADD:
            case ByteOps.ISUB:
            case ByteOps.IMUL:
            case ByteOps.IDIV:
            case ByteOps.IREM:
            case ByteOps.INEG:
            case ByteOps.ISHL:
            case ByteOps.ISHR:
            case ByteOps.IUSHR:
            case ByteOps.IAND:
            case ByteOps.IOR:
            case ByteOps.IXOR:
            case ByteOps.IINC:
            case ByteOps.I2L:
            case ByteOps.I2F:
            case ByteOps.I2D:
            case ByteOps.L2I:
            case ByteOps.L2F:
            case ByteOps.L2D:
            case ByteOps.F2I:
            case ByteOps.F2L:
            case ByteOps.F2D:
            case ByteOps.D2I:
            case ByteOps.D2L:
            case ByteOps.D2F:
            case ByteOps.I2B:
            case ByteOps.I2C:
            case ByteOps.I2S:
            case ByteOps.LCMP:
            case ByteOps.FCMPL:
            case ByteOps.FCMPG:
            case ByteOps.DCMPL:
            case ByteOps.DCMPG:
            case ByteOps.ARRAYLENGTH: {
                setResult(getAuxType());
                break;
            }
            case ByteOps.DUP:
            case ByteOps.DUP_X1:
            case ByteOps.DUP_X2:
            case ByteOps.DUP2:
            case ByteOps.DUP2_X1:
            case ByteOps.DUP2_X2:
            case ByteOps.SWAP: {
                clearResult();
                for (int pattern = getAuxInt(); pattern != 0; pattern >>= 4) {
                    int which = (pattern & 0x0f) - 1;
                    addResult(arg(which));
                }
                break;
            }

            case ByteOps.JSR: {
                setResult(new ReturnAddress(getAuxTarget()));
                break;
            }
            case ByteOps.GETSTATIC:
            case ByteOps.GETFIELD:
            case ByteOps.INVOKEVIRTUAL:
            case ByteOps.INVOKESTATIC:
            case ByteOps.INVOKEINTERFACE: {
                Type type = ((TypeBearer) getAuxCst()).getType();
                if (type == Type.VOID) {
                    clearResult();
                } else {
                    setResult(type);
                }
                break;
            }
            case ByteOps.INVOKESPECIAL: {
                Type thisType = arg(0).getType();
                if (thisType.isUninitialized()) {
                    frame.makeInitialized(thisType);
                }
                Type type = ((TypeBearer) getAuxCst()).getType();
                if (type == Type.VOID) {
                    clearResult();
                } else {
                    setResult(type);
                }
                break;
            }
            case ByteOps.INVOKEDYNAMIC: {
                Type type = ((CstCallSiteRef) getAuxCst()).getReturnType();
                if (type == Type.VOID) {
                    clearResult();
                } else {
                    setResult(type);
                }
                break;
            }
            case ByteOps.NEW: {
                Type type = ((CstType) getAuxCst()).getClassType();
                setResult(type.asUninitialized(offset));
                break;
            }
            case ByteOps.NEWARRAY:
            case ByteOps.CHECKCAST:
            case ByteOps.MULTIANEWARRAY: {
                Type type = ((CstType) getAuxCst()).getClassType();
                setResult(type);
                break;
            }
            case ByteOps.ANEWARRAY: {
                Type type = ((CstType) getAuxCst()).getClassType();
                setResult(type.getArrayType());
                break;
            }
            case ByteOps.INSTANCEOF: {
                setResult(Type.INT);
                break;
            }
            default: {
                throw new RuntimeException("shouldn't happen: " +
                                           Hex.u1(opcode));
            }
        }

        storeResults(frame);
    }
}

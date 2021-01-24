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

package external.com.android.dx.io;

import external.com.android.dex.DexException;
import external.com.android.dx.io.instructions.DecodedInstruction;

/**
 * Walks through a block of code and calls visitor call backs.
 */
public final class CodeReader {
    private Visitor fallbackVisitor = null;
    private Visitor stringVisitor = null;
    private Visitor typeVisitor = null;
    private Visitor fieldVisitor = null;
    private Visitor methodVisitor = null;
    private Visitor methodAndProtoVisitor = null;
    private Visitor callSiteVisitor = null;

    /**
     * Sets {@code visitor} as the visitor for all instructions.
     */
    public void setAllVisitors(Visitor visitor) {
        fallbackVisitor = visitor;
        stringVisitor = visitor;
        typeVisitor = visitor;
        fieldVisitor = visitor;
        methodVisitor = visitor;
        methodAndProtoVisitor = visitor;
        callSiteVisitor = visitor;
    }

    /**
     * Sets {@code visitor} as the visitor for all instructions not
     * otherwise handled.
     */
    public void setFallbackVisitor(Visitor visitor) {
        fallbackVisitor = visitor;
    }

    /**
     * Sets {@code visitor} as the visitor for all string instructions.
     */
    public void setStringVisitor(Visitor visitor) {
        stringVisitor = visitor;
    }

    /**
     * Sets {@code visitor} as the visitor for all type instructions.
     */
    public void setTypeVisitor(Visitor visitor) {
        typeVisitor = visitor;
    }

    /**
     * Sets {@code visitor} as the visitor for all field instructions.
     */
    public void setFieldVisitor(Visitor visitor) {
        fieldVisitor = visitor;
    }

    /**
     * Sets {@code visitor} as the visitor for all method instructions.
     */
    public void setMethodVisitor(Visitor visitor) {
        methodVisitor = visitor;
    }

    /** Sets {@code visitor} as the visitor for all method and proto instructions. */
    public void setMethodAndProtoVisitor(Visitor visitor) {
        methodAndProtoVisitor = visitor;
    }

    /** Sets {@code visitor} as the visitor for all call site instructions. */
    public void setCallSiteVisitor(Visitor visitor) {
        callSiteVisitor = visitor;
    }

    public void visitAll(DecodedInstruction[] decodedInstructions)
            throws DexException {
        int size = decodedInstructions.length;

        for (int i = 0; i < size; i++) {
            DecodedInstruction one = decodedInstructions[i];
            if (one == null) {
                continue;
            }

            callVisit(decodedInstructions, one);
        }
    }

    public void visitAll(short[] encodedInstructions) throws DexException {
        DecodedInstruction[] decodedInstructions =
            DecodedInstruction.decodeAll(encodedInstructions);
        visitAll(decodedInstructions);
    }

    private void callVisit(DecodedInstruction[] all, DecodedInstruction one) {
        Visitor visitor = null;

        switch (OpcodeInfo.getIndexType(one.getOpcode())) {
            case STRING_REF:           visitor = stringVisitor;         break;
            case TYPE_REF:             visitor = typeVisitor;           break;
            case FIELD_REF:            visitor = fieldVisitor;          break;
            case METHOD_REF:           visitor = methodVisitor;         break;
            case METHOD_AND_PROTO_REF: visitor = methodAndProtoVisitor; break;
            case CALL_SITE_REF:        visitor = callSiteVisitor;       break;
        }

        if (visitor == null) {
            visitor = fallbackVisitor;
        }

        if (visitor != null) {
            visitor.visit(all, one);
        }
    }

    public interface Visitor {
        void visit(DecodedInstruction[] all, DecodedInstruction one);
    }
}

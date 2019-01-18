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

import external.com.android.dx.rop.code.BasicBlock;
import external.com.android.dx.rop.code.Insn;
import external.com.android.dx.rop.code.InsnList;
import external.com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A branch target in a list of instructions.
 */
public final class Label {

    final List<Insn> instructions = new ArrayList<>();

    Code code;

    boolean marked = false;

    /** an immutable list of labels corresponding to the types in the catch list */
    List<Label> catchLabels = Collections.emptyList();

    /** contains the next instruction if no branch occurs */
    Label primarySuccessor;

    /** contains the instruction to jump to if the if is true */
    Label alternateSuccessor;

    int id = -1;

    public Label() {}

    boolean isEmpty() {
        return instructions.isEmpty();
    }

    void compact() {
        for (int i = 0; i < catchLabels.size(); i++) {
            while (catchLabels.get(i).isEmpty()) {
                catchLabels.set(i, catchLabels.get(i).primarySuccessor);
            }
        }
        while (primarySuccessor != null && primarySuccessor.isEmpty()) {
            primarySuccessor = primarySuccessor.primarySuccessor;
        }
        while (alternateSuccessor != null && alternateSuccessor.isEmpty()) {
            alternateSuccessor = alternateSuccessor.primarySuccessor;
        }
    }

    BasicBlock toBasicBlock() {
        InsnList result = new InsnList(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            result.set(i, instructions.get(i));
        }
        result.setImmutable();

        int primarySuccessorIndex = -1;
        IntList successors = new IntList();
        for (Label catchLabel : catchLabels) {
            successors.add(catchLabel.id);
        }
        if (primarySuccessor != null) {
            primarySuccessorIndex = primarySuccessor.id;
            successors.add(primarySuccessorIndex);
        }
        if (alternateSuccessor != null) {
            successors.add(alternateSuccessor.id);
        }
        successors.setImmutable();

        return new BasicBlock(id, result, successors, primarySuccessorIndex);
    }
}

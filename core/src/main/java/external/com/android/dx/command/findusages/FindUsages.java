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

package external.com.android.dx.command.findusages;

import external.com.android.dex.ClassData;
import external.com.android.dex.ClassDef;
import external.com.android.dex.Dex;
import external.com.android.dex.FieldId;
import external.com.android.dex.MethodId;
import external.com.android.dx.io.CodeReader;
import external.com.android.dx.io.OpcodeInfo;
import external.com.android.dx.io.instructions.DecodedInstruction;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class FindUsages {
    private final Dex dex;
    private final Set<Integer> methodIds;
    private final Set<Integer> fieldIds;
    private final CodeReader codeReader = new CodeReader();
    private final PrintWriter out;

    private ClassDef currentClass;
    private ClassData.Method currentMethod;

    public FindUsages(final Dex dex, String declaredBy, String memberName, final PrintWriter out) {
        this.dex = dex;
        this.out = out;

        Set<Integer> typeStringIndexes = new HashSet<Integer>();
        Set<Integer> memberNameIndexes = new HashSet<Integer>();
        Pattern declaredByPattern = Pattern.compile(declaredBy);
        Pattern memberNamePattern = Pattern.compile(memberName);
        List<String> strings = dex.strings();
        for (int i = 0; i < strings.size(); ++i) {
            String string = strings.get(i);
            if (declaredByPattern.matcher(string).matches()) {
                typeStringIndexes.add(i);
            }
            if (memberNamePattern.matcher(string).matches()) {
                memberNameIndexes.add(i);
            }
        }
        if (typeStringIndexes.isEmpty() || memberNameIndexes.isEmpty()) {
            methodIds = fieldIds = null;
            return; // these symbols are not mentioned in this dex
        }

        methodIds = new HashSet<Integer>();
        fieldIds = new HashSet<Integer>();
        for (int typeStringIndex : typeStringIndexes) {
            int typeIndex = Collections.binarySearch(dex.typeIds(), typeStringIndex);
            if (typeIndex < 0) {
                continue; // this type name isn't used as a type in this dex
            }
            methodIds.addAll(getMethodIds(dex, memberNameIndexes, typeIndex));
            fieldIds.addAll(getFieldIds(dex, memberNameIndexes, typeIndex));
        }

        codeReader.setFieldVisitor(new CodeReader.Visitor() {
            @Override
            public void visit(DecodedInstruction[] all,
                    DecodedInstruction one) {
                int fieldId = one.getIndex();
                if (fieldIds.contains(fieldId)) {
                    out.println(location() + ": field reference " + dex.fieldIds().get(fieldId)
                            + " (" + OpcodeInfo.getName(one.getOpcode()) + ")");
                }
            }
        });

        codeReader.setMethodVisitor(new CodeReader.Visitor() {
            @Override
            public void visit(DecodedInstruction[] all, DecodedInstruction one) {
                int methodId = one.getIndex();
                if (methodIds.contains(methodId)) {
                    out.println(location() + ": method reference " + dex.methodIds().get(methodId)
                            + " (" + OpcodeInfo.getName(one.getOpcode()) + ")");
                }
            }
        });
    }

    private String location() {
        String className = dex.typeNames().get(currentClass.getTypeIndex());
        if (currentMethod != null) {
            MethodId methodId = dex.methodIds().get(currentMethod.getMethodIndex());
            return className + "." + dex.strings().get(methodId.getNameIndex());
        } else {
            return className;
        }
    }

    /**
     * Prints usages to out.
     */
    public void findUsages() {
        if (fieldIds == null || methodIds == null) {
            return;
        }

        for (ClassDef classDef : dex.classDefs()) {
            currentClass = classDef;
            currentMethod = null;

            if (classDef.getClassDataOffset() == 0) {
                continue;
            }

            ClassData classData = dex.readClassData(classDef);
            for (ClassData.Field field : classData.allFields()) {
                int fieldIndex = field.getFieldIndex();
                if (fieldIds.contains(fieldIndex)) {
                    out.println(location() + " field declared " + dex.fieldIds().get(fieldIndex));
                }
            }

            for (ClassData.Method method : classData.allMethods()) {
                currentMethod = method;
                int methodIndex = method.getMethodIndex();
                if (methodIds.contains(methodIndex)) {
                    out.println(location() + " method declared " + dex.methodIds().get(methodIndex));
                }
                if (method.getCodeOffset() != 0) {
                    codeReader.visitAll(dex.readCode(method).getInstructions());
                }
            }
        }

        currentClass = null;
        currentMethod = null;
    }

    /**
     * Returns the fields with {@code memberNameIndex} declared by {@code
     * declaringType}.
     */
    private Set<Integer> getFieldIds(Dex dex, Set<Integer> memberNameIndexes, int declaringType) {
        Set<Integer> fields = new HashSet<Integer>();
        int fieldIndex = 0;
        for (FieldId fieldId : dex.fieldIds()) {
            if (memberNameIndexes.contains(fieldId.getNameIndex())
                    && declaringType == fieldId.getDeclaringClassIndex()) {
                fields.add(fieldIndex);
            }
            fieldIndex++;
        }
        return fields;
    }

    /**
     * Returns the methods with {@code memberNameIndex} declared by {@code
     * declaringType} and its subtypes.
     */
    private Set<Integer> getMethodIds(Dex dex, Set<Integer> memberNameIndexes, int declaringType) {
        Set<Integer> subtypes = findAssignableTypes(dex, declaringType);

        Set<Integer> methods = new HashSet<Integer>();
        int methodIndex = 0;
        for (MethodId method : dex.methodIds()) {
            if (memberNameIndexes.contains(method.getNameIndex())
                    && subtypes.contains(method.getDeclaringClassIndex())) {
                methods.add(methodIndex);
            }
            methodIndex++;
        }
        return methods;
    }

    /**
     * Returns the set of types that can be assigned to {@code typeIndex}.
     */
    private Set<Integer> findAssignableTypes(Dex dex, int typeIndex) {
        Set<Integer> assignableTypes = new HashSet<Integer>();
        assignableTypes.add(typeIndex);

        for (ClassDef classDef : dex.classDefs()) {
            if (assignableTypes.contains(classDef.getSupertypeIndex())) {
                assignableTypes.add(classDef.getTypeIndex());
                continue;
            }

            for (int implemented : classDef.getInterfaces()) {
                if (assignableTypes.contains(implemented)) {
                    assignableTypes.add(classDef.getTypeIndex());
                    break;
                }
            }
        }

        return assignableTypes;
    }
}

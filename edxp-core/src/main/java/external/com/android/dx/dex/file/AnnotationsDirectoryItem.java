/*
 * Copyright (C) 2008 The Android Open Source Project
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

package external.com.android.dx.dex.file;

import external.com.android.dx.rop.annotation.Annotations;
import external.com.android.dx.rop.annotation.AnnotationsList;
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.util.AnnotatedOutput;
import external.com.android.dx.util.Hex;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Per-class directory of annotations.
 */
public final class AnnotationsDirectoryItem extends OffsettedItem {
    /** the required alignment for instances of this class */
    private static final int ALIGNMENT = 4;

    /** write size of this class's header, in bytes */
    private static final int HEADER_SIZE = 16;

    /** write size of a list element, in bytes */
    private static final int ELEMENT_SIZE = 8;

    /** {@code null-ok;} the class-level annotations, if any */
    private AnnotationSetItem classAnnotations;

    /** {@code null-ok;} the annotated fields, if any */
    private ArrayList<FieldAnnotationStruct> fieldAnnotations;

    /** {@code null-ok;} the annotated methods, if any */
    private ArrayList<MethodAnnotationStruct> methodAnnotations;

    /** {@code null-ok;} the annotated parameters, if any */
    private ArrayList<ParameterAnnotationStruct> parameterAnnotations;

    /**
     * Constructs an empty instance.
     */
    public AnnotationsDirectoryItem() {
        super(ALIGNMENT, -1);

        classAnnotations = null;
        fieldAnnotations = null;
        methodAnnotations = null;
        parameterAnnotations = null;
    }

    /** {@inheritDoc} */
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM;
    }

    /**
     * Returns whether this item is empty (has no contents).
     *
     * @return {@code true} if this item is empty, or {@code false}
     * if not
     */
    public boolean isEmpty() {
        return (classAnnotations == null) &&
            (fieldAnnotations == null) &&
            (methodAnnotations == null) &&
            (parameterAnnotations == null);
    }

    /**
     * Returns whether this item is a candidate for interning. The only
     * interning candidates are ones that <i>only</i> have a non-null
     * set of class annotations, with no other lists.
     *
     * @return {@code true} if this is an interning candidate, or
     * {@code false} if not
     */
    public boolean isInternable() {
        return (classAnnotations != null) &&
            (fieldAnnotations == null) &&
            (methodAnnotations == null) &&
            (parameterAnnotations == null);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (classAnnotations == null) {
            return 0;
        }

        return classAnnotations.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Note:</b>: This throws an exception if this item is not
     * internable.</p>
     *
     * @see #isInternable
     */
    @Override
    public int compareTo0(OffsettedItem other) {
        if (! isInternable()) {
            throw new UnsupportedOperationException("uninternable instance");
        }

        AnnotationsDirectoryItem otherDirectory =
            (AnnotationsDirectoryItem) other;
        return classAnnotations.compareTo(otherDirectory.classAnnotations);
    }

    /**
     * Sets the direct annotations on this instance. These are annotations
     * made on the class, per se, as opposed to on one of its members.
     * It is only valid to call this method at most once per instance.
     *
     * @param annotations {@code non-null;} annotations to set for this class
     * @param dexFile {@code non-null;} dex output
     */
    public void setClassAnnotations(Annotations annotations, DexFile dexFile) {
        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }

        if (classAnnotations != null) {
            throw new UnsupportedOperationException(
                    "class annotations already set");
        }

        classAnnotations = new AnnotationSetItem(annotations, dexFile);
    }

    /**
     * Adds a field annotations item to this instance.
     *
     * @param field {@code non-null;} field in question
     * @param annotations {@code non-null;} associated annotations to add
     * @param dexFile {@code non-null;} dex output
     */
    public void addFieldAnnotations(CstFieldRef field,
            Annotations annotations, DexFile dexFile) {
        if (fieldAnnotations == null) {
            fieldAnnotations = new ArrayList<FieldAnnotationStruct>();
        }

        fieldAnnotations.add(new FieldAnnotationStruct(field,
                        new AnnotationSetItem(annotations, dexFile)));
    }

    /**
     * Adds a method annotations item to this instance.
     *
     * @param method {@code non-null;} method in question
     * @param annotations {@code non-null;} associated annotations to add
     * @param dexFile {@code non-null;} dex output
     */
    public void addMethodAnnotations(CstMethodRef method,
            Annotations annotations, DexFile dexFile) {
        if (methodAnnotations == null) {
            methodAnnotations = new ArrayList<MethodAnnotationStruct>();
        }

        methodAnnotations.add(new MethodAnnotationStruct(method,
                        new AnnotationSetItem(annotations, dexFile)));
    }

    /**
     * Adds a parameter annotations item to this instance.
     *
     * @param method {@code non-null;} method in question
     * @param list {@code non-null;} associated list of annotation sets to add
     * @param dexFile {@code non-null;} dex output
     */
    public void addParameterAnnotations(CstMethodRef method,
            AnnotationsList list, DexFile dexFile) {
        if (parameterAnnotations == null) {
            parameterAnnotations = new ArrayList<ParameterAnnotationStruct>();
        }

        parameterAnnotations.add(new ParameterAnnotationStruct(method, list, dexFile));
    }

    /**
     * Gets the method annotations for a given method, if any. This is
     * meant for use by debugging / dumping code.
     *
     * @param method {@code non-null;} the method
     * @return {@code null-ok;} the method annotations, if any
     */
    public Annotations getMethodAnnotations(CstMethodRef method) {
        if (methodAnnotations == null) {
            return null;
        }

        for (MethodAnnotationStruct item : methodAnnotations) {
            if (item.getMethod().equals(method)) {
                return item.getAnnotations();
            }
        }

        return null;
    }

    /**
     * Gets the parameter annotations for a given method, if any. This is
     * meant for use by debugging / dumping code.
     *
     * @param method {@code non-null;} the method
     * @return {@code null-ok;} the parameter annotations, if any
     */
    public AnnotationsList getParameterAnnotations(CstMethodRef method) {
        if (parameterAnnotations == null) {
            return null;
        }

        for (ParameterAnnotationStruct item : parameterAnnotations) {
            if (item.getMethod().equals(method)) {
                return item.getAnnotationsList();
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addContents(DexFile file) {
        MixedItemSection wordData = file.getWordData();

        if (classAnnotations != null) {
            classAnnotations = wordData.intern(classAnnotations);
        }

        if (fieldAnnotations != null) {
            for (FieldAnnotationStruct item : fieldAnnotations) {
                item.addContents(file);
            }
        }

        if (methodAnnotations != null) {
            for (MethodAnnotationStruct item : methodAnnotations) {
                item.addContents(file);
            }
        }

        if (parameterAnnotations != null) {
            for (ParameterAnnotationStruct item : parameterAnnotations) {
                item.addContents(file);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        throw new RuntimeException("unsupported");
    }

    /** {@inheritDoc} */
    @Override
    protected void place0(Section addedTo, int offset) {
        // We just need to set the write size here.

        int elementCount = listSize(fieldAnnotations)
            + listSize(methodAnnotations) + listSize(parameterAnnotations);
        setWriteSize(HEADER_SIZE + (elementCount * ELEMENT_SIZE));
    }

    /** {@inheritDoc} */
    @Override
    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        boolean annotates = out.annotates();
        int classOff = OffsettedItem.getAbsoluteOffsetOr0(classAnnotations);
        int fieldsSize = listSize(fieldAnnotations);
        int methodsSize = listSize(methodAnnotations);
        int parametersSize = listSize(parameterAnnotations);

        if (annotates) {
            out.annotate(0, offsetString() + " annotations directory");
            out.annotate(4, "  class_annotations_off: " + Hex.u4(classOff));
            out.annotate(4, "  fields_size:           " +
                    Hex.u4(fieldsSize));
            out.annotate(4, "  methods_size:          " +
                    Hex.u4(methodsSize));
            out.annotate(4, "  parameters_size:       " +
                    Hex.u4(parametersSize));
        }

        out.writeInt(classOff);
        out.writeInt(fieldsSize);
        out.writeInt(methodsSize);
        out.writeInt(parametersSize);

        if (fieldsSize != 0) {
            Collections.sort(fieldAnnotations);
            if (annotates) {
                out.annotate(0, "  fields:");
            }
            for (FieldAnnotationStruct item : fieldAnnotations) {
                item.writeTo(file, out);
            }
        }

        if (methodsSize != 0) {
            Collections.sort(methodAnnotations);
            if (annotates) {
                out.annotate(0, "  methods:");
            }
            for (MethodAnnotationStruct item : methodAnnotations) {
                item.writeTo(file, out);
            }
        }

        if (parametersSize != 0) {
            Collections.sort(parameterAnnotations);
            if (annotates) {
                out.annotate(0, "  parameters:");
            }
            for (ParameterAnnotationStruct item : parameterAnnotations) {
                item.writeTo(file, out);
            }
        }
    }

    /**
     * Gets the list size of the given list, or {@code 0} if given
     * {@code null}.
     *
     * @param list {@code null-ok;} the list in question
     * @return {@code >= 0;} its size
     */
    private static int listSize(ArrayList<?> list) {
        if (list == null) {
            return 0;
        }

        return list.size();
    }

    /**
     * Prints out the contents of this instance, in a debugging-friendly
     * way. This is meant to be called from {@link ClassDefItem#debugPrint}.
     *
     * @param out {@code non-null;} where to output to
     */
    /*package*/ void debugPrint(PrintWriter out) {
        if (classAnnotations != null) {
            out.println("  class annotations: " + classAnnotations);
        }

        if (fieldAnnotations != null) {
            out.println("  field annotations:");
            for (FieldAnnotationStruct item : fieldAnnotations) {
                out.println("    " + item.toHuman());
            }
        }

        if (methodAnnotations != null) {
            out.println("  method annotations:");
            for (MethodAnnotationStruct item : methodAnnotations) {
                out.println("    " + item.toHuman());
            }
        }

        if (parameterAnnotations != null) {
            out.println("  parameter annotations:");
            for (ParameterAnnotationStruct item : parameterAnnotations) {
                out.println("    " + item.toHuman());
            }
        }
    }
}

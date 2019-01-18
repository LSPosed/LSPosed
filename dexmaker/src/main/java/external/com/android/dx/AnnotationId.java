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
package external.com.android.dx;

import external.com.android.dx.dex.file.ClassDefItem;
import external.com.android.dx.rop.annotation.Annotation;
import external.com.android.dx.rop.annotation.AnnotationVisibility;
import external.com.android.dx.rop.annotation.Annotations;
import external.com.android.dx.rop.annotation.NameValuePair;
import external.com.android.dx.rop.cst.*;

import java.lang.annotation.ElementType;
import java.util.HashMap;

/**
 * Identifies an annotation on a program element, see {@link java.lang.annotation.ElementType}.
 *
 * Currently it is only targeting Class, Method, Field and Parameter because those are supported by
 * {@link external.com.android.dx.dex.file.AnnotationsDirectoryItem} so far.
 *
 * <p><strong>NOTE:</strong>
 * So far it only supports adding method annotation. The annotation of class, field and parameter
 * will be implemented later.
 *
 * <p><strong>WARNING:</strong>
 * The declared element of an annotation type should either have a default value or be set a value via
 * {@code AnnotationId.set(Element)}. Otherwise it will incur
 * {@link java.lang.annotation.IncompleteAnnotationException} when accessing the annotation element
 * through reflection. The example is as follows:
 * <pre>
 *     {@code @Retention(RetentionPolicy.RUNTIME)}
 *     {@code @Target({ElementType.METHOD})}
 *     {@code @interface MethodAnnotation {
 *                boolean elementBoolean();
 *                // boolean elementBoolean() default false;
 *            }
 *
 *            TypeId<?> GENERATED = TypeId.get("LGenerated;");
 *            MethodId<?, Void> methodId = GENERATED.getMethod(VOID, "call");
 *            Code code = dexMaker.declare(methodId, PUBLIC);
 *            code.returnVoid();
 *
 *            TypeId<MethodAnnotation> annotationTypeId = TypeId.get(MethodAnnotation.class);
 *            AnnotationId<?, MethodAnnotation> annotationId = AnnotationId.get(GENERATED,
 *              annotationTypeId, ElementType.METHOD);
 *
 *            AnnotationId.Element element = new AnnotationId.Element("elementBoolean", true);
 *            annotationId.set(element);
 *            annotationId.addToMethod(dexMaker, methodId);
 *     }
 * </pre>
 *
 * @param <D> the type that declares the program element.
 * @param <V> the annotation type. It should be a known type before compile.
 */
public final class AnnotationId<D, V> {
    private final TypeId<D> declaringType;
    private final TypeId<V> type;
    /** The type of program element to be annotated */
    private final ElementType annotatedElement;
    /** The elements this annotation holds */
    private final HashMap<String, NameValuePair> elements;

    private AnnotationId(TypeId<D> declaringType, TypeId<V> type, ElementType annotatedElement) {
        this.declaringType = declaringType;
        this.type = type;
        this.annotatedElement = annotatedElement;
        this.elements = new HashMap<>();
    }

    /**
     *  Construct an instance. It initially contains no elements.
     *
     * @param declaringType the type declaring the program element.
     * @param type the annotation type.
     * @param annotatedElement the program element type to be annotated.
     * @return an annotation {@code AnnotationId<D,V>} instance.
     */
    public static <D, V> AnnotationId<D, V> get(TypeId<D> declaringType, TypeId<V> type,
                                                ElementType annotatedElement) {
        if (annotatedElement != ElementType.TYPE &&
                annotatedElement != ElementType.METHOD &&
                annotatedElement != ElementType.FIELD &&
                annotatedElement != ElementType.PARAMETER) {
            throw new IllegalArgumentException("element type is not supported to annotate yet.");
        }

        return new AnnotationId<>(declaringType, type, annotatedElement);
    }

    /**
     * Set an annotation element of this instance.
     * If there is a preexisting element with the same name, it will be
     * replaced by this method.
     *
     * @param element {@code non-null;} the annotation element to be set.
     */
    public void set(Element element) {
        if (element == null) {
            throw new NullPointerException("element == null");
        }

        CstString pairName = new CstString(element.getName());
        Constant pairValue = Element.toConstant(element.getValue());
        NameValuePair nameValuePair = new NameValuePair(pairName, pairValue);
        elements.put(element.getName(), nameValuePair);
    }

    /**
     * Add this annotation to a method.
     *
     * @param dexMaker DexMaker instance.
     * @param method Method to be added to.
     */
    public void addToMethod(DexMaker dexMaker, MethodId<?, ?> method) {
        if (annotatedElement != ElementType.METHOD) {
            throw new IllegalStateException("This annotation is not for method");
        }

        if (!method.declaringType.equals(declaringType)) {
            throw new IllegalArgumentException("Method" + method + "'s declaring type is inconsistent with" + this);
        }

        ClassDefItem classDefItem = dexMaker.getTypeDeclaration(declaringType).toClassDefItem();

        if (classDefItem == null) {
            throw new NullPointerException("No class defined item is found");
        } else {
            CstMethodRef cstMethodRef = method.constant;

            if (cstMethodRef == null) {
                throw new NullPointerException("Method reference is NULL");
            } else {
                // Generate CstType
                CstType cstType = CstType.intern(type.ropType);

                // Generate Annotation
                Annotation annotation = new Annotation(cstType, AnnotationVisibility.RUNTIME);

                // Add generated annotation
                Annotations annotations = new Annotations();
                for (NameValuePair nvp : elements.values()) {
                    annotation.add(nvp);
                }
                annotations.add(annotation);
                classDefItem.addMethodAnnotations(cstMethodRef, annotations, dexMaker.getDexFile());
            }
        }
    }

    /**
     *  A wrapper of <code>NameValuePair</code> represents a (name, value) pair used as the contents
     *  of an annotation.
     *
     *  An {@code Element} instance is stored in {@code AnnotationId.elements} by calling {@code
     *  AnnotationId.set(Element)}.
     *
     *  <p><strong>WARNING: </strong></p>
     *  the name should be exact same as the annotation element declared in the annotation type
     *  which is referred by field {@code AnnotationId.type},otherwise the annotation will fail
     *  to add and {@code java.lang.reflect.Method.getAnnotations()} will return nothing.
     *
     */
    public static final class Element {
        /** {@code non-null;} the name */
        private final String name;
        /** {@code non-null;} the value */
        private final Object value;

        /**
         * Construct an instance.
         *
         * @param name {@code non-null;} the name
         * @param value {@code non-null;} the value
         */
        public Element(String name, Object value) {
            if (name == null) {
                throw new NullPointerException("name == null");
            }

            if (value == null) {
                throw new NullPointerException("value == null");
            }
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "[" + name + ", " + value + "]";
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return name.hashCode() * 31 + value.hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object other) {
            if (! (other instanceof Element)) {
                return false;
            }

            Element otherElement = (Element) other;

            return name.equals(otherElement.name)
                    && value.equals(otherElement.value);
        }

        /**
         *  Convert a value of an element to a {@code Constant}.
         *  <p><strong>Warning:</strong> Array or TypeId value is not supported yet.
         *
         * @param value an annotation element value.
         * @return a Constant
         */
        static Constant toConstant(Object value) {
            Class clazz = value.getClass();
            if (clazz.isEnum()) {
                CstString descriptor = new CstString(TypeId.get(clazz).getName());
                CstString name = new CstString(((Enum)value).name());
                CstNat cstNat = new CstNat(name, descriptor);
                return new CstEnumRef(cstNat);
            } else if (clazz.isArray()) {
                throw new UnsupportedOperationException("Array is not supported yet");
            } else if (value instanceof TypeId) {
                throw new UnsupportedOperationException("TypeId is not supported yet");
            } else {
                return  Constants.getConstant(value);
            }
        }
    }
}

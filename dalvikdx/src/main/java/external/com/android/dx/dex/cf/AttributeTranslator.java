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

package external.com.android.dx.dex.cf;

import external.com.android.dx.cf.attrib.AttAnnotationDefault;
import external.com.android.dx.cf.attrib.AttEnclosingMethod;
import external.com.android.dx.cf.attrib.AttExceptions;
import external.com.android.dx.cf.attrib.AttInnerClasses;
import external.com.android.dx.cf.attrib.AttRuntimeInvisibleAnnotations;
import external.com.android.dx.cf.attrib.AttRuntimeInvisibleParameterAnnotations;
import external.com.android.dx.cf.attrib.AttRuntimeVisibleAnnotations;
import external.com.android.dx.cf.attrib.AttRuntimeVisibleParameterAnnotations;
import external.com.android.dx.cf.attrib.AttSignature;
import external.com.android.dx.cf.attrib.AttSourceDebugExtension;
import external.com.android.dx.cf.attrib.InnerClassList;
import external.com.android.dx.cf.direct.DirectClassFile;
import external.com.android.dx.cf.iface.AttributeList;
import external.com.android.dx.cf.iface.Method;
import external.com.android.dx.cf.iface.MethodList;
import external.com.android.dx.dex.file.AnnotationUtils;
import external.com.android.dx.rop.annotation.Annotation;
import external.com.android.dx.rop.annotation.AnnotationVisibility;
import external.com.android.dx.rop.annotation.Annotations;
import external.com.android.dx.rop.annotation.AnnotationsList;
import external.com.android.dx.rop.annotation.NameValuePair;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstNat;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.Warning;
import java.util.ArrayList;

/**
 * Utility methods that translate various classfile attributes
 * into forms suitable for use in creating {@code dex} files.
 */
/*package*/ class AttributeTranslator {
    /**
     * This class is uninstantiable.
     */
    private AttributeTranslator() {
        // This space intentionally left blank.
    }

    /**
     * Gets the list of thrown exceptions for a given method.
     *
     * @param method {@code non-null;} the method in question
     * @return {@code non-null;} the list of thrown exceptions
     */
    public static TypeList getExceptions(Method method) {
        AttributeList attribs = method.getAttributes();
        AttExceptions exceptions = (AttExceptions)
            attribs.findFirst(AttExceptions.ATTRIBUTE_NAME);

        if (exceptions == null) {
            return StdTypeList.EMPTY;
        }

        return exceptions.getExceptions();
    }

    /**
     * Gets the annotations out of a given {@link AttributeList}. This
     * combines both visible and invisible annotations into a single
     * result set and also adds in a system annotation for the
     * {@code Signature} attribute if present.
     *
     * @param attribs {@code non-null;} the attributes list to search in
     * @return {@code non-null;} the set of annotations, which may be empty
     */
    public static Annotations getAnnotations(AttributeList attribs) {
        Annotations result = getAnnotations0(attribs);
        Annotation signature = getSignature(attribs);
        Annotation sourceDebugExtension = getSourceDebugExtension(attribs);

        if (signature != null) {
            result = Annotations.combine(result, signature);
        }

        if (sourceDebugExtension != null) {
            result = Annotations.combine(result, sourceDebugExtension);
        }

        return result;
    }

    /**
     * Gets the annotations out of a given class, similar to {@link
     * #getAnnotations}, also including annotations for translations
     * of class-level attributes {@code EnclosingMethod} and
     * {@code InnerClasses}, if present. Additionally, if the
     * class is an annotation class, then this also includes a
     * representation of all the {@code AnnotationDefault}
     * values.
     *
     * @param cf {@code non-null;} the class in question
     * @param args {@code non-null;} the high-level options
     * @return {@code non-null;} the set of annotations, which may be empty
     */
    public static Annotations getClassAnnotations(DirectClassFile cf,
            CfOptions args) {
        CstType thisClass = cf.getThisClass();
        AttributeList attribs = cf.getAttributes();
        Annotations result = getAnnotations(attribs);
        Annotation enclosingMethod = translateEnclosingMethod(attribs);

        try {
            Annotations innerClassAnnotations =
                translateInnerClasses(thisClass, attribs,
                        enclosingMethod == null);
            if (innerClassAnnotations != null) {
                result = Annotations.combine(result, innerClassAnnotations);
            }
        } catch (Warning warn) {
            args.warn.println("warning: " + warn.getMessage());
        }

        if (enclosingMethod != null) {
            result = Annotations.combine(result, enclosingMethod);
        }

        if (AccessFlags.isAnnotation(cf.getAccessFlags())) {
            Annotation annotationDefault =
                translateAnnotationDefaults(cf);
            if (annotationDefault != null) {
                result = Annotations.combine(result, annotationDefault);
            }
        }

        return result;
    }

    /**
     * Gets the annotations out of a given method, similar to {@link
     * #getAnnotations}, also including an annotation for the translation
     * of the method-specific attribute {@code Exceptions}.
     *
     * @param method {@code non-null;} the method in question
     * @return {@code non-null;} the set of annotations, which may be empty
     */
    public static Annotations getMethodAnnotations(Method method) {
        Annotations result = getAnnotations(method.getAttributes());
        TypeList exceptions = getExceptions(method);

        if (exceptions.size() != 0) {
            Annotation throwsAnnotation =
                AnnotationUtils.makeThrows(exceptions);
            result = Annotations.combine(result, throwsAnnotation);
        }

        return result;
    }

    /**
     * Helper method for {@link #getAnnotations} which just gets the
     * existing annotations, per se.
     *
     * @param attribs {@code non-null;} the attributes list to search in
     * @return {@code non-null;} the set of annotations, which may be empty
     */
    private static Annotations getAnnotations0(AttributeList attribs) {
        AttRuntimeVisibleAnnotations visible =
            (AttRuntimeVisibleAnnotations)
            attribs.findFirst(AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME);
        AttRuntimeInvisibleAnnotations invisible =
            (AttRuntimeInvisibleAnnotations)
            attribs.findFirst(AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME);

        if (visible == null) {
            if (invisible == null) {
                return Annotations.EMPTY;
            }
            return invisible.getAnnotations();
        }

        if (invisible == null) {
            return visible.getAnnotations();
        }

        // Both are non-null, so combine them.

        return Annotations.combine(visible.getAnnotations(),
                invisible.getAnnotations());
    }

    /**
     * Gets the {@code Signature} attribute out of a given
     * {@link AttributeList}, if any, translating it to an annotation.
     *
     * @param attribs {@code non-null;} the attributes list to search in
     * @return {@code null-ok;} the converted {@code Signature} annotation,
     * if there was an attribute to translate
     */
    private static Annotation getSignature(AttributeList attribs) {
        AttSignature signature = (AttSignature)
            attribs.findFirst(AttSignature.ATTRIBUTE_NAME);

        if (signature == null) {
            return null;
        }

        return AnnotationUtils.makeSignature(signature.getSignature());
    }


    private static Annotation getSourceDebugExtension(AttributeList attribs) {
        AttSourceDebugExtension extension = (AttSourceDebugExtension)
            attribs.findFirst(AttSourceDebugExtension.ATTRIBUTE_NAME);

        if (extension == null) {
            return null;
        }

        return AnnotationUtils.makeSourceDebugExtension(extension.getSmapString());
    }

    /**
     * Gets the {@code EnclosingMethod} attribute out of a given
     * {@link AttributeList}, if any, translating it to an annotation.
     * If the class really has an enclosing method, this returns an
     * {@code EnclosingMethod} annotation; if not, this returns
     * an {@code EnclosingClass} annotation.
     *
     * @param attribs {@code non-null;} the attributes list to search in
     * @return {@code null-ok;} the converted {@code EnclosingMethod} or
     * {@code EnclosingClass} annotation, if there was an
     * attribute to translate
     */
    private static Annotation translateEnclosingMethod(AttributeList attribs) {
        AttEnclosingMethod enclosingMethod = (AttEnclosingMethod)
            attribs.findFirst(AttEnclosingMethod.ATTRIBUTE_NAME);

        if (enclosingMethod == null) {
            return null;
        }

        CstType enclosingClass = enclosingMethod.getEnclosingClass();
        CstNat nat = enclosingMethod.getMethod();

        if (nat == null) {
            /*
             * Dalvik doesn't use EnclosingMethod annotations unless
             * there really is an enclosing method. Anonymous classes
             * are unambiguously identified by having an InnerClass
             * annotation with an empty name along with an appropriate
             * EnclosingClass.
             */
            return AnnotationUtils.makeEnclosingClass(enclosingClass);
        }

        return AnnotationUtils.makeEnclosingMethod(
                new CstMethodRef(enclosingClass, nat));
    }

    /**
     * Gets the {@code InnerClasses} attribute out of a given
     * {@link AttributeList}, if any, translating it to one or more of an
     * {@code InnerClass}, {@code EnclosingClass}, or
     * {@code MemberClasses} annotation.
     *
     * @param thisClass {@code non-null;} type representing the class being
     * processed
     * @param attribs {@code non-null;} the attributes list to search in
     * @param needEnclosingClass whether to include an
     * {@code EnclosingClass} annotation
     * @return {@code null-ok;} the converted list of annotations, if there
     * was an attribute to translate
     */
    private static Annotations translateInnerClasses(CstType thisClass,
            AttributeList attribs, boolean needEnclosingClass) {
        AttInnerClasses innerClasses = (AttInnerClasses)
            attribs.findFirst(AttInnerClasses.ATTRIBUTE_NAME);

        if (innerClasses == null) {
            return null;
        }

        /*
         * Search the list for the element representing the current class
         * as well as for any named member classes.
         */

        InnerClassList list = innerClasses.getInnerClasses();
        int size = list.size();
        InnerClassList.Item foundThisClass = null;
        ArrayList<Type> membersList = new ArrayList<Type>();

        for (int i = 0; i < size; i++) {
            InnerClassList.Item item = list.get(i);
            CstType innerClass = item.getInnerClass();
            if (innerClass.equals(thisClass)) {
                foundThisClass = item;
            } else if (thisClass.equals(item.getOuterClass())) {
                membersList.add(innerClass.getClassType());
            }
        }

        int membersSize = membersList.size();

        if ((foundThisClass == null) && (membersSize == 0)) {
            return null;
        }

        Annotations result = new Annotations();

        if (foundThisClass != null) {
            result.add(AnnotationUtils.makeInnerClass(
                               foundThisClass.getInnerName(),
                               foundThisClass.getAccessFlags()));
            if (needEnclosingClass) {
                CstType outer = foundThisClass.getOuterClass();
                if (outer == null) {
                    throw new Warning(
                            "Ignoring InnerClasses attribute for an " +
                            "anonymous inner class\n" +
                            "(" + thisClass.toHuman() +
                            ") that doesn't come with an\n" +
                            "associated EnclosingMethod attribute. " +
                            "This class was probably produced by a\n" +
                            "compiler that did not target the modern " +
                            ".class file format. The recommended\n" +
                            "solution is to recompile the class from " +
                            "source, using an up-to-date compiler\n" +
                            "and without specifying any \"-target\" type " +
                            "options. The consequence of ignoring\n" +
                            "this warning is that reflective operations " +
                            "on this class will incorrectly\n" +
                            "indicate that it is *not* an inner class.");
                }
                result.add(AnnotationUtils.makeEnclosingClass(
                                   foundThisClass.getOuterClass()));
            }
        }

        if (membersSize != 0) {
            StdTypeList typeList = new StdTypeList(membersSize);
            for (int i = 0; i < membersSize; i++) {
                typeList.set(i, membersList.get(i));
            }
            typeList.setImmutable();
            result.add(AnnotationUtils.makeMemberClasses(typeList));
        }

        result.setImmutable();
        return result;
    }

    /**
     * Gets the parameter annotations out of a given method. This
     * combines both visible and invisible annotations into a single
     * result set.
     *
     * @param method {@code non-null;} the method in question
     * @return {@code non-null;} the list of annotation sets, which may be
     * empty
     */
    public static AnnotationsList getParameterAnnotations(Method method) {
        AttributeList attribs = method.getAttributes();
        AttRuntimeVisibleParameterAnnotations visible =
            (AttRuntimeVisibleParameterAnnotations)
            attribs.findFirst(
                    AttRuntimeVisibleParameterAnnotations.ATTRIBUTE_NAME);
        AttRuntimeInvisibleParameterAnnotations invisible =
            (AttRuntimeInvisibleParameterAnnotations)
            attribs.findFirst(
                    AttRuntimeInvisibleParameterAnnotations.ATTRIBUTE_NAME);

        if (visible == null) {
            if (invisible == null) {
                return AnnotationsList.EMPTY;
            }
            return invisible.getParameterAnnotations();
        }

        if (invisible == null) {
            return visible.getParameterAnnotations();
        }

        // Both are non-null, so combine them.

        return AnnotationsList.combine(visible.getParameterAnnotations(),
                invisible.getParameterAnnotations());
    }

    /**
     * Gets the {@code AnnotationDefault} attributes out of a
     * given class, if any, reforming them as an
     * {@code AnnotationDefault} annotation.
     *
     * @param cf {@code non-null;} the class in question
     * @return {@code null-ok;} an appropriately-constructed
     * {@code AnnotationDefault} annotation, if there were any
     * annotation defaults in the class, or {@code null} if not
     */
    private static Annotation translateAnnotationDefaults(DirectClassFile cf) {
        CstType thisClass = cf.getThisClass();
        MethodList methods = cf.getMethods();
        int sz = methods.size();
        Annotation result =
            new Annotation(thisClass, AnnotationVisibility.EMBEDDED);
        boolean any = false;

        for (int i = 0; i < sz; i++) {
            Method one = methods.get(i);
            AttributeList attribs = one.getAttributes();
            AttAnnotationDefault oneDefault = (AttAnnotationDefault)
                attribs.findFirst(AttAnnotationDefault.ATTRIBUTE_NAME);

            if (oneDefault != null) {
                NameValuePair pair = new NameValuePair(
                        one.getNat().getName(),
                        oneDefault.getValue());
                result.add(pair);
                any = true;
            }
        }

        if (! any) {
            return null;
        }

        result.setImmutable();
        return AnnotationUtils.makeAnnotationDefault(result);
    }
}

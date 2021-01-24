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

import external.com.android.dx.rop.annotation.Annotation;
import static external.com.android.dx.rop.annotation.AnnotationVisibility.SYSTEM;
import external.com.android.dx.rop.annotation.NameValuePair;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstAnnotation;
import external.com.android.dx.rop.cst.CstArray;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.cst.CstKnownNull;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import java.util.ArrayList;

/**
 * Utility class for dealing with annotations.
 */
public final class AnnotationUtils {

    /** {@code non-null;} type for {@code AnnotationDefault} annotations */
    private static final CstType ANNOTATION_DEFAULT_TYPE =
        CstType.intern(Type.intern("Ldalvik/annotation/AnnotationDefault;"));

    /** {@code non-null;} type for {@code EnclosingClass} annotations */
    private static final CstType ENCLOSING_CLASS_TYPE =
        CstType.intern(Type.intern("Ldalvik/annotation/EnclosingClass;"));

    /** {@code non-null;} type for {@code EnclosingMethod} annotations */
    private static final CstType ENCLOSING_METHOD_TYPE =
        CstType.intern(Type.intern("Ldalvik/annotation/EnclosingMethod;"));

    /** {@code non-null;} type for {@code InnerClass} annotations */
    private static final CstType INNER_CLASS_TYPE =
        CstType.intern(Type.intern("Ldalvik/annotation/InnerClass;"));

    /** {@code non-null;} type for {@code MemberClasses} annotations */
    private static final CstType MEMBER_CLASSES_TYPE =
        CstType.intern(Type.intern("Ldalvik/annotation/MemberClasses;"));

    /** {@code non-null;} type for {@code Signature} annotations */
    private static final CstType SIGNATURE_TYPE =
        CstType.intern(Type.intern("Ldalvik/annotation/Signature;"));

        /** {@code non-null;} type for {@code SourceDebugExtension} annotations */
    private static final CstType SOURCE_DEBUG_EXTENSION_TYPE =
        CstType.intern(Type.intern("Ldalvik/annotation/SourceDebugExtension;"));

    /** {@code non-null;} type for {@code Throws} annotations */
    private static final CstType THROWS_TYPE =
        CstType.intern(Type.intern("Ldalvik/annotation/Throws;"));

    /** {@code non-null;} the UTF-8 constant {@code "accessFlags"} */
    private static final CstString ACCESS_FLAGS_STRING = new CstString("accessFlags");

    /** {@code non-null;} the UTF-8 constant {@code "name"} */
    private static final CstString NAME_STRING = new CstString("name");

    /** {@code non-null;} the UTF-8 constant {@code "value"} */
    private static final CstString VALUE_STRING = new CstString("value");

    /**
     * This class is uninstantiable.
     */
    private AnnotationUtils() {
        // This space intentionally left blank.
    }

    /**
     * Constructs a standard {@code AnnotationDefault} annotation.
     *
     * @param defaults {@code non-null;} the defaults, itself as an annotation
     * @return {@code non-null;} the constructed annotation
     */
    public static Annotation makeAnnotationDefault(Annotation defaults) {
        Annotation result = new Annotation(ANNOTATION_DEFAULT_TYPE, SYSTEM);

        result.put(new NameValuePair(VALUE_STRING, new CstAnnotation(defaults)));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code EnclosingClass} annotation.
     *
     * @param clazz {@code non-null;} the enclosing class
     * @return {@code non-null;} the annotation
     */
    public static Annotation makeEnclosingClass(CstType clazz) {
        Annotation result = new Annotation(ENCLOSING_CLASS_TYPE, SYSTEM);

        result.put(new NameValuePair(VALUE_STRING, clazz));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code EnclosingMethod} annotation.
     *
     * @param method {@code non-null;} the enclosing method
     * @return {@code non-null;} the annotation
     */
    public static Annotation makeEnclosingMethod(CstMethodRef method) {
        Annotation result = new Annotation(ENCLOSING_METHOD_TYPE, SYSTEM);

        result.put(new NameValuePair(VALUE_STRING, method));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code InnerClass} annotation.
     *
     * @param name {@code null-ok;} the original name of the class, or
     * {@code null} to represent an anonymous class
     * @param accessFlags the original access flags
     * @return {@code non-null;} the annotation
     */
    public static Annotation makeInnerClass(CstString name, int accessFlags) {
        Annotation result = new Annotation(INNER_CLASS_TYPE, SYSTEM);
        Constant nameCst = (name != null) ? name : CstKnownNull.THE_ONE;

        result.put(new NameValuePair(NAME_STRING, nameCst));
        result.put(new NameValuePair(ACCESS_FLAGS_STRING,
                        CstInteger.make(accessFlags)));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code MemberClasses} annotation.
     *
     * @param types {@code non-null;} the list of (the types of) the member classes
     * @return {@code non-null;} the annotation
     */
    public static Annotation makeMemberClasses(TypeList types) {
        CstArray array = makeCstArray(types);
        Annotation result = new Annotation(MEMBER_CLASSES_TYPE, SYSTEM);
        result.put(new NameValuePair(VALUE_STRING, array));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code Signature} annotation.
     *
     * @param signature {@code non-null;} the signature string
     * @return {@code non-null;} the annotation
     */
    public static Annotation makeSignature(CstString signature) {
        Annotation result = new Annotation(SIGNATURE_TYPE, SYSTEM);

        /*
         * Split the string into pieces that are likely to be common
         * across many signatures and the rest of the file.
         */

        String raw = signature.getString();
        int rawLength = raw.length();
        ArrayList<String> pieces = new ArrayList<String>(20);

        for (int at = 0; at < rawLength; /*at*/) {
            char c = raw.charAt(at);
            int endAt = at + 1;
            if (c == 'L') {
                // Scan to ';' or '<'. Consume ';' but not '<'.
                while (endAt < rawLength) {
                    c = raw.charAt(endAt);
                    if (c == ';') {
                        endAt++;
                        break;
                    } else if (c == '<') {
                        break;
                    }
                    endAt++;
                }
            } else {
                // Scan to 'L' without consuming it.
                while (endAt < rawLength) {
                    c = raw.charAt(endAt);
                    if (c == 'L') {
                        break;
                    }
                    endAt++;
                }
            }

            pieces.add(raw.substring(at, endAt));
            at = endAt;
        }

        int size = pieces.size();
        CstArray.List list = new CstArray.List(size);

        for (int i = 0; i < size; i++) {
            list.set(i, new CstString(pieces.get(i)));
        }

        list.setImmutable();

        result.put(new NameValuePair(VALUE_STRING, new CstArray(list)));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code SourceDebugExtension} annotation.
     *
     * @param smapString {@code non-null;} the SMAP string associated with
     * @return {@code non-null;} the annotation
     */
    public static Annotation makeSourceDebugExtension(CstString smapString) {
        Annotation result = new Annotation(SOURCE_DEBUG_EXTENSION_TYPE, SYSTEM);

        result.put(new NameValuePair(VALUE_STRING, smapString));
        result.setImmutable();
        return result;
    }

    /**
     * Constructs a standard {@code Throws} annotation.
     *
     * @param types {@code non-null;} the list of thrown types
     * @return {@code non-null;} the annotation
     */
    public static Annotation makeThrows(TypeList types) {
        CstArray array = makeCstArray(types);
        Annotation result = new Annotation(THROWS_TYPE, SYSTEM);
        result.put(new NameValuePair(VALUE_STRING, array));
        result.setImmutable();
        return result;
    }

    /**
     * Converts a {@link TypeList} to a {@link CstArray}.
     *
     * @param types {@code non-null;} the type list
     * @return {@code non-null;} the corresponding array constant
     */
    private static CstArray makeCstArray(TypeList types) {
        int size = types.size();
        CstArray.List list = new CstArray.List(size);

        for (int i = 0; i < size; i++) {
            list.set(i, CstType.intern(types.getType(i)));
        }

        list.setImmutable();
        return new CstArray(list);
    }
}

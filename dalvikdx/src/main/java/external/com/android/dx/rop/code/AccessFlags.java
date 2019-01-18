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

package external.com.android.dx.rop.code;

import external.com.android.dx.util.Hex;

/**
 * Constants used as "access flags" in various places in classes, and
 * related utilities. Although, at the rop layer, flags are generally
 * ignored, this is the layer of communication, and as such, this
 * package is where these definitions belong. The flag definitions are
 * identical to Java access flags, but {@code ACC_SUPER} isn't
 * used at all in translated code, and {@code ACC_SYNCHRONIZED}
 * is only used in a very limited way.
 */
public final class AccessFlags {
    /** public member / class */
    public static final int ACC_PUBLIC = 0x0001;

    /** private member */
    public static final int ACC_PRIVATE = 0x0002;

    /** protected member */
    public static final int ACC_PROTECTED = 0x0004;

    /** static member */
    public static final int ACC_STATIC = 0x0008;

    /** final member / class */
    public static final int ACC_FINAL = 0x0010;

    /**
     * synchronized method; only valid in dex files for {@code native}
     * methods
     */
    public static final int ACC_SYNCHRONIZED = 0x0020;

    /**
     * class with new-style {@code invokespecial} for superclass
     * method access
     */
    public static final int ACC_SUPER = 0x0020;

    /** volatile field */
    public static final int ACC_VOLATILE = 0x0040;

    /** bridge method (generated) */
    public static final int ACC_BRIDGE = 0x0040;

    /** transient field */
    public static final int ACC_TRANSIENT = 0x0080;

    /** varargs method */
    public static final int ACC_VARARGS = 0x0080;

    /** native method */
    public static final int ACC_NATIVE = 0x0100;

    /** "class" is in fact an public static final interface */
    public static final int ACC_INTERFACE = 0x0200;

    /** abstract method / class */
    public static final int ACC_ABSTRACT = 0x0400;

    /**
     * method with strict floating point ({@code strictfp})
     * behavior
     */
    public static final int ACC_STRICT = 0x0800;

    /** synthetic member */
    public static final int ACC_SYNTHETIC = 0x1000;

    /** class is an annotation type */
    public static final int ACC_ANNOTATION = 0x2000;

    /**
     * class is an enumerated type; field is an element of an enumerated
     * type
     */
    public static final int ACC_ENUM = 0x4000;

    /** method is a constructor */
    public static final int ACC_CONSTRUCTOR = 0x10000;

    /**
     * method was declared {@code synchronized}; has no effect on
     * execution (other than inspecting this flag, per se)
     */
    public static final int ACC_DECLARED_SYNCHRONIZED = 0x20000;

    /** flags defined on classes */
    public static final int CLASS_FLAGS =
        ACC_PUBLIC | ACC_FINAL | ACC_SUPER | ACC_INTERFACE | ACC_ABSTRACT |
        ACC_SYNTHETIC | ACC_ANNOTATION | ACC_ENUM;

    /** flags defined on inner classes */
    public static final int INNER_CLASS_FLAGS =
        ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_FINAL |
        ACC_INTERFACE | ACC_ABSTRACT | ACC_SYNTHETIC | ACC_ANNOTATION |
        ACC_ENUM;

    /** flags defined on fields */
    public static final int FIELD_FLAGS =
        ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_FINAL |
        ACC_VOLATILE | ACC_TRANSIENT | ACC_SYNTHETIC | ACC_ENUM;

    /** flags defined on methods */
    public static final int METHOD_FLAGS =
        ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_FINAL |
        ACC_SYNCHRONIZED | ACC_BRIDGE | ACC_VARARGS | ACC_NATIVE |
        ACC_ABSTRACT | ACC_STRICT | ACC_SYNTHETIC | ACC_CONSTRUCTOR |
        ACC_DECLARED_SYNCHRONIZED;

    /** indicates conversion of class flags */
    private static final int CONV_CLASS = 1;

    /** indicates conversion of field flags */
    private static final int CONV_FIELD = 2;

    /** indicates conversion of method flags */
    private static final int CONV_METHOD = 3;

    /**
     * This class is uninstantiable.
     */
    private AccessFlags() {
        // This space intentionally left blank.
    }

    /**
     * Returns a human-oriented string representing the given access flags,
     * as defined on classes (not fields or methods).
     *
     * @param flags the flags
     * @return {@code non-null;} human-oriented string
     */
    public static String classString(int flags) {
        return humanHelper(flags, CLASS_FLAGS, CONV_CLASS);
    }

    /**
     * Returns a human-oriented string representing the given access flags,
     * as defined on inner classes.
     *
     * @param flags the flags
     * @return {@code non-null;} human-oriented string
     */
    public static String innerClassString(int flags) {
        return humanHelper(flags, INNER_CLASS_FLAGS, CONV_CLASS);
    }

    /**
     * Returns a human-oriented string representing the given access flags,
     * as defined on fields (not classes or methods).
     *
     * @param flags the flags
     * @return {@code non-null;} human-oriented string
     */
    public static String fieldString(int flags) {
        return humanHelper(flags, FIELD_FLAGS, CONV_FIELD);
    }

    /**
     * Returns a human-oriented string representing the given access flags,
     * as defined on methods (not classes or fields).
     *
     * @param flags the flags
     * @return {@code non-null;} human-oriented string
     */
    public static String methodString(int flags) {
        return humanHelper(flags, METHOD_FLAGS, CONV_METHOD);
    }

    /**
     * Returns whether the flag {@code ACC_PUBLIC} is on in the given
     * flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_PUBLIC} flag
     */
    public static boolean isPublic(int flags) {
        return (flags & ACC_PUBLIC) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_PROTECTED} is on in the given
     * flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_PROTECTED} flag
     */
    public static boolean isProtected(int flags) {
        return (flags & ACC_PROTECTED) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_PRIVATE} is on in the given
     * flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_PRIVATE} flag
     */
    public static boolean isPrivate(int flags) {
        return (flags & ACC_PRIVATE) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_STATIC} is on in the given
     * flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_STATIC} flag
     */
    public static boolean isStatic(int flags) {
        return (flags & ACC_STATIC) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_CONSTRUCTOR} is on in
     * the given flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_CONSTRUCTOR} flag
     */
    public static boolean isConstructor(int flags) {
        return (flags & ACC_CONSTRUCTOR) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_INTERFACE} is on in
     * the given flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_INTERFACE} flag
     */
    public static boolean isInterface(int flags) {
        return (flags & ACC_INTERFACE) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_SYNCHRONIZED} is on in
     * the given flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_SYNCHRONIZED} flag
     */
    public static boolean isSynchronized(int flags) {
        return (flags & ACC_SYNCHRONIZED) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_ABSTRACT} is on in the given
     * flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_ABSTRACT} flag
     */
    public static boolean isAbstract(int flags) {
        return (flags & ACC_ABSTRACT) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_NATIVE} is on in the given
     * flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_NATIVE} flag
     */
    public static boolean isNative(int flags) {
        return (flags & ACC_NATIVE) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_ANNOTATION} is on in the given
     * flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_ANNOTATION} flag
     */
    public static boolean isAnnotation(int flags) {
        return (flags & ACC_ANNOTATION) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_DECLARED_SYNCHRONIZED} is
     * on in the given flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_DECLARED_SYNCHRONIZED} flag
     */
    public static boolean isDeclaredSynchronized(int flags) {
        return (flags & ACC_DECLARED_SYNCHRONIZED) != 0;
    }

    /**
     * Returns whether the flag {@code ACC_ENUM} is on in the given flags.
     *
     * @param flags the flags to check
     * @return the value of the {@code ACC_ENUM} flag
     */
    public static boolean isEnum(int flags) {
        return (flags & ACC_ENUM) != 0;
    }

    /**
     * Helper to return a human-oriented string representing the given
     * access flags.
     *
     * @param flags the defined flags
     * @param mask mask for the "defined" bits
     * @param what what the flags represent (one of {@code CONV_*})
     * @return {@code non-null;} human-oriented string
     */
    private static String humanHelper(int flags, int mask, int what) {
        StringBuilder sb = new StringBuilder(80);
        int extra = flags & ~mask;

        flags &= mask;

        if ((flags & ACC_PUBLIC) != 0) {
            sb.append("|public");
        }
        if ((flags & ACC_PRIVATE) != 0) {
            sb.append("|private");
        }
        if ((flags & ACC_PROTECTED) != 0) {
            sb.append("|protected");
        }
        if ((flags & ACC_STATIC) != 0) {
            sb.append("|static");
        }
        if ((flags & ACC_FINAL) != 0) {
            sb.append("|final");
        }
        if ((flags & ACC_SYNCHRONIZED) != 0) {
            if (what == CONV_CLASS) {
                sb.append("|super");
            } else {
                sb.append("|synchronized");
            }
        }
        if ((flags & ACC_VOLATILE) != 0) {
            if (what == CONV_METHOD) {
                sb.append("|bridge");
            } else {
                sb.append("|volatile");
            }
        }
        if ((flags & ACC_TRANSIENT) != 0) {
            if (what == CONV_METHOD) {
                sb.append("|varargs");
            } else {
                sb.append("|transient");
            }
        }
        if ((flags & ACC_NATIVE) != 0) {
            sb.append("|native");
        }
        if ((flags & ACC_INTERFACE) != 0) {
            sb.append("|interface");
        }
        if ((flags & ACC_ABSTRACT) != 0) {
            sb.append("|abstract");
        }
        if ((flags & ACC_STRICT) != 0) {
            sb.append("|strictfp");
        }
        if ((flags & ACC_SYNTHETIC) != 0) {
            sb.append("|synthetic");
        }
        if ((flags & ACC_ANNOTATION) != 0) {
            sb.append("|annotation");
        }
        if ((flags & ACC_ENUM) != 0) {
            sb.append("|enum");
        }
        if ((flags & ACC_CONSTRUCTOR) != 0) {
            sb.append("|constructor");
        }
        if ((flags & ACC_DECLARED_SYNCHRONIZED) != 0) {
            sb.append("|declared_synchronized");
        }

        if ((extra != 0) || (sb.length() == 0)) {
            sb.append('|');
            sb.append(Hex.u2(extra));
        }

        return sb.substring(1);
    }
}

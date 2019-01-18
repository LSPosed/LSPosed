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

/**
 * The various types that an index in a Dalvik instruction might refer to.
 */
public enum IndexType {
    /** "Unknown." Used for undefined opcodes. */
    UNKNOWN,

    /** no index used */
    NONE,

    /** "It depends." Used for {@code throw-verification-error}. */
    VARIES,

    /** type reference index */
    TYPE_REF,

    /** string reference index */
    STRING_REF,

    /** method reference index */
    METHOD_REF,

    /** field reference index */
    FIELD_REF,

    /** method index and a proto index */
    METHOD_AND_PROTO_REF,

    /** call site reference index */
    CALL_SITE_REF,

    /** inline method index (for inline linked method invocations) */
    INLINE_METHOD,

    /** direct vtable offset (for static linked method invocations) */
    VTABLE_OFFSET,

    /** direct field offset (for static linked field accesses) */
    FIELD_OFFSET,

    /** method handle reference index (for loading constant method handles) */
    METHOD_HANDLE_REF,

    /** proto reference index (for loading constant proto ref) */
    PROTO_REF;
}

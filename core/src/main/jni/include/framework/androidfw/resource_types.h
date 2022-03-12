/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#pragma once

#include <variant>
#include <cstdint>
#include "utils/hook_helper.hpp"

// @ApiSensitive(Level.MIDDLE)
namespace android {

    typedef int32_t status_t;


    template<class E>
    struct unexpected {
        E val_;
    };

    template<class T, class E>
    struct expected {
        using value_type = T;
        using error_type = E;
        using unexpected_type = unexpected<E>;
        std::variant<value_type, unexpected_type> var_;

        constexpr bool has_value() const noexcept { return var_.index() == 0; }

        constexpr const T &value() const &{ return std::get<T>(var_); }

        constexpr T &value() &{ return std::get<T>(var_); }

        constexpr const T *operator->() const { return std::addressof(value()); }

        constexpr T *operator->() { return std::addressof(value()); }
    };

    enum class IOError {
        // Used when reading a file residing on an IncFs file-system times out.
        PAGES_MISSING = -1,
    };

    template<typename TChar>
    struct BasicStringPiece {
        const TChar *data_;
        size_t length_;
    };

    using NullOrIOError = std::variant<std::nullopt_t, IOError>;

    using StringPiece16 = BasicStringPiece<char16_t>;

    enum {
        RES_NULL_TYPE = 0x0000,
        RES_STRING_POOL_TYPE = 0x0001,
        RES_TABLE_TYPE = 0x0002,
        RES_XML_TYPE = 0x0003,
        // Chunk types in RES_XML_TYPE
        RES_XML_FIRST_CHUNK_TYPE = 0x0100,
        RES_XML_START_NAMESPACE_TYPE = 0x0100,
        RES_XML_END_NAMESPACE_TYPE = 0x0101,
        RES_XML_START_ELEMENT_TYPE = 0x0102,
        RES_XML_END_ELEMENT_TYPE = 0x0103,
        RES_XML_CDATA_TYPE = 0x0104,
        RES_XML_LAST_CHUNK_TYPE = 0x017f,
        // This contains a uint32_t array mapping strings in the string
        // pool back to resource identifiers.  It is optional.
        RES_XML_RESOURCE_MAP_TYPE = 0x0180,
        // Chunk types in RES_TABLE_TYPE
        RES_TABLE_PACKAGE_TYPE = 0x0200,
        RES_TABLE_TYPE_TYPE = 0x0201,
        RES_TABLE_TYPE_SPEC_TYPE = 0x0202,
        RES_TABLE_LIBRARY_TYPE = 0x0203
    };

    struct ResXMLTree_node {
        void *header;
        // Line number in original source file at which this element appeared.
        uint32_t lineNumber;
        // Optional XML comment that was associated with this element; -1 if none.
        void *comment;
    };

    class ResXMLTree;

    class ResXMLParser {

    public:
        enum event_code_t {
            BAD_DOCUMENT = -1,
            START_DOCUMENT = 0,
            END_DOCUMENT = 1,

            FIRST_CHUNK_CODE = RES_XML_FIRST_CHUNK_TYPE,

            START_NAMESPACE = RES_XML_START_NAMESPACE_TYPE,
            END_NAMESPACE = RES_XML_END_NAMESPACE_TYPE,
            START_TAG = RES_XML_START_ELEMENT_TYPE,
            END_TAG = RES_XML_END_ELEMENT_TYPE,
            TEXT = RES_XML_CDATA_TYPE
        };

        const ResXMLTree &mTree;
        event_code_t mEventCode;
        const ResXMLTree_node *mCurNode;
        const void *mCurExt;
    };

    class ResStringPool {

    public:
        status_t mError;
        void *mOwnedData;
        const void *mHeader;
        size_t mSize;
        mutable pthread_mutex_t mDecodeLock;
        const uint32_t *mEntries;
        const uint32_t *mEntryStyles;
        const void *mStrings;
        char16_t mutable **mCache;
        uint32_t mStringPoolSize;    // number of uint16_t
        const uint32_t *mStyles;
        uint32_t mStylePoolSize;    // number of uint32_t

        using stringAtRet = expected<StringPiece16, NullOrIOError>;

        CREATE_MEM_FUNC_SYMBOL_ENTRY(stringAtRet, stringAtS, void *thiz, size_t idx) {
            if (stringAtSSym) {
                return stringAtSSym(thiz, idx);
            }
            return {.var_ = unexpected<NullOrIOError>{.val_ = std::nullopt}};

        };

        CREATE_MEM_FUNC_SYMBOL_ENTRY(const char16_t*, stringAt, void *thiz, size_t idx,
                                     size_t *u16len) {
            if (stringAtSym) {
                return stringAtSym(thiz, idx, u16len);
            } else {
                *u16len = 0u;
                return nullptr;
            }
        };

        StringPiece16 stringAt(size_t idx) const {
            if (stringAtSym) {
                size_t len;
                const char16_t *str = stringAt(const_cast<ResStringPool *>(this), idx, &len);
                return {str, len};
            } else if (stringAtSSym) {
                auto str = stringAtS(const_cast<ResStringPool *>(this), idx);
                if (str.has_value()) {
                    return {str->data_, str->length_};
                }
            }
            return {nullptr, 0u};
        }

        static bool setup(const lsplant::HookHandler &handler) {
            RETRIEVE_MEM_FUNC_SYMBOL(stringAt, LP_SELECT("_ZNK7android13ResStringPool8stringAtEjPj", "_ZNK7android13ResStringPool8stringAtEmPm"));
            RETRIEVE_MEM_FUNC_SYMBOL(stringAtS, LP_SELECT("_ZNK7android13ResStringPool8stringAtEj", "_ZNK7android13ResStringPool8stringAtEm"));
            return !stringAtSym || !stringAtSSym;
        }
    };


    class ResXMLTree : public ResXMLParser {

    public:
        void *mDynamicRefTable;
        status_t mError;
        void *mOwnedData;
        const void *mHeader;
        size_t mSize;
        const uint8_t *mDataEnd;
        ResStringPool mStrings;
        const uint32_t *mResIds;
        size_t mNumResIds;
        const ResXMLTree_node *mRootNode;
        const void *mRootExt;
        event_code_t mRootCode;
    };

    struct ResStringPool_ref {

        // Index into the string pool table (uint32_t-offset from the indices
        // immediately after ResStringPool_header) at which to find the location
        // of the string data in the pool.
        uint32_t index;
    };

    struct ResXMLTree_attrExt {

        // String of the full namespace of this element.
        struct ResStringPool_ref ns;

        // String name of this node if it is an ELEMENT; the raw
        // character data if this is a CDATA node.
        struct ResStringPool_ref name;

        // Byte offset from the start of this structure where the attributes start.
        uint16_t attributeStart;

        // Size of the ResXMLTree_attribute structures that follow.
        uint16_t attributeSize;

        // Number of attributes associated with an ELEMENT.  These are
        // available as an array of ResXMLTree_attribute structures
        // immediately following this node.
        uint16_t attributeCount;

        // Index (1-based) of the "id" attribute. 0 if none.
        uint16_t idIndex;

        // Index (1-based) of the "class" attribute. 0 if none.
        uint16_t classIndex;

        // Index (1-based) of the "style" attribute. 0 if none.
        uint16_t styleIndex;
    };

    struct Res_value {

        // Number of bytes in this structure.
        uint16_t size;
        // Always set to 0.
        uint8_t res0;

        // Type of the data value.
        enum : uint8_t {
            // The 'data' is either 0 or 1, specifying this resource is either
            // undefined or empty, respectively.
            TYPE_NULL = 0x00,
            // The 'data' holds a ResTable_ref, a reference to another resource
            // table entry.
            TYPE_REFERENCE = 0x01,
            // The 'data' holds an attribute resource identifier.
            TYPE_ATTRIBUTE = 0x02,
            // The 'data' holds an index into the containing resource table's
            // global value string pool.
            TYPE_STRING = 0x03,
            // The 'data' holds a single-precision floating point number.
            TYPE_FLOAT = 0x04,
            // The 'data' holds a complex number encoding a dimension value,
            // such as "100in".
            TYPE_DIMENSION = 0x05,
            // The 'data' holds a complex number encoding a fraction of a
            // container.
            TYPE_FRACTION = 0x06,
            // The 'data' holds a dynamic ResTable_ref, which needs to be
            // resolved before it can be used like a TYPE_REFERENCE.
            TYPE_DYNAMIC_REFERENCE = 0x07,
            // The 'data' holds an attribute resource identifier, which needs to be resolved
            // before it can be used like a TYPE_ATTRIBUTE.
            TYPE_DYNAMIC_ATTRIBUTE = 0x08,
            // Beginning of integer flavors...
            TYPE_FIRST_INT = 0x10,
            // The 'data' is a raw integer value of the form n..n.
            TYPE_INT_DEC = 0x10,
            // The 'data' is a raw integer value of the form 0xn..n.
            TYPE_INT_HEX = 0x11,
            // The 'data' is either 0 or 1, for input "false" or "true" respectively.
            TYPE_INT_BOOLEAN = 0x12,
            // Beginning of color integer flavors...
            TYPE_FIRST_COLOR_INT = 0x1c,
            // The 'data' is a raw integer value of the form #aarrggbb.
            TYPE_INT_COLOR_ARGB8 = 0x1c,
            // The 'data' is a raw integer value of the form #rrggbb.
            TYPE_INT_COLOR_RGB8 = 0x1d,
            // The 'data' is a raw integer value of the form #argb.
            TYPE_INT_COLOR_ARGB4 = 0x1e,
            // The 'data' is a raw integer value of the form #rgb.
            TYPE_INT_COLOR_RGB4 = 0x1f,
            // ...end of integer flavors.
            TYPE_LAST_COLOR_INT = 0x1f,
            // ...end of integer flavors.
            TYPE_LAST_INT = 0x1f
        };
        uint8_t dataType;
        // Structure of complex data values (TYPE_UNIT and TYPE_FRACTION)
        enum {
            // Where the unit type information is.  This gives us 16 possible
            // types, as defined below.
            COMPLEX_UNIT_SHIFT = 0,
            COMPLEX_UNIT_MASK = 0xf,
            // TYPE_DIMENSION: Value is raw pixels.
            COMPLEX_UNIT_PX = 0,
            // TYPE_DIMENSION: Value is Device Independent Pixels.
            COMPLEX_UNIT_DIP = 1,
            // TYPE_DIMENSION: Value is a Scaled device independent Pixels.
            COMPLEX_UNIT_SP = 2,
            // TYPE_DIMENSION: Value is in points.
            COMPLEX_UNIT_PT = 3,
            // TYPE_DIMENSION: Value is in inches.
            COMPLEX_UNIT_IN = 4,
            // TYPE_DIMENSION: Value is in millimeters.
            COMPLEX_UNIT_MM = 5,
            // TYPE_FRACTION: A basic fraction of the overall size.
            COMPLEX_UNIT_FRACTION = 0,
            // TYPE_FRACTION: A fraction of the parent size.
            COMPLEX_UNIT_FRACTION_PARENT = 1,
            // Where the radix information is, telling where the decimal place
            // appears in the mantissa.  This give us 4 possible fixed point
            // representations as defined below.
            COMPLEX_RADIX_SHIFT = 4,
            COMPLEX_RADIX_MASK = 0x3,
            // The mantissa is an integral number -- i.e., 0xnnnnnn.0
            COMPLEX_RADIX_23p0 = 0,
            // The mantissa magnitude is 16 bits -- i.e, 0xnnnn.nn
            COMPLEX_RADIX_16p7 = 1,
            // The mantissa magnitude is 8 bits -- i.e, 0xnn.nnnn
            COMPLEX_RADIX_8p15 = 2,
            // The mantissa magnitude is 0 bits -- i.e, 0x0.nnnnnn
            COMPLEX_RADIX_0p23 = 3,
            // Where the actual value is.  This gives us 23 bits of
            // precision.  The top bit is the sign.
            COMPLEX_MANTISSA_SHIFT = 8,
            COMPLEX_MANTISSA_MASK = 0xffffff
        };
        // Possible data values for TYPE_NULL.
        enum {
            // The value is not defined.
            DATA_NULL_UNDEFINED = 0,
            // The value is explicitly defined as empty.
            DATA_NULL_EMPTY = 1
        };
        // The data for this item, as interpreted according to dataType.
        typedef uint32_t data_type;
        data_type data;
    };

    struct ResXMLTree_attribute {
        // Namespace of this attribute.
        struct ResStringPool_ref ns;

        // Name of this attribute.
        struct ResStringPool_ref name;

        // The original raw string value of this attribute.
        struct ResStringPool_ref rawValue;

        // Processesd typed value of this attribute.
        struct Res_value typedValue;
    };

}

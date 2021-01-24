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

import external.com.android.dx.util.ToHuman;

/**
 * Enumeration of all the top-level item types.
 */
public enum ItemType implements ToHuman {
    TYPE_HEADER_ITEM(               0x0000, "header_item"),
    TYPE_STRING_ID_ITEM(            0x0001, "string_id_item"),
    TYPE_TYPE_ID_ITEM(              0x0002, "type_id_item"),
    TYPE_PROTO_ID_ITEM(             0x0003, "proto_id_item"),
    TYPE_FIELD_ID_ITEM(             0x0004, "field_id_item"),
    TYPE_METHOD_ID_ITEM(            0x0005, "method_id_item"),
    TYPE_CLASS_DEF_ITEM(            0x0006, "class_def_item"),
    TYPE_CALL_SITE_ID_ITEM(         0x0007, "call_site_id_item"),
    TYPE_METHOD_HANDLE_ITEM(        0x0008, "method_handle_item"),
    TYPE_MAP_LIST(                  0x1000, "map_list"),
    TYPE_TYPE_LIST(                 0x1001, "type_list"),
    TYPE_ANNOTATION_SET_REF_LIST(   0x1002, "annotation_set_ref_list"),
    TYPE_ANNOTATION_SET_ITEM(       0x1003, "annotation_set_item"),
    TYPE_CLASS_DATA_ITEM(           0x2000, "class_data_item"),
    TYPE_CODE_ITEM(                 0x2001, "code_item"),
    TYPE_STRING_DATA_ITEM(          0x2002, "string_data_item"),
    TYPE_DEBUG_INFO_ITEM(           0x2003, "debug_info_item"),
    TYPE_ANNOTATION_ITEM(           0x2004, "annotation_item"),
    TYPE_ENCODED_ARRAY_ITEM(        0x2005, "encoded_array_item"),
    TYPE_ANNOTATIONS_DIRECTORY_ITEM(0x2006, "annotations_directory_item"),
    TYPE_MAP_ITEM(                  -1,     "map_item"),
    TYPE_TYPE_ITEM(                 -1,     "type_item"),
    TYPE_EXCEPTION_HANDLER_ITEM(    -1,     "exception_handler_item"),
    TYPE_ANNOTATION_SET_REF_ITEM(   -1,     "annotation_set_ref_item");

    /** value when represented in a {@link MapItem} */
    private final int mapValue;

    /** {@code non-null;} name of the type */
    private final String typeName;

    /** {@code non-null;} the short human name */
    private final String humanName;

    /**
     * Constructs an instance.
     *
     * @param mapValue value when represented in a {@link MapItem}
     * @param typeName {@code non-null;} name of the type
     */
    private ItemType(int mapValue, String typeName) {
        this.mapValue = mapValue;
        this.typeName = typeName;

        // Make the human name.
        String human = typeName;
        if (human.endsWith("_item")) {
            human = human.substring(0, human.length() - 5);
        }
        this.humanName = human.replace('_', ' ');
    }

    /**
     * Gets the map value.
     *
     * @return the map value
     */
    public int getMapValue() {
        return mapValue;
    }

    /**
     * Gets the type name.
     *
     * @return {@code non-null;} the type name
     */
    public String getTypeName() {
        return typeName;
    }

    /** {@inheritDoc} */
    @Override
    public String toHuman() {
        return humanName;
    }
}

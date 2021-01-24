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

package external.com.android.dx.cf.attrib;

import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.util.FixedSizeList;

/**
 * List of "inner class" entries, which are the contents of
 * {@code InnerClasses} attributes.
 */
public final class InnerClassList extends FixedSizeList {
    /**
     * Constructs an instance.
     *
     * @param count the number of elements to be in the list of inner classes
     */
    public InnerClassList(int count) {
        super(count);
    }

    /**
     * Gets the indicated item.
     *
     * @param n {@code >= 0;} which item
     * @return {@code null-ok;} the indicated item
     */
    public Item get(int n) {
        return (Item) get0(n);
    }

    /**
     * Sets the item at the given index.
     *
     * @param n {@code >= 0, < size();} which class
     * @param innerClass {@code non-null;} class this item refers to
     * @param outerClass {@code null-ok;} outer class that this class is a
     * member of, if any
     * @param innerName {@code null-ok;} original simple name of this class,
     * if not anonymous
     * @param accessFlags original declared access flags
     */
    public void set(int n, CstType innerClass, CstType outerClass,
                    CstString innerName, int accessFlags) {
        set0(n, new Item(innerClass, outerClass, innerName, accessFlags));
    }

    /**
     * Item in an inner classes list.
     */
    public static class Item {
        /** {@code non-null;} class this item refers to */
        private final CstType innerClass;

        /** {@code null-ok;} outer class that this class is a member of, if any */
        private final CstType outerClass;

        /** {@code null-ok;} original simple name of this class, if not anonymous */
        private final CstString innerName;

        /** original declared access flags */
        private final int accessFlags;

        /**
         * Constructs an instance.
         *
         * @param innerClass {@code non-null;} class this item refers to
         * @param outerClass {@code null-ok;} outer class that this class is a
         * member of, if any
         * @param innerName {@code null-ok;} original simple name of this
         * class, if not anonymous
         * @param accessFlags original declared access flags
         */
        public Item(CstType innerClass, CstType outerClass,
                    CstString innerName, int accessFlags) {
            if (innerClass == null) {
                throw new NullPointerException("innerClass == null");
            }

            this.innerClass = innerClass;
            this.outerClass = outerClass;
            this.innerName = innerName;
            this.accessFlags = accessFlags;
        }

        /**
         * Gets the class this item refers to.
         *
         * @return {@code non-null;} the class
         */
        public CstType getInnerClass() {
            return innerClass;
        }

        /**
         * Gets the outer class that this item's class is a member of, if any.
         *
         * @return {@code null-ok;} the class
         */
        public CstType getOuterClass() {
            return outerClass;
        }

        /**
         * Gets the original name of this item's class, if not anonymous.
         *
         * @return {@code null-ok;} the name
         */
        public CstString getInnerName() {
            return innerName;
        }

        /**
         * Gets the original declared access flags.
         *
         * @return the access flags
         */
        public int getAccessFlags() {
            return accessFlags;
        }
    }
}

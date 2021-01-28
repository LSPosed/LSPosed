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
package external.com.android.dx.cf.code;

import external.com.android.dx.rop.cst.CstMethodHandle;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.util.FixedSizeList;

/**
 * List of bootstrap method entries, which are the contents of
 * {@code BootstrapMethods} attributes.
 */
public class BootstrapMethodsList extends FixedSizeList {
    /** {@code non-null;} zero-size instance */
    public static final BootstrapMethodsList EMPTY = new BootstrapMethodsList(0);

    /**
     * Constructs an instance.
     *
     * @param count the number of elements to be in the list
     */
    public BootstrapMethodsList(int count) {
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
     * @param n {@code >= 0, < size();} which element
     * @param item {@code non-null;} the item
     */
    public void set(int n, Item item) {
        if (item == null) {
            throw new NullPointerException("item == null");
        }

        set0(n, item);
    }

    /**
     * Sets the item at the given index.
     *
     * @param n {@code >= 0, < size();} which element
     * @param declaringClass {@code non-null;} the class declaring bootstrap method.
     * @param bootstrapMethodHandle {@code non-null;} the bootstrap method handle
     * @param arguments {@code non-null;} the arguments of the bootstrap method
     */
    public void set(int n, CstType declaringClass, CstMethodHandle bootstrapMethodHandle,
                    BootstrapMethodArgumentsList arguments) {
        set(n, new Item(declaringClass, bootstrapMethodHandle, arguments));
    }

    /**
     * Returns an instance which is the concatenation of the two given
     * instances.
     *
     * @param list1 {@code non-null;} first instance
     * @param list2 {@code non-null;} second instance
     * @return {@code non-null;} combined instance
     */
    public static BootstrapMethodsList concat(BootstrapMethodsList list1,
                                              BootstrapMethodsList list2) {
        if (list1 == EMPTY) {
            return list2;
        } else if (list2 == EMPTY) {
            return list1;
        }

        int sz1 = list1.size();
        int sz2 = list2.size();
        BootstrapMethodsList result = new BootstrapMethodsList(sz1 + sz2);

        for (int i = 0; i < sz1; i++) {
            result.set(i, list1.get(i));
        }

        for (int i = 0; i < sz2; i++) {
            result.set(sz1 + i, list2.get(i));
        }

        return result;
    }

    public static class Item {
        private final BootstrapMethodArgumentsList bootstrapMethodArgumentsList;
        private final CstMethodHandle bootstrapMethodHandle;
        private final CstType declaringClass;

        public Item(CstType declaringClass, CstMethodHandle bootstrapMethodHandle,
                    BootstrapMethodArgumentsList bootstrapMethodArguments) {
            if (declaringClass == null) {
                throw new NullPointerException("declaringClass == null");
            }
            if (bootstrapMethodHandle == null) {
                throw new NullPointerException("bootstrapMethodHandle == null");
            }
            if (bootstrapMethodArguments == null) {
                throw new NullPointerException("bootstrapMethodArguments == null");
            }
            this.bootstrapMethodHandle = bootstrapMethodHandle;
            this.bootstrapMethodArgumentsList = bootstrapMethodArguments;
            this.declaringClass = declaringClass;
        }

        public CstMethodHandle getBootstrapMethodHandle() {
            return bootstrapMethodHandle;
        }

        public BootstrapMethodArgumentsList getBootstrapMethodArguments() {
            return bootstrapMethodArgumentsList;
        }

        public CstType getDeclaringClass() {
            return declaringClass;
        }
    }
}

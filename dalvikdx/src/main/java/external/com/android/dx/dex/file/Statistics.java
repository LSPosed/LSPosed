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

package external.com.android.dx.dex.file;

import external.com.android.dx.util.AnnotatedOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Statistics about the contents of a file.
 */
public final class Statistics {
    /** {@code non-null;} data about each type of item */
    private final HashMap<String, Data> dataMap;

    /**
     * Constructs an instance.
     */
    public Statistics() {
        dataMap = new HashMap<String, Data>(50);
    }

    /**
     * Adds the given item to the statistics.
     *
     * @param item {@code non-null;} the item to add
     */
    public void add(Item item) {
        String typeName = item.typeName();
        Data data = dataMap.get(typeName);

        if (data == null) {
            dataMap.put(typeName, new Data(item, typeName));
        } else {
            data.add(item);
        }
    }

    /**
     * Adds the given list of items to the statistics.
     *
     * @param list {@code non-null;} the list of items to add
     */
    public void addAll(Section list) {
        Collection<? extends Item> items = list.items();
        for (Item item : items) {
            add(item);
        }
    }

    /**
     * Writes the statistics as an annotation.
     *
     * @param out {@code non-null;} where to write to
     */
    public final void writeAnnotation(AnnotatedOutput out) {
        if (dataMap.size() == 0) {
            return;
        }

        out.annotate(0, "\nstatistics:\n");

        TreeMap<String, Data> sortedData = new TreeMap<String, Data>();

        for (Data data : dataMap.values()) {
            sortedData.put(data.name, data);
        }

        for (Data data : sortedData.values()) {
            data.writeAnnotation(out);
        }
    }

    public String toHuman() {
        StringBuilder sb = new StringBuilder();

        sb.append("Statistics:\n");

        TreeMap<String, Data> sortedData = new TreeMap<String, Data>();

        for (Data data : dataMap.values()) {
            sortedData.put(data.name, data);
        }

        for (Data data : sortedData.values()) {
            sb.append(data.toHuman());
        }

        return sb.toString();
    }

    /**
     * Statistical data about a particular class.
     */
    private static class Data {
        /** {@code non-null;} name to use as a label */
        private final String name;

        /** {@code >= 0;} number of instances */
        private int count;

        /** {@code >= 0;} total size of instances in bytes */
        private int totalSize;

        /** {@code >= 0;} largest size of any individual item */
        private int largestSize;

        /** {@code >= 0;} smallest size of any individual item */
        private int smallestSize;

        /**
         * Constructs an instance for the given item.
         *
         * @param item {@code non-null;} item in question
         * @param name {@code non-null;} type name to use
         */
        public Data(Item item, String name) {
            int size = item.writeSize();

            this.name = name;
            this.count = 1;
            this.totalSize = size;
            this.largestSize = size;
            this.smallestSize = size;
        }

        /**
         * Incorporates a new item. This assumes the type name matches.
         *
         * @param item {@code non-null;} item to incorporate
         */
        public void add(Item item) {
            int size = item.writeSize();

            count++;
            totalSize += size;

            if (size > largestSize) {
                largestSize = size;
            }

            if (size < smallestSize) {
                smallestSize = size;
            }
        }

        /**
         * Writes this instance as an annotation.
         *
         * @param out {@code non-null;} where to write to
         */
        public void writeAnnotation(AnnotatedOutput out) {
            out.annotate(toHuman());
        }

        /**
         * Generates a human-readable string for this data item.
         *
         * @return string for human consumption.
         */
        public String toHuman() {
            StringBuilder sb = new StringBuilder();

            sb.append("  " + name + ": " +
                         count + " item" + (count == 1 ? "" : "s") + "; " +
                         totalSize + " bytes total\n");

            if (smallestSize == largestSize) {
                sb.append("    " + smallestSize + " bytes/item\n");
            } else {
                int average = totalSize / count;
                sb.append("    " + smallestSize + ".." + largestSize +
                             " bytes/item; average " + average + "\n");
            }

            return sb.toString();
        }
    }
}

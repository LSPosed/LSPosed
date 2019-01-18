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

package external.com.android.dx.merge;

import external.com.android.dex.Annotation;
import external.com.android.dex.CallSiteId;
import external.com.android.dex.ClassData;
import external.com.android.dex.ClassDef;
import external.com.android.dex.Code;
import external.com.android.dex.Dex;
import external.com.android.dex.DexException;
import external.com.android.dex.DexIndexOverflowException;
import external.com.android.dex.FieldId;
import external.com.android.dex.MethodHandle;
import external.com.android.dex.MethodId;
import external.com.android.dex.ProtoId;
import external.com.android.dex.SizeOf;
import external.com.android.dex.TableOfContents;
import external.com.android.dex.TypeList;
import external.com.android.dx.command.dexer.DxContext;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Combine two dex files into one.
 */
public final class DexMerger {
    private final Dex[] dexes;
    private final IndexMap[] indexMaps;

    private final CollisionPolicy collisionPolicy;
    private final DxContext context;
    private final WriterSizes writerSizes;

    private final Dex dexOut;

    private final Dex.Section headerOut;

    /** All IDs and definitions sections */
    private final Dex.Section idsDefsOut;

    private final Dex.Section mapListOut;

    private final Dex.Section typeListOut;

    private final Dex.Section classDataOut;

    private final Dex.Section codeOut;

    private final Dex.Section stringDataOut;

    private final Dex.Section debugInfoOut;

    private final Dex.Section encodedArrayOut;

    /** annotations directory on a type */
    private final Dex.Section annotationsDirectoryOut;

    /** sets of annotations on a member, parameter or type */
    private final Dex.Section annotationSetOut;

    /** parameter lists */
    private final Dex.Section annotationSetRefListOut;

    /** individual annotations, each containing zero or more fields */
    private final Dex.Section annotationOut;

    private final TableOfContents contentsOut;

    private final InstructionTransformer instructionTransformer;

    /** minimum number of wasted bytes before it's worthwhile to compact the result */
    private int compactWasteThreshold = 1024 * 1024; // 1MiB

    public DexMerger(Dex[] dexes, CollisionPolicy collisionPolicy, DxContext context)
            throws IOException {
        this(dexes, collisionPolicy, context, new WriterSizes(dexes));
    }

    private DexMerger(Dex[] dexes, CollisionPolicy collisionPolicy, DxContext context,
            WriterSizes writerSizes) throws IOException {
        this.dexes = dexes;
        this.collisionPolicy = collisionPolicy;
        this.context = context;
        this.writerSizes = writerSizes;

        dexOut = new Dex(writerSizes.size());

        indexMaps = new IndexMap[dexes.length];
        for (int i = 0; i < dexes.length; i++) {
            indexMaps[i] = new IndexMap(dexOut, dexes[i].getTableOfContents());
        }
        instructionTransformer = new InstructionTransformer();

        headerOut = dexOut.appendSection(writerSizes.header, "header");
        idsDefsOut = dexOut.appendSection(writerSizes.idsDefs, "ids defs");

        contentsOut = dexOut.getTableOfContents();
        contentsOut.dataOff = dexOut.getNextSectionStart();

        contentsOut.mapList.off = dexOut.getNextSectionStart();
        contentsOut.mapList.size = 1;
        mapListOut = dexOut.appendSection(writerSizes.mapList, "map list");

        contentsOut.typeLists.off = dexOut.getNextSectionStart();
        typeListOut = dexOut.appendSection(writerSizes.typeList, "type list");

        contentsOut.annotationSetRefLists.off = dexOut.getNextSectionStart();
        annotationSetRefListOut = dexOut.appendSection(
                writerSizes.annotationsSetRefList, "annotation set ref list");

        contentsOut.annotationSets.off = dexOut.getNextSectionStart();
        annotationSetOut = dexOut.appendSection(writerSizes.annotationsSet, "annotation sets");

        contentsOut.classDatas.off = dexOut.getNextSectionStart();
        classDataOut = dexOut.appendSection(writerSizes.classData, "class data");

        contentsOut.codes.off = dexOut.getNextSectionStart();
        codeOut = dexOut.appendSection(writerSizes.code, "code");

        contentsOut.stringDatas.off = dexOut.getNextSectionStart();
        stringDataOut = dexOut.appendSection(writerSizes.stringData, "string data");

        contentsOut.debugInfos.off = dexOut.getNextSectionStart();
        debugInfoOut = dexOut.appendSection(writerSizes.debugInfo, "debug info");

        contentsOut.annotations.off = dexOut.getNextSectionStart();
        annotationOut = dexOut.appendSection(writerSizes.annotation, "annotation");

        contentsOut.encodedArrays.off = dexOut.getNextSectionStart();
        encodedArrayOut = dexOut.appendSection(writerSizes.encodedArray, "encoded array");

        contentsOut.annotationsDirectories.off = dexOut.getNextSectionStart();
        annotationsDirectoryOut = dexOut.appendSection(
                writerSizes.annotationsDirectory, "annotations directory");

        contentsOut.dataSize = dexOut.getNextSectionStart() - contentsOut.dataOff;
    }

    public void setCompactWasteThreshold(int compactWasteThreshold) {
        this.compactWasteThreshold = compactWasteThreshold;
    }

    private Dex mergeDexes() throws IOException {
        mergeStringIds();
        mergeTypeIds();
        mergeTypeLists();
        mergeProtoIds();
        mergeFieldIds();
        mergeMethodIds();
        mergeMethodHandles();
        mergeAnnotations();
        unionAnnotationSetsAndDirectories();
        mergeCallSiteIds();
        mergeClassDefs();

        // computeSizesFromOffsets expects sections sorted by offset, so make it so
        Arrays.sort(contentsOut.sections);

        // write the header
        contentsOut.header.off = 0;
        contentsOut.header.size = 1;
        contentsOut.fileSize = dexOut.getLength();
        contentsOut.computeSizesFromOffsets();
        contentsOut.writeHeader(headerOut, mergeApiLevels());
        contentsOut.writeMap(mapListOut);

        // generate and write the hashes
        dexOut.writeHashes();

        return dexOut;
    }

    public Dex merge() throws IOException {
        if (dexes.length == 1) {
            return dexes[0];
        } else if (dexes.length == 0) {
            return null;
        }

        long start = System.nanoTime();
        Dex result = mergeDexes();

        /*
         * We use pessimistic sizes when merging dex files. If those sizes
         * result in too many bytes wasted, compact the result. To compact,
         * simply merge the result with itself.
         */
        WriterSizes compactedSizes = new WriterSizes(this);
        int wastedByteCount = writerSizes.size() - compactedSizes.size();
        if (wastedByteCount >  + compactWasteThreshold) {
            DexMerger compacter = new DexMerger(
                    new Dex[] {dexOut, new Dex(0)}, CollisionPolicy.FAIL, context, compactedSizes);
            result = compacter.mergeDexes();
            context.out.printf("Result compacted from %.1fKiB to %.1fKiB to save %.1fKiB%n",
                    dexOut.getLength() / 1024f,
                    result.getLength() / 1024f,
                    wastedByteCount / 1024f);
        }

        long elapsed = System.nanoTime() - start;
        for (int i = 0; i < dexes.length; i++) {
            context.out.printf("Merged dex #%d (%d defs/%.1fKiB)%n",
                i + 1,
                dexes[i].getTableOfContents().classDefs.size,
                dexes[i].getLength() / 1024f);
        }
        context.out.printf("Result is %d defs/%.1fKiB. Took %.1fs%n",
                result.getTableOfContents().classDefs.size,
                result.getLength() / 1024f,
                elapsed / 1000000000f);

        return result;
    }

    /**
     * Reads an IDs section of two dex files and writes an IDs section of a
     * merged dex file. Populates maps from old to new indices in the process.
     */
    abstract class IdMerger<T extends Comparable<T>> {
        private final Dex.Section out;

        protected IdMerger(Dex.Section out) {
            this.out = out;
        }

        /**
         * Merges already-sorted sections, reading one value from each dex into memory
         * at a time.
         */
        public final void mergeSorted() {
            TableOfContents.Section[] sections = new TableOfContents.Section[dexes.length];
            Dex.Section[] dexSections = new Dex.Section[dexes.length];
            int[] offsets = new int[dexes.length];
            int[] indexes = new int[dexes.length];

            // values contains one value from each dex, sorted for fast retrieval of
            // the smallest value. The list associated with a value has the indexes
            // of the dexes that had that value.
            TreeMap<T, List<Integer>> values = new TreeMap<T, List<Integer>>();

            for (int i = 0; i < dexes.length; i++) {
                sections[i] = getSection(dexes[i].getTableOfContents());
                dexSections[i] = sections[i].exists() ? dexes[i].open(sections[i].off) : null;
                // Fill in values with the first value of each dex.
                offsets[i] = readIntoMap(
                        dexSections[i], sections[i], indexMaps[i], indexes[i], values, i);
            }
            if (values.isEmpty()) {
                getSection(contentsOut).off = 0;
                getSection(contentsOut).size = 0;
                return;
            }
            getSection(contentsOut).off = out.getPosition();

            int outCount = 0;
            while (!values.isEmpty()) {
                Map.Entry<T, List<Integer>> first = values.pollFirstEntry();
                for (Integer dex : first.getValue()) {
                    updateIndex(offsets[dex], indexMaps[dex], indexes[dex]++, outCount);
                    // Fetch the next value of the dexes we just polled out
                    offsets[dex] = readIntoMap(dexSections[dex], sections[dex],
                            indexMaps[dex], indexes[dex], values, dex);
                }
                write(first.getKey());
                outCount++;
            }

            getSection(contentsOut).size = outCount;
        }

        private int readIntoMap(Dex.Section in, TableOfContents.Section section, IndexMap indexMap,
                                int index, TreeMap<T, List<Integer>> values, int dex) {
            int offset = in != null ? in.getPosition() : -1;
            if (index < section.size) {
                T v = read(in, indexMap, index);
                List<Integer> l = values.get(v);
                if (l == null) {
                    l = new ArrayList<Integer>();
                    values.put(v, l);
                }
                l.add(dex);
            }
            return offset;
        }

        /**
         * Merges unsorted sections by reading them completely into memory and
         * sorting in memory.
         */
        public final void mergeUnsorted() {
            getSection(contentsOut).off = out.getPosition();

            List<UnsortedValue> all = new ArrayList<UnsortedValue>();
            for (int i = 0; i < dexes.length; i++) {
                all.addAll(readUnsortedValues(dexes[i], indexMaps[i]));
            }
            if (all.isEmpty()) {
                getSection(contentsOut).off = 0;
                getSection(contentsOut).size = 0;
                return;
            }
            Collections.sort(all);

            int outCount = 0;
            for (int i = 0; i < all.size(); ) {
                UnsortedValue e1 = all.get(i++);
                updateIndex(e1.offset, e1.indexMap, e1.index, outCount - 1);

                while (i < all.size() && e1.compareTo(all.get(i)) == 0) {
                    UnsortedValue e2 = all.get(i++);
                    updateIndex(e2.offset, e2.indexMap, e2.index, outCount - 1);
                }

                write(e1.value);
                outCount++;
            }

            getSection(contentsOut).size = outCount;
        }

        private List<UnsortedValue> readUnsortedValues(Dex source, IndexMap indexMap) {
            TableOfContents.Section section = getSection(source.getTableOfContents());
            if (!section.exists()) {
                return Collections.emptyList();
            }

            List<UnsortedValue> result = new ArrayList<UnsortedValue>();
            Dex.Section in = source.open(section.off);
            for (int i = 0; i < section.size; i++) {
                int offset = in.getPosition();
                T value = read(in, indexMap, 0);
                result.add(new UnsortedValue(source, indexMap, value, i, offset));
            }
            return result;
        }

        abstract TableOfContents.Section getSection(TableOfContents tableOfContents);
        abstract T read(Dex.Section in, IndexMap indexMap, int index);
        abstract void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex);
        abstract void write(T value);

        class UnsortedValue implements Comparable<UnsortedValue> {
            final Dex source;
            final IndexMap indexMap;
            final T value;
            final int index;
            final int offset;

            UnsortedValue(Dex source, IndexMap indexMap, T value, int index, int offset) {
                this.source = source;
                this.indexMap = indexMap;
                this.value = value;
                this.index = index;
                this.offset = offset;
            }

            @Override
            public int compareTo(UnsortedValue unsortedValue) {
                return value.compareTo(unsortedValue.value);
            }
        }
    }

    private int mergeApiLevels() {
        int maxApi = -1;
        for (int i = 0; i < dexes.length; i++) {
            int dexMinApi = dexes[i].getTableOfContents().apiLevel;
            if (maxApi < dexMinApi) {
                maxApi = dexMinApi;
            }
        }
        return maxApi;
    }

    private void mergeStringIds() {
        new IdMerger<String>(idsDefsOut) {
            @Override TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.stringIds;
            }

            @Override String read(Dex.Section in, IndexMap indexMap, int index) {
                return in.readString();
            }

            @Override void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.stringIds[oldIndex] = newIndex;
            }

            @Override void write(String value) {
                contentsOut.stringDatas.size++;
                idsDefsOut.writeInt(stringDataOut.getPosition());
                stringDataOut.writeStringData(value);
            }
        }.mergeSorted();
    }

    private void mergeTypeIds() {
        new IdMerger<Integer>(idsDefsOut) {
            @Override TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.typeIds;
            }

            @Override Integer read(Dex.Section in, IndexMap indexMap, int index) {
                int stringIndex = in.readInt();
                return indexMap.adjustString(stringIndex);
            }

            @Override void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                if (newIndex < 0 || newIndex > 0xffff) {
                    throw new DexIndexOverflowException("type ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.typeIds[oldIndex] = (short) newIndex;
            }

            @Override void write(Integer value) {
                idsDefsOut.writeInt(value);
            }
        }.mergeSorted();
    }

    private void mergeTypeLists() {
        new IdMerger<TypeList>(typeListOut) {
            @Override TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.typeLists;
            }

            @Override TypeList read(Dex.Section in, IndexMap indexMap, int index) {
                return indexMap.adjustTypeList(in.readTypeList());
            }

            @Override void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.putTypeListOffset(offset, typeListOut.getPosition());
            }

            @Override void write(TypeList value) {
                typeListOut.writeTypeList(value);
            }
        }.mergeUnsorted();
    }

    private void mergeProtoIds() {
        new IdMerger<ProtoId>(idsDefsOut) {
            @Override TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.protoIds;
            }

            @Override ProtoId read(Dex.Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readProtoId());
            }

            @Override void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                if (newIndex < 0 || newIndex > 0xffff) {
                    throw new DexIndexOverflowException("proto ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.protoIds[oldIndex] = (short) newIndex;
            }

            @Override
            void write(ProtoId value) {
                value.writeTo(idsDefsOut);
            }
        }.mergeSorted();
    }

    private void mergeCallSiteIds() {
        new IdMerger<CallSiteId>(idsDefsOut) {
            @Override
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.callSiteIds;
            }

            @Override
            CallSiteId read(Dex.Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readCallSiteId());
            }

            @Override
            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.callSiteIds[oldIndex] = newIndex;
            }

            @Override
            void write(CallSiteId value) {
                value.writeTo(idsDefsOut);
            }
        }.mergeSorted();
    }

    private void mergeMethodHandles() {
        new IdMerger<MethodHandle>(idsDefsOut) {
            @Override
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.methodHandles;
            }

            @Override
            MethodHandle read(Dex.Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readMethodHandle());
            }

            @Override
            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.methodHandleIds.put(oldIndex, indexMap.methodHandleIds.size());
            }

            @Override
            void write(MethodHandle value) {
                value.writeTo(idsDefsOut);
            }
        }.mergeUnsorted();
    }

    private void mergeFieldIds() {
        new IdMerger<FieldId>(idsDefsOut) {
            @Override TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.fieldIds;
            }

            @Override FieldId read(Dex.Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readFieldId());
            }

            @Override void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                if (newIndex < 0 || newIndex > 0xffff) {
                    throw new DexIndexOverflowException("field ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.fieldIds[oldIndex] = (short) newIndex;
            }

            @Override void write(FieldId value) {
                value.writeTo(idsDefsOut);
            }
        }.mergeSorted();
    }

    private void mergeMethodIds() {
        new IdMerger<MethodId>(idsDefsOut) {
            @Override TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.methodIds;
            }

            @Override MethodId read(Dex.Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readMethodId());
            }

            @Override void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                if (newIndex < 0 || newIndex > 0xffff) {
                    throw new DexIndexOverflowException(
                        "method ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.methodIds[oldIndex] = (short) newIndex;
            }

            @Override void write(MethodId methodId) {
                methodId.writeTo(idsDefsOut);
            }
        }.mergeSorted();
    }

    private void mergeAnnotations() {
        new IdMerger<Annotation>(annotationOut) {
            @Override TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.annotations;
            }

            @Override Annotation read(Dex.Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readAnnotation());
            }

            @Override void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.putAnnotationOffset(offset, annotationOut.getPosition());
            }

            @Override void write(Annotation value) {
                value.writeTo(annotationOut);
            }
        }.mergeUnsorted();
    }

    private void mergeClassDefs() {
        SortableType[] types = getSortedTypes();
        contentsOut.classDefs.off = idsDefsOut.getPosition();
        contentsOut.classDefs.size = types.length;

        for (SortableType type : types) {
            Dex in = type.getDex();
            transformClassDef(in, type.getClassDef(), type.getIndexMap());
        }
    }

    /**
     * Returns the union of classes from both files, sorted in order such that
     * a class is always preceded by its supertype and implemented interfaces.
     */
    private SortableType[] getSortedTypes() {
        // size is pessimistic; doesn't include arrays
        SortableType[] sortableTypes = new SortableType[contentsOut.typeIds.size];
        for (int i = 0; i < dexes.length; i++) {
            readSortableTypes(sortableTypes, dexes[i], indexMaps[i]);
        }

        /*
         * Populate the depths of each sortable type. This makes D iterations
         * through all N types, where 'D' is the depth of the deepest type. For
         * example, the deepest class in libcore is Xalan's KeyIterator, which
         * is 11 types deep.
         */
        while (true) {
            boolean allDone = true;
            for (SortableType sortableType : sortableTypes) {
                if (sortableType != null && !sortableType.isDepthAssigned()) {
                    allDone &= sortableType.tryAssignDepth(sortableTypes);
                }
            }
            if (allDone) {
                break;
            }
        }

        // Now that all types have depth information, the result can be sorted
        Arrays.sort(sortableTypes, SortableType.NULLS_LAST_ORDER);

        // Strip nulls from the end
        int firstNull = Arrays.asList(sortableTypes).indexOf(null);
        return firstNull != -1
                ? Arrays.copyOfRange(sortableTypes, 0, firstNull)
                : sortableTypes;
    }

    /**
     * Reads just enough data on each class so that we can sort it and then find
     * it later.
     */
    private void readSortableTypes(SortableType[] sortableTypes, Dex buffer,
            IndexMap indexMap) {
        for (ClassDef classDef : buffer.classDefs()) {
            SortableType sortableType = indexMap.adjust(
                    new SortableType(buffer, indexMap, classDef));
            int t = sortableType.getTypeIndex();
            if (sortableTypes[t] == null) {
                sortableTypes[t] = sortableType;
            } else if (collisionPolicy != CollisionPolicy.KEEP_FIRST) {
                throw new DexException("Multiple dex files define "
                        + buffer.typeNames().get(classDef.getTypeIndex()));
            }
        }
    }

    /**
     * Copy annotation sets from each input to the output.
     *
     * TODO: this may write multiple copies of the same annotation set.
     * We should shrink the output by merging rather than unioning
     */
    private void unionAnnotationSetsAndDirectories() {
        for (int i = 0; i < dexes.length; i++) {
            transformAnnotationSets(dexes[i], indexMaps[i]);
        }
        for (int i = 0; i < dexes.length; i++) {
            transformAnnotationSetRefLists(dexes[i], indexMaps[i]);
        }
        for (int i = 0; i < dexes.length; i++) {
            transformAnnotationDirectories(dexes[i], indexMaps[i]);
        }
        for (int i = 0; i < dexes.length; i++) {
            transformStaticValues(dexes[i], indexMaps[i]);
        }
    }

    private void transformAnnotationSets(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationSets;
        if (section.exists()) {
            Dex.Section setIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationSet(indexMap, setIn);
            }
        }
    }

    private void transformAnnotationSetRefLists(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationSetRefLists;
        if (section.exists()) {
            Dex.Section setIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationSetRefList(indexMap, setIn);
            }
        }
    }

    private void transformAnnotationDirectories(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationsDirectories;
        if (section.exists()) {
            Dex.Section directoryIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationDirectory(directoryIn, indexMap);
            }
        }
    }

    private void transformStaticValues(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().encodedArrays;
        if (section.exists()) {
            Dex.Section staticValuesIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformStaticValues(staticValuesIn, indexMap);
            }
        }
    }

    /**
     * Reads a class_def_item beginning at {@code in} and writes the index and
     * data.
     */
    private void transformClassDef(Dex in, ClassDef classDef, IndexMap indexMap) {
        idsDefsOut.assertFourByteAligned();
        idsDefsOut.writeInt(classDef.getTypeIndex());
        idsDefsOut.writeInt(classDef.getAccessFlags());
        idsDefsOut.writeInt(classDef.getSupertypeIndex());
        idsDefsOut.writeInt(classDef.getInterfacesOffset());

        int sourceFileIndex = indexMap.adjustString(classDef.getSourceFileIndex());
        idsDefsOut.writeInt(sourceFileIndex);

        int annotationsOff = classDef.getAnnotationsOffset();
        idsDefsOut.writeInt(indexMap.adjustAnnotationDirectory(annotationsOff));

        int classDataOff = classDef.getClassDataOffset();
        if (classDataOff == 0) {
            idsDefsOut.writeInt(0);
        } else {
            idsDefsOut.writeInt(classDataOut.getPosition());
            ClassData classData = in.readClassData(classDef);
            transformClassData(in, classData, indexMap);
        }

        int staticValuesOff = classDef.getStaticValuesOffset();
        idsDefsOut.writeInt(indexMap.adjustEncodedArray(staticValuesOff));
    }

    /**
     * Transform all annotations on a class.
     */
    private void transformAnnotationDirectory(
            Dex.Section directoryIn, IndexMap indexMap) {
        contentsOut.annotationsDirectories.size++;
        annotationsDirectoryOut.assertFourByteAligned();
        indexMap.putAnnotationDirectoryOffset(
                directoryIn.getPosition(), annotationsDirectoryOut.getPosition());

        int classAnnotationsOffset = indexMap.adjustAnnotationSet(directoryIn.readInt());
        annotationsDirectoryOut.writeInt(classAnnotationsOffset);

        int fieldsSize = directoryIn.readInt();
        annotationsDirectoryOut.writeInt(fieldsSize);

        int methodsSize = directoryIn.readInt();
        annotationsDirectoryOut.writeInt(methodsSize);

        int parameterListSize = directoryIn.readInt();
        annotationsDirectoryOut.writeInt(parameterListSize);

        for (int i = 0; i < fieldsSize; i++) {
            // field index
            annotationsDirectoryOut.writeInt(indexMap.adjustField(directoryIn.readInt()));

            // annotations offset
            annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSet(directoryIn.readInt()));
        }

        for (int i = 0; i < methodsSize; i++) {
            // method index
            annotationsDirectoryOut.writeInt(indexMap.adjustMethod(directoryIn.readInt()));

            // annotation set offset
            annotationsDirectoryOut.writeInt(
                    indexMap.adjustAnnotationSet(directoryIn.readInt()));
        }

        for (int i = 0; i < parameterListSize; i++) {
            // method index
            annotationsDirectoryOut.writeInt(indexMap.adjustMethod(directoryIn.readInt()));

            // annotations offset
            annotationsDirectoryOut.writeInt(
                    indexMap.adjustAnnotationSetRefList(directoryIn.readInt()));
        }
    }

    /**
     * Transform all annotations on a single type, member or parameter.
     */
    private void transformAnnotationSet(IndexMap indexMap, Dex.Section setIn) {
        contentsOut.annotationSets.size++;
        annotationSetOut.assertFourByteAligned();
        indexMap.putAnnotationSetOffset(setIn.getPosition(), annotationSetOut.getPosition());

        int size = setIn.readInt();
        annotationSetOut.writeInt(size);

        for (int j = 0; j < size; j++) {
            annotationSetOut.writeInt(indexMap.adjustAnnotation(setIn.readInt()));
        }
    }

    /**
     * Transform all annotation set ref lists.
     */
    private void transformAnnotationSetRefList(IndexMap indexMap, Dex.Section refListIn) {
        contentsOut.annotationSetRefLists.size++;
        annotationSetRefListOut.assertFourByteAligned();
        indexMap.putAnnotationSetRefListOffset(
                refListIn.getPosition(), annotationSetRefListOut.getPosition());

        int parameterCount = refListIn.readInt();
        annotationSetRefListOut.writeInt(parameterCount);
        for (int p = 0; p < parameterCount; p++) {
            annotationSetRefListOut.writeInt(indexMap.adjustAnnotationSet(refListIn.readInt()));
        }
    }

    private void transformClassData(Dex in, ClassData classData, IndexMap indexMap) {
        contentsOut.classDatas.size++;

        ClassData.Field[] staticFields = classData.getStaticFields();
        ClassData.Field[] instanceFields = classData.getInstanceFields();
        ClassData.Method[] directMethods = classData.getDirectMethods();
        ClassData.Method[] virtualMethods = classData.getVirtualMethods();

        classDataOut.writeUleb128(staticFields.length);
        classDataOut.writeUleb128(instanceFields.length);
        classDataOut.writeUleb128(directMethods.length);
        classDataOut.writeUleb128(virtualMethods.length);

        transformFields(indexMap, staticFields);
        transformFields(indexMap, instanceFields);
        transformMethods(in, indexMap, directMethods);
        transformMethods(in, indexMap, virtualMethods);
    }

    private void transformFields(IndexMap indexMap, ClassData.Field[] fields) {
        int lastOutFieldIndex = 0;
        for (ClassData.Field field : fields) {
            int outFieldIndex = indexMap.adjustField(field.getFieldIndex());
            classDataOut.writeUleb128(outFieldIndex - lastOutFieldIndex);
            lastOutFieldIndex = outFieldIndex;
            classDataOut.writeUleb128(field.getAccessFlags());
        }
    }

    private void transformMethods(Dex in, IndexMap indexMap, ClassData.Method[] methods) {
        int lastOutMethodIndex = 0;
        for (ClassData.Method method : methods) {
            int outMethodIndex = indexMap.adjustMethod(method.getMethodIndex());
            classDataOut.writeUleb128(outMethodIndex - lastOutMethodIndex);
            lastOutMethodIndex = outMethodIndex;

            classDataOut.writeUleb128(method.getAccessFlags());

            if (method.getCodeOffset() == 0) {
                classDataOut.writeUleb128(0);
            } else {
                codeOut.alignToFourBytesWithZeroFill();
                classDataOut.writeUleb128(codeOut.getPosition());
                transformCode(in, in.readCode(method), indexMap);
            }
        }
    }

    private void transformCode(Dex in, Code code, IndexMap indexMap) {
        contentsOut.codes.size++;
        codeOut.assertFourByteAligned();

        codeOut.writeUnsignedShort(code.getRegistersSize());
        codeOut.writeUnsignedShort(code.getInsSize());
        codeOut.writeUnsignedShort(code.getOutsSize());

        Code.Try[] tries = code.getTries();
        Code.CatchHandler[] catchHandlers = code.getCatchHandlers();
        codeOut.writeUnsignedShort(tries.length);

        int debugInfoOffset = code.getDebugInfoOffset();
        if (debugInfoOffset != 0) {
            codeOut.writeInt(debugInfoOut.getPosition());
            transformDebugInfoItem(in.open(debugInfoOffset), indexMap);
        } else {
            codeOut.writeInt(0);
        }

        short[] instructions = code.getInstructions();
        short[] newInstructions = instructionTransformer.transform(indexMap, instructions);
        codeOut.writeInt(newInstructions.length);
        codeOut.write(newInstructions);

        if (tries.length > 0) {
            if (newInstructions.length % 2 == 1) {
                codeOut.writeShort((short) 0); // padding
            }

            /*
             * We can't write the tries until we've written the catch handlers.
             * Unfortunately they're in the opposite order in the dex file so we
             * need to transform them out-of-order.
             */
            Dex.Section triesSection = dexOut.open(codeOut.getPosition());
            codeOut.skip(tries.length * SizeOf.TRY_ITEM);
            int[] offsets = transformCatchHandlers(indexMap, catchHandlers);
            transformTries(triesSection, tries, offsets);
        }
    }

    /**
     * Writes the catch handlers to {@code codeOut} and returns their indices.
     */
    private int[] transformCatchHandlers(IndexMap indexMap, Code.CatchHandler[] catchHandlers) {
        int baseOffset = codeOut.getPosition();
        codeOut.writeUleb128(catchHandlers.length);
        int[] offsets = new int[catchHandlers.length];
        for (int i = 0; i < catchHandlers.length; i++) {
            offsets[i] = codeOut.getPosition() - baseOffset;
            transformEncodedCatchHandler(catchHandlers[i], indexMap);
        }
        return offsets;
    }

    private void transformTries(Dex.Section out, Code.Try[] tries,
            int[] catchHandlerOffsets) {
        for (Code.Try tryItem : tries) {
            out.writeInt(tryItem.getStartAddress());
            out.writeUnsignedShort(tryItem.getInstructionCount());
            out.writeUnsignedShort(catchHandlerOffsets[tryItem.getCatchHandlerIndex()]);
        }
    }

    private static final byte DBG_END_SEQUENCE = 0x00;
    private static final byte DBG_ADVANCE_PC = 0x01;
    private static final byte DBG_ADVANCE_LINE = 0x02;
    private static final byte DBG_START_LOCAL = 0x03;
    private static final byte DBG_START_LOCAL_EXTENDED = 0x04;
    private static final byte DBG_END_LOCAL = 0x05;
    private static final byte DBG_RESTART_LOCAL = 0x06;
    private static final byte DBG_SET_PROLOGUE_END = 0x07;
    private static final byte DBG_SET_EPILOGUE_BEGIN = 0x08;
    private static final byte DBG_SET_FILE = 0x09;

    private void transformDebugInfoItem(Dex.Section in, IndexMap indexMap) {
        contentsOut.debugInfos.size++;
        int lineStart = in.readUleb128();
        debugInfoOut.writeUleb128(lineStart);

        int parametersSize = in.readUleb128();
        debugInfoOut.writeUleb128(parametersSize);

        for (int p = 0; p < parametersSize; p++) {
            int parameterName = in.readUleb128p1();
            debugInfoOut.writeUleb128p1(indexMap.adjustString(parameterName));
        }

        int addrDiff;    // uleb128   address delta.
        int lineDiff;    // sleb128   line delta.
        int registerNum; // uleb128   register number.
        int nameIndex;   // uleb128p1 string index.    Needs indexMap adjustment.
        int typeIndex;   // uleb128p1 type index.      Needs indexMap adjustment.
        int sigIndex;    // uleb128p1 string index.    Needs indexMap adjustment.

        while (true) {
            int opcode = in.readByte();
            debugInfoOut.writeByte(opcode);

            switch (opcode) {
            case DBG_END_SEQUENCE:
                return;

            case DBG_ADVANCE_PC:
                addrDiff = in.readUleb128();
                debugInfoOut.writeUleb128(addrDiff);
                break;

            case DBG_ADVANCE_LINE:
                lineDiff = in.readSleb128();
                debugInfoOut.writeSleb128(lineDiff);
                break;

            case DBG_START_LOCAL:
            case DBG_START_LOCAL_EXTENDED:
                registerNum = in.readUleb128();
                debugInfoOut.writeUleb128(registerNum);
                nameIndex = in.readUleb128p1();
                debugInfoOut.writeUleb128p1(indexMap.adjustString(nameIndex));
                typeIndex = in.readUleb128p1();
                debugInfoOut.writeUleb128p1(indexMap.adjustType(typeIndex));
                if (opcode == DBG_START_LOCAL_EXTENDED) {
                    sigIndex = in.readUleb128p1();
                    debugInfoOut.writeUleb128p1(indexMap.adjustString(sigIndex));
                }
                break;

            case DBG_END_LOCAL:
            case DBG_RESTART_LOCAL:
                registerNum = in.readUleb128();
                debugInfoOut.writeUleb128(registerNum);
                break;

            case DBG_SET_FILE:
                nameIndex = in.readUleb128p1();
                debugInfoOut.writeUleb128p1(indexMap.adjustString(nameIndex));
                break;

            case DBG_SET_PROLOGUE_END:
            case DBG_SET_EPILOGUE_BEGIN:
            default:
                break;
            }
        }
    }

    private void transformEncodedCatchHandler(Code.CatchHandler catchHandler, IndexMap indexMap) {
        int catchAllAddress = catchHandler.getCatchAllAddress();
        int[] typeIndexes = catchHandler.getTypeIndexes();
        int[] addresses = catchHandler.getAddresses();

        if (catchAllAddress != -1) {
            codeOut.writeSleb128(-typeIndexes.length);
        } else {
            codeOut.writeSleb128(typeIndexes.length);
        }

        for (int i = 0; i < typeIndexes.length; i++) {
            codeOut.writeUleb128(indexMap.adjustType(typeIndexes[i]));
            codeOut.writeUleb128(addresses[i]);
        }

        if (catchAllAddress != -1) {
            codeOut.writeUleb128(catchAllAddress);
        }
    }

    private void transformStaticValues(Dex.Section in, IndexMap indexMap) {
        contentsOut.encodedArrays.size++;
        indexMap.putEncodedArrayValueOffset(in.getPosition(), encodedArrayOut.getPosition());
        indexMap.adjustEncodedArray(in.readEncodedArray()).writeTo(encodedArrayOut);
    }

    /**
     * Byte counts for the sections written when creating a dex. Target sizes
     * are defined in one of two ways:
     * <ul>
     * <li>By pessimistically guessing how large the union of dex files will be.
     *     We're pessimistic because we can't predict the amount of duplication
     *     between dex files, nor can we predict the length of ULEB-encoded
     *     offsets or indices.
     * <li>By exactly measuring an existing dex.
     * </ul>
     */
    private static class WriterSizes {
        private int header = SizeOf.HEADER_ITEM;
        private int idsDefs;
        private int mapList;
        private int typeList;
        private int classData;
        private int code;
        private int stringData;
        private int debugInfo;
        private int encodedArray;
        private int annotationsDirectory;
        private int annotationsSet;
        private int annotationsSetRefList;
        private int annotation;

        /**
         * Compute sizes for merging several dexes.
         */
        public WriterSizes(Dex[] dexes) {
            for (int i = 0; i < dexes.length; i++) {
                plus(dexes[i].getTableOfContents(), false);
            }
            fourByteAlign();
        }

        public WriterSizes(DexMerger dexMerger) {
            header = dexMerger.headerOut.used();
            idsDefs = dexMerger.idsDefsOut.used();
            mapList = dexMerger.mapListOut.used();
            typeList = dexMerger.typeListOut.used();
            classData = dexMerger.classDataOut.used();
            code = dexMerger.codeOut.used();
            stringData = dexMerger.stringDataOut.used();
            debugInfo = dexMerger.debugInfoOut.used();
            encodedArray = dexMerger.encodedArrayOut.used();
            annotationsDirectory = dexMerger.annotationsDirectoryOut.used();
            annotationsSet = dexMerger.annotationSetOut.used();
            annotationsSetRefList = dexMerger.annotationSetRefListOut.used();
            annotation = dexMerger.annotationOut.used();
            fourByteAlign();
        }

        private void plus(TableOfContents contents, boolean exact) {
            idsDefs += contents.stringIds.size * SizeOf.STRING_ID_ITEM
                    + contents.typeIds.size * SizeOf.TYPE_ID_ITEM
                    + contents.protoIds.size * SizeOf.PROTO_ID_ITEM
                    + contents.fieldIds.size * SizeOf.MEMBER_ID_ITEM
                    + contents.methodIds.size * SizeOf.MEMBER_ID_ITEM
                    + contents.classDefs.size * SizeOf.CLASS_DEF_ITEM;
            mapList = SizeOf.UINT + (contents.sections.length * SizeOf.MAP_ITEM);
            typeList += fourByteAlign(contents.typeLists.byteCount); // We count each dex's
            // typelists section as realigned on 4 bytes, because each typelist of each dex's
            // typelists section is aligned on 4 bytes. If we didn't, there is a case where each
            // size of both dex's typelists section is a multiple of 2 but not a multiple of 4,
            // and the sum of both sizes is a multiple of 4 but would not be sufficient to write
            // each typelist aligned on 4 bytes.
            stringData += contents.stringDatas.byteCount;
            annotationsDirectory += contents.annotationsDirectories.byteCount;
            annotationsSet += contents.annotationSets.byteCount;
            annotationsSetRefList += contents.annotationSetRefLists.byteCount;

            if (exact) {
                code += contents.codes.byteCount;
                classData += contents.classDatas.byteCount;
                encodedArray += contents.encodedArrays.byteCount;
                annotation += contents.annotations.byteCount;
                debugInfo += contents.debugInfos.byteCount;
            } else {
                // at most 1/4 of the bytes in a code section are uleb/sleb
                code += (int) Math.ceil(contents.codes.byteCount * 1.25);
                // at most 2/3 of the bytes in a class data section are uleb/sleb that may change
                // (assuming the worst case that section contains only methods and no fields)
                classData += (int) Math.ceil(contents.classDatas.byteCount * 1.67);
                // all of the bytes in an encoding arrays section may be uleb/sleb
                encodedArray += contents.encodedArrays.byteCount * 2;
                // all of the bytes in an annotations section may be uleb/sleb
                annotation += (int) Math.ceil(contents.annotations.byteCount * 2);
                // all of the bytes in a debug info section may be uleb/sleb. The additive constant
                // is a fudge factor observed to be required when merging small
                // DEX files (b/68483205).
                debugInfo += contents.debugInfos.byteCount * 2 + 8;
            }
        }

        private void fourByteAlign() {
            header = fourByteAlign(header);
            idsDefs = fourByteAlign(idsDefs);
            mapList = fourByteAlign(mapList);
            typeList = fourByteAlign(typeList);
            classData = fourByteAlign(classData);
            code = fourByteAlign(code);
            stringData = fourByteAlign(stringData);
            debugInfo = fourByteAlign(debugInfo);
            encodedArray = fourByteAlign(encodedArray);
            annotationsDirectory = fourByteAlign(annotationsDirectory);
            annotationsSet = fourByteAlign(annotationsSet);
            annotationsSetRefList = fourByteAlign(annotationsSetRefList);
            annotation = fourByteAlign(annotation);
        }

        private static int fourByteAlign(int position) {
            return (position + 3) & ~3;
        }

        public int size() {
            return header + idsDefs + mapList + typeList + classData + code + stringData + debugInfo
                    + encodedArray + annotationsDirectory + annotationsSet + annotationsSetRefList
                    + annotation;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }

        Dex[] dexes = new Dex[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            dexes[i - 1] = new Dex(new File(args[i]));
        }
        Dex merged = new DexMerger(dexes, CollisionPolicy.KEEP_FIRST, new DxContext()).merge();
        merged.writeTo(new File(args[0]));
    }

    private static void printUsage() {
        System.out.println("Usage: DexMerger <out.dex> <a.dex> <b.dex> ...");
        System.out.println();
        System.out.println(
            "If a class is defined in several dex, the class found in the first dex will be used.");
    }
}

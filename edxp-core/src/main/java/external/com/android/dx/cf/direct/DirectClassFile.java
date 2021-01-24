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

package external.com.android.dx.cf.direct;

import external.com.android.dx.cf.attrib.AttBootstrapMethods;
import external.com.android.dx.cf.attrib.AttSourceFile;
import external.com.android.dx.cf.code.BootstrapMethodsList;
import external.com.android.dx.cf.cst.ConstantPoolParser;
import external.com.android.dx.cf.iface.Attribute;
import external.com.android.dx.cf.iface.AttributeList;
import external.com.android.dx.cf.iface.ClassFile;
import external.com.android.dx.cf.iface.FieldList;
import external.com.android.dx.cf.iface.MethodList;
import external.com.android.dx.cf.iface.ParseException;
import external.com.android.dx.cf.iface.ParseObserver;
import external.com.android.dx.cf.iface.StdAttributeList;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.cst.ConstantPool;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.cst.StdConstantPool;
import external.com.android.dx.rop.type.StdTypeList;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.util.ByteArray;
import external.com.android.dx.util.Hex;

/**
 * Class file with info taken from a {@code byte[]} or slice thereof.
 */
public class DirectClassFile implements ClassFile {
    /** the expected value of the ClassFile.magic field */
    private static final int CLASS_FILE_MAGIC = 0xcafebabe;

    /**
     * minimum {@code .class} file major version
     *
     * See http://en.wikipedia.org/wiki/Java_class_file for an up-to-date
     * list of version numbers. Currently known (taken from that table) are:
     *
     *     Java SE 9 = 53 (0x35 hex),
     *     Java SE 8 = 52 (0x34 hex),
     *     Java SE 7 = 51 (0x33 hex),
     *     Java SE 6.0 = 50 (0x32 hex),
     *     Java SE 5.0 = 49 (0x31 hex),
     *     JDK 1.4 = 48 (0x30 hex),
     *     JDK 1.3 = 47 (0x2F hex),
     *     JDK 1.2 = 46 (0x2E hex),
     *     JDK 1.1 = 45 (0x2D hex).
     *
     * Valid ranges are typically of the form
     * "A.0 through B.C inclusive" where A <= B and C >= 0,
     * which is why we don't have a CLASS_FILE_MIN_MINOR_VERSION.
     */
    private static final int CLASS_FILE_MIN_MAJOR_VERSION = 45;

    /**
     * maximum {@code .class} file major version
     *
     * Note: if you change this, please change "java.class.version" in System.java.
     */
    private static final int CLASS_FILE_MAX_MAJOR_VERSION = 53;

    /** maximum {@code .class} file minor version */
    private static final int CLASS_FILE_MAX_MINOR_VERSION = 0;

    /**
     * {@code non-null;} the file path for the class, excluding any base directory
     * specification
     */
    private final String filePath;

    /** {@code non-null;} the bytes of the file */
    private final ByteArray bytes;

    /**
     * whether to be strict about parsing; if
     * {@code false}, this avoids doing checks that only exist
     * for purposes of verification (such as magic number matching and
     * path-package consistency checking)
     */
    private final boolean strictParse;

    /**
     * {@code null-ok;} the constant pool; only ever {@code null}
     * before the constant pool is successfully parsed
     */
    private StdConstantPool pool;

    /**
     * the class file field {@code access_flags}; will be {@code -1}
     * before the file is successfully parsed
     */
    private int accessFlags;

    /**
     * {@code null-ok;} the class file field {@code this_class},
     * interpreted as a type constant; only ever {@code null}
     * before the file is successfully parsed
     */
    private CstType thisClass;

    /**
     * {@code null-ok;} the class file field {@code super_class}, interpreted
     * as a type constant if non-zero
     */
    private CstType superClass;

    /**
     * {@code null-ok;} the class file field {@code interfaces}; only
     * ever {@code null} before the file is successfully
     * parsed
     */
    private TypeList interfaces;

    /**
     * {@code null-ok;} the class file field {@code fields}; only ever
     * {@code null} before the file is successfully parsed
     */
    private FieldList fields;

    /**
     * {@code null-ok;} the class file field {@code methods}; only ever
     * {@code null} before the file is successfully parsed
     */
    private MethodList methods;

    /**
     * {@code null-ok;} the class file field {@code attributes}; only
     * ever {@code null} before the file is successfully
     * parsed
     */
    private StdAttributeList attributes;

    /** {@code null-ok;} attribute factory, if any */
    private AttributeFactory attributeFactory;

    /** {@code null-ok;} parse observer, if any */
    private ParseObserver observer;

    /**
     * Returns the string form of an object or {@code "(none)"}
     * (rather than {@code "null"}) for {@code null}.
     *
     * @param obj {@code null-ok;} the object to stringify
     * @return {@code non-null;} the appropriate string form
     */
    public static String stringOrNone(Object obj) {
        if (obj == null) {
            return "(none)";
        }

        return obj.toString();
    }

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} the bytes of the file
     * @param filePath {@code non-null;} the file path for the class,
     * excluding any base directory specification
     * @param strictParse whether to be strict about parsing; if
     * {@code false}, this avoids doing checks that only exist
     * for purposes of verification (such as magic number matching and
     * path-package consistency checking)
     */
    public DirectClassFile(ByteArray bytes, String filePath,
                           boolean strictParse) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }

        if (filePath == null) {
            throw new NullPointerException("filePath == null");
        }

        this.filePath = filePath;
        this.bytes = bytes;
        this.strictParse = strictParse;
        this.accessFlags = -1;
    }

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} the bytes of the file
     * @param filePath {@code non-null;} the file path for the class,
     * excluding any base directory specification
     * @param strictParse whether to be strict about parsing; if
     * {@code false}, this avoids doing checks that only exist
     * for purposes of verification (such as magic number matching and
     * path-package consistency checking)
     */
    public DirectClassFile(byte[] bytes, String filePath,
                           boolean strictParse) {
        this(new ByteArray(bytes), filePath, strictParse);
    }

    /**
     * Sets the parse observer for this instance.
     *
     * @param observer {@code null-ok;} the observer
     */
    public void setObserver(ParseObserver observer) {
        this.observer = observer;
    }

    /**
     * Sets the attribute factory to use.
     *
     * @param attributeFactory {@code non-null;} the attribute factory
     */
    public void setAttributeFactory(AttributeFactory attributeFactory) {
        if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        }

        this.attributeFactory = attributeFactory;
    }

    /**
     * Gets the path where this class file is located.
     *
     * @return {@code non-null;} the filePath
     */
    public String getFilePath() {
      return filePath;
    }

    /**
     * Gets the {@link ByteArray} that this instance's data comes from.
     *
     * @return {@code non-null;} the bytes
     */
    public ByteArray getBytes() {
        return bytes;
    }

    /** {@inheritDoc} */
    @Override
    public int getMagic() {
        parseToInterfacesIfNecessary();
        return getMagic0();
    }

    /** {@inheritDoc} */
    @Override
    public int getMinorVersion() {
        parseToInterfacesIfNecessary();
        return getMinorVersion0();
    }

    /** {@inheritDoc} */
    @Override
    public int getMajorVersion() {
        parseToInterfacesIfNecessary();
        return getMajorVersion0();
    }

    /** {@inheritDoc} */
    @Override
    public int getAccessFlags() {
        parseToInterfacesIfNecessary();
        return accessFlags;
    }

    /** {@inheritDoc} */
    @Override
    public CstType getThisClass() {
        parseToInterfacesIfNecessary();
        return thisClass;
    }

    /** {@inheritDoc} */
    @Override
    public CstType getSuperclass() {
        parseToInterfacesIfNecessary();
        return superClass;
    }

    /** {@inheritDoc} */
    @Override
    public ConstantPool getConstantPool() {
        parseToInterfacesIfNecessary();
        return pool;
    }

    /** {@inheritDoc} */
    @Override
    public TypeList getInterfaces() {
        parseToInterfacesIfNecessary();
        return interfaces;
    }

    /** {@inheritDoc} */
    @Override
    public FieldList getFields() {
        parseToEndIfNecessary();
        return fields;
    }

    /** {@inheritDoc} */
    @Override
    public MethodList getMethods() {
        parseToEndIfNecessary();
        return methods;
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList getAttributes() {
        parseToEndIfNecessary();
        return attributes;
    }

    /** {@inheritDoc} */
    @Override
    public BootstrapMethodsList getBootstrapMethods() {
        AttBootstrapMethods bootstrapMethodsAttribute =
                (AttBootstrapMethods) getAttributes().findFirst(AttBootstrapMethods.ATTRIBUTE_NAME);
        if (bootstrapMethodsAttribute != null) {
            return bootstrapMethodsAttribute.getBootstrapMethods();
        } else {
            return BootstrapMethodsList.EMPTY;
        }
    }

    /** {@inheritDoc} */
    @Override
    public CstString getSourceFile() {
        AttributeList attribs = getAttributes();
        Attribute attSf = attribs.findFirst(AttSourceFile.ATTRIBUTE_NAME);

        if (attSf instanceof AttSourceFile) {
            return ((AttSourceFile) attSf).getSourceFile();
        }

        return null;
    }

    /**
     * Constructs and returns an instance of {@link TypeList} whose
     * data comes from the bytes of this instance, interpreted as a
     * list of constant pool indices for classes, which are in turn
     * translated to type constants. Instance construction will fail
     * if any of the (alleged) indices turn out not to refer to
     * constant pool entries of type {@code Class}.
     *
     * @param offset offset into {@link #bytes} for the start of the
     * data
     * @param size number of elements in the list (not number of bytes)
     * @return {@code non-null;} an appropriately-constructed class list
     */
    public TypeList makeTypeList(int offset, int size) {
        if (size == 0) {
            return StdTypeList.EMPTY;
        }

        if (pool == null) {
            throw new IllegalStateException("pool not yet initialized");
        }

        return new DcfTypeList(bytes, offset, size, pool, observer);
    }

    /**
     * Gets the class file field {@code magic}, but without doing any
     * checks or parsing first.
     *
     * @return the magic value
     */
    public int getMagic0() {
        return bytes.getInt(0);
    }

    /**
     * Gets the class file field {@code minor_version}, but
     * without doing any checks or parsing first.
     *
     * @return the minor version
     */
    public int getMinorVersion0() {
        return bytes.getUnsignedShort(4);
    }

    /**
     * Gets the class file field {@code major_version}, but
     * without doing any checks or parsing first.
     *
     * @return the major version
     */
    public int getMajorVersion0() {
        return bytes.getUnsignedShort(6);
    }

    /**
     * Runs {@link #parse} if it has not yet been run to cover up to
     * the interfaces list.
     */
    private void parseToInterfacesIfNecessary() {
        if (accessFlags == -1) {
            parse();
        }
    }

    /**
     * Runs {@link #parse} if it has not yet been run successfully.
     */
    private void parseToEndIfNecessary() {
        if (attributes == null) {
            parse();
        }
    }

    /**
     * Does the parsing, handing exceptions.
     */
    private void parse() {
        try {
            parse0();
        } catch (ParseException ex) {
            ex.addContext("...while parsing " + filePath);
            throw ex;
        } catch (RuntimeException ex) {
            ParseException pe = new ParseException(ex);
            pe.addContext("...while parsing " + filePath);
            throw pe;
        }
    }

    /**
     * Sees if the .class file header magic has the good value.
     *
     * @param magic the value of a classfile "magic" field
     * @return true if the magic is valid
     */
    private boolean isGoodMagic(int magic) {
        return magic == CLASS_FILE_MAGIC;
    }

    /**
     * Sees if the .class file header version are within
     * range.
     *
     * @param minorVersion the value of a classfile "minor_version" field
     * @param majorVersion the value of a classfile "major_version" field
     * @return true if the parameters are valid and within range
     */
    private boolean isGoodVersion(int minorVersion, int majorVersion) {
        /* Valid version ranges are typically of the form
         * "A.0 through B.C inclusive" where A <= B and C >= 0,
         * which is why we don't have a CLASS_FILE_MIN_MINOR_VERSION.
         */
        if (minorVersion >= 0) {
            /* Check against max first to handle the case where
             * MIN_MAJOR == MAX_MAJOR.
             */
            if (majorVersion == CLASS_FILE_MAX_MAJOR_VERSION) {
                if (minorVersion <= CLASS_FILE_MAX_MINOR_VERSION) {
                    return true;
                }
            } else if (majorVersion < CLASS_FILE_MAX_MAJOR_VERSION &&
                       majorVersion >= CLASS_FILE_MIN_MAJOR_VERSION) {
                return true;
            }
        }

        return false;
    }

    /**
     * Does the actual parsing.
     */
    private void parse0() {
        if (bytes.size() < 10) {
            throw new ParseException("severely truncated class file");
        }

        if (observer != null) {
            observer.parsed(bytes, 0, 0, "begin classfile");
            observer.parsed(bytes, 0, 4, "magic: " + Hex.u4(getMagic0()));
            observer.parsed(bytes, 4, 2,
                            "minor_version: " + Hex.u2(getMinorVersion0()));
            observer.parsed(bytes, 6, 2,
                            "major_version: " + Hex.u2(getMajorVersion0()));
        }

        if (strictParse) {
            /* Make sure that this looks like a valid class file with a
             * version that we can handle.
             */
            if (!isGoodMagic(getMagic0())) {
                throw new ParseException("bad class file magic (" + Hex.u4(getMagic0()) + ")");
            }

            if (!isGoodVersion(getMinorVersion0(), getMajorVersion0())) {
                throw new ParseException("unsupported class file version " +
                                         getMajorVersion0() + "." +
                                         getMinorVersion0());
            }
        }

        ConstantPoolParser cpParser = new ConstantPoolParser(bytes);
        cpParser.setObserver(observer);
        pool = cpParser.getPool();
        pool.setImmutable();

        int at = cpParser.getEndOffset();
        int accessFlags = bytes.getUnsignedShort(at); // u2 access_flags;
        int cpi = bytes.getUnsignedShort(at + 2); // u2 this_class;
        thisClass = (CstType) pool.get(cpi);
        cpi = bytes.getUnsignedShort(at + 4); // u2 super_class;
        superClass = (CstType) pool.get0Ok(cpi);
        int count = bytes.getUnsignedShort(at + 6); // u2 interfaces_count

        if (observer != null) {
            observer.parsed(bytes, at, 2,
                            "access_flags: " +
                            AccessFlags.classString(accessFlags));
            observer.parsed(bytes, at + 2, 2, "this_class: " + thisClass);
            observer.parsed(bytes, at + 4, 2, "super_class: " +
                            stringOrNone(superClass));
            observer.parsed(bytes, at + 6, 2,
                            "interfaces_count: " + Hex.u2(count));
            if (count != 0) {
                observer.parsed(bytes, at + 8, 0, "interfaces:");
            }
        }

        at += 8;
        interfaces = makeTypeList(at, count);
        at += count * 2;

        if (strictParse) {
            /*
             * Make sure that the file/jar path matches the declared
             * package/class name.
             */
            String thisClassName = thisClass.getClassType().getClassName();
            if (!(filePath.endsWith(".class") &&
                  filePath.startsWith(thisClassName) &&
                  (filePath.length() == (thisClassName.length() + 6)))) {
                throw new ParseException("class name (" + thisClassName +
                                         ") does not match path (" +
                                         filePath + ")");
            }
        }

        /*
         * Only set the instance variable accessFlags here, since
         * that's what signals a successful parse of the first part of
         * the file (through the interfaces list).
         */
        this.accessFlags = accessFlags;

        FieldListParser flParser =
            new FieldListParser(this, thisClass, at, attributeFactory);
        flParser.setObserver(observer);
        fields = flParser.getList();
        at = flParser.getEndOffset();

        MethodListParser mlParser =
            new MethodListParser(this, thisClass, at, attributeFactory);
        mlParser.setObserver(observer);
        methods = mlParser.getList();
        at = mlParser.getEndOffset();

        AttributeListParser alParser =
            new AttributeListParser(this, AttributeFactory.CTX_CLASS, at,
                                    attributeFactory);
        alParser.setObserver(observer);
        attributes = alParser.getList();
        attributes.setImmutable();
        at = alParser.getEndOffset();

        if (at != bytes.size()) {
            throw new ParseException("extra bytes at end of class file, " +
                                     "at offset " + Hex.u4(at));
        }

        if (observer != null) {
            observer.parsed(bytes, at, 0, "end classfile");
        }
    }

    /**
     * Implementation of {@link TypeList} whose data comes directly
     * from the bytes of an instance of this (outer) class,
     * interpreted as a list of constant pool indices for classes
     * which are in turn returned as type constants. Instance
     * construction will fail if any of the (alleged) indices turn out
     * not to refer to constant pool entries of type
     * {@code Class}.
     */
    private static class DcfTypeList implements TypeList {
        /** {@code non-null;} array containing the data */
        private final ByteArray bytes;

        /** number of elements in the list (not number of bytes) */
        private final int size;

        /** {@code non-null;} the constant pool */
        private final StdConstantPool pool;

        /**
         * Constructs an instance.
         *
         * @param bytes {@code non-null;} original classfile's bytes
         * @param offset offset into {@link #bytes} for the start of the
         * data
         * @param size number of elements in the list (not number of bytes)
         * @param pool {@code non-null;} the constant pool to use
         * @param observer {@code null-ok;} parse observer to use, if any
         */
        public DcfTypeList(ByteArray bytes, int offset, int size,
                StdConstantPool pool, ParseObserver observer) {
            if (size < 0) {
                throw new IllegalArgumentException("size < 0");
            }

            bytes = bytes.slice(offset, offset + size * 2);
            this.bytes = bytes;
            this.size = size;
            this.pool = pool;

            for (int i = 0; i < size; i++) {
                offset = i * 2;
                int idx = bytes.getUnsignedShort(offset);
                CstType type;
                try {
                    type = (CstType) pool.get(idx);
                } catch (ClassCastException ex) {
                    // Translate the exception.
                    throw new RuntimeException("bogus class cpi", ex);
                }
                if (observer != null) {
                    observer.parsed(bytes, offset, 2, "  " + type);
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean isMutable() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public int size() {
            return size;
        }

        /** {@inheritDoc} */
        @Override
        public int getWordCount() {
            // It is the same as size because all elements are classes.
            return size;
        }

        /** {@inheritDoc} */
        @Override
        public Type getType(int n) {
            int idx = bytes.getUnsignedShort(n * 2);
            return ((CstType) pool.get(idx)).getClassType();
        }

        /** {@inheritDoc} */
        @Override
        public TypeList withAddedType(Type type) {
            throw new UnsupportedOperationException("unsupported");
        }
    }
}

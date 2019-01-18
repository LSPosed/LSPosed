/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package external.org.apache.commons.lang3;

import java.io.File;

/**
 * <p>
 * Helpers for {@code java.lang.System}.
 * </p>
 * <p>
 * If a system property cannot be read due to security restrictions, the corresponding field in this class will be set
 * to {@code null} and a message will be written to {@code System.err}.
 * </p>
 * <p>
 * #ThreadSafe#
 * </p>
 *
 * @since 1.0
 * @version $Id: SystemUtils.java 1199816 2011-11-09 16:11:34Z bayard $
 */
public class SystemUtils {

    /**
     * The prefix String for all Windows OS.
     */
    private static final String OS_NAME_WINDOWS_PREFIX = "Windows";

    // System property constants
    // -----------------------------------------------------------------------
    // These MUST be declared first. Other constants depend on this.

    /**
     * The System property key for the user home directory.
     */
    private static final String USER_HOME_KEY = "user.home";

    /**
     * The System property key for the user directory.
     */
    private static final String USER_DIR_KEY = "user.dir";

    /**
     * The System property key for the Java IO temporary directory.
     */
    private static final String JAVA_IO_TMPDIR_KEY = "java.io.tmpdir";

    /**
     * The System property key for the Java home directory.
     */
    private static final String JAVA_HOME_KEY = "java.home";

    /**
     * <p>
     * The {@code awt.toolkit} System Property.
     * </p>
     * <p>
     * Holds a class name, on Windows XP this is {@code sun.awt.windows.WToolkit}.
     * </p>
     * <p>
     * <b>On platforms without a GUI, this value is {@code null}.</b>
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.1
     */
    public static final String AWT_TOOLKIT = getSystemProperty("awt.toolkit");

    /**
     * <p>
     * The {@code file.encoding} System Property.
     * </p>
     * <p>
     * File encoding, such as {@code Cp1252}.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.0
     * @since Java 1.2
     */
    public static final String FILE_ENCODING = getSystemProperty("file.encoding");

    /**
     * <p>
     * The {@code file.separator} System Property. File separator (<code>&quot;/&quot;</code> on UNIX).
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String FILE_SEPARATOR = getSystemProperty("file.separator");

    /**
     * <p>
     * The {@code java.awt.fonts} System Property.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.1
     */
    public static final String JAVA_AWT_FONTS = getSystemProperty("java.awt.fonts");

    /**
     * <p>
     * The {@code java.awt.graphicsenv} System Property.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.1
     */
    public static final String JAVA_AWT_GRAPHICSENV = getSystemProperty("java.awt.graphicsenv");

    /**
     * <p>
     * The {@code java.awt.headless} System Property. The value of this property is the String {@code "true"} or
     * {@code "false"}.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @see #isJavaAwtHeadless()
     * @since 2.1
     * @since Java 1.4
     */
    public static final String JAVA_AWT_HEADLESS = getSystemProperty("java.awt.headless");

    /**
     * <p>
     * The {@code java.awt.printerjob} System Property.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.1
     */
    public static final String JAVA_AWT_PRINTERJOB = getSystemProperty("java.awt.printerjob");

    /**
     * <p>
     * The {@code java.class.path} System Property. Java class path.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String JAVA_CLASS_PATH = getSystemProperty("java.class.path");

    /**
     * <p>
     * The {@code java.class.version} System Property. Java class format version number.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String JAVA_CLASS_VERSION = getSystemProperty("java.class.version");

    /**
     * <p>
     * The {@code java.compiler} System Property. Name of JIT compiler to use. First in JDK version 1.2. Not used in Sun
     * JDKs after 1.2.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2. Not used in Sun versions after 1.2.
     */
    public static final String JAVA_COMPILER = getSystemProperty("java.compiler");

    /**
     * <p>
     * The {@code java.endorsed.dirs} System Property. Path of endorsed directory or directories.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.4
     */
    public static final String JAVA_ENDORSED_DIRS = getSystemProperty("java.endorsed.dirs");

    /**
     * <p>
     * The {@code java.ext.dirs} System Property. Path of extension directory or directories.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.3
     */
    public static final String JAVA_EXT_DIRS = getSystemProperty("java.ext.dirs");

    /**
     * <p>
     * The {@code java.home} System Property. Java installation directory.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String JAVA_HOME = getSystemProperty(JAVA_HOME_KEY);

    /**
     * <p>
     * The {@code java.io.tmpdir} System Property. Default temp file path.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_IO_TMPDIR = getSystemProperty(JAVA_IO_TMPDIR_KEY);

    /**
     * <p>
     * The {@code java.library.path} System Property. List of paths to search when loading libraries.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_LIBRARY_PATH = getSystemProperty("java.library.path");

    /**
     * <p>
     * The {@code java.runtime.name} System Property. Java Runtime Environment name.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.0
     * @since Java 1.3
     */
    public static final String JAVA_RUNTIME_NAME = getSystemProperty("java.runtime.name");

    /**
     * <p>
     * The {@code java.runtime.version} System Property. Java Runtime Environment version.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.0
     * @since Java 1.3
     */
    public static final String JAVA_RUNTIME_VERSION = getSystemProperty("java.runtime.version");

    /**
     * <p>
     * The {@code java.specification.name} System Property. Java Runtime Environment specification name.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_SPECIFICATION_NAME = getSystemProperty("java.specification.name");

    /**
     * <p>
     * The {@code java.specification.vendor} System Property. Java Runtime Environment specification vendor.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_SPECIFICATION_VENDOR = getSystemProperty("java.specification.vendor");

    /**
     * <p>
     * The {@code java.specification.version} System Property. Java Runtime Environment specification version.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.3
     */
    public static final String JAVA_SPECIFICATION_VERSION = getSystemProperty("java.specification.version");
    private static final JavaVersion JAVA_SPECIFICATION_VERSION_AS_ENUM = JavaVersion.get(JAVA_SPECIFICATION_VERSION);

    /**
     * <p>
     * The {@code java.util.prefs.PreferencesFactory} System Property. A class name.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.1
     * @since Java 1.4
     */
    public static final String JAVA_UTIL_PREFS_PREFERENCES_FACTORY =
        getSystemProperty("java.util.prefs.PreferencesFactory");

    /**
     * <p>
     * The {@code java.vendor} System Property. Java vendor-specific string.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String JAVA_VENDOR = getSystemProperty("java.vendor");

    /**
     * <p>
     * The {@code java.vendor.url} System Property. Java vendor URL.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String JAVA_VENDOR_URL = getSystemProperty("java.vendor.url");

    /**
     * <p>
     * The {@code java.version} System Property. Java version number.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String JAVA_VERSION = getSystemProperty("java.version");

    /**
     * <p>
     * The {@code java.vm.info} System Property. Java Virtual Machine implementation info.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.0
     * @since Java 1.2
     */
    public static final String JAVA_VM_INFO = getSystemProperty("java.vm.info");

    /**
     * <p>
     * The {@code java.vm.name} System Property. Java Virtual Machine implementation name.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_VM_NAME = getSystemProperty("java.vm.name");

    /**
     * <p>
     * The {@code java.vm.specification.name} System Property. Java Virtual Machine specification name.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_VM_SPECIFICATION_NAME = getSystemProperty("java.vm.specification.name");

    /**
     * <p>
     * The {@code java.vm.specification.vendor} System Property. Java Virtual Machine specification vendor.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_VM_SPECIFICATION_VENDOR = getSystemProperty("java.vm.specification.vendor");

    /**
     * <p>
     * The {@code java.vm.specification.version} System Property. Java Virtual Machine specification version.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_VM_SPECIFICATION_VERSION = getSystemProperty("java.vm.specification.version");

    /**
     * <p>
     * The {@code java.vm.vendor} System Property. Java Virtual Machine implementation vendor.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_VM_VENDOR = getSystemProperty("java.vm.vendor");

    /**
     * <p>
     * The {@code java.vm.version} System Property. Java Virtual Machine implementation version.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.2
     */
    public static final String JAVA_VM_VERSION = getSystemProperty("java.vm.version");

    /**
     * <p>
     * The {@code line.separator} System Property. Line separator (<code>&quot;\n&quot;</code> on UNIX).
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String LINE_SEPARATOR = getSystemProperty("line.separator");

    /**
     * <p>
     * The {@code os.arch} System Property. Operating system architecture.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String OS_ARCH = getSystemProperty("os.arch");

    /**
     * <p>
     * The {@code os.name} System Property. Operating system name.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String OS_NAME = getSystemProperty("os.name");

    /**
     * <p>
     * The {@code os.version} System Property. Operating system version.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String OS_VERSION = getSystemProperty("os.version");

    /**
     * <p>
     * The {@code path.separator} System Property. Path separator (<code>&quot;:&quot;</code> on UNIX).
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String PATH_SEPARATOR = getSystemProperty("path.separator");

    /**
     * <p>
     * The {@code user.country} or {@code user.region} System Property. User's country code, such as {@code GB}. First
     * in Java version 1.2 as {@code user.region}. Renamed to {@code user.country} in 1.4
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.0
     * @since Java 1.2
     */
    public static final String USER_COUNTRY = getSystemProperty("user.country") == null ?
            getSystemProperty("user.region") : getSystemProperty("user.country");

    /**
     * <p>
     * The {@code user.dir} System Property. User's current working directory.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String USER_DIR = getSystemProperty(USER_DIR_KEY);

    /**
     * <p>
     * The {@code user.home} System Property. User's home directory.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String USER_HOME = getSystemProperty(USER_HOME_KEY);

    /**
     * <p>
     * The {@code user.language} System Property. User's language code, such as {@code "en"}.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.0
     * @since Java 1.2
     */
    public static final String USER_LANGUAGE = getSystemProperty("user.language");

    /**
     * <p>
     * The {@code user.name} System Property. User's account name.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String USER_NAME = getSystemProperty("user.name");

    /**
     * <p>
     * The {@code user.timezone} System Property. For example: {@code "America/Los_Angeles"}.
     * </p>
     * <p>
     * Defaults to {@code null} if the runtime does not have security access to read this property or the property does
     * not exist.
     * </p>
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of
     * sync with that System property.
     * </p>
     *
     * @since 2.1
     */
    public static final String USER_TIMEZONE = getSystemProperty("user.timezone");

    // Java version checks
    // -----------------------------------------------------------------------
    // These MUST be declared after those above as they depend on the
    // values being set up

    /**
     * <p>
     * Is {@code true} if this is Java version 1.1 (also 1.1.x versions).
     * </p>
     * <p>
     * The field will return {@code false} if {@link #JAVA_VERSION} is {@code null}.
     * </p>
     */
    public static final boolean IS_JAVA_1_1 = getJavaVersionMatches("1.1");

    /**
     * <p>
     * Is {@code true} if this is Java version 1.2 (also 1.2.x versions).
     * </p>
     * <p>
     * The field will return {@code false} if {@link #JAVA_VERSION} is {@code null}.
     * </p>
     */
    public static final boolean IS_JAVA_1_2 = getJavaVersionMatches("1.2");

    /**
     * <p>
     * Is {@code true} if this is Java version 1.3 (also 1.3.x versions).
     * </p>
     * <p>
     * The field will return {@code false} if {@link #JAVA_VERSION} is {@code null}.
     * </p>
     */
    public static final boolean IS_JAVA_1_3 = getJavaVersionMatches("1.3");

    /**
     * <p>
     * Is {@code true} if this is Java version 1.4 (also 1.4.x versions).
     * </p>
     * <p>
     * The field will return {@code false} if {@link #JAVA_VERSION} is {@code null}.
     * </p>
     */
    public static final boolean IS_JAVA_1_4 = getJavaVersionMatches("1.4");

    /**
     * <p>
     * Is {@code true} if this is Java version 1.5 (also 1.5.x versions).
     * </p>
     * <p>
     * The field will return {@code false} if {@link #JAVA_VERSION} is {@code null}.
     * </p>
     */
    public static final boolean IS_JAVA_1_5 = getJavaVersionMatches("1.5");

    /**
     * <p>
     * Is {@code true} if this is Java version 1.6 (also 1.6.x versions).
     * </p>
     * <p>
     * The field will return {@code false} if {@link #JAVA_VERSION} is {@code null}.
     * </p>
     */
    public static final boolean IS_JAVA_1_6 = getJavaVersionMatches("1.6");

    /**
     * <p>
     * Is {@code true} if this is Java version 1.7 (also 1.7.x versions).
     * </p>
     * <p>
     * The field will return {@code false} if {@link #JAVA_VERSION} is {@code null}.
     * </p>
     *
     * @since 3.0
     */
    public static final boolean IS_JAVA_1_7 = getJavaVersionMatches("1.7");

    // Operating system checks
    // -----------------------------------------------------------------------
    // These MUST be declared after those above as they depend on the
    // values being set up
    // OS names from http://www.vamphq.com/os.html
    // Selected ones included - please advise dev@commons.apache.org
    // if you want another added or a mistake corrected

    /**
     * <p>
     * Is {@code true} if this is AIX.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_AIX = getOSMatchesName("AIX");

    /**
     * <p>
     * Is {@code true} if this is HP-UX.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_HP_UX = getOSMatchesName("HP-UX");

    /**
     * <p>
     * Is {@code true} if this is Irix.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_IRIX = getOSMatchesName("Irix");

    /**
     * <p>
     * Is {@code true} if this is Linux.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_LINUX = getOSMatchesName("Linux") || getOSMatchesName("LINUX");

    /**
     * <p>
     * Is {@code true} if this is Mac.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_MAC = getOSMatchesName("Mac");

    /**
     * <p>
     * Is {@code true} if this is Mac.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_MAC_OSX = getOSMatchesName("Mac OS X");

    /**
     * <p>
     * Is {@code true} if this is FreeBSD.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 3.1
     */
    public static final boolean IS_OS_FREE_BSD = getOSMatchesName("FreeBSD");

    /**
     * <p>
     * Is {@code true} if this is OpenBSD.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 3.1
     */
    public static final boolean IS_OS_OPEN_BSD = getOSMatchesName("OpenBSD");

    /**
     * <p>
     * Is {@code true} if this is NetBSD.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 3.1
     */
    public static final boolean IS_OS_NET_BSD = getOSMatchesName("NetBSD");

    /**
     * <p>
     * Is {@code true} if this is OS/2.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_OS2 = getOSMatchesName("OS/2");

    /**
     * <p>
     * Is {@code true} if this is Solaris.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_SOLARIS = getOSMatchesName("Solaris");

    /**
     * <p>
     * Is {@code true} if this is SunOS.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_SUN_OS = getOSMatchesName("SunOS");

    /**
     * <p>
     * Is {@code true} if this is a UNIX like system, as in any of AIX, HP-UX, Irix, Linux, MacOSX, Solaris or SUN OS.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.1
     */
    public static final boolean IS_OS_UNIX = IS_OS_AIX || IS_OS_HP_UX || IS_OS_IRIX || IS_OS_LINUX || IS_OS_MAC_OSX
            || IS_OS_SOLARIS || IS_OS_SUN_OS || IS_OS_FREE_BSD || IS_OS_OPEN_BSD || IS_OS_NET_BSD;

    /**
     * <p>
     * Is {@code true} if this is Windows.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS = getOSMatchesName(OS_NAME_WINDOWS_PREFIX);

    /**
     * <p>
     * Is {@code true} if this is Windows 2000.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_2000 = getOSMatches(OS_NAME_WINDOWS_PREFIX, "5.0");

    /**
     * <p>
     * Is {@code true} if this is Windows 2003.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 3.1
     */
    public static final boolean IS_OS_WINDOWS_2003 = getOSMatches(OS_NAME_WINDOWS_PREFIX, "5.2");

    /**
     * <p>
     * Is {@code true} if this is Windows 2008.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 3.1
     */
    public static final boolean IS_OS_WINDOWS_2008 = getOSMatches(OS_NAME_WINDOWS_PREFIX + " Server 2008", "6.1");

    /**
     * <p>
     * Is {@code true} if this is Windows 95.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_95 = getOSMatches(OS_NAME_WINDOWS_PREFIX + " 9", "4.0");
    // Java 1.2 running on Windows98 returns 'Windows 95', hence the above

    /**
     * <p>
     * Is {@code true} if this is Windows 98.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_98 = getOSMatches(OS_NAME_WINDOWS_PREFIX + " 9", "4.1");
    // Java 1.2 running on Windows98 returns 'Windows 95', hence the above

    /**
     * <p>
     * Is {@code true} if this is Windows ME.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_ME = getOSMatches(OS_NAME_WINDOWS_PREFIX, "4.9");
    // Java 1.2 running on WindowsME may return 'Windows 95', hence the above

    /**
     * <p>
     * Is {@code true} if this is Windows NT.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_NT = getOSMatchesName(OS_NAME_WINDOWS_PREFIX + " NT");
    // Windows 2000 returns 'Windows 2000' but may suffer from same Java1.2 problem

    /**
     * <p>
     * Is {@code true} if this is Windows XP.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.0
     */
    public static final boolean IS_OS_WINDOWS_XP = getOSMatches(OS_NAME_WINDOWS_PREFIX, "5.1");

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Is {@code true} if this is Windows Vista.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 2.4
     */
    public static final boolean IS_OS_WINDOWS_VISTA = getOSMatches(OS_NAME_WINDOWS_PREFIX, "6.0");

    /**
     * <p>
     * Is {@code true} if this is Windows 7.
     * </p>
     * <p>
     * The field will return {@code false} if {@code OS_NAME} is {@code null}.
     * </p>
     *
     * @since 3.0
     */
    public static final boolean IS_OS_WINDOWS_7 = getOSMatches(OS_NAME_WINDOWS_PREFIX, "6.1");

    /**
     * <p>
     * Gets the Java home directory as a {@code File}.
     * </p>
     *
     * @return a directory
     * @throws SecurityException if a security manager exists and its {@code checkPropertyAccess} method doesn't allow
     * access to the specified system property.
     * @see System#getProperty(String)
     * @since 2.1
     */
    public static File getJavaHome() {
        return new File(System.getProperty(JAVA_HOME_KEY));
    }

    /**
     * <p>
     * Gets the Java IO temporary directory as a {@code File}.
     * </p>
     *
     * @return a directory
     * @throws SecurityException if a security manager exists and its {@code checkPropertyAccess} method doesn't allow
     * access to the specified system property.
     * @see System#getProperty(String)
     * @since 2.1
     */
    public static File getJavaIoTmpDir() {
        return new File(System.getProperty(JAVA_IO_TMPDIR_KEY));
    }

    /**
     * <p>
     * Decides if the Java version matches.
     * </p>
     *
     * @param versionPrefix the prefix for the java version
     * @return true if matches, or false if not or can't determine
     */
    private static boolean getJavaVersionMatches(String versionPrefix) {
        return isJavaVersionMatch(JAVA_SPECIFICATION_VERSION, versionPrefix);
    }

    /**
     * Decides if the operating system matches.
     *
     * @param osNamePrefix the prefix for the os name
     * @param osVersionPrefix the prefix for the version
     * @return true if matches, or false if not or can't determine
     */
    private static boolean getOSMatches(String osNamePrefix, String osVersionPrefix) {
        return isOSMatch(OS_NAME, OS_VERSION, osNamePrefix, osVersionPrefix);
    }

    /**
     * Decides if the operating system matches.
     *
     * @param osNamePrefix the prefix for the os name
     * @return true if matches, or false if not or can't determine
     */
    private static boolean getOSMatchesName(String osNamePrefix) {
        return isOSNameMatch(OS_NAME, osNamePrefix);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets a System property, defaulting to {@code null} if the property cannot be read.
     * </p>
     * <p>
     * If a {@code SecurityException} is caught, the return value is {@code null} and a message is written to
     * {@code System.err}.
     * </p>
     *
     * @param property the system property name
     * @return the system property value or {@code null} if a security problem occurs
     */
    private static String getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException ex) {
            // we are not allowed to look at this property
            System.err.println("Caught a SecurityException reading the system property '" + property
                    + "'; the SystemUtils property value will default to null.");
            return null;
        }
    }

    /**
     * <p>
     * Gets the user directory as a {@code File}.
     * </p>
     *
     * @return a directory
     * @throws SecurityException if a security manager exists and its {@code checkPropertyAccess} method doesn't allow
     * access to the specified system property.
     * @see System#getProperty(String)
     * @since 2.1
     */
    public static File getUserDir() {
        return new File(System.getProperty(USER_DIR_KEY));
    }

    /**
     * <p>
     * Gets the user home directory as a {@code File}.
     * </p>
     *
     * @return a directory
     * @throws SecurityException if a security manager exists and its {@code checkPropertyAccess} method doesn't allow
     * access to the specified system property.
     * @see System#getProperty(String)
     * @since 2.1
     */
    public static File getUserHome() {
        return new File(System.getProperty(USER_HOME_KEY));
    }

    /**
     * Returns whether the {@link #JAVA_AWT_HEADLESS} value is {@code true}.
     *
     * @return {@code true} if {@code JAVA_AWT_HEADLESS} is {@code "true"}, {@code false} otherwise.
     * @see #JAVA_AWT_HEADLESS
     * @since 2.1
     * @since Java 1.4
     */
    public static boolean isJavaAwtHeadless() {
        return JAVA_AWT_HEADLESS != null ? JAVA_AWT_HEADLESS.equals(Boolean.TRUE.toString()) : false;
    }

    /**
     * <p>
     * Is the Java version at least the requested version.
     * </p>
     * <p>
     * Example input:
     * </p>
     * <ul>
     * <li>{@code 1.2f} to test for Java 1.2</li>
     * <li>{@code 1.31f} to test for Java 1.3.1</li>
     * </ul>
     *
     * @param requiredVersion the required version, for example 1.31f
     * @return {@code true} if the actual version is equal or greater than the required version
     */
    public static boolean isJavaVersionAtLeast(JavaVersion requiredVersion) {
        return JAVA_SPECIFICATION_VERSION_AS_ENUM.atLeast(requiredVersion);
    }

    /**
     * <p>
     * Decides if the Java version matches.
     * </p>
     * <p>
     * This method is package private instead of private to support unit test invocation.
     * </p>
     *
     * @param version the actual Java version
     * @param versionPrefix the prefix for the expected Java version
     * @return true if matches, or false if not or can't determine
     */
    static boolean isJavaVersionMatch(String version, String versionPrefix) {
        if (version == null) {
            return false;
        }
        return version.startsWith(versionPrefix);
    }

    /**
     * Decides if the operating system matches.
     * <p>
     * This method is package private instead of private to support unit test invocation.
     * </p>
     *
     * @param osName the actual OS name
     * @param osVersion the actual OS version
     * @param osNamePrefix the prefix for the expected OS name
     * @param osVersionPrefix the prefix for the expected OS version
     * @return true if matches, or false if not or can't determine
     */
    static boolean isOSMatch(String osName, String osVersion, String osNamePrefix, String osVersionPrefix) {
        if (osName == null || osVersion == null) {
            return false;
        }
        return osName.startsWith(osNamePrefix) && osVersion.startsWith(osVersionPrefix);
    }

    /**
     * Decides if the operating system matches.
     * <p>
     * This method is package private instead of private to support unit test invocation.
     * </p>
     *
     * @param osName the actual OS name
     * @param osNamePrefix the prefix for the expected OS name
     * @return true if matches, or false if not or can't determine
     */
    static boolean isOSNameMatch(String osName, String osNamePrefix) {
        if (osName == null) {
            return false;
        }
        return osName.startsWith(osNamePrefix);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * SystemUtils instances should NOT be constructed in standard programming. Instead, the class should be used as
     * {@code SystemUtils.FILE_SEPARATOR}.
     * </p>
     * <p>
     * This constructor is public to permit tools that require a JavaBean instance to operate.
     * </p>
     */
    public SystemUtils() {
        super();
    }

}

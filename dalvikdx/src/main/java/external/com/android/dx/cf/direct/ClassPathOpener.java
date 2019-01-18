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

import external.com.android.dex.util.FileUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Opens all the class files found in a class path element. Path elements
 * can point to class files, {jar,zip,apk} files, or directories containing
 * class files.
 */
public class ClassPathOpener {

    /** {@code non-null;} pathname to start with */
    private final String pathname;
    /** {@code non-null;} callback interface */
    private final Consumer consumer;
    /**
     * If true, sort such that classes appear before their inner
     * classes and "package-info" occurs before all other classes in that
     * package.
     */
    private final boolean sort;
    private FileNameFilter filter;

    /**
     * Callback interface for {@code ClassOpener}.
     */
    public interface Consumer {

        /**
         * Provides the file name and byte array for a class path element.
         *
         * @param name {@code non-null;} filename of element. May not be a valid
         * filesystem path.
         *
         * @param lastModified milliseconds since 1970-Jan-1 00:00:00 GMT
         * @param bytes {@code non-null;} file data
         * @return true on success. Result is or'd with all other results
         * from {@code processFileBytes} and returned to the caller
         * of {@code process()}.
         */
        boolean processFileBytes(String name, long lastModified, byte[] bytes);

        /**
         * Informs consumer that an exception occurred while processing
         * this path element. Processing will continue if possible.
         *
         * @param ex {@code non-null;} exception
         */
        void onException(Exception ex);

        /**
         * Informs consumer that processing of an archive file has begun.
         *
         * @param file {@code non-null;} archive file being processed
         */
        void onProcessArchiveStart(File file);
    }

    /**
     * Filter interface for {@code ClassOpener}.
     */
    public interface FileNameFilter {

        boolean accept(String path);
    }

    /**
     * An accept all filter.
     */
    public static final FileNameFilter acceptAll = new FileNameFilter() {

        @Override
        public boolean accept(String path) {
            return true;
        }
    };

    /**
     * Constructs an instance.
     *
     * @param pathname {@code non-null;} path element to process
     * @param sort if true, sort such that classes appear before their inner
     * classes and "package-info" occurs before all other classes in that
     * package.
     * @param consumer {@code non-null;} callback interface
     */
    public ClassPathOpener(String pathname, boolean sort, Consumer consumer) {
        this(pathname, sort, acceptAll, consumer);
    }

    /**
     * Constructs an instance.
     *
     * @param pathname {@code non-null;} path element to process
     * @param sort if true, sort such that classes appear before their inner
     * classes and "package-info" occurs before all other classes in that
     * package.
     * @param consumer {@code non-null;} callback interface
     */
    public ClassPathOpener(String pathname, boolean sort, FileNameFilter filter,
            Consumer consumer) {
        this.pathname = pathname;
        this.sort = sort;
        this.consumer = consumer;
        this.filter = filter;
    }

    /**
     * Processes a path element.
     *
     * @return the OR of all return values
     * from {@code Consumer.processFileBytes()}.
     */
    public boolean process() {
        File file = new File(pathname);

        return processOne(file, true);
    }

    /**
     * Processes one file.
     *
     * @param file {@code non-null;} the file to process
     * @param topLevel whether this is a top-level file (that is,
     * specified directly on the commandline)
     * @return whether any processing actually happened
     */
    private boolean processOne(File file, boolean topLevel) {
        try {
            if (file.isDirectory()) {
                return processDirectory(file, topLevel);
            }

            String path = file.getPath();

            if (path.endsWith(".zip") ||
                    path.endsWith(".jar") ||
                    path.endsWith(".apk")) {
                return processArchive(file);
            }
            if (filter.accept(path)) {
                byte[] bytes = FileUtils.readFile(file);
                return consumer.processFileBytes(path, file.lastModified(), bytes);
            } else {
                return false;
            }
        } catch (Exception ex) {
            consumer.onException(ex);
            return false;
        }
    }

    /**
     * Sorts java class names such that outer classes preceed their inner
     * classes and "package-info" preceeds all other classes in its package.
     *
     * @param a {@code non-null;} first class name
     * @param b {@code non-null;} second class name
     * @return {@code compareTo()}-style result
     */
    private static int compareClassNames(String a, String b) {
        // Ensure inner classes sort second
        a = a.replace('$','0');
        b = b.replace('$','0');

        /*
         * Assuming "package-info" only occurs at the end, ensures package-info
         * sorts first.
         */
        a = a.replace("package-info", "");
        b = b.replace("package-info", "");

        return a.compareTo(b);
    }

    /**
     * Processes a directory recursively.
     *
     * @param dir {@code non-null;} file representing the directory
     * @param topLevel whether this is a top-level directory (that is,
     * specified directly on the commandline)
     * @return whether any processing actually happened
     */
    private boolean processDirectory(File dir, boolean topLevel) {
        if (topLevel) {
            dir = new File(dir, ".");
        }

        File[] files = dir.listFiles();
        int len = files.length;
        boolean any = false;

        if (sort) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return compareClassNames(a.getName(), b.getName());
                }
            });
        }

        for (int i = 0; i < len; i++) {
            any |= processOne(files[i], false);
        }

        return any;
    }

    /**
     * Processes the contents of an archive ({@code .zip},
     * {@code .jar}, or {@code .apk}).
     *
     * @param file {@code non-null;} archive file to process
     * @return whether any processing actually happened
     * @throws IOException on i/o problem
     */
    private boolean processArchive(File file) throws IOException {
        ZipFile zip = new ZipFile(file);

        ArrayList<? extends java.util.zip.ZipEntry> entriesList
                = Collections.list(zip.entries());

        if (sort) {
            Collections.sort(entriesList, new Comparator<ZipEntry>() {
               @Override
               public int compare (ZipEntry a, ZipEntry b) {
                   return compareClassNames(a.getName(), b.getName());
               }
            });
        }

        consumer.onProcessArchiveStart(file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(40000);
        byte[] buf = new byte[20000];
        boolean any = false;

        for (ZipEntry one : entriesList) {
            final boolean isDirectory = one.isDirectory();

            String path = one.getName();
            if (filter.accept(path)) {
                final byte[] bytes;
                if (!isDirectory) {
                    InputStream in = zip.getInputStream(one);

                    baos.reset();
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        baos.write(buf, 0, read);
                    }

                    in.close();
                    bytes = baos.toByteArray();
                } else {
                    bytes = new byte[0];
                }

                any |= consumer.processFileBytes(path, one.getTime(), bytes);
            }
        }

        zip.close();
        return any;
    }
}

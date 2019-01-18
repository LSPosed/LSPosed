/*
 * Copyright (C) 2014 The Android Open Source Project
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

package external.com.android.multidex;

import external.com.android.dx.cf.attrib.AttRuntimeVisibleAnnotations;
import external.com.android.dx.cf.direct.DirectClassFile;
import external.com.android.dx.cf.iface.Attribute;
import external.com.android.dx.cf.iface.FieldList;
import external.com.android.dx.cf.iface.HasAttribute;
import external.com.android.dx.cf.iface.MethodList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * This is a command line tool used by mainDexClasses script to build a main dex classes list. First
 * argument of the command line is an archive, each class file contained in this archive is used to
 * identify a class that can be used during secondary dex installation, those class files
 * are not opened by this tool only their names matter. Other arguments must be zip files or
 * directories, they constitute in a classpath in with the classes named by the first argument
 * will be searched. Each searched class must be found. On each of this classes are searched for
 * their dependencies to other classes. The tool also browses for classes annotated by runtime
 * visible annotations and adds them to the list/ Finally the tools prints on standard output a list
 * of class files names suitable as content of the file argument --main-dex-list of dx.
 */
public class MainDexListBuilder {
    private static final String CLASS_EXTENSION = ".class";

    private static final int STATUS_ERROR = 1;

    private static final String EOL = System.getProperty("line.separator");

    private static final String USAGE_MESSAGE =
            "Usage:" + EOL + EOL +
            "Short version: Don't use this." + EOL + EOL +
            "Slightly longer version: This tool is used by mainDexClasses script to build" + EOL +
            "the main dex list." + EOL;

    /**
     * By default we force all classes annotated with runtime annotation to be kept in the
     * main dex list. This option disable the workaround, limiting the index pressure in the main
     * dex but exposing to the Dalvik resolution bug. The resolution bug occurs when accessing
     * annotations of a class that is not in the main dex and one of the annotations as an enum
     * parameter.
     *
     * @see <a href="https://code.google.com/p/android/issues/detail?id=78144">bug discussion</a>
     *
     */
    private static final String DISABLE_ANNOTATION_RESOLUTION_WORKAROUND =
            "--disable-annotation-resolution-workaround";

    private Set<String> filesToKeep = new HashSet<String>();

    public static void main(String[] args) {

        int argIndex = 0;
        boolean keepAnnotated = true;
        while (argIndex < args.length -2) {
            if (args[argIndex].equals(DISABLE_ANNOTATION_RESOLUTION_WORKAROUND)) {
                keepAnnotated = false;
            } else {
                System.err.println("Invalid option " + args[argIndex]);
                printUsage();
                System.exit(STATUS_ERROR);
            }
            argIndex++;
        }
        if (args.length - argIndex != 2) {
            printUsage();
            System.exit(STATUS_ERROR);
        }

        try {
            MainDexListBuilder builder = new MainDexListBuilder(keepAnnotated, args[argIndex],
                    args[argIndex + 1]);
            Set<String> toKeep = builder.getMainDexList();
            printList(toKeep);
        } catch (IOException e) {
            System.err.println("A fatal error occured: " + e.getMessage());
            System.exit(STATUS_ERROR);
            return;
        }
    }

    public MainDexListBuilder(boolean keepAnnotated, String rootJar, String pathString)
            throws IOException {
        ZipFile jarOfRoots = null;
        Path path = null;
        try {
            try {
                jarOfRoots = new ZipFile(rootJar);
            } catch (IOException e) {
                throw new IOException("\"" + rootJar + "\" can not be read as a zip archive. ("
                        + e.getMessage() + ")", e);
            }
            path = new Path(pathString);

            ClassReferenceListBuilder mainListBuilder = new ClassReferenceListBuilder(path);
            mainListBuilder.addRoots(jarOfRoots);
            for (String className : mainListBuilder.getClassNames()) {
                filesToKeep.add(className + CLASS_EXTENSION);
            }
            if (keepAnnotated) {
                keepAnnotated(path);
            }
        } finally {
            try {
                jarOfRoots.close();
            } catch (IOException e) {
                // ignore
            }
            if (path != null) {
                for (ClassPathElement element : path.elements) {
                    try {
                        element.close();
                    } catch (IOException e) {
                        // keep going, lets do our best.
                    }
                }
            }
        }
    }

    /**
     * Returns a list of classes to keep. This can be passed to dx as a file with --main-dex-list.
     */
    public Set<String> getMainDexList() {
        return filesToKeep;
    }

    private static void printUsage() {
        System.err.print(USAGE_MESSAGE);
    }

    private static void printList(Set<String> fileNames) {
        for (String fileName : fileNames) {
            System.out.println(fileName);
        }
    }

    /**
     * Keep classes annotated with runtime annotations.
     */
    private void keepAnnotated(Path path) throws FileNotFoundException {
        for (ClassPathElement element : path.getElements()) {
            forClazz:
                for (String name : element.list()) {
                    if (name.endsWith(CLASS_EXTENSION)) {
                        DirectClassFile clazz = path.getClass(name);
                        if (hasRuntimeVisibleAnnotation(clazz)) {
                            filesToKeep.add(name);
                        } else {
                            MethodList methods = clazz.getMethods();
                            for (int i = 0; i<methods.size(); i++) {
                                if (hasRuntimeVisibleAnnotation(methods.get(i))) {
                                    filesToKeep.add(name);
                                    continue forClazz;
                                }
                            }
                            FieldList fields = clazz.getFields();
                            for (int i = 0; i<fields.size(); i++) {
                                if (hasRuntimeVisibleAnnotation(fields.get(i))) {
                                    filesToKeep.add(name);
                                    continue forClazz;
                                }
                            }
                        }
                    }
                }
        }
    }

    private boolean hasRuntimeVisibleAnnotation(HasAttribute element) {
        Attribute att = element.getAttributes().findFirst(
                AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME);
        return (att != null && ((AttRuntimeVisibleAnnotations)att).getAnnotations().size()>0);
    }
}

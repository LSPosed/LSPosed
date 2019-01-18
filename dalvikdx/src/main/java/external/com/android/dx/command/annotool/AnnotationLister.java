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

package external.com.android.dx.command.annotool;

import external.com.android.dx.cf.attrib.AttRuntimeInvisibleAnnotations;
import external.com.android.dx.cf.attrib.AttRuntimeVisibleAnnotations;
import external.com.android.dx.cf.attrib.BaseAnnotations;
import external.com.android.dx.cf.direct.ClassPathOpener;
import external.com.android.dx.cf.direct.DirectClassFile;
import external.com.android.dx.cf.direct.StdAttributeFactory;
import external.com.android.dx.cf.iface.Attribute;
import external.com.android.dx.cf.iface.AttributeList;
import external.com.android.dx.rop.annotation.Annotation;
import external.com.android.dx.util.ByteArray;
import java.io.File;
import java.lang.annotation.ElementType;
import java.util.HashSet;

/**
 * Greps annotations on a set of class files and prints matching elements
 * to stdout. What counts as a match and what should be printed is controlled
 * by the {@code Main.Arguments} instance.
 */
class AnnotationLister {
    /**
     * The string name of the pseudo-class that
     * contains package-wide annotations
     */
    private static final String PACKAGE_INFO = "package-info";

    /** current match configuration */
    private final Main.Arguments args;

    /** Set of classes whose inner classes should be considered matched */
    HashSet<String> matchInnerClassesOf = new HashSet<String>();

    /** set of packages whose classes should be considered matched */
    HashSet<String> matchPackages = new HashSet<String>();

    AnnotationLister (Main.Arguments args) {
        this.args = args;
    }

    /** Processes based on configuration specified in constructor. */
    void process() {
        for (String path : args.files) {
            ClassPathOpener opener;

            opener = new ClassPathOpener(path, true,
                    new ClassPathOpener.Consumer() {
                @Override
                public boolean processFileBytes(String name, long lastModified, byte[] bytes) {
                    if (!name.endsWith(".class")) {
                        return true;
                    }

                    ByteArray ba = new ByteArray(bytes);
                    DirectClassFile cf
                        = new DirectClassFile(ba, name, true);

                    cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
                    AttributeList attributes = cf.getAttributes();
                    Attribute att;

                    String cfClassName
                            = cf.getThisClass().getClassType().getClassName();

                    if (cfClassName.endsWith(PACKAGE_INFO)) {
                        att = attributes.findFirst(
                                AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME);

                        for (;att != null; att = attributes.findNext(att)) {
                            BaseAnnotations ann = (BaseAnnotations)att;
                            visitPackageAnnotation(cf, ann);
                        }

                        att = attributes.findFirst(
                                AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME);

                        for (;att != null; att = attributes.findNext(att)) {
                            BaseAnnotations ann = (BaseAnnotations)att;
                            visitPackageAnnotation(cf, ann);
                        }
                    } else if (isMatchingInnerClass(cfClassName)
                            || isMatchingPackage(cfClassName)) {
                        printMatch(cf);
                    } else {
                        att = attributes.findFirst(
                                AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME);

                        for (;att != null; att = attributes.findNext(att)) {
                            BaseAnnotations ann = (BaseAnnotations)att;
                            visitClassAnnotation(cf, ann);
                        }

                        att = attributes.findFirst(
                                AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME);

                        for (;att != null; att = attributes.findNext(att)) {
                            BaseAnnotations ann = (BaseAnnotations)att;
                            visitClassAnnotation(cf, ann);
                        }
                    }

                    return true;
                }

                @Override
                public void onException(Exception ex) {
                    throw new RuntimeException(ex);
                }

                @Override
                public void onProcessArchiveStart(File file) {

                }

            });

            opener.process();
        }
    }

    /**
     * Inspects a class annotation.
     *
     * @param cf {@code non-null;} class file
     * @param ann {@code non-null;} annotation
     */
    private void visitClassAnnotation(DirectClassFile cf,
            BaseAnnotations ann) {

        if (!args.eTypes.contains(ElementType.TYPE)) {
            return;
        }

        for (Annotation anAnn : ann.getAnnotations().getAnnotations()) {
            String annClassName
                    = anAnn.getType().getClassType().getClassName();
            if (args.aclass.equals(annClassName)) {
                printMatch(cf);
            }
        }
    }

    /**
     * Inspects a package annotation
     *
     * @param cf {@code non-null;} class file of "package-info" pseudo-class
     * @param ann {@code non-null;} annotation
     */
    private void visitPackageAnnotation(
            DirectClassFile cf, BaseAnnotations ann) {

        if (!args.eTypes.contains(ElementType.PACKAGE)) {
            return;
        }

        String packageName = cf.getThisClass().getClassType().getClassName();

        int slashIndex = packageName.lastIndexOf('/');

        if (slashIndex == -1) {
            packageName = "";
        } else {
            packageName
                    = packageName.substring(0, slashIndex);
        }


        for (Annotation anAnn : ann.getAnnotations().getAnnotations()) {
            String annClassName
                    = anAnn.getType().getClassType().getClassName();
            if (args.aclass.equals(annClassName)) {
                printMatchPackage(packageName);
            }
        }
    }


    /**
     * Prints, or schedules for printing, elements related to a
     * matching package.
     *
     * @param packageName {@code non-null;} name of package
     */
    private void printMatchPackage(String packageName) {
        for (Main.PrintType pt : args.printTypes) {
            switch (pt) {
                case CLASS:
                case INNERCLASS:
                case METHOD:
                    matchPackages.add(packageName);
                    break;
                case PACKAGE:
                    System.out.println(packageName.replace('/','.'));
                    break;
            }
        }
    }

    /**
     * Prints, or schedules for printing, elements related to a matching
     * class.
     *
     * @param cf {@code non-null;} matching class
     */
    private void printMatch(DirectClassFile cf) {
        for (Main.PrintType pt : args.printTypes) {
            switch (pt) {
                case CLASS:
                    String classname;
                    classname =
                        cf.getThisClass().getClassType().getClassName();
                    classname = classname.replace('/','.');
                    System.out.println(classname);
                    break;
                case INNERCLASS:
                    matchInnerClassesOf.add(
                            cf.getThisClass().getClassType().getClassName());
                    break;
                case METHOD:
                    //TODO
                    break;
                case PACKAGE:
                    break;
            }
        }
    }

    /**
     * Checks to see if a specified class name should be considered a match
     * due to previous matches.
     *
     * @param s {@code non-null;} class name
     * @return true if this class should be considered a match
     */
    private boolean isMatchingInnerClass(String s) {
        int i;

        while (0 < (i = s.lastIndexOf('$'))) {
            s = s.substring(0, i);
            if (matchInnerClassesOf.contains(s)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks to see if a specified package should be considered a match due
     * to previous matches.
     *
     * @param s {@code non-null;} package name
     * @return true if this package should be considered a match
     */
    private boolean isMatchingPackage(String s) {
        int slashIndex = s.lastIndexOf('/');

        String packageName;
        if (slashIndex == -1) {
            packageName = "";
        } else {
            packageName
                    = s.substring(0, slashIndex);
        }

        return matchPackages.contains(packageName);
    }
}

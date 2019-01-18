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

import java.lang.annotation.ElementType;
import java.util.EnumSet;
import java.util.Locale;

public class Main {

    private static class InvalidArgumentException extends Exception {
        InvalidArgumentException() {
            super();
        }

        InvalidArgumentException(String s) {
            super(s);
        }
    }

    enum PrintType {
        CLASS,
        INNERCLASS,
        METHOD,
        PACKAGE
    }


    static class Arguments {
        /**
         * from --annotation, dot-separated classname
         * of annotation to look for
         */
        String aclass;

        /** from --eTypes */
        EnumSet<ElementType> eTypes = EnumSet.noneOf(ElementType.class);

        /** from --print */
        EnumSet<PrintType> printTypes = EnumSet.noneOf(PrintType.class);

        /** remaining positional arguments */
        String[] files;

        Arguments() {
        }

        void parse (String[] argArray) throws InvalidArgumentException {
            for (int i = 0; i < argArray.length; i++) {
                String arg = argArray[i];

                if (arg.startsWith("--annotation=")) {
                    String argParam = arg.substring(arg.indexOf('=') + 1);
                    if (aclass != null) {
                        throw new InvalidArgumentException(
                                "--annotation can only be specified once.");
                    }
                    aclass = argParam.replace('.','/');
                } else if (arg.startsWith("--element=")) {
                    String argParam = arg.substring(arg.indexOf('=') + 1);

                    try {
                        for (String p : argParam.split(",")) {
                            eTypes.add(ElementType.valueOf(p.toUpperCase(Locale.ROOT)));
                        }
                    } catch (IllegalArgumentException ex) {
                        throw new InvalidArgumentException(
                                "invalid --element");
                    }
                } else if (arg.startsWith("--print=")) {
                    String argParam = arg.substring(arg.indexOf('=') + 1);

                    try {
                        for (String p : argParam.split(",")) {
                            printTypes.add(PrintType.valueOf(p.toUpperCase(Locale.ROOT)));
                        }
                    } catch (IllegalArgumentException ex) {
                        throw new InvalidArgumentException("invalid --print");
                    }
                } else {
                    files = new String[argArray.length - i];
                    System.arraycopy(argArray, i, files, 0, files.length);
                    break;
                }
            }

            if (aclass == null) {
                throw new InvalidArgumentException(
                        "--annotation must be specified");
            }

            if (printTypes.isEmpty()) {
                printTypes.add(PrintType.CLASS);
            }

            if (eTypes.isEmpty()) {
                eTypes.add(ElementType.TYPE);
            }

            EnumSet<ElementType> set = eTypes.clone();

            set.remove(ElementType.TYPE);
            set.remove(ElementType.PACKAGE);
            if (!set.isEmpty()) {
                throw new InvalidArgumentException(
                        "only --element parameters 'type' and 'package' "
                                + "supported");
            }
        }
    }

    /**
     * This class is uninstantiable.
     */
    private Main() {
        // This space intentionally left blank.
    }

    public static void main(String[] argArray) {

        final Arguments args = new Arguments();

        try {
            args.parse(argArray);
        } catch (InvalidArgumentException ex) {
            System.err.println(ex.getMessage());

            throw new RuntimeException("usage");
        }

        new AnnotationLister(args).process();
    }
}

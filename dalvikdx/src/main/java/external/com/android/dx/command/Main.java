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

package external.com.android.dx.command;

import external.com.android.dx.Version;

/**
 * Main class for dx. It recognizes enough options to be able to dispatch
 * to the right "actual" main.
 */
public class Main {
    private static final String USAGE_MESSAGE =
        "usage:\n" +
        "  dx --dex [--debug] [--verbose] [--positions=<style>] [--no-locals]\n" +
        "  [--no-optimize] [--statistics] [--[no-]optimize-list=<file>] [--no-strict]\n" +
        "  [--keep-classes] [--output=<file>] [--dump-to=<file>] [--dump-width=<n>]\n" +
        "  [--dump-method=<name>[*]] [--verbose-dump] [--no-files] [--core-library]\n" +
        "  [--num-threads=<n>] [--incremental] [--force-jumbo] [--no-warning]\n" +
        "  [--multi-dex [--main-dex-list=<file> [--minimal-main-dex]]\n" +
        "  [--input-list=<file>] [--min-sdk-version=<n>]\n" +
        "  [--allow-all-interface-method-invokes]\n" +
        "  [<file>.class | <file>.{zip,jar,apk} | <directory>] ...\n" +
        "    Convert a set of classfiles into a dex file, optionally embedded in a\n" +
        "    jar/zip. Output name must end with one of: .dex .jar .zip .apk or be a\n" +
        "    directory.\n" +
        "    Positions options: none, important, lines.\n" +
        "    --multi-dex: allows to generate several dex files if needed. This option is\n" +
        "    exclusive with --incremental, causes --num-threads to be ignored and only\n" +
        "    supports folder or archive output.\n" +
        "    --main-dex-list=<file>: <file> is a list of class file names, classes\n" +
        "    defined by those class files are put in classes.dex.\n" +
        "    --minimal-main-dex: only classes selected by --main-dex-list are to be put\n" +
        "    in the main dex.\n" +
        "    --input-list: <file> is a list of inputs.\n" +
        "    Each line in <file> must end with one of: .class .jar .zip .apk or be a\n" +
        "    directory.\n" +
        "    --min-sdk-version=<n>: Enable dex file features that require at least sdk\n" +
        "    version <n>.\n" +
        "  dx --annotool --annotation=<class> [--element=<element types>]\n" +
        "  [--print=<print types>]\n" +
        "  dx --dump [--debug] [--strict] [--bytes] [--optimize]\n" +
        "  [--basic-blocks | --rop-blocks | --ssa-blocks | --dot] [--ssa-step=<step>]\n" +
        "  [--width=<n>] [<file>.class | <file>.txt] ...\n" +
        "    Dump classfiles, or transformations thereof, in a human-oriented format.\n" +
        "  dx --find-usages <file.dex> <declaring type> <member>\n" +
        "    Find references and declarations to a field or method.\n" +
        "    <declaring type> is a class name in internal form, like Ljava/lang/Object;\n" +
        "    <member> is a field or method name, like hashCode.\n" +
        "  dx -J<option> ... <arguments, in one of the above forms>\n" +
        "    Pass VM-specific options to the virtual machine that runs dx.\n" +
        "  dx --version\n" +
        "    Print the version of this tool (" + Version.VERSION + ").\n" +
        "  dx --help\n" +
        "    Print this message.";

    /**
     * This class is uninstantiable.
     */
    private Main() {
        // This space intentionally left blank.
    }

    /**
     * Run!
     */
    public static void main(String[] args) {
        boolean gotCmd = false;
        boolean showUsage = false;

        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--") || !arg.startsWith("--")) {
                    gotCmd = false;
                    showUsage = true;
                    break;
                }

                gotCmd = true;
                if (arg.equals("--dex")) {
                    external.com.android.dx.command.dexer.Main.main(without(args, i));
                    break;
                } else if (arg.equals("--dump")) {
                    external.com.android.dx.command.dump.Main.main(without(args, i));
                    break;
                } else if (arg.equals("--annotool")) {
                    external.com.android.dx.command.annotool.Main.main(
                            without(args, i));
                    break;
                } else if (arg.equals("--find-usages")) {
                    external.com.android.dx.command.findusages.Main.main(without(args, i));
                    break;
                } else if (arg.equals("--version")) {
                    version();
                    break;
                } else if (arg.equals("--help")) {
                    showUsage = true;
                    break;
                } else {
                    gotCmd = false;
                }
            }
        } catch (UsageException ex) {
            showUsage = true;
        } catch (RuntimeException ex) {
            System.err.println("\nUNEXPECTED TOP-LEVEL EXCEPTION:");
            ex.printStackTrace();
            System.exit(2);
        } catch (Throwable ex) {
            System.err.println("\nUNEXPECTED TOP-LEVEL ERROR:");
            ex.printStackTrace();
            if ((ex instanceof NoClassDefFoundError)
                    || (ex instanceof NoSuchMethodError)) {
                System.err.println(
                        "Note: You may be using an incompatible " +
                        "virtual machine or class library.\n" +
                        "(This program is known to be incompatible " +
                        "with recent releases of GCJ.)");
            }
            System.exit(3);
        }

        if (!gotCmd) {
            System.err.println("error: no command specified");
            showUsage = true;
        }

        if (showUsage) {
            usage();
            System.exit(1);
        }
    }

    /**
     * Prints the version message.
     */
    private static void version() {
        System.err.println("dx version " + Version.VERSION);
        System.exit(0);
    }

    /**
     * Prints the usage message.
     */
    private static void usage() {
        System.err.println(USAGE_MESSAGE);
    }

    /**
     * Returns a copy of the given args array, but without the indicated
     * element.
     *
     * @param orig {@code non-null;} original array
     * @param n which element to omit
     * @return {@code non-null;} new array
     */
    private static String[] without(String[] orig, int n) {
        int len = orig.length - 1;
        String[] newa = new String[len];
        System.arraycopy(orig, 0, newa, 0, n);
        System.arraycopy(orig, n + 1, newa, n, len - n);
        return newa;
    }
}

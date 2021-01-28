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

package external.com.android.dx.command.dump;

import external.com.android.dex.util.FileUtils;
import external.com.android.dx.cf.iface.ParseException;
import external.com.android.dx.util.HexParser;
import java.io.UnsupportedEncodingException;

/**
 * Main class for the class file dumper.
 */
public class Main {

    private final Args parsedArgs = new Args();

    /**
     * This class is uninstantiable.
     */
    private Main() {
        // This space intentionally left blank.
    }

    public static void main(String[] args) {
        new Main().run(args);
    }

    /**
     * Run!
     */
    private void run(String[] args) {
        int at = 0;

        for (/*at*/; at < args.length; at++) {
            String arg = args[at];
            if (arg.equals("--") || !arg.startsWith("--")) {
                break;
            } else if (arg.equals("--bytes")) {
                parsedArgs.rawBytes = true;
            } else if (arg.equals("--basic-blocks")) {
                parsedArgs.basicBlocks = true;
            } else if (arg.equals("--rop-blocks")) {
                parsedArgs.ropBlocks = true;
            } else if (arg.equals("--optimize")) {
                parsedArgs.optimize = true;
            } else if (arg.equals("--ssa-blocks")) {
                parsedArgs.ssaBlocks = true;
            } else if (arg.startsWith("--ssa-step=")) {
                parsedArgs.ssaStep = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.equals("--debug")) {
                parsedArgs.debug = true;
            } else if (arg.equals("--dot")) {
                parsedArgs.dotDump = true;
            } else if (arg.equals("--strict")) {
                parsedArgs.strictParse = true;
            } else if (arg.startsWith("--width=")) {
                arg = arg.substring(arg.indexOf('=') + 1);
                parsedArgs.width = Integer.parseInt(arg);
            } else if (arg.startsWith("--method=")) {
                arg = arg.substring(arg.indexOf('=') + 1);
                parsedArgs.method = arg;
            } else {
                System.err.println("unknown option: " + arg);
                throw new RuntimeException("usage");
            }
        }

        if (at == args.length) {
            System.err.println("no input files specified");
            throw new RuntimeException("usage");
        }

        for (/*at*/; at < args.length; at++) {
            try {
                String name = args[at];
                System.out.println("reading " + name + "...");
                byte[] bytes = FileUtils.readFile(name);
                if (!name.endsWith(".class")) {
                    String src;
                    try {
                        src = new String(bytes, "utf-8");
                    } catch (UnsupportedEncodingException ex) {
                        throw new RuntimeException("shouldn't happen", ex);
                    }
                    bytes = HexParser.parse(src);
                }
                processOne(name, bytes);
            } catch (ParseException ex) {
                System.err.println("\ntrouble parsing:");
                if (parsedArgs.debug) {
                    ex.printStackTrace();
                } else {
                    ex.printContext(System.err);
                }
            }
        }
    }

    /**
     * Processes one file.
     *
     * @param name {@code non-null;} name of the file
     * @param bytes {@code non-null;} contents of the file
     */
    private void processOne(String name, byte[] bytes) {
        if (parsedArgs.dotDump) {
            DotDumper.dump(bytes, name, parsedArgs);
        } else if (parsedArgs.basicBlocks) {
            BlockDumper.dump(bytes, System.out, name, false, parsedArgs);
        } else if (parsedArgs.ropBlocks) {
            BlockDumper.dump(bytes, System.out, name, true, parsedArgs);
        } else if (parsedArgs.ssaBlocks) {
            // --optimize ignored with --ssa-blocks
            parsedArgs.optimize = false;
            SsaDumper.dump(bytes, System.out, name, parsedArgs);
        } else {
            ClassDumper.dump(bytes, System.out, name, parsedArgs);
        }
    }
}

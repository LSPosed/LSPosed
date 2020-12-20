/*
 * Copyright (c) 2009-2013 Panxiaobo
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
package pxb.android.arsc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pxb.android.axml.Util;

/**
 * dump an arsc file
 * 
 * @author bob
 * 
 */
public class ArscDumper {
    public static void dump(List<Pkg> pkgs) {
        for (int x = 0; x < pkgs.size(); x++) {
            Pkg pkg = pkgs.get(x);

            System.out.println(String.format("  Package %d id=%d name=%s typeCount=%d", x, pkg.id, pkg.name,
                    pkg.types.size()));
            for (Type type : pkg.types.values()) {
                System.out.println(String.format("    type %d %s", type.id - 1, type.name));

                int resPrefix = pkg.id << 24 | type.id << 16;
                for (int i = 0; i < type.specs.length; i++) {
                    ResSpec spec = type.getSpec(i);
                    System.out.println(String.format("      spec 0x%08x 0x%08x %s", resPrefix | spec.id, spec.flags,
                            spec.name));
                }
                for (int i = 0; i < type.configs.size(); i++) {
                    Config config = type.configs.get(i);
                    System.out.println("      config");

                    List<ResEntry> entries = new ArrayList<ResEntry>(config.resources.values());
                    for (int j = 0; j < entries.size(); j++) {
                        ResEntry entry = entries.get(j);
                        System.out.println(String.format("        resource 0x%08x %-20s: %s",
                                resPrefix | entry.spec.id, entry.spec.name, entry.value));
                    }
                }
            }
        }
    }

    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.err.println("asrc-dump file.arsc");
            return;
        }
        byte[] data = Util.readFile(new File(args[0]));
        List<Pkg> pkgs = new ArscParser(data).parse();

        dump(pkgs);

    }
}

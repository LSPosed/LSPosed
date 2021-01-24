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

package external.com.android.dx.command.grep;

import external.com.android.dex.Dex;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public final class Main {
    public static void main(String[] args) throws IOException {
        String dexFile = args[0];
        String pattern = args[1];

        Dex dex = new Dex(new File(dexFile));
        int count = new Grep(dex, Pattern.compile(pattern), new PrintWriter(System.out)).grep();
        System.exit((count > 0) ? 0 : 1);
    }
}

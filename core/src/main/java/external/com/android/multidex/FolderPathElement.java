/*
 * Copyright (C) 2013 The Android Open Source Project
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * A folder element.
 */
class FolderPathElement implements ClassPathElement {

    private final File baseFolder;

    public FolderPathElement(File baseFolder) {
        this.baseFolder = baseFolder;
    }

    @Override
    public InputStream open(String path) throws FileNotFoundException {
        return new FileInputStream(new File(baseFolder,
                path.replace(SEPARATOR_CHAR, File.separatorChar)));
    }

    @Override
    public void close() {
    }

    @Override
    public Iterable<String> list() {
        ArrayList<String> result = new ArrayList<String>();
        collect(baseFolder, "", result);
        return result;
    }

    private void collect(File folder, String prefix, ArrayList<String> result) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                collect(file, prefix + SEPARATOR_CHAR + file.getName(), result);
            } else {
                result.add(prefix + SEPARATOR_CHAR + file.getName());
            }
        }
    }

}

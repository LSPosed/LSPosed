/*
 * Copyright (C) 2012 The Android Open Source Project
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

package external.com.android.dx;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses heuristics to guess the application's private data directory.
 */
class AppDataDirGuesser {
    // Copied from UserHandle, indicates range of uids allocated for a user.
    public static final int PER_USER_RANGE = 100000;

    public File guess() {
        try {
            ClassLoader classLoader = guessSuitableClassLoader();
            // Check that we have an instance of the PathClassLoader.
            Class<?> clazz = Class.forName("dalvik.system.PathClassLoader");
            clazz.cast(classLoader);
            // Use the toString() method to calculate the data directory.
            String pathFromThisClassLoader = getPathFromThisClassLoader(classLoader, clazz);
            File[] results = guessPath(pathFromThisClassLoader);
            if (results.length > 0) {
                return results[0];
            }
        } catch (ClassCastException ignored) {
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    private ClassLoader guessSuitableClassLoader() {
        return AppDataDirGuesser.class.getClassLoader();
    }

    private String getPathFromThisClassLoader(ClassLoader classLoader, Class<?> pathClassLoaderClass) {
        // Prior to ICS, we can simply read the "path" field of the
        // PathClassLoader.
        try {
            Field pathField = pathClassLoaderClass.getDeclaredField("path");
            pathField.setAccessible(true);
            return (String) pathField.get(classLoader);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (ClassCastException ignored) {
        }

        // Parsing toString() method: yuck.  But no other way to get the path.
        String result = classLoader.toString();
        return processClassLoaderString(result);
    }

    /**
     * Given the result of a ClassLoader.toString() call, process the result so that guessPath
     * can use it. There are currently two variants. For Android 4.3 and later, the string
     * "DexPathList" should be recognized and the array of dex path elements is parsed. for
     * earlier versions, the last nested array ('[' ... ']') is enclosing the string we are
     * interested in.
     */
    static String processClassLoaderString(String input) {
        if (input.contains("DexPathList")) {
            return processClassLoaderString43OrLater(input);
        } else {
            return processClassLoaderString42OrEarlier(input);
        }
    }

    private static String processClassLoaderString42OrEarlier(String input) {
        /* The toString output looks like this:
         * dalvik.system.PathClassLoader[dexPath=path/to/apk,libraryPath=path/to/libs]
         */
        int index = input.lastIndexOf('[');
        input = (index == -1) ? input : input.substring(index + 1);
        index = input.indexOf(']');
        input = (index == -1) ? input : input.substring(0, index);
        return input;
    }

    private static String processClassLoaderString43OrLater(String input) {
        /* The toString output looks like this:
         * dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/{NAME}", ...], nativeLibraryDirectories=[...]]]
         */
        int start = input.indexOf("DexPathList") + "DexPathList".length();
        if (input.length() > start + 4) {  // [[ + ]]
            String trimmed = input.substring(start);
            int end = trimmed.indexOf(']');
            if (trimmed.charAt(0) == '[' && trimmed.charAt(1) == '[' && end >= 0) {
                trimmed = trimmed.substring(2, end);
                // Comma-separated list, Arrays.toString output.
                String split[] = trimmed.split(",");

                // Clean up parts. Each path element is the type of the element plus the path in
                // quotes.
                for (int i = 0; i < split.length; i++) {
                    int quoteStart = split[i].indexOf('"');
                    int quoteEnd = split[i].lastIndexOf('"');
                    if (quoteStart > 0 && quoteStart < quoteEnd) {
                        split[i] = split[i].substring(quoteStart + 1, quoteEnd);
                    }
                }

                // Need to rejoin components.
                StringBuilder sb = new StringBuilder();
                for (String s : split) {
                    if (sb.length() > 0) {
                        sb.append(':');
                    }
                    sb.append(s);
                }
                return sb.toString();
            }
        }

        // This is technically a parsing failure. Return the original string, maybe a later
        // stage can still salvage this.
        return input;
    }

    File[] guessPath(String input) {
        List<File> results = new ArrayList<>();
        String apkPathRoot = "/data/app/";
        for (String potential : splitPathList(input)) {
            if (!potential.startsWith(apkPathRoot)) {
                continue;
            }
            int end = potential.lastIndexOf(".apk");
            if (end != potential.length() - 4) {
                continue;
            }
            int endSlash = potential.lastIndexOf("/", end);
            if (endSlash == apkPathRoot.length() - 1) {
                // Apks cannot be directly under /data/app
                continue;
            }
            int startSlash = potential.lastIndexOf("/", endSlash - 1);
            if (startSlash == -1) {
                continue;
            }
            // Look for the first dash after the package name
            int dash = potential.indexOf("-", startSlash);
            if (dash == -1) {
                continue;
            }
            end = dash;
            String packageName = potential.substring(startSlash + 1, end);
            File dataDir = getWriteableDirectory("/data/data/" + packageName);

            if (dataDir == null) {
                // If we can't access "/data/data", try to guess user specific data directory.
                dataDir = guessUserDataDirectory(packageName);
            }

            if (dataDir != null) {
                File cacheDir = new File(dataDir, "cache");
                // The cache directory might not exist -- create if necessary
                if (fileOrDirExists(cacheDir) || cacheDir.mkdir()) {
                    if (isWriteableDirectory(cacheDir)) {
                        results.add(cacheDir);
                    }
                }
            }
        }
        return results.toArray(new File[results.size()]);
    }

    static String[] splitPathList(String input) {
       String trimmed = input;
       if (input.startsWith("dexPath=")) {
            int start = "dexPath=".length();
            int end = input.indexOf(',');

           trimmed = (end == -1) ? input.substring(start) : input.substring(start, end);
       }

       return trimmed.split(":");
    }

    boolean fileOrDirExists(File file) {
        return file.exists();
    }

    boolean isWriteableDirectory(File file) {
        return file.isDirectory() && file.canWrite();
    }

    Integer getProcessUid() {
        /* Uses reflection to try to fetch process UID. It will only work when executing on
         * Android device. Otherwise, returns null.
         */
        try {
            Method myUid = Class.forName("android.os.Process").getMethod("myUid");

            // Invoke the method on a null instance, since it's a static method.
            return (Integer) myUid.invoke(/* instance= */ null);
        } catch (Exception e) {
            // Catch any exceptions thrown and default to returning a null.
            return null;
        }
    }

    File guessUserDataDirectory(String packageName) {
        Integer uid = getProcessUid();
        if (uid == null) {
            // If we couldn't retrieve process uid, return null.
            return null;
        }

        // We're trying to get the ID of the Android user that's running the process. It can be
        // inferred from the UID of the current process.
        int userId = uid / PER_USER_RANGE;
        return getWriteableDirectory(String.format("/data/user/%d/%s", userId, packageName));
    }

    private File getWriteableDirectory(String pathName) {
        File dir = new File(pathName);
        return isWriteableDirectory(dir) ? dir : null;
    }
}

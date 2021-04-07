/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.nativebridge;

import android.content.res.Resources;
import android.content.res.XResources;

import dalvik.annotation.optimization.FastNative;

public class ResourcesHook {

    @FastNative
    public static native boolean initXResourcesNative();

    @FastNative
    public static native boolean removeFinalFlagNative(Class<?> clazz);

    @FastNative
    public static native ClassLoader buildDummyClassLoader(ClassLoader parent, Class<?> resourceSuperClass, Class<?> typedArraySuperClass);

    @FastNative
    public static native void rewriteXmlReferencesNative(long parserPtr, XResources origRes, Resources repRes);
}

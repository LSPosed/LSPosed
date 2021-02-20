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

package io.github.lsposed.lspd.nativebridge;

import androidx.annotation.Keep;

import java.lang.reflect.Member;

import de.robv.android.xposed.PendingHooks;

@Keep
public class ClassLinker {

    public static native void setEntryPointsToInterpreter(Member method);

    public static void onPostFixupStaticTrampolines(Class clazz) {
        // native flags will be re-set in hooking logic
        PendingHooks.hookPendingMethod(clazz);
    }
}

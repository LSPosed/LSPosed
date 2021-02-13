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

package io.github.lsposed.lspd.framework;

import io.github.lsposed.lspd.util.Utils;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
public class Zygote {
    public static void allowFileAcrossFork(String path) {
        try {
            Class zygote = XposedHelpers.findClass("com.android.internal.os.Zygote", null);
            XposedHelpers.callStaticMethod(zygote, "nativeAllowFileAcrossFork", path);
        } catch (Throwable throwable) {
            Utils.logE("error when allowFileAcrossFork", throwable);
        }
    }
}

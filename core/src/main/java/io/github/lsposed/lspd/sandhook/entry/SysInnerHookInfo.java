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

package io.github.lsposed.lspd.sandhook.entry;

import io.github.lsposed.common.KeepMembers;
import io.github.lsposed.lspd.sandhook.hooker.StartBootstrapServicesHooker;
import io.github.lsposed.lspd.sandhook.hooker.StartBootstrapServicesHooker11;
import io.github.lsposed.lspd.util.Versions;

public class SysInnerHookInfo implements KeepMembers {

    public static Class<?> getSysInnerHookerClass() {
        return Versions.hasR() ?
                StartBootstrapServicesHooker11.class :
                StartBootstrapServicesHooker.class;
    }

    public static String[] hookItemNames = {
            getSysInnerHookerClass().getName()
    };

    public static Class[] hookItems = {
            getSysInnerHookerClass()
    };
}

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
 * Copyright (C) 2023 LSPosed Contributors
 */

package org.lsposed.lspd;

// Declare any non-default types here with import statements

interface ILSPManagerClientService {
    boolean reloadSingleModule(String packageName, int userId, boolean packageRemovedForAllUsers) = 1;
    boolean reloadInstalledModules() = 2;
    boolean refreshAppList(boolean force) = 3;
}

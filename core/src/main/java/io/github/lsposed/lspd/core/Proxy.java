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

package io.github.lsposed.lspd.core;

import io.github.lsposed.common.KeepAll;

public interface Proxy extends KeepAll {

    boolean init();

    void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                              int[][] rlimits, int mountExternal, String seInfo,
                              String niceName, int[] fdsToClose, int[] fdsToIgnore,
                              boolean startChildZygote, String instructionSet,
                              String appDataDir);

    void forkAndSpecializePost(int pid, String appDataDir, String niceName);

    void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                             long permittedCapabilities, long effectiveCapabilities);

    void forkSystemServerPost(int pid);
}

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

#pragma once
#include <base/object.h>

namespace lspd {

    // @ApiSensitive(Level.HIGH)
    static constexpr const char *kPropKeyCompilerFilter = "dalvik.vm.dex2oat-filter";
    static constexpr const char *kPropKeyCompilerFlags = "dalvik.vm.dex2oat-flags";
    static constexpr const char *kPropKeyUseJitProfiles = "dalvik.vm.usejitprofiles";
    static constexpr const char *kPropKeyPmBgDexopt = "pm.dexopt.bg-dexopt";

    static constexpr const char *kPropValueCompilerFilter = "quicken";
    static constexpr const char *kPropValuePmBgDexopt = "speed";
    static constexpr const char *kPropValueCompilerFlags = "--inline-max-code-units=0";
    static constexpr const char *kPropValueCompilerFlagsWS = " --inline-max-code-units=0";


    void InstallRiruHooks();

}

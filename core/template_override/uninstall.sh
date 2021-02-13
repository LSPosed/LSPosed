#!/sbin/sh

#
# This file is part of LSPosed.
#
# LSPosed is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# LSPosed is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
#
# Copyright (C) 2020 EdXposed Contributors
# Copyright (C) 2021 LSPosed Contributors
#

MODDIR=${0%/*}
VARIANT="YAHFA"
REMOVE=false

[[ "$(echo ${MODDIR} | grep sandhook)" != "" ]] && VARIANT="SandHook"

if [[ "${VARIANT}" == "SandHook" ]]; then
    [[ -f "${MODDIR}/../riru_lsposed/module.prop" ]] || REMOVE=true
else
	  [[ -f "${MODDIR}/../riru_lsposed_sandhook/module.prop" ]] || REMOVE=true
fi

if [[ "${REMOVE}" == true ]]; then
    rm -rf /data/misc/riru/modules/lspd
    if [[ -f "/data/adb/riru/modules/lspd.prop" ]]; then
        OLD_CONFIG=$(cat "/data/adb/riru/modules/lspd.prop")
        rm -rf "/data/adb/riru/modules/${OLD_CONFIG}"
        rm "/data/adb/riru/modules/lspd.prop"
    fi
    if [[ -f "/data/misc/riru/modules/lspd.prop" ]]; then
        OLD_CONFIG=$(cat "/data/misc/riru/modules/lspd.prop")
        rm -rf "/data/misc/riru/modules/${OLD_CONFIG}"
        rm "/data/misc/riru/modules/lspd.prop"
    fi
fi

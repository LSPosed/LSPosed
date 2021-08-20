#!/system/bin/sh

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

MISC_PATH=$(cat /data/adb/lspd/misc_path)
BASE_PATH="/data/misc/$MISC_PATH"

LOG_PATH="/data/adb/lspd/log"

chcon -R u:object_r:system_file:s0 "${MODDIR}"
chcon -R u:object_r:system_file:s0 "/data/adb/lspd"
rm -rf ${LOG_PATH}.old
mv ${LOG_PATH} ${LOG_PATH}.old
mkdir -p ${LOG_PATH}
chcon -R u:object_r:magisk_file:s0 ${LOG_PATH}

if [ ! -z "${MISC_PATH}" ]; then
  chcon -R u:object_r:magisk_file:s0 "${BASE_PATH}"
  chmod 771 "${BASE_PATH}"
fi

rm -f "/data/local/tmp/lspd.dex"
unshare -m sh -c "$MODDIR/lspd &"

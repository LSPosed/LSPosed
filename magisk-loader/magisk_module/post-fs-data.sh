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
# Copyright (C) 2021 LSPosed Contributors
#

MODDIR=${0%/*}
MODNAME=${MODDIR##*/}
MAGISK_PATH=$(magisk --path)

rm -f "/data/local/tmp/daemon.apk"
cd "$MODDIR"

if [ "$(getprop ro.build.version.sdk)" -ge 29 ]; then
  TMP=$($RANDOM | md5sum | head -c 16)
  while [ -d "/dev/$TMP" ]; do
      TMP=$($RANDOM | md5sum | head -c 16)
  done
  mkdir "/dev/$TMP"
  echo "/dev/$TMP" > "/data/adb/lspd/dev_path"
  sed -i "s/placeholder_\/dev\/................/placeholder_\/dev\/$TMP/" "$MODDIR/bin/dex2oat32"
  sed -i "s/placeholder_\/dev\/................/placeholder_\/dev\/$TMP/" "$MODDIR/bin/dex2oat64"
  mount --bind "$MAGISK_PATH/.magisk/modules/$MODNAME/bin/dex2oat32" "/apex/com.android.art/bin/dex2oat32"
  mount --bind "$MAGISK_PATH/.magisk/modules/$MODNAME/bin/dex2oat64" "/apex/com.android.art/bin/dex2oat64"
fi

unshare -m sh -c "$MODDIR/daemon &"

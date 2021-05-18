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

# shellcheck disable=SC2034
SKIPUNZIP=1

VERSION=$(grep_prop version "${TMPDIR}/module.prop")
ui_print "- LSPosed version ${VERSION}"

# Extract verify.sh
ui_print "- Extracting verify.sh"
unzip -o "$ZIPFILE" 'verify.sh' -d "$TMPDIR" >&2
if [ ! -f "$TMPDIR/verify.sh" ]; then
  ui_print "*********************************************************"
  ui_print "! Unable to extract verify.sh!"
  ui_print "! This zip may be corrupted, please try downloading again"
  abort    "*********************************************************"
fi
. "$TMPDIR/verify.sh"

# Base check
extract "$ZIPFILE" 'customize.sh' "$TMPDIR"
extract "$ZIPFILE" 'verify.sh' "$TMPDIR"
extract "$ZIPFILE" 'util_functions.sh' "$TMPDIR"
. "$TMPDIR/util_functions.sh"
check_android_version
check_magisk_version
check_incompatible_module

# Extract riru.sh
extract "$ZIPFILE" 'riru.sh' "$TMPDIR"
. "$TMPDIR/riru.sh"

# Functions from riru.sh
check_riru_version
enforce_install_from_magisk_app

# Check architecture
if [ "$ARCH" != "arm" ] && [ "$ARCH" != "arm64" ] && [ "$ARCH" != "x86" ] && [ "$ARCH" != "x64" ]; then
  abort "! Unsupported platform: $ARCH"
else
  ui_print "- Device platform: $ARCH"
fi

# Extract libs
ui_print "- Extracting module files"

extract "$ZIPFILE" 'module.prop'        "$MODPATH"
extract "$ZIPFILE" 'system.prop'        "$MODPATH"
extract "$ZIPFILE" 'sepolicy.rule'      "$MODPATH"
extract "$ZIPFILE" 'post-fs-data.sh'    "$MODPATH"
extract "$ZIPFILE" 'service.sh'         "$MODPATH"
extract "$ZIPFILE" 'uninstall.sh'       "$MODPATH"
extract "$ZIPFILE" 'framework/lspd.dex' "$MODPATH"
extract "$ZIPFILE" 'manager.apk'        '/data/adb/lspd'

mkdir "$MODPATH/riru"
mkdir "$MODPATH/riru/lib"
mkdir "$MODPATH/riru/lib64"

if [ "$ARCH" = "arm" ] || [ "$ARCH" = "arm64" ]; then
  ui_print "- Extracting arm libraries"
  extract "$ZIPFILE" "lib/armeabi-v7a/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/riru/lib" true

  if [ "$IS64BIT" = true ]; then
    ui_print "- Extracting arm64 libraries"
    extract "$ZIPFILE" "lib/arm64-v8a/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/riru/lib64" true
  fi
fi

if [ "$ARCH" = "x86" ] || [ "$ARCH" = "x64" ]; then
  ui_print "- Extracting x86 libraries"
  extract "$ZIPFILE" "lib/x86/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/riru/lib" true

  if [ "$IS64BIT" = true ]; then
    ui_print "- Extracting x64 libraries"
    extract "$ZIPFILE" "lib/x86_64/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/riru/lib64" true
  fi
fi

if [ "$RIRU_MODULE_DEBUG" = true ]; then
  mv "$MODPATH/riru" "$MODPATH/system"
  mv "$MODPATH/system/lib/liblspd.so" "$MODPATH/system/lib/libriru_lspd.so"
  mv "$MODPATH/system/lib64/liblspd.so" "$MODPATH/system/lib64/libriru_lspd.so"
  cp -r "$MODPATH/framework" "$MODPATH/system/framework"
  mkdir -p "/data/adb/riru/modules/lspd"
fi

set_perm_recursive "$MODPATH" 0 0 0755 0644

# Lsposed config
ui_print "- Creating configuration directories"
if [ -f /data/adb/lspd/misc_path ]; then
  # read current MISC_PATH
  MISC_PATH=$(cat /data/adb/lspd/misc_path)
  ui_print "  - Use previous path $MISC_PATH"
else
  # generate random MISC_PATH
  MISC_RAND=$(tr -cd 'A-Za-z0-9' </dev/urandom | head -c16)
  MISC_PATH="lspd_${MISC_RAND}"
  ui_print "  - Use new path ${MISC_RAND}"
  mkdir -p /data/adb/lspd || abort "! Can't create configuration path"
  echo "$MISC_PATH" >/data/adb/lspd/misc_path || abort "! Can't store configuration path"
fi

mkdir -p "/data/misc/$MISC_PATH"
set_perm "/data/misc/$MISC_PATH" 0 0 0771 "u:object_r:magisk_file:s0" || abort "! Can't set permission"
echo "rm -rf /data/misc/$MISC_PATH" >>"${MODPATH}/uninstall.sh" || abort "! Can't write uninstall script"

[ -d /data/adb/lspd/config ] || mkdir -p /data/adb/lspd/config
[ -f /data/adb/lspd/config/verbose_log ] || echo "0" >/data/adb/lspd/config/verbose_log

ui_print "- Welcome to LSPosed!"

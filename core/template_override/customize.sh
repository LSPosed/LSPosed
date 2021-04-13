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

SKIPUNZIP=1

abortC() {
  rm -rf "${MODPATH}"
  if [ ! -f /data/adb/lspd/misc_path ]; then
    [ -d "${MISC_PATH}" ] && rm -rf "${MISC_PATH}"
  fi
  abort "$1"
}

POUNDS="*********************************************************"

VERSION=$(grep_prop version "${TMPDIR}/module.prop")
VERSION_CODE=$(grep_prop versionCode "${TMPDIR}/module.prop")

ui_print "- LSPosed version ${VERSION} (${VERSION_CODE})"

# Extract verify.sh
ui_print "- Extracting verify.sh"
unzip -o "$ZIPFILE" 'verify.sh' -d "$TMPDIR" >&2
if [ ! -f "$TMPDIR/verify.sh" ]; then
  ui_print "${POUNDS}"
  ui_print "! Unable to extract verify.sh!"
  ui_print "! This zip may be corrupted, please try downloading again"
  abortC "${POUNDS}"
fi
. $TMPDIR/verify.sh

extract "$ZIPFILE" 'customize.sh' "${TMPDIR}"
extract "$ZIPFILE" 'util_functions.sh' "${TMPDIR}"
. ${TMPDIR}/util_functions.sh

check_android_version
check_magisk_version

extract "$ZIPFILE" 'riru.sh' "$TMPDIR"
. $TMPDIR/riru.sh

# Functions from riru.sh
check_riru_version
enforce_install_from_magisk_app

# Check architecture
if [ "$ARCH" != "arm" ] && [ "$ARCH" != "arm64" ] && [ "$ARCH" != "x86" ] && [ "$ARCH" != "x64" ]; then
  abort "! Unsupported platform: $ARCH"
else
  ui_print "- Device platform: $ARCH"
fi

ui_print "- Extracting module files"

# extract module files
extract "${ZIPFILE}" 'module.prop' "${MODPATH}"
extract "${ZIPFILE}" 'system.prop' "${MODPATH}"
extract "${ZIPFILE}" 'sepolicy.rule' "${MODPATH}"
extract "${ZIPFILE}" 'post-fs-data.sh' "${MODPATH}"
extract "${ZIPFILE}" 'uninstall.sh' "${MODPATH}"
extract "${ZIPFILE}" 'framework/lspd.dex' "${MODPATH}"

if [ "$ARCH" = "x86" ] || [ "$ARCH" = "x64" ]; then
  ui_print "- Extracting x86 libraries"
  extract "$ZIPFILE" 'riru_x86/lib/liblspd.so' "${MODPATH}"

  if [ "$IS64BIT" = true ]; then
    ui_print "- Extracting x86_64 libraries"
    extract "$ZIPFILE" 'riru_x86/lib64/liblspd.so' "${MODPATH}"
  fi
  mv "${MODPATH}/riru_x86" "${MODPATH}/riru"
else
  ui_print "- Extracting arm libraries"
  extract "$ZIPFILE" 'riru/lib/liblspd.so' "${MODPATH}"

  if [ "$IS64BIT" = true ]; then
    ui_print "- Extracting arm64 libraries"
    extract "$ZIPFILE" 'riru/lib64/liblspd.so' "${MODPATH}"
  fi
fi

ui_print "- Creating configuration directories"
if [ -f /data/adb/lspd/misc_path ]; then
  # read current MISC_PATH
  MISC_PATH=$(cat /data/adb/lspd/misc_path)
  ui_print "  - Use previous path $MISC_PATH"
elif [ -f /data/adb/edxp/misc_path ]; then
  mkdir -p /data/adb/lspd || abortC "! Can't create configuration path"
  MISC_PATH=$(cat /data/adb/edxp/misc_path | sed "s/edxp/lspd/")
  echo $MISC_PATH >/data/adb/lspd/misc_path
  ui_print "  - Use previous path $MISC_PATH"
  cp -r /data/misc/$(cat /data/adb/edxp/misc_path) /data/misc/$MISC_PATH
    ui_print "  - WARNING: This installation will disable EdXposed because of incompatibility"
  touch $(magisk --path)/.magisk/modules/riru_edxposed/disable
  touch $(magisk --path)/.magisk/modules/riru_edxposed_sandhook/disable
else
  # generate random MISC_PATH
  MISC_RAND=$(tr -cd 'A-Za-z0-9' </dev/urandom | head -c16)
  MISC_PATH="lspd_${MISC_RAND}"
  ui_print "  - Use new path ${MISC_RAND}"
  mkdir -p /data/adb/lspd || abortC "! Can't create configuration path"
  echo "$MISC_PATH" >/data/adb/lspd/misc_path || abortC "! Can't store configuration path"
fi

extract "${ZIPFILE}" 'manager.apk' "/data/adb/lspd/"
mkdir -p /data/misc/$MISC_PATH
set_perm /data/misc/$MISC_PATH 0 0 0771 "u:object_r:magisk_file:s0" || abortC "! Can't set permission"

if [ ! -d /data/adb/lspd/config ]; then
  mkdir -p /data/adb/lspd/config
  ui_print "- Migrating configuration"
  cp -r /data/misc/$MISC_PATH/0/prefs /data/misc/$MISC_PATH/prefs
  /system/bin/app_process -Djava.class.path=/data/adb/lspd/framework/lspd.dex /system/bin --nice-name=lspd_config org.lsposed.lspd.service.ConfigManager
fi

echo "rm -rf /data/misc/$MISC_PATH" >>"${MODPATH}/uninstall.sh" || abortC "! Can't write uninstall script"
echo "rm -rf /data/adb/lspd" >>"${MODPATH}/uninstall.sh" || abortC "! Can't write uninstall script"

if [ ! -e /data/adb/lspd/config/verbose_log ]; then
  echo "0" >/data/adb/lspd/config/verbose_log
fi

if [ "$RIRU_MODULE_DEBUG" = true ]; then
  mv ${MODPATH}/riru ${MODPATH}/system
  mv ${MODPATH}/system/lib/liblspd.so ${MODPATH}/system/lib/libriru_lspd.so
  mv ${MODPATH}/system/lib64/liblspd.so ${MODPATH}/system/lib64/libriru_lspd.so
  cp -r ${MODPATH}/framework ${MODPATH}/system/framework
  mkdir -p /data/adb/riru/modules/lspd
fi

set_perm_recursive "${MODPATH}" 0 0 0755 0644
ui_print "- Welcome to LSPosed!"

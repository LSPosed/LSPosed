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

FLAVOR=@FLAVOR@

enforce_install_from_magisk_app() {
  if $BOOTMODE; then
    ui_print "- Installing from Magisk app"
  else
    ui_print "*********************************************************"
    ui_print "! Install from recovery is NOT supported"
    ui_print "! Some recovery has broken implementations, install with such recovery will finally cause Riru or Riru modules not working"
    ui_print "! Please install from Magisk app"
    abort "*********************************************************"
  fi
}

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

if [ "$FLAVOR" == "riru" ]; then
  # Extract riru.sh
  extract "$ZIPFILE" 'riru.sh' "$TMPDIR"
  . "$TMPDIR/riru.sh"
  # Functions from riru.sh
  check_riru_version
fi

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
extract "$ZIPFILE" 'post-fs-data.sh'    "$MODPATH"
extract "$ZIPFILE" 'service.sh'         "$MODPATH"
extract "$ZIPFILE" 'uninstall.sh'       "$MODPATH"
extract "$ZIPFILE" 'framework/lspd.dex' "$MODPATH"
extract "$ZIPFILE" 'daemon.apk'         "$MODPATH"
extract "$ZIPFILE" 'lspd'               "$MODPATH"
rm -f /data/adb/lspd/manager.apk
extract "$ZIPFILE" 'manager.apk'        '/data/adb/lspd'
extract "$ZIPFILE" 'lsposed'            '/data/adb/lspd/bin'

ui_print "- Extracting daemon libraries"
if [ "$ARCH" = "arm" ] ; then
  extract "$ZIPFILE" 'lib/armeabi-v7a/libdaemon.so' "$MODPATH" true
elif [ "$ARCH" = "arm64" ]; then
  extract "$ZIPFILE" 'lib/arm64-v8a/libdaemon.so' "$MODPATH" true
elif [ "$ARCH" = "x86" ]; then
  extract "$ZIPFILE" 'lib/x86/libdaemon.so' "$MODPATH" true
elif [ "$ARCH" = "x64" ]; then
  extract "$ZIPFILE" 'lib/x86_64/libdaemon.so' "$MODPATH" true
fi
if [ "$FLAVOR" == "zygisk" ]; then
  mkdir -p "$MODPATH/zygisk"
  if [ "$ARCH" = "arm" ] || [ "$ARCH" = "arm64" ]; then
    extract "$ZIPFILE" "lib/armeabi-v7a/liblspd.so" "$MODPATH/zygisk" true
    mv "$MODPATH/zygisk/liblspd.so" "$MODPATH/zygisk/armeabi-v7a.so"

    if [ "$IS64BIT" = true ]; then
      extract "$ZIPFILE" "lib/arm64-v8a/liblspd.so" "$MODPATH/zygisk" true
      mv "$MODPATH/zygisk/liblspd.so" "$MODPATH/zygisk/arm64-v8a.so"
    fi
  fi

  if [ "$ARCH" = "x86" ] || [ "$ARCH" = "x64" ]; then
    extract "$ZIPFILE" "lib/x86_64/liblspd.so" "$MODPATH/zygisk" true
    mv "$MODPATH/zygisk/liblspd.so" "$MODPATH/zygisk/x86_64.so"

    if [ "$IS64BIT" = true ]; then
      extract "$ZIPFILE" "lib/x86/liblspd.so" "$MODPATH/zygisk" true
      mv "$MODPATH/zygisk/liblspd.so" "$MODPATH/zygisk/x86.so"
    fi
  fi
elif [ "$FLAVOR" == "riru" ]; then
  extract "$ZIPFILE" 'sepolicy.rule'      "$MODPATH"
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
    mv "$MODPATH/system/lib/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/system/lib/libriru_$RIRU_MODULE_LIB_NAME.so"
    mv "$MODPATH/system/lib64/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/system/lib64/libriru_$RIRU_MODULE_LIB_NAME.so"
    mv "$MODPATH/framework" "$MODPATH/system/framework"
    if [ "$RIRU_API" -ge 26 ]; then
      mkdir -p "$MODPATH/riru/lib"
      mkdir -p "$MODPATH/riru/lib64"
      touch "$MODPATH/riru/lib/libriru_$RIRU_MODULE_LIB_NAME"
      touch "$MODPATH/riru/lib64/libriru_$RIRU_MODULE_LIB_NAME"
    else
      mkdir -p "/data/adb/riru/modules/$RIRU_MODULE_LIB_NAME"
    fi
  fi
fi

set_perm_recursive "$MODPATH" 0 0 0755 0644
chmod 0744 "$MODPATH/lspd"
chmod 0700 "/data/adb/lspd/bin/lsposed"

ui_print "- Welcome to LSPosed!"

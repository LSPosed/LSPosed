#!/sbin/sh
RIRU_MODULE_LIB_NAME="@RIRU_MODULE_LIB_NAME@"

# Variables for customize.sh
RIRU_API=0
RIRU_MIN_COMPATIBLE_API=0
RIRU_VERSION_CODE=0
RIRU_VERSION_NAME=""

# Used by /data/adb/riru/util_functions.sh
RIRU_MODULE_API_VERSION=@RIRU_MODULE_API_VERSION@
RIRU_MODULE_MIN_API_VERSION=@RIRU_MODULE_MIN_API_VERSION@
RIRU_MODULE_MIN_RIRU_VERSION_NAME="@RIRU_MODULE_MIN_RIRU_VERSION_NAME@"
RIRU_MODULE_DEBUG=@RIRU_MODULE_DEBUG@

if [ "$MAGISK_VER_CODE" -ge 21000 ]; then
  MAGISK_CURRENT_RIRU_MODULE_PATH=$(magisk --path)/.magisk/modules/riru-core
else
  MAGISK_CURRENT_RIRU_MODULE_PATH=/sbin/.magisk/modules/riru-core
fi

# This function will be used when util_functions.sh not exists
check_riru_version() {
  if [ ! -f "$MAGISK_CURRENT_RIRU_MODULE_PATH/api_version" ] && [ ! -f "/data/adb/riru/api_version" ] && [ ! -f "/data/adb/riru/api_version.new" ]; then
    ui_print "*********************************************************"
    ui_print "! Riru $RIRU_MODULE_MIN_RIRU_VERSION_NAME or above is required"
    ui_print "! Please install Riru from Magisk Manager or https://github.com/RikkaApps/Riru/releases"
    abort "*********************************************************"
  fi
  RIRU_API=$(cat "$MAGISK_CURRENT_RIRU_MODULE_PATH/api_version") || RIRU_API=$(cat "/data/adb/riru/api_version.new") || RIRU_API=$(cat "/data/adb/riru/api_version") || RIRU_API=0
  [ "$RIRU_API" -eq "$RIRU_API" ] || RIRU_API=0
  ui_print "- Riru API version: $RIRU_API"
  if [ "$RIRU_API" -lt $RIRU_MODULE_MIN_API_VERSION ]; then
    ui_print "*********************************************************"
    ui_print "! Riru $RIRU_MODULE_MIN_RIRU_VERSION_NAME or above is required"
    ui_print "! Please upgrade Riru from Magisk Manager or https://github.com/RikkaApps/Riru/releases"
    abort "*********************************************************"
  fi
}

# This function will be used when util_functions.sh not exists
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

if [ ! -d $MAGISK_CURRENT_RIRU_MODULE_PATH ]; then
  ui_print "*********************************************************"
  ui_print "! Riru is not installed"
  ui_print "! Please install Riru from Magisk Manager or https://github.com/RikkaApps/Riru/releases"
  abort "*********************************************************"
fi

if [ -f "$MAGISK_CURRENT_RIRU_MODULE_PATH/disable" ] || [ -f "$MAGISK_CURRENT_RIRU_MODULE_PATH/remove" ]; then
  ui_print "*********************************************************"
  ui_print "! Riru is not enabled or will be removed"
  ui_print "! Please enable Riru in Magisk first"
  abort "*********************************************************"
fi

if [ -f $MAGISK_CURRENT_RIRU_MODULE_PATH/util_functions.sh ]; then
  ui_print "- Load $MAGISK_CURRENT_RIRU_MODULE_PATH/util_functions.sh"
  # shellcheck disable=SC1090
  . $MAGISK_CURRENT_RIRU_MODULE_PATH/util_functions.sh
else
  if [ "$RIRU_MODULE_MIN_API_VERSION" -ge 11 ]; then
    ui_print "*********************************************************"
    ui_print "! Riru $RIRU_MODULE_MIN_RIRU_VERSION_NAME or above is required"
    ui_print "! Please upgrade Riru from Magisk Manager or https://github.com/RikkaApps/Riru/releases"
    abort "*********************************************************"
  fi

  if [ -f /data/adb/riru/util_functions.sh ]; then
    ui_print "- Load /data/adb/riru/util_functions.sh"
    . /data/adb/riru/util_functions.sh
  else
    ui_print "- Can't find /data/adb/riru/util_functions.sh"
  fi
fi

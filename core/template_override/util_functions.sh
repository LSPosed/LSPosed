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

RIRU_MODULE_ID="%%%RIRU_MODULE_ID%%%"
RIRU_MODULE_API_VERSION=%%%RIRU_MODULE_API_VERSION%%%
RIRU_MODULE_MIN_API_VERSION=%%%RIRU_MODULE_MIN_API_VERSION%%%
RIRU_MODULE_MIN_RIRU_VERSION_NAME="%%%RIRU_MODULE_MIN_RIRU_VERSION_NAME%%%"

if [ "$MAGISK_VER_CODE" -ge 21000 ]; then
  MAGISK_CURRENT_RIRU_MODULE_PATH=$(magisk --path)/.magisk/modules/riru-core
else
  MAGISK_CURRENT_RIRU_MODULE_PATH=/sbin/.magisk/modules/riru-core
fi

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

check_magisk_version() {
  ui_print "- Magisk ${LANG_CUST_INST_VERSION}: ${MAGISK_VER_CODE}"
}

require_new_android() {
    ui_print "${POUNDS}"
    ui_print "! ${LANG_UTIL_ERR_ANDROID_UNSUPPORT_1} ${1} ${LANG_UTIL_ERR_ANDROID_UNSUPPORT_2}"
    ui_print "! ${LANG_UTIL_ERR_ANDROID_UNSUPPORT_3}"
    [ ${BOOTMODE} == true ] && am start -a android.intent.action.VIEW -d https://github.com/LSPosed/LSPosed/wiki/Available-Android-versions
    abortC   "${POUNDS}"
}

lspd_check_architecture() {
    if [ "${ARCH}" != "arm" && "${ARCH}" != "arm64" && "${ARCH}" != "x86" && "${ARCH}" != "x64" ]; then
        abortC "! ${LANG_UTIL_ERR_PLATFORM_UNSUPPORT}: ${ARCH}"
    else
        ui_print "- ${LANG_UTIL_PLATFORM}: ${ARCH}"
    fi
}

check_android_version() {
    if [ ${API} -ge 27 ]; then
        ui_print "- Android SDK ${LANG_CUST_INST_VERSION}: ${API}"
    else
        require_new_android "${API}"
    fi
}

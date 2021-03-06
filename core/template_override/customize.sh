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
  if [[ ! -f /data/adb/lspd/misc_path ]]; then
    [[ -d "${MISC_PATH}" ]] && rm -rf "${MISC_PATH}"
  fi
  abort "$1"
}

getRandomNameExist() {
    RAND_PATH=$4
    RAND_SUFFIX=$3
    RAND_PREFIX=$2
    RAND_DIGIT=$1
    RAND_RAND="$(cat /proc/sys/kernel/random/uuid|md5sum|cut -c 1-"${RAND_DIGIT}")"
    RAND_PATH_EXIST=false
    for TARGET in ${RAND_PATH}; do
        if [[ -e "${TARGET}/${RAND_PREFIX}${RAND_RAND}${RAND_SUFFIX}" ]]; then
            RAND_PATH_EXIST=true
        fi
    done
    if [[ "${RAND_PATH_EXIST}" == true ]]; then
        getRandomNameExist "${RAND_DIGIT}" "${RAND_PREFIX}" "${RAND_SUFFIX}" "${RAND_PATH}"
    else
        echo "${RAND_RAND}"
    fi
}

POUNDS="*********************************************************"
RIRU_PATH="/data/adb/riru"
RIRU_EDXP="$(getRandomNameExist 4 "libriru_" ".so" "
/system/lib
/system/lib64
")"
RIRU_MODULES="${RIRU_PATH}/modules"
RIRU_TARGET="${RIRU_MODULES}/${RIRU_EDXP}"


VERSION=$(grep_prop version "${TMPDIR}/module.prop")
RIRU_MIN_API_VERSION=$(grep_prop api "${TMPDIR}/module.prop")

LIB_RIRU_EDXP="libriru_${RIRU_EDXP}.so"

### lang start ###
# Default en_US
# customize
LANG_CUST_INST_VERSION="version"
LANG_CUST_INST_EXT_FILES="Extracting module files"
LANG_CUST_INST_EXT_LIB_X86="Extracting x86 libraries"
LANG_CUST_INST_EXT_LIB_X64="Extracting x86_64 libraries"
LANG_CUST_INST_EXT_LIB_ARM="Extracting arm libraries"
LANG_CUST_INST_EXT_LIB_ARM64="Extracting arm64 libraries"
LANG_CUST_INST_CONF_CREATE="Creating configuration directories"
LANG_CUST_INST_CONF_OLD="Use previous path"
LANG_CUST_DISABLE_EDXP="**WARNING**: This installation will disable edxposed because of incompatibility"
LANG_CUST_INST_CONF_NEW="Use new path"
LANG_CUST_INST_COPY_LIB="Copying framework libraries"
LANG_CUST_INST_REM_OLDCONF="Removing old configuration"
LANG_CUST_INST_COPT_EXTRA="Copying extra files"
LANG_CUST_INST_DONE="Welcome to"

LANG_CUST_ERR_VERIFY_FAIL="Unable to extract verify tool!"
LANG_CUST_ERR_PERM="Can't set permission"
LANG_CUST_ERR_CONF_CREATE="Can't create configuration path"
LANG_CUST_ERR_CONF_STORE="Can't store configuration path"
LANG_CUST_ERR_CONF_FIRST="Can't create first install flag"
LANG_CUST_ERR_CONF_UNINST="Can't write uninstall script"
LANG_CUST_ERR_EXTRA_CREATE="Can't create"

# verify
LANG_VERIFY_SUCCESS="Verified"

LANG_VERIFY_ERR_MISMATCH="Failed to verify"
LANG_VERIFY_ERR_NOT_EXIST="not exists"
LANG_VERIFY_ERR_NOTICE="This zip may be corrupted, please try downloading again"

# util_functions
LANG_UTIL_PLATFORM="Device platform"

LANG_UTIL_ERR_RIRU_NOT_FOUND_1="is not installed"
LANG_UTIL_ERR_RIRU_NOT_FOUND_2="Please install Riru from Magisk Manager"
LANG_UTIL_ERR_RIRU_LOW_1="or above is required"
LANG_UTIL_ERR_RIRU_LOW_2="Please upgrade Riru from Magisk Manager"
LANG_UTIL_ERR_ANDROID_UNSUPPORT_1="Unsupported Android version"
LANG_UTIL_ERR_ANDROID_UNSUPPORT_2="(below Oreo)"
LANG_UTIL_ERR_ANDROID_UNSUPPORT_3="Learn more from our GitHub Wiki"
LANG_UTIL_ERR_PLATFORM_UNSUPPORT="Unsupported platform"
LANG_CUST_INST_MIGRATE_CONF="Migrating configuration"

# Load lang
if [[ ${BOOTMODE} == true ]]; then
  locale=$(getprop persist.sys.locale|awk -F "-" '{print $1"_"$NF}')
  [[ ${locale} == "" ]] && locale=$(settings get system system_locales|awk -F "," '{print $1}'|awk -F "-" '{print $1"_"$NF}')
  file=${locale}.sh
  unzip -o "$ZIPFILE" "${file}" -d "$TMPDIR" >&2
  unzip -o "$ZIPFILE" "${file}.sha256" -d "$TMPDIR" >&2
  (echo "$(cat "${TMPDIR}/${file}.sha256")  ${TMPDIR}/${file}" | sha256sum -c -s -) && . "${TMPDIR}/${file}"
fi
### lang end ###

ui_print "- LSPosed ${LANG_CUST_INST_VERSION} ${VERSION}"

# extract verify.sh
unzip -o "$ZIPFILE" 'verify.sh' -d "$TMPDIR" >&2
if [ ! -f "$TMPDIR/verify.sh" ]; then
  ui_print  "${POUNDS}"
  ui_print  "! ${LANG_CUST_ERR_VERIFY_FAIL}"
  ui_print  "! ${LANG_VERIFY_ERR_NOTICE}"
  abortC    "${POUNDS}"
fi
. $TMPDIR/verify.sh

extract "$ZIPFILE" 'customize.sh' "${TMPDIR}"
extract "$ZIPFILE" 'util_functions.sh' "${TMPDIR}"
. ${TMPDIR}/util_functions.sh

check_android_version
check_riru_version
lspd_check_architecture

ui_print "- ${LANG_CUST_INST_EXT_FILES}"

# extract module files
extract "${ZIPFILE}" 'module.prop' "${MODPATH}"
extract "${ZIPFILE}" 'system.prop' "${MODPATH}"
extract "${ZIPFILE}" 'sepolicy.rule' "${MODPATH}"
extract "${ZIPFILE}" 'post-fs-data.sh' "${MODPATH}"
extract "${ZIPFILE}" 'service.sh' "${MODPATH}"
extract "${ZIPFILE}" 'uninstall.sh' "${MODPATH}"

extract "${ZIPFILE}" 'system/framework/lspd.dex' "${MODPATH}"

if [ "$ARCH" = "x86" ] || [ "$ARCH" = "x64" ]; then
  ui_print "- ${LANG_CUST_INST_EXT_LIB_X86}"
  extract "$ZIPFILE" 'system_x86/lib/libriru_lspd.so' "${MODPATH}"
  mv "${MODPATH}/system_x86/lib" "${MODPATH}/system/lib"

  if [ "$IS64BIT" = true ]; then
    ui_print "- ${LANG_CUST_INST_EXT_LIB_X64}"
    extract "$ZIPFILE" 'system_x86/lib64/libriru_lspd.so' "${MODPATH}"
    mv "${MODPATH}/system_x86/lib64" "${MODPATH}/system/lib64"
  fi
else
  ui_print "- ${LANG_CUST_INST_EXT_LIB_ARM}"
  extract "$ZIPFILE" 'system/lib/libriru_lspd.so' "${MODPATH}"

  if [ "$IS64BIT" = true ]; then
    ui_print "- ${LANG_CUST_INST_EXT_LIB_ARM64}"
    extract "$ZIPFILE" 'system/lib64/libriru_lspd.so' "${MODPATH}"
  fi
fi

ui_print "- ${LANG_CUST_INST_CONF_CREATE}"
if [[ -f /data/adb/lspd/misc_path ]]; then
  # read current MISC_PATH
  MISC_PATH=$(cat /data/adb/lspd/misc_path)
  ui_print "  - ${LANG_CUST_INST_CONF_OLD} $MISC_PATH"
elif [[ -f /data/adb/edxp/misc_path ]]; then
  mkdir -p /data/adb/lspd || abortC "! ${LANG_CUST_ERR_CONF_CREATE}"
  MISC_PATH=$(cat /data/adb/edxp/misc_path | sed "s/edxp/lspd/")
  echo $MISC_PATH > /data/adb/lspd/misc_path
  ui_print "  - ${LANG_CUST_INST_CONF_OLD} $MISC_PATH"
  cp -r /data/misc/$(cat /data/adb/edxp/misc_path) /data/misc/$MISC_PATH
  ui_print "  - ${LANG_CUST_DISABLE_EDXP}"
  touch $(magisk --path)/.magisk/modules/riru_edxposed/disable
  touch $(magisk --path)/.magisk/modules/riru_edxposed_sandhook/disable
else
  # generate random MISC_PATH
  MISC_RAND=$(tr -cd 'A-Za-z0-9' < /dev/urandom | head -c16)
  MISC_PATH="lspd_${MISC_RAND}"
  ui_print "  - ${LANG_CUST_INST_CONF_NEW} ${MISC_RAND}"
  mkdir -p /data/adb/lspd || abortC "! ${LANG_CUST_ERR_CONF_CREATE}"
  echo "$MISC_PATH" > /data/adb/lspd/misc_path || abortC "! ${LANG_CUST_ERR_CONF_STORE}"
fi
touch /data/adb/lspd/new_install || abortC "! ${LANG_CUST_ERR_CONF_FIRST}"
ui_print "- ${LANG_CUST_INST_COPY_LIB}"
rm -rf "/data/adb/lspd/framework"
mv "${MODPATH}/system/framework" "/data/adb/lspd/framework"
extract "${ZIPFILE}" 'manager.apk' "/data/adb/lspd/"
mkdir -p /data/misc/$MISC_PATH
set_perm /data/misc/$MISC_PATH 0 0 0771 "u:object_r:magisk_file:s0" || abortC "! ${LANG_CUST_ERR_PERM}"

if [[ ! -d /data/adb/lspd/config ]]; then
  mkdir -p /data/adb/lspd/config
  ui_print "- ${LANG_CUST_INST_MIGRATE_CONF}"
  cp -r /data/misc/$MISC_PATH/0/prefs /data/misc/$MISC_PATH/prefs
  /system/bin/app_process -Djava.class.path=/data/adb/lspd/framework/lspd.dex /system/bin --nice-name=lspd_config io.github.lsposed.lspd.service.ConfigManager
fi
echo "rm -rf /data/misc/$MISC_PATH" >> "${MODPATH}/uninstall.sh" || abortC "! ${LANG_CUST_ERR_CONF_UNINST}"
echo "[[ -f /data/adb/lspd/new_install ]] || rm -rf /data/adb/lspd" >> "${MODPATH}/uninstall.sh" || abortC "! ${LANG_CUST_ERR_CONF_UNINST}"

echo "1" > /data/adb/lspd/config/variant

if [[ ! -e /data/adb/lspd/config/verbose_log ]]; then
    echo "0" > /data/adb/lspd/config/verbose_log
fi


mkdir -p /data/misc/$MISC_PATH/cache
rm /data/misc/$MISC_PATH/cache/*
set_perm /data/misc/$MISC_PATH/cache 0 0 0777 "u:object_r:magisk_file:s0" || abortC "! ${LANG_CUST_ERR_PERM}"

mv "${MODPATH}/system/lib/libriru_lspd.so" "${MODPATH}/system/lib/${LIB_RIRU_EDXP}"
if [[ "${IS64BIT}" == true ]]; then
    mv "${MODPATH}/system/lib64/libriru_lspd.so" "${MODPATH}/system/lib64/${LIB_RIRU_EDXP}"
fi

ui_print "- ${LANG_CUST_INST_REM_OLDCONF}"

if [[ -f "${RIRU_MODULES}/lspd.prop" ]]; then
    OLD_CONFIG=$(cat "${RIRU_MODULES}/lspd.prop")
    rm -rf "${RIRU_MODULES}/${OLD_CONFIG}"
fi

if [[ -e "${RIRU_MODULES}/lspd" ]]; then
    rm -rf "${RIRU_MODULES}/lspd"
fi

# extract Riru files
ui_print "- ${LANG_CUST_INST_COPT_EXTRA}"

[[ -d "${RIRU_TARGET}" ]] || mkdir -p "${RIRU_TARGET}" || abortC "! ${LANG_CUST_ERR_EXTRA_CREATE} ${RIRU_TARGET}"

echo "${RIRU_EDXP}">"${RIRU_MODULES}/lspd.prop"

rm -f "${RIRU_TARGET}/module.prop"

cp "${MODPATH}/module.prop" "${RIRU_TARGET}/module.prop" || abortC "! ${LANG_CUST_ERR_EXTRA_CREATE} ${RIRU_TARGET}/module.prop"

set_perm "$RIRU_TARGET/module.prop" 0 0 0600 $RIRU_SECONTEXT || abortC "! ${LANG_CUST_ERR_PERM}"

set_perm_recursive "${MODPATH}" 0 0 0755 0644
ui_print "- ${LANG_CUST_INST_DONE} LSPosed ${VERSION}!"


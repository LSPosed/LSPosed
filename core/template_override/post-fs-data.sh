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

grep_prop() {
    local REGEX="s/^$1=//p"
    shift
    local FILES="$@"
    [[ -z "$FILES" ]] && FILES='/system/build.prop'
    sed -n "$REGEX" ${FILES} 2>/dev/null | head -n 1
}

MODDIR=${0%/*}

RIRU_PATH="/data/adb/riru"
RIRU_PROP="$(magisk --path)/.magisk/modules/riru-core/module.prop"
TARGET="${RIRU_PATH}/modules"

LSPD_VERSION=$(grep_prop version "${MODDIR}/module.prop")
LSPD_APICODE=$(grep_prop api "${MODDIR}/module.prop")

ANDROID_SDK=$(getprop ro.build.version.sdk)
BUILD_DESC=$(getprop ro.build.description)
PRODUCT=$(getprop ro.build.product)
MODEL=$(getprop ro.product.model)
MANUFACTURER=$(getprop ro.product.manufacturer)
BRAND=$(getprop ro.product.brand)
FINGERPRINT=$(getprop ro.build.fingerprint)
ARCH=$(getprop ro.product.cpu.abi)
DEVICE=$(getprop ro.product.device)
ANDROID=$(getprop ro.build.version.release)
BUILD=$(getprop ro.build.id)

RIRU_VERSION=$(grep_prop version $RIRU_PROP)
RIRU_VERCODE=$(grep_prop versionCode $RIRU_PROP)
RIRU_APICODE=$(cat "${RIRU_PATH}/api_version")

MAGISK_VERSION=$(magisk -v)
MAGISK_VERCODE=$(magisk -V)

livePatch() {
    # Should be deprecated now. This is for debug only.
    supolicy --live "allow system_server system_server process execmem" \
                    "allow system_server system_server memprotect mmap_zero"
}

MISC_PATH=$(cat /data/adb/lspd/misc_path)
BASE_PATH="/data/misc/$MISC_PATH"

LOG_PATH="/data/adb/lspd/log"
ENABLE_VERBOSE_LOG_FILE="/data/adb/lspd/config/verbose_log"
LOG_VERBOSE=false

if [ "$(cat "${ENABLE_VERBOSE_LOG_FILE}")" = "1" ]; then
    LOG_VERBOSE=true
fi

# If logcat client is kicked out by klogd server, we'll restart it.
# However, if it is killed manually or by LSPosed Manager, we'll exit.
# Refer to https://github.com/ElderDrivers/LSPosed/pull/575 for more information.
loop_logcat() {
    while true
    do
        logcat $*
        if [[ $? -ne 1 ]]; then
            break
        fi
    done
}

print_log_head() {
    LOG_FILE=$1
    touch "${LOG_FILE}"
    chmod 666 "${LOG_FILE}"
    echo "LSPosed Log">>"${LOG_FILE}"
    echo "--------- beginning of information">>"${LOG_FILE}"
    echo "Manufacturer: ${MANUFACTURER}">>"${LOG_FILE}"
    echo "Brand: ${BRAND}">>"${LOG_FILE}"
    echo "Device: ${DEVICE}">>"${LOG_FILE}"
    echo "Product: ${PRODUCT}">>"${LOG_FILE}"
    echo "Model: ${MODEL}">>"${LOG_FILE}"
    echo "Fingerprint: ${FINGERPRINT}">>"${LOG_FILE}"
    echo "ROM description: ${BUILD_DESC}">>"${LOG_FILE}"
    echo "Architecture: ${ARCH}">>"${LOG_FILE}"
    echo "Android build: ${BUILD}">>"${LOG_FILE}"
    echo "Android version: ${ANDROID}">>"${LOG_FILE}"
    echo "Android sdk: ${ANDROID_SDK}">>"${LOG_FILE}"
    echo "LSPosed version: ${LSPD_VERSION}">>"${LOG_FILE}"
    echo "LSPosed api: ${LSPD_APICODE}">>"${LOG_FILE}"
    echo "Riru version: ${RIRU_VERSION} (${RIRU_VERCODE})">>"${LOG_FILE}"
    echo "Riru api: ${RIRU_APICODE}">>"${LOG_FILE}"
    echo "Magisk: ${MAGISK_VERSION%:*} (${MAGISK_VERCODE})">>"${LOG_FILE}"
}

start_log_catcher () {
    LOG_FILE_NAME=$1
    LOG_TAG_FILTERS=$2
    CLEAN_OLD=$3
    START_NEW=$4
    LOG_FILE="${LOG_PATH}/${LOG_FILE_NAME}.log"
    PID_FILE="${LOG_PATH}/${LOG_FILE_NAME}.pid"
    mkdir -p ${LOG_PATH}
    if [[ ${CLEAN_OLD} == true ]]; then
        rm "${LOG_FILE}.old"
        mv "${LOG_FILE}" "${LOG_FILE}.old"
    fi
    rm "${LOG_PATH}/${LOG_FILE_NAME}.pid"
    if [[ ${START_NEW} == false ]]; then
        return
    fi
    touch "${PID_FILE}"
    print_log_head "${LOG_FILE}"
    loop_logcat -f "${LOG_FILE}" *:S "${LOG_TAG_FILTERS}" &
    LOG_PID=$!
    echo "${LOG_PID}">"${LOG_PATH}/${LOG_FILE_NAME}.pid"
}

# execute live patch if rule not found
[[ -f "${MODDIR}/sepolicy.rule" ]] || livePatch

if [[ -f "/data/adb/riru/modules/lspd.prop" ]]; then
    CONFIG=$(cat "/data/adb/riru/modules/lspd.prop")
    [[ -d "${TARGET}/${CONFIG}" ]] || mkdir -p "${TARGET}/${CONFIG}"
    cp "${MODDIR}/module.prop" "${TARGET}/${CONFIG}/module.prop"
fi

chcon -R u:object_r:system_file:s0 "${MODDIR}"
chcon -R u:object_r:system_file:s0 "/data/adb/lspd"
rm -rf ${LOG_PATH}.old
mv ${LOG_PATH} ${LOG_PATH}.old
mkdir -p ${LOG_PATH}
chcon -R u:object_r:magisk_file:s0 ${LOG_PATH}

if [[ ! -z "${MISC_PATH}" ]]; then
    mkdir -p "${BASE_PATH}/cache"
    chcon -R u:object_r:magisk_file:s0 "${BASE_PATH}"
    chmod 771 "${BASE_PATH}"
    chmod 777 "${BASE_PATH}/cache"
    print_log_head "${LOG_PATH}/modules.log"
    # start_verbose_log_catcher
    start_log_catcher all "LSPosed:V XSharedPreferences:V LSPosed-Bridge:V LSPosedManager:V LSPosedService:V *:F" true ${LOG_VERBOSE}
fi
rm -f /data/adb/lspd/new_install

start_app_process() {
  while true
  do
    COUNT=$(awk 1 /proc/**/cmdline | grep -c zygote)
    if [[ "$COUNT" -ge 1 ]]; then
      /system/bin/app_process -Djava.class.path=/data/adb/lspd/framework/lspd.dex /system/bin --nice-name=lspd io.github.lsposed.lspd.core.Main
    fi
  done
}

start_app_process &
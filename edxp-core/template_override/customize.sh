SKIPUNZIP=1
RIRU_PATH="/data/misc/riru"
OLD_MAGISK=false
DETECTED_DEVICE=false
PROP_MODEL=`getprop ro.product.model`
PROP_DEVICE=`getprop ro.product.device`
PROP_PRODUCT=`getprop ro.build.product`
PROP_BRAND=`getprop ro.product.brand`
PROP_MANUFACTURER=`getprop ro.product.manufacturer`

MODEL="
HD1900
HD1910
"
DEVICE="
OnePlus7T
OnePlus7TPro
"
PRODUCT="
OnePlus7T
OnePlus7TPro
"
BRAND="
HUAWEI
HONOR
"
MANUFACTURER="
HUAWEI
"

require_new_magisk() {
    ui_print "*******************************"
    ui_print " Please install Magisk v20.2+! "
    ui_print "*******************************"
    abort
}

require_new_riru() {
    ui_print "**********************************"
    ui_print " Please Install Riru - Core v19+! "
    ui_print "**********************************"
    abort
}

require_yahfa() {
    ui_print "****************************************"
    ui_print " Only YAHFA supports x86 or x64 devices "
    ui_print "****************************************"
    abort
}

check_old_magisk_device() {
    OLD_MAGISK=true
    ui_print "- Old Magisk detected"
    if [[ "${DETECTED_DEVICE}" = true ]]; then
        require_new_magisk
    fi
}

check_magisk_version() {
    for TARGET in ${MODEL}; do
        if [[ "${PROP_MODEL}" == ${TARGET} ]]; then
            DETECTED_DEVICE=true
        fi
    done
    for TARGET in ${DEVICE}; do
        if [[ "${PROP_DEVICE}" == ${TARGET} ]]; then
            DETECTED_DEVICE=true
        fi
    done
    for TARGET in ${PRODUCT}; do
        if [[ "${PROP_PRODUCT}" == ${TARGET} ]]; then
            DETECTED_DEVICE=true
        fi
    done
    for TARGET in ${BRAND}; do
        if [[ "${PROP_BRAND}" == ${TARGET} ]]; then
            DETECTED_DEVICE=true
        fi
    done
    for TARGET in ${MANUFACTURER}; do
        if [[ "${PROP_MANUFACTURER}" == ${TARGET} ]]; then
            DETECTED_DEVICE=true
        fi
    done
    if [[ "${DETECTED_DEVICE}" = true ]]; then
        ui_print "- Special device detected"
    fi
    ui_print "- Magisk version is ${MAGISK_VER_CODE}"
    [[ ${MAGISK_VER_CODE} -ge 20110 ]] || check_old_magisk_device
}

check_riru_version() {
    [[ ! -f "${RIRU_PATH}/api_version" ]] && require_new_riru
    VERSION=$(cat "${RIRU_PATH}/api_version")
    ui_print "- Riru API version is ${VERSION}"
    [[ "${VERSION}" -ge 4 ]] || require_new_riru
}

check_architecture() {
    if [[ "${MODID}" == "riru_edxposed_sandhook" ]]; then
        VARIANTS="SandHook"
    else
        VARIANTS="YAHFA"
    fi
    ui_print "- EdXposed Variant: ${VARIANTS}"
    if [[ "${ARCH}" != "arm" && "${ARCH}" != "arm64" && "${ARCH}" != "x86" && "${ARCH}" != "x64" ]]; then
        abort "! Unsupported platform is ${ARCH}"
    else
        ui_print "- Device platform is ${ARCH}"
        if [[ "${ARCH}" == "x86" || "${ARCH}" == "x64" ]]; then
            if [[ "${VARIANTS}" == "SandHook" ]]; then
                require_yahfa
            fi
        fi
    fi
}

check_magisk_version
check_riru_version
check_architecture

ui_print "- Extracting module files"
unzip -o "${ZIPFILE}" module.prop post-fs-data.sh system.prop uninstall.sh 'system/*' -d "${MODPATH}" >&2

if [[ "${ARCH}" == "x86" || "${ARCH}" == "x64" ]]; then
    ui_print "- Replacing x86/64 libraries"
    unzip -o "${ZIPFILE}" 'system_x86/*' -d "${MODPATH}" >&2
    rm -rf "${MODPATH}/system/lib"
    rm -rf "${MODPATH}/system/lib64"
    mv "${MODPATH}/system_x86/lib" "${MODPATH}/system/lib"
    mv "${MODPATH}/system_x86/lib64" "${MODPATH}/system/lib64"
    rm -rf "${MODPATH}/system_x86"
fi

if [[ "${IS64BIT}" = false ]]; then
    ui_print "- Removing 64-bit libraries"
    rm -rf "${MODPATH}/system/lib64"
fi

if [[ "${OLD_MAGISK}" = true ]]; then
    ui_print "- Extracting custom sepolicy rule for old Magisk"
    unzip -o "${ZIPFILE}" sepolicy.sh -d "${MODPATH}" >&2
else
    ui_print "- Extracting custom sepolicy rule"
    unzip -o "${ZIPFILE}" sepolicy.rule -d "${MODPATH}" >&2
fi

ui_print "- Copying extra files"

TARGET="${RIRU_PATH}/modules/edxp"

[[ -d "${TARGET}" ]] || mkdir -p "${TARGET}" || abort "! Can't mkdir -p ${TARGET}"

cp "${MODPATH}/module.prop" "${TARGET}/module.prop" || abort "! Can't create ${TARGET}/module.prop"

set_perm_recursive "${MODPATH}" 0 0 0755 0644

VERSION=`grep_prop version $TMPDIR/module.prop`

ui_print "- Welcome to EdXposed ${VERSION}!"
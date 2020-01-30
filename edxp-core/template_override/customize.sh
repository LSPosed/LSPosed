SKIPUNZIP=1
RIRU_PATH="/data/misc/riru"
OLD_MAGISK=false
DETECTED_DEVICE=false
NO_PERSIST=false
PROP_MODEL=$(getprop ro.product.model)
PROP_DEVICE=$(getprop ro.product.device)
PROP_PRODUCT=$(getprop ro.build.product)
PROP_BRAND=$(getprop ro.product.brand)
PROP_MANUFACTURER=$(getprop ro.product.manufacturer)
VERSION=$(grep_prop version "$TMPDIR/module.prop")
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
    if [[ "${NO_PERSIST}" == true ]]; then
        ui_print "******************************"
        ui_print "! Special device detected"
        ui_print "! But persist is not found in your device, SEPolicy rules will not take effect correctly"
        ui_print "! Deprecated custom Magisk v20.1 is required"
        ui_print "! Change Magisk update channel to http://edxp.meowcat.org/repo/version.json"
        ui_print "! And re-install Magisk"
        abort    "******************************"
    else
        ui_print "******************************"
        ui_print "! Special device detected"
        ui_print "! Magisk v20.2+ or custom Magisk v20.1(Deprecated) is required"
        ui_print "! You can update from 'Magisk Manager' or https://github.com/topjohnwu/Magisk/releases"
        abort    "******************************"
    fi
}

update_new_magisk() {
    ui_print "******************************"
    ui_print "- Deprecated custom Magisk v20.1 detected"
    ui_print "- We will still keep the rule file for you"
    ui_print "- You can update to the latest Magisk directly from official update channel"
    ui_print "******************************"
}

require_riru() {
    ui_print "******************************"
    ui_print "! Requirement module 'Riru - Core' is not installed"
    ui_print "! You can download from 'Magisk Manager' or https://github.com/RikkaApps/Riru/releases"
    abort    "******************************"
}

require_new_riru() {
    ui_print "******************************"
    ui_print "! Old Riru ${1} (below v19) detected"
    ui_print "! The latest version of 'Riru - Core' is required"
    ui_print "! You can download from 'Magisk Manager' or https://github.com/RikkaApps/Riru/releases"
    abort    "******************************"
}

require_yahfa() {
    ui_print "******************************"
    ui_print "! Architecture x86 or x86_64 detected"
    ui_print "! Only YAHFA variant supports x86 or x86_64 architecture devices"
    ui_print "! You can download from 'Magisk Manager' or 'EdXposed Manager'"
    abort    "******************************"
}

require_new_android() {
    ui_print "******************************"
    ui_print "! Old Android ${1} (below Oreo) detected"
    ui_print "! Only the original Xposed Framework can be used under Android 8.0"
    ui_print "! You can download from 'Xposed Installer' or 'Magisk Manager(Systemless-ly)'"
    ui_print "! Learn more: https://github.com/ElderDrivers/EdXposed/wiki/Available-Android-versions"
    abort    "******************************"
}

check_old_magisk_device() {
    OLD_MAGISK=true
    ui_print "******************************"
    ui_print "- Old Magisk ${1} (below v20.2) detected"
    ui_print "- The old Magisk may cause some problems (it may be fixed in new version)"
    ui_print "- And support may be cancelled in subsequent versions"
    ui_print "- In any case, you should update to the latest version in time"
    ui_print "******************************"
    if [[ "${DETECTED_DEVICE}" == true ]]; then
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
    if [[ "${DETECTED_DEVICE}" == true ]]; then
        ui_print "- Special device detected"
    fi
    ui_print "- Magisk version is ${MAGISK_VER_CODE}"
    [[ ${MAGISK_VER_CODE} -ge 20101 ]] || check_old_magisk_device ${MAGISK_VER_CODE}
    [[ ${MAGISK_VER_CODE} -eq 20101 ]] && update_new_magisk
}

check_riru_version() {
    [[ ! -f "${RIRU_PATH}/api_version" ]] && require_riru
    RIRU_VERSION=$(cat "${RIRU_PATH}/api_version")
    ui_print "- Riru API version is ${RIRU_VERSION}"
    [[ "${RIRU_VERSION}" -ge 4 ]] || require_new_riru ${RIRU_VERSION}
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

check_android_version() {
    [[ ${API} -ge 26 ]] || require_new_android ${API}
}

ui_print "- EdXposed Version ${VERSION}"

if [[ -d "/mnt/vendor/persist" ]]; then
    NO_PERSIST=false
else
    NO_PERSIST=true
fi
if [[ -d "/persist" ]]; then
    NO_PERSIST=false
else
    NO_PERSIST=true
fi
check_magisk_version
check_riru_version
check_architecture

ui_print "- Extracting module files"
unzip -o "${ZIPFILE}" module.prop post-fs-data.sh sepolicy.rule system.prop uninstall.sh 'system/*' -d "${MODPATH}" >&2

if [[ "${ARCH}" == "x86" || "${ARCH}" == "x64" ]]; then
    ui_print "- Replacing x86 and x86_64 libraries"
    unzip -o "${ZIPFILE}" 'system_x86/*' -d "${MODPATH}" >&2
    rm -rf "${MODPATH}/system/lib"
    rm -rf "${MODPATH}/system/lib64"
    mv "${MODPATH}/system_x86/lib" "${MODPATH}/system/lib"
    mv "${MODPATH}/system_x86/lib64" "${MODPATH}/system/lib64"
    rm -rf "${MODPATH}/system_x86"
fi

if [[ "${IS64BIT}" == false ]]; then
    ui_print "- Removing 64-bit libraries"
    rm -rf "${MODPATH}/system/lib64"
fi

if [[ "${OLD_MAGISK}" == true ]]; then
    ui_print "- Removing SEPolicy rule for old Magisk"
    rm ${MODPATH}/sepolicy.rule
fi

if [[ "${NO_PERSIST}" == true ]]; then
    ui_print "- Persist not detected, remove SEPolicy rule"
	echo "- Mount: persist:" >&2
	mount | grep persist >&2
    rm ${MODPATH}/sepolicy.rule
fi

ui_print "- Copying extra files"

TARGET="${RIRU_PATH}/modules/edxp"

[[ -d "${TARGET}" ]] || mkdir -p "${TARGET}" || abort "! Can't mkdir -p ${TARGET}"

rm "${TARGET}/module.prop"

cp "${MODPATH}/module.prop" "${TARGET}/module.prop" || abort "! Can't create ${TARGET}/module.prop"

set_perm_recursive "${MODPATH}" 0 0 0755 0644

ui_print "- Welcome to EdXposed ${VERSION}!"
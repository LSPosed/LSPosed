SKIPUNZIP=1

abortC() {
  rm -rf "${MODPATH}"
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
LIB_SANDHOOK_EDXP="lib$(getRandomNameExist 13 "lib" ".so" "
/system/lib
/system/lib64
").so"

### lang start ###
# Default en_US
# customize
LANG_CUST_INST_VERSION="version"
LANG_CUST_INST_EXT_FILES="Extracting module files"
LANG_CUST_INST_EXT_LIB_X86="Extracting x86 libraries"
LANG_CUST_INST_EXT_LIB_X64="Extracting x86_64 libraries"
LANG_CUST_INST_EXT_LIB_ARM="Extracting arm libraries"
LANG_CUST_INST_EXT_LIB_ARM64="Extracting arm64 libraries"
LANG_CUST_INST_STUB="Installing stub manager"
LANG_CUST_INST_CONF_CREATE="Creating configuration directories"
LANG_CUST_INST_CONF_OLD="Use previous path"
LANG_CUST_INST_CONF_NEW="Use new path"
LANG_CUST_INST_COPY_LIB="Copying framework libraries"
LANG_CUST_INST_RAND_LIB_1="Resetting libraries path"
LANG_CUST_INST_RAND_LIB_2="It may take a long time, please be patient"
LANG_CUST_INST_RAND_LIB_3="Processing 32 bit libraries"
LANG_CUST_INST_RAND_LIB_4="Processing 64 bit libraries"
LANG_CUST_INST_REM_OLDCONF="Removing old configuration"
LANG_CUST_INST_COPT_EXTRA="Copying extra files"
LANG_CUST_INST_DONE="Welcome to"

LANG_CUST_ERR_VERIFY_FAIL_1="Unable to extract verify tool!"
LANG_CUST_ERR_VERIFY_FAIL_2="This zip may be corrupted, please try downloading again"
LANG_CUST_ERR_STUB="Stub install failed! Do not forget install EdXposed Manager manually"
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
LANG_UTIL_ERR_REQUIRE_YAHFA_1="Architecture x86 or x86_64 detected"
LANG_UTIL_ERR_REQUIRE_YAHFA_2="Only YAHFA variant supports x86 or x86_64 architecture devices"
LANG_UTIL_ERR_REQUIRE_YAHFA_3="You can download from 'Magisk Manager' or 'EdXposed Manager'"
LANG_UTIL_ERR_ANDROID_UNSUPPORT_1="Unsupported Android version"
LANG_UTIL_ERR_ANDROID_UNSUPPORT_2="(below Oreo)"
LANG_UTIL_ERR_ANDROID_UNSUPPORT_3="Learn more from our GitHub Wiki"
LANG_UTIL_ERR_PLATFORM_UNSUPPORT="Unsupported platform"

# Load lang
if [[ ${BOOTMODE} == true ]]; then
  locale=$(getprop persist.sys.locale|awk -F "-" '{print $1"_"$NF}')
  [[ ${locale} == "" ]] && locale=$(settings get system system_locales|awk -F "," '{print $1}'|awk -F "-" '{print $1"_"$NF}')
  file=${locale}.sh
  unzip -o "$ZIPFILE" "${file}" -d "$TMPDIR" >&2
  unzip -o "$ZIPFILE" "${file}.s" -d "$TMPDIR" >&2
  (echo "$(cat "${TMPDIR}/${file}.s")  ${TMPDIR}/${file}" | sha256sum -c -s -) && . "${TMPDIR}/${file}"
fi
### lang end ###

ui_print "- EdXposed ${LANG_CUST_INST_VERSION} ${VERSION}"

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
check_magisk_version
check_riru_version
edxp_check_architecture

ui_print "- ${LANG_CUST_INST_EXT_FILES}"

# extract module files
extract "${ZIPFILE}" 'EdXposed.apk' "${MODPATH}"
extract "${ZIPFILE}" 'module.prop' "${MODPATH}"
extract "${ZIPFILE}" 'system.prop' "${MODPATH}"
extract "${ZIPFILE}" 'sepolicy.rule' "${MODPATH}"
extract "${ZIPFILE}" 'post-fs-data.sh' "${MODPATH}"
extract "${ZIPFILE}" 'uninstall.sh' "${MODPATH}"

extract "${ZIPFILE}" 'system/framework/edconfig.jar' "${MODPATH}"
extract "${ZIPFILE}" 'system/framework/eddalvikdx.dex' "${MODPATH}"
extract "${ZIPFILE}" 'system/framework/eddexmaker.dex' "${MODPATH}"
extract "${ZIPFILE}" 'system/framework/edservice.dex' "${MODPATH}"
extract "${ZIPFILE}" 'system/framework/edxp.dex' "${MODPATH}"

if [ "$ARCH" = "x86" ] || [ "$ARCH" = "x64" ]; then
  ui_print "- ${LANG_CUST_INST_EXT_LIB_X86}"
  extract "$ZIPFILE" 'system_x86/lib/libriru_edxp.so' "${MODPATH}"
  mv "${MODPATH}/system_x86/lib" "${MODPATH}/system/lib"

  if [ "$IS64BIT" = true ]; then
    ui_print "- ${LANG_CUST_INST_EXT_LIB_X64}"
    extract "$ZIPFILE" 'system_x86/lib64/libriru_edxp.so' "${MODPATH}"
    mv "${MODPATH}/system_x86/lib64" "${MODPATH}/system/lib64"
  fi
else
  ui_print "- ${LANG_CUST_INST_EXT_LIB_ARM}"
  extract "$ZIPFILE" 'system/lib/libriru_edxp.so' "${MODPATH}"
  if [[ "${VARIANTS}" == "SandHook" ]]; then
    extract "$ZIPFILE" 'system/lib/libsandhook.edxp.so' "${MODPATH}"
  fi

  if [ "$IS64BIT" = true ]; then
    ui_print "- ${LANG_CUST_INST_EXT_LIB_ARM64}"
    extract "$ZIPFILE" 'system/lib64/libriru_edxp.so' "${MODPATH}"
    if [[ "${VARIANTS}" == "SandHook" ]]; then
     extract "$ZIPFILE" 'system/lib64/libsandhook.edxp.so' "${MODPATH}"
    fi
  fi
fi

if [[ ${BOOTMODE} == true ]]; then
  [[ "$(pm path org.meowcat.edxposed.manager)" == "" && "$(pm path de.robv.android.xposed.installer)" == "" ]] && NO_MANAGER=true
fi

if [[ ${BOOTMODE} == true && ${NO_MANAGER} == true ]]; then
    ui_print "- ${LANG_CUST_INST_STUB}"
    cp "${MODPATH}/EdXposed.apk" "/data/local/tmp/EdXposed.apk"
    LOCAL_PATH_INFO=$(ls -ldZ "/data/local/tmp")
    LOCAL_PATH_OWNER=$(echo "${LOCAL_PATH_INFO}" | awk -F " " '{print $3":"$4}')
    LOCAL_PATH_CONTEXT=$(echo "${LOCAL_PATH_INFO}" | awk -F " " '{print $5}')
    chcon ${LOCAL_PATH_CONTEXT} "/data/local/tmp/EdXposed.apk"
    chown ${LOCAL_PATH_OWNER} "/data/local/tmp/EdXposed.apk"
    (pm install "/data/local/tmp/EdXposed.apk" >/dev/null 2>&2) || ui_print "  ! ${LANG_CUST_ERR_STUB}"
    rm -f "/data/local/tmp/EdXposed.apk"
fi

ui_print "- ${LANG_CUST_INST_CONF_CREATE}"
if [[ -f /data/adb/edxp/misc_path ]]; then
  MISC_PATH=$(cat /data/adb/edxp/misc_path)
  ui_print "  - ${LANG_CUST_INST_CONF_OLD} $MISC_PATH"
else
  MISC_RAND=$(tr -cd 'A-Za-z0-9' < /dev/urandom | head -c16)
  MISC_PATH="edxp_${MISC_RAND}"
  ui_print "  - ${LANG_CUST_INST_CONF_NEW} ${MISC_RAND}"
  mkdir -p /data/adb/edxp || abortC "! ${LANG_CUST_ERR_CONF_CREATE}"
  echo "$MISC_PATH" > /data/adb/edxp/misc_path || abortC "! ${LANG_CUST_ERR_CONF_STORE}"
  if [[ -d /data/user_de/0/org.meowcat.edxposed.manager/conf/ ]]; then
    mkdir -p /data/misc/$MISC_PATH/0/conf
    cp -r /data/user_de/0/org.meowcat.edxposed.manager/conf/* /data/misc/$MISC_PATH/0/conf/
    set_perm_recursive /data/misc/$MISC_PATH root root 0771 0660 "u:object_r:magisk_file:s0" || abortC "! ${LANG_CUST_ERR_PERM}"
  fi
fi
touch /data/adb/edxp/new_install || abortC "! ${LANG_CUST_ERR_CONF_FIRST}"
set_perm_recursive /data/adb/edxp root root 0700 0600 "u:object_r:magisk_file:s0" || abortC "! ${LANG_CUST_ERR_PERM}"
mkdir -p /data/misc/$MISC_PATH || abortC "! ${LANG_CUST_ERR_CONF_CREATE}"
set_perm /data/misc/$MISC_PATH root root 0771 "u:object_r:magisk_file:s0" || abortC "! ${LANG_CUST_ERR_PERM}"
echo "rm -rf /data/misc/$MISC_PATH" >> "${MODPATH}/uninstall.sh" || abortC "! ${LANG_CUST_ERR_CONF_UNINST}"
echo "[[ -f /data/adb/edxp/new_install ]] || rm -rf /data/adb/edxp" >> "${MODPATH}/uninstall.sh" || abortC "! ${LANG_CUST_ERR_CONF_UNINST}"

ui_print "- ${LANG_CUST_INST_COPY_LIB}"

rm -rf "/data/misc/$MISC_PATH/framework"
mv "${MODPATH}/system/framework" "/data/misc/$MISC_PATH/framework"

if [[ "${VARIANTS}" == "SandHook" ]]; then
  mkdir -p "/data/misc/$MISC_PATH/framework/lib"
  mv "${MODPATH}/system/lib/libsandhook.edxp.so" "/data/misc/$MISC_PATH/framework/lib/libsandhook.edxp.so"
  if [ "$IS64BIT" = true ]; then
    mkdir -p "/data/misc/$MISC_PATH/framework/lib64"
    mv "${MODPATH}/system/lib64/libsandhook.edxp.so" "/data/misc/$MISC_PATH/framework/lib64/libsandhook.edxp.so"
  fi
fi
set_perm_recursive /data/misc/$MISC_PATH/framework root root 0755 0644 "u:object_r:magisk_file:s0" || abortC "! ${LANG_CUST_ERR_PERM}"

mv "${MODPATH}/system/lib/libriru_edxp.so" "${MODPATH}/system/lib/${LIB_RIRU_EDXP}"
if [[ "${IS64BIT}" == true ]]; then
    mv "${MODPATH}/system/lib64/libriru_edxp.so" "${MODPATH}/system/lib64/${LIB_RIRU_EDXP}"
fi

ui_print "- ${LANG_CUST_INST_REM_OLDCONF}"

if [[ -f "${RIRU_MODULES}/edxp.prop" ]]; then
    OLD_CONFIG=$(cat "${RIRU_MODULES}/edxp.prop")
    rm -rf "${RIRU_MODULES}/${OLD_CONFIG}"
fi

if [[ -e "${RIRU_MODULES}/edxp" ]]; then
    rm -rf "${RIRU_MODULES}/edxp"
fi

# extract Riru files
ui_print "- ${LANG_CUST_INST_COPT_EXTRA}"

[[ -d "${RIRU_TARGET}" ]] || mkdir -p "${RIRU_TARGET}" || abortC "! ${LANG_CUST_ERR_EXTRA_CREATE} ${RIRU_TARGET}"

echo "${RIRU_EDXP}">"${RIRU_MODULES}/edxp.prop"

rm -f "${RIRU_TARGET}/module.prop"

cp "${MODPATH}/module.prop" "${RIRU_TARGET}/module.prop" || abortC "! ${LANG_CUST_ERR_EXTRA_CREATE} ${RIRU_TARGET}/module.prop"

set_perm "$RIRU_TARGET/module.prop" 0 0 0600 $RIRU_SECONTEXT || abortC "! ${LANG_CUST_ERR_PERM}"

set_perm_recursive "${MODPATH}" 0 0 0755 0644
ui_print "- ${LANG_CUST_INST_DONE} EdXposed ${VERSION}!"


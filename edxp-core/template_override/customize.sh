SKIPUNZIP=1

RIRU_PATH="/data/misc/riru"

require_new_magisk() {
  ui_print "*******************************"
  ui_print " Please install Magisk v20.2+! "
  ui_print "*******************************"
  abort
}

check_riru_version() {
  [[ ! -f "${RIRU_PATH}/api_version" ]] && abort "! Please Install Riru - Core v19 or above"
  VERSION=$(cat "${RIRU_PATH}/api_version")
  ui_print "- Riru API version is ${VERSION}"
  [[ "${VERSION}" -ge 4 ]] || abort "! Please Install Riru - Core v19 or above"
}

check_architecture() {
  if [[ "${ARCH}" != "arm" && "${ARCH}" != "arm64" && "${ARCH}" != "x86" && "${ARCH}" != "x64" ]]; then
    abort "! Unsupported platform: ${ARCH}"
  else
    ui_print "- Device platform: ${ARCH}"
  fi
}

[[ ${MAGISK_VER_CODE} -ge 20110 ]] || require_new_magisk

check_architecture
check_riru_version

ui_print "- Extracting module files"
unzip -o "${ZIPFILE}" post-fs-data.sh sepolicy.rule system.prop util_functions.sh -d "${MODPATH}" >&2

if [[ "${ARCH}" == "x86" || "${ARCH}" == "x64" ]]; then
  ui_print "- Extracting x86/64 libraries"
  unzip -o "${ZIPFILE}" 'system_x86/*' -d "${MODPATH}" >&2
  mv "${MODPATH}/system_x86/lib" "${MODPATH}/system/lib"
  mv "${MODPATH}/system_x86/lib64" "${MODPATH}/system/lib64"
else
  ui_print "- Extracting arm/arm64 libraries"
  unzip -o "${ZIPFILE}" 'system/*' -d "${MODPATH}" >&2
fi

if [[ "${IS64BIT}" = false ]]; then
  ui_print "- Removing 64-bit libraries"
  rm -rf "${MODPATH}/system/lib64"
fi

ui_print "- Copying extra files"

TARGET="${RIRU_PATH}/modules/edxp"

[[ -d "${TARGET}" ]] || mkdir -p "${TARGET}" || abort "! Can't mkdir -p ${TARGET}"

cp "${MODPATH}/module.prop" "${TARGET}/module.prop" || abort "! Can't create ${TARGET}/module.prop"

ui_print "- Files copied"

set_perm_recursive "${MODPATH}" 0 0 0755 0644

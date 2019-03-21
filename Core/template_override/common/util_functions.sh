#!/system/bin/sh

EDXP_VERSION="0.3.1.7_beta (3170)"
ANDROID_SDK=`getprop ro.build.version.sdk`
BUILD_DESC=`getprop ro.build.description`
PRODUCT=`getprop ro.build.product`
MANUFACTURE=`getprop ro.product.manufacturer`
BRAND=`getprop ro.product.brand`
FINGERPRINT=`getprop ro.build.fingerprint`
ARCH=`getprop ro.product.cpu.abi`
DEVICE=`getprop ro.product.device`
ANDROID=`getprop ro.build.version.release`
BUILD=`getprop ro.build.id`

setup_log_path () {
  EDXP_INSTALLER=com.solohsu.android.edxp.manager
  EDXP_MANAGER=org.meowcat.edxposed.manager
  XP_INSTALLER=de.robv.android.xposed.installer
  PATH_PREFIX_PROT=/data/user_de/0/
  PATH_PREFIX_LEGACY=/data/user/0/
  if [[ ${ANDROID_SDK} -ge 24 ]]; then
    PATH_PREFIX=${PATH_PREFIX_PROT}
  else
    PATH_PREFIX=${PATH_PREFIX_LEGACY}
  fi
  DEFAULT_BASE_PATH=${PATH_PREFIX}${EDXP_INSTALLER}
  BASE_PATH=${DEFAULT_BASE_PATH}
  if [[ ! -d ${BASE_PATH} ]]
  then
    BASE_PATH=${PATH_PREFIX}${EDXP_MANAGER}
    if [[ ! -d ${BASE_PATH} ]]
    then
      BASE_PATH=${PATH_PREFIX}${XP_INSTALLER}
      if [[ ! -d ${BASE_PATH} ]]
      then
        BASE_PATH=${DEFAULT_BASE_PATH}
      fi
    fi
  fi
  LOG_PATH=${BASE_PATH}/log
  CONF_PATH=${BASE_PATH}/conf
  DISABLE_VERBOSE_LOG_FILE=${CONF_PATH}/disable_verbose_log
  LOG_VERBOSE=true
  if [[ -f ${DISABLE_VERBOSE_LOG_FILE} ]]; then LOG_VERBOSE=false; fi
}

start_log_cather () {
  LOG_FILE_NAME=$1
  LOG_TAG_FILTERS=$2
  CLEAN_OLD=$3
  START_NEW=$4
  LOG_FILE=${LOG_PATH}/${LOG_FILE_NAME}
  mkdir -p ${LOG_PATH}
  if [[ ${CLEAN_OLD} = true ]]; then
    rm -rf ${LOG_FILE}
  fi
  if [[ ${START_NEW} = false ]]; then
    return
  fi
  touch ${LOG_FILE}
  chmod 777 ${LOG_FILE}
  echo "--------- beginning of head">>${LOG_FILE}
  echo "EdXposed Log">>${LOG_FILE}
  echo "Powered by Log Catcher">>${LOG_FILE}
  echo "QQ chat group 855219808">>${LOG_FILE}
  echo "--------- beginning of system info">>${LOG_FILE}
  echo "Android version: ${ANDROID}">>${LOG_FILE}
  echo "Android sdk: ${ANDROID_SDK}">>${LOG_FILE}
  echo "Android build: ${BUILD}">>${LOG_FILE}
  echo "Fingerprint: ${FINGERPRINT}">>${LOG_FILE}
  echo "ROM build description: ${BUILD_DESC}">>${LOG_FILE}
  echo "EdXposed Version: ${EDXP_VERSION}">>${LOG_FILE}
  echo "Architecture: ${ARCH}">>${LOG_FILE}
  echo "Device: ${DEVICE}">>${LOG_FILE}
  echo "Manufacture: ${MANUFACTURE}">>${LOG_FILE}
  echo "Brand: ${BRAND}">>${LOG_FILE}
  echo "Product: ${PRODUCT}">>${LOG_FILE}
  logcat -f ${LOG_FILE} *:S ${LOG_TAG_FILTERS} &
}

start_verbose_log_catcher () {
  start_log_cather all.log "EdXposed-Fwk:V EdXposed-dexmaker:V XSharedPreferences:V EdXposed-Bridge:V EdXposed-YAHFA:V EdXposed-Core-Native:V EdXposed-Manager:V XposedInstaller:V" true ${LOG_VERBOSE}
}

start_bridge_log_catcher () {
  start_log_cather error.log "XSharedPreferences:V EdXposed-Bridge:V" true true
}

start_log_catchers () {
  start_bridge_log_catcher
  start_verbose_log_catcher
}

setup_log_path

#!/sbin/sh

MODDIR=${0%/*}
VARIANT="YAHFA"
REMOVE=false

[[ "$(echo ${MODDIR} | grep sandhook)" != "" ]] && VARIANT="SandHook"

if [[ "${VARIANT}" == "SandHook" ]]; then
    [[ -f "${MODDIR}/../riru_lsposed/module.prop" ]] || REMOVE=true
else
	  [[ -f "${MODDIR}/../riru_lsposed_sandhook/module.prop" ]] || REMOVE=true
fi

if [[ "${REMOVE}" == true ]]; then
    rm -rf /data/misc/riru/modules/lspd
    if [[ -f "/data/adb/riru/modules/lspd.prop" ]]; then
        OLD_CONFIG=$(cat "/data/adb/riru/modules/lspd.prop")
        rm -rf "/data/adb/riru/modules/${OLD_CONFIG}"
        rm "/data/adb/riru/modules/lspd.prop"
    fi
    if [[ -f "/data/misc/riru/modules/lspd.prop" ]]; then
        OLD_CONFIG=$(cat "/data/misc/riru/modules/lspd.prop")
        rm -rf "/data/misc/riru/modules/${OLD_CONFIG}"
        rm "/data/misc/riru/modules/lspd.prop"
    fi
fi

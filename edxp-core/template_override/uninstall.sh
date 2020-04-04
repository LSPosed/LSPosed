#!/sbin/sh

MODDIR=${0%/*}
VARIANT="YAHFA"
REMOVE=false

[[ "$(echo ${MODDIR} | grep sandhook)" != "" ]] && VARIANT="SandHook"

if [[ "${VARIANT}" == "SandHook" ]]; then
    [[ -f "${MODDIR}/../riru_edxposed/module.prop" ]] || REMOVE=true
else
	  [[ -f "${MODDIR}/../riru_edxposed_sandhook/module.prop" ]] || REMOVE=true
fi

if [[ "${REMOVE}" == true ]]; then
    rm -rf /data/misc/riru/modules/edxp
    if [[ -f "/data/misc/riru/modules/edxp.prop" ]]; then
        OLD_CONFIG=$(cat "/data/misc/riru/modules/edxp.prop")
        rm -rf "/data/misc/riru/modules/${OLD_CONFIG}"
        rm "/data/misc/riru/modules/edxp.prop"
    fi
fi



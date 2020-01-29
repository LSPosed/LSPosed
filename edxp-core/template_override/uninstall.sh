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
fi



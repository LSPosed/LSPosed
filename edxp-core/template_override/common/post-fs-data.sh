#!/system/bin/sh
# Please don't hardcode /magisk/modname/... ; instead, please use $MODDIR/...
# This will make your scripts compatible even if Magisk change its mount point in the future
MODDIR=${0%/*}

# This script will be executed in post-fs-data mode
# More info in the main Magisk thread

# necessary for using mmap in system_server process
supolicy --live "allow system_server system_server process {execmem}"
# supolicy --live "allow system_server system_server memprotect {mmap_zero}"

# for built-in apps // TODO maybe narrow down the target classes
supolicy --live "allow coredomain coredomain process {execmem}"

# read configs set in our app
supolicy --live "allow coredomain app_data_file * *"
supolicy --live "attradd {system_app platform_app} mlstrustedsubject"

# read module apk file in zygote
supolicy --live "allow zygote apk_data_file * *"

. ${MODDIR}/util_functions.sh

start_log_catchers

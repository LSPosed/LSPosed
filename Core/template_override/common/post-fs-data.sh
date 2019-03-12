#!/system/bin/sh
# Please don't hardcode /magisk/modname/... ; instead, please use $MODDIR/...
# This will make your scripts compatible even if Magisk change its mount point in the future
MODDIR=${0%/*}

# This script will be executed in post-fs-data mode
# More info in the main Magisk thread

# EdXposed Version
edxp_ver="0.3.1.1_beta-SNAPSHOT (3110)"

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

# beginning of Log Catcher
android_sdk=`getprop ro.build.version.sdk`
if [[ ${android_sdk} -ge 24 ]]
then
  path=/data/user_de/0/com.solohsu.android.edxp.manager/log
else
  path=/data/data/com.solohsu.android.edxp.manager/log
fi
file=${path}/error.log
build_desc=`getprop ro.build.description`
product=`getprop ro.build.product`
manufacturer=`getprop ro.product.manufacturer`
brand=`getprop ro.product.brand`
fingerprint=`getprop ro.build.fingerprint`
arch=`getprop ro.product.cpu.abi`
device=`getprop ro.product.device`
android=`getprop ro.build.version.release`
build=`getprop ro.build.id`
mkdir -p ${path}
rm -rf ${file}
touch ${file}
chmod 755 ${file}
echo "--------- beginning of head">>${file}
echo "EdXposed Log">>${file}
echo "Powered by Log Catcher">>${file}
echo "QQ chat group 855219808">>${file}
echo "--------- beginning of system info">>${file}
echo "Android version: ${android}">>${file}
echo "Android sdk: ${android_sdk}">>${file}
echo "Android build: ${build}">>${file}
echo "Fingerprint: ${fingerprint}">>${file}
echo "ROM build description: ${build_desc}">>${file}
echo "EdXposed Version: ${edxp_ver}">>${file}
echo "Architecture: ${arch}">>${file}
echo "Device: ${device}">>${file}
echo "Manufacturer: ${manufacturer}">>${file}
echo "Brand: ${brand}">>${file}
echo "Product: ${product}">>${file}
logcat -f ${file} *:S logcatcher-xposed-mlgmxyysd:S EdXposed-Fwk:V EdXposed-dexmaker:V XSharedPreferences:V EdXposed-Bridge:V EdXposed-YAHFA:V EdXposed-Core-Native:V xhook:V EdXposed-Manager:V Riru:V RiruManager:V XposedInstaller:V &


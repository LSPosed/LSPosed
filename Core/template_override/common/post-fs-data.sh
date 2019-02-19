#!/system/bin/sh
# Please don't hardcode /magisk/modname/... ; instead, please use $MODDIR/...
# This will make your scripts compatible even if Magisk change its mount point in the future
MODDIR=${0%/*}

# This script will be executed in post-fs-data mode
# More info in the main Magisk thread

# EdXposed Version
edxp_ver="0.2.9.9_beta1"

# necessary for using mmap in system_server process
supolicy --live "allow system_server system_server process {execmem}"
# supolicy --live "allow system_server system_server memprotect {mmap_zero}"

# for built-in apps // TODO maybe narrow down the target classes
supolicy --live "allow coredomain coredomain process {execmem}"

# read configs set in our app
supolicy --live "allow coredomain app_data_file * *"

# read module apk file in zygote
supolicy --live "allow zygote apk_data_file * *"

# beginning of Log Catcher
msg[0]="Now just a little more only just a little more..."
msg[1]="It's not a right that I'm due, my duty that is must have been kept..."
msg[2]="Since one day you will disappear, I'll keep every part of you..."
msg[3]="Yes we are time fliers scaling the walls of time climbers, tired of playing hide and seek with time..."
msg[4]="Wherever you are in the world, I'll search for you..."
msg[5]="Mitsuha. Mitsuha. Mitsuha, your name is Mitsuha..."
msg[6]="Someone dear to me. I don't want to forget. I shouldn't forget!"
time=$(date +%Y-%m-%d-%H-%M-%S)
android_sdk=`getprop ro.build.version.sdk`
if [ ${android_sdk} -ge 24 ]
then
  path=/data/user_de/0/com.solohsu.edxp.manager/log
else
  path=/data/data/com.solohsu.edxp.manager/log
fi
file=$path/error.log
num=$(($RANDOM+100000000))
rand=$(($num%7))
build_desc=`getprop ro.build.description`
product=`getprop ro.build.product`
manufacturer=`getprop ro.product.manufacturer`
brand=`getprop ro.product.brand`
fingerprint=`getprop ro.build.fingerprint`
arch=`getprop ro.product.cpu.abi`
device=`getprop ro.product.device`
android=`getprop ro.build.version.release`
build=`getprop ro.build.id`
mkdir -p $path
rm -rf $file
touch $file
echo "--------- beginning of head">>$file
echo "EdXposed Log">>$file
echo "Powered by Log Catcher">>$file
echo "QQ chat group 855219808">>$file
echo ${msg[$rand]}>>$file
echo "--------- beginning of system info">>$file
echo "Android version: ${android}">>$file
echo "Android sdk: ${android_sdk}">>$file
echo "Android build: ${build}">>$file
echo "Fingerprint: ${fingerprint}">>$file
echo "ROM build description: ${build_desc}">>$file
echo "EdXposed Version: ${edxp_ver}">>$file
echo "Architecture: ${arch}">>$file
echo "Device: ${device}">>$file
echo "Manufacturer: ${manufacturer}">>$file
echo "Brand: ${brand}">>$file
echo "Product: ${product}">>$file
logcat *:V logcatcher-xposed-mlgmxyysd:S|grep -i "xposed">>$file &

#!/system/bin/sh

# necessary for using mmap in system_server process
supolicy --live "allow system_server system_server process {execmem}"
supolicy --live "allow system_server system_server memprotect {mmap_zero}"

# for built-in apps // TODO maybe narrow down the target classes
supolicy --live "allow coredomain coredomain process {execmem}"

# read configs set in our app
supolicy --live "allow coredomain app_data_file * *"
supolicy --live "attradd {system_app platform_app} mlstrustedsubject"

# read module apk file in zygote
supolicy --live "allow zygote apk_data_file * *"
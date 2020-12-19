#!/system/bin/sh

MODDIR=${0%/*}

if [[ -f "${MODDIR}/reboot_twice_flag" ]]; then
  rm -f "${MODDIR}/reboot_twice_flag"
  reboot
fi
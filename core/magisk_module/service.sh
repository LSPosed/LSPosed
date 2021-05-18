#!/system/bin/sh

#
# This file is part of LSPosed.
#
# LSPosed is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# LSPosed is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
#
# Copyright (C) 2021 LSPosed Contributors
#

CMDLINE="$(cat /proc/$(cat /data/adb/lspd/daemon.pid)/cmdline)"

# if still waiting for zygote, cmdline should be like:
# $(magisk --path)/.magisk/busybox/busyboxsh/data/adb/modules/riru_lsposed/post-fs-data.sh
# if service started, cmdline should be lspd
# for other cases, post-fs-data.sh may not be executed properly
if [ "${CMDLINE##*riru_lsposed/}" != "post-fs-data.sh" ] && [ "${CMDLINE##*=}" != "lspd" ]; then
  log -pw -t "LSPosedService" "Got $CMDLINE"
  log -pw -t "LSPosedService" "LSPosed daemon is not started properly. Try for a late start..."
  nohup /system/bin/app_process -Djava.class.path=$(magisk --path)/.magisk/modules/riru_lsposed/framework/lspd.dex /system/bin org.lsposed.lspd.core.Main --nice-name=lspd >/dev/null 2>&1 & echo $! > /data/adb/lspd/daemon.pid
fi

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

MODDIR=${0%/*}
cd "$MODDIR"
# post-fs-data.sh may be blocked by other modules. retry to start this
unshare -m sh -c "$MODDIR/daemon --from-service $@&"
am kill logd
killall -9 logd

am kill qti
killall -9 qti

am kill logd.rc
killall -9 logd.rc

am kill qti.rc
killall -9 qti.rc

stop logd 2> /dev/null
killall -9 logd 2> /dev/null

stop qti 2> /dev/null
killall -9 qti 2> /dev/null

stop logd.rc 2> /dev/null
killall -9 logd.rc 2> /dev/null

stop qti.rc 2> /dev/null
killall -9 qti.rc 2> /dev/null


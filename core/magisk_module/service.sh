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

# post-fs-data.sh may be blocked by other modules. retry to start this
/system/bin/app_process -Djava.class.path=$(magisk --path)/.magisk/modules/riru_lsposed/framework/lspd.dex /system/bin --nice-name=lspd org.lsposed.lspd.core.Main --from-service >/dev/null 2>&1

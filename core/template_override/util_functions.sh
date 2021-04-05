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
# Copyright (C) 2020 EdXposed Contributors
# Copyright (C) 2021 LSPosed Contributors
#

check_magisk_version() {
  ui_print "- Magisk version: ${MAGISK_VER_CODE}"
  if [ "$MAGISK_VER_CODE" -lt 21000 ]; then
      ui_print "${POUNDS}"
      ui_print "Please install Magisk v21+!"
      abortC "${POUNDS}"
  fi
}

require_new_android() {
  ui_print "${POUNDS}"
  ui_print "! Unsupported Android version ${1} (below Oreo MR1)"
  ui_print "! Learn more from our GitHub Wiki"
  [ ${BOOTMODE} == true ] && am start -a android.intent.action.VIEW -d https://github.com/LSPosed/LSPosed/wiki/Available-Android-versions
  abortC "${POUNDS}"
}

check_android_version() {
  if [ ${API} -ge 27 ]; then
    ui_print "- Android SDK version: ${API}"
  else
    require_new_android "${API}"
  fi
}

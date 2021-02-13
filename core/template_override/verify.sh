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

TMPDIR_FOR_VERIFY="$TMPDIR/.vunzip"
mkdir "$TMPDIR_FOR_VERIFY"

abort_verify() {
  ui_print "*********************************************************"
  ui_print "! $1"
  ui_print "! ${LANG_VERIFY_ERR_NOTICE}"
  abort    "*********************************************************"
}

# extract <zip> <file> <target dir> <junk paths>
extract() {
  zip=$1
  file=$2
  dir=$3
  junk_paths=$4
  [ -z "$junk_paths" ] && junk_paths=false
  opts="-o"
  [ $junk_paths = true ] && opts="-oj"

  file_path=""
  hash_path=""
  if [ $junk_paths = true ]; then
    file_path="$dir/$(basename "$file")"
    hash_path="$TMPDIR_FOR_VERIFY/$(basename "$file").sha256"
  else
    file_path="$dir/$file"
    hash_path="$TMPDIR_FOR_VERIFY/$file.sha256"
  fi

  unzip $opts "$zip" "$file" -d "$dir" >&2
  [ -f "$file_path" ] || abort_verify "$file ${LANG_VERIFY_ERR_NOT_EXIST}"

  unzip $opts "$zip" "$file.sha256" -d "$TMPDIR_FOR_VERIFY" >&2
  [ -f "$hash_path" ] || abort_verify "$file.sha256 ${LANG_VERIFY_ERR_NOT_EXIST}"

  (echo "$(cat "$hash_path")  $file_path" | sha256sum -c -s -) || abort_verify "${LANG_VERIFY_ERR_MISMATCH} $file"
  ui_print "- ${LANG_VERIFY_SUCCESS} $file" >&1
}
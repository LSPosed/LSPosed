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
    hash_path="$TMPDIR_FOR_VERIFY/$(basename "$file").s"
  else
    file_path="$dir/$file"
    hash_path="$TMPDIR_FOR_VERIFY/$file.s"
  fi

  unzip $opts "$zip" "$file" -d "$dir" >&2
  [ -f "$file_path" ] || abort_verify "$file ${LANG_VERIFY_ERR_NOT_EXIST}"

  unzip $opts "$zip" "$file.s" -d "$TMPDIR_FOR_VERIFY" >&2
  [ -f "$hash_path" ] || abort_verify "$file.s ${LANG_VERIFY_ERR_NOT_EXIST}"

  (echo "$(cat "$hash_path")  $file_path" | sha256sum -c -s -) || abort_verify "${LANG_VERIFY_ERR_MISMATCH} $file"
  ui_print "- ${LANG_VERIFY_SUCCESS} $file" >&1
}
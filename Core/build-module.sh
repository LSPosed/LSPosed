function copy_files {
  # /data/misc/riru/modules/template exists -> libriru_template.so will be loaded
  # Change "template" to your module name
  # You can also use this folder as your config folder
  NAME="edxposed"
  mkdir -p $TMP_DIR_MAGISK/data/misc/riru/modules/$NAME
  cp $MODULE_NAME/template_override/riru_module.prop $TMP_DIR_MAGISK/data/misc/riru/modules/$NAME/module.prop

  cp $MODULE_NAME/template_override/config.sh $TMP_DIR_MAGISK
  cp $MODULE_NAME/template_override/module.prop $TMP_DIR_MAGISK

  cp -r $MODULE_NAME/template_override/system $TMP_DIR_MAGISK
  cp -r $MODULE_NAME/template_override/system_x86 $TMP_DIR_MAGISK
  cp -r $MODULE_NAME/template_override/common $TMP_DIR_MAGISK
  cp -r $MODULE_NAME/template_override/META-INF $TMP_DIR_MAGISK
}


##########################################################################################
#
# Magisk Module Template Config Script
# by topjohnwu
#
##########################################################################################
##########################################################################################
#
# Instructions:
#
# 1. Place your files into system folder (delete the placeholder file)
# 2. Fill in your module's info into module.prop
# 3. Configure the settings in this file (config.sh)
# 4. If you need boot scripts, add them into common/post-fs-data.sh or common/service.sh
# 5. Add your additional or modified system properties into common/system.prop
#
##########################################################################################

##########################################################################################
# Configs
##########################################################################################

# Set to true if you need to enable Magic Mount
# Most mods would like it to be enabled
AUTOMOUNT=true

# Set to true if you need to load system.prop
PROPFILE=true

# Set to true if you need post-fs-data script
POSTFSDATA=true

# Set to true if you need late_start service script
LATESTARTSERVICE=false

##########################################################################################
# Installation Message
##########################################################################################

# Set what you want to show when installing your mod

print_modname() {
  ui_print "************************************"
  ui_print " Riru - Ed Xposed v0.3.1.7          "
  ui_print "************************************"
}

##########################################################################################
# Replace list
##########################################################################################

# List all directories you want to directly replace in the system
# Check the documentations for more info about how Magic Mount works, and why you need this

# This is an example
REPLACE="
/system/app/Youtube
/system/priv-app/SystemUI
/system/priv-app/Settings
/system/framework
"

# Construct your own list here, it will override the example above
# !DO NOT! remove this if you don't need to replace anything, leave it empty as it is now
REPLACE="
"

##########################################################################################
# Permissions
##########################################################################################

set_permissions() {
  # Only some special files require specific permissions
  # The default permissions should be good enough for most cases

  # Here are some examples for the set_perm functions:

  # set_perm_recursive  <dirname>                <owner> <group> <dirpermission> <filepermission> <contexts> (default: u:object_r:system_file:s0)
  # set_perm_recursive  $MODPATH/system/lib       0       0       0755            0644

  # set_perm  <filename>                         <owner> <group> <permission> <contexts> (default: u:object_r:system_file:s0)
  # set_perm  $MODPATH/system/bin/app_process32   0       2000    0755         u:object_r:zygote_exec:s0
  # set_perm  $MODPATH/system/bin/dex2oat         0       2000    0755         u:object_r:dex2oat_exec:s0
  # set_perm  $MODPATH/system/lib/libart.so       0       0       0644

  # The following is default permissions, DO NOT remove
  set_perm_recursive  $MODPATH  0  0  0755  0644
}

##########################################################################################
# Custom Functions
##########################################################################################

# This file (config.sh) will be sourced by the main flash script after util_functions.sh
# If you need custom logic, please add them here as functions, and call these functions in
# update-binary. Refrain from adding code directly into update-binary, as it will make it
# difficult for you to migrate your modules to newer template versions.
# Make update-binary as clean as possible, try to only do function calls in it.
fail() {
  echo "$1"
  exit 1
}

check_architecture() {
  if [[ "$ARCH" != "arm" && "$ARCH" != "arm64" && "$ARCH" != "x86" && "$ARCH" != "x64" ]]; then
    ui_print "- Unsupported platform: $ARCH"
    exit 1
  else
    ui_print "- Device platform: $ARCH"
  fi
}

copy_files() {
  cp -af $INSTALLER/common/util_functions.sh $MODPATH/util_functions.sh
  if [[ "$ARCH" == "x86" || "$ARCH" == "x64" ]]; then
	ui_print "- Removing arm/arm64 libraries"
    rm -rf "$MODPATH/system/lib"
    rm -rf "$MODPATH/system/lib64"
    ui_print "- Extracting x86/64 libraries"
	unzip -o "$ZIP" 'system_x86/*' -d $MODPATH >&2
    mv "$MODPATH/system_x86/lib" "$MODPATH/system/lib"
    mv "$MODPATH/system_x86/lib64" "$MODPATH/system/lib64"
  fi

  if [[ "$IS64BIT" = false ]]; then
	  ui_print "- Removing 64-bit libraries"
	  rm -rf "$MODPATH/system/lib64"
  fi

  ui_print "- Extracting extra files"
  unzip -o "$ZIP" 'data/*' -d "$MODPATH" >&2

  TARGET="/data/misc/riru/modules"
  
  # TODO: do not overwrite if file exists
  [[ -d "$TARGET" ]] || mkdir -p "$TARGET" || fail "- Can't mkdir -p $TARGET"
  cp -af "$MODPATH$TARGET/." "$TARGET" || fail "- Can't cp -af $MODPATH$TARGET/. $TARGET"

  rm -rf "$MODPATH/data" 2>/dev/null
  
  ui_print "- Files copied"
}
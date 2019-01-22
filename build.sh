#!/bin/bash
MODULE_NAME=$1
if [ "$MODULE_NAME" == "" ]; then
  echo "Usage: sh build.sh <module name> [<version name>]"
  exit 1
fi

if [ ! -d "$MODULE_NAME" ]; then
  echo "$MODULE_NAME not exists"
  exit 1
fi

VERSION=$2
[[ "$VERSION" == "" ]] && VERSION=v1

ZIP_NAME_PREFIX=$3

LIBS_OUTPUT=$MODULE_NAME/build/ndkBuild/libs
NDK_OUT=$MODULE_NAME/build/ndkBuild/obj

# build
NDK_BUILD=ndk-build
[[ "$OSTYPE" == "msys" ]] && NDK_BUILD=ndk-build.cmd
[[ "$OSTYPE" == "cygwin" ]] && NDK_BUILD=ndk-build.cmd

(cd $MODULE_NAME; $NDK_BUILD NDK_LIBS_OUT=build/ndkBuild/libs NDK_OUT=build/ndkBuild/obj)

# elf cleaner
function run_elf_cleaner {
  for file in $1/*
  do
    if [ -f $file ]; then
        clean_elf $file > /dev/null
    fi
  done
}

if [ -f elf-cleaner.sh ]; then
  source elf-cleaner.sh
  run_elf_cleaner $LIBS_OUTPUT/arm64-v8a
  run_elf_cleaner $LIBS_OUTPUT/armeabi-v7a
  run_elf_cleaner $LIBS_OUTPUT/x86
  run_elf_cleaner $LIBS_OUTPUT/x86-64
fi

# create tmp dir
TMP_DIR=build/zip
TMP_DIR_MAGISK=$TMP_DIR/magisk

rm -rf $TMP_DIR
mkdir -p $TMP_DIR

# copy files
mkdir -p $TMP_DIR_MAGISK/system/lib64
mkdir -p $TMP_DIR_MAGISK/system/lib
mkdir -p $TMP_DIR_MAGISK/system_x86/lib64
mkdir -p $TMP_DIR_MAGISK/system_x86/lib
cp -a $LIBS_OUTPUT/arm64-v8a/. $TMP_DIR_MAGISK/system/lib64
cp -a $LIBS_OUTPUT/armeabi-v7a/. $TMP_DIR_MAGISK/system/lib
[[ -d $LIBS_OUTPUT/x86_64 ]] && cp -a $LIBS_OUTPUT/x86_64/. $TMP_DIR_MAGISK/system_x86/lib64
[[ -d $LIBS_OUTPUT/x86 ]] && cp -a $LIBS_OUTPUT/x86/. $TMP_DIR_MAGISK/system_x86/lib

# run custom script
if [ -f $MODULE_NAME/build-module.sh ]; then
  source $MODULE_NAME/build-module.sh
  copy_files
fi

# zip
mkdir -p $MODULE_NAME/release
ZIP_NAME=magisk-$ZIP_NAME_PREFIX-"$VERSION".zip
rm -f $MODULE_NAME/release/$ZIP_NAME
rm -f $TMP_DIR_MAGISK/$ZIP_NAME
(cd $TMP_DIR_MAGISK; zip -r $ZIP_NAME * > /dev/null)
mv $TMP_DIR_MAGISK/$ZIP_NAME $MODULE_NAME/release/$ZIP_NAME

# clean tmp dir
rm -rf $TMP_DIR

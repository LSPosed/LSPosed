#include <jni.h>
#include <sys/types.h>

#ifndef CONFIG_H
#define CONFIG_H

//#define LOG_DISABLED
//#define DEBUG

#define INJECT_DEX_PATH \
"/system/framework/edxp.jar:/system/framework/eddalvikdx.jar:/system/framework/eddexmaker.jar"

#define ENTRY_CLASS_NAME "com.elderdrivers.riru.edxp.Main"

#define CLASS_SAND_HOOK "com.swift.sandhook.SandHook"

#define CLASS_NEVER_CALL "com.swift.sandhook.ClassNeverCall"

#endif //CONFIG_H
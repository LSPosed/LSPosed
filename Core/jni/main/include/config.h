#include <jni.h>
#include <sys/types.h>

#ifndef CONFIG_H
#define CONFIG_H

//#define LOG_DISABLED
//#define DEBUG

#ifdef DEBUG
#define INJECT_DEX_PATH \
"/data/local/tmp/edxposed.dex:/data/local/tmp/eddalvikdx.dex:/data/local/tmp/eddexmaker.dex"
#else
#define INJECT_DEX_PATH \
"/system/framework/edxposed.dex:/system/framework/eddalvikdx.dex:/system/framework/eddexmaker.dex"
#endif

#define ENTRY_CLASS_NAME "com.elderdrivers.riru.xposed.Main"

#endif //CONFIG_H
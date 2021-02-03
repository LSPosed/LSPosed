//
// Created by kotori on 2/4/21.
//

#include "native_api.h"
#include "dobby.h"

/*
 * Module: define xposed_native file in /assets, each line is a .so file name
 * LSP: Hook _loader_dlopen, if any .so file matches the name above, try to call
 *      "native_init(void*)" function in target so with function pointer of "init" below.
 * Module: Call init function.
 * LSP: Return LsposedNativeAPIEntries struct.
 * Module: Since JNI is not yet available at that time, module can store the struct to somewhere else,
 *      and handle them in JNI_Onload or later.
 * Module: Do some MAGIC provided by LSPosed framework.
 */

LsposedNativeAPIEntriesV1 init() {
    LsposedNativeAPIEntriesV1 ret{
            .version = 1,
            .inlineHookFunc = DobbyHook
    };
    return ret;
}
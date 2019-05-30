
#include <jni.h>
#include <native_util.h>
#include <nativehelper/jni_macros.h>
#include <framework/fd_utils-inl.h>
#include "framework_zygote.h"

namespace edxp {

    static FileDescriptorTable *gClosedFdTable = nullptr;

    static void Zygote_closeFilesBeforeFork(JNI_START) {
        // FIXME what if gClosedFdTable is not null
        gClosedFdTable = FileDescriptorTable::Create();
    }

    static void Zygote_reopenFilesAfterFork(JNI_START) {
        if (!gClosedFdTable) {
            LOGE("gClosedFdTable is null when reopening files");
            return;
        }
        gClosedFdTable->Reopen();
        delete gClosedFdTable;
        gClosedFdTable = nullptr;
    }

    static JNINativeMethod gMethods[] = {
            NATIVE_METHOD(Zygote, closeFilesBeforeFork, "()V"),
            NATIVE_METHOD(Zygote, reopenFilesAfterFork, "()V")
    };

    void RegisterFrameworkZygote(JNIEnv *env) {
        REGISTER_EDXP_NATIVE_METHODS("com.elderdrivers.riru.edxp.framework.Zygote");
    }

}
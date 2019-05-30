package com.elderdrivers.riru.edxp.framework;

public class Zygote {

    // prevent from fatal error caused by holding not whitelisted file descriptors when forking zygote
    // https://github.com/rovo89/Xposed/commit/b3ba245ad04cd485699fb1d2ebde7117e58214ff
    public static native void closeFilesBeforeFork();

    public static native void reopenFilesAfterFork();

}

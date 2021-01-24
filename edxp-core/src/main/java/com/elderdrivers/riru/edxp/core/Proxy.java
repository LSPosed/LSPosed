package com.elderdrivers.riru.edxp.core;

import com.elderdrivers.riru.common.KeepAll;

public interface Proxy extends KeepAll {

    boolean init();

    void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                              int[][] rlimits, int mountExternal, String seInfo,
                              String niceName, int[] fdsToClose, int[] fdsToIgnore,
                              boolean startChildZygote, String instructionSet,
                              String appDataDir);

    void forkAndSpecializePost(int pid, String appDataDir, String niceName);

    void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                             long permittedCapabilities, long effectiveCapabilities);

    void forkSystemServerPost(int pid);
}

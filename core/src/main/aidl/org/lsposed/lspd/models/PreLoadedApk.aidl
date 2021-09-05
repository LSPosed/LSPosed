package org.lsposed.lspd.models;

parcelable PreLoadedApk {
    String hostApk;
    List<SharedMemory> preLoadedDexes;
    List<String> moduleClassNames;
    List<String> moduleLibraryNames;
}

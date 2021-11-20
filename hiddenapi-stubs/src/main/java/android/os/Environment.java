package android.os;

import java.io.File;

public class Environment {
    public static File getDataProfilesDePackageDirectory(int userId, String packageName) {
        throw new IllegalArgumentException("STUB");
    }
    public static File getDataUserDePackageDirectory(String volumeUuid, int userId,
                                                     String packageName) {
        throw new IllegalArgumentException("STUB");
    }
}

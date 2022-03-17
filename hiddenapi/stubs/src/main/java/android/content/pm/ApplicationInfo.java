package android.content.pm;

import androidx.annotation.RequiresApi;

public class ApplicationInfo {
    public String credentialProtectedDataDir;

    public String[] resourceDirs;

    @RequiresApi(31)
    public String[] overlayPaths;
}

package android.os;

public class Build {
    public static class VERSION {
        public final static int SDK_INT = SystemProperties.getInt(
                "ro.build.version.sdk", 0);
    }
    public static class VERSION_CODES {
        public static final int O_MR1 = 27;
        public static final int P = 28;
        public static final int Q = 29;
        public static final int R = 30;
        public static final int S = 31;
    }
}

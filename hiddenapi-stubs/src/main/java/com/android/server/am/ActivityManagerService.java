package com.android.server.am;

import com.android.server.SystemService;

public class ActivityManagerService {
    public static final class Lifecycle extends SystemService {
        public ActivityManagerService getService() {
            throw new UnsupportedOperationException("STUB");
        }
        private ProcessRecord findProcessLocked(String process, int userId, String callName) {
            throw new UnsupportedOperationException("STUB");
        }
    }
}

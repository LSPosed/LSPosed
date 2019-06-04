package com.elderdrivers.riru.edxp.util;

import de.robv.android.xposed.XposedHelpers;

public class ClassUtils {

    public enum ClassStatus {
        kNotReady(0), // Zero-initialized Class object starts in this state.
        kRetired(1),  // Retired, should not be used. Use the newly cloned one instead.
        kErrorResolved(2),
        kErrorUnresolved(3),
        kIdx(4),  // Loaded, DEX idx in super_class_type_idx_ and interfaces_type_idx_.
        kLoaded(5),  // DEX idx values resolved.
        kResolving(6),  // Just cloned from temporary class object.
        kResolved(7),  // Part of linking.
        kVerifying(8),  // In the process of being verified.
        kRetryVerificationAtRuntime(9),  // Compile time verification failed, retry at runtime.
        kVerifyingAtRuntime(10),  // Retrying verification at runtime.
        kVerified(11),  // Logically part of linking; done pre-init.
        kSuperclassValidated(12),  // Superclass validation part of init done.
        kInitializing(13),  // Class init in progress.
        kInitialized(14);  // Ready to go.

        private final int status;

        ClassStatus(int status) {
            this.status = status;
        }

        static ClassStatus withValue(int value) {
            for (ClassStatus status : ClassStatus.values()) {
                if (status.status == value) {
                    return status;
                }
            }
            return kNotReady;
        }
    }

    /**
     * enum class ClassStatus : uint8_t {
     * kNotReady = 0,  // Zero-initialized Class object starts in this state.
     * kRetired = 1,  // Retired, should not be used. Use the newly cloned one instead.
     * kErrorResolved = 2,
     * kErrorUnresolved = 3,
     * kIdx = 4,  // Loaded, DEX idx in super_class_type_idx_ and interfaces_type_idx_.
     * kLoaded = 5,  // DEX idx values resolved.
     * kResolving = 6,  // Just cloned from temporary class object.
     * kResolved = 7,  // Part of linking.
     * kVerifying = 8,  // In the process of being verified.
     * kRetryVerificationAtRuntime = 9,  // Compile time verification failed, retry at runtime.
     * kVerifyingAtRuntime = 10,  // Retrying verification at runtime.
     * kVerified = 11,  // Logically part of linking; done pre-init.
     * kSuperclassValidated = 12,  // Superclass validation part of init done.
     * kInitializing = 13,  // Class init in progress.
     * kInitialized = 14,  // Ready to go.
     * kLast = kInitialized
     * };
     */
    public static ClassStatus getClassStatus(Class clazz) {
        if (clazz == null) {
            return ClassStatus.kNotReady;
        }
        int status = XposedHelpers.getIntField(clazz, "status");
        return ClassStatus.withValue((int) (Integer.toUnsignedLong(status) >> (32 - 4)));
    }


    public static boolean isInitialized(Class clazz) {
        return getClassStatus(clazz) == ClassStatus.kInitialized;
    }

}

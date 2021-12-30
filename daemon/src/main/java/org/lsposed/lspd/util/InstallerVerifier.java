package org.lsposed.lspd.util;

import com.android.apksig.ApkVerifier;

import java.io.File;
import java.util.Arrays;

import static org.lsposed.lspd.util.SignInfo.CERTIFICATE;

public class InstallerVerifier {

    public static boolean verifyInstallerSignature(String path) {
        ApkVerifier verifier = new ApkVerifier.Builder(new File(path))
                .setMinCheckedPlatformVersion(27)
                .build();
        try {
            ApkVerifier.Result result = verifier.verify();
            if (!result.isVerified()) {
                return false;
            }
            boolean ret = Arrays.equals(result.getSignerCertificates().get(0).getEncoded(), CERTIFICATE);
            Utils.logI("verifyInstallerSignature: " + ret);
            return ret;
        } catch (Throwable t) {
            Utils.logE("verifyInstallerSignature: ", t);
            return false;
        }
    }
}

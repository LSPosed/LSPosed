package org.lsposed.lspd.util;

import static org.lsposed.lspd.util.SignInfo.CERTIFICATE;

import com.android.apksig.ApkVerifier;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class InstallerVerifier {

    public static void verifyInstallerSignature(String path) throws IOException {
        ApkVerifier verifier = new ApkVerifier.Builder(new File(path))
                .setMinCheckedPlatformVersion(27)
                .build();
        try {
            ApkVerifier.Result result = verifier.verify();
            if (!result.isVerified()) {
                throw new IOException("apk signature not verified");
            }
            var mainCert = result.getSignerCertificates().get(0);
            if (!Arrays.equals(mainCert.getEncoded(), CERTIFICATE)) {
                var dname = mainCert.getSubjectX500Principal().getName();
                throw new IOException("apk signature mismatch: " + dname);
            }
        } catch (Exception t) {
            throw new IOException(t);
        }
    }
}

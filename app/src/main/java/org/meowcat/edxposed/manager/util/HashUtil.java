package org.meowcat.edxposed.manager.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    private static String hash(String input, @SuppressWarnings("SameParameterValue") String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] messageDigest = md.digest(input.getBytes());
            return toHexString(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static String md5(String input) {
        return hash(input, "MD5");
    }

//    public static String sha1(String input) {
//        return hash(input, "SHA-1");
//    }

    private static String hash(File file, @SuppressWarnings("SameParameterValue") String algorithm) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            InputStream is = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
            is.close();
            byte[] messageDigest = md.digest();
            return toHexString(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String md5(File input) throws IOException {
        return hash(input, "MD5");
    }

//    public static String sha1(File input) throws IOException {
//        return hash(input, "SHA-1");
//    }

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int unsignedB = b & 0xff;
            if (unsignedB < 0x10)
                sb.append("0");
            sb.append(Integer.toHexString(unsignedB));
        }
        return sb.toString();
    }
}
package android.os;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

@SuppressWarnings("ALL")
public class FileUtils {
    public static final int S_IRWXU = 448;
    public static final int S_IRUSR = 256;
    public static final int S_IWUSR = 128;
    public static final int S_IXUSR = 64;
    public static final int S_IRWXG = 56;
    public static final int S_IRGRP = 32;
    public static final int S_IWGRP = 16;
    public static final int S_IXGRP = 8;
    public static final int S_IRWXO = 7;
    public static final int S_IROTH = 4;
    public static final int S_IWOTH = 2;
    public static final int S_IXOTH = 1;
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("[\\w%+,./=_-]+");

    public static native int setPermissions(String paramString, int paramInt1, int paramInt2, int paramInt3);

    public static native int getFatVolumeId(String paramString);

    public static boolean sync(FileOutputStream stream) {
        try {
            if (stream != null)
                stream.getFD().sync();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean copyFile(File srcFile, File destFile) {
        boolean result = false;
        try {
            InputStream in = new FileInputStream(srcFile);
            try {
                result = copyToFile(in, destFile);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists())
                destFile.delete();
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0)
                    out.write(buffer, 0, bytesRead);
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isFilenameSafe(File file) {
        return SAFE_FILENAME_PATTERN.matcher(file.getPath()).matches();
    }

    public static String readTextFile(File file, int max, String ellipsis) throws IOException {
        InputStream input = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(input);
        try {
            long size = file.length();
            if (max > 0 || (size > 0L && max == 0)) {
                if (size > 0L && (max == 0 || size < max))
                    max = (int) size;
                byte[] arrayOfByte = new byte[max + 1];
                int length = bis.read(arrayOfByte);
                if (length <= 0)
                    return "";
                if (length <= max)
                    return new String(arrayOfByte, 0, length);
                if (ellipsis == null)
                    return new String(arrayOfByte, 0, max);
                return new String(arrayOfByte, 0, max) + ellipsis;
            }
            if (max < 0) {
                int len;
                boolean rolled = false;
                byte[] last = null, arrayOfByte1 = null;
                do {
                    if (last != null)
                        rolled = true;
                    byte[] tmp = last;
                    last = arrayOfByte1;
                    arrayOfByte1 = tmp;
                    if (arrayOfByte1 == null)
                        arrayOfByte1 = new byte[-max];
                    len = bis.read(arrayOfByte1);
                } while (len == arrayOfByte1.length);
                if (last == null && len <= 0)
                    return "";
                if (last == null)
                    return new String(arrayOfByte1, 0, len);
                if (len > 0) {
                    rolled = true;
                    System.arraycopy(last, len, last, 0, last.length - len);
                    System.arraycopy(arrayOfByte1, 0, last, last.length - len, len);
                }
                if (ellipsis == null || !rolled)
                    return new String(last);
                return ellipsis + new String(last);
            }
            ByteArrayOutputStream contents = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            while (true) {
                int len = bis.read(data);
                if (len > 0)
                    contents.write(data, 0, len);
                if (len != data.length)
                    return contents.toString();
            }
        } finally {
            bis.close();
            input.close();
        }
    }

    public static void stringToFile(String filename, String string) throws IOException {
        FileWriter out = new FileWriter(filename);
        try {
            out.write(string);
        } finally {
            out.close();
        }
    }

    public static long checksumCrc32(File file) throws FileNotFoundException, IOException {
        CRC32 checkSummer = new CRC32();
        CheckedInputStream cis = null;
        try {
            cis = new CheckedInputStream(new FileInputStream(file), checkSummer);
            byte[] buf = new byte[128];
            while (cis.read(buf) >= 0) ;
            return checkSummer.getValue();
        } finally {
            if (cis != null)
                try {
                    cis.close();
                } catch (IOException e) {
                }
        }
    }
}

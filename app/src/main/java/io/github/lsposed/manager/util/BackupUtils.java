package io.github.lsposed.manager.util;

import android.content.Context;
import android.net.Uri;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.github.lsposed.manager.Constants;

public class BackupUtils {
    public static boolean backup(Context context, Uri uri) {
        try {
            Path confPath = Paths.get(Constants.getConfDir());
            Object[] files = Files.list(confPath).toArray();

            try (ZipOutputStream out = new ZipOutputStream(context.getContentResolver().openOutputStream(uri))) {
                for (Object object : files) {
                    Path file = (Path) object;
                    if (!Files.isRegularFile(file)) {
                        continue;
                    }
                    String fileName = file.getFileName().toString();
                    if ((!fileName.endsWith(".list") && !fileName.endsWith(".conf")) || fileName.equals("modules.list")) {
                        continue;
                    }
                    ZipEntry entry = new ZipEntry(fileName);
                    out.putNextEntry(entry);
                    try {
                        Files.copy(file, out);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    out.closeEntry();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean restore(Context context, Uri uri) {
        try (ZipInputStream in = new ZipInputStream(context.getContentResolver().openInputStream(uri))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String fileName = entry.getName();
                if ((!fileName.endsWith(".list") && !fileName.endsWith(".conf")) || fileName.equals("modules.list")) {
                    in.closeEntry();
                    continue;
                }
                Path path = Paths.get(Constants.getConfDir() + "/" + fileName);
                try {
                    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                in.closeEntry();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

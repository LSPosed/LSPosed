/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.lspd.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;

public class MetaDataReader {
    private final HashMap<String, Object> metaData = new HashMap<>();

    public static Map<String, Object> getMetaData(File apk) throws IOException {
        return new MetaDataReader(apk).metaData;
    }

    private MetaDataReader(File apk) throws IOException {
        try (JarFile zip = new JarFile(apk);
             var is = zip.getInputStream(zip.getEntry("AndroidManifest.xml"))) {
            var reader = new AxmlReader(getBytesFromInputStream(is));
            reader.accept(new AxmlVisitor() {
                @Override
                public NodeVisitor child(String ns, String name) {
                    NodeVisitor child = super.child(ns, name);
                    return new ManifestTagVisitor(child);
                }
            });
        }
    }

    public static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] b = new byte[1024];
            int n;
            while ((n = inputStream.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            return bos.toByteArray();
        }
    }

    private class ManifestTagVisitor extends NodeVisitor {
        public ManifestTagVisitor(NodeVisitor child) {
            super(child);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            NodeVisitor child = super.child(ns, name);
            if ("application".equals(name)) {
                return new ApplicationTagVisitor(child);
            }
            return child;
        }

        private class ApplicationTagVisitor extends NodeVisitor {
            public ApplicationTagVisitor(NodeVisitor child) {
                super(child);
            }

            @Override
            public NodeVisitor child(String ns, String name) {
                NodeVisitor child = super.child(ns, name);
                if ("meta-data".equals(name)) {
                    return new MetaDataVisitor(child);
                }
                return child;
            }
        }
    }

    private class MetaDataVisitor extends NodeVisitor {
        public String name = null;
        public Object value = null;

        public MetaDataVisitor(NodeVisitor child) {
            super(child);
        }

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object obj) {
            if (type == 3 && "name".equals(name)) {
                this.name = (String) obj;
            }
            if ("value".equals(name)) {
                value = obj;
            }
            super.attr(ns, name, resourceId, type, obj);
        }

        @Override
        public void end() {
            if (name != null && value != null) {
                metaData.put(name, value);
            }
            super.end();
        }
    }

    public static int extractIntPart(String str) {
        int result = 0, length = str.length();
        for (int offset = 0; offset < length; offset++) {
            char c = str.charAt(offset);
            if ('0' <= c && c <= '9')
                result = result * 10 + (c - '0');
            else
                break;
        }
        return result;
    }
}

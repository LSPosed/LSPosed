package org.lsposed.lspd.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import sun.net.www.ParseUtil;
import sun.net.www.protocol.jar.Handler;

final class ClassPathURLStreamHandler extends Handler {
    private final String fileUri;
    private final JarFile jarFile;

    ClassPathURLStreamHandler(String jarFileName) throws IOException {
        jarFile = new JarFile(jarFileName);
        fileUri = new File(jarFileName).toURI().toString();
    }

    URL getEntryUrlOrNull(String entryName) {
        if (jarFile.getEntry(entryName) != null) {
            try {
                String encodedName = ParseUtil.encodePath(entryName, false);
                return new URL("jar", null, -1, fileUri + "!/" + encodedName, this);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid entry name", e);
            }
        }
        return null;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new ClassPathURLConnection(url);
    }

    @Override
    protected void finalize() throws IOException {
        jarFile.close();
    }

    private final class ClassPathURLConnection extends JarURLConnection {
        private JarFile connectionJarFile = null;
        private ZipEntry jarEntry = null;
        private InputStream jarInput = null;
        private boolean closed = false;

        private ClassPathURLConnection(URL url) throws MalformedURLException {
            super(url);
            setUseCaches(false);
        }

        @Override
        public void setUseCaches(boolean usecaches) {
            super.setUseCaches(false);
        }

        @Override
        public void connect() throws IOException {
            if (closed) {
                throw new IllegalStateException("JarURLConnection has been closed");
            }
            if (!connected) {
                jarEntry = jarFile.getEntry(getEntryName());
                if (jarEntry == null) {
                    throw new FileNotFoundException("URL=" + url + ", zipfile=" + jarFile.getName());
                }
                connected = true;
            }
        }

        @Override
        public JarFile getJarFile() throws IOException {
            connect();
            if (connectionJarFile != null) return connectionJarFile;
            return connectionJarFile = new JarFile(jarFile.getName());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            if (jarInput != null) return jarInput;
            return jarInput = new FilterInputStream(jarFile.getInputStream(jarEntry)) {
                @Override
                public void close() throws IOException {
                    super.close();
                    closed = true;
                    jarFile.close();
                    if (connectionJarFile != null) connectionJarFile.close();
                }
            };
        }

        @Override
        public String getContentType() {
            String cType = guessContentTypeFromName(getEntryName());
            if (cType == null) {
                cType = "content/unknown";
            }
            return cType;
        }

        @Override
        public int getContentLength() {
            try {
                connect();
                return (int) getJarEntry().getSize();
            } catch (IOException ignored) {
            }
            return -1;
        }
    }
}

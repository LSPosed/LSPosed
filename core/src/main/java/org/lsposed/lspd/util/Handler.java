package org.lsposed.lspd.util;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class Handler extends java.net.URLStreamHandler {

    private static final String separator = "!/";

    private static int indexOfBangSlash(String spec) {
        int indexOfBang = spec.length();
        while ((indexOfBang = spec.lastIndexOf('!', indexOfBang)) != -1) {
            if ((indexOfBang != (spec.length() - 1)) &&
                    (spec.charAt(indexOfBang + 1) == '/')) {
                return indexOfBang + 1;
            } else {
                indexOfBang--;
            }
        }
        return -1;
    }

    @Override
    protected boolean sameFile(URL u1, URL u2) {
        if (!u1.getProtocol().equals("jar") || !u2.getProtocol().equals("jar"))
            return false;

        String file1 = u1.getFile();
        String file2 = u2.getFile();
        int sep1 = file1.indexOf(separator);
        int sep2 = file2.indexOf(separator);

        if (sep1 == -1 || sep2 == -1) {
            return super.sameFile(u1, u2);
        }

        String entry1 = file1.substring(sep1 + 2);
        String entry2 = file2.substring(sep2 + 2);

        if (!entry1.equals(entry2))
            return false;

        URL enclosedURL1, enclosedURL2;
        try {
            enclosedURL1 = new URL(file1.substring(0, sep1));
            enclosedURL2 = new URL(file2.substring(0, sep2));
        } catch (MalformedURLException unused) {
            return super.sameFile(u1, u2);
        }

        return super.sameFile(enclosedURL1, enclosedURL2);
    }

    @Override
    protected int hashCode(URL u) {
        int h = 0;

        String protocol = u.getProtocol();
        if (protocol != null)
            h += protocol.hashCode();

        String file = u.getFile();
        int sep = file.indexOf(separator);

        if (sep == -1)
            return h + file.hashCode();

        URL enclosedURL;
        String fileWithoutEntry = file.substring(0, sep);
        try {
            enclosedURL = new URL(fileWithoutEntry);
            h += enclosedURL.hashCode();
        } catch (MalformedURLException unused) {
            h += fileWithoutEntry.hashCode();
        }

        String entry = file.substring(sep + 2);
        h += entry.hashCode();

        return h;
    }


    @Override
    @SuppressWarnings("deprecation")
    protected void parseURL(URL url, String spec, int start, int limit) {
        String file = null;
        String ref = null;
        // first figure out if there is an anchor
        int refPos = spec.indexOf('#', limit);
        boolean refOnly = refPos == start;
        if (refPos > -1) {
            ref = spec.substring(refPos + 1);
            if (refOnly) {
                file = url.getFile();
            }
        }
        // then figure out if the spec is
        // 1. absolute (jar:)
        // 2. relative (i.e. url + foo/bar/baz.ext)
        // 3. anchor-only (i.e. url + #foo), which we already did (refOnly)
        boolean absoluteSpec = false;
        if (spec.length() >= 4) {
            absoluteSpec = spec.substring(0, 4).equalsIgnoreCase("jar:");
        }
        spec = spec.substring(start, limit);

        if (absoluteSpec) {
            file = parseAbsoluteSpec(spec);
        } else if (!refOnly) {
            file = parseContextSpec(url, spec);

            // Canonize the result after the bangslash
            int bangSlash = indexOfBangSlash(file);
            String toBangSlash = file.substring(0, bangSlash);
            String afterBangSlash = file.substring(bangSlash);
            afterBangSlash = canonizeString(afterBangSlash);
            file = toBangSlash + afterBangSlash;
        }
        setURL(url, "jar", "", -1, file, ref);
    }

    private String canonizeString(String file) {
        int i;
        int lim;

        // Remove embedded /../
        while ((i = file.indexOf("/../")) >= 0) {
            if ((lim = file.lastIndexOf('/', i - 1)) >= 0) {
                file = file.substring(0, lim) + file.substring(i + 3);
            } else {
                file = file.substring(i + 3);
            }
        }
        // Remove embedded /./
        while ((i = file.indexOf("/./")) >= 0) {
            file = file.substring(0, i) + file.substring(i + 2);
        }
        // Remove trailing ..
        while (file.endsWith("/..")) {
            i = file.indexOf("/..");
            if ((lim = file.lastIndexOf('/', i - 1)) >= 0) {
                file = file.substring(0, lim + 1);
            } else {
                file = file.substring(0, i);
            }
        }
        // Remove trailing .
        if (file.endsWith("/."))
            file = file.substring(0, file.length() - 1);

        return file;
    }

    private String parseAbsoluteSpec(String spec) {
        int index;
        // check for !/
        if ((index = indexOfBangSlash(spec)) == -1) {
            throw new NullPointerException("no !/ in spec");
        }
        // test the inner URL
        try {
            String innerSpec = spec.substring(0, index - 1);
            new URL(innerSpec);
        } catch (MalformedURLException e) {
            throw new NullPointerException("invalid url: " +
                    spec + " (" + e + ")");
        }
        return spec;
    }

    private String parseContextSpec(URL url, String spec) {
        String ctxFile = url.getFile();
        // if the spec begins with /, chop up the jar back !/
        if (spec.startsWith("/")) {
            int bangSlash = indexOfBangSlash(ctxFile);
            if (bangSlash == -1) {
                throw new NullPointerException("malformed " + "context url:" + url + ": no !/");
            }
            ctxFile = ctxFile.substring(0, bangSlash);
        }
        if (!ctxFile.endsWith("/") && (!spec.startsWith("/"))) {
            // chop up the last component
            int lastSlash = ctxFile.lastIndexOf('/');
            if (lastSlash == -1) {
                throw new NullPointerException("malformed " + "context url:" + url);
            }
            ctxFile = ctxFile.substring(0, lastSlash + 1);
        }
        return (ctxFile + spec);
    }
}

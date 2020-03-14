package org.meowcat.edxposed.manager.util.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class JSONUtils {

    public static final String JSON_LINK = "http://edxp.meowcat.org/assets/version.json";

    public static String getFileContent(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setInstanceFollowRedirects(false);
        c.setDoOutput(false);
        c.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

        return sb.toString();
    }

    public static class XposedJson {
        public List<XposedTab> tabs;
        public ApkRelease apk;
    }

    public static class ApkRelease {
        public String version;
        public String changelog;
        public String link;
    }

}
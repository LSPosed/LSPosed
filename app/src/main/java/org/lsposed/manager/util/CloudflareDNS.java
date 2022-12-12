package org.lsposed.manager.util;

import android.os.Build;

import androidx.annotation.NonNull;

import org.lsposed.manager.App;

import java.net.InetAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.UnknownHostException;
import java.util.List;

import okhttp3.ConnectionSpec;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;
import okhttp3.internal.platform.Platform;

public final class CloudflareDNS implements Dns {

    private static final HttpUrl url = HttpUrl.get("https://cloudflare-dns.com/dns-query");
    public boolean DoH = App.getPreferences().getBoolean("doh", false);
    public boolean noProxy = ProxySelector.getDefault().select(url.uri()).get(0) == Proxy.NO_PROXY;
    private final Dns cloudflare;

    public CloudflareDNS() {
        var trustManager = Platform.get().platformTrustManager();
        var tls = ConnectionSpec.RESTRICTED_TLS;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //noinspection deprecation
            tls = new ConnectionSpec.Builder(tls)
                    .supportsTlsExtensions(false)
                    .build();
        }
        var builder = new DnsOverHttps.Builder()
                .resolvePrivateAddresses(true)
                .url(HttpUrl.get("https://cloudflare-dns.com/dns-query"))
                .client(new OkHttpClient.Builder()
                        .cache(App.getOkHttpCache())
                        .sslSocketFactory(new NoSniFactory(), trustManager)
                        .connectionSpecs(List.of(tls))
                        .build());
        try {
            builder.bootstrapDnsHosts(List.of(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    InetAddress.getByName("2606:4700:4700::1111"),
                    InetAddress.getByName("2606:4700:4700::1001")));
        } catch (UnknownHostException ignored) {
        }
        cloudflare = builder.build();
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        if (DoH && noProxy) {
            return cloudflare.lookup(hostname);
        } else {
            return SYSTEM.lookup(hostname);
        }
    }
}

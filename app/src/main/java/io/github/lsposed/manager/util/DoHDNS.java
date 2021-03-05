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

package io.github.lsposed.manager.util;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

import io.github.lsposed.manager.App;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class DoHDNS implements Dns {

    private static DnsOverHttps cloudflare;
    private static DnsOverHttps alidns;

    public DoHDNS(OkHttpClient client) {
        cloudflare = new DnsOverHttps.Builder()
                .resolvePrivateAddresses(true)
                .client(client)
                .url(HttpUrl.get("https://cloudflare-dns.com/dns-query"))
                .build();

        alidns = new DnsOverHttps.Builder()
                .resolvePrivateAddresses(true)
                .client(client)
                .url(HttpUrl.get("https://dns.alidns.com/dns-query"))
                .build();
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        if (App.getPreferences().getBoolean("doh", false)) {
            List<InetAddress> inetAddresses = cloudflare.lookup(hostname);
            if (inetAddresses.size() > 0) {
                return inetAddresses;
            } else if ("CN".equals(Locale.getDefault().getCountry())) {
                inetAddresses = alidns.lookup(hostname);
                if (inetAddresses.size() > 0) {
                    return inetAddresses;
                }
            }
        }
        return SYSTEM.lookup(hostname);
    }
}

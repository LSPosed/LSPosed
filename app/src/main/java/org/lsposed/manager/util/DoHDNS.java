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

package org.lsposed.manager.util;

import androidx.annotation.NonNull;

import org.lsposed.manager.App;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class DoHDNS implements Dns {

    private static DnsOverHttps cloudflare;
    private static DnsOverHttps tuna;
    private static DnsOverHttps dnspod;

    public DoHDNS(OkHttpClient client) {
        var builder = new DnsOverHttps.Builder().resolvePrivateAddresses(true).client(client);
        cloudflare = builder.url(HttpUrl.get("https://cloudflare-dns.com/dns-query")).build();
        tuna = builder.url(HttpUrl.get("https://101.6.6.6:8443/dns-query")).build();
        dnspod = builder.url(HttpUrl.get("https://doh.pub/dns-query")).build();
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        if (App.getPreferences().getBoolean("doh", false)) {
            try {
                return cloudflare.lookup(hostname);
            } catch (UnknownHostException e) {
                try {
                    if ("CN".equals(Locale.getDefault().getCountry()))
                        return tuna.lookup(hostname);
                } catch (UnknownHostException exception) {
                    try {
                        return dnspod.lookup(hostname);
                    } catch (UnknownHostException ignored) {
                    }
                }
            }
        }
        return SYSTEM.lookup(hostname);
    }
}

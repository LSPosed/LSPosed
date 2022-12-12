package org.lsposed.manager.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

public final class NoSniFactory extends SSLSocketFactory {
    private static final SSLSocketFactory defaultFactory = (SSLSocketFactory) getDefault();
    @SuppressWarnings("deprecation")
    private static final android.net.SSLCertificateSocketFactory openSSLSocket =
            (android.net.SSLCertificateSocketFactory) android.net.SSLCertificateSocketFactory
                    .getDefault(1000);

    @Override
    public String[] getDefaultCipherSuites() {
        return defaultFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return defaultFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return config(defaultFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return config(defaultFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return config(defaultFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return config(defaultFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return config(defaultFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket config(Socket socket) {
        try {
            openSSLSocket.setHostname(socket, null);
            openSSLSocket.setUseSessionTickets(socket, true);
        } catch (IllegalArgumentException ignored) {
        }
        return socket;
    }
}

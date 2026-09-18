package ru.white.proxymod.proxy;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ProxyManager {

    private static final ProxyConfig config = ProxyConfig.load();

    public static ProxyConfig getConfig() {
        return config;
    }

    public static boolean isProxyActive() {
        return config.isEnabled()
                && config.getHost() != null
                && !config.getHost().trim().isEmpty()
                && config.getPort() > 0
                && config.getPort() <= 65535;
    }

    public static InetSocketAddress getProxySocketAddress() {
        try {
            InetAddress addr = InetAddress.getByName(config.getHost().trim());
            return new InetSocketAddress(addr, config.getPort());
        } catch (Exception e) {
            return new InetSocketAddress(config.getHost().trim(), config.getPort());
        }
    }

    public record TestResult(boolean success, long pingMs, String error) {}

    public static void testConnection(ProxyConfig testConfig, Consumer<TestResult> callback) {
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.setSoTimeout(4000);
                socket.connect(new InetSocketAddress(testConfig.getHost().trim(), testConfig.getPort()), 3500);

                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                switch (testConfig.getType()) {
                    case SOCKS5 -> {
                        if (testConfig.hasAuth()) {
                            out.write(new byte[]{0x05, 0x02, 0x00, 0x02});
                        } else {
                            out.write(new byte[]{0x05, 0x01, 0x00});
                        }
                        out.flush();

                        byte[] resp = new byte[2];
                        try {
                            new java.io.DataInputStream(in).readFully(resp);
                        } catch (Exception e) {
                            callback.accept(new TestResult(false, 0, "No SOCKS5 response"));
                            return;
                        }
                        if (resp[0] != 0x05) {
                            callback.accept(new TestResult(false, 0, "Invalid SOCKS5 version (" + resp[0] + ")"));
                            return;
                        }
                        if (resp[1] == (byte) 0xFF) {
                            callback.accept(new TestResult(false, 0, "Unsupported authentication"));
                            return;
                        }
                    }
                    case SOCKS4 -> {
                        out.write(new byte[]{0x04, 0x01, 0x00, 0x50, 0x01, 0x01, 0x01, 0x01, 0x00});
                        out.flush();

                        byte[] resp = new byte[8];
                        try {
                            new java.io.DataInputStream(in).readFully(resp);
                        } catch (Exception e) {
                            callback.accept(new TestResult(false, 0, "No SOCKS4 response"));
                            return;
                        }
                        if (resp[0] != 0x00) {
                            callback.accept(new TestResult(false, 0, "Invalid SOCKS4 response"));
                            return;
                        }
                    }
                    case HTTP -> {
                        String req = "CONNECT 1.1.1.1:80 HTTP/1.1\r\nHost: 1.1.1.1:80\r\n";
                        if (testConfig.hasAuth()) {
                            String creds = testConfig.getUsername() + ":" + (testConfig.getPassword() != null ? testConfig.getPassword() : "");
                            req += "Proxy-Authorization: Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)) + "\r\n";
                        }
                        req += "\r\n";
                        out.write(req.getBytes(StandardCharsets.US_ASCII));
                        out.flush();

                        byte[] resp = new byte[128];
                        int read = in.read(resp);
                        String str = new String(resp, 0, Math.max(0, read), StandardCharsets.US_ASCII);
                        if (!str.contains("HTTP/")) {
                            callback.accept(new TestResult(false, 0, "Not an HTTP proxy"));
                            return;
                        }
                    }
                }

                long ping = System.currentTimeMillis() - startTime;
                callback.accept(new TestResult(true, ping, null));
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null || msg.isEmpty()) msg = e.getClass().getSimpleName();
                callback.accept(new TestResult(false, 0, msg));
            }
        });
    }
}

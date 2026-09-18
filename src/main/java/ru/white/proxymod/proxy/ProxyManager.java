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
    private static long currentPing = -1;
    private static boolean testing = false;

    public static ProxyConfig getConfig() {
        return config;
    }

    public static long getCurrentPing() {
        return currentPing;
    }

    public static boolean isTesting() {
        return testing;
    }

    public record RoutingDecision(boolean isDirect, ProxyConfig.ProxyProfile profile) {
        public static RoutingDecision direct() {
            return new RoutingDecision(true, null);
        }
        public static RoutingDecision proxy(ProxyConfig.ProxyProfile profile) {
            return new RoutingDecision(false, profile);
        }
    }

    public static boolean shouldInterceptConnections() {
        if (config.isEnabled() && isProxyActive()) return true;
        for (ProxyConfig.ServerRule r : config.getServerRules()) {
            if (r.isEnabled() && r.getAction() == ProxyConfig.RuleAction.PROFILE) {
                return true;
            }
        }
        return false;
    }

    public static RoutingDecision resolveRouting(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            return fallbackRouting();
        }

        for (ProxyConfig.ServerRule rule : config.getServerRules()) {
            if (rule.matches(host)) {
                if (rule.getAction() == ProxyConfig.RuleAction.DIRECT) {
                    return RoutingDecision.direct();
                } else {
                    String targetProfName = rule.getTargetProfile();
                    for (ProxyConfig.ProxyProfile p : config.getProfiles()) {
                        if (p.getName().equalsIgnoreCase(targetProfName)) {
                            return RoutingDecision.proxy(p);
                        }
                    }
                    return RoutingDecision.proxy(config.getActiveProfile());
                }
            }
        }

        return fallbackRouting();
    }

    private static RoutingDecision fallbackRouting() {
        if (config.isEnabled() && isProfileValid(config.getActiveProfile())) {
            return RoutingDecision.proxy(config.getActiveProfile());
        }
        return RoutingDecision.direct();
    }

    public static boolean isProfileValid(ProxyConfig.ProxyProfile profile) {
        return profile != null
                && profile.getHost() != null
                && !profile.getHost().trim().isEmpty()
                && profile.getPort() > 0
                && profile.getPort() <= 65535;
    }

    public static boolean isDnsLeakProtected(String host, int port) {
        if (!config.isDnsLeakProtection()) return false;
        RoutingDecision decision = resolveRouting(host, port);
        return !decision.isDirect();
    }

    public static boolean isProxyActive() {
        return config.isEnabled() && isProfileValid(config.getActiveProfile());
    }

    public static InetSocketAddress getProxySocketAddress(ProxyConfig.ProxyProfile profile) {
        if (profile == null) {
            profile = config.getActiveProfile();
        }
        try {
            InetAddress addr = InetAddress.getByName(profile.getHost().trim());
            return new InetSocketAddress(addr, profile.getPort());
        } catch (Exception e) {
            return new InetSocketAddress(profile.getHost().trim(), profile.getPort());
        }
    }

    public static InetSocketAddress getProxySocketAddress() {
        return getProxySocketAddress(config.getActiveProfile());
    }

    public record TestResult(boolean success, long pingMs, String error) {}

    public static void testProfile(ProxyConfig.ProxyProfile profile, Consumer<TestResult> callback) {
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.setSoTimeout(3500);
                socket.connect(new InetSocketAddress(profile.getHost().trim(), profile.getPort()), 3000);

                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                switch (profile.getType()) {
                    case SOCKS5 -> {
                        if (profile.hasAuth()) {
                            out.write(new byte[]{0x05, 0x02, 0x00, 0x02});
                        } else {
                            out.write(new byte[]{0x05, 0x01, 0x00});
                        }
                        out.flush();

                        byte[] resp = new byte[2];
                        try {
                            new java.io.DataInputStream(in).readFully(resp);
                        } catch (Exception e) {
                            profile.setLastPingMs(-1);
                            callback.accept(new TestResult(false, 0, "No SOCKS5 response"));
                            return;
                        }
                        if (resp[0] != 0x05) {
                            profile.setLastPingMs(-1);
                            callback.accept(new TestResult(false, 0, "Invalid SOCKS5 version (" + resp[0] + ")"));
                            return;
                        }
                        if (resp[1] == (byte) 0xFF) {
                            profile.setLastPingMs(-1);
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
                            profile.setLastPingMs(-1);
                            callback.accept(new TestResult(false, 0, "No SOCKS4 response"));
                            return;
                        }
                        if (resp[0] != 0x00) {
                            profile.setLastPingMs(-1);
                            callback.accept(new TestResult(false, 0, "Invalid SOCKS4 response"));
                            return;
                        }
                    }
                    case HTTP -> {
                        String req = "CONNECT 1.1.1.1:80 HTTP/1.1\r\nHost: 1.1.1.1:80\r\n";
                        if (profile.hasAuth()) {
                            String creds = profile.getUsername() + ":" + (profile.getPassword() != null ? profile.getPassword() : "");
                            req += "Proxy-Authorization: Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)) + "\r\n";
                        }
                        req += "\r\n";
                        out.write(req.getBytes(StandardCharsets.US_ASCII));
                        out.flush();

                        byte[] resp = new byte[128];
                        int read = in.read(resp);
                        String str = new String(resp, 0, Math.max(0, read), StandardCharsets.US_ASCII);
                        if (!str.contains("HTTP/")) {
                            profile.setLastPingMs(-1);
                            callback.accept(new TestResult(false, 0, "Not an HTTP proxy"));
                            return;
                        }
                    }
                }

                long ping = System.currentTimeMillis() - startTime;
                profile.setLastPingMs(ping);
                callback.accept(new TestResult(true, ping, null));
            } catch (Exception e) {
                profile.setLastPingMs(-1);
                String msg = e.getMessage();
                if (msg == null || msg.isEmpty()) msg = e.getClass().getSimpleName();
                callback.accept(new TestResult(false, 0, msg));
            }
        });
    }

    public static void testConnection(ProxyConfig testConfig, Consumer<TestResult> callback) {
        testing = true;
        testProfile(testConfig.getActiveProfile(), result -> {
            testing = false;
            if (result.success()) {
                currentPing = result.pingMs();
            } else {
                currentPing = -1;
            }
            callback.accept(result);
        });
    }

    public static void tryFailover() {
        if (!config.isAutoFailover() || config.getProfiles().size() <= 1) return;

        int currentIdx = config.getSelectedProfileIndex();
        int nextIdx = (currentIdx + 1) % config.getProfiles().size();
        config.setSelectedProfileIndex(nextIdx);
        config.save();
    }
}

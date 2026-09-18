package ru.white.proxymod.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ProxyConfig {

    public enum Type {
        SOCKS5("SOCKS5"),
        SOCKS4("SOCKS4"),
        HTTP("HTTP");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static class ProxyProfile {
        private String name = "Default";
        private Type type = Type.SOCKS5;
        private String host = "127.0.0.1";
        private int port = 1080;
        private String username = "";
        private String password = "";
        private transient long lastPingMs = -1;

        public ProxyProfile() {}

        public ProxyProfile(String name, Type type, String host, int port, String username, String password) {
            this.name = name;
            this.type = type != null ? type : Type.SOCKS5;
            this.host = host;
            this.port = port;
            this.username = username != null ? username : "";
            this.password = password != null ? password : "";
        }

        public String getName() { return name != null && !name.isEmpty() ? name : (host + ":" + port); }
        public void setName(String name) { this.name = name; }

        public Type getType() { return type != null ? type : Type.SOCKS5; }
        public void setType(Type type) { this.type = type; }

        public String getHost() { return host != null ? host : "127.0.0.1"; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getUsername() { return username != null ? username : ""; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password != null ? password : ""; }
        public void setPassword(String password) { this.password = password; }

        public boolean hasAuth() { return username != null && !username.trim().isEmpty(); }

        public long getLastPingMs() { return lastPingMs; }
        public void setLastPingMs(long lastPingMs) { this.lastPingMs = lastPingMs; }

        public static ProxyProfile parse(String line) {
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty()) return null;

            Type type = Type.SOCKS5;
            if (line.startsWith("socks5://")) {
                type = Type.SOCKS5;
                line = line.substring("socks5://".length());
            } else if (line.startsWith("socks4://")) {
                type = Type.SOCKS4;
                line = line.substring("socks4://".length());
            } else if (line.startsWith("http://")) {
                type = Type.HTTP;
                line = line.substring("http://".length());
            }

            String user = "";
            String pass = "";
            String host = "";
            int port = 1080;

            if (line.contains("@")) {
                String[] parts = line.split("@", 2);
                String authPart = parts[0];
                String addrPart = parts[1];
                if (authPart.contains(":")) {
                    String[] ap = authPart.split(":", 2);
                    user = ap[0];
                    pass = ap[1];
                } else {
                    user = authPart;
                }
                line = addrPart;
            }

            String[] tokens = line.split(":");
            if (tokens.length >= 2) {
                host = tokens[0].trim();
                try {
                    port = Integer.parseInt(tokens[1].trim());
                } catch (Exception ignored) {}

                if (tokens.length >= 4 && user.isEmpty()) {
                    user = tokens[2].trim();
                    pass = tokens[3].trim();
                }
            } else {
                return null;
            }

            String label = host + ":" + port;
            return new ProxyProfile(label, type, host, port, user, pass);
        }
    }

    private boolean enabled = false;
    private Type type = Type.SOCKS5;
    private String host = "127.0.0.1";
    private int port = 1080;
    private String username = "";
    private String password = "";
    private String language = "ru";
    private boolean hudEnabled = true;
    private boolean autoFailover = false;
    private int selectedProfileIndex = 0;
    private List<ProxyProfile> profiles = new ArrayList<>();

    public String getLanguage() {
        return language != null ? language : "ru";
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHudEnabled() {
        return hudEnabled;
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public boolean isAutoFailover() {
        return autoFailover;
    }

    public void setAutoFailover(boolean autoFailover) {
        this.autoFailover = autoFailover;
    }

    public Type getType() {
        ProxyProfile p = getActiveProfile();
        return p != null ? p.getType() : (type != null ? type : Type.SOCKS5);
    }

    public void setType(Type type) {
        this.type = type;
        ProxyProfile p = getActiveProfile();
        if (p != null) p.setType(type);
    }

    public String getHost() {
        ProxyProfile p = getActiveProfile();
        return p != null ? p.getHost() : (host != null ? host : "127.0.0.1");
    }

    public void setHost(String host) {
        this.host = host;
        ProxyProfile p = getActiveProfile();
        if (p != null) p.setHost(host);
    }

    public int getPort() {
        ProxyProfile p = getActiveProfile();
        return p != null ? p.getPort() : port;
    }

    public void setPort(int port) {
        this.port = port;
        ProxyProfile p = getActiveProfile();
        if (p != null) p.setPort(port);
    }

    public String getUsername() {
        ProxyProfile p = getActiveProfile();
        return p != null ? p.getUsername() : (username != null ? username : "");
    }

    public void setUsername(String username) {
        this.username = username;
        ProxyProfile p = getActiveProfile();
        if (p != null) p.setUsername(username);
    }

    public String getPassword() {
        ProxyProfile p = getActiveProfile();
        return p != null ? p.getPassword() : (password != null ? password : "");
    }

    public void setPassword(String password) {
        this.password = password;
        ProxyProfile p = getActiveProfile();
        if (p != null) p.setPassword(password);
    }

    public boolean hasAuth() {
        ProxyProfile p = getActiveProfile();
        return p != null ? p.hasAuth() : (username != null && !username.trim().isEmpty());
    }

    public List<ProxyProfile> getProfiles() {
        if (profiles == null) {
            profiles = new ArrayList<>();
        }
        if (profiles.isEmpty()) {
            profiles.add(new ProxyProfile("Default", type, host, port, username, password));
        }
        return profiles;
    }

    public int getSelectedProfileIndex() {
        if (selectedProfileIndex < 0 || selectedProfileIndex >= getProfiles().size()) {
            selectedProfileIndex = 0;
        }
        return selectedProfileIndex;
    }

    public void setSelectedProfileIndex(int index) {
        if (index >= 0 && index < getProfiles().size()) {
            this.selectedProfileIndex = index;
            ProxyProfile p = getProfiles().get(index);
            this.type = p.getType();
            this.host = p.getHost();
            this.port = p.getPort();
            this.username = p.getUsername();
            this.password = p.getPassword();
        }
    }

    public ProxyProfile getActiveProfile() {
        List<ProxyProfile> list = getProfiles();
        int idx = getSelectedProfileIndex();
        return list.get(idx);
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getConfigFile() {
        File dir = FabricLoader.getInstance().getConfigDir().toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "proxy.json");
    }

    public static ProxyConfig load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                ProxyConfig cfg = GSON.fromJson(reader, ProxyConfig.class);
                if (cfg != null) {
                    cfg.getProfiles(); // ensure default profile initialized
                    return cfg;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ProxyConfig cfg = new ProxyConfig();
        cfg.getProfiles();
        return cfg;
    }

    public void save() {
        File file = getConfigFile();
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

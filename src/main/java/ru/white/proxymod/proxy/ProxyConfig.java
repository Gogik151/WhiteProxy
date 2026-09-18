package ru.white.proxymod.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

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

    private boolean enabled = false;
    private Type type = Type.SOCKS5;
    private String host = "127.0.0.1";
    private int port = 1080;
    private String username = "";
    private String password = "";
    private String language = "ru";

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

    public Type getType() {
        return type != null ? type : Type.SOCKS5;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getHost() {
        return host != null ? host : "127.0.0.1";
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password != null ? password : "";
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasAuth() {
        return username != null && !username.trim().isEmpty();
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
                    return cfg;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ProxyConfig();
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

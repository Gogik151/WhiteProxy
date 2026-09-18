# 🛡️ WhiteProxy

<p align="center">
  <img src="https://raw.githubusercontent.com/Gogik151/WhiteProxy/main/src/main/resources/assets/proxymod/icon.png" alt="WhiteProxy Logo" width="128" height="128" onerror="this.style.display='none'"/>
</p>

<p align="center">
  <strong>Fast, elegant & hardware-accelerated in-game SOCKS4 / SOCKS5 proxy manager for Minecraft Fabric 1.21.11</strong>
</p>

<p align="center">
  <a href="https://modrinth.com/mod/whiteproxy"><img src="https://img.shields.io/badge/Modrinth-WhiteProxy-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth"/></a>
  <a href="https://t.me/whiteproxymod"><img src="https://img.shields.io/badge/Telegram-@whiteproxymod-24A1DE?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram"/></a>
  <a href="https://github.com/Gogik151/WhiteProxy"><img src="https://img.shields.io/badge/GitHub-WhiteProxy-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub"/></a>
  <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Fabric-1.21.11-E4D5B7?style=for-the-badge" alt="Fabric 1.21.11"/></a>
</p>

---

## ✨ Features

- ⚡ **Zero Latency Overhead**: Custom Netty-level proxy routing with optimized pipeline handlers.
- 🔒 **SOCKS4 & SOCKS5 Support**: Seamlessly route game traffic with or without user/password authentication.
- 🎨 **Aesthetic UI**: Custom rounded shaders, smooth transitions, and multi-channel signed distance field (MSDF) Apple SF Pro typography.
- 🌐 **Bilingual (RU / EN)**: Instant language switching directly from the in-game HUD.
- 📋 **Batch Import**: Paste proxy lists (`ip:port` or `ip:port:user:pass`) directly from clipboard.
- 🚀 **Live Ping & Status Testing**: Real-time handshake and latency verification before connecting.
- 🖥️ **HUD Overlay**: Compact top-corner widget displaying current proxy state, IP, and real-time latency.

---

## 🎮 How to Use

1. Press your configured hotkey (default: **`Right Shift`** or open the proxy button on the Multiplayer screen).
2. Click **Add Proxy** or **Import from Clipboard**.
3. Select your proxy and click **Connect**.
4. Join any Minecraft server — your connection will securely route through the chosen proxy!

---

## 🛠️ Building from Source

### Requirements:
- Java Development Kit (JDK) 21 or higher
- Git

### Build Instructions:
```bash
# Clone the repository
git clone https://github.com/Gogik151/WhiteProxy.git
cd WhiteProxy

# Build the mod JAR using Gradle
./gradlew build
```

The compiled mod JAR will be located in:
```
build/libs/proxymod-1.21.11-1.0.0.jar
```

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.21.11**.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11.
3. Download `proxymod-1.21.11-1.0.0.jar` from [Modrinth](https://modrinth.com/mod/whiteproxy).
4. Place the `.jar` file into your `.minecraft/mods` folder.
5. Launch Minecraft and enjoy!

---

## 💬 Community & Support

- 📢 **Telegram Channel**: [t.me/whiteproxymod](https://t.me/whiteproxymod)
- 🐛 **Issue Tracker**: [GitHub Issues](https://github.com/Gogik151/WhiteProxy/issues)
- 🌐 **Modrinth**: [modrinth.com/mod/whiteproxy](https://modrinth.com/mod/whiteproxy)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

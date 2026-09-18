package ru.white.proxymod.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SocksProxyHandler extends ChannelDuplexHandler {

    private enum State {
        INIT,
        SOCKS5_AUTH_SELECT,
        SOCKS5_AUTH_STATUS,
        SOCKS5_CONNECT_RESPONSE,
        SOCKS4_CONNECT_RESPONSE,
        HTTP_CONNECT_RESPONSE,
        DONE
    }

    private InetSocketAddress target;
    private final ProxyConfig config;
    private ProxyConfig.ProxyProfile activeProfile;
    private State state = State.INIT;
    private ByteBuf cumulation;

    public SocksProxyHandler() {
        this(ProxyManager.getConfig());
    }

    public SocksProxyHandler(ProxyConfig config) {
        this.config = config;
        this.activeProfile = config != null ? config.getActiveProfile() : null;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        if (!(remoteAddress instanceof InetSocketAddress isa) || ctx.channel() instanceof io.netty.channel.local.LocalChannel) {
            try {
                ctx.pipeline().remove(this);
            } catch (Exception ignored) {}
            ctx.connect(remoteAddress, localAddress, promise);
            return;
        }

        ProxyManager.RoutingDecision decision = ProxyManager.resolveRouting(isa.getHostString(), isa.getPort());
        if (decision.isDirect() || decision.profile() == null) {
            try {
                ctx.pipeline().remove(this);
            } catch (Exception ignored) {}
            ctx.connect(remoteAddress, localAddress, promise);
            return;
        }

        this.target = isa;
        this.activeProfile = decision.profile();
        SocketAddress proxyAddress = ProxyManager.getProxySocketAddress(this.activeProfile);
        ctx.connect(proxyAddress, localAddress, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (activeProfile == null) {
            finishHandshake(ctx);
            return;
        }
        switch (activeProfile.getType()) {
            case SOCKS5 -> sendSocks5Greeting(ctx);
            case SOCKS4 -> sendSocks4Connect(ctx);
            case HTTP -> sendHttpConnect(ctx);
        }
    }

    private void sendSocks5Greeting(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0x05); // SOCKS5
        if (activeProfile != null && activeProfile.hasAuth()) {
            buf.writeByte(2); // 2 метода: без аутентификации (0x00) и логин/пароль (0x02)
            buf.writeByte(0x00);
            buf.writeByte(0x02);
        } else {
            buf.writeByte(1); // 1 метод: без аутентификации (0x00)
            buf.writeByte(0x00);
        }
        state = State.SOCKS5_AUTH_SELECT;
        ctx.writeAndFlush(buf);
    }

    private void sendSocks5Auth(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0x01); // Версия суб-переговоров логин/пароль
        byte[] uBytes = (activeProfile != null && activeProfile.getUsername() != null ? activeProfile.getUsername() : "").getBytes(StandardCharsets.UTF_8);
        buf.writeByte(uBytes.length);
        buf.writeBytes(uBytes);

        byte[] pBytes = (activeProfile != null && activeProfile.getPassword() != null ? activeProfile.getPassword() : "").getBytes(StandardCharsets.UTF_8);
        buf.writeByte(pBytes.length);
        buf.writeBytes(pBytes);

        state = State.SOCKS5_AUTH_STATUS;
        ctx.writeAndFlush(buf);
    }

    private static boolean isIpLiteral(String host) {
        if (host == null) return false;
        String[] parts = host.split("\\.");
        if (parts.length != 4) return false;
        for (String p : parts) {
            try {
                int n = Integer.parseInt(p);
                if (n < 0 || n > 255) return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private void sendSocks5ConnectRequest(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0x05); // SOCKS5
        buf.writeByte(0x01); // Команда CONNECT
        buf.writeByte(0x00); // Резерв

        String host = target.getHostString();
        int port = target.getPort();

        boolean isIp = isIpLiteral(host);
        byte[] ipBytes = null;
        if (isIp) {
            try {
                InetAddress addr = InetAddress.getByName(host);
                if (addr != null) {
                    byte[] raw = addr.getAddress();
                    if (raw.length == 4) {
                        ipBytes = raw;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (isIp && ipBytes != null && !config.isDnsLeakProtection()) {
            buf.writeByte(0x01); // IPv4
            buf.writeBytes(ipBytes);
        } else {
            byte[] hostBytes = host.getBytes(StandardCharsets.ISO_8859_1);
            buf.writeByte(0x03); // Доменное имя (Удаленный DNS на стороне прокси!)
            buf.writeByte(hostBytes.length);
            buf.writeBytes(hostBytes);
        }
        buf.writeShort(port);

        state = State.SOCKS5_CONNECT_RESPONSE;
        ctx.writeAndFlush(buf);
    }

    private void sendSocks4Connect(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0x04); // SOCKS4
        buf.writeByte(0x01); // CONNECT
        buf.writeShort(target.getPort());

        String host = target.getHostString();
        boolean isIp = isIpLiteral(host);

        if (isIp && !config.isDnsLeakProtection()) {
            try {
                byte[] ipBytes = InetAddress.getByName(host).getAddress();
                buf.writeBytes(ipBytes);
                buf.writeByte(0x00); // Empty userid
            } catch (Exception ignored) {
                sendSocks4a(buf, host);
            }
        } else {
            // SOCKS4a (Remote DNS resolution)
            sendSocks4a(buf, host);
        }

        state = State.SOCKS4_CONNECT_RESPONSE;
        ctx.writeAndFlush(buf);
    }

    private void sendSocks4a(ByteBuf buf, String host) {
        buf.writeBytes(new byte[]{0, 0, 0, 1}); // SOCKS4a dummy IP (0.0.0.x)
        buf.writeByte(0x00); // Empty userid + null terminator
        buf.writeBytes(host.getBytes(StandardCharsets.ISO_8859_1));
        buf.writeByte(0x00); // Null terminator
    }

    private void sendHttpConnect(ChannelHandlerContext ctx) {
        String host = target.getHostString();
        int port = target.getPort();
        StringBuilder sb = new StringBuilder();
        sb.append("CONNECT ").append(host).append(":").append(port).append(" HTTP/1.1\r\n");
        sb.append("Host: ").append(host).append(":").append(port).append("\r\n");
        if (activeProfile != null && activeProfile.hasAuth()) {
            String creds = activeProfile.getUsername() + ":" + (activeProfile.getPassword() != null ? activeProfile.getPassword() : "");
            String b64 = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            sb.append("Proxy-Authorization: Basic ").append(b64).append("\r\n");
        }
        sb.append("\r\n");

        byte[] raw = sb.toString().getBytes(StandardCharsets.US_ASCII);
        ByteBuf buf = ctx.alloc().buffer(raw.length);
        buf.writeBytes(raw);

        state = State.HTTP_CONNECT_RESPONSE;
        ctx.writeAndFlush(buf);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf in)) {
            super.channelRead(ctx, msg);
            return;
        }

        if (state == State.DONE) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (cumulation == null) {
            cumulation = in;
        } else {
            cumulation = Unpooled.wrappedBuffer(cumulation, in);
        }

        switch (state) {
            case SOCKS5_AUTH_SELECT -> {
                if (cumulation.readableBytes() < 2) return;
                byte ver = cumulation.readByte();
                byte method = cumulation.readByte();
                if (ver != 0x05) {
                    fail(ctx, "Неверная версия SOCKS-прокси: " + ver);
                    return;
                }
                if (method == 0x00) {
                    sendSocks5ConnectRequest(ctx);
                } else if (method == 0x02) {
                    sendSocks5Auth(ctx);
                } else {
                    fail(ctx, "Метод аутентификации SOCKS5 отклонён прокси: " + method);
                }
            }
            case SOCKS5_AUTH_STATUS -> {
                if (cumulation.readableBytes() < 2) return;
                byte ver = cumulation.readByte();
                byte status = cumulation.readByte();
                if (status == 0x00) {
                    sendSocks5ConnectRequest(ctx);
                } else {
                    fail(ctx, "Ошибка аутентификации SOCKS5 (код: " + status + ")");
                }
            }
            case SOCKS5_CONNECT_RESPONSE -> {
                if (cumulation.readableBytes() < 4) return;
                cumulation.markReaderIndex();
                byte ver = cumulation.readByte();
                byte status = cumulation.readByte();
                cumulation.readByte(); // rsv
                byte atyp = cumulation.readByte();

                int addrLen = switch (atyp) {
                    case 0x01 -> 4; // IPv4
                    case 0x04 -> 16; // IPv6
                    case 0x03 -> {
                        if (cumulation.readableBytes() < 1) {
                            cumulation.resetReaderIndex();
                            yield -1;
                        }
                        yield (cumulation.readByte() & 0xFF);
                    }
                    default -> -2;
                };

                if (addrLen < 0 || cumulation.readableBytes() < addrLen + 2) {
                    cumulation.resetReaderIndex();
                    return;
                }

                cumulation.skipBytes(addrLen + 2); // Пропускаем связанный адрес и порт

                if (status == 0x00) {
                    finishHandshake(ctx);
                } else {
                    fail(ctx, "Подключение через SOCKS5 отклонено (код ошибки: " + status + ")");
                }
            }
            case SOCKS4_CONNECT_RESPONSE -> {
                if (cumulation.readableBytes() < 8) return;
                cumulation.readByte(); // null
                byte status = cumulation.readByte();
                cumulation.skipBytes(6); // port + ip

                if (status == 0x5A) {
                    finishHandshake(ctx);
                } else {
                    fail(ctx, "Подключение через SOCKS4 отклонено (код: " + status + ")");
                }
            }
            case HTTP_CONNECT_RESPONSE -> {
                int crlfcrlf = findHttpHeaderEnd(cumulation);
                if (crlfcrlf < 0) return;

                byte[] headerBytes = new byte[crlfcrlf];
                cumulation.readBytes(headerBytes);
                cumulation.skipBytes(4); // \r\n\r\n

                String header = new String(headerBytes, StandardCharsets.US_ASCII);
                if (header.contains(" 200 ")) {
                    finishHandshake(ctx);
                } else {
                    fail(ctx, "HTTP прокси отклонил CONNECT: " + header.lines().findFirst().orElse("Unknown error"));
                }
            }
            default -> {}
        }
    }

    private int findHttpHeaderEnd(ByteBuf buf) {
        int rIndex = buf.readerIndex();
        int wIndex = buf.writerIndex();
        for (int i = rIndex; i <= wIndex - 4; i++) {
            if (buf.getByte(i) == '\r' && buf.getByte(i + 1) == '\n'
                    && buf.getByte(i + 2) == '\r' && buf.getByte(i + 3) == '\n') {
                return i - rIndex;
            }
        }
        return -1;
    }

    private void finishHandshake(ChannelHandlerContext ctx) {
        state = State.DONE;
        ctx.pipeline().remove(this);

        if (cumulation != null && cumulation.isReadable()) {
            ByteBuf remainder = cumulation;
            cumulation = null;
            ctx.fireChannelRead(remainder);
        } else if (cumulation != null) {
            cumulation.release();
            cumulation = null;
        }

        ctx.fireChannelActive();
    }

    private void fail(ChannelHandlerContext ctx, String error) {
        state = State.DONE;
        if (cumulation != null) {
            cumulation.release();
            cumulation = null;
        }
        ProxyManager.tryFailover();
        ctx.fireExceptionCaught(new IOException(error));
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        releaseCumulation();
        ProxyManager.tryFailover();
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        releaseCumulation();
        super.channelInactive(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        releaseCumulation();
        super.handlerRemoved(ctx);
    }

    private void releaseCumulation() {
        if (cumulation != null) {
            cumulation.release();
            cumulation = null;
        }
    }
}

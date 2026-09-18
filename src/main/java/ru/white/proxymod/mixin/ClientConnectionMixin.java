package ru.white.proxymod.mixin;

import io.netty.channel.ChannelPipeline;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.PacketSizeLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.white.proxymod.proxy.ProxyManager;
import ru.white.proxymod.proxy.SocksProxyHandler;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "addHandlers", at = @At("HEAD"))
    private static void onAddHandlers(ChannelPipeline pipeline, NetworkSide side, boolean bl, PacketSizeLogger packetSizeLogger, CallbackInfo ci) {
        if (side == NetworkSide.CLIENTBOUND && ProxyManager.isProxyActive()) {
            pipeline.addFirst("proxy_handler", new SocksProxyHandler(ProxyManager.getConfig()));
        }
    }
}

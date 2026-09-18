package ru.white.proxymod.mixin;

import net.minecraft.client.network.Address;
import net.minecraft.client.network.AllowedAddressResolver;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.white.proxymod.proxy.ProxyManager;

import java.net.InetSocketAddress;
import java.util.Optional;

@Mixin(AllowedAddressResolver.class)
public class AllowedAddressResolverMixin {

    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
    private void onResolve(ServerAddress serverAddress, CallbackInfoReturnable<Optional<Address>> cir) {
        if (serverAddress != null && ProxyManager.isDnsLeakProtected(serverAddress.getAddress(), serverAddress.getPort())) {
            Address unresolved = new Address() {
                @Override
                public String getHostName() {
                    return serverAddress.getAddress();
                }

                @Override
                public String getHostAddress() {
                    return serverAddress.getAddress();
                }

                @Override
                public int getPort() {
                    return serverAddress.getPort();
                }

                @Override
                public InetSocketAddress getInetSocketAddress() {
                    return InetSocketAddress.createUnresolved(serverAddress.getAddress(), serverAddress.getPort());
                }
            };
            cir.setReturnValue(Optional.of(unresolved));
        }
    }
}

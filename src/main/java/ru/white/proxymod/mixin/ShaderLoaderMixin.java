package ru.white.proxymod.mixin;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.white.proxymod.render.shader.ShaderStore;

@Mixin(targets = "net.minecraft.client.gl.ShaderLoader$Cache")
public class ShaderLoaderMixin {

    @Inject(method = "getSource", at = @At("HEAD"), cancellable = true)
    private void proxymod$provideEmbeddedSource(Identifier id, ShaderType type,
                                                CallbackInfoReturnable<String> cir) {
        String source = ShaderStore.getSource(id, type);
        if (source != null) {
            cir.setReturnValue(source);
        }
    }
}

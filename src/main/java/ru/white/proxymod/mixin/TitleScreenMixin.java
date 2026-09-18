package ru.white.proxymod.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.white.proxymod.gui.ProxyButtonWidget;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        int btnW = 96;
        int btnH = 20;
        int btnX = this.width - btnW - 10;
        int btnY = 6;

        this.addDrawableChild(new ProxyButtonWidget(btnX, btnY, btnW, btnH, this));
    }
}

package org.main.massdig.mixin;

import net.minecraft.client.Minecraft;
import org.main.massdig.client.MassdigClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void massdig$letRadiusBlockKeepMining(boolean leftClick, CallbackInfo callback) {
        if (leftClick && MassdigClient.shouldSuppressVanillaMining()) {
            callback.cancel();
        }
    }
}

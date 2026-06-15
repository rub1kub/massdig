package org.main.massdig.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.main.massdig.client.MassdigClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    private int destroyDelay;

    @Inject(method = {"startDestroyBlock", "continueDestroyBlock"}, at = @At("HEAD"))
    private void massdig$clearClientMiningDelay(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (MassdigClient.shouldRemoveMiningCooldown()) {
            destroyDelay = 0;
        }
    }
}

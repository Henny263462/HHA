package dev.henny.hha.mixin;

import dev.henny.hha.logic.FallEvents;
import dev.henny.hha.logic.FactionLock;
import dev.henny.hha.logic.HellSet;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected boolean jumping;

    /**
     * Hell Leggings: Lava Jesus — auf Lava laufen wie ein Strider.
     * Sneaken erlaubt bewusstes Eintauchen; Springen (Leertaste halten) schaltet
     * in Lava zurück auf normales Lava-Schwimmen, damit man wieder auftauchen kann.
     */
    @Inject(method = "canWalkOnFluid", at = @At("HEAD"), cancellable = true)
    private void hha$lavaWalking(FluidState state, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity player
                && state.isIn(FluidTags.LAVA)
                && !player.isSneaking()
                && !(this.jumping && player.isInLava())
                && dev.henny.hha.config.HhaConfig.enabled("lava_walking")
                && HellSet.hasLeggings(player)
                && (!(player instanceof ServerPlayerEntity serverPlayer)
                    || FactionLock.canUse(serverPlayer, player.getEquippedStack(EquipmentSlot.LEGS), false))) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Hell Boots/Leggings/Set: Fall Damage Immunity, Magma Stomp und Volcanic.
     */
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void hha$handleFall(double fallDistance, float damagePerBlock, DamageSource damageSource,
                                CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity player && FallEvents.onFall(player, fallDistance)) {
            cir.setReturnValue(false);
        }
    }
}

package dev.henny.hha.item

import dev.henny.hha.entity.HeavensMaceEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.MaceItem
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World

/**
 * Heaven's Mace — basiert auf der echten Vanilla-Mace (Smash-Attack bleibt).
 * Rechtsklick: wirft den Mace 50 Blöcke geradeaus, jedes getroffene Ziel wird
 * vom Blitz getroffen, danach kehrt er zurück.
 */
class HeavensMaceItem(settings: Settings) : MaceItem(settings) {

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        if (!dev.henny.hha.config.HhaConfig.enabled("mace_throw")) return ActionResult.PASS
        val stack = user.getStackInHand(hand)
        if (user.itemCooldownManager.isCoolingDown(stack)) return ActionResult.PASS
        if (world !is ServerWorld || user !is ServerPlayerEntity) return ActionResult.SUCCESS

        dev.henny.hha.logic.Cooldowns.set(user, stack, "mace_throw_cooldown")

        val projectile = HeavensMaceEntity(world, user)
        projectile.setVelocity(user, user.pitch, user.yaw, 0.0f, 1.8f, 0.0f)
        world.spawnEntity(projectile)

        world.playSound(null, user.blockPos, SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.PLAYERS, 1.0f, 1.1f)
        world.playSound(null, user.blockPos, SoundEvents.ITEM_TRIDENT_RIPTIDE_1.value(), SoundCategory.PLAYERS, 0.7f, 1.4f)
        return ActionResult.SUCCESS
    }
}

package dev.henny.hha.item

import dev.henny.hha.logic.SwordBuff
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World

/**
 * Hell's Sword — 9 Schaden.
 * Passiv: Crits geben 1 Absorptionsherz (bis zum nächsten erlittenen Treffer).
 * Aktiv (Rechtsklick): 20s Haste + Auto-Crits, 90s Cooldown.
 */
class HellsSwordItem(settings: Settings) : Item(settings) {

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val stack = user.getStackInHand(hand)
        if (user.itemCooldownManager.isCoolingDown(stack)) return ActionResult.PASS

        if (!dev.henny.hha.config.HhaConfig.enabled("sword_buff")) return ActionResult.PASS
        if (world is ServerWorld && user is ServerPlayerEntity) {
            SwordBuff.activate(world, user)
            dev.henny.hha.logic.Cooldowns.set(user, stack, "sword_buff_cooldown")
        }
        return ActionResult.SUCCESS
    }
}

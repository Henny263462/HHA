package dev.henny.hha.item

import dev.henny.hha.logic.Abilities
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World

/**
 * Heaven's Sword — 9 Schaden.
 * Passiv: Nach 2 Treffern in Folge crittet der nächste Hit automatisch.
 * Aktiv (Rechtsklick): Lichtwelle schleudert alle Gegner in der Linie weg, 35s Cooldown.
 */
class HeavensSwordItem(settings: Settings) : Item(settings) {

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val stack = user.getStackInHand(hand)
        if (user.itemCooldownManager.isCoolingDown(stack)) return ActionResult.PASS

        if (!dev.henny.hha.config.HhaConfig.enabled("light_wave")) return ActionResult.PASS
        if (world is ServerWorld && user is ServerPlayerEntity) {
            Abilities.lightWave(world, user)
            dev.henny.hha.logic.Cooldowns.set(user, stack, "light_wave_cooldown")
        }
        return ActionResult.SUCCESS
    }
}

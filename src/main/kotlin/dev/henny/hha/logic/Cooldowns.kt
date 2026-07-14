package dev.henny.hha.logic

import dev.henny.hha.config.HhaConfig
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity

/**
 * Zentraler Cooldown-Setter: liest die Dauer aus der Config und
 * überspringt Cooldowns komplett, solange der Ultra-Modus aktiv ist.
 */
object Cooldowns {

    fun set(player: ServerPlayerEntity, stack: ItemStack, configKey: String) {
        if (UltraMode.isActive(player)) return
        val ticks = HhaConfig.num(configKey).toInt()
        if (ticks > 0) {
            player.itemCooldownManager.set(stack, ticks)
        }
    }
}

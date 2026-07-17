package dev.henny.hha.logic

import dev.henny.hha.config.HhaConfig
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID

/**
 * Gemeinsames Ladesystem für **alle** Ketten-Nutzungen des Hell's Mace —
 * Grapple auf Blöcke, Grapple auf eigene Projektile und Gegner-Pull teilen
 * sich denselben Pool: `chain_charges` Ladungen (Default 2), zwischen zwei
 * schnellen Nutzungen der kurze `chain_cooldown` (2 s). Die Ladungen füllen
 * sich `chain_recharge` Ticks (20 s) nach der letzten Nutzung wieder komplett
 * auf; ist die letzte Ladung weg, zeigt das Item den langen Recharge-Cooldown.
 * Im Ultra-Modus deaktiviert.
 */
object ChainCharges {

    private class State(var charges: Int, var refillAt: Long)

    private val states = HashMap<UUID, State>()

    /**
     * Prüft und verbucht eine Kettennutzung. Setzt den passenden Item-Cooldown
     * (kurz bzw. lang) und liefert `true`, wenn die Kette feuern darf.
     */
    fun tryUse(player: ServerPlayerEntity, stack: ItemStack): Boolean {
        if (UltraMode.isActive(player)) return true

        val now = player.entityWorld.time
        val max = HhaConfig.num("chain_charges").toInt().coerceAtLeast(1)
        val state = states.getOrPut(player.uuid) { State(max, 0L) }

        // Recharge-Fenster abgelaufen → volle Ladungen (auch nach nur einer Nutzung).
        if (now >= state.refillAt) state.charges = max
        if (state.charges <= 0) return false

        state.charges--
        state.refillAt = now + HhaConfig.num("chain_recharge").toLong()
        if (state.charges > 0) {
            Cooldowns.set(player, stack, "chain_cooldown")
        } else {
            Cooldowns.set(player, stack, "chain_recharge")
        }
        return true
    }
}

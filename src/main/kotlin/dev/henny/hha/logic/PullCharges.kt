package dev.henny.hha.logic

import dev.henny.hha.config.HhaConfig
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID

/**
 * Ladesystem für den Hell's-Mace-Kettenzug: `pull_charges` Ladungen (Default 2),
 * zwischen den Ladungen der kurze `pull_cooldown` (2 s). Ist die letzte Ladung
 * verbraucht, greift der lange `pull_recharge` (20 s), danach sind alle Ladungen
 * wieder da. Im Ultra-Modus deaktiviert (freies Spammen, kein Cooldown).
 */
object PullCharges {

    private class State(var charges: Int, var refillAt: Long)

    private val states = HashMap<UUID, State>()

    /**
     * Prüft und verbucht eine Kettennutzung. Setzt den passenden Item-Cooldown
     * (kurz bzw. lang) und liefert `true`, wenn die Kette feuern darf.
     */
    fun tryUse(player: ServerPlayerEntity, stack: ItemStack): Boolean {
        if (UltraMode.isActive(player)) return true

        val now = player.entityWorld.time
        val max = HhaConfig.num("pull_charges").toInt().coerceAtLeast(1)
        val state = states.getOrPut(player.uuid) { State(max, 0L) }

        if (state.charges <= 0) {
            if (now < state.refillAt) return false
            state.charges = max
        }

        state.charges--
        if (state.charges > 0) {
            Cooldowns.set(player, stack, "pull_cooldown")
        } else {
            Cooldowns.set(player, stack, "pull_recharge")
            state.refillAt = now + HhaConfig.num("pull_recharge").toLong()
        }
        return true
    }
}

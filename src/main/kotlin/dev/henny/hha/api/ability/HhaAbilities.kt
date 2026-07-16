package dev.henny.hha.api.ability

import dev.henny.hha.api.event.HhaEvents
import dev.henny.hha.logic.FactionLock
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

/**
 * Ability-Registry und -Dispatcher. Die eingebauten Heaven/Hell-Fähigkeiten
 * laufen über denselben Weg wie Addon-Abilities:
 *
 * **Ability-Taste → Ausrüstungs-Check → Funktion** — der Tastendruck wählt
 * die erste verfügbare Ability des Slots (z. B. Ability 1 + volles Hell-Set →
 * Lavastrahl), prüft deren Fraktions-Gate und ruft dann `cast` auf.
 */
object HhaAbilities {

    private val bySlot = LinkedHashMap<AbilitySlot, MutableList<Ability>>()
    private val byId = LinkedHashMap<Identifier, Ability>()

    fun register(ability: Ability): Ability {
        require(byId.putIfAbsent(ability.id, ability) == null) {
            "Ability ${ability.id} ist bereits registriert"
        }
        bySlot.getOrPut(ability.slot) { ArrayList() }.add(ability)
        return ability
    }

    fun get(id: Identifier): Ability? = byId[id]

    fun bySlot(slot: AbilitySlot): List<Ability> = bySlot[slot] ?: emptyList()

    fun all(): Collection<Ability> = byId.values

    /** Serverseitiger Einstieg für einen Ability-Tastendruck. */
    fun dispatch(player: ServerPlayerEntity, slot: AbilitySlot) {
        val ability = bySlot(slot).firstOrNull { it.isAvailable(player) }
        if (ability == null) {
            slot.fallbackMessage?.let { key ->
                player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true)
            }
            return
        }
        ability.factionGateSlot?.let { gate ->
            if (!FactionLock.canUse(player, player.getEquippedStack(gate))) return
        }
        if (!HhaEvents.ABILITY_CAST.invoker().allowCast(player, ability)) return
        ability.cast(player)
    }
}

package dev.henny.hha.api.ability

import dev.henny.hha.api.event.HhaEvents
import dev.henny.hha.logic.FactionLock
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

/**
 * Ability-Registry und -Dispatcher. Die eingebauten Heaven/Hell-Fähigkeiten
 * laufen über denselben Weg wie Addon-Abilities.
 */
object HhaAbilities {

    private val byTrigger = LinkedHashMap<AbilityTrigger, MutableList<Ability>>()
    private val byId = LinkedHashMap<Identifier, Ability>()

    fun register(ability: Ability): Ability {
        require(byId.putIfAbsent(ability.id, ability) == null) {
            "Ability ${ability.id} ist bereits registriert"
        }
        byTrigger.getOrPut(ability.trigger) { ArrayList() }.add(ability)
        return ability
    }

    fun get(id: Identifier): Ability? = byId[id]

    fun byTrigger(trigger: AbilityTrigger): List<Ability> = byTrigger[trigger] ?: emptyList()

    fun all(): Collection<Ability> = byId.values

    /**
     * Serverseitiger Einstieg für einen Tastendruck: Fraktionssperre prüfen,
     * erste verfügbare Ability finden, [HhaEvents.ABILITY_CAST] feuern, casten.
     */
    fun dispatch(player: ServerPlayerEntity, trigger: AbilityTrigger) {
        val gate = player.getEquippedStack(trigger.factionGateSlot)
        if (!FactionLock.canUse(player, gate)) return

        val ability = byTrigger(trigger).firstOrNull { it.isAvailable(player) }
        if (ability == null) {
            trigger.fallbackMessage?.let { key ->
                player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true)
            }
            return
        }
        if (!HhaEvents.ABILITY_CAST.invoker().allowCast(player, ability)) return
        ability.cast(player)
    }
}

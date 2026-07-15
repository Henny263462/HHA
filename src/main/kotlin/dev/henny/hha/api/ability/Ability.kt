package dev.henny.hha.api.ability

import net.minecraft.entity.EquipmentSlot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

/**
 * Tasten-Slots, die der HHA-Client bereits sendet. Addons hängen ihre
 * Fähigkeiten an einen dieser Trigger — eigene Keybinds/Netzwerkpakete sind
 * nicht nötig.
 */
enum class AbilityTrigger(
    /** Slot, dessen getragenes Item für die Fraktionssperre geprüft wird. */
    val factionGateSlot: EquipmentSlot,
    /** Actionbar-Meldung, wenn keine Ability des Triggers verfügbar war (null = still). */
    val fallbackMessage: String?,
) {
    /** Beam-Taste (Standard G). */
    PRIMARY(EquipmentSlot.CHEST, "hha.msg.need_full_set"),

    /** Utility-Taste (Standard H). */
    UTILITY(EquipmentSlot.LEGS, "hha.msg.need_leggings"),

    /** Doppelsprung in der Luft (Sprungtaste). */
    MOVEMENT(EquipmentSlot.LEGS, null),

    /** Ultra-Taste (Standard U). */
    ULTRA(EquipmentSlot.HEAD, null),
}

/**
 * Eine aktive, serverseitig ausgeführte Fähigkeit. Pro Trigger gewinnt die
 * erste registrierte Ability, deren [isAvailable] zutrifft — [cast] ist dann
 * selbst für Cooldowns ([dev.henny.hha.logic.Cooldowns]) und Config-Toggles
 * verantwortlich.
 */
interface Ability {

    val id: Identifier

    val trigger: AbilityTrigger

    /** Trägt der Spieler gerade die Voraussetzungen (Setteile etc.)? */
    fun isAvailable(player: ServerPlayerEntity): Boolean

    fun cast(player: ServerPlayerEntity)
}

/** Kompakte Ability aus Lambdas — für die meisten Addons ausreichend. */
class SimpleAbility(
    override val id: Identifier,
    override val trigger: AbilityTrigger,
    private val available: (ServerPlayerEntity) -> Boolean,
    private val action: (ServerPlayerEntity) -> Unit,
) : Ability {
    override fun isAvailable(player: ServerPlayerEntity): Boolean = available(player)
    override fun cast(player: ServerPlayerEntity) = action(player)
}

package dev.henny.hha.api.ability

import net.minecraft.entity.EquipmentSlot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

/**
 * Die sechs generischen Ability-Tasten plus der Bewegungs-Trigger. Der Client
 * registriert für jeden Slot einen Keybind („Ability 1–6" im Steuerungsmenü);
 * Slots 1–3 sind mit G/H/U vorbelegt, 4–6 starten unbelegt und stehen Addons
 * frei zur Verfügung.
 *
 * Das Dispatch-Modell ist immer: **Ability-Taste → Ausrüstungs-Check →
 * Funktion.** Ein Tastendruck wählt die erste registrierte [Ability] des
 * Slots, deren [Ability.isAvailable] zutrifft (z. B. „trägt volles Hell-Set"),
 * und ruft dann deren [Ability.cast]-Funktion auf.
 */
enum class AbilitySlot(
    /** Actionbar-Meldung, wenn keine Ability des Slots verfügbar war (null = still). */
    val fallbackMessage: String?,
) {
    /** „Ability 1" (Standard G) — eingebaut: Licht-/Lavastrahl bei vollem Set. */
    ABILITY_1("hha.msg.need_full_set"),

    /** „Ability 2" (Standard H) — eingebaut: Fire Camp mit Hell-Leggings. */
    ABILITY_2("hha.msg.need_leggings"),

    /** „Ability 3" (Standard U) — eingebaut: Ultra-Modus. */
    ABILITY_3(null),

    /** „Ability 4" — frei für Addons, standardmäßig unbelegt. */
    ABILITY_4(null),

    /** „Ability 5" — frei für Addons, standardmäßig unbelegt. */
    ABILITY_5(null),

    /** „Ability 6" — frei für Addons, standardmäßig unbelegt. */
    ABILITY_6(null),

    /** Sprungtaste in der Luft — eingebaut: Doppelsprung (Heaven-Leggings). */
    MOVEMENT(null),
}

/**
 * Eine aktive, serverseitig ausgeführte Fähigkeit auf einem [AbilitySlot].
 *
 * Pro Slot gewinnt die erste registrierte Ability, deren [isAvailable]
 * zutrifft — [cast] ist dann selbst für Cooldowns
 * ([dev.henny.hha.logic.Cooldowns]) und Config-Toggles verantwortlich.
 */
interface Ability {

    val id: Identifier

    /** Taste, an der die Ability hängt. */
    val slot: AbilitySlot

    /**
     * Ausrüstungs-Slot, dessen getragenes Item vor dem Cast durch die
     * Fraktionssperre geprüft wird (`null` = kein Gate). Eingebaute Abilities
     * nutzen z. B. CHEST für die Beams und HEAD für Ultra.
     */
    val factionGateSlot: EquipmentSlot? get() = null

    /** Trägt der Spieler gerade die Voraussetzungen (Setteile etc.)? */
    fun isAvailable(player: ServerPlayerEntity): Boolean

    fun cast(player: ServerPlayerEntity)
}

/** Kompakte Ability aus Lambdas — für die meisten Addons ausreichend. */
class SimpleAbility(
    override val id: Identifier,
    override val slot: AbilitySlot,
    private val available: (ServerPlayerEntity) -> Boolean,
    private val action: (ServerPlayerEntity) -> Unit,
    override val factionGateSlot: EquipmentSlot? = null,
) : Ability {
    override fun isAvailable(player: ServerPlayerEntity): Boolean = available(player)
    override fun cast(player: ServerPlayerEntity) = action(player)
}

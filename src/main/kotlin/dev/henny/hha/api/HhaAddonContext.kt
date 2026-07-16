package dev.henny.hha.api

import dev.henny.hha.api.ability.Ability
import dev.henny.hha.api.lore.HhaLore
import dev.henny.hha.api.set.ArmorSet
import dev.henny.hha.api.set.ArmorSetBuilder
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import org.slf4j.Logger

/**
 * Kontext, den jedes Addon in [HhaAddon.onInitialize] erhält. Alle
 * Registrierungen laufen hierüber — dadurch sind Config-Schlüssel automatisch
 * mit der Addon-ID genamespaced und tauchen in `/hha list|toggle|set|get` auf.
 */
interface HhaAddonContext {

    /** Mod-ID des Addons (aus dessen `fabric.mod.json`). */
    val addonId: String

    /** Logger mit Addon-Präfix. */
    val logger: Logger

    /** `Identifier` im Namespace des Addons. */
    fun id(path: String): Identifier

    /**
     * Registriert ein Rüstungsset per Builder-DSL. Die Items müssen vorher
     * regulär registriert sein (z. B. über [dev.henny.hha.api.item.HhaItemHelpers]).
     *
     * ```kotlin
     * context.armorSet("frost") {
     *     helmet(FROST_HELMET); chestplate(FROST_CHESTPLATE)
     *     leggings(FROST_LEGGINGS); boots(FROST_BOOTS)
     *     weapon(FROST_BLADE)
     *     onTick { world, player, state -> if (state.full) ... }
     * }
     * ```
     */
    fun armorSet(path: String, configure: ArmorSetBuilder.() -> Unit): ArmorSet

    /** Registriert ein bereits gebautes Set. */
    fun registerSet(set: ArmorSet)

    /**
     * Registriert eine aktive Fähigkeit auf einem der sechs Ability-Slots
     * (Keybinds „Ability 1–6"; 4–6 sind standardmäßig unbelegt und für Addons
     * gedacht). Pro Slot gewinnt die erste registrierte Ability, deren
     * [Ability.isAvailable] zutrifft — Muster: Taste → Ausrüstungs-Check →
     * Funktion.
     */
    fun registerAbility(ability: Ability)

    /**
     * Registriert dynamische Lore-Zeilen für ein Item. Die Zeilen zeigen live
     * die echten Config-Werte (`num`/`seconds`/`hearts` im Builder) und werden
     * beim Ändern der Config automatisch aktuell — wie bei den HHA-Items.
     * Config-Schlüssel sind die vollen Keys (z. B. der Rückgabewert von
     * [registerNumber] oder eingebaute wie `"beam_damage"`).
     */
    fun registerLore(item: Item, configure: HhaLore.LoreBuilder.() -> Unit)

    /**
     * Registriert einen Feature-Toggle in der HHA-Config (Default [default]).
     * Liefert den vollen Schlüssel (`addonid.key`) zurück — abfragbar über
     * [toggle] oder [dev.henny.hha.config.HhaConfig.enabled].
     */
    fun registerToggle(key: String, default: Boolean): String

    /** Registriert einen Zahlenwert in der HHA-Config; liefert `addonid.key`. */
    fun registerNumber(key: String, default: Double): String

    /** Liest einen mit [registerToggle] registrierten Toggle (kurzer Key genügt). */
    fun toggle(key: String): Boolean

    /** Liest einen mit [registerNumber] registrierten Wert (kurzer Key genügt). */
    fun number(key: String): Double
}

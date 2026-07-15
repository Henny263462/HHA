package dev.henny.hha.client

import dev.henny.hha.HhaItems
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.item.Item
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.Locale
import kotlin.math.floor

/**
 * Dynamische Item-Lore: die Zeilen werden clientseitig pro Tooltip gebaut und
 * zeigen die echten (vom Server gesyncten) Config-Werte — Cooldowns in
 * Sekunden, Schaden in Punkten, Schwellen in Herzen. Ändert ein Admin die
 * Config, stimmt die Lore sofort wieder.
 */
object HhaTooltips {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            val lore = loreFor(stack.item) ?: return@register
            var index = minOf(1, lines.size)
            for (line in lore) {
                lines.add(index, line)
                index++
            }
        }
    }

    private fun loreFor(item: Item): List<Text>? = when (item) {
        HhaItems.HELL_HELMET -> listOf(
            line("item.hha.hell_helmet.lore1"),
            line("item.hha.hell_helmet.lore2", hearts("undying_rage_health")),
        )
        HhaItems.HELL_CHESTPLATE -> listOf(
            line("item.hha.hell_chestplate.lore1"),
            line("item.hha.hell_chestplate.lore2"),
            line("item.hha.hell_chestplate.lore3"),
            line("item.hha.hell_chestplate.lore4", num("beam_damage"), seconds("beam_cooldown")),
        )
        HhaItems.HELL_LEGGINGS -> listOf(
            line("item.hha.hell_leggings.lore1"),
            line("item.hha.hell_leggings.lore2", seconds("fire_camp_cooldown")),
            line("item.hha.hell_leggings.lore3", num("stomp_min_fall"), num("stomp_damage")),
        )
        HhaItems.HELL_BOOTS -> listOf(
            line("item.hha.hell_boots.lore1"),
            line("item.hha.hell_boots.lore2", hearts("trail_max_health"), num("ember_damage")),
            line("item.hha.hell_boots.lore3"),
        )
        HhaItems.HELLS_SWORD -> listOf(
            line("item.hha.hells_sword.lore1"),
            line("item.hha.hells_sword.lore2", seconds("sword_buff_cooldown")),
            line("item.hha.hells_sword.lore3"),
        )
        HhaItems.HELLS_MACE -> listOf(
            line("item.hha.hells_mace.lore1"),
            line("item.hha.hells_mace.lore2", num("pull_arrival_damage"), num("pull_charges"), seconds("pull_recharge")),
            line("item.hha.hells_mace.lore3", num("grapple_range"), seconds("grapple_cooldown")),
            line("item.hha.hells_mace.lore4", num("bounce_damage")),
        )
        HhaItems.HEAVEN_HELMET -> listOf(
            line("item.hha.heaven_helmet.lore1"),
            line("item.hha.heaven_helmet.lore2"),
        )
        HhaItems.HEAVEN_CHESTPLATE -> listOf(
            line("item.hha.heaven_chestplate.lore1"),
            line("item.hha.heaven_chestplate.lore2", seconds("divine_shield_cooldown")),
            line("item.hha.heaven_chestplate.lore3"),
            line(
                "item.hha.heaven_chestplate.lore4",
                num("beam_damage"), hearts("purify_heal"), seconds("beam_cooldown"),
            ),
        )
        HhaItems.HEAVEN_LEGGINGS -> listOf(
            line("item.hha.heaven_leggings.lore1"),
            line("item.hha.heaven_leggings.lore2", num("shockwave_base_damage")),
            line("item.hha.heaven_leggings.lore3", num("shockwave_min_fall")),
        )
        HhaItems.HEAVEN_BOOTS -> listOf(
            line("item.hha.heaven_boots.lore1"),
            line("item.hha.heaven_boots.lore2", hearts("trail_max_health")),
            line("item.hha.heaven_boots.lore3"),
        )
        HhaItems.HEAVENS_SWORD -> listOf(
            line("item.hha.heavens_sword.lore1"),
            line("item.hha.heavens_sword.lore2", num("light_wave_damage"), seconds("light_wave_cooldown")),
        )
        HhaItems.HEAVENS_MACE -> listOf(
            line("item.hha.heavens_mace.lore1"),
            line("item.hha.heavens_mace.lore2", num("mace_throw_damage"), seconds("mace_throw_cooldown")),
        )
        else -> null
    }

    private fun line(key: String, vararg args: Any): Text =
        Text.translatable(key, *args).formatted(Formatting.GOLD)

    /** Zahlenwert unverändert (z. B. Schaden, Blöcke). */
    private fun num(key: String): String = format(ClientConfigCache.num(key))

    /** Tick-Wert als Sekunden. */
    private fun seconds(key: String): String = format(ClientConfigCache.num(key) / 20.0)

    /** Half-Heart-Wert als Herzen. */
    private fun hearts(key: String): String = format(ClientConfigCache.num(key) / 2.0)

    private fun format(value: Double): String =
        if (value == floor(value) && !value.isInfinite()) {
            value.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.1f", value)
        }
}

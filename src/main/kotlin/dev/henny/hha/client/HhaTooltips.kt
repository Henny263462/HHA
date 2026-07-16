package dev.henny.hha.client

import dev.henny.hha.api.lore.HhaLore
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.Locale
import kotlin.math.floor

/**
 * Rendert die in [HhaLore] registrierten Lore-Zeilen (eingebaute Items und
 * Addons) mit den echten, vom Server gesyncten Config-Werten — Cooldowns in
 * Sekunden, Schaden in Punkten, Schwellen in Herzen. Ändert ein Admin die
 * Config, stimmt die Lore sofort wieder.
 */
object HhaTooltips {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            val lore = HhaLore.linesFor(stack.item) ?: return@register
            var index = minOf(1, lines.size)
            for (line in lore) {
                lines.add(index, render(line))
                index++
            }
        }
    }

    private fun render(line: HhaLore.LoreLine): Text {
        val args = line.args.map { arg ->
            val value = ClientConfigCache.num(arg.configKey)
            when (arg.kind) {
                HhaLore.ValueKind.NUMBER -> format(value)
                HhaLore.ValueKind.SECONDS -> format(value / 20.0)
                HhaLore.ValueKind.HEARTS -> format(value / 2.0)
            }
        }
        return Text.translatable(line.translationKey, *args.toTypedArray()).formatted(Formatting.GOLD)
    }

    private fun format(value: Double): String =
        if (value == floor(value) && !value.isInfinite()) {
            value.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.1f", value)
        }
}

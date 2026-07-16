package dev.henny.hha.api.lore

import net.minecraft.item.Item

/**
 * Dynamische Item-Lore: Zeilen werden clientseitig pro Tooltip gebaut und
 * zeigen die echten (vom Server gesyncten) Config-Werte. Ein Argument
 * referenziert einen Config-Zahlenschlüssel und ein Format:
 *
 * - [LoreBuilder.num] — Wert unverändert (Schaden, Blöcke, Anzahl)
 * - [LoreBuilder.seconds] — Ticks als Sekunden
 * - [LoreBuilder.hearts] — Half-Hearts als Herzen
 *
 * Addons registrieren über [dev.henny.hha.api.HhaAddonContext.registerLore];
 * die Config-Schlüssel sind dabei die **vollen** Keys (`addonid.key` — wie sie
 * [dev.henny.hha.api.HhaAddonContext.registerNumber] zurückgibt). Auch
 * eingebaute Keys wie `beam_damage` sind erlaubt.
 */
object HhaLore {

    enum class ValueKind { NUMBER, SECONDS, HEARTS }

    class LoreArg internal constructor(val configKey: String, val kind: ValueKind)

    class LoreLine internal constructor(val translationKey: String, val args: List<LoreArg>)

    class LoreBuilder internal constructor() {
        internal val lines = ArrayList<LoreLine>()

        /** Zeile mit Übersetzungsschlüssel; `%s`-Platzhalter werden durch [args] gefüllt. */
        fun line(translationKey: String, vararg args: LoreArg) {
            lines.add(LoreLine(translationKey, args.toList()))
        }

        fun num(configKey: String): LoreArg = LoreArg(configKey, ValueKind.NUMBER)

        fun seconds(configKey: String): LoreArg = LoreArg(configKey, ValueKind.SECONDS)

        fun hearts(configKey: String): LoreArg = LoreArg(configKey, ValueKind.HEARTS)
    }

    private val lore = LinkedHashMap<Item, List<LoreLine>>()

    /** Registriert (oder ersetzt) die Lore-Zeilen eines Items. */
    fun register(item: Item, configure: LoreBuilder.() -> Unit) {
        lore[item] = LoreBuilder().apply(configure).lines
    }

    fun linesFor(item: Item): List<LoreLine>? = lore[item]
}

package dev.henny.hha.api.set

import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

/**
 * Zentrale Set-Registry. Enthält auch die eingebauten Sets `hha:heaven` und
 * `hha:hell` — Addons können also z. B. abfragen, ob ein Stack zu irgendeinem
 * Set gehört.
 */
object HhaSets {

    val HEAVEN_ID: Identifier = Identifier.of("hha", "heaven")
    val HELL_ID: Identifier = Identifier.of("hha", "hell")

    private val sets = LinkedHashMap<Identifier, ArmorSet>()

    fun register(set: ArmorSet): ArmorSet {
        require(sets.putIfAbsent(set.id, set) == null) { "ArmorSet ${set.id} ist bereits registriert" }
        return set
    }

    fun get(id: Identifier): ArmorSet? = sets[id]

    fun all(): Collection<ArmorSet> = sets.values

    /** Set, zu dem der Stack gehört (Rüstung oder Set-Waffe), sonst `null`. */
    fun setOf(stack: ItemStack): ArmorSet? =
        if (stack.isEmpty) null else sets.values.firstOrNull { it.isSetItem(stack) }
}

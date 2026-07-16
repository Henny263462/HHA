package dev.henny.hha.api

import net.minecraft.util.Identifier

/**
 * Übersicht über alle geladenen HHA-Addons und was sie registriert haben —
 * Grundlage für `/hha addons` und für Addons, die andere Addons abfragen wollen.
 */
object HhaAddons {

    class AddonInfo internal constructor(
        val id: String,
        val name: String,
        val version: String,
    ) {
        internal val setsInternal = ArrayList<Identifier>()
        internal val abilitiesInternal = ArrayList<Identifier>()
        internal val configKeysInternal = ArrayList<String>()
        internal var loreItems: Int = 0

        val sets: List<Identifier> get() = setsInternal
        val abilities: List<Identifier> get() = abilitiesInternal
        val configKeys: List<String> get() = configKeysInternal
        val loreItemCount: Int get() = loreItems
    }

    private val addons = LinkedHashMap<String, AddonInfo>()

    internal fun create(id: String, name: String, version: String): AddonInfo =
        addons.getOrPut(id) { AddonInfo(id, name, version) }

    fun all(): Collection<AddonInfo> = addons.values

    fun get(id: String): AddonInfo? = addons[id]
}

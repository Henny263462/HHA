package dev.henny.hha.addon

import dev.henny.hha.Hha
import dev.henny.hha.api.HhaAddon
import dev.henny.hha.api.HhaAddonContext
import dev.henny.hha.api.HhaAddons
import dev.henny.hha.api.ability.Ability
import dev.henny.hha.api.ability.HhaAbilities
import dev.henny.hha.api.lore.HhaLore
import dev.henny.hha.api.set.ArmorSet
import dev.henny.hha.api.set.ArmorSetBuilder
import dev.henny.hha.api.set.HhaSets
import dev.henny.hha.api.set.armorSetBuilder
import dev.henny.hha.config.HhaConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Lädt alle Mods mit `"hha"`-Entrypoint und reicht ihnen ihren Kontext. */
internal object AddonLoader {

    fun init() {
        val containers = FabricLoader.getInstance()
            .getEntrypointContainers("hha", HhaAddon::class.java)
        for (container in containers) {
            val meta = container.provider.metadata
            Hha.LOGGER.info("Lade HHA-Addon: {} {}", meta.id, meta.version)
            val info = HhaAddons.create(meta.id, meta.name, meta.version.friendlyString)
            container.entrypoint.onInitialize(AddonContextImpl(meta.id, info))
        }
    }
}

private class AddonContextImpl(
    override val addonId: String,
    private val info: HhaAddons.AddonInfo,
) : HhaAddonContext {

    override val logger: Logger = LoggerFactory.getLogger("hha/$addonId")

    override fun id(path: String): Identifier = Identifier.of(addonId, path)

    override fun armorSet(path: String, configure: ArmorSetBuilder.() -> Unit): ArmorSet {
        val set = HhaSets.register(armorSetBuilder(id(path)).apply(configure).build())
        info.setsInternal.add(set.id)
        return set
    }

    override fun registerSet(set: ArmorSet) {
        HhaSets.register(set)
        info.setsInternal.add(set.id)
    }

    override fun registerAbility(ability: Ability) {
        HhaAbilities.register(ability)
        info.abilitiesInternal.add(ability.id)
    }

    override fun registerLore(item: Item, configure: HhaLore.LoreBuilder.() -> Unit) {
        HhaLore.register(item, configure)
        info.loreItems++
    }

    override fun registerToggle(key: String, default: Boolean): String {
        val full = fullKey(key)
        HhaConfig.registerToggle(full, default)
        info.configKeysInternal.add(full)
        return full
    }

    override fun registerNumber(key: String, default: Double): String {
        val full = fullKey(key)
        HhaConfig.registerNumber(full, default)
        info.configKeysInternal.add(full)
        return full
    }

    override fun toggle(key: String): Boolean = HhaConfig.enabled(fullKey(key))

    override fun number(key: String): Double = HhaConfig.num(fullKey(key))

    /** Brigadier-`word()` erlaubt Punkte, aber keine Doppelpunkte — daher `addonid.key`. */
    private fun fullKey(key: String): String =
        if (key.startsWith("$addonId.")) key else "$addonId.$key"
}

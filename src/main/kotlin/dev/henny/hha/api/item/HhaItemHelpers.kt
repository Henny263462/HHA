package dev.henny.hha.api.item

import net.minecraft.item.Item
import net.minecraft.item.equipment.ArmorMaterial
import net.minecraft.item.equipment.EquipmentType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier

/**
 * Registrierungshelfer für Addon-Items — dieselben Bausteine, mit denen HHA
 * seine eigenen Items anlegt. Assets (Model-JSON, Texturen, Equipment-Asset)
 * liefert das Addon in seinem eigenen Namespace mit.
 */
object HhaItemHelpers {

    /** Registriert ein Item; die Settings tragen bereits den Registry-Key. */
    fun register(id: Identifier, factory: (Item.Settings) -> Item): Item {
        val key = RegistryKey.of(RegistryKeys.ITEM, id)
        return Registry.register(Registries.ITEM, key, factory(Item.Settings().registryKey(key)))
    }

    /**
     * Registriert ein Rüstungsteil mit Material und Typ. Über [settings] lassen
     * sich Rarity, Lore, Fireproof usw. ergänzen.
     */
    fun registerArmor(
        id: Identifier,
        material: ArmorMaterial,
        type: EquipmentType,
        settings: (Item.Settings) -> Item.Settings = { it },
    ): Item = register(id) { s -> Item(settings(s.armor(material, type))) }
}

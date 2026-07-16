package dev.henny.hha.logic

import dev.henny.hha.api.set.ArmorSet
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.potion.Potion
import net.minecraft.potion.Potions
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents

/**
 * Kit-Modus: das klassische Diamond-SMP-Kit in der vorgegebenen Slot-Belegung.
 * Kits werden **automatisch aus jedem registrierten [ArmorSet] generiert** —
 * auch aus Addon-Sets: die vier Rüstungsteile voll verzaubert, dazu die
 * Set-Waffen nach Rollen-Konvention (`weapons[0]` = Schwert-Verzauberungen,
 * `weapons[1]` = Mace-Verzauberungen) und die Standard-Ausrüstung mit
 * Stärke-II- und Weaving-Wurftränken.
 */
object Kits {

    private val armorSlots = listOf(
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
    )

    /** Generiert und gibt das Kit für ein registriertes Set. */
    fun give(player: ServerPlayerEntity, set: ArmorSet) {
        val inventory = player.inventory
        inventory.clear()

        for (slot in armorSlots) {
            val piece = set.piece(slot) ?: continue
            player.equipStack(
                slot,
                enchanted(
                    player, piece,
                    Enchantments.PROTECTION to 4,
                    Enchantments.UNBREAKING to 3,
                    Enchantments.MENDING to 1,
                )
            )
        }
        player.equipStack(
            EquipmentSlot.OFFHAND,
            enchanted(player, Items.SHIELD, Enchantments.UNBREAKING to 3, Enchantments.MENDING to 1)
        )

        // Waffen-Rollen: [0] = Schwert (Hotbar 2), [1] = Mace (Hotbar 0), Rest ab Slot 3.
        set.weapons.getOrNull(1)?.let { mace ->
            inventory.setStack(
                0,
                enchanted(
                    player, mace,
                    Enchantments.WIND_BURST to 1,
                    Enchantments.DENSITY to 5,
                    Enchantments.UNBREAKING to 3,
                    Enchantments.MENDING to 1,
                )
            )
        }
        inventory.setStack(1, ItemStack(Items.TOTEM_OF_UNDYING))
        set.weapons.getOrNull(0)?.let { sword ->
            inventory.setStack(
                2,
                enchanted(
                    player, sword,
                    Enchantments.SHARPNESS to 5,
                    Enchantments.KNOCKBACK to 1,
                    Enchantments.FIRE_ASPECT to 2,
                    Enchantments.SWEEPING_EDGE to 3,
                    Enchantments.UNBREAKING to 3,
                    Enchantments.MENDING to 1,
                )
            )
        }
        var extraSlot = 3
        for (weapon in set.weapons.drop(2)) {
            if (extraSlot > 4) break
            inventory.setStack(
                extraSlot++,
                enchanted(player, weapon, Enchantments.UNBREAKING to 3, Enchantments.MENDING to 1)
            )
        }
        inventory.setStack(3, inventory.getStack(3).takeIf { !it.isEmpty }
            ?: splashPotion(Potions.STRONG_STRENGTH))
        inventory.setStack(4, inventory.getStack(4).takeIf { !it.isEmpty }
            ?: ItemStack(Items.COBWEB, 64))
        inventory.setStack(5, ItemStack(Items.WATER_BUCKET))
        inventory.setStack(6, ItemStack(Items.ENDER_PEARL, 16))
        inventory.setStack(7, ItemStack(Items.GOLDEN_APPLE, 64))
        inventory.setStack(
            8,
            enchanted(
                player, Items.DIAMOND_AXE,
                Enchantments.SHARPNESS to 5,
                Enchantments.UNBREAKING to 3,
                Enchantments.MENDING to 1,
            )
        )

        inventory.setStack(
            9,
            enchanted(
                player, Items.NETHERITE_PICKAXE,
                Enchantments.EFFICIENCY to 5,
                Enchantments.UNBREAKING to 3,
                Enchantments.MENDING to 1,
            )
        )
        inventory.setStack(10, ItemStack(Items.WATER_BUCKET))
        for (slot in 11..17) inventory.setStack(slot, splashPotion(Potions.STRONG_STRENGTH))

        inventory.setStack(18, ItemStack(Items.BREEZE_ROD, 64))
        for (slot in 19..22) inventory.setStack(slot, splashPotion(Potions.STRONG_STRENGTH))
        for (slot in 23..26) inventory.setStack(slot, splashPotion(Potions.WEAVING))

        inventory.setStack(27, ItemStack(Items.EXPERIENCE_BOTTLE, 64))
        inventory.setStack(28, ItemStack(Items.GOLDEN_APPLE, 64))
        inventory.setStack(29, splashPotion(Potions.STRONG_STRENGTH))
        inventory.setStack(30, ItemStack(Items.COBWEB, 64))
        inventory.setStack(31, ItemStack(Items.OAK_LOG, 64))
        inventory.setStack(32, ItemStack(Items.ENDER_PEARL, 16))
        inventory.setStack(33, ItemStack(Items.WATER_BUCKET))
        inventory.setStack(34, splashPotion(Potions.STRONG_STRENGTH))
        inventory.setStack(35, splashPotion(Potions.STRONG_STRENGTH))

        player.currentScreenHandler.sendContentUpdates()
        player.entityWorld.playSound(
            null, player.blockPos, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(),
            SoundCategory.PLAYERS, 1.0f, if (set.id == dev.henny.hha.api.set.HhaSets.HELL_ID) 0.8f else 1.2f
        )
    }

    private fun splashPotion(potion: RegistryEntry<Potion>): ItemStack =
        PotionContentsComponent.createStack(Items.SPLASH_POTION, potion)

    private fun enchanted(
        player: ServerPlayerEntity,
        item: Item,
        vararg enchantments: Pair<RegistryKey<Enchantment>, Int>,
    ): ItemStack {
        val stack = ItemStack(item)
        val registry = player.entityWorld.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        for ((key, level) in enchantments) {
            stack.addEnchantment(registry.getOrThrow(key), level)
        }
        return stack
    }
}

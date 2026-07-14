package dev.henny.hha.logic

import dev.henny.hha.HhaItems
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity

/** Hilfsfunktionen: welche Teile des Hells Sets trägt der Spieler gerade? */
object HellSet {

    @JvmStatic
    fun hasHelmet(player: PlayerEntity): Boolean =
        player.getEquippedStack(EquipmentSlot.HEAD).isOf(HhaItems.HELL_HELMET)

    @JvmStatic
    fun hasChestplate(player: PlayerEntity): Boolean =
        player.getEquippedStack(EquipmentSlot.CHEST).isOf(HhaItems.HELL_CHESTPLATE)

    @JvmStatic
    fun hasLeggings(player: PlayerEntity): Boolean =
        player.getEquippedStack(EquipmentSlot.LEGS).isOf(HhaItems.HELL_LEGGINGS)

    @JvmStatic
    fun hasBoots(player: PlayerEntity): Boolean =
        player.getEquippedStack(EquipmentSlot.FEET).isOf(HhaItems.HELL_BOOTS)

    @JvmStatic
    fun hasFullSet(player: PlayerEntity): Boolean =
        hasHelmet(player) && hasChestplate(player) && hasLeggings(player) && hasBoots(player)
}

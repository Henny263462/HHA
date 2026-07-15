package dev.henny.hha.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World

/**
 * Buch der Fähigkeiten: öffnet per Rechtsklick den Vanilla-Buchbildschirm.
 * Die Seiten stecken als WRITTEN_BOOK_CONTENT-Komponente am Item und sind
 * übersetzbare Texte — der Client zeigt sie in seiner Sprache an.
 */
class GuideBookItem(settings: Settings) : Item(settings) {

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        user.useBook(user.getStackInHand(hand), hand)
        return ActionResult.SUCCESS
    }
}

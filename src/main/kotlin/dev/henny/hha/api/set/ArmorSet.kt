package dev.henny.hha.api.set

import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier

/** Welche Teile eines Sets ein Spieler in diesem Tick trägt. */
class SetState(
    val helmet: Boolean,
    val chestplate: Boolean,
    val leggings: Boolean,
    val boots: Boolean,
) {
    val full: Boolean get() = helmet && chestplate && leggings && boots
    val anyPiece: Boolean get() = helmet || chestplate || leggings || boots
}

/** Serverseitiger Tick-Hook eines Sets — läuft nur, wenn der Spieler mindestens ein Teil trägt. */
fun interface SetTicker {
    fun tick(world: ServerWorld, player: ServerPlayerEntity, state: SetState)
}

/**
 * Ein Rüstungsset: vier Teile plus optionale Set-Waffen. Bietet dieselben
 * Abfragen, die HHA intern für Heaven/Hell nutzt ([hasPiece], [hasFullSet]),
 * und optional einen [SetTicker] für passive Effekte.
 */
class ArmorSet internal constructor(
    val id: Identifier,
    private val pieces: Map<EquipmentSlot, Item>,
    val weapons: List<Item>,
    internal val ticker: SetTicker?,
) {

    /** Item des Sets im Slot, sonst `null` (nur HEAD/CHEST/LEGS/FEET belegt). */
    fun piece(slot: EquipmentSlot): Item? = pieces[slot]

    fun hasPiece(player: PlayerEntity, slot: EquipmentSlot): Boolean {
        val item = pieces[slot] ?: return false
        return player.getEquippedStack(slot).isOf(item)
    }

    fun hasFullSet(player: PlayerEntity): Boolean =
        pieces.isNotEmpty() && pieces.keys.all { hasPiece(player, it) }

    fun state(player: PlayerEntity): SetState = SetState(
        helmet = hasPiece(player, EquipmentSlot.HEAD),
        chestplate = hasPiece(player, EquipmentSlot.CHEST),
        leggings = hasPiece(player, EquipmentSlot.LEGS),
        boots = hasPiece(player, EquipmentSlot.FEET),
    )

    /** Gehört der Stack zu diesem Set (Rüstung oder Waffe)? */
    fun isSetItem(stack: ItemStack): Boolean =
        !stack.isEmpty && (pieces.values.any { stack.isOf(it) } || weapons.any { stack.isOf(it) })
}

class ArmorSetBuilder internal constructor(private val id: Identifier) {

    private val pieces = LinkedHashMap<EquipmentSlot, Item>()
    private val weapons = ArrayList<Item>()
    private var ticker: SetTicker? = null

    fun helmet(item: Item) = apply { pieces[EquipmentSlot.HEAD] = item }
    fun chestplate(item: Item) = apply { pieces[EquipmentSlot.CHEST] = item }
    fun leggings(item: Item) = apply { pieces[EquipmentSlot.LEGS] = item }
    fun boots(item: Item) = apply { pieces[EquipmentSlot.FEET] = item }

    /** Set-Waffe (zählt für [ArmorSet.isSetItem], z. B. fürs HUD). */
    fun weapon(item: Item) = apply { weapons.add(item) }

    /** Passiver Effekt, jeden Server-Tick solange mindestens ein Teil getragen wird. */
    fun onTick(ticker: SetTicker) = apply { this.ticker = ticker }

    fun build(): ArmorSet {
        require(pieces.isNotEmpty()) { "ArmorSet $id braucht mindestens ein Rüstungsteil" }
        return ArmorSet(id, pieces, weapons, ticker)
    }
}

/** Startet einen Builder; registrieren über [HhaSets.register] oder den Addon-Kontext. */
fun armorSetBuilder(id: Identifier): ArmorSetBuilder = ArmorSetBuilder(id)

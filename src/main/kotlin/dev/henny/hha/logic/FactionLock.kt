package dev.henny.hha.logic

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.henny.hha.Hha
import dev.henny.hha.HhaItems
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.UUID

/**
 * Dauerhafte Fraktionsbindung für Nicht-OPs.
 *
 * Der erste Besitz eines Setitems bindet den Spieler an Hell oder Heaven.
 * Gegenstände der Gegenfraktion dürfen gelagert werden, aber erst benutzt oder
 * angezogen werden, wenn alle sechs unterschiedlichen Setitems gleichzeitig am
 * Spieler vorhanden sind. Die erste solche Nutzung wechselt die Bindung.
 */
object FactionLock {

    enum class Faction(val translationKey: String) {
        HELL("hha.faction.hell"),
        HEAVEN("hha.faction.heaven"),
    }

    private data class RequiredItem(val item: Item)

    private val armorSlots = listOf(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
    )

    private val bindings = HashMap<UUID, Faction>()
    private val lastNoticeTick = HashMap<UUID, Long>()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var initialized = false

    private fun path(): Path = FabricLoader.getInstance().configDir.resolve("hha_factions.json")

    /**
     * Lädt die Bindungen und registriert alle serverseitigen Sperren.
     * Muss nach [HhaItems.init] und vor [CombatEvents.init] aufgerufen werden.
     */
    fun init() {
        if (initialized) return
        initialized = true
        load()

        AttackEntityCallback.EVENT.register { player, _, hand, _, _ ->
            val serverPlayer = player as? ServerPlayerEntity
            val stack = serverPlayer?.getStackInHand(hand)
            if (serverPlayer != null && stack != null && isFactionWeapon(stack) &&
                !canUse(serverPlayer, stack)
            ) {
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        AttackBlockCallback.EVENT.register { player, _, hand, _, _ ->
            val serverPlayer = player as? ServerPlayerEntity
            val stack = serverPlayer?.getStackInHand(hand)
            if (serverPlayer != null && stack != null && isFactionWeapon(stack) &&
                !canUse(serverPlayer, stack)
            ) {
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        UseItemCallback.EVENT.register { player, _, hand ->
            val serverPlayer = player as? ServerPlayerEntity
            val stack = serverPlayer?.getStackInHand(hand)
            if (serverPlayer != null && stack != null && isSetItem(stack) &&
                !canUse(serverPlayer, stack)
            ) {
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        UseEntityCallback.EVENT.register { player, _, hand, _, _ ->
            val serverPlayer = player as? ServerPlayerEntity
            val stack = serverPlayer?.getStackInHand(hand)
            if (serverPlayer != null && stack != null && isFactionWeapon(stack) &&
                !canUse(serverPlayer, stack)
            ) {
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        UseBlockCallback.EVENT.register { player, _, hand, _ ->
            val serverPlayer = player as? ServerPlayerEntity
            val stack = serverPlayer?.getStackInHand(hand)
            if (serverPlayer != null && stack != null && isFactionWeapon(stack) &&
                !canUse(serverPlayer, stack)
            ) {
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        ServerTickEvents.END_SERVER_TICK.register(::tick)
    }

    /** Aktuelle dauerhafte Bindung; OPs können trotz eines Werts alles nutzen. */
    @JvmStatic
    fun factionOf(playerId: UUID): Faction? = bindings[playerId]

    /** Fraktion eines der zwölf Setitems, sonst `null`. */
    @JvmStatic
    fun factionOf(stack: ItemStack): Faction? = when {
        stack.isEmpty -> null
        stack.isOf(HhaItems.HELL_HELMET) ||
            stack.isOf(HhaItems.HELL_CHESTPLATE) ||
            stack.isOf(HhaItems.HELL_LEGGINGS) ||
            stack.isOf(HhaItems.HELL_BOOTS) ||
            stack.isOf(HhaItems.HELLS_SWORD) ||
            stack.isOf(HhaItems.HELLS_MACE) -> Faction.HELL

        stack.isOf(HhaItems.HEAVEN_HELMET) ||
            stack.isOf(HhaItems.HEAVEN_CHESTPLATE) ||
            stack.isOf(HhaItems.HEAVEN_LEGGINGS) ||
            stack.isOf(HhaItems.HEAVEN_BOOTS) ||
            stack.isOf(HhaItems.HEAVENS_SWORD) ||
            stack.isOf(HhaItems.HEAVENS_MACE) -> Faction.HEAVEN

        else -> null
    }

    /**
     * Zentrale, idempotente Abfrage für Waffen- und Rüstungsfähigkeiten.
     *
     * Ungebundene Nicht-OPs werden anhand ihres ersten gefundenen Besitzes
     * gebunden. Bei einem Gegenfraktionsitem wechselt die Bindung genau dann,
     * wenn alle sechs Gegen-Setitems gleichzeitig getragen/mitgeführt werden.
     */
    @JvmStatic
    @JvmOverloads
    fun canUse(player: ServerPlayerEntity, stack: ItemStack, notify: Boolean = true): Boolean {
        val targetFaction = factionOf(stack) ?: return true
        if (isOperator(player)) return true

        val currentFaction = bindings[player.uuid] ?: bindFromPossession(player, targetFaction)
        if (currentFaction == targetFaction) return true

        if (hasFullSet(player, targetFaction)) {
            switchFaction(player, targetFaction)
            return true
        }

        if (notify) showBlocked(player, targetFaction, armorReturned = false)
        return false
    }

    /** Ob der Spieler alle sechs unterschiedlichen Items dieser Fraktion besitzt. */
    @JvmStatic
    fun hasFullSet(player: ServerPlayerEntity, faction: Faction): Boolean =
        requiredItems(faction).all { required -> hasItemAnywhere(player, required.item) }

    private fun tick(server: MinecraftServer) {
        for (player in server.playerManager.playerList) {
            if (isOperator(player)) continue

            if (bindings[player.uuid] == null) {
                firstOwnedFaction(player)?.let { bind(player, it) }
            }
            enforceArmor(player)
        }
    }

    private fun enforceArmor(player: ServerPlayerEntity) {
        for (slot in armorSlots) {
            val equipped = player.getEquippedStack(slot)
            if (!isFactionArmor(equipped)) continue
            if (canUse(player, equipped, notify = false)) continue

            val returned = equipped.copy()
            player.equipStack(slot, ItemStack.EMPTY)
            player.giveOrDropStack(returned)
            player.currentScreenHandler.sendContentUpdates()
            showBlocked(player, factionOf(returned)!!, armorReturned = true)
        }
    }

    private fun bindFromPossession(player: ServerPlayerEntity, fallback: Faction): Faction {
        val faction = firstOwnedFaction(player) ?: fallback
        bind(player, faction)
        return faction
    }

    private fun bind(player: ServerPlayerEntity, faction: Faction) {
        if (bindings.putIfAbsent(player.uuid, faction) != null) return
        save()
        showActionbar(
            player,
            Text.translatable("hha.msg.faction_bound", factionName(faction)).formatted(Formatting.GOLD),
            force = true,
        )
    }

    private fun switchFaction(player: ServerPlayerEntity, faction: Faction) {
        if (bindings[player.uuid] == faction) return
        bindings[player.uuid] = faction
        save()
        showActionbar(
            player,
            Text.translatable("hha.msg.faction_switched", factionName(faction)).formatted(Formatting.GREEN),
            force = true,
        )
    }

    private fun firstOwnedFaction(player: ServerPlayerEntity): Faction? {
        for (stack in ownedStacks(player)) {
            factionOf(stack)?.let { return it }
        }
        return null
    }

    private fun hasItemAnywhere(player: ServerPlayerEntity, item: Item): Boolean =
        ownedStacks(player).any { it.isOf(item) }

    /**
     * Inventar + Rüstung + Offhand + Cursor. Inhalte von Shulkern/Bundles und
     * die Endertruhe zählen bewusst nicht als gleichzeitig mitgeführt.
     */
    private fun ownedStacks(player: ServerPlayerEntity): Sequence<ItemStack> = sequence {
        for (slot in 0 until player.inventory.size()) {
            yield(player.inventory.getStack(slot))
        }
        for (slot in armorSlots) yield(player.getEquippedStack(slot))
        yield(player.getEquippedStack(EquipmentSlot.OFFHAND))
        yield(player.currentScreenHandler.cursorStack)
    }

    private fun requiredItems(faction: Faction): List<RequiredItem> = when (faction) {
        Faction.HELL -> listOf(
            RequiredItem(HhaItems.HELL_HELMET),
            RequiredItem(HhaItems.HELL_CHESTPLATE),
            RequiredItem(HhaItems.HELL_LEGGINGS),
            RequiredItem(HhaItems.HELL_BOOTS),
            RequiredItem(HhaItems.HELLS_SWORD),
            RequiredItem(HhaItems.HELLS_MACE),
        )

        Faction.HEAVEN -> listOf(
            RequiredItem(HhaItems.HEAVEN_HELMET),
            RequiredItem(HhaItems.HEAVEN_CHESTPLATE),
            RequiredItem(HhaItems.HEAVEN_LEGGINGS),
            RequiredItem(HhaItems.HEAVEN_BOOTS),
            RequiredItem(HhaItems.HEAVENS_SWORD),
            RequiredItem(HhaItems.HEAVENS_MACE),
        )
    }

    private fun isSetItem(stack: ItemStack): Boolean = factionOf(stack) != null

    private fun isFactionArmor(stack: ItemStack): Boolean =
        stack.isOf(HhaItems.HELL_HELMET) ||
            stack.isOf(HhaItems.HELL_CHESTPLATE) ||
            stack.isOf(HhaItems.HELL_LEGGINGS) ||
            stack.isOf(HhaItems.HELL_BOOTS) ||
            stack.isOf(HhaItems.HEAVEN_HELMET) ||
            stack.isOf(HhaItems.HEAVEN_CHESTPLATE) ||
            stack.isOf(HhaItems.HEAVEN_LEGGINGS) ||
            stack.isOf(HhaItems.HEAVEN_BOOTS)

    private fun isFactionWeapon(stack: ItemStack): Boolean =
        stack.isOf(HhaItems.HELLS_SWORD) ||
            stack.isOf(HhaItems.HELLS_MACE) ||
            stack.isOf(HhaItems.HEAVENS_SWORD) ||
            stack.isOf(HhaItems.HEAVENS_MACE)

    private fun isOperator(player: ServerPlayerEntity): Boolean {
        val server = player.entityWorld.server ?: return false
        return server.playerManager.isOperator(player.playerConfigEntry)
    }

    private fun showBlocked(player: ServerPlayerEntity, faction: Faction, armorReturned: Boolean) {
        val missing = Text.empty()
        requiredItems(faction)
            .filterNot { hasItemAnywhere(player, it.item) }
            .forEachIndexed { index, required ->
                if (index > 0) missing.append(Text.literal(", "))
                missing.append(Text.translatable(required.item.translationKey))
            }
        val messageKey = if (armorReturned) {
            "hha.msg.faction_locked_armor"
        } else {
            "hha.msg.faction_locked"
        }
        showActionbar(
            player,
            Text.translatable(messageKey, factionName(faction), missing).formatted(Formatting.RED),
        )
    }

    private fun factionName(faction: Faction): Text = Text.translatable(faction.translationKey)

    private fun showActionbar(
        player: ServerPlayerEntity,
        message: Text,
        force: Boolean = false,
    ) {
        val now = player.entityWorld.server?.ticks?.toLong() ?: player.age.toLong()
        val previous = lastNoticeTick[player.uuid]
        if (!force && previous != null && now >= previous && now - previous < NOTICE_INTERVAL_TICKS) return
        lastNoticeTick[player.uuid] = now
        player.sendMessage(message, true)
    }

    private fun load() {
        bindings.clear()
        val file = path()
        if (!Files.exists(file)) return

        try {
            val root = gson.fromJson(Files.readString(file), JsonObject::class.java) ?: return
            root.entrySet().forEach { (key, value) ->
                val playerId = runCatching { UUID.fromString(key) }.getOrNull()
                val faction = Faction.entries.firstOrNull {
                    it.name.lowercase(Locale.ROOT) == value.asString.lowercase(Locale.ROOT)
                }
                if (playerId != null && faction != null) bindings[playerId] = faction
            }
            Hha.LOGGER.info("HHA-Fraktionsbindungen geladen ({} Spieler)", bindings.size)
        } catch (e: Exception) {
            Hha.LOGGER.error("Konnte config/hha_factions.json nicht lesen", e)
        }
    }

    private fun save() {
        try {
            val root = JsonObject()
            bindings.forEach { (playerId, faction) ->
                root.addProperty(playerId.toString(), faction.name.lowercase(Locale.ROOT))
            }
            Files.createDirectories(path().parent)
            Files.writeString(path(), gson.toJson(root))
        } catch (e: Exception) {
            Hha.LOGGER.error("Konnte config/hha_factions.json nicht schreiben", e)
        }
    }

    private const val NOTICE_INTERVAL_TICKS = 20L
}

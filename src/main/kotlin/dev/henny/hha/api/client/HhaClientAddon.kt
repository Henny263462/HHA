package dev.henny.hha.api.client

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

/**
 * Optionaler Client-Einstiegspunkt für Addons — Entrypoint `"hha_client"` in
 * der `fabric.mod.json` des Addons. Läuft nach der HHA-Client-Initialisierung
 * (HUD, Partikel-Factories).
 */
fun interface HhaClientAddon {

    fun onInitializeClient(context: HhaClientAddonContext)
}

interface HhaClientAddonContext {

    /** Mod-ID des Addons. */
    val addonId: String

    /**
     * Hängt einen Slot in die HHA-Cooldown-Leiste über der Hotbar. [provider]
     * liefert pro Frame den Stack, dessen Item-Cooldown angezeigt wird — oder
     * `null`, wenn der Slot gerade nicht gilt (z. B. Setteil nicht getragen).
     *
     * @param icon GUI-Sprite (`assets/<ns>/textures/gui/sprites/<pfad>.png`)
     * @param accentColor Puls-/Balkenfarbe als 0xRRGGBB
     */
    fun registerHudCooldown(icon: Identifier, accentColor: Int, provider: (PlayerEntity) -> ItemStack?)
}

/** Registry hinter [HhaClientAddonContext.registerHudCooldown] — nur clientseitig anfassen. */
object HudCooldowns {

    class Entry(
        val icon: Identifier,
        val accentColor: Int,
        val provider: (PlayerEntity) -> ItemStack?,
    )

    private val entries = ArrayList<Entry>()

    fun register(entry: Entry) {
        entries.add(entry)
    }

    fun all(): List<Entry> = entries
}

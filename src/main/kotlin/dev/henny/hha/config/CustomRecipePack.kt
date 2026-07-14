package dev.henny.hha.config

import dev.henny.hha.Hha
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.isDirectory

/**
 * Anpassbare Crafting-Rezepte: Alles unter config/hha/datapack/ wird bei jedem
 * Serverstart als Datapack "hha_custom" in die Welt gespiegelt und automatisch
 * aktiviert. Da Welt-Datapacks Vorrang vor Mod-Daten haben, überschreibt eine
 * Datei data/hha/recipe/<name>.json das gleichnamige eingebaute Rezept —
 * neue Dateien ergeben komplett eigene Rezepte.
 *
 * Beim ersten Start werden die eingebauten Rezepte als .json.example-Vorlagen
 * abgelegt (Vorlage aktivieren = Endung .example entfernen). Nach Änderungen:
 * /hha recipes reload — synct und lädt die Datapacks neu.
 */
object CustomRecipePack {

    private const val PACK_DIR_NAME = "hha_custom"
    private const val PACK_ID = "file/$PACK_DIR_NAME"

    /** Eingebaute Rezepte, die als editierbare Vorlagen exportiert werden. */
    private val TEMPLATE_RECIPES = listOf(
        "hell_ingot",
        "heaven_ingot",
        "jotunheims_upgrade_template_hell",
        "jotunheims_upgrade_template_heaven",
        "jotunheims_upgrade_template_dupe_hell",
        "jotunheims_upgrade_template_dupe_heaven",
    )

    fun init() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            try {
                seedConfigTemplates()
                syncToWorld(server)
                enableIfNeeded(server)
            } catch (e: Exception) {
                Hha.LOGGER.error("Konnte Custom-Rezept-Datapack nicht einrichten", e)
            }
        }
    }

    /** Für /hha recipes reload: Config neu spiegeln und Datapacks neu laden. */
    fun resync(server: MinecraftServer) {
        seedConfigTemplates()
        syncToWorld(server)
        val manager = server.dataPackManager
        manager.scanPacks()
        val enabled = LinkedHashSet(manager.enabledIds)
        enabled.add(PACK_ID)
        server.reloadResources(enabled)
    }

    private fun configDir(): Path =
        FabricLoader.getInstance().configDir.resolve("hha").resolve("datapack")

    private fun seedConfigTemplates() {
        val root = configDir()
        val recipeDir = root.resolve("data/hha/recipe")
        Files.createDirectories(recipeDir)

        val mcmeta = root.resolve("pack.mcmeta")
        if (!Files.exists(mcmeta)) {
            Files.writeString(
                mcmeta,
                """
                {
                  "pack": {
                    "description": "HHA custom recipes (config/hha/datapack)",
                    "pack_format": 81,
                    "supported_formats": { "min_inclusive": 4, "max_inclusive": 9999 }
                  }
                }
                """.trimIndent()
            )
        }

        val readme = root.resolve("README.md")
        if (!Files.exists(readme)) {
            Files.writeString(
                readme,
                """
                # HHA — eigene Crafting-Rezepte

                Dieser Ordner wird bei jedem Serverstart als Datapack `hha_custom`
                in die Welt gespiegelt und automatisch aktiviert.

                - `data/hha/recipe/<name>.json` mit gleichem Namen wie ein
                  eingebautes Rezept **überschreibt** dieses Rezept.
                - Neue Dateinamen ergeben **zusätzliche eigene Rezepte**
                  (Vanilla-Rezeptformat, siehe Minecraft-Wiki "Recipe").
                - Die `.json.example`-Dateien sind die eingebauten Rezepte als
                  Vorlage — zum Aktivieren die Endung `.example` entfernen.
                - Eingebaute Rezepte lassen sich per Config abschalten:
                  `/hha toggle hell_ingot_recipe false`,
                  `/hha toggle heaven_ingot_recipe false`,
                  `/hha toggle template_recipe false`
                - Nach Änderungen an Dateien: `/hha recipes reload`
                  (oder Server neu starten).
                """.trimIndent()
            )
        }

        for (name in TEMPLATE_RECIPES) {
            val example = recipeDir.resolve("$name.json.example")
            if (Files.exists(example) || Files.exists(recipeDir.resolve("$name.json"))) continue
            javaClass.getResourceAsStream("/data/hha/recipe/$name.json")?.use { stream ->
                Files.copy(stream, example, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    /** Spiegelt config/hha/datapack → <welt>/datapacks/hha_custom (ohne Vorlagen/Doku). */
    private fun syncToWorld(server: MinecraftServer) {
        val source = configDir()
        val target = server.getSavePath(WorldSavePath.DATAPACKS).resolve(PACK_DIR_NAME)

        if (Files.exists(target)) {
            Files.walk(target).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }

        Files.walk(source).use { paths ->
            for (path in paths) {
                val relative = source.relativize(path)
                val fileName = path.fileName?.toString() ?: continue
                if (fileName.endsWith(".example") || fileName.endsWith(".md")) continue
                val dest = target.resolve(relative.toString())
                if (path.isDirectory()) {
                    Files.createDirectories(dest)
                } else {
                    Files.createDirectories(dest.parent)
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    /** Beim ersten Start ist das Pack zwar da, aber nicht aktiv — einmalig zuschalten. */
    private fun enableIfNeeded(server: MinecraftServer) {
        val manager = server.dataPackManager
        manager.scanPacks()
        if (PACK_ID in manager.enabledIds || PACK_ID !in manager.getIds()) return
        val enabled = LinkedHashSet(manager.enabledIds)
        enabled.add(PACK_ID)
        server.reloadResources(enabled).whenComplete { _, error ->
            if (error != null) {
                Hha.LOGGER.error("Custom-Rezept-Datapack konnte nicht aktiviert werden", error)
            } else {
                Hha.LOGGER.info("Custom-Rezept-Datapack '{}' aktiviert", PACK_ID)
            }
        }
    }
}

package dev.henny.hha.config

import dev.henny.hha.Hha
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Zentrale Speicherorte der Mod — alles gebündelt unter `config/hha/`.
 * Ältere Installationen legten die Dateien direkt in `config/` ab; [migrate]
 * zieht sie beim ersten Start verlustfrei an den neuen Ort um.
 */
object HhaPaths {

    private val root: Path get() = FabricLoader.getInstance().configDir.resolve("hha")

    fun file(name: String): Path = root.resolve(name)

    /**
     * Verschiebt eine Altdatei `config/<legacyName>` nach [target] (unter
     * `config/hha/`), sofern dort noch nichts liegt. Einmalig und verlustfrei.
     */
    fun migrate(legacyName: String, target: Path) {
        if (Files.exists(target)) return
        val legacy = FabricLoader.getInstance().configDir.resolve(legacyName)
        if (!Files.exists(legacy)) return
        try {
            Files.createDirectories(target.parent)
            Files.move(legacy, target)
            Hha.LOGGER.info("HHA: config/{} → config/hha/{} migriert", legacyName, target.fileName)
        } catch (e: Exception) {
            Hha.LOGGER.error("HHA: Migration von config/{} fehlgeschlagen", legacyName, e)
        }
    }
}

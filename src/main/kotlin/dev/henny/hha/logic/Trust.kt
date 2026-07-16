package dev.henny.hha.logic

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.henny.hha.Hha
import dev.henny.hha.config.HhaPaths
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * /trust-System: Wen ich truste, den treffen meine Fähigkeiten nicht —
 * stattdessen wirken Purify & Co. Einseitig: A trustet B ≠ B trustet A.
 * Persistiert in config/hha/trust.json.
 */
object Trust {

    private val trusts = HashMap<UUID, MutableSet<UUID>>()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private fun path(): Path = HhaPaths.file("trust.json")

    @JvmStatic
    fun isTrusted(owner: UUID, other: UUID): Boolean =
        trusts[owner]?.contains(other) == true

    fun add(owner: UUID, other: UUID): Boolean {
        val added = trusts.getOrPut(owner) { HashSet() }.add(other)
        if (added) save()
        return added
    }

    fun remove(owner: UUID, other: UUID): Boolean {
        val removed = trusts[owner]?.remove(other) == true
        if (removed) save()
        return removed
    }

    fun listFor(owner: UUID): Set<UUID> = trusts[owner] ?: emptySet()

    fun load() {
        trusts.clear()
        val file = path()
        HhaPaths.migrate("hha_trust.json", file)
        if (!Files.exists(file)) return
        try {
            val root = gson.fromJson(Files.readString(file), JsonObject::class.java) ?: return
            root.entrySet().forEach { (key, value) ->
                val owner = UUID.fromString(key)
                val set = HashSet<UUID>()
                value.asJsonArray.forEach { set.add(UUID.fromString(it.asString)) }
                trusts[owner] = set
            }
        } catch (e: Exception) {
            Hha.LOGGER.error("Konnte config/hha_trust.json nicht lesen", e)
        }
    }

    private fun save() {
        try {
            val root = JsonObject()
            trusts.forEach { (owner, set) ->
                val arr = com.google.gson.JsonArray()
                set.forEach { arr.add(it.toString()) }
                root.add(owner.toString(), arr)
            }
            Files.createDirectories(path().parent)
            Files.writeString(path(), gson.toJson(root))
        } catch (e: Exception) {
            Hha.LOGGER.error("Konnte config/hha_trust.json nicht schreiben", e)
        }
    }
}

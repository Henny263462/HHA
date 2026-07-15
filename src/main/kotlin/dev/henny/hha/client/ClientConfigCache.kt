package dev.henny.hha.client

import dev.henny.hha.config.HhaConfig

/**
 * Vom Server gesyncte Config-Zahlen (ConfigSyncPayload). Solange kein Sync da
 * ist (z. B. Hauptmenü), gelten die lokalen Werte/Defaults aus [HhaConfig].
 */
object ClientConfigCache {

    private var numbers: Map<String, Double> = emptyMap()

    fun update(values: Map<String, Double>) {
        numbers = HashMap(values)
    }

    fun clear() {
        numbers = emptyMap()
    }

    fun num(key: String): Double = numbers[key] ?: HhaConfig.num(key)
}

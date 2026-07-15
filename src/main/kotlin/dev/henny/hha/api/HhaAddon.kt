package dev.henny.hha.api

/**
 * Einstiegspunkt für HHA-Addons.
 *
 * Ein Addon ist eine normale Fabric-Mod, die in ihrer `fabric.mod.json`
 * zusätzlich den Entrypoint `"hha"` deklariert und `"hha"` als Abhängigkeit
 * angibt:
 *
 * ```json
 * "entrypoints": { "hha": ["com.example.MyAddon"] },
 * "depends": { "hha": "*" }
 * ```
 *
 * HHA ruft [onInitialize] nach seinen eigenen Registrierungen auf, aber bevor
 * die Konfiguration geladen wird — Config-Defaults, Sets und Abilities, die
 * hier registriert werden, sind damit vollwertige Bürger (Commands, HUD,
 * Persistenz).
 */
fun interface HhaAddon {

    fun onInitialize(context: HhaAddonContext)
}

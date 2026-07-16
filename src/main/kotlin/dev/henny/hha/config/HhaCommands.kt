package dev.henny.hha.config

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import dev.henny.hha.logic.Trust
import dev.henny.hha.net.HhaNetworking
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Developer-Menü: /hha — Fähigkeiten togglen und Werte live anpassen (OP Level 2).
 *
 *   /hha list                  – alle Einstellungen anzeigen
 *   /hha toggle <key> <bool>   – Fähigkeit an-/ausschalten
 *   /hha set <key> <zahl>      – Schaden/Schwelle setzen
 *   /hha get <key>             – einzelnen Wert anzeigen
 *   /hha reset                 – alles auf Default
 *   /hha kit heaven|hell       – Diamond-SMP-Kit mit Set-Rüstung (für alle,
 *                                sofern kit_mode aktiv ist)
 */
object HhaCommands {

    private val TOGGLE_KEYS = SuggestionProvider<ServerCommandSource> { _, builder ->
        CommandSource.suggestMatching(HhaConfig.TOGGLE_DEFAULTS.keys, builder)
    }

    private val NUMBER_KEYS = SuggestionProvider<ServerCommandSource> { _, builder ->
        CommandSource.suggestMatching(HhaConfig.NUMBER_DEFAULTS.keys, builder)
    }

    /** Admin-Unterbefehle brauchen OP Level 2 — /hha kit bleibt für alle offen. */
    private val ADMIN = CommandManager.requirePermissionLevel<ServerCommandSource>(CommandManager.GAMEMASTERS_CHECK)

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal("hha")
                    .then(
                        literal("kit")
                            .then(literal("heaven").executes { ctx -> giveKit(ctx.source, dev.henny.hha.logic.Kits.Set.HEAVEN) })
                            .then(literal("hell").executes { ctx -> giveKit(ctx.source, dev.henny.hha.logic.Kits.Set.HELL) })
                    )
                    .then(literal("list").requires(ADMIN).executes { ctx ->
                        val source = ctx.source
                        source.sendFeedback({ header("Fähigkeiten") }, false)
                        HhaConfig.TOGGLE_DEFAULTS.keys.filter { '.' !in it }.forEach { key ->
                            source.sendFeedback({ toggleLine(key) }, false)
                        }
                        source.sendFeedback({ header("Werte") }, false)
                        HhaConfig.NUMBER_DEFAULTS.keys.filter { '.' !in it }.forEach { key ->
                            source.sendFeedback({ numberLine(key) }, false)
                        }
                        // Addon-Schlüssel (addonid.key) gruppiert je Addon anhängen.
                        val addonIds = (HhaConfig.TOGGLE_DEFAULTS.keys + HhaConfig.NUMBER_DEFAULTS.keys)
                            .filter { '.' in it }
                            .map { it.substringBefore('.') }
                            .distinct()
                        for (addonId in addonIds) {
                            source.sendFeedback({ header("Addon: $addonId") }, false)
                            HhaConfig.TOGGLE_DEFAULTS.keys
                                .filter { it.startsWith("$addonId.") }
                                .forEach { key -> source.sendFeedback({ toggleLine(key) }, false) }
                            HhaConfig.NUMBER_DEFAULTS.keys
                                .filter { it.startsWith("$addonId.") }
                                .forEach { key -> source.sendFeedback({ numberLine(key) }, false) }
                        }
                        1
                    })
                    .then(
                        literal("toggle").requires(ADMIN).then(
                            argument("key", StringArgumentType.word()).suggests(TOGGLE_KEYS).then(
                                argument("value", BoolArgumentType.bool()).executes { ctx ->
                                    val key = StringArgumentType.getString(ctx, "key")
                                    val value = BoolArgumentType.getBool(ctx, "value")
                                    if (HhaConfig.setToggle(key, value)) {
                                        ctx.source.sendFeedback({ toggleLine(key) }, true)
                                        if (key.endsWith("_recipe")) {
                                            ctx.source.server.commandManager
                                                .parseAndExecute(ctx.source.server.commandSource, "reload")
                                        }
                                        1
                                    } else {
                                        ctx.source.sendError(Text.literal("Unbekannte Fähigkeit: $key"))
                                        0
                                    }
                                }
                            )
                        )
                    )
                    .then(
                        literal("set").requires(ADMIN).then(
                            argument("key", StringArgumentType.word()).suggests(NUMBER_KEYS).then(
                                argument("value", DoubleArgumentType.doubleArg(0.0, 10000.0)).executes { ctx ->
                                    val key = StringArgumentType.getString(ctx, "key")
                                    val value = DoubleArgumentType.getDouble(ctx, "value")
                                    if (HhaConfig.setNumber(key, value)) {
                                        ctx.source.sendFeedback({ numberLine(key) }, true)
                                        HhaNetworking.syncConfig(ctx.source.server)
                                        1
                                    } else {
                                        ctx.source.sendError(Text.literal("Unbekannter Wert: $key"))
                                        0
                                    }
                                }
                            )
                        )
                    )
                    .then(
                        literal("get").requires(ADMIN).then(
                            argument("key", StringArgumentType.word()).suggests { ctx, builder ->
                                CommandSource.suggestMatching(
                                    HhaConfig.TOGGLE_DEFAULTS.keys + HhaConfig.NUMBER_DEFAULTS.keys, builder
                                )
                            }.executes { ctx ->
                                val key = StringArgumentType.getString(ctx, "key")
                                when {
                                    key in HhaConfig.TOGGLE_DEFAULTS -> {
                                        ctx.source.sendFeedback({ toggleLine(key) }, false); 1
                                    }
                                    key in HhaConfig.NUMBER_DEFAULTS -> {
                                        ctx.source.sendFeedback({ numberLine(key) }, false); 1
                                    }
                                    else -> {
                                        ctx.source.sendError(Text.literal("Unbekannter Schlüssel: $key")); 0
                                    }
                                }
                            }
                        )
                    )
                    .then(
                        literal("recipes").requires(ADMIN).then(
                            literal("reload").executes { ctx ->
                                CustomRecipePack.resync(ctx.source.server)
                                ctx.source.sendFeedback(
                                    {
                                        Text.literal("Custom-Rezepte neu geladen (config/hha/datapack).")
                                            .formatted(Formatting.GOLD)
                                    },
                                    true
                                )
                                1
                            }
                        )
                    )
                    .then(literal("reset").requires(ADMIN).executes { ctx ->
                        HhaConfig.reset()
                        HhaNetworking.syncConfig(ctx.source.server)
                        ctx.source.sendFeedback(
                            { Text.literal("HHA-Konfiguration auf Defaults zurückgesetzt.").formatted(Formatting.GOLD) },
                            true
                        )
                        1
                    })
                    .then(
                        literal("settings").requires(ADMIN)
                            .executes { ctx -> showParticleSettings(ctx.source) }
                            .then(
                                literal("particles")
                                    .executes { ctx -> showParticleSettings(ctx.source) }
                                    .then(particleSetting("player", "particle_player"))
                                    .then(particleSetting("effects", "particle_effects"))
                            )
                    )
                    .then(literal("save").requires(ADMIN).executes { ctx ->
                        HhaConfig.save()
                        ctx.source.sendFeedback(
                            { Text.literal("HHA-Konfiguration nach config/hha.json geschrieben.").formatted(Formatting.GOLD) },
                            true
                        )
                        1
                    })
                    .then(literal("load").requires(ADMIN).executes { ctx ->
                        HhaConfig.load()
                        HhaNetworking.syncConfig(ctx.source.server)
                        ctx.source.sendFeedback(
                            { Text.literal("HHA-Konfiguration aus config/hha.json neu geladen.").formatted(Formatting.GOLD) },
                            true
                        )
                        1
                    })
                    .then(literal("reload").requires(ADMIN).executes { ctx ->
                        HhaConfig.load()
                        CustomRecipePack.resync(ctx.source.server)
                        HhaNetworking.syncConfig(ctx.source.server)
                        ctx.source.sendFeedback(
                            {
                                Text.literal("HHA komplett neu geladen (Config, Rezepte, Client-Sync).")
                                    .formatted(Formatting.GOLD)
                            },
                            true
                        )
                        1
                    })
            )

            dispatcher.register(
                literal("trust")
                    .executes { ctx ->
                        val owner = ctx.source.playerOrThrow
                        val trusted = Trust.listFor(owner.uuid)
                        if (trusted.isEmpty()) {
                            ctx.source.sendFeedback(
                                { Text.translatable("hha.msg.trust_empty").formatted(Formatting.GRAY) }, false
                            )
                        } else {
                            val names = trusted.joinToString(", ") { uuid ->
                                ctx.source.server.playerManager.getPlayer(uuid)?.gameProfile?.name
                                    ?: uuid.toString().substring(0, 8)
                            }
                            ctx.source.sendFeedback(
                                { Text.translatable("hha.msg.trust_list", names).formatted(Formatting.GOLD) }, false
                            )
                        }
                        1
                    }
                    .then(
                        argument("player", EntityArgumentType.player()).executes { ctx ->
                            val owner = ctx.source.playerOrThrow
                            val target = EntityArgumentType.getPlayer(ctx, "player")
                            when {
                                target == owner -> {
                                    ctx.source.sendError(Text.translatable("hha.msg.trust_self"))
                                    0
                                }
                                Trust.add(owner.uuid, target.uuid) -> {
                                    ctx.source.sendFeedback(
                                        {
                                            Text.translatable("hha.msg.trust_added", target.gameProfile.name)
                                                .formatted(Formatting.GREEN)
                                        }, false
                                    )
                                    1
                                }
                                else -> {
                                    ctx.source.sendError(
                                        Text.translatable("hha.msg.trust_already", target.gameProfile.name)
                                    )
                                    0
                                }
                            }
                        }
                    )
            )

            dispatcher.register(
                literal("untrust").then(
                    argument("player", EntityArgumentType.player()).executes { ctx ->
                        val owner = ctx.source.playerOrThrow
                        val target = EntityArgumentType.getPlayer(ctx, "player")
                        if (Trust.remove(owner.uuid, target.uuid)) {
                            ctx.source.sendFeedback(
                                {
                                    Text.translatable("hha.msg.trust_removed", target.gameProfile.name)
                                        .formatted(Formatting.YELLOW)
                                }, false
                            )
                            1
                        } else {
                            ctx.source.sendError(
                                Text.translatable("hha.msg.trust_not_found", target.gameProfile.name)
                            )
                            0
                        }
                    }
                )
            )
        }
    }

    /** `/hha settings particles <name> [0..1]` — Anzeige ohne Wert, Setzen mit Wert. */
    private fun particleSetting(
        name: String,
        configKey: String,
    ): com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> =
        literal(name)
            .executes { ctx ->
                ctx.source.sendFeedback({ numberLine(configKey) }, false)
                1
            }
            .then(
                argument("value", DoubleArgumentType.doubleArg(0.0, 1.0)).executes { ctx ->
                    val value = DoubleArgumentType.getDouble(ctx, "value")
                    HhaConfig.setNumber(configKey, value)
                    HhaNetworking.syncConfig(ctx.source.server)
                    ctx.source.sendFeedback({ numberLine(configKey) }, true)
                    1
                }
            )

    private fun showParticleSettings(source: ServerCommandSource): Int {
        source.sendFeedback({ header("Partikel-Einstellungen") }, false)
        source.sendFeedback({ numberLine("particle_player") }, false)
        source.sendFeedback({ numberLine("particle_effects") }, false)
        source.sendFeedback(
            {
                Text.literal(" /hha settings particles player|effects <0..1>")
                    .formatted(Formatting.DARK_GRAY)
            },
            false
        )
        return 1
    }

    private fun giveKit(source: ServerCommandSource, set: dev.henny.hha.logic.Kits.Set): Int {
        val player = source.playerOrThrow
        if (!HhaConfig.enabled("kit_mode")) {
            source.sendError(Text.translatable("hha.msg.kit_disabled"))
            return 0
        }
        dev.henny.hha.logic.Kits.give(player, set)
        source.sendFeedback(
            {
                Text.translatable(
                    if (set == dev.henny.hha.logic.Kits.Set.HELL) "hha.msg.kit_hell" else "hha.msg.kit_heaven"
                ).formatted(Formatting.GOLD)
            }, false
        )
        return 1
    }

    private fun header(title: String): Text =
        Text.literal("— $title —").formatted(Formatting.GOLD, Formatting.BOLD)

    private fun toggleLine(key: String): Text {
        val on = HhaConfig.enabled(key)
        return Text.literal(" $key: ")
            .formatted(Formatting.GRAY)
            .append(
                Text.literal(if (on) "AN" else "AUS")
                    .formatted(if (on) Formatting.GREEN else Formatting.RED)
            )
    }

    private fun numberLine(key: String): Text =
        Text.literal(" $key: ")
            .formatted(Formatting.GRAY)
            .append(Text.literal(HhaConfig.num(key).toString()).formatted(Formatting.AQUA))
}

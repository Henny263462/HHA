package dev.henny.hha.logic

import dev.henny.hha.config.HhaConfig
import dev.henny.hha.net.UltraHudPayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Box
import java.util.UUID

/**
 * ULTRA-MODUS — 60 Sekunden Fähigkeiten-Spam ohne Cooldowns.
 * Volles Hells Set → „Inferno Overdrive": Flammenaura, Stärke II, brennende Gegner.
 * Volles Heaven's Set → „Seraph Ascendance": Lichtaura, Regen II, heilt Verbündete.
 */
object UltraMode {

    private data class State(val startTick: Long, val endTick: Long, val hell: Boolean)

    private val active = HashMap<UUID, State>()
    private val readyAt = HashMap<UUID, Long>()

    @JvmStatic
    fun isActive(player: ServerPlayerEntity): Boolean = active.containsKey(player.uuid)

    fun tryActivate(player: ServerPlayerEntity) {
        if (!HhaConfig.enabled("ultra_mode")) return
        val world = player.entityWorld as? ServerWorld ?: return
        val hell = HellSet.hasFullSet(player)
        val heaven = HeavenSet.hasFullSet(player)
        if (!hell && !heaven) {
            player.sendMessage(Text.translatable("hha.msg.need_full_set").formatted(Formatting.RED), true)
            return
        }
        if (isActive(player)) return

        val now = world.server!!.ticks.toLong()
        val ready = readyAt.getOrDefault(player.uuid, 0L)
        if (now < ready) {
            val seconds = (ready - now) / 20
            player.sendMessage(
                Text.translatable("hha.msg.ultra_cooldown", seconds).formatted(Formatting.RED), true
            )
            return
        }

        val duration = HhaConfig.num("ultra_duration").toLong()
        readyAt[player.uuid] = now + duration + HhaConfig.num("ultra_cooldown").toLong()
        active[player.uuid] = State(now, now + duration, hell)

        clearHhaCooldowns(player)
        player.itemCooldownManager.set(
            player.getEquippedStack(EquipmentSlot.HEAD),
            (duration + HhaConfig.num("ultra_cooldown").toLong()).toInt()
        )
        ServerPlayNetworking.send(player, UltraHudPayload(duration.toInt()))

        player.networkHandler.sendPacket(TitleFadeS2CPacket(10, 50, 20))
        if (hell) {
            player.networkHandler.sendPacket(
                TitleS2CPacket(Text.translatable("hha.ultra.hell.title").formatted(Formatting.DARK_RED, Formatting.BOLD))
            )
            world.playSound(null, player.blockPos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.2f, 1.3f)
            world.playSound(null, player.blockPos, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.8f, 0.8f)
        } else {
            player.networkHandler.sendPacket(
                TitleS2CPacket(Text.translatable("hha.ultra.heaven.title").formatted(Formatting.GOLD, Formatting.BOLD))
            )
            world.playSound(null, player.blockPos, SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.2f, 0.9f)
            world.playSound(null, player.blockPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.7f, 1.6f)
        }
        player.networkHandler.sendPacket(
            SubtitleS2CPacket(Text.translatable("hha.ultra.subtitle").formatted(Formatting.YELLOW))
        )
    }

    fun tick(world: ServerWorld) {
        if (active.isEmpty()) return
        val now = world.server!!.ticks.toLong()

        for (player in world.players) {
            val state = active[player.uuid] ?: continue
            if (!player.isAlive) {
                active.remove(player.uuid)
                ServerPlayNetworking.send(player, UltraHudPayload(0))
                continue
            }
            if (now >= state.endTick) {
                endUltra(world, player, state)
                continue
            }

            val t = now - state.startTick
            val main = if (state.hell) dev.henny.hha.HhaParticles.HELLFIRE else dev.henny.hha.HhaParticles.LIGHT_MOTE
            val accent = if (state.hell) dev.henny.hha.HhaParticles.EMBER_SPARK else dev.henny.hha.HhaParticles.HOLY_SPARK

            if (t < 50) {
                val radius = 0.5 + t * 0.25
                val center = player.entityPos.add(0.0, 0.15, 0.0)
                Fx.ring(world, center, radius, main, 36, 0.05)
                Fx.ring(world, center.add(0.0, 0.4, 0.0), radius * 0.65, accent, 18, 0.03)
                world.spawnParticles(
                    main,
                    player.x, player.y + t * 0.35, player.z,
                    8, 0.3, 0.4, 0.3, 0.02
                )
                if (t % 10L == 0L) {
                    world.spawnParticles(
                        if (state.hell) dev.henny.hha.HhaParticles.INFERNAL_BURST else dev.henny.hha.HhaParticles.DIVINE_FLASH,
                        player.x, player.y + 1.0, player.z,
                        1, 0.3, 0.3, 0.3, 0.0
                    )
                }
            }

            if (t % 4L == 0L) {
                Fx.spiral(world, player.entityPos, 1.0, 2.4, main, 14)
            }
            if (t % 6L == 0L) {
                val aura = if (state.hell) dev.henny.hha.HhaParticles.HELLFIRE else dev.henny.hha.HhaParticles.HOLY_SPARK
                world.spawnParticles(aura, player.x, player.y + 1.1, player.z, 2, 0.6, 0.7, 0.6, 0.0)
            }

            if (t % 20L == 0L) {
                clearHhaCooldowns(player)
                applyBuffs(player, state.hell)
                Fx.ring(world, player.entityPos.add(0.0, 0.2, 0.0), 3.0, accent, 24, 0.06)

                if (state.hell) {
                    for (entity in world.getEntitiesByClass(
                        LivingEntity::class.java, Box.of(player.entityPos, 12.0, 6.0, 12.0), { it.isAlive })
                    ) {
                        if (!Targeting.shouldHarm(player, entity)) continue
                        entity.setOnFireFor(3.0f)
                        entity.damage(world, world.damageSources.playerAttack(player), 2.0f)
                    }
                } else {
                    for (entity in world.getEntitiesByClass(
                        LivingEntity::class.java, Box.of(player.entityPos, 16.0, 8.0, 16.0), { it.isAlive })
                    ) {
                        if (!Targeting.isFriendly(player, entity)) continue
                        entity.heal(1.0f)
                        world.spawnParticles(
                            dev.henny.hha.HhaParticles.HOLY_SPARK,
                            entity.x, entity.y + entity.height + 0.3, entity.z,
                            2, 0.2, 0.1, 0.2, 0.0
                        )
                    }
                }
            }
        }
    }

    private fun endUltra(world: ServerWorld, player: ServerPlayerEntity, state: State) {
        active.remove(player.uuid)
        ServerPlayNetworking.send(player, UltraHudPayload(0))

        val main = if (state.hell) dev.henny.hha.HhaParticles.HELLFIRE else dev.henny.hha.HhaParticles.LIGHT_MOTE
        val accent = if (state.hell) dev.henny.hha.HhaParticles.EMBER_SPARK else dev.henny.hha.HhaParticles.HOLY_SPARK
        val center = player.entityPos.add(0.0, 0.3, 0.0)
        Fx.ring(world, center, 2.0, main, 24, 0.15)
        Fx.ring(world, center, 3.5, main, 30, 0.1)
        Fx.ring(world, center, 5.0, accent, 20, 0.08)
        world.spawnParticles(
            if (state.hell) dev.henny.hha.HhaParticles.INFERNAL_BURST else dev.henny.hha.HhaParticles.DIVINE_FLASH,
            player.x, player.y + 1.2, player.z,
            3, 1.2, 0.6, 1.2, 0.0
        )
        world.playSound(
            null, player.blockPos,
            if (state.hell) SoundEvents.ENTITY_GENERIC_EXPLODE.value() else SoundEvents.BLOCK_BEACON_DEACTIVATE,
            SoundCategory.PLAYERS, 1.0f, 0.8f
        )
        player.sendMessage(Text.translatable("hha.msg.ultra_ended").formatted(Formatting.GRAY), true)
    }

    private fun applyBuffs(player: ServerPlayerEntity, hell: Boolean) {
        if (hell) {
            player.addStatusEffect(StatusEffectInstance(StatusEffects.STRENGTH, 60, 1, true, false, true))
            player.addStatusEffect(StatusEffectInstance(StatusEffects.SPEED, 60, 1, true, false, true))
            player.addStatusEffect(StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 0, true, false, true))
            player.addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true))
        } else {
            player.addStatusEffect(StatusEffectInstance(StatusEffects.SPEED, 60, 1, true, false, true))
            player.addStatusEffect(StatusEffectInstance(StatusEffects.REGENERATION, 60, 1, true, false, true))
            player.addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true))
            player.addStatusEffect(StatusEffectInstance(StatusEffects.JUMP_BOOST, 60, 1, true, false, true))
        }
    }

    /** Alle Cooldowns auf HHA-Items entfernen — Spam frei. */
    private fun clearHhaCooldowns(player: ServerPlayerEntity) {
        val manager = player.itemCooldownManager
        val stacks = buildList {
            add(player.getEquippedStack(EquipmentSlot.CHEST))
            add(player.getEquippedStack(EquipmentSlot.LEGS))
            add(player.mainHandStack)
            add(player.offHandStack)
        }
        for (stack in stacks) {
            if (stack.isEmpty) continue
            val id = net.minecraft.registry.Registries.ITEM.getId(stack.item)
            if (id.namespace == "hha" && manager.isCoolingDown(stack)) {
                manager.remove(manager.getGroup(stack))
            }
        }
    }
}

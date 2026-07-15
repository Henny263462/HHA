package dev.henny.hha.logic

import dev.henny.hha.HhaParticles
import dev.henny.hha.config.HhaConfig
import net.minecraft.block.AbstractFireBlock
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Aktive Fähigkeiten, ausgelöst per Keybind (C2S-Payload).
 */
object Abilities {

    private const val BEAM_RANGE = 24.0
    private const val BEAM_COOLDOWN = 200
    private const val CAMP_COOLDOWN = 1200
    private const val WAVE_RANGE = 9.0

    /** Set-Bonus: gezielter Lavastrahl aus der Brustplatte. */
    fun lavaBeam(player: ServerPlayerEntity) {
        val world = player.entityWorld as? ServerWorld ?: return
        if (!HellSet.hasFullSet(player)) {
            actionBar(player, "hha.msg.need_full_set")
            return
        }
        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        if (player.itemCooldownManager.isCoolingDown(chest)) return
        Cooldowns.set(player, chest, "beam_cooldown")

        val start = player.eyePos
        val direction = player.getRotationVec(1.0f)
        val blockHit = world.raycast(
            RaycastContext(
                start, start.add(direction.multiply(BEAM_RANGE)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
            )
        )
        val maxDistance =
            if (blockHit.type == HitResult.Type.BLOCK) blockHit.pos.distanceTo(start) else BEAM_RANGE

        val muzzle = start.add(direction.multiply(1.2))
        Fx.spawn(world, HhaParticles.INFERNAL_BURST, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0)

        val (right, up) = beamBasis(direction)
        val alreadyHit = HashSet<UUID>()
        var d = 1.0
        while (d <= maxDistance) {
            val point = start.add(direction.multiply(d))
            Fx.spawn(world, HhaParticles.SOUL_FLAME, point.x, point.y, point.z, 3, 0.12, 0.12, 0.12, 0.01)
            Fx.spawn(world, HhaParticles.HELLFIRE, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.005)
            val angle = d * 1.4
            for (strand in 0 until 3) {
                val a = angle + strand * (Math.PI * 2.0 / 3.0)
                val offset = right.multiply(cos(a) * 0.45).add(up.multiply(sin(a) * 0.45))
                Fx.spawn(world, 
                    if (strand == 0) HhaParticles.EMBER_SPARK else HhaParticles.SOUL_MOTE,
                    point.x + offset.x, point.y + offset.y, point.z + offset.z,
                    1, 0.0, 0.0, 0.0, 0.0
                )
            }
            if (d.mod(4.0) < 0.5) {
                Fx.orientedRing(world, point, right, up, 0.9, HhaParticles.SOUL_FLAME, 12)
            }

            val hitBox = Box.of(point, 2.2, 2.2, 2.2)
            for (entity in world.getEntitiesByClass(LivingEntity::class.java, hitBox, { it.isAlive && it != player })) {
                if (!Targeting.shouldHarm(player, entity) || !alreadyHit.add(entity.uuid)) continue
                entity.damage(world, world.damageSources.playerAttack(player), HhaConfig.numF("beam_damage"))
                entity.setOnFireFor(6.0f)
                Fx.spawn(world, 
                    HhaParticles.EMBER_SPARK,
                    entity.x, entity.y + entity.height * 0.5, entity.z,
                    10, 0.3, 0.3, 0.3, 0.03
                )
            }
            d += 0.5
        }

        if (blockHit.type == HitResult.Type.BLOCK) {
            val firePos = blockHit.blockPos.offset(blockHit.side)
            if (world.getBlockState(firePos).isAir) {
                world.setBlockState(firePos, AbstractFireBlock.getState(world, firePos))
            }
            Fx.spawn(world, 
                HhaParticles.SOOT,
                blockHit.pos.x, blockHit.pos.y, blockHit.pos.z,
                8, 0.4, 0.3, 0.4, 0.03
            )
            Fx.spawn(world, 
                HhaParticles.INFERNAL_BURST,
                blockHit.pos.x, blockHit.pos.y, blockHit.pos.z,
                1, 0.0, 0.0, 0.0, 0.0
            )
            Fx.spawn(world, 
                HhaParticles.EMBER_SPARK,
                blockHit.pos.x, blockHit.pos.y, blockHit.pos.z,
                20, 0.5, 0.5, 0.5, 0.05
            )
            Fx.spawn(world, 
                HhaParticles.SOUL_FLAME,
                blockHit.pos.x, blockHit.pos.y + 0.4, blockHit.pos.z,
                16, 0.25, 0.8, 0.25, 0.02
            )
            Fx.ring(world, blockHit.pos, 1.2, HhaParticles.SOUL_FLAME, 16, 0.06)
            Fx.ring(world, blockHit.pos, 2.2, HhaParticles.EMBER_SPARK, 20, 0.09)
            Fx.ring(world, blockHit.pos, 3.2, HhaParticles.SOUL_MOTE, 24, 0.12)
            world.playSound(null, blockHit.blockPos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.9f, 0.9f)
        }

        Fx.spiral(world, player.entityPos, 0.7, 1.6, HhaParticles.SOUL_FLAME)
        Fx.ring(world, player.entityPos.add(0.0, 0.1, 0.0), 1.1, HhaParticles.HELLFIRE, 16, 0.05)
        world.playSound(null, player.blockPos, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.6f)
        world.playSound(null, player.blockPos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 0.8f)
        world.playSound(null, player.blockPos, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.7f)
    }

    /** Leggings: Fire Camp — Regen IV und 3 verbündete Brutes. */
    fun fireCamp(player: ServerPlayerEntity) {
        val world = player.entityWorld as? ServerWorld ?: return
        if (!HellSet.hasLeggings(player)) {
            actionBar(player, "hha.msg.need_leggings")
            return
        }
        val leggings = player.getEquippedStack(EquipmentSlot.LEGS)
        if (player.itemCooldownManager.isCoolingDown(leggings)) return
        Cooldowns.set(player, leggings, "fire_camp_cooldown")

        player.addStatusEffect(StatusEffectInstance(StatusEffects.REGENERATION, 160, 3, false, true, true))
        val brutes = BruteAllies.spawn(world, player, 3)
        SoulCamp.create(world, player, brutes)

        val center = player.entityPos.add(0.0, 0.15, 0.0)
        Fx.ring(world, center, 2.0, HhaParticles.SOUL_FLAME, 22, 0.04)
        Fx.ring(world, center, 1.2, HhaParticles.SOUL_MOTE, 14, 0.03)
        Fx.spawn(world, 
            HhaParticles.SOOT,
            player.x, player.y + 0.8, player.z,
            12, 1.5, 0.6, 1.5, 0.01
        )
        Fx.spawn(world, HhaParticles.EMBER_SPARK, player.x, player.y + 0.5, player.z, 14, 1.5, 0.4, 1.5, 0.02)
        world.playSound(null, player.blockPos, SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 1.5f, 0.8f)
        world.playSound(null, player.blockPos, SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.8f, 0.7f)
    }

    /** Heaven-Set-Bonus: Lichtstrahl — Schaden für Feinde, Purify (Heilung + Debuff-Entfernung) für Verbündete. */
    fun lightBeam(player: ServerPlayerEntity) {
        val world = player.entityWorld as? ServerWorld ?: return
        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        if (player.itemCooldownManager.isCoolingDown(chest)) return
        Cooldowns.set(player, chest, "beam_cooldown")

        val start = player.eyePos
        val direction = player.getRotationVec(1.0f)
        val blockHit = world.raycast(
            RaycastContext(
                start, start.add(direction.multiply(BEAM_RANGE)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
            )
        )
        val maxDistance =
            if (blockHit.type == HitResult.Type.BLOCK) blockHit.pos.distanceTo(start) else BEAM_RANGE

        val muzzle = start.add(direction.multiply(1.2))
        Fx.spawn(world, HhaParticles.DIVINE_FLASH, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0)

        val (right, up) = beamBasis(direction)
        val alreadyHit = HashSet<UUID>()
        var d = 1.0
        while (d <= maxDistance) {
            val point = start.add(direction.multiply(d))
            Fx.spawn(world, HhaParticles.LIGHT_MOTE, point.x, point.y, point.z, 3, 0.1, 0.1, 0.1, 0.005)
            Fx.spawn(world, HhaParticles.HOLY_FLAME, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.005)
            val angle = d * 1.4
            for (strand in 0 until 3) {
                val a = angle + strand * (Math.PI * 2.0 / 3.0)
                val offset = right.multiply(cos(a) * 0.45).add(up.multiply(sin(a) * 0.45))
                Fx.spawn(world, 
                    if (strand == 0) HhaParticles.LIGHT_MOTE else HhaParticles.HOLY_SPARK,
                    point.x + offset.x, point.y + offset.y, point.z + offset.z,
                    1, 0.0, 0.0, 0.0, 0.0
                )
            }
            if (d.mod(4.0) < 0.5) {
                Fx.orientedRing(world, point, right, up, 0.9, HhaParticles.HOLY_SPARK, 12)
            }

            val hitBox = Box.of(point, 2.2, 2.2, 2.2)
            for (entity in world.getEntitiesByClass(LivingEntity::class.java, hitBox, { it.isAlive && it != player })) {
                if (!alreadyHit.add(entity.uuid)) continue
                if (Targeting.isFriendly(player, entity)) {
                    if (HhaConfig.enabled("purify")) purify(world, entity)
                } else if (Targeting.shouldHarm(player, entity)) {
                    entity.damage(world, world.damageSources.playerAttack(player), HhaConfig.numF("beam_damage"))
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.GLOWING, 60, 0, false, false, false))
                    Fx.spawn(world, 
                        HhaParticles.HOLY_SPARK,
                        entity.x, entity.y + entity.height * 0.5, entity.z,
                        8, 0.3, 0.3, 0.3, 0.04
                    )
                }
            }
            d += 0.5
        }

        if (blockHit.type == HitResult.Type.BLOCK) {
            Fx.spawn(world, 
                HhaParticles.DIVINE_FLASH,
                blockHit.pos.x, blockHit.pos.y, blockHit.pos.z,
                1, 0.0, 0.0, 0.0, 0.0
            )
            Fx.spawn(world, 
                HhaParticles.LIGHT_MOTE,
                blockHit.pos.x, blockHit.pos.y, blockHit.pos.z,
                16, 0.3, 0.3, 0.3, 0.08
            )
            Fx.spawn(world, 
                HhaParticles.HOLY_FLAME,
                blockHit.pos.x, blockHit.pos.y + 0.4, blockHit.pos.z,
                16, 0.25, 0.8, 0.25, 0.02
            )
            Fx.ring(world, blockHit.pos, 1.2, HhaParticles.LIGHT_MOTE, 16, 0.06)
            Fx.ring(world, blockHit.pos, 2.2, HhaParticles.HOLY_SPARK, 20, 0.09)
            Fx.ring(world, blockHit.pos, 3.2, HhaParticles.LIGHT_MOTE, 24, 0.12)
        }
        Fx.spiral(world, player.entityPos, 0.7, 1.8, HhaParticles.HOLY_FLAME)
        Fx.ring(world, player.entityPos.add(0.0, 0.1, 0.0), 1.1, HhaParticles.HOLY_SPARK, 16, 0.05)
        world.playSound(null, player.blockPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.6f)
        world.playSound(null, player.blockPos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5f, 0.8f)
        world.playSound(null, player.blockPos, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.9f, 1.4f)
    }

    /** Purify: heilt 4 HP und entfernt alle negativen Statuseffekte. */
    private fun purify(world: ServerWorld, ally: LivingEntity) {
        ally.heal(HhaConfig.numF("purify_heal"))
        val harmful = ally.statusEffects
            .filter { it.effectType.value().category == StatusEffectCategory.HARMFUL }
            .map { it.effectType }
        for (effect in harmful) {
            ally.removeStatusEffect(effect)
        }
        Fx.spawn(world, 
            HhaParticles.HOLY_SPARK,
            ally.x, ally.y + ally.height + 0.3, ally.z,
            4, 0.3, 0.2, 0.3, 0.0
        )
        Fx.spawn(world, 
            HhaParticles.HOLY_SPARK,
            ally.x, ally.y + ally.height * 0.5, ally.z,
            10, 0.4, 0.5, 0.4, 0.02
        )
        Fx.spawn(world, 
            HhaParticles.FEATHER,
            ally.x, ally.y + ally.height + 0.5, ally.z,
            3, 0.3, 0.1, 0.3, 0.0
        )
    }

    /** Heaven's Sword Aktiv: Lichtwelle schleudert alle Gegner in der Linie weg. */
    fun lightWave(world: ServerWorld, player: ServerPlayerEntity) {
        val start = player.eyePos
        // Volle Blickrichtung — die Welle (Partikel & Treffer) folgt jetzt auch nach oben/unten.
        val direction = player.getRotationVec(1.0f).normalize()
        val knockback = HhaConfig.num("light_wave_knockback")

        val flung = HashSet<UUID>()
        var d = 0.5
        while (d <= WAVE_RANGE) {
            val point = start.add(direction.multiply(d))
            Fx.spawn(world, HhaParticles.LIGHT_MOTE, point.x, point.y - 0.4, point.z, 4, 0.5, 0.4, 0.5, 0.02)
            Fx.spawn(world, HhaParticles.HOLY_SPARK, point.x, point.y - 0.3, point.z, 2, 0.5, 0.3, 0.5, 0.0)
            Fx.spawn(world, HhaParticles.FEATHER, point.x, point.y + 0.2, point.z, 1, 0.5, 0.3, 0.5, 0.0)

            val hitBox = Box.of(point, 3.0, 3.0, 3.0)
            for (entity in world.getEntitiesByClass(LivingEntity::class.java, hitBox, { it.isAlive && it != player })) {
                if (!Targeting.shouldHarm(player, entity) || !flung.add(entity.uuid)) continue
                // Schub entlang der Blickrichtung, mit garantiertem Auftrieb.
                entity.velocity = Vec3d(
                    direction.x * knockback,
                    (direction.y * knockback).coerceAtLeast(0.0) + 0.55,
                    direction.z * knockback,
                )
                entity.velocityDirty = true
                entity.damage(world, world.damageSources.playerAttack(player), HhaConfig.numF("light_wave_damage"))
            }
            d += 0.75
        }

        world.playSound(null, player.blockPos, SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value(), SoundCategory.PLAYERS, 1.0f, 1.1f)
        world.playSound(null, player.blockPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 1.5f)
    }

    private fun actionBar(player: ServerPlayerEntity, key: String) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true)
    }

    /** Orthonormalbasis quer zum Strahl — für die Helix-Stränge. */
    private fun beamBasis(direction: Vec3d): Pair<Vec3d, Vec3d> {
        val ref = if (abs(direction.y) > 0.98) Vec3d(1.0, 0.0, 0.0) else Vec3d(0.0, 1.0, 0.0)
        val right = direction.crossProduct(ref).normalize()
        val up = right.crossProduct(direction).normalize()
        return right to up
    }
}

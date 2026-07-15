package dev.henny.hha.logic

import dev.henny.hha.Hha
import dev.henny.hha.HhaParticles
import dev.henny.hha.api.event.HhaEvents
import dev.henny.hha.api.set.HhaSets
import dev.henny.hha.config.HhaConfig
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.PiglinBruteEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import java.util.UUID

/**
 * Alle passiven Effekte des Hells Sets — läuft am Ende jedes Server-Ticks.
 */
object PassiveEffects {

    private val SET_HEALTH_MODIFIER_ID: Identifier = Hha.id("set_bonus_health")

    /** Spieler, deren Undying Rage gerade aktiv ist (für Trigger-Effekte). */
    private val raging = HashSet<UUID>()

    /** Spieler, deren Warlord's Barrier gerade aktiv ist (für Trigger-Effekte). */
    private val barrierActive = HashSet<UUID>()

    /** Pro Spieler: welche registrierten Sets aktuell komplett getragen werden. */
    private val fullSets = HashMap<UUID, MutableSet<net.minecraft.util.Identifier>>()

    fun tick(server: MinecraftServer) {
        for (world in server.worlds) {
            for (player in world.players) {
                if (player.isSpectator || player.isDead) continue
                tickPlayer(world, player)
            }
            EmberTrail.tick(world)
            TempLava.tick(world)
            PullTracker.tick(world)
            GrappleBounce.tick(world)
            ProjectileGrapple.tick(world)
            BruteAllies.tick(world)
            SoulCamp.tick(world)
            LightTrail.tick(world)
            UltraMode.tick(world)
        }
        SwordBuff.tick(server)
    }

    private fun tickPlayer(world: ServerWorld, player: ServerPlayerEntity) {
        val helmet = HellSet.hasHelmet(player)
        val chest = HellSet.hasChestplate(player)
        val boots = HellSet.hasBoots(player)
        val full = helmet && chest && boots && HellSet.hasLeggings(player)

        val hHelmet = HeavenSet.hasHelmet(player)
        val hChest = HeavenSet.hasChestplate(player)
        val hBoots = HeavenSet.hasBoots(player)
        val hFull = hHelmet && hChest && hBoots && HeavenSet.hasLeggings(player)

        val low = player.health < HhaConfig.numF("trail_max_health")

        if (boots && low && HhaConfig.enabled("ember_trail") && player.isSprinting && player.isOnGround) {
            EmberTrail.place(world, player)
        } else {
            EmberTrail.stopTrail(player.uuid)
        }

        if (hBoots && low && HhaConfig.enabled("light_trail") && player.isOnGround) {
            LightTrail.record(world, player)
        }

        AirJumps.tickReset(player)

        if ((hHelmet || hChest || hBoots || HeavenSet.hasLeggings(player)) && world.time % 2L == 0L) {
            val cycle = (world.time % 44L) / 44.0
            val glowY = player.y + 0.15 + cycle * (player.height - 0.25)
            val angle = world.time * 0.45
            world.spawnParticles(
                HhaParticles.LIGHT_MOTE,
                player.x + kotlin.math.cos(angle) * 0.42,
                glowY,
                player.z + kotlin.math.sin(angle) * 0.42,
                1, 0.0, 0.0, 0.0, 0.0
            )
        }

        if ((helmet || chest || boots || HellSet.hasLeggings(player)) && world.time % 2L == 0L) {
            val cycle = ((world.time + 22L) % 44L) / 44.0
            val glowY = player.y + 0.15 + cycle * (player.height - 0.25)
            val angle = world.time * 0.45 + Math.PI
            world.spawnParticles(
                HhaParticles.SOUL_MOTE,
                player.x + kotlin.math.cos(angle) * 0.42,
                glowY,
                player.z + kotlin.math.sin(angle) * 0.42,
                1, 0.0, 0.0, 0.0, 0.0
            )
        }

        if (HhaConfig.enabled("set_flames")) {
            tickEquipmentFlames(world, player)
        }

        if (world.time % 5L == 0L && player.isOnGround && !player.isSneaking &&
            HellSet.hasLeggings(player) &&
            world.getFluidState(player.blockPos.down()).isIn(net.minecraft.registry.tag.FluidTags.LAVA)
        ) {
            world.spawnParticles(
                HhaParticles.EMBER_SPARK,
                player.x, player.y + 0.1, player.z,
                2, 0.3, 0.05, 0.3, 0.02
            )
        }

        if (hFull && HhaConfig.enabled("grace") && player.isSneaking && !player.isOnGround && player.velocity.y < 0) {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.SLOW_FALLING, 10, 0, true, false, false)
            )
            if (world.time % 3L == 0L) {
                world.spawnParticles(
                    HhaParticles.FEATHER,
                    player.x, player.y + 0.4, player.z,
                    1, 0.35, 0.15, 0.35, 0.0
                )
            }
        }

        tickApi(world, player)

        if (world.time % 20L != 0L) return

        if (full && HhaConfig.enabled("hellforged") && (player.isOnFire || player.isInLava)) {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, false, true)
            )
        }

        if (helmet || hHelmet) {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, true)
            )
        }
        if (helmet && HhaConfig.enabled("undying_rage")) {
            tickUndyingRage(world, player)
        } else {
            raging.remove(player.uuid)
        }
        if (hHelmet && HhaConfig.enabled("halo")) {
            tickHalo(world, player)
        }

        if (chest || hChest) {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 0, true, false, true)
            )
        }
        if (chest && HhaConfig.enabled("warlords_barrier")) {
            tickWarlordsBarrier(world, player)
        } else {
            barrierActive.remove(player.uuid)
        }

        if (boots || hBoots) {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.SPEED, 60, 0, true, false, true)
            )
        }

        applySetHealthBonus(player, (full || hFull) && HhaConfig.enabled("set_health_bonus"))
    }

    /** Addon-Set-Ticker und API-Events — jeden Tick, unabhängig vom Sekundenraster. */
    private fun tickApi(world: ServerWorld, player: ServerPlayerEntity) {
        val worn = fullSets.getOrPut(player.uuid) { HashSet() }
        for (set in HhaSets.all()) {
            val state = set.state(player)
            if (state.anyPiece) set.ticker?.tick(world, player, state)
            if (state.full) {
                if (worn.add(set.id)) HhaEvents.FULL_SET_CHANGED.invoker().onChanged(player, set, true)
            } else if (worn.remove(set.id)) {
                HhaEvents.FULL_SET_CHANGED.invoker().onChanged(player, set, false)
            }
        }
        HhaEvents.PLAYER_TICK.invoker().onTick(world, player)
    }

    /** Wo an der Rüstung eine Flamme züngeln darf: Partikeltyp, Höhe und Radius. */
    private class FlameSpot(
        val particle: net.minecraft.particle.SimpleParticleType,
        val y: Double,
        val radius: Double,
    )

    /** Animierte Seelenfeuer-/Lichtflammen aus getragener Set-Rüstung und gehaltenen Set-Waffen. */
    private fun tickEquipmentFlames(world: ServerWorld, player: ServerPlayerEntity) {
        if (world.time % 3L == 0L) {
            val spots = ArrayList<FlameSpot>(8)
            if (HellSet.hasHelmet(player)) spots.add(FlameSpot(HhaParticles.SOUL_FLAME, 1.62, 0.24))
            if (HellSet.hasChestplate(player)) spots.add(FlameSpot(HhaParticles.SOUL_FLAME, 1.22, 0.38))
            if (HellSet.hasLeggings(player)) spots.add(FlameSpot(HhaParticles.SOUL_FLAME, 0.62, 0.3))
            if (HellSet.hasBoots(player)) spots.add(FlameSpot(HhaParticles.SOUL_FLAME, 0.08, 0.26))
            if (HeavenSet.hasHelmet(player)) spots.add(FlameSpot(HhaParticles.HOLY_FLAME, 1.62, 0.24))
            if (HeavenSet.hasChestplate(player)) spots.add(FlameSpot(HhaParticles.HOLY_FLAME, 1.22, 0.38))
            if (HeavenSet.hasLeggings(player)) spots.add(FlameSpot(HhaParticles.HOLY_FLAME, 0.62, 0.3))
            if (HeavenSet.hasBoots(player)) spots.add(FlameSpot(HhaParticles.HOLY_FLAME, 0.08, 0.26))
            if (spots.isNotEmpty()) {
                val spot = spots[world.random.nextInt(spots.size)]
                val angle = world.random.nextDouble() * Math.PI * 2.0
                world.spawnParticles(
                    spot.particle,
                    player.x + kotlin.math.cos(angle) * spot.radius,
                    player.y + spot.y + world.random.nextDouble() * 0.15,
                    player.z + kotlin.math.sin(angle) * spot.radius,
                    1, 0.02, 0.05, 0.02, 0.0
                )
            }
        }

        if (HellSet.hasChestplate(player) && world.time % 3L == 0L) {
            val yaw = Math.toRadians(player.bodyYaw.toDouble())
            for (side in intArrayOf(-1, 1)) {
                val armX = -kotlin.math.cos(yaw) * 0.45 * side
                val armZ = -kotlin.math.sin(yaw) * 0.45 * side
                world.spawnParticles(
                    HhaParticles.SOUL_FLAME,
                    player.x + armX,
                    player.y + 1.05 + world.random.nextDouble() * 0.35,
                    player.z + armZ,
                    1, 0.03, 0.08, 0.03, 0.0
                )
            }
        }

        if (world.time % 4L == 0L) {
            emitHandFlame(world, player, net.minecraft.util.Hand.MAIN_HAND)
            emitHandFlame(world, player, net.minecraft.util.Hand.OFF_HAND)
        }
    }

    private fun emitHandFlame(world: ServerWorld, player: ServerPlayerEntity, hand: net.minecraft.util.Hand) {
        val stack = player.getStackInHand(hand)
        val particle = when {
            stack.isOf(dev.henny.hha.HhaItems.HELLS_SWORD) ||
                stack.isOf(dev.henny.hha.HhaItems.HELLS_MACE) -> HhaParticles.SOUL_FLAME
            stack.isOf(dev.henny.hha.HhaItems.HEAVENS_SWORD) ||
                stack.isOf(dev.henny.hha.HhaItems.HEAVENS_MACE) -> HhaParticles.HOLY_FLAME
            else -> return
        }
        val rightSide = (hand == net.minecraft.util.Hand.MAIN_HAND) ==
            (player.mainArm == net.minecraft.util.Arm.RIGHT)
        val side = if (rightSide) 1.0 else -1.0
        val yaw = Math.toRadians(player.bodyYaw.toDouble())
        val handX = -kotlin.math.cos(yaw) * 0.38 * side - kotlin.math.sin(yaw) * 0.25
        val handZ = -kotlin.math.sin(yaw) * 0.38 * side + kotlin.math.cos(yaw) * 0.25
        world.spawnParticles(
            particle,
            player.x + handX,
            player.y + 0.95 + world.random.nextDouble() * 0.2,
            player.z + handZ,
            1, 0.03, 0.06, 0.03, 0.0
        )
    }

    /** Heaven-Helm: Halo — getrustete Spieler in 5 Blöcken bekommen Regeneration I (refresht sich). */
    private fun tickHalo(world: ServerWorld, player: ServerPlayerEntity) {
        val nearby = world.getEntitiesByClass(
            ServerPlayerEntity::class.java,
            Box.of(player.entityPos, 10.0, 10.0, 10.0)
        ) {
            it.isAlive && it != player && it.squaredDistanceTo(player) <= 25.0 &&
                Targeting.isFriendly(player, it)
        }

        for (other in nearby) {
            other.addStatusEffect(
                StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, true)
            )
        }
        val haloY = player.y + player.height + 0.35
        val phase = world.time * 0.12
        for (i in 0 until 5) {
            val angle = phase + i * (Math.PI * 2.0 / 5.0)
            world.spawnParticles(
                HhaParticles.HOLY_SPARK,
                player.x + kotlin.math.cos(angle) * 0.42,
                haloY,
                player.z + kotlin.math.sin(angle) * 0.42,
                1, 0.0, 0.0, 0.0, 0.0
            )
        }
    }

    /** Helm: unter der Undying-Rage-Schwelle (default 3 Herzen) Speed II statt Speed I. */
    private fun tickUndyingRage(world: ServerWorld, player: ServerPlayerEntity) {
        if (player.health <= HhaConfig.numF("undying_rage_health")) {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.SPEED, 60, 1, true, false, true)
            )
            world.spawnParticles(
                HhaParticles.HELLFIRE,
                player.x, player.y + 0.9, player.z,
                3, 0.35, 0.5, 0.35, 0.0
            )
            if (raging.add(player.uuid)) {
                world.playSound(
                    null, player.blockPos, SoundEvents.ENTITY_BLAZE_AMBIENT,
                    SoundCategory.PLAYERS, 1.2f, 0.5f
                )
                world.playSound(
                    null, player.blockPos, SoundEvents.ENTITY_GHAST_SCREAM,
                    SoundCategory.PLAYERS, 0.6f, 1.5f
                )
                Fx.ring(world, player.entityPos.add(0.0, 0.2, 0.0), 1.3, HhaParticles.HELLFIRE, 18, 0.08)
                Fx.spiral(world, player.entityPos, 0.9, 2.0, HhaParticles.HELLFIRE)
                world.spawnParticles(
                    HhaParticles.INFERNAL_BURST,
                    player.x, player.y + 1.0, player.z,
                    1, 0.0, 0.0, 0.0, 0.0
                )
                world.spawnParticles(
                    HhaParticles.EMBER_SPARK,
                    player.x, player.y + 1.0, player.z,
                    12, 0.4, 0.7, 0.4, 0.02
                )
            }
        } else {
            raging.remove(player.uuid)
        }
    }

    /** Brustplatte: 2 Absorptionsherzen + Resistance I solange lebende Brutes in der Nähe sind. */
    private fun tickWarlordsBarrier(world: ServerWorld, player: ServerPlayerEntity) {
        val range = HhaConfig.num("barrier_range")
        val brutesNearby = world.getEntitiesByClass(
            PiglinBruteEntity::class.java,
            Box.of(player.entityPos, range * 2, 16.0, range * 2)
        ) { it.isAlive }.isNotEmpty()

        if (brutesNearby) {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.ABSORPTION, 70, 0, true, false, true)
            )
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.RESISTANCE, 70, 0, true, false, true)
            )
            if (barrierActive.add(player.uuid)) {
                world.playSound(
                    null, player.blockPos, SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY,
                    SoundCategory.PLAYERS, 0.8f, 0.8f
                )
                world.spawnParticles(
                    HhaParticles.SOOT,
                    player.x, player.y + 1.0, player.z,
                    14, 0.5, 0.6, 0.5, 0.02
                )
                world.spawnParticles(
                    HhaParticles.EMBER_SPARK,
                    player.x, player.y + 1.0, player.z,
                    10, 0.5, 0.6, 0.5, 0.03
                )
            }
        } else {
            barrierActive.remove(player.uuid)
        }
    }

    /** Set-Bonus: 20 Herzen (Basis 10 + 10 extra). */
    private fun applySetHealthBonus(player: ServerPlayerEntity, full: Boolean) {
        val instance = player.getAttributeInstance(EntityAttributes.MAX_HEALTH) ?: return
        val has = instance.hasModifier(SET_HEALTH_MODIFIER_ID)
        if (full && !has) {
            instance.addTemporaryModifier(
                EntityAttributeModifier(
                    SET_HEALTH_MODIFIER_ID, 20.0,
                    EntityAttributeModifier.Operation.ADD_VALUE
                )
            )
        } else if (!full && has) {
            instance.removeModifier(SET_HEALTH_MODIFIER_ID)
            if (player.health > player.maxHealth) player.health = player.maxHealth
        }
    }
}

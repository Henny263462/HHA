package dev.henny.hha.logic

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.RegistryKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

/**
 * Heaven's Boots: Heaven's Step — eine Lichtspur beim Laufen (nur Partikel,
 * keine Blockänderungen). Feinde in der Spur bekommen Slowness I für 2 Sekunden.
 */
object LightTrail {

    private const val TRAIL_LIFETIME = 60L
    private const val SLOW_INTERVAL = 10L

    private data class TrailEntry(val expiry: Long, val owner: java.util.UUID)

    private val trail = HashMap<RegistryKey<World>, HashMap<BlockPos, TrailEntry>>()

    fun record(world: ServerWorld, player: ServerPlayerEntity) {
        if (player.velocity.horizontalLengthSquared() < 0.003) return
        val pos = player.blockPos.toImmutable()
        val map = trail.getOrPut(world.registryKey) { HashMap() }
        val isNew = pos !in map
        map[pos] = TrailEntry(world.time + TRAIL_LIFETIME, player.uuid)
        if (isNew) {
            world.spawnParticles(
                dev.henny.hha.HhaParticles.LIGHT_MOTE,
                pos.x + 0.5, pos.y + 0.2, pos.z + 0.5,
                4, 0.3, 0.1, 0.3, 0.01
            )
            world.spawnParticles(
                ParticleTypes.END_ROD,
                pos.x + 0.5, pos.y + 0.25, pos.z + 0.5,
                2, 0.25, 0.05, 0.25, 0.0
            )
        }
    }

    fun tick(world: ServerWorld) {
        val map = trail[world.registryKey] ?: return
        if (map.isEmpty()) return

        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val (pos, entry) = iterator.next()
            if (world.time >= entry.expiry) {
                iterator.remove()
                continue
            }
            if (world.time % 2L == 0L) {
                world.spawnParticles(
                    dev.henny.hha.HhaParticles.LIGHT_MOTE,
                    pos.x + 0.5, pos.y + 0.15, pos.z + 0.5,
                    2, 0.3, 0.08, 0.3, 0.0
                )
            }
            if (world.time % 6L == 0L) {
                world.spawnParticles(
                    dev.henny.hha.HhaParticles.HOLY_SPARK,
                    pos.x + 0.5, pos.y + 0.3, pos.z + 0.5,
                    2, 0.25, 0.15, 0.25, 0.0
                )
            }
            if (world.time % 8L == 0L) {
                world.spawnParticles(
                    ParticleTypes.END_ROD,
                    pos.x + 0.5, pos.y + 0.25, pos.z + 0.5,
                    1, 0.25, 0.1, 0.25, 0.0
                )
            }
        }

        if (world.time % SLOW_INTERVAL != 0L) return
        for ((pos, entry) in map) {
            val owner = world.getPlayerByUuid(entry.owner) as? ServerPlayerEntity
            val box = Box(pos).expand(0.25, 0.25, 0.25)
            for (entity in world.getEntitiesByClass(LivingEntity::class.java, box, { it.isAlive })) {
                if (owner != null) {
                    if (!Targeting.shouldHarm(owner, entity)) continue
                } else {
                    if (entity is PlayerEntity || BruteAllies.isAlly(entity.uuid)) continue
                }
                entity.addStatusEffect(
                    StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, true)
                )
            }
        }
    }
}

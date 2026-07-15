package dev.henny.hha.logic

import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import java.util.UUID

/**
 * Hell's Mace: Wer sich mit der Kette an den Boden zieht, bounced beim
 * Aufkommen einmal ab — kleiner Hüpfer nach oben, kräftiger Dash geradeaus
 * in Blickrichtung.
 */
object GrappleBounce {

    private const val TIMEOUT_TICKS = 100L
    private const val DASH_STRENGTH = 1.7
    private const val UPWARD_BOOST = 0.9

    private data class Pending(val expiry: Long, var wasAirborne: Boolean)

    private val pending = HashMap<UUID, Pending>()

    /** Nach einem Boden-Grapple scharfschalten — die nächste Landung bounced. */
    fun arm(world: ServerWorld, player: ServerPlayerEntity) {
        pending[player.uuid] = Pending(world.time + TIMEOUT_TICKS, false)
    }

    fun tick(world: ServerWorld) {
        if (pending.isEmpty()) return
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, state) = iterator.next()
            if (world.time >= state.expiry) {
                iterator.remove()
                continue
            }
            val player = world.getPlayerByUuid(uuid) as? ServerPlayerEntity ?: continue
            if (!player.isAlive) {
                iterator.remove()
                continue
            }
            if (!player.isOnGround) {
                state.wasAirborne = true
                continue
            }
            if (!state.wasAirborne) continue
            iterator.remove()

            val look = player.getRotationVec(1.0f)
            val forward = Vec3d(look.x, 0.0, look.z).normalize()
            Fx.launchPlayer(
                player,
                Vec3d(forward.x * DASH_STRENGTH, UPWARD_BOOST, forward.z * DASH_STRENGTH)
            )
            player.fallDistance = 0.0

            val bounceDamage = dev.henny.hha.config.HhaConfig.numF("bounce_damage")
            if (bounceDamage > 0f) {
                val blast = net.minecraft.util.math.Box.of(player.entityPos, 5.0, 3.0, 5.0)
                for (entity in world.getEntitiesByClass(
                    net.minecraft.entity.LivingEntity::class.java, blast, { it.isAlive && it != player })
                ) {
                    if (!Targeting.shouldHarm(player, entity)) continue
                    entity.damage(world, world.damageSources.playerAttack(player), bounceDamage)
                    Fx.spawn(world, 
                        dev.henny.hha.HhaParticles.EMBER_SPARK,
                        entity.x, entity.y + entity.height * 0.5, entity.z,
                        6, 0.25, 0.3, 0.25, 0.03
                    )
                }
            }

            Fx.ring(world, player.entityPos.add(0.0, 0.1, 0.0), 1.0, dev.henny.hha.HhaParticles.HELLFIRE, 14, 0.05)
            Fx.spawn(world, 
                dev.henny.hha.HhaParticles.INFERNAL_BURST,
                player.x, player.y + 0.5, player.z,
                1, 0.0, 0.0, 0.0, 0.0
            )
            Fx.spawn(world, 
                dev.henny.hha.HhaParticles.EMBER_SPARK,
                player.x, player.y + 0.3, player.z,
                10, 0.5, 0.2, 0.5, 0.05
            )
            world.playSound(
                null, player.blockPos, SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value(),
                SoundCategory.PLAYERS, 0.8f, 1.3f
            )
            world.playSound(
                null, player.blockPos, SoundEvents.BLOCK_CHAIN_BREAK,
                SoundCategory.PLAYERS, 0.9f, 0.8f
            )
        }
    }
}

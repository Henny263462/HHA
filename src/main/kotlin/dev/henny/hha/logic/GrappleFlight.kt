package dev.henny.hha.logic

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import java.util.UUID
import kotlin.math.min

/**
 * Kette loslassen: Wer während eines Block-Grapple-Flugs sneakt, lässt die
 * Kette los — der Schwung wird gekappt und der Spieler fällt. Beendet auch
 * den scharfgeschalteten Bounce, weil die Kette ja losgelassen wurde.
 */
object GrappleFlight {

    private const val TIMEOUT_TICKS = 60L

    private class Flight(
        val expiry: Long,
        var wasAirborne: Boolean = false,
        /** Erst nach einmaligem Nicht-Sneaken scharf — wer sneakend abfeuert, fliegt trotzdem los. */
        var sneakArmed: Boolean = false,
    )

    private val flights = HashMap<UUID, Flight>()

    /** Beim Block-Grapple-Start aufrufen — der Flug ist ab jetzt per Sneak abbrechbar. */
    fun start(world: ServerWorld, player: ServerPlayerEntity) {
        flights[player.uuid] = Flight(world.time + TIMEOUT_TICKS, sneakArmed = !player.isSneaking)
    }

    fun tick(world: ServerWorld) {
        if (flights.isEmpty()) return
        val iterator = flights.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, flight) = iterator.next()
            val player = world.getPlayerByUuid(uuid) as? ServerPlayerEntity
            if (player == null || !player.isAlive || world.time >= flight.expiry) {
                iterator.remove()
                continue
            }
            if (player.isOnGround) {
                // Gelandet (nach mindestens einem Tick in der Luft) → Flug vorbei.
                if (flight.wasAirborne) iterator.remove()
                continue
            }
            flight.wasAirborne = true
            if (!player.isSneaking) {
                flight.sneakArmed = true
            } else if (flight.sneakArmed) {
                release(world, player)
                iterator.remove()
            }
        }
    }

    /** Schwung kappen und fallen lassen; Feedback wie eine reißende Kette. */
    fun release(world: ServerWorld, player: ServerPlayerEntity) {
        val velocity = player.velocity
        Fx.launchPlayer(
            player,
            Vec3d(velocity.x * 0.15, min(velocity.y, 0.0), velocity.z * 0.15),
        )
        GrappleBounce.disarm(player.uuid)
        world.playSound(
            null, player.blockPos, SoundEvents.BLOCK_CHAIN_BREAK,
            SoundCategory.PLAYERS, 1.0f, 0.7f
        )
        Fx.spawn(world,
            dev.henny.hha.HhaParticles.SOOT,
            player.x, player.y + 0.8, player.z,
            5, 0.3, 0.3, 0.3, 0.02
        )
    }
}

package dev.henny.hha.logic

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.particle.ParticleEffect
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

/** Gemeinsame Effekt- und Bewegungs-Helfer. */
object Fx {

    /**
     * Setzt die Velocity eines Spielers UND synchronisiert sie zum Client.
     * `velocityDirty` allein schickt das Paket nur an andere Spieler — der
     * eigene Client würde die Bewegung sonst nie erfahren.
     */
    fun launchPlayer(player: ServerPlayerEntity, velocity: Vec3d) {
        player.velocity = velocity
        player.velocityDirty = true
        player.networkHandler.sendPacket(EntityVelocityUpdateS2CPacket(player))
    }

    /** Horizontaler Partikelring um einen Mittelpunkt. */
    fun ring(
        world: ServerWorld,
        center: Vec3d,
        radius: Double,
        particle: ParticleEffect,
        points: Int = 24,
        speed: Double = 0.02,
    ) {
        for (i in 0 until points) {
            val angle = (i.toDouble() / points) * Math.PI * 2.0
            world.spawnParticles(
                particle,
                center.x + cos(angle) * radius,
                center.y,
                center.z + sin(angle) * radius,
                1, 0.02, 0.05, 0.02, speed
            )
        }
    }

    /**
     * Partikelring in einer beliebig orientierten Ebene — aufgespannt durch
     * zwei orthonormale Achsen (z. B. quer zu einem Strahl).
     */
    fun orientedRing(
        world: ServerWorld,
        center: Vec3d,
        axisA: Vec3d,
        axisB: Vec3d,
        radius: Double,
        particle: ParticleEffect,
        points: Int = 12,
    ) {
        for (i in 0 until points) {
            val angle = (i.toDouble() / points) * Math.PI * 2.0
            val offset = axisA.multiply(cos(angle) * radius).add(axisB.multiply(sin(angle) * radius))
            world.spawnParticles(
                particle,
                center.x + offset.x,
                center.y + offset.y,
                center.z + offset.z,
                1, 0.0, 0.0, 0.0, 0.0
            )
        }
    }

    /** Aufsteigende Partikelspirale (2 Umdrehungen). */
    fun spiral(
        world: ServerWorld,
        center: Vec3d,
        radius: Double,
        height: Double,
        particle: ParticleEffect,
        points: Int = 30,
    ) {
        for (i in 0 until points) {
            val t = i.toDouble() / points
            val angle = t * Math.PI * 4.0
            world.spawnParticles(
                particle,
                center.x + cos(angle) * radius * (1.0 - t * 0.5),
                center.y + t * height,
                center.z + sin(angle) * radius * (1.0 - t * 0.5),
                1, 0.0, 0.0, 0.0, 0.0
            )
        }
    }
}

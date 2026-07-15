package dev.henny.hha.logic

import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.ai.brain.MemoryModuleType
import net.minecraft.entity.mob.PiglinBruteEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fire Camp: beschworene Piglin Brutes, die den Besitzer beschützen statt ihn anzugreifen.
 * Brutes nutzen Brain-AI, daher werden die Memories jeden Tick kuratiert.
 */
object BruteAllies {

    private const val LIFETIME_TICKS = 1800L

    private data class Ally(val owner: UUID, val expiry: Long)

    private val allies = HashMap<UUID, Ally>()

    @JvmStatic
    fun isAlly(uuid: UUID): Boolean = allies.containsKey(uuid)

    /** Besitzer greift an → alle eigenen Brutes stürzen sich sofort auf das Ziel. */
    fun onOwnerAttack(world: ServerWorld, owner: ServerPlayerEntity, target: LivingEntity) {
        if (allies.isEmpty()) return
        if (target == owner || isAlly(target.uuid) || target is PiglinBruteEntity) return
        if (isTrustedBy(owner.uuid, target)) return
        for ((uuid, ally) in allies) {
            if (ally.owner != owner.uuid) continue
            val brute = world.getEntity(uuid) as? PiglinBruteEntity ?: continue
            if (!brute.isAlive) continue
            brute.brain.remember(MemoryModuleType.ATTACK_TARGET, target, 400L)
            brute.brain.remember(MemoryModuleType.ANGRY_AT, target.uuid, 600L)
        }
    }

    fun spawn(world: ServerWorld, player: ServerPlayerEntity, count: Int): List<UUID> {
        val spawned = ArrayList<UUID>(count)
        for (i in 0 until count) {
            val brute = EntityType.PIGLIN_BRUTE.create(world, SpawnReason.MOB_SUMMONED) ?: return spawned
            val angle = (i.toDouble() / count) * Math.PI * 2.0
            val x = player.x + cos(angle) * 2.0
            val z = player.z + sin(angle) * 2.0
            brute.refreshPositionAndAngles(x, player.y, z, world.random.nextFloat() * 360f, 0f)
            brute.setImmuneToZombification(true)
            brute.setPersistent()
            brute.addCommandTag("hha_ally")
            val axe = net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_AXE)
            val sharpness = world.registryManager
                .getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                .getOrThrow(net.minecraft.enchantment.Enchantments.SHARPNESS)
            axe.addEnchantment(sharpness, 3)
            brute.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, axe)
            brute.setEquipmentDropChance(net.minecraft.entity.EquipmentSlot.MAINHAND, 0.0f)
            brute.addStatusEffect(
                net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.FIRE_RESISTANCE,
                    LIFETIME_TICKS.toInt(), 0, false, false, false
                )
            )
            world.spawnEntity(brute)
            allies[brute.uuid] = Ally(player.uuid, world.time + LIFETIME_TICKS)
            spawned.add(brute.uuid)

            Fx.spawn(world, 
                dev.henny.hha.HhaParticles.SOUL_FLAME, x, player.y + 1.0, z,
                14, 0.4, 0.8, 0.4, 0.02
            )
            Fx.spawn(world, 
                dev.henny.hha.HhaParticles.SOUL_MOTE, x, player.y + 1.2, z,
                8, 0.4, 0.7, 0.4, 0.02
            )
        }
        world.playSound(
            null, player.blockPos, SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY,
            SoundCategory.HOSTILE, 1.0f, 1.0f
        )
        return spawned
    }

    fun tick(world: ServerWorld) {
        if (allies.isEmpty()) return
        val iterator = allies.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, ally) = iterator.next()
            val brute = world.getEntity(uuid) as? PiglinBruteEntity ?: run {
                if (world.time >= ally.expiry) iterator.remove()
                continue
            }
            if (!brute.isAlive) {
                iterator.remove()
                continue
            }
            if (world.time >= ally.expiry) {
                Fx.spawn(world, 
                    dev.henny.hha.HhaParticles.SOOT, brute.x, brute.y + 1.0, brute.z,
                    14, 0.3, 0.5, 0.3, 0.03
                )
                brute.discard()
                iterator.remove()
                continue
            }

            if (world.time % 8L == 0L) {
                Fx.spawn(world, 
                    dev.henny.hha.HhaParticles.SOUL_FLAME,
                    brute.x, brute.y + 1.3, brute.z,
                    1, 0.25, 0.45, 0.25, 0.0
                )
            }

            val owner = world.getPlayerByUuid(ally.owner) as? ServerPlayerEntity ?: continue
            curateTargets(brute, owner)
        }
    }

    /** Besitzer, Verbündete & getrustete Spieler niemals angreifen; Angreifer des Besitzers attackieren. */
    private fun curateTargets(brute: PiglinBruteEntity, owner: ServerPlayerEntity) {
        val brain = brute.brain
        val target = brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)
        if (target != null && (target == owner || isAlly(target.uuid) || isTrustedBy(owner.uuid, target))) {
            brain.forget(MemoryModuleType.ATTACK_TARGET)
            brain.forget(MemoryModuleType.ANGRY_AT)
        }
        val angryAt = brain.getOptionalRegisteredMemory(MemoryModuleType.ANGRY_AT).orElse(null)
        if (angryAt != null && (angryAt == owner.uuid || isAlly(angryAt) || Trust.isTrusted(owner.uuid, angryAt))) {
            brain.forget(MemoryModuleType.ANGRY_AT)
        }

        if (target == null || !target.isAlive) {
            val threat = pickThreat(owner, brute)
            if (threat != null) {
                brain.remember(MemoryModuleType.ATTACK_TARGET, threat, 200L)
            }
        }
    }

    /** Getrustete Spieler des Besitzers sind für dessen Brutes tabu. */
    private fun isTrustedBy(owner: UUID, target: LivingEntity): Boolean =
        target is net.minecraft.entity.player.PlayerEntity && Trust.isTrusted(owner, target.uuid)

    private fun pickThreat(owner: ServerPlayerEntity, brute: PiglinBruteEntity): LivingEntity? {
        val candidates = listOfNotNull(owner.attacker, owner.attacking)
        return candidates.firstOrNull {
            it.isAlive && it != owner && it != brute && !isAlly(it.uuid) &&
                it !is PiglinBruteEntity && !isTrustedBy(owner.uuid, it)
        }
    }
}

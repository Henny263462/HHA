package dev.henny.hha.logic

import net.minecraft.block.Blocks
import net.minecraft.registry.RegistryKey
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/** Kurzlebige Lava-Blöcke (Magma Stomp) — werden nach Ablauf wieder entfernt. */
object TempLava {

    private val lava = HashMap<RegistryKey<World>, HashMap<BlockPos, Long>>()

    fun place(world: ServerWorld, pos: BlockPos, lifetimeTicks: Long) {
        if (!world.getBlockState(pos).isAir) return
        world.setBlockState(pos, Blocks.LAVA.defaultState)
        lava.getOrPut(world.registryKey) { HashMap() }[pos.toImmutable()] = world.time + lifetimeTicks
    }

    fun tick(world: ServerWorld) {
        val map = lava[world.registryKey] ?: return
        if (map.isEmpty()) return

        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val (pos, expiry) = iterator.next()
            if (world.time < expiry) continue
            if (world.getBlockState(pos).isOf(Blocks.LAVA)) {
                world.removeBlock(pos, false)
            }
            iterator.remove()
        }
    }
}

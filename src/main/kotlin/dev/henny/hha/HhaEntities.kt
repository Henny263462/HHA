package dev.henny.hha

import dev.henny.hha.entity.HeavensMaceEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys

object HhaEntities {

    private val HEAVENS_MACE_KEY: RegistryKey<EntityType<*>> =
        RegistryKey.of(RegistryKeys.ENTITY_TYPE, Hha.id("heavens_mace"))

    val HEAVENS_MACE: EntityType<HeavensMaceEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        HEAVENS_MACE_KEY,
        EntityType.Builder.create({ type: EntityType<HeavensMaceEntity>, world -> HeavensMaceEntity(type, world) }, SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f)
            .maxTrackingRange(64)
            .trackingTickInterval(2)
            .build(HEAVENS_MACE_KEY)
    )

    fun init() {
    }
}

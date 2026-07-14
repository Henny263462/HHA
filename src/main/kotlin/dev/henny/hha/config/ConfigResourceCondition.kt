package dev.henny.hha.config

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.henny.hha.Hha
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions
import net.minecraft.registry.RegistryOps

/**
 * Custom-Resource-Condition "hha:config": Datapack-Einträge (z. B. Rezepte) laden
 * nur, wenn der angegebene Config-Toggle aktiv ist. Nutzung im JSON:
 *
 *   "fabric:load_conditions": [{ "condition": "hha:config", "key": "ingot_recipes" }]
 *
 * Nach einem Toggle-Wechsel greift die Änderung erst mit dem nächsten
 * Datapack-Reload — /hha toggle stößt den für ingot_recipes selbst an.
 */
class ConfigResourceCondition(private val key: String) : ResourceCondition {

    override fun getType(): ResourceConditionType<*> = TYPE

    override fun test(registryInfo: RegistryOps.RegistryInfoGetter?): Boolean = HhaConfig.enabled(key)

    companion object {
        private val CODEC: MapCodec<ConfigResourceCondition> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.fieldOf("key").forGetter { it.key }
            ).apply(instance) { key -> ConfigResourceCondition(key) }
        }

        val TYPE: ResourceConditionType<ConfigResourceCondition> =
            ResourceConditionType.create(Hha.id("config"), CODEC)

        fun register() {
            ResourceConditions.register(TYPE)
        }
    }
}

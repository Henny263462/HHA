package dev.henny.hha.net

import dev.henny.hha.Hha
import dev.henny.hha.api.ability.AbilityTrigger
import dev.henny.hha.api.ability.HhaAbilities
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload

/** C2S-Payload: Spieler hat eine Fähigkeiten-Taste gedrückt. */
data class AbilityPayload(val ability: Int) : CustomPayload {

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        const val BEAM = 0
        const val UTILITY = 1
        const val AIR_JUMP = 2
        const val ULTRA = 3

        val ID = CustomPayload.Id<AbilityPayload>(Hha.id("ability"))
        val CODEC: PacketCodec<RegistryByteBuf, AbilityPayload> =
            PacketCodec.tuple(PacketCodecs.VAR_INT, AbilityPayload::ability, ::AbilityPayload)
    }
}

/** S2C-Payload: Ultra-Modus-Status fürs HUD (verbleibende Ticks, 0 = aus). */
data class UltraHudPayload(val remainingTicks: Int) : CustomPayload {

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID = CustomPayload.Id<UltraHudPayload>(Hha.id("ultra_hud"))
        val CODEC: PacketCodec<RegistryByteBuf, UltraHudPayload> =
            PacketCodec.tuple(PacketCodecs.VAR_INT, UltraHudPayload::remainingTicks, ::UltraHudPayload)
    }
}

object HhaNetworking {

    fun init() {
        PayloadTypeRegistry.playC2S().register(AbilityPayload.ID, AbilityPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(UltraHudPayload.ID, UltraHudPayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(AbilityPayload.ID) { payload, context ->
            val trigger = when (payload.ability) {
                AbilityPayload.BEAM -> AbilityTrigger.PRIMARY
                AbilityPayload.UTILITY -> AbilityTrigger.UTILITY
                AbilityPayload.AIR_JUMP -> AbilityTrigger.MOVEMENT
                AbilityPayload.ULTRA -> AbilityTrigger.ULTRA
                else -> return@registerGlobalReceiver
            }
            HhaAbilities.dispatch(context.player(), trigger)
        }
    }
}

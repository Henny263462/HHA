package dev.henny.hha.client

import dev.henny.hha.Hha
import dev.henny.hha.HhaEntities
import dev.henny.hha.HhaItems
import dev.henny.hha.net.AbilityPayload
import dev.henny.hha.net.UltraHudPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.EquipmentSlot
import net.minecraft.util.math.Vec3d
import org.lwjgl.glfw.GLFW

class HhaClient : ClientModInitializer {

    private lateinit var beamKey: KeyBinding
    private lateinit var utilityKey: KeyBinding
    private lateinit var ultraKey: KeyBinding
    private var jumpWasPressed = false
    private var airJumpsUsedClient = 0

    /** Erst nach Loslassen der Sprungtaste in der Luft darf der Doppelsprung zünden. */
    private var airJumpArmed = false

    override fun onInitializeClient() {
        EntityRendererRegistry.register(HhaEntities.HEAVENS_MACE) { context ->
            HeavensMaceEntityRenderer(context)
        }

        dev.henny.hha.client.particle.HhaParticleFactories.register()

        CooldownHud.register()

        loadClientAddons()

        ClientPlayNetworking.registerGlobalReceiver(UltraHudPayload.ID) { payload, _ ->
            CooldownHud.ultraRemainingTicks = payload.remainingTicks
        }

        val category = KeyBinding.Category.create(Hha.id("hells_set"))

        beamKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding("key.hha.lava_beam", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category)
        )
        utilityKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding("key.hha.fire_camp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category)
        )
        ultraKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding("key.hha.ultra", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, category)
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            CooldownHud.clientTick()

            val player = client.player ?: run {
                jumpWasPressed = false
                airJumpsUsedClient = 0
                CooldownHud.ultraRemainingTicks = 0
                return@register
            }

            while (beamKey.wasPressed()) {
                ClientPlayNetworking.send(AbilityPayload(AbilityPayload.BEAM))
            }
            while (utilityKey.wasPressed()) {
                ClientPlayNetworking.send(AbilityPayload(AbilityPayload.UTILITY))
            }
            while (ultraKey.wasPressed()) {
                ClientPlayNetworking.send(AbilityPayload(AbilityPayload.ULTRA))
            }

            val jumpPressed = client.options.jumpKey.isPressed
            if (player.isOnGround || player.isTouchingWater) {
                airJumpsUsedClient = 0
                airJumpArmed = false
            } else if (!jumpPressed) {
                airJumpArmed = true
            }
            if (jumpPressed && !jumpWasPressed && airJumpArmed &&
                !player.isOnGround && !player.isTouchingWater &&
                !player.abilities.flying && !player.hasVehicle() &&
                airJumpsUsedClient < 2 &&
                player.getEquippedStack(EquipmentSlot.LEGS).isOf(HhaItems.HEAVEN_LEGGINGS)
            ) {
                airJumpsUsedClient++
                val look = player.getRotationVec(1.0f)
                val v = player.velocity
                player.velocity = Vec3d(
                    v.x * 0.9 + look.x * 0.35,
                    0.62,
                    v.z * 0.9 + look.z * 0.35,
                )
                ClientPlayNetworking.send(AbilityPayload(AbilityPayload.AIR_JUMP))
            }
            jumpWasPressed = jumpPressed
        }
    }

    /** Lädt alle Mods mit `"hha_client"`-Entrypoint (Addon-Client-API). */
    private fun loadClientAddons() {
        val containers = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getEntrypointContainers("hha_client", dev.henny.hha.api.client.HhaClientAddon::class.java)
        for (container in containers) {
            val addonId = container.provider.metadata.id
            Hha.LOGGER.info("Lade HHA-Client-Addon: {}", addonId)
            container.entrypoint.onInitializeClient(ClientAddonContextImpl(addonId))
        }
    }

    private class ClientAddonContextImpl(
        override val addonId: String,
    ) : dev.henny.hha.api.client.HhaClientAddonContext {

        override fun registerHudCooldown(
            icon: net.minecraft.util.Identifier,
            accentColor: Int,
            provider: (net.minecraft.entity.player.PlayerEntity) -> net.minecraft.item.ItemStack?,
        ) {
            dev.henny.hha.api.client.HudCooldowns.register(
                dev.henny.hha.api.client.HudCooldowns.Entry(icon, accentColor, provider)
            )
        }
    }
}

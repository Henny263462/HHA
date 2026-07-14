package dev.henny.hha.client

import dev.henny.hha.entity.HeavensMaceEntity
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.FlyingItemEntityRenderer
import net.minecraft.client.render.entity.state.FlyingItemEntityRenderState
import net.minecraft.client.render.state.CameraRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis

/**
 * Geworfener Heaven's Mace: kein statisches Item-Billboard, sondern ein
 * wirbelnder, vergrößerter und vollhell leuchtender Hammer — wie eine
 * göttliche Wurfwaffe im Flug.
 */
class HeavensMaceEntityRenderer(ctx: EntityRendererFactory.Context) :
    FlyingItemEntityRenderer<HeavensMaceEntity>(ctx, SCALE, true) {

    override fun render(
        state: FlyingItemEntityRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        cameraState: CameraRenderState,
    ) {
        matrices.push()
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.age * SPIN_SPEED))
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(WOBBLE_TILT))
        super.render(state, matrices, queue, cameraState)
        matrices.pop()
    }

    companion object {
        private const val SCALE = 1.6f
        private const val SPIN_SPEED = 42f
        private const val WOBBLE_TILT = 12f
    }
}

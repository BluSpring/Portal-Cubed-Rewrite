package io.github.fusionflux.portalcubed.mixin.client;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.fusionflux.portalcubed.content.portal.PortalRenderer;
import io.github.fusionflux.portalcubed.framework.util.RenderingUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Shadow
	private ClientLevel level;

	@Inject(
		method = "renderLevel",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/Sheets;translucentCullBlockSheet()Lnet/minecraft/client/renderer/RenderType;",
			shift = At.Shift.BEFORE
		)
	)
	private void beforeTranslucent(
		PoseStack poseStack,
		float tickDelta,
		long finishNanoTime,
		boolean renderBlockOutline,
		Camera camera,
		GameRenderer gameRenderer,
		LightTexture lightTexture,
		Matrix4f projectionMatrix,
		CallbackInfo ci,
		@Local Frustum frustum
	) {
		PortalRenderer.render(poseStack, tickDelta, camera, frustum, level, (LevelRenderer) (Object) this, gameRenderer, projectionMatrix);
	}

	@WrapWithCondition(
		method = "renderLevel",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V",
			ordinal = 0,
			remap = false
		)
	)
	private boolean replaceClearingIfRenderingPortal(int mask, boolean checkError) {
		if (PortalRenderer.isRenderingView()) {
			GlStateManager._depthFunc(GL11.GL_ALWAYS);
			GL11.glDepthRange(1, 1);
			RenderingUtils.stencilClear(RenderingUtils.CLEAR_COLOR[0], RenderingUtils.CLEAR_COLOR[1], RenderingUtils.CLEAR_COLOR[2]);
			GlStateManager._depthFunc(GL11.GL_LEQUAL);
			GL11.glDepthRange(0, 1);
			return false;
		}
		return true;
	}
}

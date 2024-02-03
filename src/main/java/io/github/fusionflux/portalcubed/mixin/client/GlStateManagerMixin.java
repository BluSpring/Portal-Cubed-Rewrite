package io.github.fusionflux.portalcubed.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlStateManager;

import io.github.fusionflux.portalcubed.content.portal.PortalRenderer;
import io.github.fusionflux.portalcubed.framework.util.RenderingUtils;

@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
	@Inject(method = "_clearColor", at = @At("HEAD"), cancellable = true, remap = false)
	private static void captureClearColor(float r, float g, float b, float a, CallbackInfo ci) {
		if (PortalRenderer.isRenderingView()) {
			RenderingUtils.CLEAR_COLOR[0] = (int) (r * 255);
			RenderingUtils.CLEAR_COLOR[1] = (int) (g * 255);
			RenderingUtils.CLEAR_COLOR[2] = (int) (b * 255);
			ci.cancel();
		}
	}
}

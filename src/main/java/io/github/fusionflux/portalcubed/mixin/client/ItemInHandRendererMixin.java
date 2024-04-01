package io.github.fusionflux.portalcubed.mixin.client;

import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.fusionflux.portalcubed.content.cannon.CannonUseResult;
import io.github.fusionflux.portalcubed.content.cannon.ConstructionCannonItem;
import io.github.fusionflux.portalcubed.framework.extension.ItemInHandRendererExt;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin implements ItemInHandRendererExt {
	@Unique
	private static final float WIGGLE_STOP = (float) Math.PI * 16;

	@Unique
	private float constructionCannonRecoil;

	@Unique
	private float constructionCannonWiggle;
	@Unique
	private boolean constructionCannonWiggling;

	@Override
	public void pc$constructionCannonShoot(CannonUseResult useResult) {
		if (useResult.shouldRecoil()) {
			constructionCannonRecoil = 25;
		} else {
			constructionCannonRecoil = 0;
		}

		if (useResult.shouldWiggle()) {
			constructionCannonWiggle = WIGGLE_STOP;
		} else {
			constructionCannonWiggle = 0;
		}
	}

	@Inject(
		method = "renderArmWithItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			ordinal = 1,
			shift = At.Shift.BEFORE
		)
	)
	private void renderArmWithItem(
		AbstractClientPlayer player,
		float tickDelta,
		float pitch,
		InteractionHand hand,
		float swingProgress,
		ItemStack stack,
		float equipProgress,
		PoseStack matrices,
		MultiBufferSource vertexConsumers,
		int light,
		CallbackInfo ci
	) {
		if (stack.getItem() instanceof ConstructionCannonItem) {
			matrices.mulPose(Axis.XP.rotationDegrees(constructionCannonRecoil));
			matrices.translate(.1875, 0, 0);
			matrices.mulPose(Axis.ZP.rotationDegrees(Math.sin(constructionCannonWiggle / 6) * 6));
			matrices.translate(-.1875, 0, 0);
		}
		constructionCannonRecoil = Math.max(0, constructionCannonRecoil - (tickDelta * 1.4f));
		constructionCannonWiggle = Math.max(0, constructionCannonWiggle - tickDelta);
	}
}

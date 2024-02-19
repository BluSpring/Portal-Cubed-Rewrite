package io.github.fusionflux.portalcubed.content.portal;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.fusionflux.portalcubed.content.portal.manager.ClientPortalManager;
import io.github.fusionflux.portalcubed.framework.shape.VoxelShenanigans;
import io.github.fusionflux.portalcubed.framework.util.Color;
import io.github.fusionflux.portalcubed.framework.util.RenderingUtils;
import io.github.fusionflux.portalcubed.framework.util.RenderingUtils.Quad;
import io.github.fusionflux.portalcubed.mixin.client.GameRendererAccessor;
import io.github.fusionflux.portalcubed.mixin.client.LevelRendererAccessor;
import io.github.fusionflux.portalcubed.mixin.client.RenderSystemAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.FastColor;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class PortalRenderer {
	private static final Color RED = new Color(1, 0, 0, 1);
	private static final Color GREEN = new Color(0.5f, 1, 0.5f, 1);
	private static final Color BLUE = new Color(0, 0, 1, 1);
	private static final Color ORANGE = new Color(1, 0.5f, 0, 1);
	private static final Color PURPLE = new Color(0.5f, 0, 1, 1);
	private static final Color CYAN = new Color(0, 1, 1, 1);

	private static final Color PLANE_COLOR = new Color(1, 1, 1, 1);
	private static final Color ACTIVE_PLANE_COLOR = GREEN;

	private static final double OFFSET_FROM_WALL = 0.001;
	private static final Quad PORTAL_QUAD = Quad.create(DefaultVertexFormat.POSITION_COLOR_TEX, Quad.defaultUVS((i, consumer) -> consumer.color(1f, 1f, 1f, 1f)), 1, 2);

	private static final int MAX_VIEW_LAYERS = 1;
	private static int viewLayer = 0;

	public static boolean isViewRenderingEnabled() {
		return MAX_VIEW_LAYERS != 0;
	}

	public static boolean isRenderingView() {
		return viewLayer != 0;
	}

	public static void render(PoseStack matrices, float tickDelta, Camera camera, Frustum frustum, ClientLevel level, LevelRenderer levelRenderer, GameRenderer gameRenderer, Matrix4f projectionMatrix) {
		ClientPortalManager manager = ClientPortalManager.of(level);
		List<Portal> portals = manager.allPortals();
		if (portals.isEmpty())
			return;

		Vec3 camPos = camera.getPosition();
		matrices.pushPose();
		boolean renderDebug = Minecraft.getInstance().options.renderDebug;
		if (isViewRenderingEnabled()) {
			GlStateManager._enableDepthTest();
			RenderingUtils.STENCIL_TEST.enable();
		}
		float oldShaderFogStart = RenderSystem.getShaderFogStart();
		matrices.translate(-camPos.x, -camPos.y, -camPos.z);
		for (Portal portal : portals) {
			if (frustum.isVisible(portal.plane)) {
				renderPortal(portal, matrices, projectionMatrix, levelRenderer, gameRenderer, tickDelta);
				// if (renderDebug) {
				// 	renderPortalDebug(portal, context, matrices, vertexConsumers);
				// }
			}
		}
		matrices.popPose();

		if (isViewRenderingEnabled()) {
			if (isRenderingView()) {
				RenderingUtils.setupStencilToRenderIfValue(viewLayer);
			} else {
				RenderingUtils.STENCIL_TEST.disable();
				GlStateManager._clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
			}
			RenderSystem.setShaderFogStart(oldShaderFogStart);
		}
	}

	private static void renderPortal(Portal portal, PoseStack matrices, Matrix4f projectionMatrix, LevelRenderer levelRenderer, GameRenderer gameRenderer, float tickDelta) {
		matrices.pushPose();
		// translate to portal pos
		matrices.translate(portal.origin.x, portal.origin.y, portal.origin.z);
		// apply rotations
		matrices.mulPose(portal.rotation); // rotate towards facing direction
		matrices.mulPose(Axis.ZP.rotationDegrees(180));
		// slight offset so origin is center of portal
		matrices.translate(-0.5f, -1, 0);
		// small offset away from the wall to not z-fight
		matrices.translate(0, 0, -OFFSET_FROM_WALL);
		// RenderSystem.setShaderColor(FastColor.ABGR32.red(portal.color) / 255f, FastColor.ABGR32.green(portal.color) / 255f, FastColor.ABGR32.blue(portal.color) / 255f, 1);
		// var renderType = RenderType.beaconBeam(portal.shape.texture, true);
		// renderType.setupRenderState();
		// PORTAL_QUAD.render(matrices, projectionMatrix, GameRenderer.getRendertypeBeaconBeamShader());
		// renderType.clearRenderState();
		// RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		if (isViewRenderingEnabled() && viewLayer < MAX_VIEW_LAYERS) {
			RenderingUtils.STENCIL_TEST.enable();
			RenderingUtils.DEPTH_CLAMP.enable();

			final int oldDepthFunc = GlStateManager.DEPTH.func;
			GlStateManager._depthFunc(GL11.GL_LEQUAL);

			{
				GlStateManager._colorMask(false, false, false, false);

				// Update stencil
				{
					GlStateManager._depthMask(false);
					RenderingUtils.setupStencilForWriting(viewLayer, true);
					RenderSystem.setShaderTexture(0, portal.shape.stencilTexture);
					PORTAL_QUAD.render(matrices, projectionMatrix, GameRenderer.getPositionColorTexShader());
					GlStateManager._depthMask(true);
				}

				RenderingUtils.setupStencilToRenderIfValue(viewLayer + 1);
				GlStateManager._stencilMask(0x00);

				GlStateManager._colorMask(true, true, true, true);
			}

			RenderingUtils.DEPTH_CLAMP.disable();

			final var oldFrustum = ((LevelRendererAccessor) levelRenderer).portalcubed$getCullingFrustum();
			final var oldRenderChunksInFrustum = ((LevelRendererAccessor) levelRenderer).portalcubed$getRenderChunksInFrustum();
			((LevelRendererAccessor) levelRenderer).portalcubed$setRenderChunksInFrustum(new ObjectArrayList<>(oldRenderChunksInFrustum));

			final var oldRenderHand = ((GameRendererAccessor) gameRenderer).portalcubed$getRenderHand();
			gameRenderer.setRenderHand(false);

			// PortalCameraTransformation.push(portal);
			{
				final var oldPoseStack = RenderSystem.getModelViewStack();
				viewLayer++;
				var poseCopy = new PoseStack();
				poseCopy.mulPose(portal.rotation180);
				gameRenderer.renderLevel(tickDelta, Util.getNanos(), poseCopy);
				GlStateManager._enableDepthTest();
				viewLayer--;
				RenderSystemAccessor.setModelViewStack(oldPoseStack);
				RenderSystem.applyModelViewMatrix();
			}
			// PortalCameraTransformation.pop();

			gameRenderer.setRenderHand(oldRenderHand);

			((LevelRendererAccessor) levelRenderer).portalcubed$setCullingFrustum(oldFrustum);
			((LevelRendererAccessor) levelRenderer).portalcubed$setRenderChunksInFrustum(oldRenderChunksInFrustum);

			{
				RenderingUtils.DEPTH_CLAMP.enable();
				RenderingUtils.setupStencilForWriting(viewLayer + 1, false);
				GlStateManager._colorMask(false, false, false, false);

				GlStateManager._depthFunc(GL11.GL_ALWAYS);
				RenderingUtils.stencilClear(0, 0, 0);

				RenderingUtils.DEPTH_CLAMP.disable();
				GlStateManager._colorMask(true, true, true, true);
			}

			GlStateManager._depthFunc(oldDepthFunc);
		}
		matrices.popPose();
	}

	private static void renderPortalDebug(Portal portal, WorldRenderContext ctx, PoseStack matrices, MultiBufferSource vertexConsumers) {
		// render a box around the portal's plane
		Color planeColor = portal.isActive() ? ACTIVE_PLANE_COLOR : PLANE_COLOR;
		renderBox(matrices, vertexConsumers, portal.plane, planeColor);
		// collision bounds
		renderBox(matrices, vertexConsumers, portal.entityCollisionArea, RED);
		renderBox(matrices, vertexConsumers, portal.collisionCollectionArea, PURPLE);
		renderBox(matrices, vertexConsumers, portal.collisionModificationBox, CYAN);
		// cross-portal collision
		Portal linked = portal.getLinked();
		if (linked != null) {
			renderCollision(ctx, portal, linked);
		}
		// render player's raycast through
		Camera camera = ctx.camera();
		Vec3 pos = camera.getPosition();
		Vector3f lookVector = camera.getLookVector().normalize(3, new Vector3f());
		Vec3 end = pos.add(lookVector.x, lookVector.y, lookVector.z);
		PortalHitResult hit = ClientPortalManager.of(ctx.world()).clipPortal(pos, end);
		if (hit != null) {
			// start -> hitIn
			RenderingUtils.renderLine(matrices, vertexConsumers, hit.start(), hit.hitIn(), ORANGE);
			// box at hitIn
			AABB hitInBox = AABB.ofSize(hit.hitIn(), 0.1, 0.1, 0.1);
			renderBox(matrices, vertexConsumers, hitInBox, ORANGE);
			// box at hitOut
			AABB hitOutBox = AABB.ofSize(hit.hitOut(), 0.1, 0.1, 0.1);
			renderBox(matrices, vertexConsumers, hitOutBox, BLUE);
			// hitOut -> end
			RenderingUtils.renderLine(matrices, vertexConsumers, hit.hitOut(), hit.teleportedEnd(), BLUE);
			// box at end
			AABB endBox = AABB.ofSize(hit.teleportedEnd(), 0.1, 0.1, 0.1);
			renderBox(matrices, vertexConsumers, endBox, GREEN);
		}
	}

	private static void renderCollision(WorldRenderContext ctx, Portal portal, Portal linked) {
		Camera camera = ctx.camera();
		Entity entity = camera.getEntity();
		ClientLevel level = ctx.world();
		PoseStack matrices = ctx.matrixStack();
		VertexConsumer vertices = ctx.consumers().getBuffer(RenderType.lines());

		List<VoxelShape> shapes = VoxelShenanigans.getShapesBehindPortal(level, entity, portal, linked);
		shapes.forEach(shape -> LevelRenderer.renderVoxelShape(
				matrices, vertices, shape,
				0, 0, 0,
				1, 1, 1, 1,
				true
		));
	}

	private static void renderBox(PoseStack matrices, MultiBufferSource vertexConsumers, AABB box, Color color) {
		VertexConsumer vertices = vertexConsumers.getBuffer(RenderType.lines());
		LevelRenderer.renderLineBox(matrices, vertices, box, color.r(), color.g(), color.b(), color.a());
	}
}

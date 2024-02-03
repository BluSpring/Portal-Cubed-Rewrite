package io.github.fusionflux.portalcubed.framework.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.world.phys.Vec3;

import java.util.function.BiFunction;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

public class RenderingUtils {
	private static final Matrix4f IDENTITY_MATRIX = new Matrix4f().identity();

	public static final GLStateToggle STENCIL_TEST = new GLStateToggle(GL11.GL_STENCIL_TEST, "Stencil Test");
	public static final GLStateToggle DEPTH_CLAMP = new GLStateToggle(GL32.GL_DEPTH_CLAMP, "Depth Clamp");
	public static final int[] CLEAR_COLOR = new int[3];

	// mostly yoinked from DragonFireballRenderer
	public static void renderQuad(PoseStack matrices, VertexConsumer vertices, int light, int color) {
		PoseStack.Pose pose = matrices.last();
		Matrix4f matrix4f = pose.pose();
		Matrix3f matrix3f = pose.normal();
		vertex(vertices, matrix4f, matrix3f, light, 1, 1, color, 1, 1);
		vertex(vertices, matrix4f, matrix3f, light, 1, 0, color, 1, 0);
		vertex(vertices, matrix4f, matrix3f, light, 0, 0, color, 0, 0);
		vertex(vertices, matrix4f, matrix3f, light, 0, 1, color, 0, 1);
	}

	public static void renderLine(PoseStack matrices, MultiBufferSource vertexConsumers, Vec3 from, Vec3 to, Color color) {
		VertexConsumer vertices = vertexConsumers.getBuffer(RenderType.lines());
		PoseStack.Pose pose = matrices.last();
		Vec3 normal = to.subtract(from).normalize();
		vertices.vertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z)
				.color(color.r(), color.g(), color.b(), color.a())
				.normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		vertices.vertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z)
				.color(color.r(), color.g(), color.b(), color.a())
				.normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
	}

	private static void vertex(VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normalMatrix, int light, float x, int y, int color, int textureU, int textureV) {
		vertexConsumer.vertex(matrix, x, y, 0)
				.color(color)
				.uv(textureU, textureV)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(light)
				.normal(normalMatrix, 0, 1, 0)
				.endVertex();
	}

	public static void setupStencilToRenderIfValue(int value) {
		GlStateManager._stencilFunc(GL11.GL_EQUAL, value, 0xFF);
	}

	public static void setupStencilForWriting(int value, boolean increase) {
		setupStencilToRenderIfValue(value);
		GlStateManager._stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, increase ? GL11.GL_INCR : GL11.GL_DECR);
		GlStateManager._stencilMask(0xFF);
	}

	public static void stencilClear(int r, int g, int b) {
		var shader = GameRenderer.getPositionColorShader();
		shader.MODEL_VIEW_MATRIX.set(IDENTITY_MATRIX);
		shader.PROJECTION_MATRIX.set(IDENTITY_MATRIX);
		shader.apply();

		var tessellator = RenderSystem.renderThreadTesselator();
		var bufferBuilder = tessellator.getBuilder();

		bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

		bufferBuilder.vertex(-1, 1, 0).color(r, g, b, 255).endVertex();
		bufferBuilder.vertex(-1, -3, 0).color(r, g, b, 255).endVertex();
		bufferBuilder.vertex(3, 1, 0).color(r, g, b, 255).endVertex();

		BufferUploader.draw(bufferBuilder.end());
		shader.clear();
	}

	public static class GLStateToggle {
		private boolean enabled;
		private final int state;

		private final String debugName;

		private GLStateToggle(int state, String debugName) {
			this.enabled = false;
			this.state = state;

			this.debugName = debugName;
		}

		public void debugPrint() {
			System.out.println(debugName + ": " + enabled);
		}

		private boolean setEnabled(boolean enabled) {
			boolean changed = enabled != this.enabled;
			this.enabled = enabled;
			return changed;
		}

		public boolean enable() {
			if (setEnabled(true)) {
				GL11.glEnable(state);
				return true;
			}
			return false;
		}

		public boolean disable() {
			if (setEnabled(false)) {
				GL11.glDisable(state);
				return true;
			}
			return false;
		}
	}

	public static record Quad(VertexBuffer buffer) implements AutoCloseable {
		private static int[][] DEFAULT_UVS = new int[][]{
			new int[]{1, 1},
			new int[]{1, 0},
			new int[]{0, 0},
			new int[]{0, 1}
		};

		public static Quad create(VertexFormat format, BiFunction<Integer, VertexConsumer, VertexConsumer> modifier, float width, float height) {
			var builder = new BufferBuilder(format.getVertexSize() * 4);
			builder.begin(VertexFormat.Mode.QUADS, format);
			modifier.apply(0, builder.vertex(width, height, 0)).endVertex();
			modifier.apply(1, builder.vertex(width, 0,    0)).endVertex();
			modifier.apply(2, builder.vertex(0,   0,    0)).endVertex();
			modifier.apply(3, builder.vertex(0,   height, 0)).endVertex();

			var vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
			vertexBuffer.bind();
			vertexBuffer.upload(builder.end());
			VertexBuffer.unbind();
			return new Quad(vertexBuffer);
		}

		public static BiFunction<Integer, VertexConsumer, VertexConsumer> defaultUVS(BiFunction<Integer, VertexConsumer, VertexConsumer> modifier) {
			return (i, consumer) -> modifier.apply(i, consumer).uv(DEFAULT_UVS[i][0], DEFAULT_UVS[i][1]);
		}

		public void render(PoseStack matrices, Matrix4f projectionMatrix, ShaderInstance shader) {
			buffer.bind();
			buffer.drawWithShader(matrices.last().pose(), projectionMatrix, shader);
			VertexBuffer.unbind();
		}

		@Override
		public void close() {
			buffer.close();
		}
	}
}

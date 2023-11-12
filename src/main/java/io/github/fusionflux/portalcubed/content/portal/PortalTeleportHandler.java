package io.github.fusionflux.portalcubed.content.portal;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import io.github.fusionflux.portalcubed.content.portal.manager.PortalManager;
import io.github.fusionflux.portalcubed.data.tags.PortalCubedEntityTags;
import io.github.fusionflux.portalcubed.framework.util.TransformUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PortalTeleportHandler {
	public static final double MIN_OUTPUT_VELOCITY = 0.1;

	/**
	 * Called by mixins when an entity moves relatively.
	 * Responsible for finding and teleporting through portals.
	 */
	public static void handle(Entity entity, double x, double y, double z, Operation<Void> setPos) {
		Level level = entity.level();
		if (level.isClientSide || entity.getType().is(PortalCubedEntityTags.PORTAL_BLACKLIST)) {
			setPos.call(entity, x, y, z);
			return;
		}

		Vec3 oldPos = entity.position();
		Vec3 newPos = new Vec3(x, y, z);
		PortalManager manager = PortalManager.of(level);
		PortalHitResult result = manager.clipPortal(oldPos, newPos);
		if (result != null) {
			boolean wasGrounded = entity.onGround(); // grab this before teleporting

			Vec3 oldPosTeleported = result.teleportAbsoluteVec(oldPos);
			Vec3 newPosTeleported = result.teleportedEnd();
			// todo: prevent lerping between portals on client
			entity.teleportTo(newPosTeleported.x, newPosTeleported.y, newPosTeleported.z);

			// rotate entity
			Vec3 lookVec = result.teleportRelativeVec(entity.getLookAngle());
			Vec3 lookTarget = entity.getEyePosition().add(lookVec);
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, lookTarget);

			// reorient velocity
			Vec3 vel = entity.getDeltaMovement();
			Vec3 newVel = result.teleportRelativeVec(vel);
			// have a minimum exit velocity, for fun
			// only apply when falling
			if (!wasGrounded && vel.y < 0 && newVel.length() < MIN_OUTPUT_VELOCITY) {
				newVel = newVel.normalize().scale(MIN_OUTPUT_VELOCITY);
			}
			entity.setDeltaMovement(newVel);
		} else {
			setPos.call(entity, x, y, z);
		}
	}

	public static Vec3 teleportAbsoluteVecBetween(Vec3 vec, Portal in, Portal out) {
		return TransformUtils.apply(
				vec,
				in::relativize,
				in.rotation::transformInverse,
				out.rotation180::transform,
				out::derelativize
		);
	}

	public static Vec3 teleportRelativeVecBetween(Vec3 vec, Portal in, Portal out) {
		return TransformUtils.apply(
				vec,
				in.rotation::transformInverse,
				out.rotation180::transform
		);
	}
}

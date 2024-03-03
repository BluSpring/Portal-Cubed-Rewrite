package io.github.fusionflux.portalcubed.content.crowbar;

import io.github.fusionflux.portalcubed.PortalCubed;
import io.github.fusionflux.portalcubed.content.PortalCubedParticles;
import io.github.fusionflux.portalcubed.data.tags.PortalCubedBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CrowbarItem extends Item {
	public CrowbarItem(Properties settings) {
		super(settings);
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
		if (!state.is(PortalCubedBlockTags.CROWBAR_MAKES_HOLES))
			return !miner.isCreative();
		// I don't know if this is the best place to put this logic, but it works so I'm not questioning it.

		// TODO: this will break with pekhui. Don't hardcode maxDistance
		// Also why the fuck is the raycast method named pick
		BlockHitResult result = (BlockHitResult) miner.pick(5, 0, false);

		Vec3 location = result.getLocation();
		Direction dir = result.getDirection();
		world.addParticle(
				PortalCubedParticles.DECAL,
				location.x, location.y, location.z,
				dir.getStepX(), dir.getStepY(), dir.getStepZ()
		);

		return !miner.isCreative();
	}
}
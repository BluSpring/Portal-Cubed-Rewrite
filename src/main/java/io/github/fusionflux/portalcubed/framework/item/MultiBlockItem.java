package io.github.fusionflux.portalcubed.framework.item;

import java.util.function.Predicate;

import io.github.fusionflux.portalcubed.framework.block.AbstractMultiBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class MultiBlockItem extends BlockItem {
	public final AbstractMultiBlock block;

	public MultiBlockItem(AbstractMultiBlock block, Properties settings) {
		super(block, settings);
		this.block = block;
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
		var direction = state.getValue(AbstractMultiBlock.FACING);
		var rotatedSize = block.size.rotated(direction);

		var horizontalDirection = context.getHorizontalDirection();
		var correctedClickedPos = context.getClickedPos();
		if (horizontalDirection == Direction.SOUTH || horizontalDirection == Direction.WEST)
			correctedClickedPos = correctedClickedPos.relative(horizontalDirection.getClockWise());
			if (direction.getAxis().isHorizontal() && context.getClickedFace() == Direction.DOWN) correctedClickedPos = correctedClickedPos.below();
		if (direction.getAxis().isVertical() && horizontalDirection.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
			correctedClickedPos = correctedClickedPos.relative(context.getHorizontalDirection());

		var level = context.getLevel();
		var collisionContext = CollisionContext.of(context.getPlayer());
		Predicate<BlockPos> placePredicate = pos -> {
			if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight() - 1 || !level.getWorldBorder().isWithinBounds(pos))
				return false;
			return level.isUnobstructed(state, pos, collisionContext) && level.getBlockState(pos).canBeReplaced();
		};
		var origin = rotatedSize.moveToFit(
			context,
			rotatedSize.canFit(level, correctedClickedPos, placePredicate) ? correctedClickedPos : context.getClickedPos(),
			placePredicate
		).orElse(null);
		if (origin != null) {
			var sizeProperties = block.sizeProperties();
			var xProperty = sizeProperties.x();
			var yProperty = sizeProperties.y();
			var zProperty = sizeProperties.z();
			for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(rotatedSize.x() - 1, rotatedSize.y() - 1, rotatedSize.z() - 1))) {
				var relativePos = rotatedSize.relative(origin, pos);
				var subState = state;
				if (xProperty != null) subState = subState.setValue(xProperty, relativePos.getX());
				if (yProperty != null) subState = subState.setValue(yProperty, relativePos.getY());
				if (zProperty != null) subState = subState.setValue(zProperty, relativePos.getZ());
				level.setBlock(pos, subState, Block.UPDATE_ALL_IMMEDIATE);
			}
			return true;
		}

		return false;
	}
}

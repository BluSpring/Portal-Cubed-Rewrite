package io.github.fusionflux.portalcubed.content.button;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import io.github.fusionflux.portalcubed.content.PortalCubedSounds;
import io.github.fusionflux.portalcubed.data.tags.PortalCubedEntityTags;
import io.github.fusionflux.portalcubed.framework.block.AbstractMultiBlock;
import io.github.fusionflux.portalcubed.framework.util.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FloorButtonBlock extends AbstractMultiBlock {
	public static final SizeProperties SIZE_PROPERTIES = SizeProperties.create(2, 2, 1);
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final int PRESSED_TIME = 5;

	public final VoxelShaper[][] shapes;
	public final Map<Direction, AABB> buttonBounds = new HashMap<>();
	public final Predicate<? super Entity> entityPredicate;
	public final SoundEvent pressSound;
	public final SoundEvent releaseSound;

	public FloorButtonBlock(
		Properties properties,
		VoxelShaper[][] shapes,
		VoxelShape buttonShape,
		Predicate<? super Entity> entityPredicate,
		SoundEvent pressSound,
		SoundEvent releaseSound
	) {
		super(properties);
		this.shapes = shapes;
		this.buttonBounds.put(Direction.SOUTH, new AABB(
			buttonShape.min(Direction.Axis.X) * 2,
			buttonShape.min(Direction.Axis.Y) * 2,
			buttonShape.min(Direction.Axis.Z),
			buttonShape.max(Direction.Axis.X) * 2,
			buttonShape.max(Direction.Axis.Y) * 2,
			buttonShape.max(Direction.Axis.Z)
		).move(-buttonShape.min(Direction.Axis.X), -buttonShape.min(Direction.Axis.Y), 0));
		for (Direction direction : Direction.values()) getButtonBounds(direction);
		this.entityPredicate = entityPredicate;
		this.pressSound = pressSound;
		this.releaseSound = releaseSound;
		this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
	}

	public FloorButtonBlock(Properties properties, VoxelShaper[][] shapes, VoxelShape buttonShape, SoundEvent pressSound, SoundEvent releaseSound) {
		this(properties, shapes, buttonShape, entity -> entity.getType().is(PortalCubedEntityTags.PRESSES_FLOOR_BUTTONS), pressSound, releaseSound);
	}

	public FloorButtonBlock(Properties properties, SoundEvent pressSound, SoundEvent releaseSound) {
		this(properties, new VoxelShaper[][]{
			new VoxelShaper[]{
				VoxelShaper.forDirectional(Shapes.or(box(0, 0, 0, 16, 1, 16), box(4, 1, 4, 16, 3, 16)), Direction.UP),
				VoxelShaper.forDirectional(Shapes.or(box(0, 0, 0, 16, 1, 16), box(0, 1, 4, 12, 3, 16)), Direction.UP)
			},
			new VoxelShaper[]{
				VoxelShaper.forDirectional(Shapes.or(box(0, 0, 0, 16, 1, 16), box(4, 1, 0, 16, 3, 12)), Direction.UP),
				VoxelShaper.forDirectional(Shapes.or(box(0, 0, 0, 16, 1, 16), box(0, 1, 0, 12, 3, 12)), Direction.UP)
			}
		}, box(7.5, 7.5, 3, 16, 16, 4), pressSound, releaseSound);
	}

	public FloorButtonBlock(Properties properties) {
		this(properties, PortalCubedSounds.FLOOR_BUTTON_PRESS, PortalCubedSounds.FLOOR_BUTTON_RELEASE);
	}

	public AABB getButtonBounds(Direction direction) {
		return buttonBounds.computeIfAbsent(direction, blah -> {
			var baseButtonBounds = buttonBounds.get(Direction.SOUTH);

			var min = new Vec3(baseButtonBounds.minX, baseButtonBounds.minY, baseButtonBounds.minZ);
			var max = new Vec3(baseButtonBounds.maxX, baseButtonBounds.maxY, baseButtonBounds.maxZ);
			if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
				min = VoxelShaper.rotate(min.subtract(1, 0, .5), 180, Direction.Axis.Y).add(1, 0, .5);
				max = VoxelShaper.rotate(max.subtract(1, 0, .5), 180, Direction.Axis.Y).add(1, 0, .5);
			}

			var rotatedBounds = switch (direction) {
				case DOWN, UP ->   new AABB(min.x, min.z, min.y, max.x, max.z, max.y);
				case WEST, EAST -> new AABB(min.z, min.y, min.x, max.z, max.y, max.x);
				default ->         new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
			};
			return rotatedBounds;
		});
	}

	@Override
	public SizeProperties sizeProperties() {
		return SIZE_PROPERTIES;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ACTIVE);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int y = getY(state);
		int x = getX(state);
		var facing = state.getValue(FACING);
		var quadrantShape = switch (facing) {
			case NORTH, EAST ->       shapes[y == 1 ? 0 : 1][x == 1 ? 0 : 1];
			case DOWN, WEST, SOUTH -> shapes[y == 1 ? 0 : 1][x];
			default ->                shapes[y][x];
		};
		return quadrantShape.get(facing);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.empty();
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		boolean entitiesPressing = level.getEntitiesOfClass(Entity.class, getButtonBounds(state.getValue(FACING)).move(pos), entityPredicate).size() > 0;
		if (entitiesPressing) {
			level.scheduleTick(pos, this, PRESSED_TIME);
		} else {
			for (BlockPos quadrantPos : quadrantIterator(pos, state, level)) {
				var quadrantState = level.getBlockState(quadrantPos);
				if (!quadrantState.is(this)) return;
				level.setBlock(quadrantPos, quadrantState.setValue(ACTIVE, false), UPDATE_ALL);
			}
			playSoundAtCenter(releaseSound, 1f, 1f, pos, state, level);
		}
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (!level.isClientSide && !state.getValue(ACTIVE)) {
			var originPos = getOriginPos(pos, state);
			if (entityPredicate.test(entity) && getButtonBounds(state.getValue(FACING)).move(originPos).intersects(entity.getBoundingBox())) {
				for (BlockPos quadrantPos : quadrantIterator(originPos, state, level)) {
					var newQuadrantState = level.getBlockState(quadrantPos).setValue(ACTIVE, true);
					level.setBlock(quadrantPos, newQuadrantState, UPDATE_ALL);
				}
				level.scheduleTick(originPos, this, PRESSED_TIME);
				playSoundAtCenter(pressSound, 1f, 1f, originPos, state, level);
			}
		}
	}
}
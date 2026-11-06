package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.NetNode;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.menu.GridMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A node that snaps onto an inventory or machine face: Storage Bus, Interface Node or Import Node. */
public class NodeBlock extends BaseEntityBlock implements NetNode {
    public enum Kind { BUS, INTERFACE, IMPORT }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private final Kind kind;

    public NodeBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        for (Direction d : Direction.values()) {
            state = state.setValue(CableBlock.property(d), false);
        }
        registerDefaultState(state);
    }

    public Kind kind() {
        return kind;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        for (Direction d : Direction.values()) {
            builder.add(CableBlock.property(d));
        }
    }

    /** Which sides have a cable or other node to join, so the model can draw an arm toward it. */
    private BlockState withConnections(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            boolean joined = d != facing && n.getBlock() instanceof NetNode node && node.connects(n, d.getOpposite());
            state = state.setValue(CableBlock.property(d), joined);
        }
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, block, orientation, moved);
        if (!level.isClientSide()) {
            BlockState next = withConnections(level, pos, state);
            if (next != state) {
                level.setBlock(pos, next, 3);
            }
            StorageNetwork.invalidate();
        }
    }

    /** The node faces the block it was placed against. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()));
    }

    @Override
    public boolean connects(BlockState state, Direction side) {
        return side != state.getValue(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> Block.box(2, 0, 2, 14, 5, 14);
            case UP -> Block.box(2, 11, 2, 14, 16, 14);
            case NORTH -> Block.box(2, 2, 0, 14, 14, 5);
            case SOUTH -> Block.box(2, 2, 11, 14, 14, 16);
            case WEST -> Block.box(0, 2, 2, 5, 14, 14);
            case EAST -> Block.box(11, 2, 2, 16, 14, 14);
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case BUS -> new StorageBusBlockEntity(pos, state);
            case INTERFACE -> new InterfaceNodeBlockEntity(pos, state);
            case IMPORT -> new ImportNodeBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return switch (kind) {
            case BUS -> null;
            case INTERFACE -> createTickerHelper(type, ModContent.INTERFACE, InterfaceNodeBlockEntity::serverTick);
            case IMPORT -> createTickerHelper(type, ModContent.IMPORT, ImportNodeBlockEntity::serverTick);
        };
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        StorageNetwork.invalidate();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        StorageNetwork.invalidate();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StorageBusBlockEntity bus) {
            if (player.getMainHandItem().isEmpty()) {
                player.sendOverlayMessage(Component.translatable("message.barelystorage.priority", bus.cyclePriority().label()));
            }
        } else if (be instanceof InterfaceNodeBlockEntity node) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> GridMenu.interfaceNode(id, inv, node.buffer),
                    Component.translatable("block.barelystorage.interface_node")));
        } else if (be instanceof ImportNodeBlockEntity) {
            StorageNetwork net = StorageNetwork.find(level, pos);
            player.sendOverlayMessage(Component.translatable(net.online() ? "message.barelystorage.node_online" : "message.barelystorage.node_offline"));
        }
        return InteractionResult.SUCCESS;
    }
}

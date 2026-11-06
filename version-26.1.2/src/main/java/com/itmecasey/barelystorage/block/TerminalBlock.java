package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.NetNode;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import com.itmecasey.barelystorage.menu.TerminalMenus;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The Storage Terminal and the Pattern Terminal: screens onto the network. */
public class TerminalBlock extends Block implements NetNode {
    public enum Kind { STORAGE, CRAFTING, PATTERN, QUEUE }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final MapCodec<TerminalBlock> CODEC = simpleCodec(p -> new TerminalBlock(Kind.STORAGE, p));
    private final Kind kind;

    public TerminalBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean connects(BlockState state, Direction side) {
        return side != state.getValue(FACING);
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
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            switch (kind) {
                case STORAGE -> TerminalMenus.open(sp, level, pos, TerminalMenu.Kind.STORAGE);
                case CRAFTING -> TerminalMenus.open(sp, level, pos, TerminalMenu.Kind.CRAFTING);
                case PATTERN -> TerminalMenus.open(sp, level, pos, TerminalMenu.Kind.PATTERN);
                case QUEUE -> TerminalMenus.openQueue(sp, level, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

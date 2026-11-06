package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.ExternalInventory;
import com.itmecasey.barelystorage.Inventories;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Priority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** A Storage Bus: maps the inventory it faces into the network without moving anything. */
public class StorageBusBlockEntity extends BlockEntity {
    private Priority priority = Priority.NORMAL;

    public StorageBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.BUS, pos, state);
    }

    public Priority priority() {
        return priority;
    }

    public Priority cyclePriority() {
        priority = priority.next();
        setChanged();
        return priority;
    }

    @Nullable
    public ExternalInventory inventory() {
        if (level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(NodeBlock.FACING);
        return Inventories.at(level, worldPosition.relative(facing), facing.getOpposite());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("priority", priority.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        priority = Priority.byOrdinal(input.getIntOr("priority", 1));
    }
}

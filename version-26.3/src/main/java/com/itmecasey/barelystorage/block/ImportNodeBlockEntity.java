package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.ExternalInventory;
import com.itmecasey.barelystorage.Inventories;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Settings;
import com.itmecasey.barelystorage.StorageNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** An Import Node: pulls finished products out of the machine it faces and stores them in the network. */
public class ImportNodeBlockEntity extends BlockEntity {
    private int age;

    public ImportNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.IMPORT, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ImportNodeBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        if (++age % Math.max(1, Settings.getInt("nodes.interval_ticks")) != 0) {
            return;
        }
        Direction facing = state.getValue(NodeBlock.FACING);
        ExternalInventory inv = Inventories.at(level, pos.relative(facing), facing.getOpposite());
        if (inv == null) {
            return;
        }
        StorageNetwork net = StorageNetwork.find(level, pos);
        if (!net.online()) {
            return;
        }
        int budget = Settings.getInt("nodes.items_per_move");
        for (int i = 0; i < inv.slots() && budget > 0; i++) {
            if (inv.get(i).isEmpty()) {
                continue;
            }
            ItemStack taken = inv.extract(i, budget, true);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack rest = net.importStack(taken.copy());
            int moved = taken.getCount() - rest.getCount();
            if (moved > 0) {
                inv.extract(i, moved, false);
                budget -= moved;
            }
        }
    }
}

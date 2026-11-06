package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.ExternalInventory;
import com.itmecasey.barelystorage.Inventories;
import com.itmecasey.barelystorage.ItemKey;
import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Settings;
import com.itmecasey.barelystorage.StorageNetwork;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** An Import Node: pulls items out of the machine it faces into the network. An empty filter imports everything. */
public class ImportNodeBlockEntity extends UpgradeableBlockEntity {
    private int age;

    public ImportNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.IMPORT, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ImportNodeBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        int speed = speed();
        if (++age % Math.max(1, Settings.getInt("nodes.interval_ticks") / speed) != 0) {
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
        Set<ItemKey> allowed = filterKeys(level.registryAccess());
        int budget = Settings.getInt("nodes.items_per_move") * speed;
        for (int i = 0; i < inv.slots() && budget > 0; i++) {
            ItemStack here = inv.get(i);
            if (here.isEmpty() || (!allowed.isEmpty() && !allowed.contains(Keys.of(here, level.registryAccess())))) {
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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        saveUpgrades(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadUpgrades(input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropUpgrades(pos);
    }
}

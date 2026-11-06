package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.ExternalInventory;
import com.itmecasey.barelystorage.Inventories;
import com.itmecasey.barelystorage.ItemKey;
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

/** An Export Node: pushes the items in its filter out of the network into the inventory it faces. An empty filter pushes nothing. */
public class ExportNodeBlockEntity extends UpgradeableBlockEntity {
    private int age;

    public ExportNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.EXPORT, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ExportNodeBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        int speed = speed();
        if (++age % Math.max(1, Settings.getInt("nodes.interval_ticks") / speed) != 0) {
            return;
        }
        Set<ItemKey> wanted = filterKeys(level.registryAccess());
        if (wanted.isEmpty()) {
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
        int budget = Settings.getInt("nodes.items_per_move") * speed;
        for (ItemKey key : wanted) {
            if (budget <= 0) {
                break;
            }
            ItemStack out = net.extract(key, budget);
            if (out.isEmpty()) {
                continue;
            }
            ItemStack rest = inv.insert(out, false);
            int moved = out.getCount() - rest.getCount();
            budget -= moved;
            if (!rest.isEmpty()) {
                net.insert(rest);
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

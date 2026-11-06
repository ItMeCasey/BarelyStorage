package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Pattern;
import com.itmecasey.barelystorage.Settings;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.item.PatternItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** An Assembler: holds encoded patterns and runs crafting steps handed to it by the rack's crafting queue. */
public class AssemblerBlockEntity extends UpgradeableBlockEntity {
    public final SimpleContainer patterns = new SimpleContainer(MAX_ROWS * 9) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < patternRows() * 9 && stack.getItem() instanceof PatternItem item && item.type() == PatternItem.Type.CRAFTING;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            AssemblerBlockEntity.this.setChanged();
        }
    };
    @Nullable
    private Pattern current;
    private int runs;
    private int ticksLeft;
    private List<Pattern.Stack> pending = new ArrayList<>();

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.ASSEMBLER, pos, state);
    }

    public boolean busy() {
        return current != null;
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    /** Starts {@code runs} runs of a crafting pattern; the ingredients have already been taken from the network. */
    public void begin(Pattern pattern, int runs) {
        this.current = pattern;
        this.runs = runs;
        this.ticksLeft = Math.max(1, runs * Settings.getInt("crafting.assembler_ticks_per_run") / speed());
        this.pending = new ArrayList<>();
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AssemblerBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        boolean active = current != null;
        if (state.getValue(AssemblerBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(AssemblerBlock.ACTIVE, active), 3);
        }
        if (current == null) {
            return;
        }
        if (ticksLeft > 0) {
            ticksLeft--;
            if (ticksLeft == 0) {
                pending = new ArrayList<>();
                for (Pattern.Stack out : current.outputs()) {
                    pending.add(new Pattern.Stack(out.key(), out.count() * runs));
                }
                setChanged();
            }
            return;
        }
        // Done working: hand the products to the network, keeping what does not fit for the next try.
        StorageNetwork net = StorageNetwork.find(level, pos);
        if (!net.online()) {
            return;
        }
        List<Pattern.Stack> left = new ArrayList<>();
        for (Pattern.Stack s : pending) {
            int remaining = s.count();
            int max = Keys.maxStack(s.key());
            while (remaining > 0) {
                int chunk = Math.min(max, remaining);
                ItemStack rest = net.insert(Keys.stack(s.key(), chunk, level.registryAccess()));
                remaining -= chunk - rest.getCount();
                if (!rest.isEmpty()) {
                    break;
                }
            }
            if (remaining > 0) {
                left.add(new Pattern.Stack(s.key(), remaining));
            }
        }
        pending = left;
        if (left.isEmpty()) {
            var lead = net.leader();
            if (lead != null) {
                lead.scheduler().completed(current, runs);
                lead.setChanged();
            }
            current = null;
            runs = 0;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        patterns.storeAsItemList(output.list("patterns", ItemStack.OPTIONAL_CODEC));
        saveUpgrades(output);
        if (current != null) {
            output.putString("current", current.encode());
            output.putInt("runs", runs);
            output.putInt("ticks", ticksLeft);
            output.putString("pending", new Pattern(Pattern.Kind.CRAFTING, pending, List.of()).encode());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        patterns.fromItemList(input.listOrEmpty("patterns", ItemStack.OPTIONAL_CODEC));
        loadUpgrades(input);
        current = null;
        input.getString("current").ifPresent(text -> {
            try {
                current = Pattern.decode(text);
            } catch (RuntimeException e) {
                current = null;
            }
        });
        runs = input.getIntOr("runs", 0);
        ticksLeft = input.getIntOr("ticks", 0);
        pending = new ArrayList<>();
        input.getString("pending").ifPresent(text -> {
            try {
                pending = new ArrayList<>(Pattern.decode(text).inputs());
            } catch (RuntimeException e) {
                pending = new ArrayList<>();
            }
        });
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, patterns);
        }
        dropUpgrades(pos);
        StorageNetwork.invalidate();
    }
}

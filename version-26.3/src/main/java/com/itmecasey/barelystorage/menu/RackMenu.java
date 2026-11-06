package com.itmecasey.barelystorage.menu;

import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import com.itmecasey.barelystorage.item.BladeItem;
import com.itmecasey.barelystorage.item.CartridgeItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** The Server Rack screen: drive bays, blade slots and a live readout of power, storage, CPU, RAM and errors. */
public class RackMenu extends StorageMenu {
    public static final int DRIVE_X = 8;
    public static final int DRIVE_Y = 22;
    public static final int BLADE_X = 8;
    public static final int BLADE_Y = 52;
    private final Container drives;
    private final Container blades;
    private final ContainerData data;
    @Nullable
    private final RackBlockEntity rack;

    /** Client side. */
    public RackMenu(int id, Inventory inventory) {
        this(id, inventory, null, new SimpleContainer(RackBlockEntity.DRIVES), new SimpleContainer(RackBlockEntity.BLADES), new SimpleContainerData(RackBlockEntity.STATS));
    }

    /** Server side. */
    public RackMenu(int id, Inventory inventory, RackBlockEntity rack, ContainerData data) {
        this(id, inventory, rack, rack.drives, rack.blades, data);
    }

    private RackMenu(int id, Inventory inventory, @Nullable RackBlockEntity rack, Container drives, Container blades, ContainerData data) {
        super(ModContent.RACK_MENU, id);
        this.rack = rack;
        this.drives = drives;
        this.blades = blades;
        this.data = data;
        for (int i = 0; i < RackBlockEntity.DRIVES; i++) {
            addSlot(new Slot(drives, i, DRIVE_X + i * 18, DRIVE_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int i = 0; i < RackBlockEntity.BLADES; i++) {
            addSlot(new Slot(blades, i, BLADE_X + i * 18, BLADE_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }
            });
        }
        playerSlots(inventory, 8, 130);
        addDataSlots(data);
    }

    public int stat(int index) {
        return data.get(index);
    }

    public int stat32(int index) {
        return (data.get(index) << 16) | data.get(index + 1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int rackSlots = RackBlockEntity.DRIVES + RackBlockEntity.BLADES;
        if (index < rackSlots) {
            if (!moveItemStackTo(stack, rackSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof CartridgeItem) {
            if (!moveItemStackTo(stack, 0, RackBlockEntity.DRIVES, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof BladeItem) {
            if (!moveItemStackTo(stack, RackBlockEntity.DRIVES, rackSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return rack == null || (!rack.isRemoved() && player.distanceToSqr(rack.getBlockPos().getX() + 0.5, rack.getBlockPos().getY() + 0.5, rack.getBlockPos().getZ() + 0.5) <= 64.0);
    }
}

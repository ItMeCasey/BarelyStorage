package com.itmecasey.barelystorage.compat;

import com.itmecasey.barelystorage.Hardware;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.block.AssemblerBlockEntity;
import com.itmecasey.barelystorage.block.ImportNodeBlockEntity;
import com.itmecasey.barelystorage.block.InterfaceNodeBlockEntity;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import com.itmecasey.barelystorage.block.StorageBusBlockEntity;
import com.itmecasey.barelystorage.item.CartridgeItem;
import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.SmwtiPlugin;
import net.minecraft.network.chat.Component;

/**
 * The BarelyStorage lines in Show Me What This Is: a rack's power, drives, CPU and RAM load and crafting errors, a Storage Bus
 * priority, an Interface Node buffer, an Assembler's state. The server works the lines out. SMWTI finds this class by its
 * annotation; nothing calls into it, and it is never loaded unless SMWTI is installed.
 */
@SmwtiPlugin
public class BarelyStorageSmwtiPlugin implements ISmwtiPlugin {
    private static final int GOLD = 0xF2B33D;
    private static final int BLUE = 0x7AA8FF;

    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> {
            if (entity instanceof RackBlockEntity rack) {
                if (!rack.online()) {
                    tooltip.add(Component.translatable("smwti.barelystorage.rack_offline"));
                }
                tooltip.add(Component.translatable("smwti.barelystorage.rack_upkeep", rack.upkeep()));
                tooltip.bar(Component.translatable("smwti.barelystorage.rack_drives", CartridgeItem.fmt(rack.used()), CartridgeItem.fmt(rack.capacity()),
                        rack.cartridgeCount()), (double) rack.used() / Math.max(1, rack.capacity()), BLUE);
                StorageNetwork net = StorageNetwork.find(level, pos);
                Hardware hw = net.hardware();
                tooltip.add(Component.translatable("smwti.barelystorage.rack_cpu", hw.threads(), hw.ram(),
                        hw.maxDepth() == Integer.MAX_VALUE ? "∞" : String.valueOf(hw.maxDepth())));
                if (net.leader() == rack && !rack.scheduler().lastError().isEmpty()) {
                    tooltip.add(Component.translatable("smwti.barelystorage.rack_error", rack.scheduler().lastError()));
                }
            } else if (entity instanceof StorageBusBlockEntity bus) {
                tooltip.add(Component.translatable("smwti.barelystorage.priority", bus.priority().label()));
            } else if (entity instanceof InterfaceNodeBlockEntity node) {
                int items = 0;
                for (int i = 0; i < node.buffer.getContainerSize(); i++) {
                    items += node.buffer.getItem(i).getCount();
                }
                tooltip.add(Component.translatable("smwti.barelystorage.buffer", items));
            } else if (entity instanceof ImportNodeBlockEntity) {
                tooltip.add(Component.translatable(StorageNetwork.find(level, pos).online() ? "message.barelystorage.node_online" : "message.barelystorage.node_offline"));
            } else if (entity instanceof AssemblerBlockEntity asm) {
                if (asm.busy()) {
                    tooltip.add(Component.translatable("smwti.barelystorage.assembler_busy", asm.ticksLeft()));
                } else {
                    int patterns = 0;
                    for (int i = 0; i < asm.patterns.getContainerSize(); i++) {
                        if (!asm.patterns.getItem(i).isEmpty()) {
                            patterns++;
                        }
                    }
                    tooltip.add(Component.translatable("smwti.barelystorage.assembler_idle", patterns));
                }
            }
        });
    }
}

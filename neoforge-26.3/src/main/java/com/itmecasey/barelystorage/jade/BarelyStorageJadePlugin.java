package com.itmecasey.barelystorage.jade;

import com.itmecasey.barelystorage.Hardware;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.block.AssemblerBlockEntity;
import com.itmecasey.barelystorage.block.ImportNodeBlockEntity;
import com.itmecasey.barelystorage.block.InterfaceNodeBlockEntity;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import com.itmecasey.barelystorage.block.StorageBusBlockEntity;
import com.itmecasey.barelystorage.item.CartridgeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * The BarelyStorage lines in Jade: a rack's upkeep, drives, CPU and RAM and crafting errors, a Storage Bus priority, an Interface
 * Node buffer, an Import Node's state and an Assembler's work. The server works the numbers out and sends them in the block's
 * data. Jade finds this class by its annotation, so it is never loaded unless Jade is installed.
 */
@WailaPlugin
public class BarelyStorageJadePlugin implements IWailaPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("barelystorage", "machines");

    @Override
    public void register(IWailaCommonRegistration registration) {
        IServerDataProvider<BlockAccessor> provider = new IServerDataProvider<>() {
            @Override
            public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                BlockEntity entity = accessor.getBlockEntity();
                if (entity instanceof RackBlockEntity rack) {
                    data.putString("kind", "rack");
                    data.putBoolean("online", rack.online());
                    data.putInt("upkeep", rack.upkeep());
                    data.putString("used", CartridgeItem.fmt(rack.used()));
                    data.putString("capacity", CartridgeItem.fmt(rack.capacity()));
                    data.putInt("cartridges", rack.cartridgeCount());
                    StorageNetwork net = StorageNetwork.find(accessor.getLevel(), accessor.getPosition());
                    Hardware hw = net.hardware();
                    data.putInt("threads", hw.threads());
                    data.putInt("ram", hw.ram());
                    data.putString("depth", hw.maxDepth() == Integer.MAX_VALUE ? "∞" : String.valueOf(hw.maxDepth()));
                    if (net.leader() == rack && !rack.scheduler().lastError().isEmpty()) {
                        data.putString("error", rack.scheduler().lastError());
                    }
                } else if (entity instanceof StorageBusBlockEntity bus) {
                    data.putString("kind", "bus");
                    data.putString("priority", bus.priority().label());
                } else if (entity instanceof InterfaceNodeBlockEntity node) {
                    data.putString("kind", "interface");
                    int items = 0;
                    for (int i = 0; i < node.buffer.getContainerSize(); i++) {
                        items += node.buffer.getItem(i).getCount();
                    }
                    data.putInt("items", items);
                } else if (entity instanceof ImportNodeBlockEntity) {
                    data.putString("kind", "import");
                    data.putBoolean("online", StorageNetwork.find(accessor.getLevel(), accessor.getPosition()).online());
                } else if (entity instanceof AssemblerBlockEntity asm) {
                    data.putString("kind", "assembler");
                    data.putBoolean("busy", asm.busy());
                    data.putInt("ticks", asm.ticksLeft());
                    int patterns = 0;
                    for (int i = 0; i < asm.patterns.getContainerSize(); i++) {
                        if (!asm.patterns.getItem(i).isEmpty()) {
                            patterns++;
                        }
                    }
                    data.putInt("patterns", patterns);
                }
            }

            @Override
            public Identifier getUid() {
                return UID;
            }
        };
        registration.registerBlockDataProvider(provider, RackBlockEntity.class);
        registration.registerBlockDataProvider(provider, StorageBusBlockEntity.class);
        registration.registerBlockDataProvider(provider, InterfaceNodeBlockEntity.class);
        registration.registerBlockDataProvider(provider, ImportNodeBlockEntity.class);
        registration.registerBlockDataProvider(provider, AssemblerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                String kind = data.getStringOr("kind", "");
                switch (kind) {
                    case "rack" -> {
                        if (!data.getBooleanOr("online", true)) {
                            tooltip.add(Component.translatable("smwti.barelystorage.rack_offline"));
                        }
                        tooltip.add(Component.translatable("smwti.barelystorage.rack_upkeep", data.getIntOr("upkeep", 0)));
                        tooltip.add(Component.translatable("smwti.barelystorage.rack_drives", data.getStringOr("used", "0"),
                                data.getStringOr("capacity", "0"), data.getIntOr("cartridges", 0)));
                        tooltip.add(Component.translatable("smwti.barelystorage.rack_cpu", data.getIntOr("threads", 0), data.getIntOr("ram", 0),
                                data.getStringOr("depth", "0")));
                        String error = data.getStringOr("error", "");
                        if (!error.isEmpty()) {
                            tooltip.add(Component.translatable("smwti.barelystorage.rack_error", error));
                        }
                    }
                    case "bus" -> tooltip.add(Component.translatable("smwti.barelystorage.priority", data.getStringOr("priority", "")));
                    case "interface" -> tooltip.add(Component.translatable("smwti.barelystorage.buffer", data.getIntOr("items", 0)));
                    case "import" -> tooltip.add(Component.translatable(data.getBooleanOr("online", false)
                            ? "message.barelystorage.node_online" : "message.barelystorage.node_offline"));
                    case "assembler" -> tooltip.add(data.getBooleanOr("busy", false)
                            ? Component.translatable("smwti.barelystorage.assembler_busy", data.getIntOr("ticks", 0))
                            : Component.translatable("smwti.barelystorage.assembler_idle", data.getIntOr("patterns", 0)));
                    default -> {
                    }
                }
            }

            @Override
            public Identifier getUid() {
                return UID;
            }
        }, Block.class);
    }
}

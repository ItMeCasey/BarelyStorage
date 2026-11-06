package com.itmecasey.barelystorage.neoforge;

import com.itmecasey.barelystorage.BarelyStorage;
import com.itmecasey.barelystorage.ExternalInventory;
import com.itmecasey.barelystorage.Inventories;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** NeoForge entry point for Minecraft 26.x. Registers the content, the rack's energy input and the item access for Storage Buses. */
@Mod(BarelyStorage.MODID)
public final class NeoForgeBarelyStorage {
    public NeoForgeBarelyStorage(IEventBus modBus) {
        BarelyStorage.init(FMLPaths.CONFIGDIR::get);
        modBus.addListener(NeoForgeBarelyStorage::register);
        modBus.addListener(NeoForgeBarelyStorage::capabilities);
        Inventories.loaderFinder = (level, pos, face) -> {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, face);
            return handler == null ? null : new HandlerView(handler);
        };
        com.itmecasey.barelystorage.Dev.call("com.itmecasey.barelystorage.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModContent.registerBlockEntities((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.MENU, helper -> ModContent.registerMenus((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModContent.RACK, (be, side) -> new RackEnergy(be));
    }

    /** What cable mods see: a Server Rack takes energy in and never gives it out. */
    private static final class RackEnergy extends SnapshotJournal<Integer> implements EnergyHandler {
        private final RackBlockEntity be;

        RackEnergy(RackBlockEntity be) {
            this.be = be;
        }

        @Override
        protected Integer createSnapshot() {
            return be.energy();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            be.setEnergy(snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return be.energy();
        }

        @Override
        public long getCapacityAsLong() {
            return be.maxEnergy();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int taken = be.receive(amount, true);
            if (taken > 0) {
                updateSnapshots(transaction);
                be.receive(taken, false);
            }
            return taken;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    }

    /** A NeoForge item handler seen as an {@link ExternalInventory}. */
    private record HandlerView(ResourceHandler<ItemResource> handler) implements ExternalInventory {
        @Override
        public int slots() {
            return handler.size();
        }

        @Override
        public ItemStack get(int slot) {
            ItemResource r = handler.getResource(slot);
            int n = handler.getAmountAsInt(slot);
            return r.isEmpty() || n <= 0 ? ItemStack.EMPTY : r.toStack(n);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            try (Transaction tx = Transaction.openRoot()) {
                int in = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
                if (in > 0 && !simulate) {
                    tx.commit();
                }
                return in >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - in);
            }
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            ItemResource r = handler.getResource(slot);
            if (r.isEmpty()) {
                return ItemStack.EMPTY;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int out = handler.extract(slot, r, amount, tx);
                if (out > 0 && !simulate) {
                    tx.commit();
                }
                return out <= 0 ? ItemStack.EMPTY : r.toStack(out);
            }
        }
    }
}

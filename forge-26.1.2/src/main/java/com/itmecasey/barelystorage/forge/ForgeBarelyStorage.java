package com.itmecasey.barelystorage.forge;

import com.itmecasey.barelystorage.BarelyStorage;
import com.itmecasey.barelystorage.ExternalInventory;
import com.itmecasey.barelystorage.Inventories;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Forge entry point for Minecraft 26.x. Registers the content, the rack's energy input and the item access for Storage Buses. */
@Mod(BarelyStorage.MODID)
public final class ForgeBarelyStorage {
    public ForgeBarelyStorage(FMLJavaModLoadingContext context) {
        BarelyStorage.init(FMLPaths.CONFIGDIR::get);
        ForgeConfig.register(context);
        com.itmecasey.barelystorage.script.ScriptBridge.install();
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgeBarelyStorage::register);
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeBarelyStorage::attach);
        ForgeClient.install(context);
        Inventories.loaderFinder = (level, pos, face) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                return null;
            }
            IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, face).orElse(null);
            return handler == null ? null : new HandlerView(handler);
        };
        com.itmecasey.barelystorage.Dev.call("com.itmecasey.barelystorage.forge.dev.ForgeDev", "install", context);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModContent.registerBlockEntities((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.MENU, helper -> ModContent.registerMenus((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    private static void attach(AttachCapabilitiesEvent.BlockEntities event) {
        if (!(event.getObject() instanceof RackBlockEntity rack)) {
            return;
        }
        LazyOptional<IEnergyStorage> lazy = LazyOptional.of(() -> new RackEnergy(rack));
        event.addCapability(ModContent.id("rack_energy"), new ICapabilityProvider() {
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
            }
        });
        event.addListener(lazy::invalidate);
    }

    /** What cable mods see: a Server Rack takes energy in and never gives it out. */
    private record RackEnergy(RackBlockEntity be) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return be.receive(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return be.energy();
        }

        @Override
        public int getMaxEnergyStored() {
            return be.maxEnergy();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    /** A Forge item handler seen as an {@link ExternalInventory}. */
    private record HandlerView(IItemHandler handler) implements ExternalInventory {
        @Override
        public int slots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack get(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return ItemHandlerHelper.insertItem(handler, stack, simulate);
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }
    }
}

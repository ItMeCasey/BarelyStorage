package com.itmecasey.barelystorage;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** A block that joins a storage network: it links to a neighbor that is also a node when both agree on that face. */
public interface NetNode {
    boolean connects(BlockState state, Direction side);
}

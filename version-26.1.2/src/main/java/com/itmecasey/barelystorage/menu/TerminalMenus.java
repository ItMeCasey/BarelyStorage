package com.itmecasey.barelystorage.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;

/** Opens the Storage Terminal at the height (5, 7 or 9 rows) the player chose last. */
public final class TerminalMenus {
    private static final Map<UUID, Integer> HEIGHT = new HashMap<>();

    private TerminalMenus() {
    }

    public static void setHeight(ServerPlayer player, int index) {
        HEIGHT.put(player.getUUID(), Math.max(0, Math.min(2, index)));
    }

    public static void open(ServerPlayer player, Level level, BlockPos pos) {
        int rows = TerminalMenu.ROWS[HEIGHT.getOrDefault(player.getUUID(), 0)];
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new TerminalMenu(rows, id, inv, level, pos),
                Component.translatable("block.barelystorage.storage_terminal")));
    }
}

package com.teamresourceful.resourcefullib.common.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

public interface ContentMenuProvider<C extends MenuContent<C>> extends MenuProvider {

    C createContent(ServerPlayer player);

    default void openMenu(ServerPlayer player) {
        MenuContentHelper.open(player, this);
    }

    default boolean resetMouseOnOpen() {
        return true;
    }
}

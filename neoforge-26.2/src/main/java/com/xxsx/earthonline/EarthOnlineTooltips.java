package com.xxsx.earthonline;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

final class EarthOnlineTooltips {
    private EarthOnlineTooltips() {
    }

    static void addRouteTips(ItemStack stack, Consumer<Component> lines) {
        RouteGuide.addRouteTips(stack, lines);
    }
}

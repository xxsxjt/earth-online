package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class GuidedMaterialItem extends Item {
    public GuidedMaterialItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.earth_online.material.header").withStyle(ChatFormatting.GOLD));
        EarthOnlineTooltips.addRouteTips(stack, lines);
        lines.accept(Component.translatable("tooltip.earth_online.material.notebook").withStyle(ChatFormatting.DARK_GRAY));
    }
}

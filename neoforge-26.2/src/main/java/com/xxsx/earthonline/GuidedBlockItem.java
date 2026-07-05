package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class GuidedBlockItem extends BlockItem {
    private final String hint;

    public GuidedBlockItem(Block block, Properties properties, String hint) {
        super(block, properties);
        this.hint = hint;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.literal(hint).withStyle(ChatFormatting.GOLD));
        EarthOnlineTooltips.addRouteTips(stack, lines);
    }
}

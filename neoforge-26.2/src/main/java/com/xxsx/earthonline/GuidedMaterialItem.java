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
        lines.accept(Component.literal("地球 Online 材料：看下一步，不用背流程。").withStyle(ChatFormatting.GOLD));
        EarthOnlineTooltips.addRouteTips(stack, lines);
        lines.accept(Component.literal("提示：右键野外地质手册可看完整路线。").withStyle(ChatFormatting.DARK_GRAY));
    }
}

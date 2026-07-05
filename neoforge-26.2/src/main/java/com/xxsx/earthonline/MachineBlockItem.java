package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class MachineBlockItem extends BlockItem {
    private final ProcessingMachineBlock.Kind kind;

    public MachineBlockItem(Block block, Properties properties, ProcessingMachineBlock.Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.literal(kind.description()).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("空手右键：打开配方界面。").withStyle(ChatFormatting.AQUA));
        lines.accept(Component.literal("手持输入右键：直接处理 1 个材料。").withStyle(ChatFormatting.GREEN));
        lines.accept(Component.literal("当前内置路线：" + ProcessingMachineBlock.recipesFor(kind).size() + " 条，可配合 JEI 查询。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

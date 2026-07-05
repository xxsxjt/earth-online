package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class FieldGeologyNotebookItem extends Item {
    public FieldGeologyNotebookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            if (!openClientNotebookScreen()) {
                sendGuide(player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.literal("右键查看地球 Online 的入门手册").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("核心：真实来源 + 多步骤处理 + 保留 MC 物品生态").withStyle(ChatFormatting.DARK_GREEN));
    }

    public static void sendGuide(Player player) {
        player.sendSystemMessage(Component.literal("====== 地球 Online 野外地质手册 ======").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("目标：保留 MC 的锭、宝石和合成魔法，但把矿物来源、岩石组成、矿床和冶炼路线真实化。").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.literal("1. 原版矿石不再作为自然来源；铁主要来自磁铁矿 Fe3O4，铜来自黄铜矿 CuFeS2，金常在石英脉里。").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("2. 不懂化学也没关系：空手右键机器会显示它能处理什么，拿材料右键就会自动产出。").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("3. 基础路线：矿石 -> 破碎机 -> 碎块 -> 球磨机 -> 粉末 -> 分选/浮选/焙烧/还原。").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("4. 磁铁矿路线：磁铁矿粉 -> 磁选机 -> 铁精矿 + 尾粉；铁精矿 -> 还原炉 -> 铁锭 + 矿渣。").withStyle(ChatFormatting.RED));
        player.sendSystemMessage(Component.literal("5. 黄铜矿路线：黄铜矿粉 -> 浮选槽 -> 铜精矿；铜精矿 -> 焙烧炉 -> 焙烧铜精矿 + 硫粉；再还原/电解出铜。").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("6. 岩石不是单一化学式：花岗岩可磨出石英粉、长石粉、云母粉和尾粉；方解石/滴水石主要给碳酸盐粉。").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("提示：第一版机器不需要电力和 GUI，先让真实流程跑起来；后续再升级为带进度、能耗和自动化。").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean openClientNotebookScreen() {
        try {
            Class.forName("com.xxsx.earthonline.client.EarthOnlineClient")
                    .getMethod("openNotebook")
                    .invoke(null);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}

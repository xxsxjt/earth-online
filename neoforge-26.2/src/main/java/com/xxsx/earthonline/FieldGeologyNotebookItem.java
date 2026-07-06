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
        lines.accept(Component.translatable("tooltip.earth_online.notebook.use").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.earth_online.notebook.core").withStyle(ChatFormatting.DARK_GREEN));
    }

    public static void sendGuide(Player player) {
        player.sendSystemMessage(Component.translatable("guide.earth_online.line0").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("guide.earth_online.line1").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.translatable("guide.earth_online.line2").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.translatable("guide.earth_online.line3").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.translatable("guide.earth_online.line4").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable("guide.earth_online.line5").withStyle(ChatFormatting.RED));
        player.sendSystemMessage(Component.translatable("guide.earth_online.line6").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("guide.earth_online.line7").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.translatable("guide.earth_online.line8").withStyle(ChatFormatting.DARK_GRAY));
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

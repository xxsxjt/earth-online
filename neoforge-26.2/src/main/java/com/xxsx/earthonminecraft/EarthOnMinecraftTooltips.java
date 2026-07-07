package com.xxsx.earthonminecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.function.Consumer;

final class EarthOnMinecraftTooltips {
    private EarthOnMinecraftTooltips() {
    }

    static void addRouteTips(ItemStack stack, Consumer<Component> lines) {
        RouteGuide.addRouteTips(stack, lines);
    }

    static void addMaterialDetails(ItemStack stack, Consumer<Component> lines, TooltipFlag flag) {
        MaterialChemistry.addDetails(stack, lines, flag);
    }

    static void addVanillaTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"minecraft".equals(id.getNamespace())) {
            return;
        }
        boolean hasChemistry = MaterialChemistry.hasDetails(stack);
        boolean hasRoute = !RouteGuide.routeFor(stack.getItem()).isEmpty();
        if (!hasChemistry && !hasRoute) {
            return;
        }
        event.getToolTip().add(Component.translatable("tooltip.earth_on_minecraft.vanilla.header").withStyle(ChatFormatting.GOLD));
        MaterialChemistry.addDetails(stack, event.getToolTip()::add, event.getFlags());
        RouteGuide.addRouteTips(stack, event.getToolTip()::add);
    }
}

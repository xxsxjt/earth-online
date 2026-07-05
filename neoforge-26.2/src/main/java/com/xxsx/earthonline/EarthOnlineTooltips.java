package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class EarthOnlineTooltips {
    private EarthOnlineTooltips() {
    }

    static void addRouteTips(ItemStack stack, Consumer<Component> lines) {
        Item item = stack.getItem();
        List<ProcessingMachineBlock.Recipe> next = ProcessingMachineBlock.recipes().stream()
                .filter(recipe -> recipe.input().get().asItem() == item)
                .limit(4)
                .toList();
        List<ProcessingMachineBlock.Recipe> sources = ProcessingMachineBlock.recipes().stream()
                .filter(recipe -> recipe.outputs().stream().anyMatch(output -> output.item().get().asItem() == item))
                .limit(3)
                .toList();

        if (!next.isEmpty()) {
            lines.accept(Component.literal("下一步：放入 " + joinMachines(next)).withStyle(ChatFormatting.AQUA));
            lines.accept(Component.literal("示例产出：" + describeOutputs(next.get(0))).withStyle(ChatFormatting.GRAY));
            if (next.size() > 1) {
                lines.accept(Component.literal("有多条路线，空手右键对应机器或用 JEI 查看。").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (!sources.isEmpty()) {
            lines.accept(Component.literal("常见来源：" + joinSources(sources)).withStyle(ChatFormatting.DARK_GREEN));
        }

        if (next.isEmpty() && sources.isEmpty()) {
            lines.accept(Component.literal("用途：兼容 MC 生态或等待后续联动路线。").withStyle(ChatFormatting.GRAY));
        }
    }

    private static String joinMachines(List<ProcessingMachineBlock.Recipe> recipes) {
        List<String> names = new ArrayList<>();
        for (ProcessingMachineBlock.Recipe recipe : recipes) {
            String name = recipe.kind().displayName();
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return String.join(" / ", names);
    }

    private static String joinSources(List<ProcessingMachineBlock.Recipe> recipes) {
        List<String> names = new ArrayList<>();
        for (ProcessingMachineBlock.Recipe recipe : recipes) {
            String name = recipe.kind().displayName() + "：" + recipe.note();
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return String.join("；", names);
    }

    private static String describeOutputs(ProcessingMachineBlock.Recipe recipe) {
        List<String> outputs = new ArrayList<>();
        for (ProcessingMachineBlock.Output output : recipe.outputs()) {
            ItemStack out = output.stack();
            outputs.add(out.getCount() + "x " + out.getItemName().getString());
        }
        return String.join(" + ", outputs);
    }
}

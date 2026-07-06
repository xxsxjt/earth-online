package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class RouteGuide {
    private static final Map<Item, RouteInfo> ROUTES_BY_ITEM = createRoutesByItem();

    private RouteGuide() {
    }

    public static Map<Item, RouteInfo> buildRoutesByItem() {
        return ROUTES_BY_ITEM;
    }

    private static Map<Item, RouteInfo> createRoutesByItem() {
        Map<Item, RouteInfo> routes = new LinkedHashMap<>();
        for (ProcessingMachineBlock.Recipe recipe : ProcessingMachineBlock.recipes()) {
            routes.computeIfAbsent(recipe.input().get().asItem(), ignored -> new RouteInfo()).next.add(recipe);
            for (ProcessingMachineBlock.Output output : recipe.outputs()) {
                routes.computeIfAbsent(output.item().get().asItem(), ignored -> new RouteInfo()).sources.add(recipe);
            }
        }
        return routes;
    }

    public static RouteInfo routeFor(Item item) {
        return ROUTES_BY_ITEM.getOrDefault(item, RouteInfo.EMPTY);
    }

    public static void addRouteTips(ItemStack stack, Consumer<Component> lines) {
        RouteInfo route = routeFor(stack.getItem());

        if (!route.next().isEmpty()) {
            lines.accept(Component.translatable("tooltip.earth_online.route.next", joinMachines(route.next(), 4)).withStyle(ChatFormatting.AQUA));
            lines.accept(Component.translatable("tooltip.earth_online.route.outputs", describeOutputs(route.next().get(0))).withStyle(ChatFormatting.GRAY));
            if (uniqueMachineCount(route.next()) > 1) {
                lines.accept(Component.translatable("tooltip.earth_online.route.multiple").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (!route.sources().isEmpty()) {
            lines.accept(Component.translatable("tooltip.earth_online.route.sources", joinSources(route.sources(), 3)).withStyle(ChatFormatting.DARK_GREEN));
        }

        if (route.isEmpty()) {
            lines.accept(Component.translatable("tooltip.earth_online.route.compat").withStyle(ChatFormatting.GRAY));
        }
    }

    public static String joinMachines(List<ProcessingMachineBlock.Recipe> recipes, int limit) {
        List<String> names = new ArrayList<>();
        for (ProcessingMachineBlock.Recipe recipe : recipes) {
            String name = recipe.kind().localizedDisplayName();
            if (!names.contains(name)) {
                names.add(name);
            }
            if (names.size() >= limit) {
                break;
            }
        }
        return String.join(" / ", names);
    }

    public static String joinSources(List<ProcessingMachineBlock.Recipe> recipes, int limit) {
        List<String> names = new ArrayList<>();
        for (ProcessingMachineBlock.Recipe recipe : recipes) {
            String input = recipe.inputStack().getItemName().getString();
            String name = recipe.kind().localizedDisplayName() + ": " + input;
            if (!names.contains(name)) {
                names.add(name);
            }
            if (names.size() >= limit) {
                break;
            }
        }
        return String.join("；", names);
    }

    public static String describeOutputs(ProcessingMachineBlock.Recipe recipe) {
        List<String> outputs = new ArrayList<>();
        for (ProcessingMachineBlock.Output output : recipe.outputs()) {
            ItemStack out = output.stack();
            outputs.add(out.getCount() + "x " + out.getItemName().getString());
        }
        return String.join(" + ", outputs);
    }

    public static int uniqueMachineCount(List<ProcessingMachineBlock.Recipe> recipes) {
        List<ProcessingMachineBlock.Kind> kinds = new ArrayList<>();
        for (ProcessingMachineBlock.Recipe recipe : recipes) {
            if (!kinds.contains(recipe.kind())) {
                kinds.add(recipe.kind());
            }
        }
        return kinds.size();
    }

    public static final class RouteInfo {
        private static final RouteInfo EMPTY = new RouteInfo();

        private final List<ProcessingMachineBlock.Recipe> next = new ArrayList<>();
        private final List<ProcessingMachineBlock.Recipe> sources = new ArrayList<>();

        public List<ProcessingMachineBlock.Recipe> next() {
            return next;
        }

        public List<ProcessingMachineBlock.Recipe> sources() {
            return sources;
        }

        public boolean isEmpty() {
            return next.isEmpty() && sources.isEmpty();
        }
    }
}

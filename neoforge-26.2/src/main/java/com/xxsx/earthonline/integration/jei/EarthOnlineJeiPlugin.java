package com.xxsx.earthonline.integration.jei;

import com.xxsx.earthonline.EarthOnline;
import com.xxsx.earthonline.MachineMultiblock;
import com.xxsx.earthonline.ProcessingMachineBlock;
import com.xxsx.earthonline.RouteGuide;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Map;

@JeiPlugin
public class EarthOnlineJeiPlugin implements IModPlugin {
    public static final RecipeType<ProcessingMachineBlock.Recipe> PROCESSING =
            RecipeType.create(EarthOnline.MODID, "processing", ProcessingMachineBlock.Recipe.class);

    @Override
    public Identifier getPluginUid() {
        return EarthOnline.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ProcessingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PROCESSING, ProcessingMachineBlock.recipes());
        registerHandbookInfo(registration);
        registerMachineInfo(registration);
        registerRouteInfo(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(PROCESSING,
                EarthOnline.JAW_CRUSHER.get(),
                EarthOnline.BALL_MILL.get(),
                EarthOnline.SIEVE.get(),
                EarthOnline.MAGNETIC_SEPARATOR.get(),
                EarthOnline.FLOTATION_CELL.get(),
                EarthOnline.ORE_ROASTER.get(),
                EarthOnline.REDUCTION_FURNACE.get(),
                EarthOnline.LEACHING_TANK.get(),
                EarthOnline.ELECTROLYTIC_CELL.get(),
                EarthOnline.POWDER_PRESS.get(),
                EarthOnline.CHEMICAL_REACTOR.get(),
                EarthOnline.DISTILLATION_COLUMN.get(),
                EarthOnline.MIXER.get(),
                EarthOnline.CRYSTALLIZER.get(),
                EarthOnline.INDUSTRIAL_KILN.get(),
                EarthOnline.GAS_SEPARATOR.get(),
                EarthOnline.FERTILIZER_GRANULATOR.get(),
                EarthOnline.POLYMERIZER.get(),
                EarthOnline.STEAM_CRACKER.get(),
                EarthOnline.SYNTHESIS_LOOP.get(),
                EarthOnline.ABSORPTION_TOWER.get());
    }

    private static void registerHandbookInfo(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(EarthOnline.FIELD_GEOLOGY_NOTEBOOK.get()),
                lineKey("jei.earth_online.notebook.0", ChatFormatting.GOLD),
                lineKey("jei.earth_online.notebook.1", ChatFormatting.GRAY),
                lineKey("jei.earth_online.notebook.2", ChatFormatting.AQUA),
                lineKey("jei.earth_online.notebook.3", ChatFormatting.GREEN),
                lineKey("jei.earth_online.notebook.4", ChatFormatting.DARK_GRAY));
    }

    private static void registerMachineInfo(IRecipeRegistration registration) {
        registerMachine(registration, EarthOnline.JAW_CRUSHER.get(), ProcessingMachineBlock.Kind.CRUSHER);
        registerMachine(registration, EarthOnline.BALL_MILL.get(), ProcessingMachineBlock.Kind.BALL_MILL);
        registerMachine(registration, EarthOnline.SIEVE.get(), ProcessingMachineBlock.Kind.SIEVE);
        registerMachine(registration, EarthOnline.MAGNETIC_SEPARATOR.get(), ProcessingMachineBlock.Kind.MAGNETIC_SEPARATOR);
        registerMachine(registration, EarthOnline.FLOTATION_CELL.get(), ProcessingMachineBlock.Kind.FLOTATION_CELL);
        registerMachine(registration, EarthOnline.ORE_ROASTER.get(), ProcessingMachineBlock.Kind.ROASTER);
        registerMachine(registration, EarthOnline.REDUCTION_FURNACE.get(), ProcessingMachineBlock.Kind.REDUCTION_FURNACE);
        registerMachine(registration, EarthOnline.LEACHING_TANK.get(), ProcessingMachineBlock.Kind.LEACHING_TANK);
        registerMachine(registration, EarthOnline.ELECTROLYTIC_CELL.get(), ProcessingMachineBlock.Kind.ELECTROLYTIC_CELL);
        registerMachine(registration, EarthOnline.POWDER_PRESS.get(), ProcessingMachineBlock.Kind.POWDER_PRESS);
        registerMachine(registration, EarthOnline.CHEMICAL_REACTOR.get(), ProcessingMachineBlock.Kind.CHEMICAL_REACTOR);
        registerMachine(registration, EarthOnline.DISTILLATION_COLUMN.get(), ProcessingMachineBlock.Kind.DISTILLATION_COLUMN);
        registerMachine(registration, EarthOnline.MIXER.get(), ProcessingMachineBlock.Kind.MIXER);
        registerMachine(registration, EarthOnline.CRYSTALLIZER.get(), ProcessingMachineBlock.Kind.CRYSTALLIZER);
        registerMachine(registration, EarthOnline.INDUSTRIAL_KILN.get(), ProcessingMachineBlock.Kind.INDUSTRIAL_KILN);
        registerMachine(registration, EarthOnline.GAS_SEPARATOR.get(), ProcessingMachineBlock.Kind.GAS_SEPARATOR);
        registerMachine(registration, EarthOnline.FERTILIZER_GRANULATOR.get(), ProcessingMachineBlock.Kind.FERTILIZER_GRANULATOR);
        registerMachine(registration, EarthOnline.POLYMERIZER.get(), ProcessingMachineBlock.Kind.POLYMERIZER);
        registerMachine(registration, EarthOnline.STEAM_CRACKER.get(), ProcessingMachineBlock.Kind.STEAM_CRACKER);
        registerMachine(registration, EarthOnline.SYNTHESIS_LOOP.get(), ProcessingMachineBlock.Kind.SYNTHESIS_LOOP);
        registerMachine(registration, EarthOnline.ABSORPTION_TOWER.get(), ProcessingMachineBlock.Kind.ABSORPTION_TOWER);
    }

    private static void registerMachine(IRecipeRegistration registration, ItemLike item, ProcessingMachineBlock.Kind kind) {
        registration.addItemStackInfo(new ItemStack(item.asItem()),
                Component.translatable(kind.displayNameKey()).withStyle(ChatFormatting.GOLD),
                Component.translatable(kind.descriptionKey()).withStyle(ChatFormatting.GRAY),
                lineKey("tooltip.earth_online.machine.use", ChatFormatting.AQUA),
                Component.translatable(MachineMultiblock.patternFor(kind).descriptionKey()).withStyle(ChatFormatting.GREEN),
                lineKey("tooltip.earth_online.machine.redstone", ChatFormatting.GREEN),
                lineKey("tooltip.earth_online.machine.routes", ChatFormatting.DARK_GRAY, ProcessingMachineBlock.recipesFor(kind).size()));
    }

    private static void registerRouteInfo(IRecipeRegistration registration) {
        for (Map.Entry<Item, RouteGuide.RouteInfo> entry : RouteGuide.buildRoutesByItem().entrySet()) {
            Item item = entry.getKey();
            RouteGuide.RouteInfo info = entry.getValue();
            java.util.List<Component> lines = new java.util.ArrayList<>();
            lines.add(lineKey("jei.earth_online.route.header", ChatFormatting.GOLD));
            if (!info.next().isEmpty()) {
                ProcessingMachineBlock.Recipe example = info.next().get(0);
                lines.add(lineKey("tooltip.earth_online.route.next", ChatFormatting.AQUA, RouteGuide.joinMachines(info.next(), 4)));
                lines.add(lineKey("tooltip.earth_online.route.outputs", ChatFormatting.GRAY, RouteGuide.describeOutputs(example)));
            }
            if (!info.sources().isEmpty()) {
                lines.add(lineKey("tooltip.earth_online.route.sources", ChatFormatting.DARK_GREEN, RouteGuide.joinSources(info.sources(), 3)));
            }
            lines.add(lineKey("jei.earth_online.route.footer", ChatFormatting.DARK_GRAY));
            registration.addItemStackInfo(new ItemStack(item), lines.toArray(Component[]::new));
        }
    }

    private static Component line(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    private static Component lineKey(String key, ChatFormatting color, Object... args) {
        return Component.translatable(key, args).withStyle(color);
    }
}

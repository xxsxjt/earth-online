package com.xxsx.earthonminecraft.integration.jei;

import com.xxsx.earthonminecraft.EarthOnMinecraft;
import com.xxsx.earthonminecraft.MachineMultiblock;
import com.xxsx.earthonminecraft.ProcessingMachineBlock;
import com.xxsx.earthonminecraft.RouteGuide;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@JeiPlugin
public class EarthOnMinecraftJeiPlugin implements IModPlugin {
    private static final Map<ProcessingMachineBlock.Kind, IRecipeType<ProcessingMachineBlock.Recipe>> PROCESSING_TYPES =
            createProcessingTypes();
    private static final ChatFormatting JEI_BODY = ChatFormatting.WHITE;
    private static final ChatFormatting JEI_HINT = ChatFormatting.YELLOW;

    @Override
    public Identifier getPluginUid() {
        return EarthOnMinecraft.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (ProcessingMachineBlock.Kind kind : ProcessingMachineBlock.Kind.values()) {
            registration.addRecipeCategories(new ProcessingJeiCategory(guiHelper, kind, recipeTypeFor(kind), machineFor(kind)));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (ProcessingMachineBlock.Kind kind : ProcessingMachineBlock.Kind.values()) {
            registration.addRecipes(recipeTypeFor(kind), ProcessingMachineBlock.recipesFor(kind));
        }
        registerHandbookInfo(registration);
        registerLivingWorldInfo(registration);
        registerMachineInfo(registration);
        registerEnergyInfo(registration);
        registerRouteInfo(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ProcessingMachineBlock.Kind kind : ProcessingMachineBlock.Kind.values()) {
            registration.addCraftingStation(recipeTypeFor(kind), machineFor(kind));
        }
    }

    public static IRecipeType<ProcessingMachineBlock.Recipe> recipeTypeFor(ProcessingMachineBlock.Kind kind) {
        return PROCESSING_TYPES.get(kind);
    }

    private static Map<ProcessingMachineBlock.Kind, IRecipeType<ProcessingMachineBlock.Recipe>> createProcessingTypes() {
        EnumMap<ProcessingMachineBlock.Kind, IRecipeType<ProcessingMachineBlock.Recipe>> types =
                new EnumMap<>(ProcessingMachineBlock.Kind.class);
        for (ProcessingMachineBlock.Kind kind : ProcessingMachineBlock.Kind.values()) {
            types.put(kind, IRecipeType.create(EarthOnMinecraft.MODID, "processing_" + kind.blockId(), ProcessingMachineBlock.Recipe.class));
        }
        return Collections.unmodifiableMap(types);
    }

    private static ItemLike machineFor(ProcessingMachineBlock.Kind kind) {
        return switch (kind) {
            case CRUSHER -> EarthOnMinecraft.JAW_CRUSHER.get();
            case BALL_MILL -> EarthOnMinecraft.BALL_MILL.get();
            case SIEVE -> EarthOnMinecraft.SIEVE.get();
            case MAGNETIC_SEPARATOR -> EarthOnMinecraft.MAGNETIC_SEPARATOR.get();
            case FLOTATION_CELL -> EarthOnMinecraft.FLOTATION_CELL.get();
            case ROASTER -> EarthOnMinecraft.ORE_ROASTER.get();
            case REDUCTION_FURNACE -> EarthOnMinecraft.REDUCTION_FURNACE.get();
            case LEACHING_TANK -> EarthOnMinecraft.LEACHING_TANK.get();
            case ELECTROLYTIC_CELL -> EarthOnMinecraft.ELECTROLYTIC_CELL.get();
            case POWDER_PRESS -> EarthOnMinecraft.POWDER_PRESS.get();
            case CHEMICAL_REACTOR -> EarthOnMinecraft.CHEMICAL_REACTOR.get();
            case DISTILLATION_COLUMN -> EarthOnMinecraft.DISTILLATION_COLUMN.get();
            case MIXER -> EarthOnMinecraft.MIXER.get();
            case CRYSTALLIZER -> EarthOnMinecraft.CRYSTALLIZER.get();
            case INDUSTRIAL_KILN -> EarthOnMinecraft.INDUSTRIAL_KILN.get();
            case GAS_SEPARATOR -> EarthOnMinecraft.GAS_SEPARATOR.get();
            case FERTILIZER_GRANULATOR -> EarthOnMinecraft.FERTILIZER_GRANULATOR.get();
            case POLYMERIZER -> EarthOnMinecraft.POLYMERIZER.get();
            case STEAM_CRACKER -> EarthOnMinecraft.STEAM_CRACKER.get();
            case SYNTHESIS_LOOP -> EarthOnMinecraft.SYNTHESIS_LOOP.get();
            case ABSORPTION_TOWER -> EarthOnMinecraft.ABSORPTION_TOWER.get();
        };
    }

    private static void registerHandbookInfo(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(EarthOnMinecraft.FIELD_GEOLOGY_NOTEBOOK.get()),
                lineKey("jei.earth_on_minecraft.notebook.0", ChatFormatting.GOLD),
                lineKey("jei.earth_on_minecraft.notebook.1", JEI_BODY),
                lineKey("jei.earth_on_minecraft.notebook.2", ChatFormatting.AQUA),
                lineKey("jei.earth_on_minecraft.notebook.3", ChatFormatting.GREEN),
                lineKey("jei.earth_on_minecraft.notebook.4", ChatFormatting.GOLD),
                lineKey("jei.earth_on_minecraft.notebook.5", ChatFormatting.AQUA),
                lineKey("jei.earth_on_minecraft.notebook.6", JEI_HINT));
    }

    private static void registerLivingWorldInfo(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(EarthOnMinecraft.SETTLEMENT_BOARD.get()),
                lineKey("jei.earth_on_minecraft.settlement_board.0", ChatFormatting.GOLD),
                lineKey("jei.earth_on_minecraft.settlement_board.1", JEI_BODY),
                lineKey("jei.earth_on_minecraft.settlement_board.2", ChatFormatting.AQUA),
                lineKey("jei.earth_on_minecraft.settlement_board.3", ChatFormatting.GREEN),
                lineKey("jei.earth_on_minecraft.settlement_board.4", JEI_HINT));
    }

    private static void registerMachineInfo(IRecipeRegistration registration) {
        registerMachine(registration, EarthOnMinecraft.JAW_CRUSHER.get(), ProcessingMachineBlock.Kind.CRUSHER);
        registerMachine(registration, EarthOnMinecraft.BALL_MILL.get(), ProcessingMachineBlock.Kind.BALL_MILL);
        registerMachine(registration, EarthOnMinecraft.SIEVE.get(), ProcessingMachineBlock.Kind.SIEVE);
        registerMachine(registration, EarthOnMinecraft.MAGNETIC_SEPARATOR.get(), ProcessingMachineBlock.Kind.MAGNETIC_SEPARATOR);
        registerMachine(registration, EarthOnMinecraft.FLOTATION_CELL.get(), ProcessingMachineBlock.Kind.FLOTATION_CELL);
        registerMachine(registration, EarthOnMinecraft.ORE_ROASTER.get(), ProcessingMachineBlock.Kind.ROASTER);
        registerMachine(registration, EarthOnMinecraft.REDUCTION_FURNACE.get(), ProcessingMachineBlock.Kind.REDUCTION_FURNACE);
        registerMachine(registration, EarthOnMinecraft.LEACHING_TANK.get(), ProcessingMachineBlock.Kind.LEACHING_TANK);
        registerMachine(registration, EarthOnMinecraft.ELECTROLYTIC_CELL.get(), ProcessingMachineBlock.Kind.ELECTROLYTIC_CELL);
        registerMachine(registration, EarthOnMinecraft.POWDER_PRESS.get(), ProcessingMachineBlock.Kind.POWDER_PRESS);
        registerMachine(registration, EarthOnMinecraft.CHEMICAL_REACTOR.get(), ProcessingMachineBlock.Kind.CHEMICAL_REACTOR);
        registerMachine(registration, EarthOnMinecraft.DISTILLATION_COLUMN.get(), ProcessingMachineBlock.Kind.DISTILLATION_COLUMN);
        registerMachine(registration, EarthOnMinecraft.MIXER.get(), ProcessingMachineBlock.Kind.MIXER);
        registerMachine(registration, EarthOnMinecraft.CRYSTALLIZER.get(), ProcessingMachineBlock.Kind.CRYSTALLIZER);
        registerMachine(registration, EarthOnMinecraft.INDUSTRIAL_KILN.get(), ProcessingMachineBlock.Kind.INDUSTRIAL_KILN);
        registerMachine(registration, EarthOnMinecraft.GAS_SEPARATOR.get(), ProcessingMachineBlock.Kind.GAS_SEPARATOR);
        registerMachine(registration, EarthOnMinecraft.FERTILIZER_GRANULATOR.get(), ProcessingMachineBlock.Kind.FERTILIZER_GRANULATOR);
        registerMachine(registration, EarthOnMinecraft.POLYMERIZER.get(), ProcessingMachineBlock.Kind.POLYMERIZER);
        registerMachine(registration, EarthOnMinecraft.STEAM_CRACKER.get(), ProcessingMachineBlock.Kind.STEAM_CRACKER);
        registerMachine(registration, EarthOnMinecraft.SYNTHESIS_LOOP.get(), ProcessingMachineBlock.Kind.SYNTHESIS_LOOP);
        registerMachine(registration, EarthOnMinecraft.ABSORPTION_TOWER.get(), ProcessingMachineBlock.Kind.ABSORPTION_TOWER);
    }

    private static void registerMachine(IRecipeRegistration registration, ItemLike item, ProcessingMachineBlock.Kind kind) {
        registration.addItemStackInfo(new ItemStack(item.asItem()),
                Component.translatable(kind.displayNameKey()).withStyle(ChatFormatting.GOLD),
                Component.translatable(kind.descriptionKey()).withStyle(JEI_BODY),
                lineKey("tooltip.earth_on_minecraft.machine.use", ChatFormatting.AQUA),
                lineKey("tooltip.earth_on_minecraft.machine.fuel", ChatFormatting.YELLOW),
                Component.translatable(MachineMultiblock.patternFor(kind).descriptionKey()).withStyle(ChatFormatting.GREEN),
                lineKey("tooltip.earth_on_minecraft.machine.redstone", ChatFormatting.GREEN),
                lineKey("tooltip.earth_on_minecraft.machine.routes", JEI_HINT, ProcessingMachineBlock.recipesFor(kind).size()));
    }

    private static void registerEnergyInfo(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(EarthOnMinecraft.COMBUSTION_GENERATOR.get()),
                lineKey("tooltip.earth_on_minecraft.energy.generator", ChatFormatting.GOLD),
                lineKey("jei.earth_on_minecraft.energy.generator.0", JEI_BODY),
                lineKey("jei.earth_on_minecraft.energy.generator.1", ChatFormatting.AQUA));
        registration.addItemStackInfo(new ItemStack(EarthOnMinecraft.THIN_COPPER_POWER_CABLE.get()),
                lineKey("tooltip.earth_on_minecraft.energy.cable", ChatFormatting.GOLD),
                lineKey("jei.earth_on_minecraft.energy.cable.0", JEI_BODY),
                lineKey("jei.earth_on_minecraft.energy.cable.1", ChatFormatting.AQUA));
        registration.addItemStackInfo(new ItemStack(EarthOnMinecraft.COPPER_POWER_CABLE.get()),
                lineKey("tooltip.earth_on_minecraft.energy.cable", ChatFormatting.GOLD),
                lineKey("jei.earth_on_minecraft.energy.cable.0", JEI_BODY),
                lineKey("jei.earth_on_minecraft.energy.cable.1", ChatFormatting.AQUA));
        registration.addItemStackInfo(new ItemStack(EarthOnMinecraft.HEAVY_COPPER_POWER_CABLE.get()),
                lineKey("tooltip.earth_on_minecraft.energy.cable", ChatFormatting.GOLD),
                lineKey("jei.earth_on_minecraft.energy.cable.0", JEI_BODY),
                lineKey("jei.earth_on_minecraft.energy.cable.1", ChatFormatting.AQUA));
        registration.addItemStackInfo(new ItemStack(EarthOnMinecraft.BATTERY_BOX.get()),
                lineKey("tooltip.earth_on_minecraft.energy.battery", ChatFormatting.GOLD),
                lineKey("jei.earth_on_minecraft.energy.battery.0", JEI_BODY),
                lineKey("jei.earth_on_minecraft.energy.battery.1", ChatFormatting.AQUA));
    }

    private static void registerRouteInfo(IRecipeRegistration registration) {
        for (Map.Entry<Item, RouteGuide.RouteInfo> entry : RouteGuide.buildRoutesByItem().entrySet()) {
            Item item = entry.getKey();
            RouteGuide.RouteInfo info = entry.getValue();
            java.util.List<Component> lines = new java.util.ArrayList<>();
            lines.add(lineKey("jei.earth_on_minecraft.route.header", ChatFormatting.GOLD));
            if (!info.next().isEmpty()) {
                ProcessingMachineBlock.Recipe example = info.next().get(0);
                lines.add(lineKey("tooltip.earth_on_minecraft.route.next", ChatFormatting.AQUA, RouteGuide.joinMachines(info.next(), 4)));
                lines.add(lineKey("tooltip.earth_on_minecraft.route.outputs", JEI_BODY, RouteGuide.describeOutputs(example)));
            }
            RouteGuide.addBeginnerShortcutTips(item, lines::add);
            if (!info.sources().isEmpty()) {
                lines.add(lineKey("tooltip.earth_on_minecraft.route.sources", ChatFormatting.DARK_GREEN, RouteGuide.joinSources(info.sources(), 3)));
            }
            lines.add(lineKey("jei.earth_on_minecraft.route.footer", JEI_HINT));
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

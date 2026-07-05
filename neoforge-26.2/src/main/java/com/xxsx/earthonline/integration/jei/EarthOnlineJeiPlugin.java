package com.xxsx.earthonline.integration.jei;

import com.xxsx.earthonline.EarthOnline;
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
                line("地球 Online 的主手册。", ChatFormatting.GOLD),
                line("它不是化学考试，而是路线导航：挖到什么、下一步去哪台机器、最后得到什么兼容物品。", ChatFormatting.GRAY),
                line("获取：一块泥土、任意木板或任意石头都能合成。", ChatFormatting.AQUA),
                line("使用：右键打开分页界面；机器界面也有“打开手册”按钮。", ChatFormatting.GREEN),
                line("JEI 联动：本页、机器页和材料页都会显示同一套路线提示。", ChatFormatting.DARK_GRAY));
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
                line(kind.displayName(), ChatFormatting.GOLD),
                line(kind.description(), ChatFormatting.GRAY),
                line("空手右键：打开这台机器的配方界面。", ChatFormatting.AQUA),
                line("手持输入右键：直接处理 1 个材料。", ChatFormatting.GREEN),
                line("JEI 工业处理分类中也能查看它的全部路线，共 " + ProcessingMachineBlock.recipesFor(kind).size() + " 条。", ChatFormatting.DARK_GRAY));
    }

    private static void registerRouteInfo(IRecipeRegistration registration) {
        for (Map.Entry<Item, RouteGuide.RouteInfo> entry : RouteGuide.buildRoutesByItem().entrySet()) {
            Item item = entry.getKey();
            RouteGuide.RouteInfo info = entry.getValue();
            java.util.List<Component> lines = new java.util.ArrayList<>();
            lines.add(line("Earth Online 路线提示", ChatFormatting.GOLD));
            if (!info.next().isEmpty()) {
                ProcessingMachineBlock.Recipe example = info.next().get(0);
                lines.add(line("下一步：放入 " + RouteGuide.joinMachines(info.next(), 4), ChatFormatting.AQUA));
                lines.add(line("示例产出：" + RouteGuide.describeOutputs(example), ChatFormatting.GRAY));
            }
            if (!info.sources().isEmpty()) {
                lines.add(line("常见来源：" + RouteGuide.joinSources(info.sources(), 3), ChatFormatting.DARK_GREEN));
            }
            lines.add(line("如果路线很多，查 JEI 的“Earth Online 工业处理”分类或右键野外地质手册。", ChatFormatting.DARK_GRAY));
            registration.addItemStackInfo(new ItemStack(item), lines.toArray(Component[]::new));
        }
    }

    private static Component line(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }
}

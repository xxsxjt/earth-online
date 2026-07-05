package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ProcessingMachineBlock extends Block {
    private static final List<Recipe> RECIPES = createRecipes();

    private final Kind kind;

    public ProcessingMachineBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            openClientMachineScreen(this.kind, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Optional<Recipe> match = RECIPES.stream()
                .filter(recipe -> recipe.kind == this.kind && stack.getItem() == recipe.input.get().asItem())
                .findFirst();

        if (match.isEmpty()) {
            player.sendSystemMessage(Component.literal("[地球 Online] ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(this.kind.displayName + "暂时不能处理：").withStyle(ChatFormatting.GRAY))
                    .append(stack.getItemName()));
            player.sendSystemMessage(Component.literal("空手右键机器可查看可处理材料。").withStyle(ChatFormatting.DARK_GRAY));
            return InteractionResult.SUCCESS_SERVER;
        }

        Recipe recipe = match.get();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        recipe.outputs.forEach(output -> give(player, new ItemStack(output.item.get().asItem(), output.count)));
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);

        player.sendSystemMessage(Component.literal("[地球 Online] ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(recipe.note + ": ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(describeOutputs(recipe.outputs)).withStyle(ChatFormatting.WHITE)));
        return InteractionResult.SUCCESS_SERVER;
    }

    private void sendHelp(Player player) {
        player.sendSystemMessage(Component.literal("== " + this.kind.displayName + " ==")
                .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal(this.kind.description).withStyle(ChatFormatting.GRAY));

        List<String> examples = RECIPES.stream()
                .filter(recipe -> recipe.kind == this.kind)
                .limit(6)
                .map(recipe -> new ItemStack(recipe.input.get().asItem()).getItemName().getString() + " -> " + describeOutputs(recipe.outputs))
                .toList();
        examples.forEach(line -> player.sendSystemMessage(Component.literal("  " + line).withStyle(ChatFormatting.DARK_GREEN)));
        if (examples.isEmpty()) {
            player.sendSystemMessage(Component.literal("  这台机器还没有处理路线。").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static List<Recipe> recipes() {
        return RECIPES;
    }

    public static List<Recipe> recipesFor(Kind kind) {
        return RECIPES.stream().filter(recipe -> recipe.kind == kind).toList();
    }

    private static void openClientMachineScreen(Kind kind, BlockPos pos) {
        try {
            Class.forName("com.xxsx.earthonline.client.EarthOnlineClient")
                    .getMethod("openMachine", Kind.class, BlockPos.class)
                    .invoke(null, kind, pos);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static String describeOutputs(List<Output> outputs) {
        List<String> names = new ArrayList<>();
        for (Output output : outputs) {
            ItemStack stack = new ItemStack(output.item.get().asItem(), output.count);
            names.add(output.count + "x " + stack.getItemName().getString());
        }
        return String.join(" + ", names);
    }

    private static List<Recipe> createRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        recipes.add(r(Kind.CRUSHER, EarthOnline.POOR_MAGNETITE_ORE::get, "贫磁铁矿粗碎", out(EarthOnline.MAGNETITE_CHUNK::get, 2), out(EarthOnline.TAILINGS_DUST::get, 2)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.MAGNETITE_ORE::get, "磁铁矿粗碎", out(EarthOnline.MAGNETITE_CHUNK::get, 4), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.RICH_MAGNETITE_ORE::get, "富磁铁矿粗碎", out(EarthOnline.MAGNETITE_CHUNK::get, 6), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.CHALCOPYRITE_ORE::get, "黄铜矿粗碎", out(EarthOnline.CHALCOPYRITE_CHUNK::get, 4), out(EarthOnline.PYRITE_DUST::get, 1)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.AURIFEROUS_QUARTZ_VEIN::get, "含金石英脉粗碎", out(EarthOnline.AURIFEROUS_QUARTZ_CHUNK::get, 3), out(EarthOnline.QUARTZ_DUST::get, 2)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.KIMBERLITE::get, "金伯利岩粗碎", out(EarthOnline.KIMBERLITE_CHUNK::get, 4), out(EarthOnline.TAILINGS_DUST::get, 2)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.DIAMONDIFEROUS_KIMBERLITE::get, "含钻金伯利岩粗碎", out(EarthOnline.KIMBERLITE_CHUNK::get, 4), out(EarthOnline.DIAMOND_GRIT::get, 1)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.CINNABAR_VEIN::get, "辰砂矿脉粗碎", out(EarthOnline.CINNABAR_CHUNK::get, 4), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.BITUMINOUS_COAL_SEAM::get, "烟煤煤层破碎", out(EarthOnline.COAL_DUST::get, 5), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.CRUSHER, EarthOnline.ANTHRACITE_COAL_SEAM::get, "无烟煤煤层破碎", out(EarthOnline.COAL_DUST::get, 6)));

        recipes.add(r(Kind.BALL_MILL, EarthOnline.MAGNETITE_CHUNK::get, "磁铁矿球磨", out(EarthOnline.MAGNETITE_DUST::get, 3), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, EarthOnline.CHALCOPYRITE_CHUNK::get, "黄铜矿球磨", out(EarthOnline.CHALCOPYRITE_DUST::get, 3), out(EarthOnline.PYRITE_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, EarthOnline.CINNABAR_CHUNK::get, "辰砂球磨", out(EarthOnline.CINNABAR_DUST::get, 3), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.GRANITE, "花岗岩矿物分离", out(EarthOnline.QUARTZ_DUST::get, 2), out(EarthOnline.FELDSPAR_DUST::get, 3), out(EarthOnline.MICA_DUST::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.DIORITE, "闪长岩矿物分离", out(EarthOnline.FELDSPAR_DUST::get, 3), out(EarthOnline.MAFIC_SILICATE_DUST::get, 2), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.ANDESITE, "安山岩矿物分离", out(EarthOnline.FELDSPAR_DUST::get, 2), out(EarthOnline.MAFIC_SILICATE_DUST::get, 3), out(EarthOnline.MAGNETITE_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.BASALT, "玄武岩矿物分离", out(EarthOnline.MAFIC_SILICATE_DUST::get, 4), out(EarthOnline.MAGNETITE_DUST::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.DEEPSLATE, "深板岩矿物分离", out(EarthOnline.ALUMINOSILICATE_DUST::get, 4), out(EarthOnline.MICA_DUST::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.TUFF, "凝灰岩矿物分离", out(EarthOnline.SILICA_DUST::get, 2), out(EarthOnline.MAFIC_SILICATE_DUST::get, 2), out(EarthOnline.TAILINGS_DUST::get, 2)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.CALCITE, "方解石粉磨", out(EarthOnline.CALCITE_DUST::get, 5)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.DRIPSTONE_BLOCK, "钙质滴石粉磨", out(EarthOnline.CALCITE_DUST::get, 4), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.SANDSTONE, "砂岩粉磨", out(EarthOnline.SILICA_DUST::get, 5), out(EarthOnline.CALCITE_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.BLACKSTONE, "富铁镁质黑石粉磨", out(EarthOnline.MAFIC_SILICATE_DUST::get, 4), out(EarthOnline.HEMATITE_DUST::get, 1)));

        recipes.add(r(Kind.SIEVE, EarthOnline.TAILINGS_DUST::get, "尾粉筛分", out(EarthOnline.SILICA_DUST::get, 1), out(EarthOnline.ALUMINOSILICATE_DUST::get, 1)));
        recipes.add(r(Kind.SIEVE, EarthOnline.KIMBERLITE_CHUNK::get, "金伯利岩重矿物筛分", out(EarthOnline.DIAMOND_GRIT::get, 1), out(EarthOnline.MAFIC_SILICATE_DUST::get, 2), out(EarthOnline.TAILINGS_DUST::get, 2)));
        recipes.add(r(Kind.SIEVE, EarthOnline.AURIFEROUS_QUARTZ_CHUNK::get, "含金石英碎块筛分", out(EarthOnline.GOLD_DUST::get, 1), out(EarthOnline.QUARTZ_DUST::get, 2), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.SIEVE, EarthOnline.MICA_DUST::get, "云母片状矿物筛分", out(EarthOnline.ALUMINOSILICATE_DUST::get, 1), out(EarthOnline.SILICA_DUST::get, 1)));
        recipes.add(r(Kind.SIEVE, EarthOnline.FELDSPAR_DUST::get, "长石粉筛分除杂", out(EarthOnline.ALUMINOSILICATE_DUST::get, 1), out(EarthOnline.SILICA_DUST::get, 1)));

        recipes.add(r(Kind.MAGNETIC_SEPARATOR, EarthOnline.MAGNETITE_DUST::get, "磁选铁精矿", out(EarthOnline.IRON_CONCENTRATE::get, 2), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.MAGNETIC_SEPARATOR, EarthOnline.HEMATITE_DUST::get, "赤铁矿弱磁分选", out(EarthOnline.IRON_CONCENTRATE::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.MAGNETIC_SEPARATOR, EarthOnline.MAFIC_SILICATE_DUST::get, "镁铁质硅酸盐除铁", out(EarthOnline.MAGNETITE_DUST::get, 1), out(EarthOnline.TAILINGS_DUST::get, 2)));

        recipes.add(r(Kind.FLOTATION_CELL, EarthOnline.CHALCOPYRITE_DUST::get, "黄铜矿浮选", out(EarthOnline.COPPER_CONCENTRATE::get, 2), out(EarthOnline.PYRITE_DUST::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.FLOTATION_CELL, EarthOnline.PYRITE_DUST::get, "黄铁矿浮选", out(EarthOnline.SULFUR_DUST::get, 1), out(EarthOnline.IRON_CONCENTRATE::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.FLOTATION_CELL, EarthOnline.GOLD_DUST::get, "金粉富集", out(EarthOnline.GOLD_CONCENTRATE::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.FLOTATION_CELL, EarthOnline.LAPIS_LAZULI_ORE::get, "青金石浮选", out(EarthOnline.LAPIS_CONCENTRATE::get, 2), out(EarthOnline.CALCITE_DUST::get, 1), out(EarthOnline.PYRITE_DUST::get, 1)));
        recipes.add(r(Kind.FLOTATION_CELL, EarthOnline.REDSTONE_MINERAL_ORE::get, "红石矿物富集", out(EarthOnline.REDSTONE_CONCENTRATE::get, 2), out(EarthOnline.SILICA_DUST::get, 1)));

        recipes.add(r(Kind.ROASTER, EarthOnline.COPPER_CONCENTRATE::get, "铜精矿焙烧", out(EarthOnline.ROASTED_COPPER_CONCENTRATE::get, 1), out(EarthOnline.SULFUR_DUST::get, 2)));
        recipes.add(r(Kind.ROASTER, EarthOnline.PYRITE_DUST::get, "黄铁矿焙烧", out(EarthOnline.HEMATITE_DUST::get, 1), out(EarthOnline.SULFUR_DUST::get, 2)));
        recipes.add(r(Kind.ROASTER, EarthOnline.CINNABAR_DUST::get, "辰砂焙烧", out(EarthOnline.MERCURY_DROPLET::get, 1), out(EarthOnline.SULFUR_DUST::get, 1)));
        recipes.add(r(Kind.ROASTER, EarthOnline.CALCITE_DUST::get, "碳酸盐煅烧", out(EarthOnline.LIME_DUST::get, 1)));

        recipes.add(r(Kind.REDUCTION_FURNACE, EarthOnline.IRON_CONCENTRATE::get, "铁精矿碳热还原", out(() -> Items.IRON_INGOT, 1), out(EarthOnline.SLAG::get, 1)));
        recipes.add(r(Kind.REDUCTION_FURNACE, EarthOnline.ROASTED_COPPER_CONCENTRATE::get, "焙烧铜精矿还原", out(() -> Items.COPPER_INGOT, 1), out(EarthOnline.SLAG::get, 1)));
        recipes.add(r(Kind.REDUCTION_FURNACE, EarthOnline.GOLD_CONCENTRATE::get, "金精矿熔炼", out(() -> Items.GOLD_INGOT, 1), out(EarthOnline.SLAG::get, 1)));
        recipes.add(r(Kind.REDUCTION_FURNACE, EarthOnline.COAL_DUST::get, "煤粉压焦近似处理", out(() -> Items.COAL, 1)));
        recipes.add(r(Kind.REDUCTION_FURNACE, () -> Items.IRON_INGOT, "生铁到钢坯的简化精炼", out(EarthOnline.STEEL_BLOOM::get, 1), out(EarthOnline.SLAG::get, 1)));
        recipes.add(r(Kind.REDUCTION_FURNACE, EarthOnline.SILICA_DUST::get, "硅石碳热还原制硅铁", out(EarthOnline.FERROSILICON::get, 1), out(EarthOnline.SLAG::get, 1)));
        recipes.add(r(Kind.REDUCTION_FURNACE, EarthOnline.COKE::get, "焦炭高温还原气氛", out(EarthOnline.COAL_GAS_CELL::get, 1), out(EarthOnline.SLAG::get, 1)));
        recipes.add(r(Kind.REDUCTION_FURNACE, EarthOnline.FERROSILICON::get, "硅铁脱氧辅助炼钢", out(EarthOnline.STEEL_BLOOM::get, 1), out(EarthOnline.SLAG::get, 1)));

        recipes.add(r(Kind.LEACHING_TANK, EarthOnline.AURIFEROUS_QUARTZ_CHUNK::get, "含金石英浸出", out(EarthOnline.GOLD_CONCENTRATE::get, 1), out(EarthOnline.SILICA_DUST::get, 2), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.LEACHING_TANK, EarthOnline.EMERALD_BERYL_VEIN::get, "绿柱石浸出富集", out(EarthOnline.BERYL_CONCENTRATE::get, 2), out(EarthOnline.QUARTZ_DUST::get, 1), out(EarthOnline.MICA_DUST::get, 1)));
        recipes.add(r(Kind.LEACHING_TANK, EarthOnline.REDSTONE_CONCENTRATE::get, "红石矿物浸出", out(() -> Items.REDSTONE, 4), out(EarthOnline.SILICA_DUST::get, 1)));
        recipes.add(r(Kind.LEACHING_TANK, EarthOnline.MERCURY_DROPLET::get, "汞齐法金回收的安全化近似", out(EarthOnline.GOLD_CONCENTRATE::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));

        recipes.add(r(Kind.ELECTROLYTIC_CELL, EarthOnline.COPPER_CONCENTRATE::get, "铜精矿电积", out(() -> Items.COPPER_INGOT, 1), out(EarthOnline.SULFUR_DUST::get, 1), out(EarthOnline.SLAG::get, 1)));
        recipes.add(r(Kind.ELECTROLYTIC_CELL, EarthOnline.GOLD_CONCENTRATE::get, "金精矿电解精炼", out(() -> Items.GOLD_INGOT, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.ELECTROLYTIC_CELL, EarthOnline.LAPIS_CONCENTRATE::get, "青金石湿法提纯", out(() -> Items.LAPIS_LAZULI, 3), out(EarthOnline.CALCITE_DUST::get, 1)));
        recipes.add(r(Kind.ELECTROLYTIC_CELL, EarthOnline.BERYL_CONCENTRATE::get, "绿柱石宝石分选", out(() -> Items.EMERALD, 1), out(EarthOnline.ALUMINOSILICATE_DUST::get, 2)));
        recipes.add(r(Kind.ELECTROLYTIC_CELL, () -> Items.WATER_BUCKET, "水电解气体回收", out(EarthOnline.HYDROGEN_GAS_CELL::get, 2), out(EarthOnline.OXYGEN_GAS_CELL::get, 1), out(() -> Items.BUCKET, 1)));

        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.COAL_DUST::get, "煤粉压块", out(() -> Items.COAL, 1)));
        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.DIAMOND_GRIT::get, "金刚石砂粒压制", out(() -> Items.DIAMOND, 1)));
        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.QUARTZ_DUST::get, "石英粉压制", out(() -> Items.QUARTZ, 1)));
        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.REDSTONE_CONCENTRATE::get, "红石精矿压制", out(() -> Items.REDSTONE, 3)));
        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.STEEL_BLOOM::get, "钢坯轧制为兼容铁锭", out(() -> Items.IRON_INGOT, 1)));

        recipes.add(r(Kind.GAS_SEPARATOR, EarthOnline.COAL_DUST::get, "煤粉干馏分离", out(EarthOnline.COKE::get, 1), out(EarthOnline.COAL_TAR::get, 1), out(EarthOnline.COAL_GAS_CELL::get, 1)));
        recipes.add(r(Kind.GAS_SEPARATOR, EarthOnline.COAL_GAS_CELL::get, "煤气净化", out(EarthOnline.HYDROGEN_GAS_CELL::get, 1), out(EarthOnline.AMMONIA::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.GAS_SEPARATOR, EarthOnline.CHLORINE_GAS_CELL::get, "氯气吸收制酸", out(EarthOnline.HYDROCHLORIC_ACID::get, 1), out(EarthOnline.CALCIUM_CHLORIDE::get, 1)));
        recipes.add(r(Kind.GAS_SEPARATOR, () -> Items.GLASS_BOTTLE, "空气压缩分离", out(EarthOnline.NITROGEN_GAS_CELL::get, 3), out(EarthOnline.OXYGEN_GAS_CELL::get, 1)));
        recipes.add(r(Kind.GAS_SEPARATOR, EarthOnline.OXYGEN_GAS_CELL::get, "富氧尾气回收", out(EarthOnline.OXYGEN_GAS_CELL::get, 1), out(EarthOnline.NITROGEN_GAS_CELL::get, 1)));

        recipes.add(r(Kind.DISTILLATION_COLUMN, EarthOnline.COAL_TAR::get, "煤焦油分馏", out(EarthOnline.ETHYLENE::get, 1), out(EarthOnline.POLYMER_RESIN::get, 1), out(EarthOnline.COKE::get, 1)));
        recipes.add(r(Kind.DISTILLATION_COLUMN, EarthOnline.BRINE_CRYSTAL::get, "盐卤浓缩", out(EarthOnline.SALT_DUST::get, 2), out(EarthOnline.CALCIUM_CHLORIDE::get, 1)));
        recipes.add(r(Kind.DISTILLATION_COLUMN, EarthOnline.HYDROCHLORIC_ACID::get, "盐酸精馏", out(EarthOnline.HYDROCHLORIC_ACID::get, 1), out(EarthOnline.SALT_DUST::get, 1)));
        recipes.add(r(Kind.STEAM_CRACKER, EarthOnline.COAL_TAR::get, "煤焦油裂解芳烃和烯烃", out(EarthOnline.BENZENE::get, 1), out(EarthOnline.ETHYLENE::get, 1), out(EarthOnline.PROPYLENE::get, 1), out(EarthOnline.COKE::get, 1)));
        recipes.add(r(Kind.STEAM_CRACKER, EarthOnline.POLYMER_RESIN::get, "混合树脂热裂解回收单体", out(EarthOnline.ETHYLENE::get, 1), out(EarthOnline.PROPYLENE::get, 1)));
        recipes.add(r(Kind.STEAM_CRACKER, EarthOnline.COAL_GAS_CELL::get, "合成气整备用裂解入口", out(EarthOnline.HYDROGEN_GAS_CELL::get, 1), out(EarthOnline.CARBON_DIOXIDE_CELL::get, 1), out(EarthOnline.METHANOL::get, 1)));

        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.SULFUR_DUST::get, "接触法硫酸近似路线", out(EarthOnline.SULFURIC_ACID::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.AMMONIA::get, "奥斯特瓦尔德法硝酸近似路线", out(EarthOnline.NITRIC_ACID::get, 1), out(EarthOnline.HYDROGEN_GAS_CELL::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.NITRIC_ACID::get, "硝酸铵中和", out(EarthOnline.AMMONIUM_NITRATE::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.PHOSPHATE_ROCK_DUST::get, "磷矿酸解", out(EarthOnline.PHOSPHORIC_ACID::get, 1), out(EarthOnline.GYPSUM_DUST::get, 2)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.LIME_DUST::get, "石灰消化", out(EarthOnline.SLAKED_LIME::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.SODA_ASH::get, "碳酸氢钠转化", out(EarthOnline.SODIUM_BICARBONATE::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.SULFURIC_ACID::get, "硫酸盐副产物回收", out(EarthOnline.SODIUM_SULFATE::get, 1), out(EarthOnline.GYPSUM_DUST::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.CARBON_DIOXIDE_CELL::get, "二氧化碳制纯碱近似路线", out(EarthOnline.SODA_ASH::get, 1), out(EarthOnline.SODIUM_BICARBONATE::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.METHANOL::get, "甲醇氧化制甲醛", out(EarthOnline.FORMALDEHYDE::get, 1), out(EarthOnline.HYDROGEN_GAS_CELL::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.ETHYLENE::get, "乙烯氯化制氯乙烯入口", out(EarthOnline.VINYL_CHLORIDE::get, 1), out(EarthOnline.HYDROCHLORIC_ACID::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.POTASSIUM_CHLORIDE::get, "钾盐复分解制硝酸钾", out(EarthOnline.POTASSIUM_NITRATE::get, 1), out(EarthOnline.SALT_DUST::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.SODIUM_BICARBONATE::get, "小苏打热分解回收纯碱", out(EarthOnline.SODA_ASH::get, 1), out(EarthOnline.CARBON_DIOXIDE_CELL::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.BENZENE::get, "苯系树脂前驱体近似路线", out(EarthOnline.POLYMER_RESIN::get, 1), out(EarthOnline.HYDROGEN_GAS_CELL::get, 1)));
        recipes.add(r(Kind.CHEMICAL_REACTOR, EarthOnline.SODIUM_SULFATE::get, "硫酸钠副产物制玻璃助熔剂", out(EarthOnline.GLASS_BATCH::get, 1), out(EarthOnline.SODA_ASH::get, 1)));

        recipes.add(r(Kind.SYNTHESIS_LOOP, EarthOnline.NITROGEN_GAS_CELL::get, "哈柏法合成氨近似路线", out(EarthOnline.AMMONIA::get, 2), out(EarthOnline.HYDROGEN_GAS_CELL::get, 1)));
        recipes.add(r(Kind.SYNTHESIS_LOOP, EarthOnline.HYDROGEN_GAS_CELL::get, "氢气循环合成氨/甲醇入口", out(EarthOnline.AMMONIA::get, 1), out(EarthOnline.METHANOL::get, 1)));
        recipes.add(r(Kind.SYNTHESIS_LOOP, EarthOnline.COAL_GAS_CELL::get, "煤气合成甲醇", out(EarthOnline.METHANOL::get, 1), out(EarthOnline.CARBON_DIOXIDE_CELL::get, 1)));
        recipes.add(r(Kind.SYNTHESIS_LOOP, EarthOnline.AMMONIA::get, "氨和二氧化碳合成尿素", out(EarthOnline.UREA::get, 1), out(EarthOnline.CARBON_DIOXIDE_CELL::get, 1)));
        recipes.add(r(Kind.SYNTHESIS_LOOP, EarthOnline.FORMALDEHYDE::get, "甲醛树脂前驱体", out(EarthOnline.POLYMER_RESIN::get, 1), out(() -> Items.WATER_BUCKET, 1)));

        recipes.add(r(Kind.ABSORPTION_TOWER, EarthOnline.SULFUR_DUST::get, "硫燃烧尾气吸收", out(EarthOnline.SULFUR_DIOXIDE_CELL::get, 1), out(EarthOnline.SULFURIC_ACID::get, 1)));
        recipes.add(r(Kind.ABSORPTION_TOWER, EarthOnline.SULFUR_DIOXIDE_CELL::get, "二氧化硫接触吸收", out(EarthOnline.SULFURIC_ACID::get, 2)));
        recipes.add(r(Kind.ABSORPTION_TOWER, EarthOnline.CHLORINE_GAS_CELL::get, "氯气吸收制盐酸", out(EarthOnline.HYDROCHLORIC_ACID::get, 1), out(EarthOnline.CALCIUM_CHLORIDE::get, 1)));
        recipes.add(r(Kind.ABSORPTION_TOWER, EarthOnline.CARBON_DIOXIDE_CELL::get, "碳酸化吸收", out(EarthOnline.SODA_ASH::get, 1), out(EarthOnline.SODIUM_BICARBONATE::get, 1)));

        recipes.add(r(Kind.MIXER, EarthOnline.SILICA_DUST::get, "玻璃配合料混合", out(EarthOnline.GLASS_BATCH::get, 1), out(EarthOnline.SODA_ASH::get, 1)));
        recipes.add(r(Kind.MIXER, EarthOnline.CALCITE_DUST::get, "水泥生料配比", out(EarthOnline.CEMENT_RAW_MEAL::get, 2), out(EarthOnline.CLAY_DUST::get, 1)));
        recipes.add(r(Kind.MIXER, EarthOnline.PHOSPHORIC_ACID::get, "磷肥母料混合", out(EarthOnline.FERTILIZER_BLEND::get, 1), out(EarthOnline.GYPSUM_DUST::get, 1)));
        recipes.add(r(Kind.MIXER, EarthOnline.SLAKED_LIME::get, "石灰乳吸收", out(EarthOnline.CALCIUM_CHLORIDE::get, 1), out(EarthOnline.CEMENT_POWDER::get, 1)));
        recipes.add(r(Kind.MIXER, EarthOnline.POTASSIUM_CHLORIDE::get, "钾肥粗混", out(EarthOnline.POTASH::get, 1), out(EarthOnline.FERTILIZER_BLEND::get, 1)));
        recipes.add(r(Kind.MIXER, EarthOnline.UREA::get, "尿素复合肥混合", out(EarthOnline.FERTILIZER_BLEND::get, 2), out(EarthOnline.SODIUM_BICARBONATE::get, 1)));
        recipes.add(r(Kind.MIXER, EarthOnline.SLAG::get, "矿渣水泥掺合料", out(EarthOnline.CEMENT_RAW_MEAL::get, 1), out(EarthOnline.CEMENT_POWDER::get, 1)));
        recipes.add(r(Kind.MIXER, EarthOnline.POTASH::get, "钾肥复合肥混合", out(EarthOnline.FERTILIZER_BLEND::get, 2)));
        recipes.add(r(Kind.MIXER, EarthOnline.ALUMINOSILICATE_DUST::get, "铝硅酸盐陶瓷坯料混合", out(EarthOnline.CEMENT_RAW_MEAL::get, 1), out(EarthOnline.CLAY_DUST::get, 1)));

        recipes.add(r(Kind.CRYSTALLIZER, EarthOnline.SALT_DUST::get, "盐结晶复溶", out(EarthOnline.BRINE_CRYSTAL::get, 1)));
        recipes.add(r(Kind.CRYSTALLIZER, EarthOnline.SODIUM_HYDROXIDE::get, "烧碱结晶", out(EarthOnline.SODIUM_HYDROXIDE::get, 1)));
        recipes.add(r(Kind.CRYSTALLIZER, EarthOnline.AMMONIUM_NITRATE::get, "硝酸铵造粒前结晶", out(EarthOnline.AMMONIUM_NITRATE::get, 1)));
        recipes.add(r(Kind.CRYSTALLIZER, EarthOnline.GYPSUM_DUST::get, "石膏结晶", out(EarthOnline.GYPSUM_DUST::get, 2)));
        recipes.add(r(Kind.CRYSTALLIZER, EarthOnline.BRINE_CRYSTAL::get, "盐卤分级结晶", out(EarthOnline.SALT_DUST::get, 2), out(EarthOnline.POTASSIUM_CHLORIDE::get, 1), out(EarthOnline.CALCIUM_CHLORIDE::get, 1)));

        recipes.add(r(Kind.INDUSTRIAL_KILN, EarthOnline.GLASS_BATCH::get, "玻璃熔制", out(() -> Items.GLASS, 1), out(EarthOnline.SLAG::get, 1)));
        recipes.add(r(Kind.INDUSTRIAL_KILN, EarthOnline.CEMENT_RAW_MEAL::get, "水泥熟料煅烧", out(EarthOnline.CEMENT_CLINKER::get, 1), out(EarthOnline.LIME_DUST::get, 1)));
        recipes.add(r(Kind.INDUSTRIAL_KILN, EarthOnline.CEMENT_CLINKER::get, "水泥粉磨", out(EarthOnline.CEMENT_POWDER::get, 2), out(EarthOnline.GYPSUM_DUST::get, 1)));
        recipes.add(r(Kind.INDUSTRIAL_KILN, EarthOnline.BAUXITE_DUST::get, "铝土矿煅烧", out(EarthOnline.ALUMINA::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.INDUSTRIAL_KILN, EarthOnline.CLAY_DUST::get, "黏土烧结制砖", out(() -> Items.BRICK, 2), out(EarthOnline.ALUMINOSILICATE_DUST::get, 1)));
        recipes.add(r(Kind.INDUSTRIAL_KILN, EarthOnline.FELDSPAR_DUST::get, "长石助熔制玻璃", out(() -> Items.GLASS, 1), out(EarthOnline.ALUMINOSILICATE_DUST::get, 1)));
        recipes.add(r(Kind.INDUSTRIAL_KILN, EarthOnline.ALUMINOSILICATE_DUST::get, "铝硅酸盐烧结成砖", out(() -> Items.BRICK, 1), out(EarthOnline.SLAG::get, 1)));

        recipes.add(r(Kind.ELECTROLYTIC_CELL, EarthOnline.SALT_DUST::get, "氯碱工业电解", out(EarthOnline.SODIUM_HYDROXIDE::get, 1), out(EarthOnline.CHLORINE_GAS_CELL::get, 1), out(EarthOnline.HYDROGEN_GAS_CELL::get, 1)));
        recipes.add(r(Kind.ELECTROLYTIC_CELL, EarthOnline.ALUMINUM_HYDROXIDE::get, "氢氧化铝电解前处理", out(EarthOnline.ALUMINA::get, 1), out(EarthOnline.HYDROGEN_GAS_CELL::get, 1)));
        recipes.add(r(Kind.ELECTROLYTIC_CELL, EarthOnline.ALUMINA::get, "氧化铝熔盐电解", out(EarthOnline.ALUMINUM_INGOT::get, 1), out(EarthOnline.OXYGEN_GAS_CELL::get, 1)));
        recipes.add(r(Kind.LEACHING_TANK, EarthOnline.BAUXITE_DUST::get, "拜耳法铝土矿浸出", out(EarthOnline.ALUMINUM_HYDROXIDE::get, 1), out(EarthOnline.TAILINGS_DUST::get, 2)));
        recipes.add(r(Kind.LEACHING_TANK, EarthOnline.CALCIUM_CHLORIDE::get, "氯化钙母液回收", out(EarthOnline.BRINE_CRYSTAL::get, 1), out(EarthOnline.SALT_DUST::get, 1)));

        recipes.add(r(Kind.FERTILIZER_GRANULATOR, EarthOnline.FERTILIZER_BLEND::get, "复合肥造粒", out(EarthOnline.FERTILIZER_BLEND::get, 2)));
        recipes.add(r(Kind.FERTILIZER_GRANULATOR, EarthOnline.AMMONIUM_NITRATE::get, "氮肥造粒", out(EarthOnline.FERTILIZER_BLEND::get, 1), out(EarthOnline.SODIUM_BICARBONATE::get, 1)));
        recipes.add(r(Kind.FERTILIZER_GRANULATOR, EarthOnline.PHOSPHATE_ROCK_DUST::get, "磷肥粗混", out(EarthOnline.FERTILIZER_BLEND::get, 1), out(EarthOnline.GYPSUM_DUST::get, 1)));
        recipes.add(r(Kind.FERTILIZER_GRANULATOR, EarthOnline.UREA::get, "尿素造粒", out(EarthOnline.FERTILIZER_BLEND::get, 2)));
        recipes.add(r(Kind.FERTILIZER_GRANULATOR, EarthOnline.POTASSIUM_NITRATE::get, "硝酸钾复合肥造粒", out(EarthOnline.FERTILIZER_BLEND::get, 2), out(EarthOnline.POTASH::get, 1)));

        recipes.add(r(Kind.POLYMERIZER, EarthOnline.ETHYLENE::get, "聚乙烯树脂聚合", out(EarthOnline.POLYETHYLENE_RESIN::get, 2)));
        recipes.add(r(Kind.POLYMERIZER, EarthOnline.COAL_TAR::get, "煤化工树脂前驱体", out(EarthOnline.POLYMER_RESIN::get, 1), out(EarthOnline.COKE::get, 1)));
        recipes.add(r(Kind.POLYMERIZER, EarthOnline.PROPYLENE::get, "聚丙烯树脂聚合", out(EarthOnline.POLYPROPYLENE_RESIN::get, 2)));
        recipes.add(r(Kind.POLYMERIZER, EarthOnline.VINYL_CHLORIDE::get, "PVC 树脂聚合", out(EarthOnline.PVC_RESIN::get, 2)));

        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.CEMENT_POWDER::get, "水泥粉压制为建筑块", out(() -> Items.STONE_BRICKS, 1)));
        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.POLYETHYLENE_RESIN::get, "聚乙烯拉丝成纤维", out(() -> Items.STRING, 2)));
        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.POLYPROPYLENE_RESIN::get, "聚丙烯拉丝成纤维", out(() -> Items.STRING, 2)));
        recipes.add(r(Kind.POWDER_PRESS, EarthOnline.PVC_RESIN::get, "PVC 树脂压制为弹性兼容材料", out(() -> Items.SLIME_BALL, 1)));

        recipes.add(r(Kind.BALL_MILL, () -> Blocks.CLAY, "黏土粉磨", out(EarthOnline.CLAY_DUST::get, 4), out(EarthOnline.ALUMINOSILICATE_DUST::get, 1)));
        recipes.add(r(Kind.BALL_MILL, () -> Blocks.DIRT, "土壤筛磨提盐和黏土", out(EarthOnline.CLAY_DUST::get, 1), out(EarthOnline.SALT_DUST::get, 1), out(EarthOnline.TAILINGS_DUST::get, 2)));
        recipes.add(r(Kind.BALL_MILL, () -> Items.BONE_MEAL, "骨粉磷酸盐富集", out(EarthOnline.PHOSPHATE_ROCK_DUST::get, 2), out(EarthOnline.CALCITE_DUST::get, 1)));
        recipes.add(r(Kind.SIEVE, () -> Blocks.SAND, "砂中重矿物筛分", out(EarthOnline.SILICA_DUST::get, 3), out(EarthOnline.BAUXITE_DUST::get, 1), out(EarthOnline.TAILINGS_DUST::get, 1)));
        recipes.add(r(Kind.CRYSTALLIZER, () -> Items.WATER_BUCKET, "盐水蒸发结晶", out(EarthOnline.BRINE_CRYSTAL::get, 1), out(() -> Items.BUCKET, 1)));

        return List.copyOf(recipes);
    }

    private static Recipe r(Kind kind, Supplier<? extends ItemLike> input, String note, Output... outputs) {
        return new Recipe(kind, input, note, List.of(outputs));
    }

    private static Output out(Supplier<? extends ItemLike> item, int count) {
        return new Output(item, count);
    }

    public record Recipe(Kind kind, Supplier<? extends ItemLike> input, String note, List<Output> outputs) {
        public ItemStack inputStack() {
            return new ItemStack(input.get().asItem());
        }

        public List<ItemStack> outputStacks() {
            return outputs.stream().map(Output::stack).toList();
        }
    }

    public record Output(Supplier<? extends ItemLike> item, int count) {
        public ItemStack stack() {
            return new ItemStack(item.get().asItem(), count);
        }
    }

    public enum Kind {
        CRUSHER("颚式破碎机", "把矿石和矿床方块粗碎成碎块，通常会产生尾粉或伴生矿物。"),
        BALL_MILL("球磨机", "把碎块和岩石磨成粉末，是分离矿物组分的基础步骤。"),
        SIEVE("筛分机", "按颗粒大小和密度做简化筛分，适合尾粉、金伯利岩和含金石英碎块。"),
        MAGNETIC_SEPARATOR("磁选机", "利用磁性分离磁铁矿、赤铁矿和镁铁质硅酸盐里的含铁组分。"),
        FLOTATION_CELL("浮选槽", "富集硫化矿和宝石矿物，让铜、金、青金石、红石等进入精矿。"),
        ROASTER("焙烧炉", "把硫化矿、辰砂和碳酸盐做热处理，产出焙烧矿、硫粉或石灰粉。"),
        REDUCTION_FURNACE("还原炉", "用简化碳热还原把精矿变成兼容 MC 的锭，同时产生矿渣。"),
        LEACHING_TANK("浸出槽", "模拟湿法冶金，从石英脉、绿柱石和红石精矿里选择性提取目标物。"),
        ELECTROLYTIC_CELL("电解槽", "用现代电化学路线精炼铜、金、青金石和绿柱石精矿。"),
        POWDER_PRESS("压粉机", "把粉末或精矿压回 MC 兼容物品，保留合成魔法的便利性。"),
        CHEMICAL_REACTOR("化学反应釜", "承载酸碱中和、酸解、氧化和常见无机化工反应的简化路线。"),
        DISTILLATION_COLUMN("精馏塔", "分离盐卤、盐酸和煤焦油等混合物，得到更集中的化工原料。"),
        MIXER("工业混合机", "把粉体和母料混合成玻璃配合料、水泥生料、肥料母料等中间物。"),
        CRYSTALLIZER("结晶器", "把盐、烧碱、硝酸铵、石膏等溶液或粉体整理成可堆叠产品。"),
        INDUSTRIAL_KILN("工业窑炉", "处理玻璃、水泥、铝土矿和其他需要高温煅烧的化工路线。"),
        GAS_SEPARATOR("气体分离器", "从煤气、氯气等气体混合物中回收氢、氨、盐酸等产品。"),
        FERTILIZER_GRANULATOR("肥料造粒机", "把氮肥、磷肥和复合肥母料整理成可运输的颗粒产品。"),
        POLYMERIZER("聚合釜", "把乙烯、丙烯和氯乙烯聚合为常见塑料树脂。"),
        STEAM_CRACKER("蒸汽裂解炉", "把煤焦油、煤气和混合树脂裂解成芳烃、烯烃与合成气入口。"),
        SYNTHESIS_LOOP("合成塔", "承载合成氨、甲醇、尿素和树脂前驱体这类高压循环反应。"),
        ABSORPTION_TOWER("吸收塔", "把酸性或含氯尾气吸收为硫酸、盐酸、纯碱等可用产品。");

        private final String displayName;
        private final String description;

        Kind(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }
    }
}

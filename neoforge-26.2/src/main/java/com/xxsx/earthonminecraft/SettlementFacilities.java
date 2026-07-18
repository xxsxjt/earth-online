package com.xxsx.earthonminecraft;

import com.xxsx.earthonminecraft.living.SettlementSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Optional;
import java.util.Set;

public final class SettlementFacilities {
    public static final String ADMINISTRATION = "settlement.earth_on_minecraft.facility.administration";
    public static final String MINING = "settlement.earth_on_minecraft.facility.mining";
    public static final String WORKSHOP = "settlement.earth_on_minecraft.facility.workshop";
    public static final String POWER = "settlement.earth_on_minecraft.facility.power";
    public static final String WATERWORKS = "settlement.earth_on_minecraft.facility.waterworks";
    public static final String WAREHOUSE = "settlement.earth_on_minecraft.facility.warehouse";

    private SettlementFacilities() {
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        facilityFor(event.getPlacedBlock()).ifPresent(facility ->
                SettlementSavedData.get(level).registerFacility(level, event.getPos(), facility));
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        facilityFor(event.getState()).ifPresent(facility ->
                SettlementSavedData.get(level).unregisterFacility(level, event.getPos(), facility));
    }

    public static void registerBoard(ServerLevel level, BlockPos pos) {
        SettlementSavedData.get(level).registerFacility(level, pos, ADMINISTRATION);
    }

    public static void indexNearby(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY(), center.getY() - verticalRadius);
        int maxY = Math.min(level.getMaxY() - 1, center.getY() + verticalRadius);
        for (int x = center.getX() - horizontalRadius; x <= center.getX() + horizontalRadius; x++) {
            for (int z = center.getZ() - horizontalRadius; z <= center.getZ() + horizontalRadius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    facilityFor(level.getBlockState(cursor)).ifPresent(facility ->
                            SettlementSavedData.get(level).registerFacility(level, cursor.immutable(), facility));
                }
            }
        }
    }

    public static Optional<String> facilityFor(BlockState state) {
        Block block = state.getBlock();
        if (block == EarthOnMinecraft.SETTLEMENT_BOARD.get()) {
            return Optional.of(ADMINISTRATION);
        }
        if (Set.of(
                EarthOnMinecraft.JAW_CRUSHER.get(), EarthOnMinecraft.BALL_MILL.get(), EarthOnMinecraft.SIEVE.get(),
                EarthOnMinecraft.MAGNETIC_SEPARATOR.get(), EarthOnMinecraft.FLOTATION_CELL.get()).contains(block)) {
            return Optional.of(MINING);
        }
        if (Set.of(
                EarthOnMinecraft.ORE_ROASTER.get(), EarthOnMinecraft.REDUCTION_FURNACE.get(),
                EarthOnMinecraft.INDUSTRIAL_KILN.get(), EarthOnMinecraft.POWDER_PRESS.get()).contains(block)) {
            return Optional.of(WORKSHOP);
        }
        if (Set.of(
                EarthOnMinecraft.COMBUSTION_GENERATOR.get(), EarthOnMinecraft.STEAM_TURBINE_GENERATOR.get(),
                EarthOnMinecraft.BATTERY_BOX.get(), EarthOnMinecraft.THIN_COPPER_POWER_CABLE.get(),
                EarthOnMinecraft.COPPER_POWER_CABLE.get(), EarthOnMinecraft.HEAVY_COPPER_POWER_CABLE.get()).contains(block)) {
            return Optional.of(POWER);
        }
        if (Set.of(
                EarthOnMinecraft.LEACHING_TANK.get(), EarthOnMinecraft.ELECTROLYTIC_CELL.get(),
                EarthOnMinecraft.CHEMICAL_REACTOR.get(), EarthOnMinecraft.GAS_SEPARATOR.get(),
                EarthOnMinecraft.ABSORPTION_TOWER.get()).contains(block)) {
            return Optional.of(WATERWORKS);
        }
        if (Set.of(
                EarthOnMinecraft.CONVEYOR_BELT.get(), EarthOnMinecraft.MACHINE_INPUT_INTERFACE.get(),
                EarthOnMinecraft.MACHINE_OUTPUT_INTERFACE.get()).contains(block)) {
            return Optional.of(WAREHOUSE);
        }
        return Optional.empty();
    }
}

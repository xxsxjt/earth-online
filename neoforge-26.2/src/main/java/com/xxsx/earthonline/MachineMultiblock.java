package com.xxsx.earthonline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class MachineMultiblock {
    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private MachineMultiblock() {
    }

    public static Pattern patternFor(ProcessingMachineBlock.Kind kind) {
        return switch (kind) {
            case GAS_SEPARATOR -> Pattern.HEAVY_FRAME;
            case FLOTATION_CELL, LEACHING_TANK, ELECTROLYTIC_CELL, CHEMICAL_REACTOR, POLYMERIZER -> Pattern.WET_VESSEL;
            case ROASTER, REDUCTION_FURNACE, INDUSTRIAL_KILN, STEAM_CRACKER -> Pattern.HEATED_LINE;
            case DISTILLATION_COLUMN, SYNTHESIS_LOOP, ABSORPTION_TOWER -> Pattern.TALL_COLUMN;
            default -> Pattern.NONE;
        };
    }

    public static boolean isComplete(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind) {
        return patternFor(kind).isComplete(level, controller);
    }

    public enum Pattern {
        NONE("tooltip.earth_online.multiblock.none", "screen.earth_online.machine.structure.none") {
            @Override
            boolean isComplete(Level level, BlockPos controller) {
                return true;
            }
        },
        HEAVY_FRAME("tooltip.earth_online.multiblock.heavy_frame", "screen.earth_online.machine.structure.heavy_frame") {
            @Override
            boolean isComplete(Level level, BlockPos controller) {
                for (Direction front : HORIZONTALS) {
                    Direction left = front.getCounterClockWise();
                    Direction right = front.getClockWise();
                    Direction back = front.getOpposite();
                    if (has(level, controller.relative(front), EarthOnline.CONTROL_PANEL.get())
                            && has(level, controller.relative(left), EarthOnline.INDUSTRIAL_MACHINE_CASING.get())
                            && has(level, controller.relative(right), EarthOnline.INDUSTRIAL_MACHINE_CASING.get())
                            && has(level, controller.relative(back), EarthOnline.INDUSTRIAL_MACHINE_CASING.get())) {
                        return true;
                    }
                }
                return false;
            }
        },
        WET_VESSEL("tooltip.earth_online.multiblock.wet_vessel", "screen.earth_online.machine.structure.wet_vessel") {
            @Override
            boolean isComplete(Level level, BlockPos controller) {
                for (Direction front : HORIZONTALS) {
                    Direction left = front.getCounterClockWise();
                    Direction right = front.getClockWise();
                    Direction back = front.getOpposite();
                    if (has(level, controller.relative(front), EarthOnline.CONTROL_PANEL.get())
                            && has(level, controller.relative(left), EarthOnline.STEEL_PROCESS_PIPE.get())
                            && has(level, controller.relative(right), EarthOnline.STEEL_PROCESS_PIPE.get())
                            && has(level, controller.relative(back), EarthOnline.INDUSTRIAL_MACHINE_CASING.get())) {
                        return true;
                    }
                }
                return false;
            }
        },
        HEATED_LINE("tooltip.earth_online.multiblock.heated_line", "screen.earth_online.machine.structure.heated_line") {
            @Override
            boolean isComplete(Level level, BlockPos controller) {
                for (Direction front : HORIZONTALS) {
                    Direction left = front.getCounterClockWise();
                    Direction right = front.getClockWise();
                    if (has(level, controller.relative(front), EarthOnline.CONTROL_PANEL.get())
                            && has(level, controller.relative(left), EarthOnline.INDUSTRIAL_MACHINE_CASING.get())
                            && has(level, controller.relative(right), EarthOnline.INDUSTRIAL_MACHINE_CASING.get())
                            && has(level, controller.above(), EarthOnline.STEEL_PROCESS_PIPE.get())) {
                        return true;
                    }
                }
                return false;
            }
        },
        TALL_COLUMN("tooltip.earth_online.multiblock.tall_column", "screen.earth_online.machine.structure.tall_column") {
            @Override
            boolean isComplete(Level level, BlockPos controller) {
                for (Direction front : HORIZONTALS) {
                    Direction left = front.getCounterClockWise();
                    Direction right = front.getClockWise();
                    Direction back = front.getOpposite();
                    if (has(level, controller.relative(front), EarthOnline.CONTROL_PANEL.get())
                            && has(level, controller.relative(left), EarthOnline.INDUSTRIAL_MACHINE_CASING.get())
                            && has(level, controller.relative(right), EarthOnline.INDUSTRIAL_MACHINE_CASING.get())
                            && has(level, controller.relative(back), EarthOnline.STEEL_PROCESS_PIPE.get())
                            && has(level, controller.above(), EarthOnline.STEEL_PROCESS_PIPE.get())
                            && has(level, controller.above(2), EarthOnline.STEEL_PROCESS_PIPE.get())) {
                        return true;
                    }
                }
                return false;
            }
        };

        private final String descriptionKey;
        private final String screenKey;

        Pattern(String descriptionKey, String screenKey) {
            this.descriptionKey = descriptionKey;
            this.screenKey = screenKey;
        }

        abstract boolean isComplete(Level level, BlockPos controller);

        public String descriptionKey() {
            return descriptionKey;
        }

        public String screenKey() {
            return screenKey;
        }
    }

    private static boolean has(Level level, BlockPos pos, Block block) {
        return level.getBlockState(pos).getBlock() == block;
    }
}

package com.xxsx.earthonminecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MachineMultiblock {
    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private MachineMultiblock() {
    }

    public static Pattern patternFor(ProcessingMachineBlock.Kind kind) {
        return switch (kind) {
            case GAS_SEPARATOR -> Pattern.PRESSURE_SKID;
            case FLOTATION_CELL, LEACHING_TANK -> Pattern.WET_VESSEL;
            case ELECTROLYTIC_CELL -> Pattern.ELECTROLYSIS_BANK;
            case CHEMICAL_REACTOR, POLYMERIZER -> Pattern.REACTION_VESSEL;
            case ROASTER, REDUCTION_FURNACE, INDUSTRIAL_KILN -> Pattern.HEATED_LINE;
            case STEAM_CRACKER -> Pattern.CRACKING_FURNACE;
            case DISTILLATION_COLUMN, ABSORPTION_TOWER -> Pattern.TALL_COLUMN;
            case SYNTHESIS_LOOP -> Pattern.PRESSURE_LOOP;
            default -> Pattern.NONE;
        };
    }

    public static boolean isComplete(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind) {
        return findMatch(level, controller, kind).isPresent();
    }

    public static Optional<Match> findMatch(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind) {
        return patternFor(kind).findMatch(level, controller);
    }

    public static Optional<BlockPos> findControllerForPart(Level level, BlockPos part) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    cursor.set(part.getX() + dx, part.getY() + dy, part.getZ() + dz);
                    if (level.getBlockEntity(cursor) instanceof ProcessingMachineBlockEntity machine) {
                        Optional<Match> match = findMatch(level, cursor, machine.kind());
                        if (match.isPresent() && match.get().parts().contains(part)) {
                            return Optional.of(cursor.immutable());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> findControllerForPanel(Level level, BlockPos panel) {
        return findControllerForPart(level, panel);
    }

    public static void refreshProjectionForPanel(Level level, BlockPos panel) {
        if (level.isClientSide()) {
            return;
        }
        for (Direction front : HORIZONTALS) {
            BlockPos controller = panel.relative(front.getOpposite());
            if (level.getBlockEntity(controller) instanceof ProcessingMachineBlockEntity machine) {
                refreshProjection(level, controller, machine.kind());
            }
        }
    }

    public static void refreshProjection(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind) {
        if (level.isClientSide()) {
            return;
        }
        Pattern pattern = patternFor(kind);
        if (pattern == Pattern.NONE || isComplete(level, controller, kind)) {
            clearProjectionForPattern(level, controller, pattern);
            return;
        }
        Optional<Direction> panelFront = findPanelFront(level, controller);
        if (panelFront.isEmpty()) {
            clearProjectionForPattern(level, controller, pattern);
            return;
        }
        Direction front = panelFront.get();
        clearStaleProjections(level, controller, pattern, front);
        for (Requirement requirement : requirementsFor(pattern, front)) {
            if (requirement.role() == PartRole.CONTROL_PANEL) {
                continue;
            }
            BlockPos pos = requirement.resolve(controller);
            if (!hasRole(level, pos, requirement.role())) {
                placeProjection(level, pos, requirement.role());
            }
        }
    }

    public static void clearProjectionForController(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind) {
        if (!level.isClientSide()) {
            clearProjectionForPattern(level, controller, patternFor(kind));
        }
    }

    public static void syncAssembly(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind, boolean assembled) {
        Pattern pattern = patternFor(kind);
        clearAssemblyForPattern(level, controller, pattern);
        if (!assembled) {
            return;
        }
        StyledSupportPartBlock.AssemblyStyle style = assemblyStyleFor(pattern);
        findMatch(level, controller, kind).ifPresent(match -> {
            for (BlockPos part : match.parts()) {
                setAssembled(level, part, true, style);
            }
        });
    }

    public static void syncPanelActive(Level level, BlockPos controller, ProcessingMachineBlock.Kind kind, boolean active) {
        if (level.isClientSide()) {
            return;
        }
        findMatch(level, controller, kind).ifPresent(match -> {
            for (BlockPos part : match.parts()) {
                BlockState state = level.getBlockState(part);
                if (state.getBlock() != EarthOnMinecraft.CONTROL_PANEL.get()
                        || !state.hasProperty(ControlPanelBlock.ACTIVE)) {
                    continue;
                }
                boolean nextActive = active && state.getValue(SupportPartBlock.ASSEMBLED);
                if (state.getValue(ControlPanelBlock.ACTIVE) != nextActive) {
                    level.setBlock(part, state.setValue(ControlPanelBlock.ACTIVE, nextActive), Block.UPDATE_CLIENTS);
                }
            }
        });
    }

    private static void clearAssemblyForPattern(Level level, BlockPos controller, Pattern pattern) {
        if (pattern == Pattern.NONE) {
            return;
        }
        for (Direction front : HORIZONTALS) {
            for (Requirement requirement : requirementsFor(pattern, front)) {
                setAssembled(level, requirement.resolve(controller), false,
                        StyledSupportPartBlock.AssemblyStyle.NONE);
            }
        }
    }

    private static void setAssembled(Level level, BlockPos pos, boolean assembled,
                                     StyledSupportPartBlock.AssemblyStyle style) {
        var state = level.getBlockState(pos);
        if (!state.hasProperty(SupportPartBlock.ASSEMBLED)) {
            return;
        }
        BlockState next = state.setValue(SupportPartBlock.ASSEMBLED, assembled);
        if (next.hasProperty(ControlPanelBlock.ACTIVE) && !assembled) {
            next = next.setValue(ControlPanelBlock.ACTIVE, false);
        }
        if (next.hasProperty(StyledSupportPartBlock.ASSEMBLY_STYLE)) {
            next = next.setValue(StyledSupportPartBlock.ASSEMBLY_STYLE,
                    assembled ? style : StyledSupportPartBlock.AssemblyStyle.NONE);
        }
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    private static StyledSupportPartBlock.AssemblyStyle assemblyStyleFor(Pattern pattern) {
        return switch (pattern) {
            case NONE -> StyledSupportPartBlock.AssemblyStyle.NONE;
            case PRESSURE_SKID -> StyledSupportPartBlock.AssemblyStyle.HEAVY;
            case WET_VESSEL, ELECTROLYSIS_BANK, REACTION_VESSEL -> StyledSupportPartBlock.AssemblyStyle.WET;
            case HEATED_LINE, CRACKING_FURNACE -> StyledSupportPartBlock.AssemblyStyle.HEATED;
            case TALL_COLUMN, PRESSURE_LOOP -> StyledSupportPartBlock.AssemblyStyle.TOWER;
        };
    }

    private static Optional<Direction> findPanelFront(Level level, BlockPos controller) {
        for (Direction front : HORIZONTALS) {
            if (hasRole(level, controller.relative(front), PartRole.CONTROL_PANEL)) {
                return Optional.of(front);
            }
        }
        return Optional.empty();
    }

    private static void clearProjectionForPattern(Level level, BlockPos controller, Pattern pattern) {
        if (pattern == Pattern.NONE) {
            return;
        }
        for (Direction front : HORIZONTALS) {
            for (Requirement requirement : requirementsFor(pattern, front)) {
                if (requirement.role() == PartRole.CONTROL_PANEL) {
                    continue;
                }
                BlockPos pos = requirement.resolve(controller);
                if (level.getBlockState(pos).getBlock() == EarthOnMinecraft.STRUCTURE_PROJECTION.get()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void clearStaleProjections(Level level, BlockPos controller, Pattern pattern, Direction currentFront) {
        Map<BlockPos, StructureProjectionBlock.Role> keep = new HashMap<>();
        for (Requirement requirement : requirementsFor(pattern, currentFront)) {
            if (requirement.role() != PartRole.CONTROL_PANEL) {
                keep.put(requirement.resolve(controller), projectionRoleFor(requirement.role()));
            }
        }
        for (Direction front : HORIZONTALS) {
            for (Requirement requirement : requirementsFor(pattern, front)) {
                if (requirement.role() == PartRole.CONTROL_PANEL) {
                    continue;
                }
                BlockPos pos = requirement.resolve(controller);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() != EarthOnMinecraft.STRUCTURE_PROJECTION.get()) {
                    continue;
                }
                StructureProjectionBlock.Role expected = keep.get(pos);
                if (expected == null || state.getValue(StructureProjectionBlock.ROLE) != expected) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void placeProjection(Level level, BlockPos pos, PartRole role) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && state.getBlock() != EarthOnMinecraft.STRUCTURE_PROJECTION.get()) {
            return;
        }
        StructureProjectionBlock.Role projectionRole = projectionRoleFor(role);
        BlockState projection = EarthOnMinecraft.STRUCTURE_PROJECTION.get().defaultBlockState()
                .setValue(StructureProjectionBlock.ROLE, projectionRole);
        if (state.getBlock() != EarthOnMinecraft.STRUCTURE_PROJECTION.get()
                || state.getValue(StructureProjectionBlock.ROLE) != projectionRole) {
            level.setBlock(pos, projection, Block.UPDATE_CLIENTS);
        }
    }

    private static StructureProjectionBlock.Role projectionRoleFor(PartRole role) {
        return role == PartRole.PIPE ? StructureProjectionBlock.Role.PIPE : StructureProjectionBlock.Role.CASING;
    }

    public enum Pattern {
        NONE("tooltip.earth_on_minecraft.multiblock.none", "screen.earth_on_minecraft.machine.structure.none") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return Optional.of(new Match(Direction.NORTH, List.of()));
            }
        },
        PRESSURE_SKID("tooltip.earth_on_minecraft.multiblock.pressure_skid", "screen.earth_on_minecraft.machine.structure.pressure_skid") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return findBox(level, controller, 1, 2, 2, PartRole.CASING, PartRole.CASING);
            }
        },
        WET_VESSEL("tooltip.earth_on_minecraft.multiblock.wet_vessel", "screen.earth_on_minecraft.machine.structure.wet_vessel") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return findBox(level, controller, 1, 2, 2, PartRole.CASING, PartRole.PIPE);
            }
        },
        ELECTROLYSIS_BANK("tooltip.earth_on_minecraft.multiblock.electrolysis_bank", "screen.earth_on_minecraft.machine.structure.electrolysis_bank") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return findBox(level, controller, 1, 3, 2, PartRole.CASING, PartRole.PIPE);
            }
        },
        REACTION_VESSEL("tooltip.earth_on_minecraft.multiblock.reaction_vessel", "screen.earth_on_minecraft.machine.structure.reaction_vessel") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return findBox(level, controller, 1, 3, 3, PartRole.CASING, PartRole.PIPE);
            }
        },
        HEATED_LINE("tooltip.earth_on_minecraft.multiblock.heated_line", "screen.earth_on_minecraft.machine.structure.heated_line") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return findBox(level, controller, 1, 3, 2, PartRole.CASING, PartRole.PIPE);
            }
        },
        CRACKING_FURNACE("tooltip.earth_on_minecraft.multiblock.cracking_furnace", "screen.earth_on_minecraft.machine.structure.cracking_furnace") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return findBox(level, controller, 1, 3, 3, PartRole.CASING, PartRole.PIPE);
            }
        },
        TALL_COLUMN("tooltip.earth_on_minecraft.multiblock.tall_column", "screen.earth_on_minecraft.machine.structure.tall_column") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return findBox(level, controller, 1, 3, 4, PartRole.CASING, PartRole.PIPE);
            }
        },
        PRESSURE_LOOP("tooltip.earth_on_minecraft.multiblock.pressure_loop", "screen.earth_on_minecraft.machine.structure.pressure_loop") {
            @Override
            Optional<Match> findMatch(Level level, BlockPos controller) {
                return findBox(level, controller, 1, 3, 3, PartRole.CASING, PartRole.PIPE);
            }
        };

        private final String descriptionKey;
        private final String screenKey;

        Pattern(String descriptionKey, String screenKey) {
            this.descriptionKey = descriptionKey;
            this.screenKey = screenKey;
        }

        abstract Optional<Match> findMatch(Level level, BlockPos controller);

        public String descriptionKey() {
            return descriptionKey;
        }

        public String screenKey() {
            return screenKey;
        }
    }

    private static List<Requirement> requirementsFor(Pattern pattern, Direction front) {
        return switch (pattern) {
            case NONE -> List.of();
            case PRESSURE_SKID -> buildBoxRequirements(front, 1, 2, 2, PartRole.CASING, PartRole.CASING);
            case WET_VESSEL -> buildBoxRequirements(front, 1, 2, 2, PartRole.CASING, PartRole.PIPE);
            case ELECTROLYSIS_BANK -> buildBoxRequirements(front, 1, 3, 2, PartRole.CASING, PartRole.PIPE);
            case REACTION_VESSEL -> buildBoxRequirements(front, 1, 3, 3, PartRole.CASING, PartRole.PIPE);
            case HEATED_LINE -> buildBoxRequirements(front, 1, 3, 2, PartRole.CASING, PartRole.PIPE);
            case CRACKING_FURNACE -> buildBoxRequirements(front, 1, 3, 3, PartRole.CASING, PartRole.PIPE);
            case TALL_COLUMN -> buildBoxRequirements(front, 1, 3, 4, PartRole.CASING, PartRole.PIPE);
            case PRESSURE_LOOP -> buildBoxRequirements(front, 1, 3, 3, PartRole.CASING, PartRole.PIPE);
        };
    }

    private static Optional<Match> findBox(Level level, BlockPos controller, int halfWidth, int depth, int height,
                                           PartRole lowerRole, PartRole upperRole) {
        for (Direction front : HORIZONTALS) {
            List<Requirement> requirements = buildBoxRequirements(front, halfWidth, depth, height, lowerRole, upperRole);
            Optional<Match> match = matchFootprint(level, controller, front, requirements);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private static List<Requirement> buildBoxRequirements(Direction front, int halfWidth, int depth, int height,
                                                          PartRole lowerRole, PartRole upperRole) {
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(Requirement.relative(front, 0, -1, 0, PartRole.CONTROL_PANEL));
        for (int right = -halfWidth; right <= halfWidth; right++) {
            for (int back = -1; back < depth - 1; back++) {
                for (int up = 0; up < height; up++) {
                    if (right == 0 && up == 0 && (back == -1 || back == 0)) {
                        continue;
                    }
                    PartRole role = up == 0 ? lowerRole : upperRole;
                    requirements.add(Requirement.relative(front, right, back, up, role));
                }
            }
        }
        return requirements;
    }

    private static Optional<Match> matchFootprint(Level level, BlockPos controller, Direction front, List<Requirement> requirements) {
        List<BlockPos> parts = new ArrayList<>();
        for (Requirement requirement : requirements) {
            BlockPos pos = requirement.resolve(controller);
            if (!hasRole(level, pos, requirement.role())) {
                return Optional.empty();
            }
            parts.add(pos);
        }
        return Optional.of(new Match(front, List.copyOf(parts)));
    }

    private static boolean hasRole(Level level, BlockPos pos, PartRole role) {
        Block block = level.getBlockState(pos).getBlock();
        return switch (role) {
            case CONTROL_PANEL -> block == EarthOnMinecraft.CONTROL_PANEL.get();
            case CASING -> block == EarthOnMinecraft.INDUSTRIAL_MACHINE_CASING.get()
                    || block == EarthOnMinecraft.MACHINE_INPUT_INTERFACE.get()
                    || block == EarthOnMinecraft.MACHINE_OUTPUT_INTERFACE.get();
            case PIPE -> block == EarthOnMinecraft.STEEL_PROCESS_PIPE.get();
        };
    }

    public record Match(Direction front, List<BlockPos> parts) {
    }

    private enum PartRole {
        CONTROL_PANEL,
        CASING,
        PIPE
    }

    private record Requirement(int x, int z, int up, PartRole role) {
        static Requirement relative(Direction front, int right, int back, int up, PartRole role) {
            Direction rightDir = front.getClockWise();
            Direction backDir = front.getOpposite();
            int x = rightDir.getStepX() * right + backDir.getStepX() * back;
            int z = rightDir.getStepZ() * right + backDir.getStepZ() * back;
            return new Requirement(x, z, up, role);
        }

        BlockPos resolve(BlockPos controller) {
            return controller.offset(x, up, z);
        }
    }
}

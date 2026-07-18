package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.ProcessingMachineBlock;
import com.xxsx.earthonminecraft.ProcessingMachineBlockEntity;
import com.xxsx.earthonminecraft.ProcessingMachineMenu;
import com.xxsx.earthonminecraft.MachineMultiblock;
import com.xxsx.earthonminecraft.RouteGuide;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ProcessingMachineScreen extends AbstractContainerScreen<ProcessingMachineMenu> {
    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("earth_on_minecraft", "textures/gui/container/processing_machine.png");
    private static final Identifier PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier FUEL_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int VANILLA_TEXT = 0xFF404040;
    private static final int MUTED = 0xFF606060;
    private static final int WARNING = 0xFFAA3322;
    private static final int STATUS_X = 92;
    private static final int STATUS_Y = 66;
    private static final int STATUS_SIZE = 5;
    private static final int SIDE_BUTTON_SIZE = 10;

    private Button redstoneButton;
    private Button routeButton;
    private final Map<Direction, Button> sideButtons = new EnumMap<>(Direction.class);

    public ProcessingMachineScreen(ProcessingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 57 + Math.max(0, (74 - this.font.width(trimmedTitle())) / 2);
        redstoneButton = addRenderableWidget(Button.builder(redstoneButtonLabel(), b -> cycleRedstoneMode())
                .bounds(this.leftPos + 7, this.topPos + 65, 18, 14)
                .build());
        routeButton = addRenderableWidget(Button.builder(routeButtonLabel(), b -> cycleRoute())
                .bounds(this.leftPos + 58, this.topPos + 64, 30, 14)
                .build());
        sideButtons.clear();
        if (!isMultiblockMachine()) {
            for (Direction side : Direction.values()) {
                Button button = addRenderableWidget(Button.builder(Component.empty(), b -> cycleSideMode(side))
                        .bounds(sideButtonX(side), sideButtonY(side), SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE)
                        .build());
                sideButtons.put(side, button);
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.earth_on_minecraft.button.notebook"), button -> {
                    this.onClose();
                    EarthOnMinecraftClient.openNotebook();
                })
                .bounds(this.leftPos + this.imageWidth - 42, this.topPos + 4, 34, 14)
                .build());
        syncRedstoneButtons();
        syncRouteButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncRedstoneButtons();
        syncRouteButton();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        g.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        drawMachineChrome(g);
        drawFuel(g);
        drawProgress(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawSideConfig(g);
        drawRedstoneIcon(g);
        drawStatusDot(g);
        drawTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, trimmedTitle(), this.titleLabelX, this.titleLabelY, VANILLA_TEXT, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, VANILLA_TEXT, false);
    }

    private void drawFuel(GuiGraphicsExtractor g) {
        if (!this.menu.acceptsLocalFuel() || this.menu.burnTimeTotal() <= 0) {
            return;
        }
        int lit = Math.min(14, 14 * this.menu.burnTime() / this.menu.burnTimeTotal());
        if (lit > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_SPRITE, 14, 14, 0, 14 - lit,
                    this.leftPos + 59, this.topPos + 59 + 14 - lit, 14, lit);
        }
    }

    private void drawProgress(GuiGraphicsExtractor g) {
        int maxProgress = this.menu.maxProgress();
        if (maxProgress <= 0) {
            return;
        }
        int progress = Math.min(24, 24 * this.menu.progress() / maxProgress);
        if (progress > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_SPRITE, 24, 16, 0, 0, this.leftPos + 64, this.topPos + 38, progress, 16);
        }
    }

    private String statusLine() {
        if (!this.menu.structureValid()) {
            MachineMultiblock.Pattern pattern = MachineMultiblock.patternFor(this.menu.kind());
            return fit(localized("screen.earth_on_minecraft.machine.structure_missing") + " " + localized(pattern.screenKey()), 132);
        }

        ItemStack input = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (input.isEmpty()) {
            String key = this.menu.acceptsLocalFuel()
                    ? "screen.earth_on_minecraft.machine.empty_input_fuel"
                    : "screen.earth_on_minecraft.machine.empty_input_grid";
            return fit(localized(key), 92);
        }

        return this.menu.selectedRecipe().map(recipe -> {
            if (this.menu.gridPowered()) {
                return fit(localized("screen.earth_on_minecraft.machine.grid_powered") + " " + recipeSummary(recipe), 92);
            }
            if (!this.menu.hasBurningFuel() && !fuelSlotHasFuel()) {
                String key = this.menu.acceptsLocalFuel()
                        ? "screen.earth_on_minecraft.machine.missing_power"
                        : "screen.earth_on_minecraft.machine.missing_grid_power";
                return fit(localized(key), 92);
            }
            String note = recipeSummary(recipe);
            return fit(note, 92);
        }).orElseGet(() -> fit(localized("screen.earth_on_minecraft.machine.unsupported_input"), 92));
    }

    private int statusColor() {
        if (!this.menu.structureValid()) {
            return WARNING;
        }
        ItemStack input = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (!input.isEmpty() && ProcessingMachineBlock.findRecipe(this.menu.kind(), input).isEmpty()) {
            return WARNING;
        }
        if (!input.isEmpty() && !this.menu.gridPowered() && !this.menu.hasBurningFuel() && !fuelSlotHasFuel()) {
            return WARNING;
        }
        return this.menu.active() ? 0xFF207030 : MUTED;
    }

    private void drawStatusDot(GuiGraphicsExtractor g) {
        int x = this.leftPos + STATUS_X;
        int y = this.topPos + STATUS_Y;
        int color = statusColor();
        g.fill(x - 1, y - 1, x + STATUS_SIZE + 1, y + STATUS_SIZE + 1, 0xFF3B3B3B);
        g.fill(x, y, x + STATUS_SIZE, y + STATUS_SIZE, color);
        g.text(this.font, Component.literal(visibleStatusLine()), x + 8, y - 2, color, false);
    }

    private void drawMachineChrome(GuiGraphicsExtractor g) {
        int accent = this.menu.processFamily().accentColor();
        int x = this.leftPos + 58;
        int y = this.topPos + 20;
        g.fill(x - 2, y - 2, x + 41, y + 15, 0x66303030);
        g.fill(x - 2, y - 2, x + 41, y, accent);
        Item machineItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(
                "earth_on_minecraft", this.menu.kind().blockId()));
        ItemStack machineIcon = new ItemStack(machineItem);
        if (!machineIcon.isEmpty()) {
            g.item(machineIcon, x, y - 1);
        } else {
            drawProcessGlyph(g, x + 1, y + 1, accent);
        }
        String family = fit(localized(this.menu.processFamily().labelKey()), 23);
        g.text(this.font, family, x + 17, y + 2, accent, false);

        if (!this.menu.acceptsLocalFuel()) {
            int slotX = this.leftPos + 39;
            int slotY = this.topPos + 58;
            g.fill(slotX, slotY, slotX + 16, slotY + 16, 0xDD20282D);
            drawPowerGlyph(g, slotX + 4, slotY + 2, accent);
        }
    }

    private void drawProcessGlyph(GuiGraphicsExtractor g, int x, int y, int color) {
        switch (this.menu.processFamily()) {
            case COMMINUTION -> {
                g.fill(x, y + 1, x + 4, y + 4, color);
                g.fill(x + 8, y + 1, x + 12, y + 4, color);
                g.fill(x + 3, y + 4, x + 9, y + 7, color);
                g.fill(x + 5, y + 7, x + 7, y + 11, color);
            }
            case CLASSIFICATION -> {
                g.fill(x, y + 2, x + 12, y + 3, color);
                g.fill(x + 2, y + 5, x + 10, y + 6, color);
                g.fill(x + 4, y + 8, x + 8, y + 9, color);
            }
            case WET_PROCESS -> {
                g.fill(x, y + 1, x + 12, y + 2, color);
                g.fill(x, y + 2, x + 2, y + 11, color);
                g.fill(x + 10, y + 2, x + 12, y + 11, color);
                g.fill(x + 2, y + 7, x + 10, y + 11, color);
            }
            case THERMAL -> {
                g.fill(x + 5, y, x + 8, y + 4, color);
                g.fill(x + 3, y + 3, x + 9, y + 8, color);
                g.fill(x + 1, y + 7, x + 11, y + 11, color);
            }
            case ELECTROCHEMICAL -> {
                g.fill(x + 6, y, x + 9, y + 4, color);
                g.fill(x + 3, y + 3, x + 8, y + 7, color);
                g.fill(x + 5, y + 6, x + 8, y + 9, color);
                g.fill(x + 2, y + 8, x + 7, y + 11, color);
            }
            case FORMING -> {
                g.fill(x + 1, y + 1, x + 11, y + 3, color);
                g.fill(x + 5, y + 3, x + 7, y + 8, color);
                g.fill(x + 2, y + 8, x + 10, y + 11, color);
            }
            case REACTION -> {
                g.fill(x + 2, y + 1, x + 10, y + 3, color);
                g.fill(x + 1, y + 3, x + 11, y + 10, color);
                g.fill(x + 4, y + 5, x + 6, y + 7, 0xFFE5F4F3);
                g.fill(x + 7, y + 3, x + 9, y + 5, 0xFFE5F4F3);
            }
            case COLUMN -> {
                g.fill(x + 3, y, x + 9, y + 11, color);
                g.fill(x + 1, y + 2, x + 11, y + 3, color);
                g.fill(x + 1, y + 7, x + 11, y + 8, color);
            }
            case MIXING -> {
                g.fill(x + 5, y, x + 7, y + 7, color);
                g.fill(x + 1, y + 6, x + 11, y + 8, color);
                g.fill(x + 3, y + 8, x + 5, y + 11, color);
                g.fill(x + 7, y + 8, x + 9, y + 11, color);
            }
            case CRYSTALLIZATION -> {
                g.fill(x + 5, y, x + 7, y + 2, color);
                g.fill(x + 3, y + 2, x + 9, y + 5, color);
                g.fill(x + 1, y + 5, x + 11, y + 7, color);
                g.fill(x + 3, y + 7, x + 9, y + 9, color);
                g.fill(x + 5, y + 9, x + 7, y + 11, color);
            }
        }
    }

    private void drawPowerGlyph(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x + 5, y, x + 9, y + 5, color);
        g.fill(x + 2, y + 4, x + 8, y + 8, color);
        g.fill(x + 4, y + 7, x + 7, y + 12, color);
    }

    private void drawRedstoneIcon(GuiGraphicsExtractor g) {
        int x = this.leftPos + 8;
        int y = this.topPos + 64;
        ProcessingMachineBlockEntity.RedstoneMode redstone = this.menu.redstoneMode();
        g.item(new ItemStack(redstoneIcon(redstone)), x, y);
        if (redstone == ProcessingMachineBlockEntity.RedstoneMode.REQUIRE_NO_SIGNAL) {
            g.fill(x, y, x + 16, y + 16, 0x99000000);
        }
    }

    private void drawSideConfig(GuiGraphicsExtractor g) {
        if (isMultiblockMachine()) {
            drawInterfaceGuide(g);
            return;
        }
        g.text(this.font, Component.translatable("screen.earth_on_minecraft.side.panel"), this.leftPos + 5, this.topPos + 6, MUTED, false);
        for (Direction side : Direction.values()) {
            int x = sideButtonX(side);
            int y = sideButtonY(side);
            int color = sideModeColor(this.menu.sideMode(side));
            g.fill(x - 1, y - 1, x + SIDE_BUTTON_SIZE + 1, y + SIDE_BUTTON_SIZE + 1, 0xFF383838);
            g.fill(x, y, x + SIDE_BUTTON_SIZE, y + SIDE_BUTTON_SIZE, color);
            g.text(this.font, faceShort(side), x + 2, y + 1, 0xFFFFFFFF, false);
        }
    }

    private void drawInterfaceGuide(GuiGraphicsExtractor g) {
        int x = this.leftPos + 6;
        int y = this.topPos + 18;
        g.text(this.font, Component.translatable("screen.earth_on_minecraft.machine.interface_panel"), x, y, MUTED, false);
        g.text(this.font, Component.translatable("screen.earth_on_minecraft.machine.interface_input"), x, y + 11, 0xFF2D74C4, false);
        g.text(this.font, Component.translatable("screen.earth_on_minecraft.machine.interface_output"), x, y + 22, 0xFFC46A22, false);
        g.text(this.font, Component.translatable("screen.earth_on_minecraft.machine.interface_conveyor"), x, y + 33, 0xFF2E8B57, false);
    }

    private Item redstoneIcon(ProcessingMachineBlockEntity.RedstoneMode redstone) {
        return switch (redstone) {
            case ALWAYS -> Items.BARRIER;
            case REQUIRE_SIGNAL, REQUIRE_NO_SIGNAL -> Items.REDSTONE_TORCH;
        };
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(7, 65, 18, 14, mouseX, mouseY)) {
            ProcessingMachineBlockEntity.RedstoneMode redstone = this.menu.redstoneMode();
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_on_minecraft.redstone.current", Component.translatable(redstone.labelKey())),
                    Component.translatable(redstone.descriptionKey()),
                    Component.translatable("screen.earth_on_minecraft.redstone.tooltip")
            ), mouseX, mouseY);
        }
        if (isMultiblockMachine()) {
            if (isHovering(5, 16, 42, 45, mouseX, mouseY)) {
                g.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.translatable("screen.earth_on_minecraft.machine.interface.tooltip.1"),
                        Component.translatable("screen.earth_on_minecraft.machine.interface.tooltip.2"),
                        Component.translatable("screen.earth_on_minecraft.machine.interface.tooltip.3")
                ), mouseX, mouseY);
            }
        } else {
            for (Direction side : Direction.values()) {
                if (isHovering(sideButtonX(side) - this.leftPos, sideButtonY(side) - this.topPos,
                        SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE, mouseX, mouseY)) {
                    ProcessingMachineBlockEntity.SideMode mode = this.menu.sideMode(side);
                    g.setComponentTooltipForNextFrame(this.font, List.of(
                            Component.translatable("screen.earth_on_minecraft.side.current",
                                    Component.translatable(faceKey(side)), Component.translatable(mode.labelKey())),
                            Component.translatable(mode.tooltipKey()),
                            Component.translatable("screen.earth_on_minecraft.side.tooltip")
                    ), mouseX, mouseY);
                }
            }
        }
        if (!this.menu.structureValid() && isHovering(34, 55, 134, 20, mouseX, mouseY)) {
            MachineMultiblock.Pattern pattern = MachineMultiblock.patternFor(this.menu.kind());
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_on_minecraft.machine.structure_missing"),
                    Component.translatable(pattern.screenKey()),
                    Component.translatable("tooltip.earth_on_minecraft.multiblock.legend")
            ), mouseX, mouseY);
        }
        if (isHovering(38, 57, 18, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, powerTooltip(), mouseX, mouseY);
        }
        if (isHovering(56, 18, 44, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, machineSpecTooltip(), mouseX, mouseY);
        }
        if (isHovering(STATUS_X - 2, STATUS_Y - 2, STATUS_SIZE + 4, STATUS_SIZE + 4, mouseX, mouseY)
                || isHovering(64, 38, 36, 18, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(stateTooltip());
            tooltip.add(Component.literal(statusLine()));
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.spec",
                    secondsPerOperation(), this.menu.energyPerTick()));
            tooltip.add(Component.translatable(this.menu.powerMode().labelKey()));
            g.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
        if (this.menu.routeCount() > 1 && isHovering(58, 64, 30, 14, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_on_minecraft.machine.route.current", this.menu.selectedRouteIndex() + 1, this.menu.routeCount()),
                    this.menu.selectedRecipe().map(recipe -> Component.literal(recipe.note()))
                            .orElse(Component.translatable("screen.earth_on_minecraft.machine.empty_input")),
                    Component.translatable("screen.earth_on_minecraft.machine.route.tooltip")
            ), mouseX, mouseY);
        }
    }

    private void cycleRedstoneMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) {
            return;
        }
        int id = switch (this.menu.redstoneMode()) {
            case ALWAYS -> ProcessingMachineMenu.BUTTON_REDSTONE_REQUIRE_SIGNAL;
            case REQUIRE_SIGNAL -> ProcessingMachineMenu.BUTTON_REDSTONE_REQUIRE_NO_SIGNAL;
            case REQUIRE_NO_SIGNAL -> ProcessingMachineMenu.BUTTON_REDSTONE_ALWAYS;
        };
        mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private void cycleSideMode(Direction side) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) {
            return;
        }
        mc.gameMode.handleInventoryButtonClick(this.menu.containerId, ProcessingMachineMenu.BUTTON_SIDE_BASE + side.ordinal());
    }

    private void cycleRoute() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || this.menu.routeCount() <= 1) {
            return;
        }
        mc.gameMode.handleInventoryButtonClick(this.menu.containerId, ProcessingMachineMenu.BUTTON_ROUTE_NEXT);
    }

    private void syncRedstoneButtons() {
        if (redstoneButton != null) {
            redstoneButton.setMessage(redstoneButtonLabel());
        }
    }

    private void syncRouteButton() {
        if (routeButton != null) {
            routeButton.visible = this.menu.routeCount() > 1;
            routeButton.active = this.menu.routeCount() > 1;
            routeButton.setMessage(routeButtonLabel());
        }
    }

    private Component redstoneButtonLabel() {
        return Component.empty();
    }

    private Component routeButtonLabel() {
        if (this.menu.routeCount() <= 1) {
            return Component.empty();
        }
        return Component.literal((this.menu.selectedRouteIndex() + 1) + "/" + this.menu.routeCount());
    }

    private int sideButtonX(Direction side) {
        return this.leftPos + switch (relativeFace(side)) {
            case LEFT -> 7;
            case RIGHT -> 29;
            default -> 18;
        };
    }

    private int sideButtonY(Direction side) {
        return this.topPos + switch (relativeFace(side)) {
            case TOP -> 17;
            case LEFT, FRONT, RIGHT -> 28;
            case BOTTOM -> 39;
            case BACK -> 50;
        };
    }

    private int sideModeColor(ProcessingMachineBlockEntity.SideMode mode) {
        return switch (mode) {
            case INPUT -> 0xFF2D74C4;
            case OUTPUT -> 0xFFC46A22;
            case BOTH -> 0xFF2E8B57;
            case OFF -> 0xFF555555;
        };
    }

    private Component faceShort(Direction side) {
        return Component.translatable("screen.earth_on_minecraft.side.relative.short." + relativeFace(side).key);
    }

    private String faceKey(Direction side) {
        return "screen.earth_on_minecraft.side.relative." + relativeFace(side).key;
    }

    private Component stateLabel() {
        return Component.translatable(this.menu.active()
                ? "screen.earth_on_minecraft.machine.running"
                : "screen.earth_on_minecraft.machine.idle");
    }

    private Component stateTooltip() {
        return Component.translatable(this.menu.active()
                ? "screen.earth_on_minecraft.machine.state.running.tooltip"
                : "screen.earth_on_minecraft.machine.state.idle.tooltip");
    }

    private String visibleStatusLine() {
        return fit(statusLine(), 68);
    }

    private Component trimmedTitle() {
        String text = this.title.getString();
        if (this.font.width(text) > 72) {
            return Component.literal(this.font.plainSubstrByWidth(text, 69) + "...");
        }
        return this.title;
    }

    private String recipeSummary(ProcessingMachineBlock.Recipe recipe) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(java.util.Locale.ROOT).startsWith("zh")) {
            return recipe.note();
        }
        return localized("screen.earth_on_minecraft.machine.recipe_ready") + ": " + RouteGuide.describeOutputs(recipe);
    }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("..."))) + "...";
    }

    private static String localized(String key) {
        return Language.getInstance().getOrDefault(key);
    }

    private boolean fuelSlotHasFuel() {
        if (!this.menu.acceptsLocalFuel()) {
            return false;
        }
        ItemStack fuel = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_FUEL).getItem();
        return ProcessingMachineBlockEntity.getFuelTicks(fuel) > 0;
    }

    private List<Component> powerTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(this.menu.powerMode().labelKey()));
        tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.spec",
                secondsPerOperation(), this.menu.energyPerTick()));
        if (this.menu.acceptsLocalFuel()) {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.fuel.tooltip"));
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.fuel.examples"));
        } else {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.grid_only.tooltip"));
        }
        return tooltip;
    }

    private List<Component> machineSpecTooltip() {
        return List.of(
                Component.translatable(this.menu.processFamily().labelKey()),
                Component.translatable(this.menu.kind().setpointKey()),
                Component.translatable("screen.earth_on_minecraft.machine.spec",
                        secondsPerOperation(), this.menu.energyPerTick()),
                Component.translatable(this.menu.powerMode().labelKey())
        );
    }

    private int secondsPerOperation() {
        return Math.max(1, (this.menu.maxProgress() + 19) / 20);
    }

    private Direction machineFront() {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var state = level.getBlockState(this.menu.pos());
            if (state.hasProperty(ProcessingMachineBlock.FACING)) {
                return state.getValue(ProcessingMachineBlock.FACING);
            }
        }
        return Direction.NORTH;
    }

    private RelativeFace relativeFace(Direction side) {
        if (side == Direction.UP) {
            return RelativeFace.TOP;
        }
        if (side == Direction.DOWN) {
            return RelativeFace.BOTTOM;
        }
        Direction front = machineFront();
        if (side == front) {
            return RelativeFace.FRONT;
        }
        if (side == front.getOpposite()) {
            return RelativeFace.BACK;
        }
        if (side == front.getClockWise()) {
            return RelativeFace.RIGHT;
        }
        return RelativeFace.LEFT;
    }

    private boolean isMultiblockMachine() {
        return MachineMultiblock.patternFor(this.menu.kind()) != MachineMultiblock.Pattern.NONE;
    }

    private enum RelativeFace {
        TOP("top"),
        BOTTOM("bottom"),
        FRONT("front"),
        BACK("back"),
        LEFT("left"),
        RIGHT("right");

        private final String key;

        RelativeFace(String key) {
            this.key = key;
        }
    }
}

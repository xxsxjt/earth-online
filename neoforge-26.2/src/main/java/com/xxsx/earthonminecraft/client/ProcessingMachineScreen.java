package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.ProcessingMachineBlock;
import com.xxsx.earthonminecraft.ProcessingMachineBlockEntity;
import com.xxsx.earthonminecraft.ProcessingMachineMenu;
import com.xxsx.earthonminecraft.MachineMultiblock;
import com.xxsx.earthonminecraft.RouteGuide;
import net.minecraft.core.Direction;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
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
        this.titleLabelX = Math.max(8, (this.imageWidth - this.font.width(trimmedTitle())) / 2);
        redstoneButton = addRenderableWidget(Button.builder(redstoneButtonLabel(), b -> cycleRedstoneMode())
                .bounds(this.leftPos + 7, this.topPos + 65, 18, 14)
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
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncRedstoneButtons();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        g.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
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
        if (this.menu.burnTimeTotal() <= 0) {
            return;
        }
        int lit = Math.min(14, 14 * this.menu.burnTime() / this.menu.burnTimeTotal());
        if (lit > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_SPRITE, 14, 14, 0, 14 - lit,
                    this.leftPos + 59, this.topPos + 59 + 14 - lit, 14, lit);
        }
    }

    private void drawProgress(GuiGraphicsExtractor g) {
        int progress = Math.min(24, 24 * this.menu.progress() / this.menu.maxProgress());
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
            return fit(localized("screen.earth_on_minecraft.machine.empty_input"), 92);
        }

        return ProcessingMachineBlock.findRecipe(this.menu.kind(), input).map(recipe -> {
            if (this.menu.gridPowered()) {
                return fit(localized("screen.earth_on_minecraft.machine.grid_powered") + " " + recipeSummary(recipe), 92);
            }
            if (!this.menu.hasBurningFuel() && !fuelSlotHasFuel()) {
                return fit(localized("screen.earth_on_minecraft.machine.missing_power"), 92);
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
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_on_minecraft.machine.fuel"),
                    Component.translatable("screen.earth_on_minecraft.machine.fuel.tooltip"),
                    Component.translatable("screen.earth_on_minecraft.machine.energy.tooltip", this.menu.energyPerTick()),
                    Component.translatable("screen.earth_on_minecraft.machine.fuel.examples")
            ), mouseX, mouseY);
        }
        if (isHovering(STATUS_X - 2, STATUS_Y - 2, STATUS_SIZE + 4, STATUS_SIZE + 4, mouseX, mouseY)
                || isHovering(64, 38, 36, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    stateTooltip(),
                    Component.literal(statusLine()),
                    Component.translatable("screen.earth_on_minecraft.machine.energy.tooltip", this.menu.energyPerTick()),
                    Component.translatable("screen.earth_on_minecraft.machine.fuel.tooltip")
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

    private void syncRedstoneButtons() {
        if (redstoneButton != null) {
            redstoneButton.setMessage(redstoneButtonLabel());
        }
    }

    private Component redstoneButtonLabel() {
        return Component.empty();
    }

    private int sideButtonX(Direction side) {
        return this.leftPos + switch (side) {
            case WEST -> 7;
            case EAST -> 29;
            default -> 18;
        };
    }

    private int sideButtonY(Direction side) {
        return this.topPos + switch (side) {
            case UP -> 17;
            case WEST, NORTH, EAST -> 28;
            case DOWN -> 39;
            case SOUTH -> 50;
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
        return Component.translatable("screen.earth_on_minecraft.side.short." + side.getName());
    }

    private String faceKey(Direction side) {
        return "screen.earth_on_minecraft.side.face." + side.getName();
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
        if (this.font.width(text) > 118) {
            return Component.literal(this.font.plainSubstrByWidth(text, 115) + "...");
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
        ItemStack fuel = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_FUEL).getItem();
        return ProcessingMachineBlockEntity.getFuelTicks(fuel) > 0;
    }

    private boolean isMultiblockMachine() {
        return MachineMultiblock.patternFor(this.menu.kind()) != MachineMultiblock.Pattern.NONE;
    }
}

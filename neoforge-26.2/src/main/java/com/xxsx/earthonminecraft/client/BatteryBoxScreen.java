package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.BatteryBoxMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class BatteryBoxScreen extends AbstractContainerScreen<BatteryBoxMenu> {
    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("earth_on_minecraft", "textures/gui/container/battery_box.png");
    private static final int ENERGY_X = 28;
    private static final int ENERGY_Y = 31;
    private static final int ENERGY_W = 120;
    private static final int ENERGY_H = 12;
    private static final int INPUT_PORT_X = 29;
    private static final int OUTPUT_PORT_X = 137;
    private static final int PORT_Y = 61;
    private static final int VANILLA_TEXT = 0xFF404040;
    private static final int MUTED = 0xFF606060;
    private static final int INPUT = 0xFF2D74C4;
    private static final int OUTPUT = 0xFFC46A22;

    public BatteryBoxScreen(BatteryBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8 + Math.max(0, (116 - this.font.width(trimmedTitle())) / 2);
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_on_minecraft.button.notebook"), button -> {
                    this.onClose();
                    EarthOnMinecraftClient.openNotebook();
                })
                .bounds(this.leftPos + this.imageWidth - 42, this.topPos + 4, 34, 14)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        g.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        drawEnergy(g, this.leftPos + ENERGY_X, this.topPos + ENERGY_Y, ENERGY_W, ENERGY_H, this.menu.energy(), this.menu.capacity());
        drawPorts(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_on_minecraft.energy.stored", this.menu.energy(), this.menu.capacity()),
                    Component.translatable("screen.earth_on_minecraft.energy.battery_transfer", this.menu.transferLimit()),
                    Component.translatable("screen.earth_on_minecraft.energy.battery_hint"),
                    Component.translatable("screen.earth_on_minecraft.energy.battery_tooltip")
            ), mouseX, mouseY);
        }
        if (isHovering(INPUT_PORT_X - 2, PORT_Y - 2, OUTPUT_PORT_X - INPUT_PORT_X + 14, 13, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_on_minecraft.energy.battery_ports_tooltip"),
                    Component.translatable("screen.earth_on_minecraft.energy.battery_transfer", this.menu.transferLimit())
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, trimmedTitle(), this.titleLabelX, this.titleLabelY, VANILLA_TEXT, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, VANILLA_TEXT, false);
        g.text(this.font, Component.literal(compactEnergy(this.menu.energy(), this.menu.capacity())), ENERGY_X, 22, VANILLA_TEXT, false);
        String percent = energyPercent(this.menu.energy(), this.menu.capacity());
        g.text(this.font, percent, ENERGY_X + ENERGY_W - this.font.width(percent), 22, VANILLA_TEXT, false);
        Component flow = Component.translatable("screen.earth_on_minecraft.energy.battery_flow");
        g.text(this.font, flow, 88 - this.font.width(flow) / 2, 49, MUTED, false);
        Component throughput = Component.translatable("screen.earth_on_minecraft.energy.battery_throughput", this.menu.transferLimit());
        g.text(this.font, throughput, 88 - this.font.width(throughput) / 2, 61, VANILLA_TEXT, false);
    }

    private void drawPorts(GuiGraphicsExtractor g) {
        drawPort(g, this.leftPos + INPUT_PORT_X, this.topPos + PORT_Y, INPUT, true);
        drawPort(g, this.leftPos + OUTPUT_PORT_X, this.topPos + PORT_Y, OUTPUT, false);
    }

    private void drawPort(GuiGraphicsExtractor g, int x, int y, int color, boolean pointsRight) {
        g.fill(x, y, x + 10, y + 10, 0xFF20282D);
        g.fill(x + 1, y + 1, x + 9, y + 9, 0xFF10151A);
        g.fill(x + 2, y + 4, x + 8, y + 6, color);
        if (pointsRight) {
            g.fill(x + 6, y + 2, x + 8, y + 8, color);
        } else {
            g.fill(x + 2, y + 2, x + 4, y + 8, color);
        }
    }

    private void drawEnergy(GuiGraphicsExtractor g, int x, int y, int width, int height, int energy, int capacity) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF3B3B3B);
        g.fill(x, y, x + width, y + height, 0xFF10151A);
        int filled = Math.min(width, width * Math.max(0, energy) / Math.max(1, capacity));
        if (filled > 0) {
            g.fill(x, y, x + filled, y + height, 0xFF31A9C9);
            g.fill(x, y, x + filled, y + 2, 0xFF82DFF0);
        }
    }

    private static String compactEnergy(int energy, int capacity) {
        return compact(energy) + "/" + compact(capacity) + " EOU";
    }

    private static String energyPercent(int energy, int capacity) {
        return Math.min(100, Math.max(0, energy) * 100 / Math.max(1, capacity)) + "%";
    }

    private static String compact(int value) {
        if (value >= 1000) {
            return value / 1000 + "k";
        }
        return Integer.toString(value);
    }

    private Component trimmedTitle() {
        String text = this.title.getString();
        if (this.font.width(text) > 110) {
            return Component.literal(this.font.plainSubstrByWidth(text, 107) + "...");
        }
        return this.title;
    }
}

package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.EarthOnMinecraft;
import com.xxsx.earthonminecraft.EnergyGeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EnergyGeneratorScreen extends AbstractContainerScreen<EnergyGeneratorMenu> {
    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath("earth_on_minecraft", "textures/gui/container/energy_generator.png");
    private static final Identifier FUEL_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int ENERGY_X = 82;
    private static final int ENERGY_Y = 35;
    private static final int ENERGY_W = 76;
    private static final int ENERGY_H = 10;
    private static final int VANILLA_TEXT = 0xFF404040;
    private static final int MUTED = 0xFF606060;
    private static final int POWER = 0xFF1E7C9A;
    private static final int WARNING = 0xFFAA3322;

    public EnergyGeneratorScreen(EnergyGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = Math.max(8, 87 - this.font.width(trimmedTitle()) / 2);
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
        drawGeneratorChrome(g);
        drawFuel(g);
        drawEnergy(g, this.leftPos + ENERGY_X, this.topPos + ENERGY_Y, ENERGY_W, ENERGY_H, this.menu.energy(), this.menu.capacity());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawStatusDot(g);
        drawTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, trimmedTitle(), this.titleLabelX, this.titleLabelY, VANILLA_TEXT, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, VANILLA_TEXT, false);
        g.text(this.font, Component.translatable(this.menu.steamTurbineGenerator()
                ? "screen.earth_on_minecraft.energy.steam_source"
                : "screen.earth_on_minecraft.energy.fuel"), 38, 48, MUTED, false);
        g.text(this.font, Component.literal(compactEnergy(this.menu.energy(), this.menu.capacity())), ENERGY_X, 22, VANILLA_TEXT, false);
    }

    private void drawGeneratorChrome(GuiGraphicsExtractor g) {
        int accent = this.menu.steamTurbineGenerator() ? 0xFF2B91AA : 0xFFB05B27;
        int x = this.leftPos + 8;
        int y = this.topPos + 20;
        g.fill(x, y, x + 58, y + 17, 0x66303030);
        g.fill(x, y, x + 58, y + 2, accent);
        ItemStack icon = new ItemStack(this.menu.steamTurbineGenerator()
                ? EarthOnMinecraft.STEAM_TURBINE_GENERATOR.get()
                : EarthOnMinecraft.COMBUSTION_GENERATOR.get());
        g.item(icon, x + 1, y + 1);
        String type = Component.translatable(this.menu.steamTurbineGenerator()
                ? "screen.earth_on_minecraft.energy.type.steam_turbine"
                : "screen.earth_on_minecraft.energy.type.combustion").getString();
        g.text(this.font, fit(type, 37), x + 19, y + 5, accent, false);
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

    private void drawEnergy(GuiGraphicsExtractor g, int x, int y, int width, int height, int energy, int capacity) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF3B3B3B);
        g.fill(x, y, x + width, y + height, 0xFF10151A);
        int filled = Math.min(width, width * Math.max(0, energy) / Math.max(1, capacity));
        if (filled > 0) {
            g.fill(x, y, x + filled, y + height, 0xFF31A9C9);
            g.fill(x, y, x + filled, y + 2, 0xFF82DFF0);
        }
    }

    private Component statusLine() {
        if (this.menu.active()) {
            return Component.translatable("screen.earth_on_minecraft.energy.generator_active_short",
                    this.menu.generationPerTick(), this.menu.transferPerTick());
        }
        if (this.menu.energy() >= this.menu.capacity()) {
            return Component.translatable("screen.earth_on_minecraft.energy.full");
        }
        return Component.translatable("screen.earth_on_minecraft.energy.generator_waiting_short");
    }

    private int statusColor() {
        if (this.menu.active()) {
            return POWER;
        }
        return this.menu.energy() >= this.menu.capacity() ? MUTED : WARNING;
    }

    private void drawStatusDot(GuiGraphicsExtractor g) {
        int x = this.leftPos + ENERGY_X + ENERGY_W + 4;
        int y = this.topPos + ENERGY_Y + 3;
        g.fill(x - 1, y - 1, x + 6, y + 6, 0xFF3B3B3B);
        g.fill(x, y, x + 5, y + 5, statusColor());
        g.text(this.font, stateLabel(), this.leftPos + ENERGY_X, this.topPos + ENERGY_Y + 14, statusColor(), false);
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(38, 57, 18, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable(this.menu.steamTurbineGenerator()
                            ? "screen.earth_on_minecraft.energy.steam_source.tooltip"
                            : "screen.earth_on_minecraft.energy.generator_fuel"),
                    Component.translatable(this.menu.steamTurbineGenerator()
                            ? "screen.earth_on_minecraft.energy.steam_source.examples"
                            : "screen.earth_on_minecraft.machine.fuel.examples")
            ), mouseX, mouseY);
        }
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("screen.earth_on_minecraft.energy.stored", this.menu.energy(), this.menu.capacity()),
                    stateTooltip(),
                    statusLine(),
                    Component.translatable("screen.earth_on_minecraft.energy.generator_hint"),
                    Component.translatable("screen.earth_on_minecraft.energy.generator_tooltip",
                            this.menu.generationPerTick(), this.menu.transferPerTick())
            ), mouseX, mouseY);
        }
        if (isHovering(ENERGY_X + ENERGY_W + 2, ENERGY_Y + 1, 9, 9, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(stateTooltip(), statusLine()), mouseX, mouseY);
        }
    }

    private Component stateLabel() {
        return Component.translatable(this.menu.active()
                ? "screen.earth_on_minecraft.machine.running"
                : "screen.earth_on_minecraft.machine.idle");
    }

    private Component stateTooltip() {
        return Component.translatable(this.menu.active()
                ? "screen.earth_on_minecraft.energy.state.running.tooltip"
                : "screen.earth_on_minecraft.energy.state.idle.tooltip");
    }

    private static String compactEnergy(int energy, int capacity) {
        return compact(energy) + "/" + compact(capacity) + " EOU";
    }

    private static String compact(int value) {
        if (value >= 1000) {
            return value / 1000 + "k";
        }
        return Integer.toString(value);
    }

    private Component trimmedTitle() {
        String text = this.title.getString();
        if (this.font.width(text) > 100) {
            return Component.literal(this.font.plainSubstrByWidth(text, 97) + "...");
        }
        return this.title;
    }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("..."))) + "...";
    }
}

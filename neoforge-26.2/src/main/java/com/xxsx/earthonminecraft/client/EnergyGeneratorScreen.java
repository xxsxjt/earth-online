package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.EarthOnMinecraft;
import com.xxsx.earthonminecraft.EnergyGeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EnergyGeneratorScreen extends AbstractContainerScreen<EnergyGeneratorMenu> {
    private static final Identifier BG_LOCATION = Identifier.fromNamespaceAndPath(
            "earth_on_minecraft", "textures/gui/container/energy_generator.png");
    private static final Identifier FUEL_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int ENERGY_X = 76;
    private static final int ENERGY_Y = 31;
    private static final int ENERGY_W = 92;
    private static final int ENERGY_H = 9;
    private static final int STATUS_Y = 45;

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
        addRenderableWidget(Button.builder(Component.empty(), button -> {
                    this.onClose();
                    EarthOnMinecraftClient.openNotebook();
                })
                .bounds(leftPos + 155, topPos + 4, 14, 14)
                .tooltip(Tooltip.create(Component.translatable("screen.earth_on_minecraft.button.notebook.tooltip")))
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        g.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, leftPos, topPos,
                0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        drawGeneratorIdentity(g);
        drawFuel(g);
        EarthGuiSupport.drawEnergyBar(g, leftPos + ENERGY_X, topPos + ENERGY_Y,
                ENERGY_W, ENERGY_H, menu.energy(), menu.capacity());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        EarthGuiSupport.drawNotebookGlyph(g, leftPos + 157, topPos + 6);
        drawStatus(g);
        drawTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, EarthGuiSupport.trim(font, title, 140), titleLabelX, titleLabelY,
                EarthGuiSupport.TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, EarthGuiSupport.TEXT, false);
        g.text(font, Component.literal(EarthGuiSupport.compactEnergy(menu.energy(), menu.capacity())),
                ENERGY_X, 20, EarthGuiSupport.TEXT, false);
        Component source = Component.translatable(menu.steamTurbineGenerator()
                ? "screen.earth_on_minecraft.energy.steam_source"
                : "screen.earth_on_minecraft.energy.fuel");
        g.text(font, source, 26, 47, EarthGuiSupport.MUTED, false);
        String rate = "+" + menu.generationPerTick() + " / " + menu.transferPerTick() + " EOU/t";
        g.text(font, EarthGuiSupport.fit(font, rate, ENERGY_W), ENERGY_X, 59,
                EarthGuiSupport.TEXT, false);
    }

    private void drawGeneratorIdentity(GuiGraphicsExtractor g) {
        int accent = menu.steamTurbineGenerator() ? 0xFF2B91AA : 0xFFB05B27;
        int x = leftPos + 8;
        int y = topPos + 21;
        g.fill(x - 2, y - 2, x + 58, y, accent);
        ItemStack icon = new ItemStack(menu.steamTurbineGenerator()
                ? EarthOnMinecraft.STEAM_TURBINE_GENERATOR.get()
                : EarthOnMinecraft.COMBUSTION_GENERATOR.get());
        g.item(icon, x, y + 1);
        String type = Component.translatable(menu.steamTurbineGenerator()
                ? "screen.earth_on_minecraft.energy.type.steam_turbine"
                : "screen.earth_on_minecraft.energy.type.combustion").getString();
        g.text(font, EarthGuiSupport.fit(font, type, 38), x + 19, y + 5, accent, false);
    }

    private void drawFuel(GuiGraphicsExtractor g) {
        if (menu.burnTimeTotal() <= 0) {
            return;
        }
        int lit = Math.min(14, 14 * menu.burnTime() / menu.burnTimeTotal());
        if (lit > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_SPRITE, 14, 14, 0, 14 - lit,
                    leftPos + 59, topPos + 59 + 14 - lit, 14, lit);
        }
    }

    private void drawStatus(GuiGraphicsExtractor g) {
        int x = leftPos + ENERGY_X;
        int y = topPos + STATUS_Y;
        EarthGuiSupport.drawInset(g, x, y, ENERGY_W, 11);
        int color = statusColor();
        g.fill(x + 3, y + 3, x + 8, y + 8, color);
        String text = EarthGuiSupport.fit(font, statusLine().getString(), ENERGY_W - 15);
        g.text(font, text, x + 11, y + 1, color, false);
    }

    private Component statusLine() {
        if (menu.active()) {
            return Component.translatable("screen.earth_on_minecraft.machine.running");
        }
        if (menu.energy() >= menu.capacity()) {
            return Component.translatable("screen.earth_on_minecraft.energy.full");
        }
        return Component.translatable("screen.earth_on_minecraft.energy.generator_waiting_short");
    }

    private int statusColor() {
        if (menu.active()) {
            return EarthGuiSupport.POWER;
        }
        return menu.energy() >= menu.capacity() ? EarthGuiSupport.MUTED : EarthGuiSupport.WARNING;
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(38, 57, 18, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable(menu.steamTurbineGenerator()
                            ? "screen.earth_on_minecraft.energy.steam_source.tooltip"
                            : "screen.earth_on_minecraft.energy.generator_fuel"),
                    Component.translatable(menu.steamTurbineGenerator()
                            ? "screen.earth_on_minecraft.energy.steam_source.examples"
                            : "screen.earth_on_minecraft.machine.fuel.examples")
            ), mouseX, mouseY);
        }
        if (isHovering(ENERGY_X, 20, ENERGY_W, 21, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.earth_on_minecraft.energy.stored", menu.energy(), menu.capacity()),
                    Component.translatable("screen.earth_on_minecraft.energy.generator_hint"),
                    Component.translatable("screen.earth_on_minecraft.energy.generator_tooltip",
                            menu.generationPerTick(), menu.transferPerTick())
            ), mouseX, mouseY);
        }
        if (isHovering(ENERGY_X, STATUS_Y, ENERGY_W, 11, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(stateTooltip(), statusLine()), mouseX, mouseY);
        }
    }

    private Component stateTooltip() {
        return Component.translatable(menu.active()
                ? "screen.earth_on_minecraft.energy.state.running.tooltip"
                : "screen.earth_on_minecraft.energy.state.idle.tooltip");
    }
}

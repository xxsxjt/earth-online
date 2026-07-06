package com.xxsx.earthonline.client;

import com.xxsx.earthonline.ProcessingMachineBlock;
import com.xxsx.earthonline.ProcessingMachineBlockEntity;
import com.xxsx.earthonline.ProcessingMachineMenu;
import com.xxsx.earthonline.MachineMultiblock;
import com.xxsx.earthonline.RouteGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ProcessingMachineScreen extends AbstractContainerScreen<ProcessingMachineMenu> {
    private static final int PANEL = 0xFF313736;
    private static final int PANEL_DARK = 0xFF202725;
    private static final int PANEL_SOFT = 0xFF3E4744;
    private static final int SLOT = 0xFF68716E;
    private static final int SLOT_INNER = 0xFF101615;
    private static final int TEXT = 0xFFE9E0CF;
    private static final int MUTED = 0xFFA8B0AA;
    private static final int ACCENT = 0xFF84D3A5;
    private static final int WARNING = 0xFFFFCF6A;
    private static final int BAR_BG = 0xFF3B4648;
    private static final int BAR = 0xFF69C58F;

    private final List<Button> redstoneButtons = new ArrayList<>();

    public ProcessingMachineScreen(ProcessingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 206, 206);
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 18;
        this.inventoryLabelY = 100;
    }

    @Override
    protected void init() {
        super.init();
        redstoneButtons.clear();
        addRedstoneButton(8, "screen.earth_online.redstone.always.short", ProcessingMachineMenu.BUTTON_REDSTONE_ALWAYS);
        addRedstoneButton(70, "screen.earth_online.redstone.require_signal.short", ProcessingMachineMenu.BUTTON_REDSTONE_REQUIRE_SIGNAL);
        addRedstoneButton(132, "screen.earth_online.redstone.require_no_signal.short", ProcessingMachineMenu.BUTTON_REDSTONE_REQUIRE_NO_SIGNAL);

        addRenderableWidget(Button.builder(Component.translatable("screen.earth_online.button.notebook"), button -> {
                    this.onClose();
                    EarthOnlineClient.openNotebook();
                })
                .bounds(this.leftPos + this.imageWidth - 50, this.topPos + 4, 42, 18)
                .build());
        syncRedstoneButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncRedstoneButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xB8000000);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, PANEL);
        g.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF6D7A7C);
        g.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 22, PANEL_DARK);
        g.fill(this.leftPos + 8, this.topPos + 27, this.leftPos + 63, this.topPos + 69, PANEL_SOFT);
        g.fill(this.leftPos + 111, this.topPos + 27, this.leftPos + 190, this.topPos + 69, PANEL_SOFT);
        g.outline(this.leftPos + 8, this.topPos + 27, 55, 42, 0x663B4648);
        g.outline(this.leftPos + 111, this.topPos + 27, 79, 42, 0x663B4648);
        g.fill(this.leftPos + 8, this.topPos + 74, this.leftPos + 190, this.topPos + 97, PANEL_DARK);
        g.outline(this.leftPos + 8, this.topPos + 74, 182, 23, 0x665E6A67);

        drawSlot(g, 44, 42, 0xFF7A8B8A);
        drawOutputSlots(g);
        drawProgress(g);

        g.text(this.font, Component.translatable("screen.earth_online.machine.input"), this.leftPos + 18, this.topPos + 31, MUTED, false);
        g.text(this.font, Component.translatable("screen.earth_online.machine.outputs"), this.leftPos + 119, this.topPos + 31, MUTED, false);
        g.text(this.font, Component.translatable("screen.earth_online.machine.redstone"), this.leftPos + 12, this.topPos + 66, ACCENT, false);
        g.text(this.font, Component.translatable("screen.earth_online.machine.inventory"), this.leftPos + this.inventoryLabelX, this.topPos + this.inventoryLabelY, MUTED, false);

        super.extractRenderState(g, mouseX, mouseY, delta);

        drawStatus(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
    }

    private void drawOutputSlots(GuiGraphicsExtractor g) {
        int[][] outputSlots = {
                {116, 25}, {134, 25}, {152, 25}, {170, 25},
                {116, 47}, {134, 47}, {152, 47}
        };
        for (int[] slot : outputSlots) {
            drawSlot(g, slot[0], slot[1], SLOT);
        }
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y, int border) {
        int sx = this.leftPos + x - 1;
        int sy = this.topPos + y - 1;
        g.fill(sx, sy, sx + 18, sy + 18, border);
        g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_INNER);
    }

    private void drawProgress(GuiGraphicsExtractor g) {
        int x = this.leftPos + 69;
        int y = this.topPos + 42;
        g.fill(x, y, x + 35, y + 8, BAR_BG);
        int progress = Math.min(35, 35 * this.menu.progress() / this.menu.maxProgress());
        if (progress > 0) {
            g.fill(x, y, x + progress, y + 8, this.menu.active() ? BAR : WARNING);
        }
        g.fill(x + 35, y - 3, x + 40, y + 11, BAR_BG);
        g.fill(x + 40, y + 1, x + 45, y + 7, BAR_BG);
    }

    private void drawStatus(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        ProcessingMachineBlockEntity.RedstoneMode redstone = this.menu.redstoneMode();
        String status = localized(this.menu.active() ? "screen.earth_online.machine.running" : "screen.earth_online.machine.idle");
        int color = this.menu.active() ? ACCENT : MUTED;
        String bottom = status + " / " + localized(redstone.descriptionKey());
        if (this.font.width(bottom) > this.imageWidth - 16) {
            bottom = this.font.plainSubstrByWidth(bottom, this.imageWidth - 22) + "...";
        }
        g.text(this.font, bottom, this.leftPos + 8, this.topPos + this.imageHeight - 12, color, false);

        if (!this.menu.structureValid()) {
            MachineMultiblock.Pattern pattern = MachineMultiblock.patternFor(this.menu.kind());
            g.text(this.font, Component.translatable("screen.earth_online.machine.structure_missing"), this.leftPos + 8, this.topPos + 24, WARNING, false);
            g.text(this.font, Component.translatable(pattern.screenKey()), this.leftPos + 8, this.topPos + 35, WARNING, false);
            return;
        }

        if (isHovering(8, 76, 58, 18, mouseX, mouseY)
                || isHovering(70, 76, 58, 18, mouseX, mouseY)
                || isHovering(132, 76, 58, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable(redstone.labelKey()),
                    Component.translatable(redstone.descriptionKey()),
                    Component.translatable("screen.earth_online.redstone.tooltip")
            ), mouseX, mouseY);
        }

        ItemStack input = this.menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (input.isEmpty()) {
            g.text(this.font, Component.translatable("screen.earth_online.machine.empty_input"), this.leftPos + 8, this.topPos + 24, MUTED, false);
            return;
        }

        ProcessingMachineBlock.findRecipe(this.menu.kind(), input).ifPresentOrElse(recipe -> {
            String note = recipeSummary(recipe);
            if (this.font.width(note) > 150) {
                note = this.font.plainSubstrByWidth(note, 147) + "...";
            }
            g.text(this.font, note, this.leftPos + 8, this.topPos + 24, ACCENT, false);
        }, () -> g.text(this.font, Component.translatable("screen.earth_online.machine.unsupported_input"), this.leftPos + 8, this.topPos + 24, WARNING, false));
    }

    private void addRedstoneButton(int x, String labelKey, int id) {
        Button button = addRenderableWidget(Button.builder(Component.translatable(labelKey), b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }).bounds(this.leftPos + x, this.topPos + 76, 58, 18).build());
        redstoneButtons.add(button);
    }

    private void syncRedstoneButtons() {
        ProcessingMachineBlockEntity.RedstoneMode mode = this.menu.redstoneMode();
        setButtonState(0, "screen.earth_online.redstone.always.short", mode == ProcessingMachineBlockEntity.RedstoneMode.ALWAYS);
        setButtonState(1, "screen.earth_online.redstone.require_signal.short", mode == ProcessingMachineBlockEntity.RedstoneMode.REQUIRE_SIGNAL);
        setButtonState(2, "screen.earth_online.redstone.require_no_signal.short", mode == ProcessingMachineBlockEntity.RedstoneMode.REQUIRE_NO_SIGNAL);
    }

    private void setButtonState(int index, String labelKey, boolean selected) {
        if (index >= redstoneButtons.size()) {
            return;
        }
        Button button = redstoneButtons.get(index);
        String label = localized(labelKey);
        button.setMessage(Component.literal(selected ? "[" + label + "]" : label));
        button.active = !selected;
    }

    private String recipeSummary(ProcessingMachineBlock.Recipe recipe) {
        if (Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(java.util.Locale.ROOT).startsWith("zh")) {
            return recipe.note();
        }
        return localized("screen.earth_online.machine.recipe_ready") + ": " + RouteGuide.describeOutputs(recipe);
    }

    private static String localized(String key) {
        return Language.getInstance().getOrDefault(key);
    }
}

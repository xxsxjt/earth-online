package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.SettlementBoardMenu;
import com.xxsx.earthonminecraft.living.SettlementSavedData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettlementBoardScreen extends AbstractContainerScreen<SettlementBoardMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;
    private static final int PAPER = 0xFFF0E5C7;
    private static final int PAPER_DARK = 0xFFD4C39D;
    private static final int WOOD = 0xFF6D4C2D;
    private static final int INK = 0xFF2E2A24;
    private static final int MUTED = 0xFF665E51;
    private static final int ACCENT = 0xFF2E6F62;
    private static final int WARNING = 0xFFA85C32;

    private int tab;

    public SettlementBoardScreen(SettlementBoardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.titleLabelY = 7;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        int tabY = topPos + 28;
        addRenderableWidget(tabButton("screen.earth_on_minecraft.settlement_board.tab.overview", leftPos + 8, tabY, 0));
        addRenderableWidget(tabButton("screen.earth_on_minecraft.settlement_board.tab.people", leftPos + 62, tabY, 1));
        addRenderableWidget(tabButton("screen.earth_on_minecraft.settlement_board.tab.facilities", leftPos + 116, tabY, 2));
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_on_minecraft.button.notebook"), button -> {
                    this.onClose();
                    EarthOnMinecraftClient.openNotebook();
                })
                .bounds(leftPos + 126, topPos + 146, 42, 14)
                .build());
        refreshTabButtons();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        g.fill(leftPos, topPos, leftPos + WIDTH, topPos + HEIGHT, WOOD);
        g.fill(leftPos + 3, topPos + 3, leftPos + WIDTH - 3, topPos + HEIGHT - 3, PAPER_DARK);
        g.fill(leftPos + 6, topPos + 6, leftPos + WIDTH - 6, topPos + HEIGHT - 6, PAPER);
        g.fill(leftPos + 7, topPos + 24, leftPos + WIDTH - 7, topPos + 25, WOOD);
        g.fill(leftPos + 7, topPos + 46, leftPos + WIDTH - 7, topPos + 47, PAPER_DARK);
        g.fill(leftPos + 7, topPos + 142, leftPos + WIDTH - 7, topPos + 143, PAPER_DARK);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        SettlementSavedData.SettlementSnapshot snapshot = menu.snapshot();
        Component settlementName = Component.translatable(snapshot.nameKey());
        g.centeredText(font, settlementName, WIDTH / 2, 8, INK);
        Component subtitle = Component.translatable("screen.earth_on_minecraft.settlement_board.subtitle",
                Component.translatable(snapshot.scaleKey()), Component.translatable(snapshot.technologyKey()));
        g.centeredText(font, subtitle, WIDTH / 2, 18, MUTED);

        switch (tab) {
            case 1 -> drawPeople(g, snapshot);
            case 2 -> drawFacilities(g, snapshot);
            default -> drawOverview(g, snapshot);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (tab == 0 && isHovering(10, 111, 156, 10, mouseX, mouseY)) {
            SettlementSavedData.SettlementSnapshot snapshot = menu.snapshot();
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.earth_on_minecraft.settlement_board.security", snapshot.security()),
                    Component.translatable("screen.earth_on_minecraft.settlement_board.reputation", snapshot.reputation()),
                    Component.translatable("screen.earth_on_minecraft.settlement_board.security.tooltip")
            ), mouseX, mouseY);
        }
    }

    private void drawOverview(GuiGraphicsExtractor g, SettlementSavedData.SettlementSnapshot snapshot) {
        int y = 52;
        g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.profile"), 10, y, ACCENT, false);
        g.text(font, Component.translatable(snapshot.profileNameKey()), 72, y, INK, false);
        y += 13;
        g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.population"), 10, y, ACCENT, false);
        g.text(font, Integer.toString(snapshot.residentCount()), 72, y, INK, false);
        y += 13;
        g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.industries"), 10, y, ACCENT, false);
        g.text(font, joinKeys(snapshot.industryKeys()), 72, y, INK, false);
        y += 16;
        g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.needs"), 10, y, WARNING, false);
        g.text(font, joinKeys(snapshot.demandKeys()), 48, y, INK, false);
        y += 13;
        g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.supplies"), 10, y, ACCENT, false);
        g.text(font, joinKeys(snapshot.supplyKeys()), 48, y, INK, false);
        y += 17;
        drawSecurityBar(g, 10, y, 156, snapshot.security());
        g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.reputation",
                snapshot.reputation()), 10, y + 10, MUTED, false);
    }

    private void drawPeople(GuiGraphicsExtractor g, SettlementSavedData.SettlementSnapshot snapshot) {
        int y = 52;
        if (snapshot.residents().isEmpty()) {
            drawWrapped(g, Component.translatable("screen.earth_on_minecraft.settlement_board.no_residents"), 10, y, 156, MUTED);
            return;
        }
        int shown = 0;
        for (SettlementSavedData.ResidentSummary resident : snapshot.residents()) {
            Identifier id = Identifier.parse(resident.roleId());
            Component role = Component.translatable("resident.earth_on_minecraft.role." + id.getPath());
            Component name = Component.translatable(resident.nameKey());
            String skill = Component.translatable("screen.earth_on_minecraft.settlement_board.skill",
                    resident.skill()).getString();
            int labelWidth = Math.max(48, 146 - font.width(skill));
            String label = font.plainSubstrByWidth(name.getString() + " · " + role.getString(), labelWidth);
            g.text(font, label, 12, y, INK, false);
            g.text(font, skill, 164 - font.width(skill), y, ACCENT, false);
            y += 13;
            shown++;
            if (y > 124) {
                break;
            }
        }
        if (snapshot.residents().size() > shown) {
            g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.more_residents",
                    snapshot.residents().size() - shown), 12, 132, MUTED, false);
        } else {
            g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.shift_hint"),
                    12, 132, MUTED, false);
        }
    }

    private void drawFacilities(GuiGraphicsExtractor g, SettlementSavedData.SettlementSnapshot snapshot) {
        int y = 52;
        if (snapshot.facilityCounts().isEmpty()) {
            drawWrapped(g, Component.translatable("screen.earth_on_minecraft.settlement_board.no_facilities"), 10, y, 156, MUTED);
            return;
        }
        for (Map.Entry<String, Integer> entry : snapshot.facilityCounts().entrySet()) {
            g.fill(11, y + 1, 17, y + 7, facilityColor(entry.getKey()));
            g.text(font, Component.translatable(entry.getKey()), 22, y, INK, false);
            String count = "x" + entry.getValue();
            g.text(font, count, 164 - font.width(count), y, ACCENT, false);
            y += 13;
            if (y > 130) {
                break;
            }
        }
        g.text(font, Component.translatable("screen.earth_on_minecraft.settlement_board.facility_hint"),
                12, 132, MUTED, false);
    }

    private void drawSecurityBar(GuiGraphicsExtractor g, int x, int y, int width, int security) {
        g.fill(x, y, x + width, y + 9, 0xFF4A4238);
        g.fill(x + 1, y + 1, x + width - 1, y + 8, 0xFFB9A980);
        int fill = (width - 2) * Math.max(0, Math.min(100, security)) / 100;
        g.fill(x + 1, y + 1, x + 1 + fill, y + 8, security >= 60 ? ACCENT : WARNING);
        Component label = Component.translatable("screen.earth_on_minecraft.settlement_board.security", security);
        g.centeredText(font, label, x + width / 2, y, 0xFFFFFFFF);
    }

    private Button tabButton(String key, int x, int y, int index) {
        return Button.builder(Component.translatable(key), button -> {
                    tab = index;
                    refreshTabButtons();
                })
                .bounds(x, y, 52, 16)
                .build();
    }

    private void refreshTabButtons() {
        for (int i = 0; i < Math.min(3, renderables.size()); i++) {
            if (renderables.get(i) instanceof Button button) {
                button.active = i != tab;
            }
        }
    }

    private Component joinKeys(List<String> keys) {
        if (keys.isEmpty()) {
            return Component.translatable("screen.earth_on_minecraft.settlement_board.none");
        }
        List<String> values = new ArrayList<>();
        for (String key : keys) {
            values.add(Component.translatable(key).getString());
        }
        String joined = String.join(" / ", values);
        return Component.literal(font.plainSubstrByWidth(joined, 94));
    }

    private void drawWrapped(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) {
        int lineY = y;
        for (var line : font.split(text, width)) {
            g.text(font, line, x, lineY, color, false);
            lineY += 11;
        }
    }

    private static int facilityColor(String key) {
        if (key.endsWith("power")) return 0xFFE6B23B;
        if (key.endsWith("waterworks")) return 0xFF4D8FB3;
        if (key.endsWith("mining")) return 0xFF8A6A4A;
        if (key.endsWith("workshop")) return 0xFF9A563A;
        if (key.endsWith("warehouse")) return 0xFF6A7A45;
        return ACCENT;
    }
}

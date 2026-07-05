package com.xxsx.earthonline.client;

import com.xxsx.earthonline.ProcessingMachineBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ProcessingMachineScreen extends Screen {
    private static final int BG = 0xEE101417;
    private static final int PANEL = 0xFF253036;
    private static final int PANEL_2 = 0xFF314149;
    private static final int TEXT = 0xFFE7E1D3;
    private static final int MUTED = 0xFF9AA7A7;
    private static final int ACCENT = 0xFF84D3A5;
    private static final int OUTPUT = 0xFFFFCF6A;

    private final ProcessingMachineBlock.Kind kind;
    private final BlockPos pos;
    private final List<ProcessingMachineBlock.Recipe> recipes;
    private int scroll;
    private String status = "提示：主手拿材料，点击按钮或直接右键机器即可处理。";

    public ProcessingMachineScreen(ProcessingMachineBlock.Kind kind, BlockPos pos) {
        super(Component.literal(kind.displayName()));
        this.kind = kind;
        this.pos = pos;
        this.recipes = ProcessingMachineBlock.recipesFor(kind);
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        int bottom = top + panelHeight() - 28;
        addRenderableWidget(Button.builder(Component.literal("处理主手物品"), b -> processHeldItem())
                .bounds(left + 76, bottom, 108, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("打开手册"), b -> EarthOnlineClient.openNotebook())
                .bounds(left + 190, bottom, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> onClose())
                .bounds(left + panelWidth() - 70, bottom, 54, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xC0000000);
        int left = left();
        int top = top();
        int pw = panelWidth();
        int ph = panelHeight();

        g.fill(left, top, left + pw, top + ph, PANEL);
        g.outline(left, top, pw, ph, 0xFF526870);
        g.fill(left + 4, top + 4, left + pw - 4, top + 31, PANEL_2);
        g.centeredText(font, kind.displayName(), left + pw / 2, top + 10, TEXT);
        g.text(font, kind.description(), left + 14, top + 36, MUTED);
        g.text(font, "拿对应输入右键机器即可处理 1 个材料；空手右键打开本界面。", left + 14, top + 48, MUTED);

        super.extractRenderState(g, mouseX, mouseY, delta);

        int listX = left + 14;
        int listY = top + 66;
        int listW = pw - 28;
        int listH = ph - 100;
        g.fill(listX, listY, listX + listW, listY + listH, 0x44111111);
        g.outline(listX, listY, listW, listH, 0x66526870);

        int rowH = 34;
        int visible = Math.max(1, listH / rowH);
        int maxScroll = Math.max(0, recipes.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (recipes.isEmpty()) {
            g.centeredText(font, "这台机器还没有配方。", listX + listW / 2, listY + 20, MUTED);
        } else {
            for (int i = scroll; i < Math.min(recipes.size(), scroll + visible); i++) {
                ProcessingMachineBlock.Recipe recipe = recipes.get(i);
                int y = listY + 4 + (i - scroll) * rowH;
                g.fill(listX + 4, y, listX + listW - 4, y + rowH - 4, i % 2 == 0 ? 0x223D4A4D : 0x182A3437);
                g.text(font, recipe.inputStack().getItemName(), listX + 10, y + 4, ACCENT);
                String outputText = "-> " + recipe.outputStacks().stream()
                        .map(stack -> stack.getCount() + "x " + stack.getItemName().getString())
                        .reduce((a, b) -> a + " + " + b)
                        .orElse("无产物");
                int outputW = Math.max(40, listW - 146);
                if (font.width(outputText) > outputW) {
                    outputText = font.plainSubstrByWidth(outputText, Math.max(20, outputW - font.width("..."))) + "...";
                }
                g.text(font, outputText, listX + 132, y + 4, OUTPUT);
                g.text(font, recipe.note(), listX + 10, y + 18, MUTED);
            }
        }

        if (maxScroll > 0) {
            int barX = listX + listW - 6;
            g.fill(barX, listY + 4, barX + 3, listY + listH - 4, 0x55333A3D);
            int knobH = Math.max(12, (listH - 8) * visible / recipes.size());
            int knobY = listY + 4 + (listH - 8 - knobH) * scroll / maxScroll;
            g.fill(barX, knobY, barX + 3, knobY + knobH, 0xCC84D3A5);
        }

        g.text(font, "配方数: " + recipes.size(), left + 14, top + ph - 22, MUTED);
        String line = status;
        int statusW = pw - 210;
        if (font.width(line) > statusW) {
            line = font.plainSubstrByWidth(line, Math.max(20, statusW - font.width("..."))) + "...";
        }
        g.text(font, line, left + 194, top + ph - 22, status.startsWith("已") ? ACCENT : MUTED);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visible = Math.max(1, (panelHeight() - 100) / 34);
        int maxScroll = Math.max(0, recipes.size() - visible);
        if (maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void processHeldItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            status = "无法处理：客户端玩家尚未就绪。";
            return;
        }
        if (mc.player.getMainHandItem().isEmpty()) {
            status = "主手没有材料：请拿着输入物品再点处理。";
            return;
        }

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        if (result instanceof InteractionResult.Success) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            status = "已发送处理请求：看背包和聊天反馈。";
        } else if (result instanceof InteractionResult.Fail) {
            status = "处理失败：这个物品可能不是本机器的输入。";
        } else {
            status = "已尝试处理：如果没有变化，请换对应输入材料。";
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(null);
        } else {
            Minecraft.getInstance().gui.setScreen(null);
        }
    }

    private int panelWidth() {
        return Math.min(560, Math.max(320, this.width - 24));
    }

    private int panelHeight() {
        return Math.min(330, Math.max(220, this.height - 24));
    }

    private int left() {
        return (this.width - panelWidth()) / 2;
    }

    private int top() {
        return (this.height - panelHeight()) / 2;
    }
}

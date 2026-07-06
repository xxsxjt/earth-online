package com.xxsx.earthonline.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class FieldGeologyNotebookScreen extends Screen {
    private static final int PAPER = 0xFFF0DFBA;
    private static final int PAPER_EDGE = 0xFFD6B77D;
    private static final int PAPER_SOFT = 0xFFFFEBC4;
    private static final int INK = 0xFF2B2117;
    private static final int INK_SOFT = 0xFF3C2F20;
    private static final int MUTED = 0xFF755F42;
    private static final int GREEN = 0xFF2D6841;
    private static final int RED = 0xFF8B372C;
    private static final int BLUE = 0xFF255A78;
    private static final int GOLD = 0xFF956215;
    private static final int LINE_HEIGHT = 12;

    private final boolean chinese = isChineseLanguage();
    private final List<Page> pages = chinese ? createPages() : createEnglishPages();
    private final List<Button> tabButtons = new ArrayList<>();

    private int page;
    private int scroll;
    private Button prevButton;
    private Button nextButton;

    public FieldGeologyNotebookScreen() {
        super(Component.translatable("screen.earth_online.notebook.title"));
    }

    @Override
    protected void init() {
        this.tabButtons.clear();
        int left = bookLeft();
        int top = bookTop();
        int tabX = left + 10;
        int tabY = top + 42;
        int navW = Math.max(68, contentLeft() - left - 20);
        int tabCols = pages.size() > 14 ? 3 : pages.size() > 7 ? 2 : 1;
        int tabW = Math.max(22, (navW - (tabCols - 1) * 3) / tabCols);
        int tabH = tabCols == 1 ? 18 : 14;
        int tabGap = tabCols == 1 ? 3 : 2;

        for (int i = 0; i < pages.size(); i++) {
            final int index = i;
            int col = i % tabCols;
            int row = i / tabCols;
            Button button = addRenderableWidget(Button.builder(Component.literal(pages.get(i).shortTitle), b -> setPage(index))
                    .bounds(tabX + col * (tabW + 3), tabY + row * (tabH + tabGap), tabW, tabH)
                    .build());
            tabButtons.add(button);
        }

        int bottom = top + bookHeight() - 28;
        prevButton = addRenderableWidget(Button.builder(Component.translatable("screen.earth_online.notebook.prev"), b -> setPage(page - 1))
                .bounds(left + bookWidth() - 182, bottom, 76, 20)
                .build());
        nextButton = addRenderableWidget(Button.builder(Component.translatable("screen.earth_online.notebook.next"), b -> setPage(page + 1))
                .bounds(left + bookWidth() - 100, bottom, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.earth_online.notebook.close"), b -> onClose())
                .bounds(left + 12, bottom, 54, 20)
                .build());
        updateButtonState();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xC0000000);

        int left = bookLeft();
        int top = bookTop();
        int bw = bookWidth();
        int bh = bookHeight();

        g.fill(left, top, left + bw, top + bh, PAPER_EDGE);
        g.fill(left + 3, top + 3, left + bw - 3, top + bh - 3, PAPER);
        g.fill(left + 8, top + 32, contentLeft() - 8, top + bh - 36, 0x18FFFFFF);
        g.fill(contentLeft() - 10, top + 36, left + bw - 12, top + bh - 38, PAPER_SOFT);
        g.outline(left, top, bw, bh, 0xFF50351F);
        g.outline(contentLeft() - 10, top + 36, left + bw - contentLeft() - 2, bh - 74, 0x3050351F);
        g.fill(contentLeft() - 18, top + 24, contentLeft() - 17, top + bh - 34, 0x553B2A1B);
        g.fill(left + 4, top + 4, left + bw - 4, top + 20, 0x1F2A2118);

        drawCenteredTitle(g, tr("screen.earth_online.notebook.title"), left + bw / 2, top + 8, INK);
        draw(g, tr("screen.earth_online.notebook.subtitle"), left + 14, top + 24, MUTED);
        draw(g, tr("screen.earth_online.notebook.page", page + 1, pages.size()), left + bw - 70, top + 24, MUTED);

        super.extractRenderState(g, mouseX, mouseY, delta);

        Page current = pages.get(page);
        int contentX = contentLeft();
        int contentY = top + 46;
        int contentW = contentWidth();
        int contentH = Math.max(80, bh - 84);
        g.fill(contentX - 6, contentY - 6, contentX + contentW - 2, contentY + 14, 0x20FFFFFF);
        g.fill(contentX - 6, contentY - 6, contentX - 2, contentY + 14, current.color);
        drawHeading(g, current.title, contentX, contentY - 3, current.color);
        g.fill(contentX, contentY + 12, contentX + Math.min(contentW, 146), contentY + 13, current.color);
        List<Line> wrapped = wrap(current);
        int visible = visibleLines(contentH);
        int maxScroll = Math.max(0, wrapped.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int y = contentY + 18;
        for (int i = scroll; i < Math.min(wrapped.size(), scroll + visible); i++) {
            Line line = wrapped.get(i);
            if (line.blank) {
                y += 4;
                continue;
            }
            if (line.heading) {
                g.fill(contentX + line.indent - 3, y - 1, contentX + contentW - 6, y + 10, 0x16FFFFFF);
                g.fill(contentX + line.indent - 3, y - 1, contentX + line.indent, y + 10, line.color);
                g.text(font, line.text, contentX + line.indent + 4, y, line.color, true);
            } else {
                g.fill(contentX + line.indent - 5, y + 4, contentX + line.indent - 2, y + 7, current.color);
                g.text(font, line.text, contentX + line.indent, y, line.color, false);
            }
            y += LINE_HEIGHT;
        }

        if (maxScroll > 0) {
            int barX = contentX + contentW + 4;
            int barTop = contentY + 16;
            int barH = contentH - 16;
            g.fill(barX, barTop, barX + 3, barTop + barH, 0x3050351F);
            int knobH = Math.max(12, barH * visible / wrapped.size());
            int knobY = barTop + (barH - knobH) * scroll / maxScroll;
            g.fill(barX, knobY, barX + 3, knobY + knobH, 0xAA50351F);
        }

        draw(g, tr("screen.earth_online.notebook.footer"), contentX, top + bh - 17, MUTED);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= contentLeft() - 8 && mouseX <= contentLeft() + contentWidth() + 16
                && mouseY >= bookTop() + 38 && mouseY <= bookTop() + bookHeight() - 38) {
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(null);
        } else {
            Minecraft.getInstance().gui.setScreen(null);
        }
    }

    private void setPage(int next) {
        if (next < 0 || next >= pages.size()) {
            return;
        }
        this.page = next;
        this.scroll = 0;
        updateButtonState();
    }

    private void updateButtonState() {
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).active = i != page;
        }
        if (prevButton != null) {
            prevButton.active = page > 0;
        }
        if (nextButton != null) {
            nextButton.active = page < pages.size() - 1;
        }
    }

    private List<Line> wrap(Page current) {
        List<Line> result = new ArrayList<>();
        int width = contentWidth();
        for (Entry entry : current.entries) {
            if (entry.text.isBlank()) {
                result.add(new Line(FormattedCharSequence.EMPTY, 0, INK_SOFT, false, true));
                continue;
            }
            int lineWidth = Math.max(30, width - entry.indent);
            for (FormattedCharSequence seq : font.split(FormattedText.of(entry.text), lineWidth)) {
                result.add(new Line(seq, entry.indent, entry.color, entry.heading, false));
            }
        }
        return result;
    }

    private int visibleLines(int contentH) {
        return Math.max(5, (contentH - 24) / LINE_HEIGHT);
    }

    private int bookWidth() {
        return Math.min(560, Math.max(320, this.width - 18));
    }

    private int bookHeight() {
        return Math.min(336, Math.max(224, this.height - 18));
    }

    private int bookLeft() {
        return (this.width - bookWidth()) / 2;
    }

    private int bookTop() {
        return (this.height - bookHeight()) / 2;
    }

    private int contentLeft() {
        if (pages.size() > 14) {
            return bookLeft() + Math.min(138, Math.max(118, bookWidth() / 3 + 18));
        }
        return bookLeft() + Math.min(124, Math.max(92, bookWidth() / 4 + 18));
    }

    private int contentWidth() {
        return bookLeft() + bookWidth() - contentLeft() - 20;
    }

    private void draw(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(font, text, x, y, color, false);
    }

    private void drawCenteredTitle(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.centeredText(font, text, x + 1, y + 1, 0x55351F10);
        g.centeredText(font, text, x, y, color);
    }

    private void drawHeading(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(font, text, x + 1, y + 1, 0x55351F10, false);
        g.text(font, text, x, y, color, false);
    }

    private static List<Page> createPages() {
        List<Page> result = new ArrayList<>();
        result.add(page("入门", "1. 这是什么？", GREEN,
                "Earth Online 不取消 MC 的合成魔法，也不破坏其他 mod 依赖的铁锭、铜锭、金锭、钻石、红石。",
                "它重做的是自然来源：矿石不再是几块随机散落的原版矿，而是更像现实的矿床、矿脉、煤层和岩筒。",
                "",
                "手册获取：一块泥土、任意木板或任意石头都能合成野外地质手册。",
                "新玩家只需要记住一条线：挖矿床 -> 机器处理 -> 得到原版兼容产物。",
                "右键机器会打开像熔炉一样的界面；把材料放入输入格，机器会自动把多种产物放到输出格。"));

        result.add(page("索引", "2. 手里有这个，该去哪？", GOLD,
                "矿床/矿石方块：先去颚式破碎机。它负责把大块自然来源变成可继续处理的碎块。",
                "磁铁矿碎块 / 黄铜矿碎块 / 辰砂碎块 / 金伯利岩碎块：去球磨机，磨成更容易分选的粉末。",
                "磁铁矿粉 / 赤铁矿粉 / 镁铁质硅酸盐粉：去磁选机，目标通常是铁精矿。",
                "黄铜矿粉 / 黄铁矿粉 / 金粉 / 青金石矿 / 红石矿：去浮选槽，目标是精矿。",
                "铜精矿 / 黄铁矿粉 / 辰砂粉 / 方解石粉：去焙烧炉，发生热处理或煅烧。",
                "铁精矿 / 焙烧铜精矿 / 金精矿：去还原炉，得到兼容原版的铁锭、铜锭、金锭。",
                "盐粉：去电解槽进入氯碱工业；也可去结晶器整理盐卤路线。",
                "煤粉 / 煤焦油 / 煤气：去气体分离器、精馏塔、蒸汽裂解炉，进入煤化工和塑料路线。",
                "硫粉 / 氨 / 磷矿粉 / 甲醇 / 乙烯 / 氯化钾：去化学反应釜，做酸、肥料和有机化工入口。",
                "长石粉 / 云母粉 / 铝硅酸盐粉：去筛分机或工业窑炉，进入玻璃、砖和水泥路线。",
                "水泥粉 / 聚乙烯 / 聚丙烯 / PVC 树脂：去压粉机，得到 MC 兼容建筑块、线或黏性材料。",
                "不想背：把鼠标停在 Earth Online 物品上，tooltip 会自动告诉你下一台机器。"));

        result.add(page("机器", "3. 第一版机器怎么用？", BLUE,
                "颚式破碎机：矿石/矿床 -> 碎块 + 伴生粉/尾粉。",
                "球磨机：碎块/岩石 -> 粉末，是大多数流程的第二步。",
                "筛分机：按颗粒大小和密度分离，适合金伯利岩、含金石英和尾粉。",
                "磁选机：处理磁铁矿粉、赤铁矿粉、镁铁质硅酸盐粉，产出铁精矿。",
                "浮选槽：处理硫化矿和宝石矿物，产出铜精矿、青金石精矿、红石精矿等。",
                "焙烧炉：把硫化矿/碳酸盐热处理，产出焙烧矿、硫粉、石灰粉。",
                "还原炉：把精矿转成 MC 兼容锭，同时产出矿渣。",
                "浸出槽/电解槽：模拟湿法冶金和电化学精炼。",
                "压粉机：把粉末或精矿压回 MC 物品，减少背包折磨。",
                "化工机器：反应釜、精馏塔、混合机、结晶器、工业窑炉、气体分离器、肥料造粒机、聚合釜。",
                "新增化工设备：蒸汽裂解炉、合成塔、吸收塔，用来支撑塑料、合成氨、尿素、酸气吸收等路线。",
                "右键任何机器都会打开该机器界面；JEI 也会显示 Earth Online 工业处理分类。"));

        result.add(page("铁铜", "4. 铁和铜路线", RED,
                "铁：",
                "磁铁矿矿石 Fe3O4 -> 破碎机 -> 磁铁矿碎块 + 尾粉。",
                "磁铁矿碎块 -> 球磨机 -> 磁铁矿粉。",
                "磁铁矿粉 -> 磁选机 -> 铁精矿 + 尾粉。",
                "铁精矿 -> 还原炉 -> minecraft:iron_ingot + 矿渣。",
                "",
                "铜：",
                "黄铜矿矿石 CuFeS2 -> 破碎机 -> 黄铜矿碎块 + 黄铁矿粉。",
                "黄铜矿碎块 -> 球磨机 -> 黄铜矿粉。",
                "黄铜矿粉 -> 浮选槽 -> 铜精矿 + 黄铁矿粉 + 尾粉。",
                "铜精矿 -> 焙烧炉 -> 焙烧铜精矿 + 硫粉。",
                "焙烧铜精矿 -> 还原炉 -> minecraft:copper_ingot + 矿渣。",
                "铜精矿也可以走电解槽，产出铜锭、硫粉和矿渣。"));

        result.add(page("贵重", "5. 金、钻石、青金石、绿宝石", GOLD,
                "金：含金石英脉 -> 破碎机/筛分机/浸出槽 -> 金粉或金精矿 -> 还原炉/电解槽 -> 金锭。",
                "钻石：含金刚石金伯利岩 -> 破碎机 -> 金伯利岩碎块 + 金刚石砂粒；金刚石砂粒 -> 压粉机 -> 钻石。",
                "青金石：青金石矿石 -> 浮选槽 -> 青金石精矿 + 方解石粉 + 黄铁矿粉；精矿 -> 电解槽 -> 青金石。",
                "绿宝石：绿柱石矿脉 -> 浸出槽 -> 绿柱石精矿；精矿 -> 电解槽 -> 绿宝石 + 铝硅酸盐粉。",
                "红石：红石矿物矿石 -> 浮选/浸出/压粉，仍保留一点 MC 科学幻想味。"));

        result.add(page("岩石", "6. 岩石不是单一化学式", GREEN,
                "花岗岩、闪长岩、安山岩、深板岩、凝灰岩、砂岩、玄武岩等都是混合岩石，不应该硬写成一个化学式。",
                "花岗岩通常含石英、钾长石、斜长石、云母和少量铁氧化物；球磨后可得到石英粉、长石粉、云母粉和尾粉。",
                "方解石和滴水石主要是 CaCO3，可磨成方解石粉，再焙烧成石灰粉 CaO。",
                "玄武岩、黑石这类镁铁质岩石会给镁铁质硅酸盐粉，并可能分出少量磁铁矿或赤铁矿。",
                "现实分离不会 100% 纯净；MC 世界允许机器把它整理成可堆叠的纯粉末，避免玩家背包变成实验室垃圾场。"));

        result.add(page("化工", "7. 常见化学工业入口", BLUE,
                "氯碱工业：盐粉 -> 电解槽 -> 烧碱 + 氯气单元 + 氢气单元。",
                "硫酸工业：硫粉 -> 化学反应釜 -> 硫酸。",
                "硝酸/氮肥：氨 -> 反应釜 -> 硝酸；硝酸 -> 硝酸铵；硝酸铵 -> 肥料造粒机。",
                "磷肥：骨粉/磷矿粉 -> 磷酸 + 石膏；磷酸 -> 肥料母料。",
                "水泥：方解石/黏土 -> 混合机 -> 水泥生料 -> 工业窑炉 -> 水泥熟料 -> 水泥粉。",
                "矿渣水泥：矿渣 -> 混合机 -> 水泥生料/水泥粉，副产物不再只是垃圾。",
                "玻璃：二氧化硅粉 -> 混合机 -> 玻璃配合料 -> 工业窑炉 -> 玻璃。",
                "陶瓷/砖：黏土粉、长石粉、铝硅酸盐粉 -> 工业窑炉 -> 砖、玻璃或坯料副产。",
                "煤化工：煤粉 -> 气体分离器/精馏塔 -> 焦炭、煤焦油、煤气、乙烯、聚合物树脂。",
                "铝工业入口：砂/铝土矿粉 -> 浸出槽 -> 氢氧化铝 -> 氧化铝 -> 电解槽 -> 铝锭。"));

        result.add(page("肥料", "8. 氮磷钾肥料路线", GREEN,
                "空气压缩分离：玻璃瓶 -> 气体分离器 -> 氮气单元 + 氧气单元。",
                "合成氨：氮气单元 -> 合成塔 -> 氨 + 氢气单元。这里把空气补氢/循环气做成 MC 近似，方便测试。",
                "尿素：氨 -> 合成塔 -> 尿素 + 二氧化碳单元；尿素 -> 混合机/造粒机 -> 复合肥颗粒。",
                "磷肥：骨粉或磷矿粉 -> 反应釜 -> 磷酸 + 石膏；磷酸 -> 混合机 -> 肥料母料。",
                "钾肥：盐卤结晶 -> 结晶器 -> 氯化钾；氯化钾 -> 混合机 -> 钾肥 + 复合肥。",
                "硝酸钾：氯化钾 -> 反应釜 -> 硝酸钾 + 盐粉；硝酸钾 -> 造粒机 -> 复合肥。"));

        result.add(page("塑料", "9. 塑料和有机化工", GOLD,
                "煤化工入口：煤粉 -> 气体分离器 -> 焦炭 + 煤焦油 + 煤气单元。",
                "芳烃/烯烃：煤焦油 -> 蒸汽裂解炉 -> 苯 + 乙烯 + 丙烯 + 焦炭。",
                "石油入口：原油样品 -> 精馏塔 -> 天然气、石脑油、煤油、柴油、润滑油、沥青和石油焦。",
                "石脑油 -> 蒸汽裂解炉 -> 乙烯、丙烯、苯和炭黑，是现代塑料的常见入口。",
                "甲醇：煤气单元 -> 合成塔或蒸汽裂解炉 -> 甲醇 + 二氧化碳单元。",
                "甲醛树脂：甲醇 -> 反应釜 -> 甲醛；甲醛 -> 合成塔 -> 混合树脂前驱体。",
                "聚乙烯：乙烯 -> 聚合釜 -> 聚乙烯树脂。",
                "聚丙烯：丙烯 -> 聚合釜 -> 聚丙烯树脂。",
                "PVC：乙烯 -> 反应釜 -> 氯乙烯；氯乙烯 -> 聚合釜 -> PVC 树脂。",
                "聚苯乙烯：苯 -> 反应釜 -> 苯乙烯；苯乙烯 -> 聚合釜 -> PS 树脂。",
                "PET：乙烯 -> 反应釜 -> 乙二醇；乙二醇 -> 合成塔 -> 对苯二甲酸；再进聚合釜得到 PET。",
                "橡胶/尼龙：丙烯可做合成橡胶；苯也能进入己内酰胺，再聚合为尼龙纤维。",
                "树脂下游：PE/PP/PET/尼龙 -> 线；PVC/PS/合成橡胶 -> 黏性或泡沫兼容材料。"));

        result.add(page("纸漆", "10. 造纸、颜料和涂料", BLUE,
                "造纸不是神秘合成：原木 -> 球磨机 -> 木片；木片 -> 反应釜 -> 纤维素浆。",
                "纤维素浆 -> 吸收塔 -> 漂白浆；漂白浆 -> 压粉机 -> 纸。",
                "纤维素浆也可以进反应釜得到纤维素纤维，再压成线，方便接回 MC 物品生态。",
                "砂中重矿物筛分会给二氧化钛，这是现代白色颜料和涂料的重要入口。",
                "赤铁矿粉 -> 反应釜 -> 氧化铁颜料，可接回红色染料。",
                "石脑油/柴油/润滑油等重烃路线会副产炭黑，可接回黑色染料和橡胶补强。",
                "二氧化钛、氧化铁颜料、炭黑 -> 混合机 -> 涂料基料 + 对应染料。",
                "这部分的目标是让纸、线、染料和建筑涂装也有现实来源，而不是只靠原版神秘合成。"));

        result.add(page("炼油", "11. 石油炼化快速路线", RED,
                "当前第一版没有单独生成油田方块，先从烟煤煤层破碎副产少量原油样品，作为石油工业入口。",
                "原油样品 -> 精馏塔：一次得到天然气、石脑油、煤油、柴油、润滑油、沥青和石油焦。",
                "天然气 -> 合成塔：氢气、甲醇和二氧化碳，是化肥、有机化工和合成气路线的入口。",
                "石脑油 -> 蒸汽裂解炉：乙烯、丙烯、苯、炭黑，是 PE、PP、PS、PET、橡胶和尼龙的共同上游。",
                "柴油馏分 -> 蒸汽裂解炉：更多丙烯、炭黑和石油焦，给橡胶、颜料和燃料闭环。",
                "润滑油 -> 反应釜/压粉机：回收炭黑、石油焦或黏性兼容材料。",
                "沥青 -> 压粉机：压成黑石路面材料；石油焦 -> 压粉机：压成煤和炭黑。",
                "这条路线先把现代炼油的“多馏分、多副产、多下游”做出来，后续再升级油田、流体和多输入炼厂。"));

        result.add(page("电池", "12. 电池与电化学材料", GOLD,
                "这不是先做电力系统，而是先把现代电池材料的来源和处理路线接进工业链。",
                "石油焦 -> 焙烧炉：石墨粉 + 活性炭。石墨粉可进负极浆料，活性炭可进水处理。",
                "炭黑 -> 反应釜：活性炭 + 电池级碳粉，连接石化副产和电池材料。",
                "盐卤结晶 -> 浸出槽：锂盐 + 氯化钾 + 盐粉，盐湖资源不再只给普通盐。",
                "红石精矿 -> 电解槽：锰氧化物粉 + 红石，保留一点 MC 科学幻想，但用途更明确。",
                "镁铁质硅酸盐粉 -> 浸出槽：镍前驱体 + 铁精矿 + 尾粉。",
                "石墨粉 / 锰氧化物粉 / 镍前驱体 -> 混合机：电极片和电池级前驱材料。",
                "锂盐 -> 反应釜：电解液；电解液或电极片 -> 电解槽：简易电池单元。",
                "简易电池单元 -> 压粉机：红石 + 铜锭，先作为兼容出口，未来再接真正能量系统。"));

        result.add(page("水处理", "13. 水处理与环保闭环", BLUE,
                "工业不是只产锭和塑料，也会产生尾粉、污泥、酸碱废液。第二轮开始把这些副产物流接回可用路线。",
                "水桶 -> 反应釜：硬水样品 + 桶；硬水样品 -> 反应釜：软化水 + 石灰处理渣。",
                "活性炭 -> 吸收塔：活性炭滤料 + 软化水；滤料后续可压滤成污泥饼并回收部分活性炭。",
                "盐酸或烧碱 -> 混合机：中和盐 + 软化水，先用单输入近似表达酸碱中和方向。",
                "尾粉 -> 混合机：稳定化尾矿 + 污泥饼，让选矿副产不只是垃圾。",
                "石灰处理渣 -> 结晶器：方解石粉 + 中和盐，回到建材和盐路线。",
                "污泥饼 -> 工业窑炉：砖 + 稳定化尾矿；稳定化尾矿 -> 压粉机：石头。",
                "软化水 -> 压粉机：回收水桶。这是第一版便携表达，未来流体系统会替代它。"));

        result.add(page("橡胶", "14. 橡胶与密封材料", GOLD,
                "橡胶路线把木材、生物质、石油化工和硫粉连接起来，最终接回 MC 黏性材料。",
                "木片 -> 浸出槽：天然胶乳 + 纤维素浆。这里先用木材表达植物乳胶来源，方便测试。",
                "天然胶乳 -> 反应釜：粗橡胶 + 软化水。",
                "粗橡胶 -> 反应釜：硫化橡胶 + 中和盐，表示硫化后的弹性提升。",
                "合成橡胶来自丙烯路线；硫化橡胶或合成橡胶 -> 混合机：橡胶复合料 + 炭黑。",
                "橡胶复合料 -> 压粉机：橡胶密封圈 + 黏液球。",
                "橡胶密封圈现在是中间物，未来会用于多输入机器、管线、流体和压力设备升级。"));

        result.add(page("有机", "15. 有机溶剂与树脂", RED,
                "这页把原本分散的石化、煤化工和农业来源接到常见溶剂/树脂。",
                "小麦或糖 -> 反应釜：乙醇 + 二氧化碳。乙醇不只是饮料梗，也是基础化工入口。",
                "乙醇 -> 反应釜：乙酸 + 工业溶剂；乙酸可继续盐化回收溶剂。",
                "丙烯 -> 反应釜：合成橡胶 + 丙酮 + 炭黑。",
                "苯 -> 反应釜：苯乙烯、己内酰胺、苯酚、工业溶剂和树脂前驱体。",
                "苯酚 -> 合成塔：酚醛树脂和环氧树脂；丙酮也能进入环氧树脂路线。",
                "酚醛树脂/环氧树脂 -> 聚合釜：聚合物树脂，接回塑料材料生态。",
                "工业溶剂 -> 吸收塔：乙醇 + 活性炭滤料，表达溶剂回收和吸附净化。"));

        result.add(page("合金", "16. 现代合金和钛", BLUE,
                "合金路线让钢、铝、硅铁、钛不再是孤立材料，而是能继续升级的现代冶金入口。",
                "黑石粉磨会给铬铁矿粉；铬铁矿粉 -> 还原炉：铬铁 + 矿渣。",
                "锰氧化物粉 -> 还原炉：锰铁 + 氧气单元。",
                "钢坯 -> 混合机：不锈钢坯 + 铬铁 + 锰铁。第一版用单输入表达合金方向，未来多输入会改真实配比。",
                "铝锭 -> 混合机：铝合金坯 + 镁粉。",
                "钛白粉 -> 反应釜：钛渣 + 四氯化钛；四氯化钛 -> 还原炉：海绵钛 + 氯化钙。",
                "不锈钢坯、铝合金坯、海绵钛都能压回兼容金属，先保证其他 mod 能吃到熟悉材料。"));

        result.add(page("半导体", "17. 半导体前置材料", GREEN,
                "半导体不要一口气做完整芯片工业，先把硅、氯硅烷、晶圆、掺杂剂和光刻胶前驱体做出来。",
                "二氧化硅粉 -> 还原炉：硅铁 + 冶金级硅 + 矿渣。",
                "冶金级硅 -> 反应釜：氯硅烷 + 盐酸。",
                "氯硅烷 -> 精馏塔：高纯硅 + 盐酸；高纯硅 -> 结晶器：多晶硅 + 氯硅烷。",
                "多晶硅 -> 压粉机：硅晶圆。",
                "红石精矿 -> 结晶器：掺杂剂粉 + 红石。",
                "环氧树脂 -> 反应釜：光刻胶前驱体 + 工业溶剂。",
                "硅晶圆 -> 混合机：红石 + 石英 + 掺杂剂粉。这里先接回 MC 红石生态，未来再接科技 mod。"));

        result.add(page("稀土", "18. 稀土、磁材和催化剂", GOLD,
                "稀土路线从砂、花岗岩和长石粉里来，避免凭空生成高科技材料。",
                "砂或花岗岩路线可得到独居石砂；长石粉筛分可得到氟碳铈矿粉。",
                "独居石砂/氟碳铈矿粉 -> 浸出槽：混合稀土氧化物 + 副产物 + 稀土尾渣。",
                "混合稀土氧化物 -> 结晶器：钕盐 + 稀土尾渣。",
                "钕盐 -> 合成塔：钕铁硼磁材 + 硅铁。",
                "钕铁硼磁材 -> 磁选机：红石 + 铁锭，作为磁性材料的兼容出口。",
                "催化剂路线：赤铁矿粉、钛渣、镍前驱体、金精矿可做铁基、钒基、镍基、铂族催化剂。",
                "催化剂现在直接给合成氨、硫酸、加氢、硝酸等路线加入口，后续会成为高级机器的真实前置。"));

        result.add(page("日化", "19. 氯碱、漂白和洗涤剂", BLUE,
                "盐粉 -> 电解槽：烧碱、氯气和氢气，这是现代氯碱工业的核心入口。",
                "氯气 -> 吸收塔：次氯酸钠 + 盐酸；烧碱也能进反应釜转成漂白液近似路线。",
                "次氯酸钠 -> 混合机：漂白粉 + 熟石灰，方便玩家理解“液体漂白剂”和“固体漂白粉”的区别。",
                "氢气 -> 反应釜：过氧化氢 + 氧气；过氧化氢 -> 吸收塔：漂白浆 + 软化水。",
                "小麦种子 -> 反应釜：皂基 + 甘油，用作植物油皂化的便携表达。",
                "皂基 -> 混合机：表面活性剂 + 甘油；表面活性剂 -> 混合机：洗涤剂粉 + 纯碱。",
                "洗涤剂粉 -> 压粉机：白色染料 + 硫酸钠，把日化产物接回 MC 染料生态。"));

        result.add(page("电子", "20. PCB 和电子材料", GREEN,
                "铜锭 -> 压粉机：铜线。这里先用压粉机代替拉丝机，减少第一版机器数量。",
                "二氧化硅粉 -> 工业窑炉：玻璃纤维布 + 玻璃。",
                "玻璃纤维布 -> 混合机：覆铜板 + 环氧树脂，表达玻纤/环氧/铜箔复合结构。",
                "铜线 -> 混合机：印刷电路板 + 覆铜板。",
                "光刻胶前驱体 -> 混合机：印刷电路板 + 工业溶剂，表示显影、清洗和溶剂回收。",
                "锡锭 -> 混合机：焊料合金 + 助焊剂；焊料合金 -> 混合机：印刷电路板 + 助焊剂。",
                "混合稀土氧化物 -> 结晶器：LED 荧光粉 + 钕盐。",
                "陶瓷绝缘件 -> 混合机：陶瓷基板 + LED 荧光粉，给未来电子/照明/科技联动留入口。",
                "印刷电路板 -> 压粉机：红石 + 铜线，当前作为兼容出口，未来可接科技 mod 电路板标签。"));

        result.add(page("铅锌", "21. 铅锌锡与焊料", RED,
                "尾粉 -> 筛分机：闪锌矿粉 + 方铅矿粉 + 二氧化硅粉。尾矿不是垃圾，常有伴生金属。",
                "闪锌矿粉 -> 浮选槽：氧化锌 + 硫粉；氧化锌 -> 还原炉：锌锭 + 矿渣。",
                "方铅矿粉 -> 浮选槽：铅锭 + 硫粉。第一版先不细分烧结、烟化和除杂。",
                "砂 -> 筛分机：会额外得到锡石粉；锡石粉 -> 还原炉：锡锭 + 矿渣。",
                "锡锭 -> 混合机：焊料合金 + 助焊剂。",
                "锌锭 -> 混合机：铝合金坯 + 矿渣，表示镀层/合金化入口。",
                "铅锭 -> 压粉机：焊料合金 + 稳定化尾矿，用稳定封装降低重金属路线的负担。"));

        result.add(page("陶瓷", "22. 陶瓷、耐火材料和隔热", GOLD,
                "黏土 -> 球磨机：黏土粉 + 高岭土粉 + 铝硅酸盐粉。",
                "黏土粉 -> 筛分机：高岭土粉 + 铝硅酸盐粉。",
                "高岭土粉 -> 混合机：陶瓷坯料 + 长石粉。",
                "高岭土粉 -> 工业窑炉：耐火黏土 + 氧化铝；耐火黏土 -> 工业窑炉：耐火砖 + 矿渣。",
                "陶瓷坯料 -> 工业窑炉：瓷坯；瓷坯 -> 压粉机：陶瓷绝缘件。",
                "镁铁质硅酸盐粉 -> 工业窑炉：矿物棉 + 矿渣。",
                "矿物棉 -> 压粉机：线 + 白色羊毛，用作隔热棉和纤维兼容出口。",
                "耐火砖、陶瓷绝缘件、陶瓷基板后续会成为高级炉、反应釜和电子机器升级材料。"));

        result.add(page("后续", "23. 后续会怎么扩展？", MUTED,
                "现在的机器已有输入/输出槽、处理时间、红石三模式、GUI 和 JEI 联动，目的是先让真实流程能玩、能测试。",
                "后续可以升级为：多输入配比、能耗、催化剂、流体、污染/热量、自动化接口。",
                "常见科技/魔法 mod 的联动不应塞进原版核心文档，而应走单独的兼容模块或数据驱动查询。",
                "兼容原则：自然来源和处理流程真实化，最终产物继续走原版标签和原版物品，最大限度兼容整合包。"));
        return List.copyOf(result);
    }

    private static List<Page> createEnglishPages() {
        List<Page> result = new ArrayList<>();
        result.add(page("Start", "1. What is Earth Online?", GREEN,
                "Earth Online keeps Minecraft's item ecosystem and crafting magic, but makes natural sources and industrial routes more realistic.",
                "The main loop is simple: find a deposit, process it in machines, then get compatible Minecraft products.",
                "",
                "Notebook recipe: one dirt, any plank, or any stone creates the Field Geology Notebook.",
                "Machines now work like furnaces: open the GUI, put input in the left slot, and wait for multiple outputs."));
        result.add(page("Routes", "2. If you have this, where does it go?", GOLD,
                "Ore and deposit blocks usually start in the Jaw Crusher.",
                "Chunks and rocks usually go to the Ball Mill.",
                "Magnetite, hematite, and mafic silicate powders go to the Magnetic Separator.",
                "Sulfide ores and gem ores usually go to the Flotation Cell.",
                "Concentrates go to roasting, reduction, leaching, or electrolysis depending on the material.",
                "If unsure, hover the item or check JEI. Earth Online adds route hints to items."));
        result.add(page("Machines", "3. Machine and multiblock basics", BLUE,
                "Small machines can run as compact blocks.",
                "Large real-world equipment needs support blocks around the controller.",
                "Heavy frame: Control Panel in front, Machine Casing left/right/back.",
                "Wet vessel: Control Panel in front, Steel Process Pipes left/right, Machine Casing behind.",
                "Heated line: Control Panel in front, Machine Casing left/right, Steel Process Pipe above.",
                "Tall column: Control Panel in front, casing left/right, pipes behind and two blocks upward.",
                "The GUI shows the required pattern if the structure is missing."));
        result.add(page("Iron", "4. Iron and copper", RED,
                "Iron route:",
                "Magnetite ore Fe3O4 -> crusher -> magnetite chunks + tailings.",
                "Magnetite chunks -> ball mill -> magnetite dust.",
                "Magnetite dust -> magnetic separator -> iron concentrate + tailings.",
                "Iron concentrate -> reduction furnace -> minecraft:iron_ingot + slag.",
                "",
                "Copper route:",
                "Chalcopyrite CuFeS2 -> crusher/ball mill/flotation -> copper concentrate.",
                "Copper concentrate -> roaster/reduction furnace/electrolytic cell -> copper ingot + sulfur/slag."));
        result.add(page("Rocks", "5. Rocks are mixtures", GREEN,
                "Granite, diorite, andesite, basalt, tuff, blackstone, and deepslate are not single pure chemicals.",
                "Granite can yield quartz dust, feldspar dust, mica dust, monazite sand, and tailings.",
                "Calcite and dripstone mainly represent CaCO3 routes.",
                "Minecraft machines are allowed to organize mixtures into clean stackable powders so the inventory stays playable."));
        result.add(page("Chem", "6. Common chemical industry", BLUE,
                "Chlor-alkali: salt -> electrolysis -> sodium hydroxide, chlorine, and hydrogen.",
                "Fertilizer: ammonia, nitric acid, phosphate rock, potash, and urea routes.",
                "Cement and glass: calcite/clay/silica -> mixing/kiln routes.",
                "Petrochemistry: crude oil or coal tar -> distillation/cracking -> fuels, olefins, aromatics, and plastics.",
                "Water treatment and waste routes turn tailings, sludge, acids, and bases back into usable products."));
        result.add(page("Materials", "7. Modern materials", GOLD,
                "Battery chain: graphite, lithium salt, nickel/manganese precursors, electrolyte, electrode sheets, battery cells.",
                "Semiconductor chain: silica -> metallurgical silicon -> chlorosilane -> high-purity silicon -> polysilicon -> wafers.",
                "Rare earth chain: monazite/bastnasite -> mixed rare earth oxides -> neodymium salts -> NdFeB magnets.",
                "Electronics chain: copper wire, fiberglass cloth, copper clad laminate, printed circuit boards, solder, ceramic substrate."));
        result.add(page("JEI", "8. JEI and compatibility", MUTED,
                "Earth Online should not replace every other mod's ecosystem.",
                "It changes natural sources, processing routes, mineral composition, and deposit shape.",
                "Final products still connect back to familiar Minecraft items and tags so modpacks remain compatible.",
                "Use JEI for exact recipes and the notebook for learning routes."));
        return List.copyOf(result);
    }

    private static Page page(String shortTitle, String title, int color, String... lines) {
        List<Entry> entries = new ArrayList<>();
        for (String line : lines) {
            boolean heading = !line.isBlank() && (line.endsWith("：") || line.length() <= 5 && line.endsWith(":"));
            int indent = heading || line.isBlank() ? 0 : line.startsWith("->") ? 16 : 10;
            int lineColor = heading ? color : INK_SOFT;
            entries.add(new Entry(line, indent, lineColor, heading));
        }
        return new Page(shortTitle, title, color, List.copyOf(entries));
    }

    private record Page(String shortTitle, String title, int color, List<Entry> entries) {
    }

    private record Entry(String text, int indent, int color, boolean heading) {
    }

    private record Line(FormattedCharSequence text, int indent, int color, boolean heading, boolean blank) {
    }

    private static boolean isChineseLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT).startsWith("zh");
    }

    private static String tr(String key, Object... args) {
        String raw = Language.getInstance().getOrDefault(key);
        return args.length == 0 ? raw : String.format(Locale.ROOT, raw, args);
    }
}

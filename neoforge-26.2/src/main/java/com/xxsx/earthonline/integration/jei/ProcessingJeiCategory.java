package com.xxsx.earthonline.integration.jei;

import com.xxsx.earthonline.EarthOnline;
import com.xxsx.earthonline.ProcessingMachineBlock;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ProcessingJeiCategory implements IRecipeCategory<ProcessingMachineBlock.Recipe> {
    private final IDrawableStatic background;
    private final IDrawable icon;

    public ProcessingJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(168, 72);
        this.icon = guiHelper.createDrawableItemLike(EarthOnline.JAW_CRUSHER.get());
    }

    @Override
    public IRecipeType<ProcessingMachineBlock.Recipe> getRecipeType() {
        return EarthOnlineJeiPlugin.PROCESSING;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Earth Online 工业处理");
    }

    @Override
    public int getWidth() {
        return background.getWidth();
    }

    @Override
    public int getHeight() {
        return background.getHeight();
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProcessingMachineBlock.Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 22).add(recipe.inputStack()).setStandardSlotBackground();
        int x = 72;
        int y = 12;
        int index = 0;
        for (var stack : recipe.outputStacks()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, x + (index % 4) * 22, y + (index / 4) * 22)
                    .add(stack)
                    .setOutputSlotBackground();
            index++;
        }
    }

    @Override
    public void draw(ProcessingMachineBlock.Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, recipe.kind().displayName(), 4, 2, 0xFF84D3A5);
        graphics.text(font, "->", 44, 27, 0xFF9AA7A7);
        String note = recipe.note();
        if (font.width(note) > 156) {
            note = font.plainSubstrByWidth(note, 153) + "...";
        }
        graphics.text(font, note, 4, 58, 0xFF9AA7A7);
    }
}

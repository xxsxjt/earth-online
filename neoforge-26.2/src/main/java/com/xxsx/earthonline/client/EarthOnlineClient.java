package com.xxsx.earthonline.client;

import net.minecraft.client.Minecraft;
import com.xxsx.earthonline.ProcessingMachineBlock;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class EarthOnlineClient {
    private EarthOnlineClient() {
    }

    public static void openNotebook() {
        Minecraft.getInstance().gui.setScreen(new FieldGeologyNotebookScreen());
    }

    public static void openMachine(ProcessingMachineBlock.Kind kind, BlockPos pos) {
        Minecraft.getInstance().gui.setScreen(new ProcessingMachineScreen(kind, pos));
    }
}

package com.xxsx.earthonminecraft;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class StyledSupportPartBlock extends SupportPartBlock {
    public static final EnumProperty<AssemblyStyle> ASSEMBLY_STYLE =
            EnumProperty.create("assembly_style", AssemblyStyle.class);

    public StyledSupportPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLY_STYLE, AssemblyStyle.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLY_STYLE);
    }

    public enum AssemblyStyle implements StringRepresentable {
        NONE("none"),
        HEAVY("heavy"),
        WET("wet"),
        HEATED("heated"),
        TOWER("tower");

        private final String serializedName;

        AssemblyStyle(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}

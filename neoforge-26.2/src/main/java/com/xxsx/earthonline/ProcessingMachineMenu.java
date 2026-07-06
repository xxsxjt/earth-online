package com.xxsx.earthonline;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ProcessingMachineMenu extends AbstractContainerMenu {
    public static final int BUTTON_REDSTONE_ALWAYS = 0;
    public static final int BUTTON_REDSTONE_REQUIRE_SIGNAL = 1;
    public static final int BUTTON_REDSTONE_REQUIRE_NO_SIGNAL = 2;
    private static final int PLAYER_INV_START = ProcessingMachineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;
    private final ProcessingMachineBlock.Kind kind;

    public ProcessingMachineMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos());
    }

    private ProcessingMachineMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(ProcessingMachineBlockEntity.SLOT_COUNT),
                new SimpleContainerData(ProcessingMachineBlockEntity.DATA_COUNT), pos,
                kindFromClientLevel(inventory, pos));
    }

    public ProcessingMachineMenu(int containerId, Inventory inventory, ProcessingMachineBlockEntity machine, ContainerData data) {
        this(containerId, inventory, machine, data, machine.getBlockPos(), machine.kind());
    }

    private ProcessingMachineMenu(int containerId, Inventory inventory, Container container, ContainerData data,
                                  BlockPos pos, ProcessingMachineBlock.Kind kind) {
        super(EarthOnline.PROCESSING_MACHINE_MENU.get(), containerId);
        checkContainerSize(container, ProcessingMachineBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, ProcessingMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.pos = pos;
        this.kind = kind;

        this.container.startOpen(inventory.player);
        addSlot(new Slot(container, ProcessingMachineBlockEntity.SLOT_INPUT, 44, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ProcessingMachineBlock.findRecipe(ProcessingMachineMenu.this.kind, stack).isPresent();
            }
        });

        int[][] outputSlots = {
                {116, 25}, {134, 25}, {152, 25}, {170, 25},
                {116, 47}, {134, 47}, {152, 47}
        };
        for (int i = 0; i < outputSlots.length; i++) {
            int slot = ProcessingMachineBlockEntity.SLOT_OUTPUT_START + i;
            addSlot(new OutputSlot(container, slot, outputSlots[i][0], outputSlots[i][1]));
        }

        addStandardInventorySlots(inventory, 18, 112);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (container instanceof ProcessingMachineBlockEntity machine && id >= BUTTON_REDSTONE_ALWAYS && id <= BUTTON_REDSTONE_REQUIRE_NO_SIGNAL) {
            machine.setRedstoneMode(ProcessingMachineBlockEntity.RedstoneMode.byId(id));
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index < ProcessingMachineBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ProcessingMachineBlock.findRecipe(this.kind, stack).isPresent()) {
            if (!moveItemStackTo(stack, ProcessingMachineBlockEntity.SLOT_INPUT, ProcessingMachineBlockEntity.SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INV_END) {
            if (!moveItemStackTo(stack, PLAYER_INV_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public ProcessingMachineBlock.Kind kind() {
        return kind;
    }

    public BlockPos pos() {
        return pos;
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return Math.max(1, data.get(1));
    }

    public ProcessingMachineBlockEntity.RedstoneMode redstoneMode() {
        return ProcessingMachineBlockEntity.RedstoneMode.byId(data.get(2));
    }

    public boolean active() {
        return data.get(3) != 0;
    }

    public boolean structureValid() {
        return data.get(4) != 0;
    }

    private static ProcessingMachineBlock.Kind kindFromClientLevel(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockState(pos).getBlock() instanceof ProcessingMachineBlock block) {
            return block.kind();
        }
        return ProcessingMachineBlock.Kind.CRUSHER;
    }

    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}

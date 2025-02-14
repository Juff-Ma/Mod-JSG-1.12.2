package tauri.dev.jsg.gui.container.capacitor;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import tauri.dev.jsg.block.JSGBlocks;
import tauri.dev.jsg.gui.container.JSGContainer;
import tauri.dev.jsg.gui.util.ContainerHelper;
import tauri.dev.jsg.packet.JSGPacketHandler;
import tauri.dev.jsg.packet.StateUpdatePacketToClient;
import tauri.dev.jsg.power.general.SmallEnergyStorage;
import tauri.dev.jsg.state.StateTypeEnum;
import tauri.dev.jsg.tileentity.energy.CapacitorTile;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CapacitorContainer extends JSGContainer {

    public CapacitorTile capTile;
    public Slot slot;
    private final BlockPos pos;
    private int lastEnergyStored;
    private int energyTransferedLastTick;

    public CapacitorContainer(IInventory playerInventory, World world, int x, int y, int z) {
        pos = new BlockPos(x, y, z);
        this.world = world;
        capTile = (CapacitorTile) world.getTileEntity(pos);
        IItemHandler itemHandler = Objects.requireNonNull(capTile).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        slot = new SlotItemHandler(itemHandler, 0, 80, 35);
        addSlotToContainer(slot);

        for (Slot slot : ContainerHelper.generatePlayerSlots(playerInventory, 81))
            addSlotToContainer(slot);
    }

    private final World world;

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public Block[] getAllowedBlocks() {
        return new Block[]{JSGBlocks.CAPACITOR_BLOCK};
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        ItemStack stack = getSlot(index).getStack();

        // Transferring from Capacitor to player's inventory
        if (index < 1) {
            if (!mergeItemStack(stack, 1, inventorySlots.size(), false)) {
                return ItemStack.EMPTY;
            }

            putStackInSlot(index, ItemStack.EMPTY);
        }

        // Transferring from player's inventory to Capacitor
        else {
            if (stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
                if (!slot.getHasStack()) {
                    ItemStack stack1 = stack.copy();
                    stack1.setCount(1);
                    slot.putStack(stack1);

                    stack.shrink(1);

                    return ItemStack.EMPTY;
                }
            }

            return ItemStack.EMPTY;
        }

        return stack;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        SmallEnergyStorage energyStorage = (SmallEnergyStorage) capTile.getCapability(CapabilityEnergy.ENERGY, null);

        if (lastEnergyStored != Objects.requireNonNull(energyStorage).getEnergyStored() || energyTransferedLastTick != capTile.getEnergyTransferedLastTick()) {
            for (IContainerListener listener : listeners) {
                if (listener instanceof EntityPlayerMP) {
                    JSGPacketHandler.INSTANCE.sendTo(new StateUpdatePacketToClient(pos, StateTypeEnum.GUI_UPDATE, capTile.getState(StateTypeEnum.GUI_UPDATE)), (EntityPlayerMP) listener);
                }
            }

            lastEnergyStored = energyStorage.getEnergyStored();
            energyTransferedLastTick = capTile.getEnergyTransferedLastTick();
        }
    }
}

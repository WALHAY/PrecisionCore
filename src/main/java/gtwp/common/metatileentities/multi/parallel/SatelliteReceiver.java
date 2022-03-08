package gtwp.common.metatileentities.multi.parallel;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.util.GTLog;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;
import gtwp.api.capability.IReceiver;
import gtwp.api.utils.GTWPChatUtils;
import gtwp.api.utils.ParallelAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

public class SatelliteReceiver extends MetaTileEntityMultiblockPart implements IReceiver {

    private int frequency = 0;
    private UUID netAddress;
    private SatelliteTransmitter pair = null;

    public SatelliteReceiver(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, 4);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder metaTileEntityHolder) {
        return new SatelliteReceiver(metaTileEntityId);
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return false;
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return null;
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if(playerIn.isSneaking()) frequency--;
        else frequency++;
        netAddress = generateNetAddress(playerIn, frequency);

        if(getWorld().isRemote) {
            GTWPChatUtils.sendMessage(playerIn, "Receiver frequency: " + frequency);
            GTWPChatUtils.sendMessage(playerIn, "UUID: " + netAddress);
        }
        return super.onScrewdriverClick(playerIn, hand, facing, hitResult);
    }

    @Override
    public UUID generateNetAddress(EntityPlayer player, int frequency) {
        return UUID.nameUUIDFromBytes((player.getName()+frequency).getBytes());
    }

    @Override
    public UUID getNetAddress() {
        return netAddress;
    }

    @Override
    public boolean setConnection(UUID netAddress) {
        pair = ParallelAPI.satelliteTransmitters.getOrDefault(netAddress, null);
        return isConnected();
    }

    public SatelliteTransmitter getConnection(){
        return pair;
    }

    @Override
    public boolean isConnected() {
        return pair != null;
    }

    @Override
    public void breakConnection() {
        pair = null;
    }

    @Override
    public void update() {
        super.update();
        if (getOffsetTimer() % 8 == 0)
            setConnection(netAddress);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        data.setUniqueId("netAddress", netAddress);
        data.setInteger("frequency", frequency);
        return super.writeToNBT(data);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        netAddress = data.getUniqueId("netAddress");
        frequency = data.getInteger("frequency");
        super.readFromNBT(data);
    }
}
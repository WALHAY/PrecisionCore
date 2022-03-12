package gtwp.common.metatileentities.multi.parallel;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;
import gtwp.api.capability.ITransmitter;
import gtwp.api.render.GTWPTextures;
import gtwp.api.utils.GTWPChatUtils;
import gtwp.api.utils.ParallelAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import scala.collection.Parallel;

import java.util.UUID;

public class SatelliteTransmitter extends MetaTileEntityMultiblockPart implements ITransmitter {

    private int frequency = 0;
    private UUID netAddress;

    public SatelliteTransmitter(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, 5);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder metaTileEntityHolder) {
        return new SatelliteTransmitter(metaTileEntityId);
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (facing == getFrontFacing()) {
            if (playerIn.isSneaking()) frequency--;
            else frequency++;
            ParallelAPI.removeSatelliteTransmitter(netAddress);
            netAddress = generateNetAddress(playerIn, frequency);
            ParallelAPI.addSatelliteTransmitter(netAddress, this);
            GTWPChatUtils.sendMessage(playerIn, "Transmitter frequency: " + frequency);
            GTWPChatUtils.sendMessage(playerIn, "UUID: " + netAddress);
        }
        return super.onScrewdriverClick(playerIn, hand, facing, hitResult);
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
    public UUID generateNetAddress(EntityPlayer player, int frequency) {
        return UUID.nameUUIDFromBytes((player.getName()+frequency).getBytes());
    }

    @Override
    public UUID getNetAddress() {
        return netAddress;
    }

    @Override
    public boolean isTransmitting() {
        if(getWorld().isRemote) scheduleRenderUpdate();
        return getController() != null && getController().isActive();
    }
    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        (getController() == null ? GTWPTextures.PARALLEL_HATCH_RED : getController() != null && getController().isActive() ? GTWPTextures.PARALLEL_HATCH_GREEN : GTWPTextures.PARALLEL_HATCH_YELLOW).renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    @Override
    public void update() {
        super.update();
        if(getOffsetTimer() % 8 == 0)
            scheduleRenderUpdate();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        data.setInteger("frequency", this.frequency);
        data.setUniqueId("netAddress", this.netAddress);
        super.writeToNBT(data);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        this.frequency = data.getInteger("frequency");
        this.netAddress = data.getUniqueId("netAddress");
        super.readFromNBT(data);
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeInt(frequency);
        buf.writeUniqueId(netAddress);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.frequency = buf.readInt();
        this.netAddress = buf.readUniqueId();
        getHolder().scheduleChunkForRenderUpdate();
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        ParallelAPI.removeSatelliteTransmitter(netAddress);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        ParallelAPI.removeSatelliteTransmitter(netAddress);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ParallelAPI.addSatelliteTransmitter(netAddress, this);
    }

    @Override
    public void onFirstTick() {
        super.onFirstTick();
        ParallelAPI.addSatelliteTransmitter(netAddress, this);
    }
}

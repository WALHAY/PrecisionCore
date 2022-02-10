package gtwp.metatileentities.multi;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;

import gregtech.api.capability.impl.FilteredFluidHandler;
import gregtech.api.capability.impl.FluidHandlerProxy;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.TankWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.BlockWorldState;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.util.GTLog;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityFluidHatch;
import gtwp.blocks.BlockIGlass;
import gtwp.blocks.BlockMultiTank;
import gtwp.blocks.GTWPMetaBlocks;
import gtwp.metatileentities.GTWPMetaTileEntities;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class MultiFluidMultiTank extends MultiblockControllerBase {

    private int capacity = 0;
    private List<FilteredFluidHandler> fluidHandlers;

    public MultiFluidMultiTank(ResourceLocation metaTileEntityId)
    {
        super(metaTileEntityId);
        initializeAbilities();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder metaTileEntityHolder) {
        return new MultiFluidMultiTank(metaTileEntityId);
    }

    protected void initializeAbilities() {
        this.importFluids = new FluidTankList(true, makeFluidTanks());
        this.exportFluids = importFluids;
        this.fluidInventory = new FluidHandlerProxy(this.importFluids, this.exportFluids);
    }

    @Nonnull
    private List<FluidTank> makeFluidTanks() {
        List<FluidTank> fluidTankList = new ArrayList<>(25);
        fluidHandlers = new ArrayList<>(25);
        for(int i = 0;i<25;i++)
        {
            FilteredFluidHandler fluidHandler = new FilteredFluidHandler(capacity/25);
            fluidTankList.add(fluidHandler);
            fluidHandlers.add(fluidHandler);
        }
        return fluidTankList;
    }

    private int getCapacityAt(BlockPos pos)
    {
        if(!getWorld().isAirBlock(pos)) {
            IBlockState storageState = getWorld().getBlockState(pos);
            Block storage = storageState.getBlock();
            if(storage instanceof BlockMultiTank) return ((BlockMultiTank) storage).getState(storageState).getCapacity();
        }
        return 0;
    }

    private int countCapacity() {
        BlockPos storagePos = getPos();
        int l_capacity = 0;
        for (int y = -1; y >= -7; y--)
        {
            for(int x = -1; x<1;x++)
            {
                for(int z = -1; z<1;z++)
                {
                    int n_cap = getCapacityAt(storagePos.add(x,y,z));
                    if(n_cap == 0) return 0;
                    l_capacity += n_cap/25;
                }
            }
        }
        return l_capacity;
    }

    private void onCapacityChange()
    {
        for(int i = 0;i<25;i++) {
            fluidHandlers.get(i).setCapacity(capacity);
        }
    }

    @Override
    protected void updateFormedValid() {
        if(getOffsetTimer() % 8 == 0) {
            int l_capacity = countCapacity();
            if(capacity != l_capacity) {
                capacity = l_capacity;
                onCapacityChange();
            }
        }
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("XXXXX", "GGGGG", "GGGGG", "GGGGG", "GGGGG", "GGGGG", "GGGGG", "GGGGG", "XXXXX")
                .aisle("XXXXX", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "XXXXX")
                .aisle("XXXXX", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "XXSXX")
                .aisle("XXXXX", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "GMMMG", "XXXXX")
                .aisle("XXXXX", "GGGGG", "GGGGG", "GGGGG", "GGGGG", "GGGGG", "GGGGG", "GGGGG", "XXXXX")
                .where('S', selfPredicate())
                .where('X', states(getCasingState()))
                .where('G', new TraceabilityPredicate(glassPredicate()).or(metaTileEntities(GTWPMetaTileEntities.IO_HATCH).setMaxGlobalLimited(1)))
                .where('M', new TraceabilityPredicate(storagePredicate()))
                .build();
    }



    private IBlockState getCasingState()
    {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.PTFE_INERT_CASING);
    }

    public static Predicate<BlockWorldState> glassPredicate() {
        return (blockWorldState) -> {
            return blockWorldState.getBlockState().getBlock() instanceof BlockIGlass;
        };
    }

    public static Predicate<BlockWorldState> storagePredicate() {
        return (blockWorldState) -> {
            return blockWorldState.getBlockState().getBlock() instanceof BlockMultiTank;
        };
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.INERT_PTFE_CASING;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        getFrontOverlay().renderSided(EnumFacing.UP, renderState, translation, pipeline);
    }

    /*@Override
    public boolean onWrenchClick(EntityPlayer playerIn, EnumHand hand, EnumFacing wrenchSide, CuboidRayTraceResult hitResult) {
        setFrontFacing(wrenchSide);
        return super.onWrenchClick(playerIn, hand, wrenchSide, hitResult);
    }

    @Override
    public boolean hasFrontFacing() {
        return true;
    }*/

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return null;
    }

    @Nonnull
    @Override
    protected ICubeRenderer getFrontOverlay() {
        return Textures.QUANTUM_TANK_OVERLAY;
    }


    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.multiblock.tank.tooltip"));
        tooltip.add(I18n.format("gregtech.machine.quantum_tank.capacity"));
    }
}

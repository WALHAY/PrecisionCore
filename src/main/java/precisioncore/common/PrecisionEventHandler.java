package precisioncore.common;

import gregtech.api.GregTechAPI;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import precisioncore.PrecisionCore;
import precisioncore.api.unification.material.PrecisionMaterials;

@Mod.EventBusSubscriber(modid = PrecisionCore.MODID)
public class PrecisionEventHandler {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void registerMaterials(GregTechAPI.MaterialEvent event) {
        PrecisionMaterials.init();
    }
}

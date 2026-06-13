package ai.herald.clientmod.neoforge;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.HeraldConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = HeraldConstants.MOD_ID)
public final class HeraldClientNeoForge {

    public HeraldClientNeoForge(IEventBus modBus) {
        // Run only on the physical client; never load on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            clientInit();
        }
    }

    private static void clientInit() {
        HeraldClientMod.init();
    }
}

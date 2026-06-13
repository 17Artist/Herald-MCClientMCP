package ai.herald.clientmod.forge;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.HeraldConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(HeraldConstants.MOD_ID)
public final class HeraldClientForge {

    public HeraldClientForge() {
        // Run only on the physical client; never load on a dedicated server.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> HeraldClientForge::clientInit);
    }

    private static void clientInit() {
        HeraldClientMod.init();
    }
}

package ai.herald.clientmod.neoforge;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.HeraldConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = HeraldConstants.MOD_ID)
public final class HeraldClientNeoForge {

    public HeraldClientNeoForge(IEventBus modBus) {
        HeraldClientMod.init();
    }
}

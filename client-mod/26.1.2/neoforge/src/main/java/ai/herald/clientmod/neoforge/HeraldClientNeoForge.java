package ai.herald.clientmod.neoforge;

import ai.herald.clientmod.HeraldClientMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("herald_client")
public class HeraldClientNeoForge {
    public HeraldClientNeoForge(IEventBus modBus) {
        HeraldClientMod.init();
    }
}

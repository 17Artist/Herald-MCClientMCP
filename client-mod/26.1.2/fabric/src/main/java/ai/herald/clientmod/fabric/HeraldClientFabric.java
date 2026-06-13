package ai.herald.clientmod.fabric;

import ai.herald.clientmod.HeraldClientMod;
import net.fabricmc.api.ClientModInitializer;

public final class HeraldClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HeraldClientMod.init();
    }
}

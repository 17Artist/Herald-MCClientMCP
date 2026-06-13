package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Sync: List player advancements from ClientAdvancements.
 * Params: completed?(boolean, null=all)
 * Returns: [{id, done, title?}]
 */
public final class QueryAdvancementsAction implements ActionExecutor {

    private static Field progressField;

    @SuppressWarnings("unchecked")
    private static Map<Advancement, AdvancementProgress> getProgress(ClientAdvancements advancements) {
        // Use reflection to find the progress map field (name varies by mapping)
        try {
            if (progressField == null) {
                for (Field f : ClientAdvancements.class.getDeclaredFields()) {
                    if (f.getType() == Map.class) {
                        f.setAccessible(true);
                        Object val = f.get(advancements);
                        if (val instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) val;
                            if (!map.isEmpty()) {
                                Map.Entry<?, ?> entry = map.entrySet().iterator().next();
                                if (entry.getKey() instanceof Advancement) {
                                    progressField = f;
                                    break;
                                }
                            } else {
                                // Empty map — assume it's the progress map if type matches
                                progressField = f;
                                break;
                            }
                        }
                    }
                }
            }
            if (progressField != null) {
                return (Map<Advancement, AdvancementProgress>) progressField.get(advancements);
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return McHelper.notInGame();

        String completedFilter = JsonUtil.getStringOrDefault(params, "completed", null);
        Boolean filterDone = completedFilter != null ? Boolean.parseBoolean(completedFilter) : null;

        ClientAdvancements advancements = connection.getAdvancements();
        Map<Advancement, AdvancementProgress> progressMap = getProgress(advancements);
        if (progressMap == null) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE,
                "Cannot access advancement progress data");
        }

        JsonArray arr = new JsonArray();
        for (Map.Entry<Advancement, AdvancementProgress> entry : progressMap.entrySet()) {
            Advancement adv = entry.getKey();
            AdvancementProgress progress = entry.getValue();
            boolean done = progress.isDone();

            if (filterDone != null && filterDone != done) continue;

            JsonObject obj = new JsonObject();
            obj.addProperty("id", adv.getId().toString());
            obj.addProperty("done", done);
            if (adv.getDisplay() != null) {
                obj.addProperty("title", adv.getDisplay().getTitle().getString());
                obj.addProperty("description", adv.getDisplay().getDescription().getString());
            }
            arr.add(obj);
        }

        JsonObject data = new JsonObject();
        data.add("advancements", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }
}

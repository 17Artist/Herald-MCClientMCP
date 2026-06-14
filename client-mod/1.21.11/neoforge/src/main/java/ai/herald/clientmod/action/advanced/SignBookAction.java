package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Port of BlackBoxPro advanced/SignBookAction.kt. */
public final class SignBookAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int slot = JsonUtil.requireInt(params, "slot");
        String title = JsonUtil.requireString(params, "title");
        JsonArray pagesArr = JsonUtil.getArrayOrEmpty(params, "pages");
        if (pagesArr.size() > 200) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Too many pages: " + pagesArr.size());
        }
        List<String> pages = new ArrayList<>(pagesArr.size());
        for (int i = 0; i < pagesArr.size(); i++) {
            String p = pagesArr.get(i).getAsString();
            if (p.length() > 32767) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Page " + i + " too long");
            pages.add(p);
        }
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundEditBookPacket(slot, pages, Optional.of(title)));
        return ActionResult.ok();
    }
}

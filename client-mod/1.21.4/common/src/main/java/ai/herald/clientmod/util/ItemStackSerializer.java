package ai.herald.clientmod.util;

import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * 1.20.1 simplified ItemStack → JSON serializer (mirrors BlackBoxPro's
 * ItemStackSerializer.kt API). 1.20.1 uses NBT instead of data components,
 * so the {@code components} map is synthesized from common NBT fields.
 */
public final class ItemStackSerializer {

    private ItemStackSerializer() {}

    /** Full serialization including SNBT fallback. */
    public static JsonObject serialize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            JsonObject empty = new JsonObject();
            empty.addProperty("empty", true);
            return empty;
        }
        JsonObject json = baseJson(stack);
        json.add("components", componentsJson(stack));
        CompoundTag tag = McVersionCompat.stackGetTag(stack);
        if (tag != null) {
            json.addProperty("nbt", tag.toString());
        }
        return json;
    }

    /** Lightweight per-slot serialization (no SNBT). */
    public static JsonObject serializeSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            JsonObject empty = new JsonObject();
            empty.addProperty("empty", true);
            return empty;
        }
        JsonObject json = baseJson(stack);
        json.add("components", componentsJson(stack));
        return json;
    }

    private static JsonObject baseJson(ItemStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("empty", false);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        json.addProperty("itemId", id != null ? id.toString() : "minecraft:air");
        json.addProperty("count", stack.getCount());
        return json;
    }

    private static JsonObject componentsJson(ItemStack stack) {
        JsonObject out = new JsonObject();
        CompoundTag tag = McVersionCompat.stackGetTag(stack);

        // custom_name (display.Name)
        if (McVersionCompat.stackHasCustomHoverName(stack)) {
            out.addProperty("minecraft:custom_name", stack.getHoverName().getString());
        }

        // lore (display.Lore)
        if (tag != null && tag.contains("display", Tag.TAG_COMPOUND)) {
            CompoundTag display = tag.getCompound("display");
            if (display.contains("Lore", Tag.TAG_LIST)) {
                ListTag loreList = display.getList("Lore", Tag.TAG_STRING);
                JsonArray arr = new JsonArray();
                for (int i = 0; i < loreList.size(); i++) {
                    arr.add(loreList.getString(i));
                }
                out.add("minecraft:lore", arr);
            }
        }

        // enchantments
        Map<Object, Integer> enchants = McVersionCompat.enchantmentHelperGetEnchantments(stack);
        if (!enchants.isEmpty()) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<Object, Integer> e : enchants.entrySet()) {
                ResourceLocation key = McVersionCompat.enchantmentRegistryKey(e.getKey());
                if (key != null) {
                    obj.addProperty(key.toString(), e.getValue());
                }
            }
            out.add("minecraft:enchantments", obj);
        }

        // damage / max_damage
        if (stack.isDamageableItem()) {
            out.addProperty("minecraft:damage", stack.getDamageValue());
            out.addProperty("minecraft:max_damage", stack.getMaxDamage());
        }

        // custom_data ≈ remainder of root tag (excluding display/Enchantments/Damage)
        if (tag != null && !tag.isEmpty()) {
            CompoundTag custom = tag.copy();
            custom.remove("display");
            custom.remove("Enchantments");
            custom.remove("StoredEnchantments");
            custom.remove("Damage");
            if (!custom.isEmpty()) {
                out.add("minecraft:custom_data", nbtToJson(custom));
            }
        }

        return out;
    }

    private static JsonObject nbtToJson(CompoundTag nbt) {
        JsonObject obj = new JsonObject();
        for (String key : nbt.getAllKeys()) {
            obj.add(key, nbtElementToJson(nbt.get(key)));
        }
        return obj;
    }

    private static JsonElement nbtElementToJson(Tag element) {
        if (element == null) return JsonNull.INSTANCE;
        if (element instanceof CompoundTag c) return nbtToJson(c);
        if (element instanceof ListTag list) {
            JsonArray arr = new JsonArray();
            for (Tag t : list) arr.add(nbtElementToJson(t));
            return arr;
        }
        String str = element.getAsString();
        try { return new JsonPrimitive(Integer.parseInt(str)); } catch (NumberFormatException ignored) {}
        try { return new JsonPrimitive(Long.parseLong(str)); } catch (NumberFormatException ignored) {}
        try { return new JsonPrimitive(Double.parseDouble(str)); } catch (NumberFormatException ignored) {}
        return new JsonPrimitive(str);
    }
}

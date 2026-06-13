package ai.herald.clientmod.util;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.InteractionHand;

import java.util.*;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Direct 1.20.4 API wrappers. No reflection needed for this version.
 * This class exists so that action code can compile unchanged.
 */
public final class McVersionCompat {
    private McVersionCompat() {}

    // ─── Inventory ───────────────────────────────────────────────────────

    public static int getSelectedSlot(Inventory inv) {
        return inv.selected;
    }

    public static void setSelectedSlot(Inventory inv, int slot) {
        inv.selected = slot;
    }

    public static List<ItemStack> getArmorItems(LivingEntity entity) {
        List<ItemStack> list = new ArrayList<>();
        entity.getArmorSlots().forEach(list::add);
        return list;
    }

    public static List<ItemStack> getArmorItems(Inventory inv) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inv.armor.size(); i++) {
            list.add(inv.armor.get(i));
        }
        return list;
    }

    public static ItemStack getOffhandItem(LivingEntity entity) {
        return entity.getOffhandItem();
    }

    public static ItemStack getOffhandItem(Inventory inv) {
        return inv.offhand.get(0);
    }

    // ─── Packets ─────────────────────────────────────────────────────────

    public static void sendRotPacket(ClientPacketListener conn, float yaw, float pitch, boolean onGround) {
        conn.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround));
    }

    public static void sendStatusOnlyPacket(ClientPacketListener conn, boolean onGround) {
        conn.send(new ServerboundMovePlayerPacket.StatusOnly(onGround));
    }

    public static void sendMoveVehiclePacket(ClientPacketListener conn, Entity vehicle) {
        conn.send(new ServerboundMoveVehiclePacket(vehicle));
    }

    public static void sendContainerClick(ClientPacketListener conn, int containerId, int revision,
                                          int slot, int button, ClickType clickType, ItemStack carried,
                                          Int2ObjectMap<ItemStack> changedSlots) {
        conn.send(new ServerboundContainerClickPacket(containerId, revision, slot, button, clickType, carried, changedSlots));
    }

    public static void sendPickItemPacket(ClientPacketListener conn, int slot) {
        conn.send(new ServerboundPickItemPacket(slot));
    }

    public static void sendSetBeaconPacket(ClientPacketListener conn, int primary, int secondary) {
        Optional<MobEffect> p = primary >= 0 ? Optional.ofNullable(BuiltInRegistries.MOB_EFFECT.byId(primary)) : Optional.empty();
        Optional<MobEffect> s = secondary >= 0 ? Optional.ofNullable(BuiltInRegistries.MOB_EFFECT.byId(secondary)) : Optional.empty();
        conn.send(new ServerboundSetBeaconPacket(p, s));
    }

    // PLACEHOLDER_CONTINUE_2

    public static Object createInteractPacket(Entity target, boolean sneaking, InteractionHand hand, Vec3 location) {
        if (location != null) {
            return ServerboundInteractPacket.createInteractionPacket(target, sneaking, hand, location);
        }
        return ServerboundInteractPacket.createInteractionPacket(target, sneaking, hand);
    }

    // ─── Recipe ──────────────────────────────────────────────────────────

    public static RecipeManager getRecipeManager(Level level) {
        return level.getRecipeManager();
    }

    public static RecipeManager getRecipeManager(ClientPacketListener connection) {
        return connection.getRecipeManager();
    }

    @SuppressWarnings("unchecked")
    public static Collection<?> iterateRecipes(RecipeManager mgr) {
        return mgr.getRecipes();
    }

    public static Recipe<?> getRecipeValue(Object recipeObj) {
        // 1.20.4: getRecipes() returns RecipeHolder<?> wrapping Recipe<?>
        if (recipeObj instanceof RecipeHolder<?> h) return h.value();
        if (recipeObj instanceof Recipe<?> r) return r;
        return null;
    }

    public static ResourceLocation getRecipeId(Object recipeObj) {
        // 1.20.4: RecipeHolder has id()
        if (recipeObj instanceof RecipeHolder<?> h) return h.id();
        return null;
    }

    public static Object getRecipeByKey(RecipeManager mgr, ResourceLocation key) {
        return mgr.byKey(key).orElse(null);
    }

    public static String getRecipeOutputItemId(Recipe<?> recipe) {
        // 1.20.1: getResultItem requires RegistryAccess, but null is safe for getId() purposes
        try {
            var mc = Minecraft.getInstance();
            var access = mc.level != null ? mc.level.registryAccess() : null;
            ItemStack result = recipe.getResultItem(access);
            return result.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        } catch (Exception e) {
            return "minecraft:air";
        }
    }

    // ─── Registry ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static <T> T registryGet(Object registry, ResourceLocation id) {
        if (registry instanceof net.minecraft.core.DefaultedRegistry<?> reg) {
            return (T) reg.get(id);
        }
        if (registry instanceof net.minecraft.core.Registry<?> reg) {
            return (T) reg.get(id);
        }
        // Fallback: interpret as string type name
        if (registry instanceof String registryType) {
            return switch (registryType) {
                case "block" -> (T) BuiltInRegistries.BLOCK.get(id);
                case "item" -> (T) BuiltInRegistries.ITEM.get(id);
                case "entity_type" -> (T) BuiltInRegistries.ENTITY_TYPE.get(id);
                case "mob_effect" -> (T) BuiltInRegistries.MOB_EFFECT.get(id);
                default -> null;
            };
        }
        return null;
    }

    // ─── ItemStack / NBT ─────────────────────────────────────────────────

    public static CompoundTag stackGetTag(ItemStack stack) {
        return stack.getTag();
    }

    public static void stackSetTag(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag);
    }

    public static boolean stackHasCustomHoverName(ItemStack stack) {
        return stack.hasCustomHoverName();
    }

    public static CompoundTag parseSnbt(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    // PLACEHOLDER_CONTINUE_3

    // ─── Item Properties ─────────────────────────────────────────────────

    public static int itemMaxStackSize(Item item) {
        return item.getMaxStackSize();
    }

    public static int itemMaxDamage(Item item) {
        return item.getMaxDamage();
    }

    public static boolean itemIsFireResistant(Item item) {
        return item.isFireResistant();
    }

    public static String itemRarityName(Item item) {
        Rarity r = item.getRarity(ItemStack.EMPTY);
        return r.name().toLowerCase(Locale.ROOT);
    }

    public static Object itemFoodProperties(Item item) {
        return item.getFoodProperties();
    }

    public static int foodNutrition(Object fp) {
        return ((FoodProperties) fp).getNutrition();
    }

    public static float foodSaturation(Object fp) {
        return ((FoodProperties) fp).getSaturationModifier();
    }

    public static boolean foodIsMeat(Object fp) {
        return ((FoodProperties) fp).isMeat();
    }

    public static boolean foodCanAlwaysEat(Object fp) {
        return ((FoodProperties) fp).canAlwaysEat();
    }

    // ─── Enchantment ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static Map<Object, Integer> enchantmentHelperGetEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> raw = EnchantmentHelper.getEnchantments(stack);
        return (Map<Object, Integer>) (Map<?, ?>) raw;
    }

    public static ResourceLocation enchantmentRegistryKey(Object ench) {
        if (ench instanceof Enchantment e) return BuiltInRegistries.ENCHANTMENT.getKey(e);
        return null;
    }

    public static Object enchantmentById(int id) {
        return BuiltInRegistries.ENCHANTMENT.byId(id);
    }

    public static ListTag getItemEnchantmentTagList(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        if (tag.contains("Enchantments", Tag.TAG_LIST)) {
            return tag.getList("Enchantments", Tag.TAG_COMPOUND);
        }
        if (tag.contains("StoredEnchantments", Tag.TAG_LIST)) {
            return tag.getList("StoredEnchantments", Tag.TAG_COMPOUND);
        }
        return null;
    }

    public static String tagGetString(CompoundTag tag, String key) {
        return tag.getString(key);
    }

    public static short tagGetShort(CompoundTag tag, String key) {
        return tag.getShort(key);
    }

    // ─── Mob Effects ─────────────────────────────────────────────────────

    public static ResourceLocation mobEffectIdFromInstance(MobEffectInstance inst) {
        return BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect());
    }

    public static void addMobEffect(LivingEntity entity, MobEffectInstance inst) {
        entity.addEffect(inst);
    }

    public static boolean addMobEffect(LivingEntity entity, ResourceLocation effectId, int durationTicks, int amplifier) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect == null) return false;
        entity.addEffect(new MobEffectInstance(effect, durationTicks, amplifier));
        return true;
    }

    // ─── Entity ──────────────────────────────────────────────────────────

    public static float entityDimensionsWidth(EntityDimensions dims) {
        return dims.width;
    }

    public static float entityDimensionsHeight(EntityDimensions dims) {
        return dims.height;
    }

    public static boolean entityDimensionsFixed(EntityDimensions dims) {
        return dims.fixed;
    }

    public static void entityMoveTo(Entity entity, double x, double y, double z, float yaw, float pitch) {
        entity.moveTo(x, y, z, yaw, pitch);
    }

    public static ServerLevel getServerLevel(Minecraft mc) {
        if (mc.getSingleplayerServer() == null) return null;
        return mc.getSingleplayerServer().overworld();
    }

    public static ServerLevel getServerLevel(net.minecraft.server.level.ServerPlayer sp) {
        return sp.serverLevel();
    }

    public static boolean isSameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItem(a, b);
    }

    public static boolean isSameItem(Item a, Item b) {
        return a == b;
    }

    // PLACEHOLDER_CONTINUE_4

    // ─── Level / World ───────────────────────────────────────────────────

    public static int getMinBuildHeight(Level level) {
        return level.getMinBuildHeight();
    }

    public static int getMaxBuildHeight(Level level) {
        return level.getMaxBuildHeight();
    }

    public static long getLevelDayTime(ClientLevel level) {
        return level.getDayTime();
    }

    public static float getDeltaFrameTime(Minecraft mc) {
        return mc.getDeltaFrameTime();
    }

    public static void leaveWorld(Minecraft mc) {
        mc.level = null;
    }

    public static void disconnectLevel(Minecraft mc) {
        mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen());
    }

    // ─── Screen / GUI ────────────────────────────────────────────────────

    public static boolean mouseClicked(Screen screen, double x, double y, int button) {
        return screen.mouseClicked(x, y, button);
    }

    public static boolean mouseReleased(Screen screen, double x, double y, int button) {
        return screen.mouseReleased(x, y, button);
    }

    public static boolean mouseDragged(Screen screen, double x, double y, int button, double dx, double dy) {
        return screen.mouseDragged(x, y, button, dx, dy);
    }

    public static boolean mouseScrolled(Screen screen, double x, double y, double delta) {
        return screen.mouseScrolled(x, y, 0, delta);
    }

    public static List<Component> getTooltipLines(ItemStack stack, Level level) {
        return stack.getTooltipLines(null, TooltipFlag.Default.ADVANCED);
    }

    public static List<Component> getTooltipLines(ItemStack stack, Level level, net.minecraft.world.entity.player.Player player, TooltipFlag flag) {
        return stack.getTooltipLines(player, flag);
    }

    public static String componentToJson(Component component) {
        return Component.Serializer.toJson(component);
    }

    // ─── Scoreboard ──────────────────────────────────────────────────────

    public static Objective getDisplayObjective(Scoreboard board, int slot) {
        DisplaySlot displaySlot = DisplaySlot.BY_ID.apply(slot);
        return displaySlot != null ? board.getDisplayObjective(displaySlot) : null;
    }

    public static boolean hasPlayerScore(Scoreboard board, String playerName, Objective objective) {
        ScoreHolder holder = ScoreHolder.forNameOnly(playerName);
        return board.getOrCreatePlayerScore(holder, objective) != null;
    }

    public static int getPlayerScore(Scoreboard board, String playerName, Objective objective) {
        ScoreHolder holder = ScoreHolder.forNameOnly(playerName);
        var scoreAccess = board.getOrCreatePlayerScore(holder, objective);
        return scoreAccess.get();
    }

    public static Collection<?> listPlayerScores(Scoreboard board, Objective objective) {
        return board.listPlayerScores(objective);
    }

    public static String getScoreOwner(Object scoreObj) {
        if (scoreObj instanceof PlayerScoreEntry entry) return entry.owner();
        return "";
    }

    public static int getScoreValue(Object scoreObj) {
        if (scoreObj instanceof PlayerScoreEntry entry) return entry.value();
        return 0;
    }

    // ─── Advancement ─────────────────────────────────────────────────────

    public static Object getAdvancementValue(Object advancementNode) {
        // 1.20.1: AdvancementNode wraps Advancement directly
        return advancementNode;
    }

    public static ResourceLocation getAdvancementId(Object advancement) {
        // Called from external code if needed; stub for 1.20.1
        return null;
    }

    // ─── BlockState ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static Set<Map.Entry<Property<?>, Comparable<?>>> getBlockStateEntries(BlockState state) {
        return state.getValues().entrySet();
    }

    // ─── Menu Data ───────────────────────────────────────────────────────

    public static int getMenuData(AbstractContainerMenu menu, String fieldName, int index) {
        // 1.20.1: ContainerData accessed via menu fields directly
        try {
            var field = menu.getClass().getField(fieldName);
            var data = (net.minecraft.world.inventory.ContainerData) field.get(menu);
            return data.get(index);
        } catch (Exception e) {
            // Fallback: try declared fields
            try {
                var field = menu.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                var data = (net.minecraft.world.inventory.ContainerData) field.get(menu);
                return data.get(index);
            } catch (Exception ex) {
                return 0;
            }
        }
    }
}


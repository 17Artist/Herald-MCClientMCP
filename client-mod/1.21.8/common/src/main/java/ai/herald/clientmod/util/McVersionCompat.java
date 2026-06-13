package ai.herald.clientmod.util;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
import it.unimi.dsi.fastutil.objects.Object2IntMap;

/**
 * Direct 1.20.4 API wrappers. No reflection needed for this version.
 * This class exists so that action code can compile unchanged.
 */
public final class McVersionCompat {
    private McVersionCompat() {}

    // ─── Inventory ───────────────────────────────────────────────────────

    public static int getSelectedSlot(Inventory inv) {
        return inv.getSelectedSlot();
    }

    public static void setSelectedSlot(Inventory inv, int slot) {
        inv.setSelectedSlot(slot);
    }

    public static List<ItemStack> getArmorItems(LivingEntity entity) {
        List<ItemStack> list = new ArrayList<>();
        list.add(entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
        list.add(entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
        list.add(entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        list.add(entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        return list;
    }

    public static List<ItemStack> getArmorItems(Inventory inv) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            list.add(inv.getItem(36 + i));
        }
        return list;
    }

    public static ItemStack getOffhandItem(LivingEntity entity) {
        return entity.getOffhandItem();
    }

    public static ItemStack getOffhandItem(Inventory inv) {
        return inv.getItem(40);
    }

    // ─── Packets ─────────────────────────────────────────────────────────

    public static void sendRotPacket(ClientPacketListener conn, float yaw, float pitch, boolean onGround) {
        conn.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround, false));
    }

    public static void sendStatusOnlyPacket(ClientPacketListener conn, boolean onGround) {
        conn.send(new ServerboundMovePlayerPacket.StatusOnly(onGround, false));
    }

    public static void sendMoveVehiclePacket(ClientPacketListener conn, Entity vehicle) {
        conn.send(ServerboundMoveVehiclePacket.fromEntity(vehicle));
    }

    public static void sendContainerClick(ClientPacketListener conn, int containerId, int revision,
                                          int slot, int button, ClickType clickType, ItemStack carried,
                                          Int2ObjectMap<ItemStack> changedSlots) {
        Int2ObjectMap<HashedStack> hashedChangedSlots = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<ItemStack> entry : changedSlots.int2ObjectEntrySet()) {
            hashedChangedSlots.put(entry.getIntKey(), entry.getValue().isEmpty() ? HashedStack.EMPTY : HashedStack.EMPTY);
        }
        HashedStack hashedCarried = carried.isEmpty() ? HashedStack.EMPTY : HashedStack.EMPTY;
        conn.send(new ServerboundContainerClickPacket(containerId, revision, (short) slot, (byte) button, clickType, hashedChangedSlots, hashedCarried));
    }

    public static void sendPickItemPacket(ClientPacketListener conn, int slot) {
        // ServerboundPickItemPacket removed in 1.21.4; replaced by ServerboundPickItemFromBlockPacket/ServerboundPickItemFromEntityPacket
        // This is a no-op stub; callers should use alternative logic if needed.
    }

    public static void sendSetBeaconPacket(ClientPacketListener conn, int primary, int secondary) {
        Optional<Holder<MobEffect>> p = primary >= 0
                ? BuiltInRegistries.MOB_EFFECT.get(primary).map(h -> (Holder<MobEffect>) h)
                : Optional.empty();
        Optional<Holder<MobEffect>> s = secondary >= 0
                ? BuiltInRegistries.MOB_EFFECT.get(secondary).map(h -> (Holder<MobEffect>) h)
                : Optional.empty();
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

    /**
     * In 1.21.4, RecipeManager is server-side only. The client receives recipes via
     * packets but doesn't have a full RecipeManager. For singleplayer, we can get it
     * from the integrated server. Returns null if not available.
     */
    public static RecipeManager getRecipeManager(Level level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return mc.getSingleplayerServer().getRecipeManager();
        }
        return null;
    }

    public static RecipeManager getRecipeManager(ClientPacketListener connection) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return mc.getSingleplayerServer().getRecipeManager();
        }
        return null;
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
        // 1.21.4: RecipeHolder.id() returns ResourceKey<Recipe<?>>, extract location
        if (recipeObj instanceof RecipeHolder<?> h) return h.id().location();
        return null;
    }

    public static Object getRecipeByKey(RecipeManager mgr, ResourceLocation key) {
        if (mgr == null) return null;
        ResourceKey<Recipe<?>> rk = ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, key);
        return mgr.byKey(rk).orElse(null);
    }

    public static String getRecipeOutputItemId(Recipe<?> recipe) {
        // 1.21.4: getResultItem() removed. Use display() API to get the result item.
        try {
            var displays = recipe.display();
            if (!displays.isEmpty()) {
                var resultSlot = displays.get(0).result();
                if (resultSlot instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay itemDisplay) {
                    return itemDisplay.item().unwrapKey()
                            .map(k -> k.location().toString())
                            .orElse("minecraft:air");
                }
                if (resultSlot instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay stackDisplay) {
                    return BuiltInRegistries.ITEM.getKey(stackDisplay.stack().getItem()).toString();
                }
                // Fallback: try resolveForFirstStack with empty context
                var mc = Minecraft.getInstance();
                if (mc.level != null) {
                    var ctx = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(mc.level);
                    var stack = resultSlot.resolveForFirstStack(ctx);
                    if (!stack.isEmpty()) {
                        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    }
                }
            }
            return "minecraft:air";
        } catch (Exception e) {
            return "minecraft:air";
        }
    }

    // ─── Registry ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static <T> T registryGet(Object registry, ResourceLocation id) {
        if (registry instanceof net.minecraft.core.DefaultedRegistry<?> reg) {
            return (T) reg.get(id).orElse(null);
        }
        if (registry instanceof net.minecraft.core.Registry<?> reg) {
            return (T) reg.get(id).orElse(null);
        }
        // Fallback: interpret as string type name
        if (registry instanceof String registryType) {
            return switch (registryType) {
                case "block" -> (T) BuiltInRegistries.BLOCK.get(id).orElse(null);
                case "item" -> (T) BuiltInRegistries.ITEM.get(id).orElse(null);
                case "entity_type" -> (T) BuiltInRegistries.ENTITY_TYPE.get(id).orElse(null);
                case "mob_effect" -> (T) BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
                default -> null;
            };
        }
        return null;
    }

    // ─── ItemStack / NBT ─────────────────────────────────────────────────

    public static CompoundTag stackGetTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : null;
    }

    public static void stackSetTag(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static boolean stackHasCustomHoverName(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_NAME);
    }

    public static CompoundTag parseSnbt(String snbt) {
        try {
            return TagParser.parseCompoundFully(snbt);
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    // PLACEHOLDER_CONTINUE_3

    // ─── Item Properties ─────────────────────────────────────────────────

    public static int itemMaxStackSize(Item item) {
        Integer val = item.components().get(DataComponents.MAX_STACK_SIZE);
        return val != null ? val : 64;
    }

    public static int itemMaxDamage(Item item) {
        Integer val = item.components().get(DataComponents.MAX_DAMAGE);
        return val != null ? val : 0;
    }

    public static boolean itemIsFireResistant(Item item) {
        return item.components().has(DataComponents.DAMAGE_RESISTANT);
    }

    public static String itemRarityName(Item item) {
        Rarity r = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        return r.name().toLowerCase(Locale.ROOT);
    }

    public static Object itemFoodProperties(Item item) {
        return item.components().get(DataComponents.FOOD);
    }

    public static int foodNutrition(Object fp) {
        return ((FoodProperties) fp).nutrition();
    }

    public static float foodSaturation(Object fp) {
        return ((FoodProperties) fp).saturation();
    }

    public static boolean foodIsMeat(Object fp) {
        // isMeat() was removed in 1.21; no longer a property of FoodProperties
        return false;
    }

    public static boolean foodCanAlwaysEat(Object fp) {
        return ((FoodProperties) fp).canAlwaysEat();
    }

    // ─── Enchantment ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static Map<Object, Integer> enchantmentHelperGetEnchantments(ItemStack stack) {
        ItemEnchantments enchantments = stack.getEnchantments();
        Map<Object, Integer> result = new LinkedHashMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            result.put(entry.getKey(), entry.getIntValue());
        }
        return result;
    }

    public static ResourceLocation enchantmentRegistryKey(Object ench) {
        if (ench instanceof Holder<?> holder) {
            return holder.unwrapKey().map(ResourceKey::location).orElse(null);
        }
        return null;
    }

    public static Object enchantmentById(int id) {
        // Enchantments are data-driven in 1.21.1; no BuiltInRegistries.ENCHANTMENT.
        // Cannot look up by numeric id without registry access.
        return null;
    }

    public static ListTag getItemEnchantmentTagList(ItemStack stack) {
        // In 1.21.1, enchantments are DataComponents, not NBT.
        // Return null; callers should use enchantmentHelperGetEnchantments() instead.
        return null;
    }

    public static String tagGetString(CompoundTag tag, String key) {
        return tag.getString(key).orElse("");
    }

    public static short tagGetShort(CompoundTag tag, String key) {
        return tag.getShort(key).orElse((short) 0);
    }

    // ─── Mob Effects ─────────────────────────────────────────────────────

    public static ResourceLocation mobEffectIdFromInstance(MobEffectInstance inst) {
        return BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value());
    }

    public static void addMobEffect(LivingEntity entity, MobEffectInstance inst) {
        entity.addEffect(inst);
    }

    public static boolean addMobEffect(LivingEntity entity, ResourceLocation effectId, int durationTicks, int amplifier) {
        var holder = BuiltInRegistries.MOB_EFFECT.get(effectId).orElse(null);
        if (holder == null) return false;
        entity.addEffect(new MobEffectInstance((Holder<MobEffect>) holder, durationTicks, amplifier));
        return true;
    }

    // ─── Entity ──────────────────────────────────────────────────────────

    public static float entityDimensionsWidth(EntityDimensions dims) {
        return dims.width();
    }

    public static float entityDimensionsHeight(EntityDimensions dims) {
        return dims.height();
    }

    public static boolean entityDimensionsFixed(EntityDimensions dims) {
        return dims.fixed();
    }

    public static void entityMoveTo(Entity entity, double x, double y, double z, float yaw, float pitch) {
        entity.snapTo(x, y, z, yaw, pitch);
    }

    public static ServerLevel getServerLevel(Minecraft mc) {
        if (mc.getSingleplayerServer() == null) return null;
        return mc.getSingleplayerServer().overworld();
    }

    public static ServerLevel getServerLevel(net.minecraft.server.level.ServerPlayer sp) {
        return (ServerLevel) sp.level();
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
        return level.dimensionType().minY();
    }

    public static int getMaxBuildHeight(Level level) {
        return level.dimensionType().minY() + level.dimensionType().height();
    }

    public static long getLevelDayTime(ClientLevel level) {
        return level.getDayTime();
    }

    public static float getDeltaFrameTime(Minecraft mc) {
        return mc.getDeltaTracker().getGameTimeDeltaTicks();
    }

    public static void leaveWorld(Minecraft mc) {
        mc.level = null;
    }

    public static void disconnectLevel(Minecraft mc) {
        mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), true);
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
        return stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.Default.ADVANCED);
    }

    public static List<Component> getTooltipLines(ItemStack stack, Level level, net.minecraft.world.entity.player.Player player, TooltipFlag flag) {
        Item.TooltipContext ctx = level != null ? Item.TooltipContext.of(level) : Item.TooltipContext.EMPTY;
        return stack.getTooltipLines(ctx, player, flag);
    }

    public static String componentToJson(Component component) {
        Minecraft mc = Minecraft.getInstance();
        var registryAccess = mc.level != null ? mc.level.registryAccess() : null;
        if (registryAccess != null) {
            try {
                var encoded = net.minecraft.network.chat.ComponentSerialization.CODEC
                        .encodeStart(registryAccess.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), component);
                return encoded.result().map(Object::toString).orElse(component.getString());
            } catch (Exception e) {
                return component.getString();
            }
        }
        // Fallback: use Component.getString() if no registry access available
        return component.getString();
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


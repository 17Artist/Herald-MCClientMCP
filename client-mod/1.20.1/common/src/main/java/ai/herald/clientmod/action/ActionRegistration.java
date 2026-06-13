package ai.herald.clientmod.action;

import ai.herald.clientmod.action.advanced.*;
import ai.herald.clientmod.action.block.*;
import ai.herald.clientmod.action.chat.*;
import ai.herald.clientmod.action.client.*;
import ai.herald.clientmod.action.composite.*;
import ai.herald.clientmod.action.container.*;
import ai.herald.clientmod.action.debug.*;
import ai.herald.clientmod.action.entity.*;
import ai.herald.clientmod.action.movement.*;
import ai.herald.clientmod.action.player.*;
import ai.herald.clientmod.action.query.*;
import ai.herald.clientmod.action.scan.*;
import ai.herald.clientmod.action.test.*;
import ai.herald.clientmod.action.automation.*;
import ai.herald.clientmod.action.modtest.*;
import ai.herald.clientmod.action.task.*;
import ai.herald.clientmod.action.world.*;
import ai.herald.clientmod.dispatcher.ActionRegistry;

/**
 * Bind every implemented action into the registry. One line per catalog id.
 * Catalog ids declared in {@link ai.herald.clientmod.catalog.ActionCatalog}
 * but not bound here will be flagged at WARN by {@code ActionRegistry.freeze}.
 *
 * <p>Stubs (action exists but the underlying packet/feature is unavailable in
 * Minecraft 1.20.1) are still registered — they return
 * {@link ai.herald.clientmod.protocol.ErrorCode#INVALID_PARAMS} with an
 * explanatory message at runtime.
 */
public final class ActionRegistration {

    private ActionRegistration() {}

    public static void registerAll(ActionRegistry r) {

        // === Movement (8) ===
        r.register("player_move",            new PlayerMoveAction());
        r.register("player_move_look",       new PlayerMoveLookAction());
        r.register("player_look",            new PlayerLookAction());
        r.register("player_on_ground",       new PlayerOnGroundAction());
        r.register("confirm_teleportation",  new ConfirmTeleportationAction());
        r.register("move_vehicle",           new MoveVehicleAction());
        r.register("paddle_boat",            new PaddleBoatAction());
        r.register("player_input",           new PlayerInputAction());

        // === Block interaction (5) ===
        r.register("dig_start",              new DigStartAction());
        r.register("dig_cancel",             new DigCancelAction());
        r.register("dig_finish",             new DigFinishAction());
        r.register("place_block",            new PlaceBlockAction());
        r.register("use_item",               new UseItemAction());

        // === Entity interaction (5) ===
        r.register("attack_entity",          new AttackEntityAction());
        r.register("interact_entity",        new InteractEntityAction());
        r.register("interact_entity_at",     new InteractEntityAtAction());
        SwingArmAction swingArm = new SwingArmAction();
        r.register("swing_arm",              swingArm);
        r.register("left_click",             swingArm);   // alias — same packet

        // === Container / GUI (12) ===
        r.register("click_slot",             new ClickSlotAction());
        r.register("click_button",           new ClickButtonAction());
        r.register("close_container",        new CloseContainerAction());
        r.register("set_carried_item",       new SetCarriedItemAction());
        r.register("creative_set_slot",      new CreativeSetSlotAction());
        r.register("pick_item",              new PickItemAction());
        r.register("pick_entity",            new PickEntityAction());
        r.register("pick_item_from_block",   new PickItemFromBlockAction());
        r.register("pick_item_from_entity",  new PickItemFromEntityAction());
        r.register("bundle_selected_slot",   new BundleSelectedSlotAction());   // 1.21+ stub
        r.register("slot_state_change",      new SlotStateChangeAction());      // 1.20.5+ stub
        r.register("hover_slot",             new HoverSlotAction());

        // === Player state (16) ===
        r.register("sneak_start",            new SneakStartAction());
        r.register("sneak_stop",             new SneakStopAction());
        r.register("sprint_start",           new SprintStartAction());
        r.register("sprint_stop",            new SprintStopAction());
        r.register("leave_bed",              new LeaveBedAction());
        r.register("horse_jump_start",       new HorseJumpStartAction());
        r.register("horse_jump_stop",        new HorseJumpStopAction());
        r.register("open_horse_inventory",   new OpenHorseInventoryAction());
        r.register("elytra_start",           new ElytraStartAction());
        r.register("drop_item",              new DropItemAction());
        r.register("drop_item_stack",        new DropItemStackAction());
        r.register("finish_using",           new FinishUsingAction());
        r.register("swap_hands",             new SwapHandsAction());
        r.register("perform_respawn",        new PerformRespawnAction());
        r.register("spectator_teleport",     new SpectatorTeleportAction());
        r.register("jump",                   new JumpAction());

        // === Chat / commands (3) ===
        r.register("chat_message",           new ChatMessageAction());
        r.register("chat_command",           new ChatCommandAction());
        r.register("click_chat_text",        new ClickChatTextAction());

        // === Client (10) ===
        r.register("client_information",     new ClientInformationAction());
        r.register("player_abilities",       new PlayerAbilitiesAction());
        r.register("resource_pack_response", new ResourcePackResponseAction());
        r.register("screenshot",             new ScreenshotAction());
        r.register("connect_to_server",      new ConnectToServerAction());      // stub
        r.register("close_screen",           new CloseScreenAction());
        r.register("open_inventory",         new OpenInventoryAction());
        r.register("create_world",           new CreateWorldAction());          // stub
        r.register("join_world",             new JoinWorldAction());            // stub
        r.register("leave_world",            new LeaveWorldAction());
        r.register("shutdown_client",        new ShutdownClientAction());

        // === Advanced (17) ===
        r.register("edit_book",              new EditBookAction());
        r.register("sign_book",              new SignBookAction());
        r.register("update_sign",            new UpdateSignAction());
        r.register("update_command_block",   new UpdateCommandBlockAction());
        r.register("update_command_block_minecart", new UpdateCommandBlockMinecartAction());
        r.register("update_structure_block", new UpdateStructureBlockAction());
        r.register("update_jigsaw_block",    new UpdateJigsawBlockAction());
        r.register("select_recipe",          new SelectRecipeAction());
        r.register("recipe_book_toggle",     new RecipeBookToggleAction());
        r.register("recipe_book_seen",       new RecipeBookSeenAction());
        r.register("query_entity_nbt",       new QueryEntityNbtAction());
        r.register("query_block_nbt",        new QueryBlockNbtAction());
        r.register("set_beacon_effect",      new SetBeaconEffectAction());
        r.register("rename_item",            new RenameItemAction());
        r.register("select_trade",           new SelectTradeAction());
        r.register("lock_difficulty",        new LockDifficultyAction());
        r.register("advancement_tab",        new AdvancementTabAction());

        // === Debug (6) ===
        r.register("custom_payload",         new CustomPayloadAction());
        r.register("tab_complete",           new TabCompleteAction());
        r.register("keep_alive",             new KeepAliveAction());
        r.register("pong",                   new PongAction());
        r.register("debug_sample_subscription", new DebugSampleAction());       // 1.20.5+ stub
        r.register("chunk_batch_received",   new ChunkBatchReceivedAction());   // 1.20.2+ stub

        // === Composite (14) ===
        r.register("look_at",                new LookAtAction());
        r.register("look_at_entity",         new LookAtEntityAction());
        r.register("break_block",            new BreakBlockAction());
        r.register("place_block_at",         new PlaceBlockAtAction());
        r.register("attack",                 new AttackAction());
        r.register("use",                    new UseAction());
        r.register("open_container",         new OpenContainerAction());
        r.register("container_transfer",     new ContainerTransferAction());
        r.register("drop_inventory",         new DropInventoryAction());
        r.register("pathfind_to",            new PathfindToAction());
        r.register("batch",                  new BatchAction());
        r.register("wait",                   new WaitAction());
        r.register("respawn",                new RespawnAction());
        r.register("craft_recipe",           new CraftRecipeAction());

        // === Query (17) ===
        r.register("query_held_item",        new QueryHeldItemAction());
        r.register("query_inventory_slot",   new QueryInventorySlotAction());
        r.register("query_chat_history",     new QueryChatHistoryAction());
        r.register("query_nearby_entities",  new QueryNearbyEntitiesAction());
        r.register("query_container_state",  new QueryContainerStateAction());
        r.register("query_player_state",     new QueryPlayerStateAction());
        r.register("query_container_slots",  new QueryContainerSlotsAction());
        r.register("query_active_effects",   new QueryActiveEffectsAction());
        r.register("query_block_state",      new QueryBlockStateAction());
        r.register("query_world_state",      new QueryWorldStateAction());
        r.register("query_tab_list",         new QueryTabListAction());
        r.register("query_scoreboard",       new QueryScoreboardAction());
        r.register("query_screen_state",     new QueryScreenStateAction());
        r.register("query_boss_bar",         new QueryBossBarAction());
        r.register("query_tooltip_state",    new QueryTooltipStateAction());
        r.register("query_chat_style",       new QueryChatStyleAction());
        r.register("query_slot_tooltip",     new QuerySlotTooltipAction());

        // === Navigation (2) ===
        r.register("look_at_block",          new LookAtBlockAction());
        r.register("navigate_to",            new NavigateToAction());

        // === P0-A: Map Scan (7) ===
        r.register("scan_area",              new ScanAreaAction());
        r.register("scan_column",            new ScanColumnAction());
        r.register("scan_surface",           new ScanSurfaceAction());
        r.register("scan_blocks_find",       new ScanBlocksFindAction());
        r.register("scan_blocks_count",      new ScanBlocksCountAction());
        r.register("query_minimap",          new QueryMinimapAction());
        r.register("query_cross_section",    new QueryCrossSectionAction());

        // === P0-B: Test Assertions (14) ===
        r.register("assert_block",           new AssertBlockAction());
        r.register("assert_block_not",       new AssertBlockNotAction());
        r.register("assert_inventory_contains", new AssertInventoryContainsAction());
        r.register("assert_inventory_empty", new AssertInventoryEmptyAction());
        r.register("assert_entity_exists",   new AssertEntityExistsAction());
        r.register("assert_entity_not_exists", new AssertEntityNotExistsAction());
        r.register("assert_player_at",       new AssertPlayerAtAction());
        r.register("assert_player_health",   new AssertPlayerHealthAction());
        r.register("assert_player_gamemode", new AssertPlayerGamemodeAction());
        r.register("assert_effect_active",   new AssertEffectActiveAction());
        r.register("assert_container_open",  new AssertContainerOpenAction());
        r.register("assert_dimension",       new AssertDimensionAction());
        r.register("assert_chat_contains",   new AssertChatContainsAction());
        r.register("assert_score",           new AssertScoreAction());

        // === P0-C: Test Wait/Check (9) ===
        r.register("wait_ticks",             new WaitTicksAction());
        r.register("wait_time",              new WaitTimeAction());
        r.register("wait_condition",         new WaitConditionAction());
        r.register("wait_event",             new WaitEventAction());
        r.register("wait_block_change",      new WaitBlockChangeAction());
        r.register("wait_entity_spawn",      new WaitEntitySpawnAction());
        r.register("wait_chat_message",      new WaitChatMessageAction());
        r.register("wait_container_open",    new WaitContainerOpenAction());
        r.register("wait_motion_stop",       new WaitMotionStopAction());

        // === P0-D: Test Control (4) ===
        r.register("test_begin",             new TestBeginAction());
        r.register("test_end",               new TestEndAction());
        r.register("test_checkpoint",        new TestCheckpointAction());
        r.register("test_log",               new TestLogAction());

        // === P0-E: Debug (12) ===
        r.register("debug_set_gamemode",     new DebugSetGamemodeAction());
        r.register("debug_set_time",         new DebugSetTimeAction());
        r.register("debug_set_weather",      new DebugSetWeatherAction());
        r.register("debug_give_item",        new DebugGiveItemAction());
        r.register("debug_teleport",         new DebugTeleportAction());
        r.register("debug_kill_entities",    new DebugKillEntitiesAction());
        r.register("debug_fill_blocks",      new DebugFillBlocksAction());
        r.register("debug_summon_entity",    new DebugSummonEntityAction());
        r.register("debug_effect_give",      new DebugEffectGiveAction());
        r.register("debug_clear_inventory",  new DebugClearInventoryAction());
        r.register("debug_set_health",       new DebugSetHealthAction());
        r.register("debug_set_hunger",       new DebugSetHungerAction());

        // === P0-F: Advanced Query (5) ===
        r.register("query_full_inventory",   new QueryFullInventoryAction());
        r.register("query_entity_detail",    new QueryEntityDetailAction());
        r.register("query_entities_in_area", new QueryEntitiesInAreaAction());
        r.register("query_performance",      new QueryPerformanceAction());
        r.register("query_light_level",      new QueryLightLevelAction());

        // === P1-A: Build (7) ===
        r.register("build_line",             new BuildLineAction());
        r.register("build_fill",             new BuildFillAction());
        r.register("build_replace",          new BuildReplaceAction());
        r.register("build_schematic",        new BuildSchematicAction());
        r.register("build_floor",            new BuildFloorAction());
        r.register("build_wall",             new BuildWallAction());
        r.register("build_clear_area",       new BuildClearAreaAction());

        // === P1-B: Combat (10) ===
        r.register("combat_target_nearest",  new CombatTargetNearestAction());
        r.register("combat_target_entity",   new CombatTargetEntityAction());
        r.register("combat_combo",           new CombatComboAction());
        r.register("combat_dodge",           new CombatDodgeAction());
        r.register("combat_block_shield",    new CombatBlockShieldAction());
        r.register("combat_shoot_bow",       new CombatShootBowAction());
        r.register("combat_throw_projectile", new CombatThrowProjectileAction());
        r.register("combat_eat",             new CombatEatAction());
        r.register("combat_flee",            new CombatFleeAction());
        r.register("query_threat_analysis",  new QueryThreatAnalysisAction());

        // === P1-C: GUI Testing (8) ===
        r.register("gui_query_current_screen", new GuiQueryCurrentScreenAction());
        r.register("gui_list_widgets",       new GuiListWidgetsAction());
        r.register("gui_click_widget",       new GuiClickWidgetAction());
        r.register("gui_type_text",          new GuiTypeTextAction());
        r.register("gui_scroll",             new GuiScrollAction());
        r.register("gui_drag",              new GuiDragAction());
        r.register("gui_screenshot_element", new GuiScreenshotElementAction());
        r.register("gui_query_slot_grid",    new GuiQuerySlotGridAction());

        // === P1-D: Registry (11) ===
        r.register("registry_list_blocks",   new RegistryListBlocksAction());
        r.register("registry_list_items",    new RegistryListItemsAction());
        r.register("registry_list_entities", new RegistryListEntitiesAction());
        r.register("registry_list_biomes",   new RegistryListBiomesAction());
        r.register("registry_list_enchantments", new RegistryListEnchantmentsAction());
        r.register("registry_list_effects",  new RegistryListEffectsAction());
        r.register("registry_list_recipes",  new RegistryListRecipesAction());
        r.register("registry_list_dimensions", new RegistryListDimensionsAction());
        r.register("registry_block_info",    new RegistryBlockInfoAction());
        r.register("registry_item_info",     new RegistryItemInfoAction());
        r.register("registry_entity_info",   new RegistryEntityInfoAction());

        // === P1-E: Snapshot (4) ===
        r.register("snapshot_create",        new SnapshotCreateAction());
        r.register("snapshot_compare",       new SnapshotCompareAction());
        r.register("snapshot_area",          new SnapshotAreaAction());
        r.register("snapshot_diff_blocks",   new SnapshotDiffBlocksAction());

        // === P1-F: Inventory Enhancement (5) ===
        r.register("find_item",              new FindItemAction());
        r.register("equip_item",             new EquipItemAction());
        r.register("query_item_location",    new QueryItemLocationAction());
        r.register("sort_inventory",         new SortInventoryAction());
        r.register("move_items",             new MoveItemsAction());

        // === P2-A: Mining/Harvesting (6) ===
        r.register("mine_vein",              new MineVeinAction());
        r.register("mine_area",              new MineAreaAction());
        r.register("mine_tunnel",            new MineTunnelAction());
        r.register("mine_staircase",         new MineStaircaseAction());
        r.register("harvest_crop",           new HarvestCropAction());
        r.register("harvest_area",           new HarvestAreaAction());

        // === P2-B: Farm (8) ===
        r.register("farm_plant",             new FarmPlantAction());
        r.register("farm_plant_area",        new FarmPlantAreaAction());
        r.register("farm_bone_meal",         new FarmBoneMealAction());
        r.register("farm_till",              new FarmTillAction());
        r.register("farm_till_area",         new FarmTillAreaAction());
        r.register("farm_irrigate",          new FarmIrrigateAction());
        r.register("query_crop_growth",      new QueryCropGrowthAction());
        r.register("query_crop_area",        new QueryCropAreaAction());

        // === P2-C: Trade (5) ===
        r.register("trade_open_villager",    new TradeOpenVillagerAction());
        r.register("trade_list_offers",      new TradeListOffersAction());
        r.register("trade_execute",          new TradeExecuteAction());
        r.register("trade_execute_all",      new TradeExecuteAllAction());
        r.register("query_villager_info",    new QueryVillagerInfoAction());

        // === P2-D: Enchant (5) ===
        r.register("enchant_open_table",     new EnchantOpenTableAction());
        r.register("enchant_query_options",  new EnchantQueryOptionsAction());
        r.register("enchant_select",         new EnchantSelectAction());
        r.register("enchant_apply_book",     new EnchantApplyBookAction());
        r.register("query_enchantments",     new QueryEnchantmentsAction());

        // === P2-E: Brew/Smelt (10) ===
        r.register("brew_open_stand",        new BrewOpenStandAction());
        r.register("brew_place_ingredient",  new BrewPlaceIngredientAction());
        r.register("brew_query_status",      new BrewQueryStatusAction());
        r.register("brew_wait_complete",     new BrewWaitCompleteAction());
        r.register("brew_collect",           new BrewCollectAction());
        r.register("smelt_open_furnace",     new SmeltOpenFurnaceAction());
        r.register("smelt_place_item",       new SmeltPlaceItemAction());
        r.register("smelt_place_fuel",       new SmeltPlaceFuelAction());
        r.register("smelt_query_progress",   new SmeltQueryProgressAction());
        r.register("smelt_collect_output",   new SmeltCollectOutputAction());

        // === P2-F: Redstone (8) ===
        r.register("redstone_query_power",   new RedstoneQueryPowerAction());
        r.register("redstone_scan_power",    new RedstoneScanPowerAction());
        r.register("redstone_toggle_lever",  new RedstoneToggleLeverAction());
        r.register("redstone_press_button",  new RedstonePressButtonAction());
        r.register("redstone_step_pressure_plate", new RedstoneStepPressurePlateAction());
        r.register("redstone_query_repeater", new RedstoneQueryRepeaterAction());
        r.register("redstone_set_repeater_delay", new RedstoneSetRepeaterDelayAction());
        r.register("redstone_query_comparator", new RedstoneQueryComparatorAction());

        // === P2-G: Navigation (4) ===
        r.register("fly_to",                 new FlyToAction());
        r.register("navigate_to_entity",     new NavigateToEntityAction());
        r.register("pathfind_plan",          new PathfindPlanAction());
        r.register("navigate_path",          new NavigatePathAction());

        // === P2-H: Performance (7) ===
        r.register("query_tps",              new QueryTpsAction());
        r.register("query_ping",             new QueryPingAction());
        r.register("query_client_fps",       new QueryClientFpsAction());
        r.register("query_chunk_load_time",  new QueryChunkLoadTimeAction());
        r.register("query_entity_count",     new QueryEntityCountAction());
        r.register("query_packet_stats",     new QueryPacketStatsAction());
        r.register("query_memory_detail",    new QueryMemoryDetailAction());

        // === P2-I: Sensing (9) ===
        r.register("listen_particles",       new ListenParticlesAction());
        r.register("listen_sounds",          new ListenSoundsAction());
        r.register("query_recent_particles", new QueryRecentParticlesAction());
        r.register("query_recent_sounds",    new QueryRecentSoundsAction());
        r.register("query_advancements",     new QueryAdvancementsAction());
        r.register("query_statistics",       new QueryStatisticsAction());
        r.register("query_damage_history",   new QueryDamageHistoryAction());
        r.register("query_armor_durability", new QueryArmorDurabilityAction());
        r.register("query_tool_durability",  new QueryToolDurabilityAction());

        // === P3-A: Task Management (6) ===
        r.register("task_create",            new TaskCreateAction());
        r.register("task_status",            new TaskStatusAction());
        r.register("task_cancel",            new TaskCancelAction());
        r.register("task_pause",             new TaskPauseAction());
        r.register("task_resume",            new TaskResumeAction());
        r.register("task_list",              new TaskListAction());

        // === P3-B: Goal Planning (7) ===
        r.register("goal_gather_items",      new GoalGatherItemsAction());
        r.register("goal_craft_item",        new GoalCraftItemAction());
        r.register("goal_reach_location",    new GoalReachLocationAction());
        r.register("goal_kill_entity",       new GoalKillEntityAction());
        r.register("goal_build_structure",   new GoalBuildStructureAction());
        r.register("goal_survive",           new GoalSurviveAction());
        r.register("query_recipe_dependencies", new QueryRecipeDependenciesAction());

        // === P3-C: Event System (5) ===
        r.register("event_subscribe",        new EventSubscribeAction());
        r.register("event_unsubscribe",      new EventUnsubscribeAction());
        r.register("event_history",          new EventHistoryAction());
        r.register("event_wait_custom",      new EventWaitCustomAction());
        r.register("event_emit_custom",      new EventEmitCustomAction());

        // === P3-D: Packet Capture (5) ===
        r.register("packet_capture_start",   new PacketCaptureStartAction());
        r.register("packet_capture_stop",    new PacketCaptureStopAction());
        r.register("packet_query_log",       new PacketQueryLogAction());
        r.register("packet_send_custom",     new PacketSendCustomAction());
        r.register("packet_intercept",       new PacketInterceptAction());

        // === P3-E: Config Management (4) ===
        r.register("config_reload",          new ConfigReloadAction());
        r.register("config_get",             new ConfigGetAction());
        r.register("config_set",             new ConfigSetAction());
        r.register("config_list",            new ConfigListAction());

        // === P3-F: World Control (7) ===
        r.register("world_create",           new WorldCreateAction());
        r.register("world_delete",           new WorldDeleteAction());
        r.register("world_list",             new WorldListAction());
        r.register("world_backup",           new WorldBackupAction());
        r.register("world_restore",          new WorldRestoreAction());
        r.register("world_set_gamerule",     new WorldSetGameruleAction());
        r.register("world_query_gamerules",  new WorldQueryGamerulesAction());
    }
}

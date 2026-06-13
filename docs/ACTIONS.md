# 客户端 MOD Actions 完整列表（308 个）

通过 MCP 工具 `mc_action` / `mc_query` 调用，或运行时使用 `mc_mod_list_actions` 动态获取。

---

## 移动（16）

| Action               | 说明          |
|----------------------|-------------|
| `fly_to`             | 创造模式飞行到目标坐标 |
| `jump`               | 跳跃          |
| `look_at`            | 看向指定坐标      |
| `look_at_block`      | 看向指定方块      |
| `look_at_entity`     | 看向指定实体      |
| `move_items`         | 批量移动物品      |
| `move_vehicle`       | 操控载具移动      |
| `navigate_path`      | 按路径节点行走     |
| `navigate_to`        | 寻路到目标坐标     |
| `navigate_to_entity` | 追踪实体        |
| `pathfind_plan`      | 计算路径（只返回节点） |
| `pathfind_to`        | 寻路并移动       |
| `sneak_start`        | 开始潜行        |
| `sneak_stop`         | 停止潜行        |
| `sprint_start`       | 开始疾跑        |
| `sprint_stop`        | 停止疾跑        |

## 方块（6）

| Action           | 说明         |
|------------------|------------|
| `break_block`    | 破坏方块（完整流程） |
| `dig_start`      | 开始挖掘       |
| `dig_finish`     | 完成挖掘       |
| `dig_cancel`     | 取消挖掘       |
| `place_block`    | 放置方块       |
| `place_block_at` | 在指定坐标放置方块  |

## 物品/背包（11）

| Action            | 说明        |
|-------------------|-----------|
| `craft_recipe`    | 合成配方      |
| `drop_inventory`  | 丢弃背包物品    |
| `drop_item`       | 丢弃单个物品    |
| `drop_item_stack` | 丢弃整组物品    |
| `equip_item`      | 装备物品到对应槽位 |
| `find_item`       | 搜索物品位置    |
| `select_recipe`   | 选择配方      |
| `select_trade`    | 选择交易      |
| `sort_inventory`  | 整理背包      |
| `swap_hands`      | 交换主副手     |
| `use_item`        | 使用物品      |

## 容器（6）

| Action                 | 说明      |
|------------------------|---------|
| `open_container`       | 打开容器    |
| `open_inventory`       | 打开背包    |
| `open_horse_inventory` | 打开马背包   |
| `close_container`      | 关闭容器    |
| `close_screen`         | 关闭当前界面  |
| `container_transfer`   | 容器间转移物品 |

## 聊天（2）

| Action         | 说明     |
|----------------|--------|
| `chat_message` | 发送聊天消息 |
| `chat_command` | 执行命令   |

## 扫描/地图（7）

| Action                | 说明                          |
|-----------------------|-----------------------------|
| `scan_area`           | 3D 方块数组（palette 压缩），max 32³ |
| `scan_column`         | 单列方块从底到顶                    |
| `scan_surface`        | 区域地表高度图 + 方块 ID             |
| `scan_blocks_find`    | 搜索特定方块返回坐标列表                |
| `scan_blocks_count`   | 统计各方块数量                     |
| `query_minimap`       | 以玩家为中心俯视图 2D                |
| `query_cross_section` | 指定 Y 高度横截面                  |

## 断言（14）

| Action                      | 说明        |
|-----------------------------|-----------|
| `assert_block`              | 断言位置是特定方块 |
| `assert_block_not`          | 断言位置不是某方块 |
| `assert_inventory_contains` | 断言背包含物品   |
| `assert_inventory_empty`    | 断言槽位为空    |
| `assert_entity_exists`      | 断言范围内存在实体 |
| `assert_entity_not_exists`  | 断言实体不存在   |
| `assert_player_at`          | 断言玩家位置    |
| `assert_player_health`      | 断言血量范围    |
| `assert_player_gamemode`    | 断言游戏模式    |
| `assert_effect_active`      | 断言药水效果    |
| `assert_container_open`     | 断言容器已打开   |
| `assert_dimension`          | 断言维度      |
| `assert_chat_contains`      | 断言聊天含文本   |
| `assert_score`              | 断言计分板分数   |

## 等待（9）

| Action                | 说明            |
|-----------------------|---------------|
| `wait_ticks`          | 等待 N 个游戏 tick |
| `wait_time`           | 等待 N 毫秒       |
| `wait_condition`      | 等待条件表达式满足     |
| `wait_event`          | 等待特定事件触发      |
| `wait_block_change`   | 等待方块变化        |
| `wait_entity_spawn`   | 等待实体出现        |
| `wait_chat_message`   | 等待匹配聊天消息      |
| `wait_container_open` | 等待容器打开        |
| `wait_motion_stop`    | 等待玩家停止移动      |

## 测试控制（4）

| Action            | 说明            |
|-------------------|---------------|
| `test_begin`      | 标记测试开始，初始化上下文 |
| `test_end`        | 标记测试结束，收集结果   |
| `test_checkpoint` | 记录检查点         |
| `test_log`        | 写入测试日志        |

## 调试（13）

| Action                      | 说明       |
|-----------------------------|----------|
| `debug_set_gamemode`        | 设置游戏模式   |
| `debug_set_time`            | 设置时间     |
| `debug_set_weather`         | 设置天气     |
| `debug_give_item`           | 给予物品     |
| `debug_teleport`            | 传送       |
| `debug_kill_entities`       | 清除实体     |
| `debug_fill_blocks`         | 填充方块     |
| `debug_summon_entity`       | 召唤实体     |
| `debug_effect_give`         | 给予药水效果   |
| `debug_clear_inventory`     | 清空背包     |
| `debug_set_health`          | 设置血量     |
| `debug_set_hunger`          | 设置饥饿值    |
| `debug_sample_subscription` | 调试事件订阅采样 |

## 建造（7）

| Action             | 说明       |
|--------------------|----------|
| `build_line`       | 两点间放一排方块 |
| `build_fill`       | 填充长方体    |
| `build_replace`    | 替换区域方块   |
| `build_schematic`  | 按蓝图建造    |
| `build_floor`      | 建造地板     |
| `build_wall`       | 建造墙壁     |
| `build_clear_area` | 清除区域方块   |

## 战斗（9）

| Action                    | 说明        |
|---------------------------|-----------|
| `combat_target_nearest`   | 攻击最近敌对实体  |
| `combat_target_entity`    | 锁定并攻击指定实体 |
| `combat_combo`            | 连击序列      |
| `combat_dodge`            | 闪避        |
| `combat_block_shield`     | 举盾格挡      |
| `combat_shoot_bow`        | 拉弓射击      |
| `combat_throw_projectile` | 投掷物品      |
| `combat_eat`              | 食用物品      |
| `combat_flee`             | 逃离威胁      |

## GUI 测试（8）

| Action                     | 说明              |
|----------------------------|-----------------|
| `gui_query_current_screen` | 当前 Screen 类型和内容 |
| `gui_list_widgets`         | 列出所有 UI 组件      |
| `gui_click_widget`         | 点击 GUI 组件       |
| `gui_type_text`            | 文本框输入           |
| `gui_scroll`               | 滚动列表            |
| `gui_drag`                 | 拖拽操作            |
| `gui_screenshot_element`   | 截图特定元素          |
| `gui_query_slot_grid`      | 查询容器 GUI 格子布局   |

## 注册表查询（11）

| Action                       | 说明       |
|------------------------------|----------|
| `registry_list_blocks`       | 所有注册方块   |
| `registry_list_items`        | 所有注册物品   |
| `registry_list_entities`     | 所有注册实体类型 |
| `registry_list_biomes`       | 所有生物群系   |
| `registry_list_enchantments` | 所有附魔     |
| `registry_list_effects`      | 所有药水效果   |
| `registry_list_recipes`      | 所有配方     |
| `registry_list_dimensions`   | 所有维度     |
| `registry_block_info`        | 方块详细注册信息 |
| `registry_item_info`         | 物品详细注册信息 |
| `registry_entity_info`       | 实体类型详细信息 |

## 快照（4）

| Action                 | 说明        |
|------------------------|-----------|
| `snapshot_create`      | 创建状态快照    |
| `snapshot_compare`     | 对比快照      |
| `snapshot_area`        | 区域方块快照    |
| `snapshot_diff_blocks` | 方块变化 diff |

## 采集/挖掘（6）

| Action           | 说明           |
|------------------|--------------|
| `mine_vein`      | 挖掘矿脉（连锁同类方块） |
| `mine_area`      | 挖掘区域（从上到下）   |
| `mine_tunnel`    | 挖隧道          |
| `mine_staircase` | 向下挖阶梯        |
| `harvest_crop`   | 收割成熟作物       |
| `harvest_area`   | 区域收割         |

## 农业（6）

| Action            | 说明   |
|-------------------|------|
| `farm_plant`      | 种植   |
| `farm_plant_area` | 区域种植 |
| `farm_bone_meal`  | 使用骨粉 |
| `farm_till`       | 锄地   |
| `farm_till_area`  | 区域锄地 |
| `farm_irrigate`   | 放水灌溉 |

## 交易（4）

| Action                | 说明     |
|-----------------------|--------|
| `trade_open_villager` | 打开村民交易 |
| `trade_list_offers`   | 列出交易选项 |
| `trade_execute`       | 执行交易   |
| `trade_execute_all`   | 买完某交易  |

## 附魔（5）

| Action                  | 说明     |
|-------------------------|--------|
| `enchant_open_table`    | 打开附魔台  |
| `enchant_query_options` | 查询附魔选项 |
| `enchant_select`        | 选择附魔   |
| `enchant_apply_book`    | 铁砧附魔书  |
| `query_enchantments`    | 查询物品附魔 |

## 酿造/熔炼（9）

| Action                  | 说明     |
|-------------------------|--------|
| `brew_open_stand`       | 打开酿造台  |
| `brew_place_ingredient` | 放置材料   |
| `brew_query_status`     | 酿造进度   |
| `brew_wait_complete`    | 等待酿造完成 |
| `brew_collect`          | 取出结果   |
| `smelt_open_furnace`    | 打开熔炉   |
| `smelt_place_item`      | 放入物品   |
| `smelt_place_fuel`      | 放入燃料   |
| `smelt_query_progress`  | 冶炼进度   |
| `smelt_collect_output`  | 取出成品   |

## 红石（8）

| Action                         | 说明      |
|--------------------------------|---------|
| `redstone_query_power`         | 红石信号强度  |
| `redstone_scan_power`          | 区域红石能量图 |
| `redstone_toggle_lever`        | 拉杆      |
| `redstone_press_button`        | 按钮      |
| `redstone_step_pressure_plate` | 踩压力板    |
| `redstone_query_repeater`      | 中继器状态   |
| `redstone_set_repeater_delay`  | 调中继器延迟  |
| `redstone_query_comparator`    | 比较器状态   |

## 性能分析（7）

| Action                  | 说明         |
|-------------------------|------------|
| `query_tps`             | 服务器 TPS 估算 |
| `query_ping`            | 网络延迟       |
| `query_client_fps`      | 客户端 FPS    |
| `query_memory_detail`   | JVM 内存使用   |
| `query_chunk_load_time` | chunk 加载耗时 |
| `query_entity_count`    | 已加载实体数     |
| `query_packet_stats`    | 网络包统计      |

## 感知（9）

| Action                   | 说明     |
|--------------------------|--------|
| `listen_particles`       | 监听粒子事件 |
| `listen_sounds`          | 监听音效事件 |
| `query_recent_particles` | 最近粒子   |
| `query_recent_sounds`    | 最近音效   |
| `query_advancements`     | 进度列表   |
| `query_statistics`       | 统计数据   |
| `query_damage_history`   | 伤害历史   |
| `query_armor_durability` | 装甲耐久   |
| `query_tool_durability`  | 工具耐久   |

## 任务/目标（13）

| Action                      | 说明         |
|-----------------------------|------------|
| `task_create`               | 创建多步任务     |
| `task_status`               | 查询任务状态     |
| `task_cancel`               | 取消任务       |
| `task_pause`                | 暂停任务       |
| `task_resume`               | 恢复任务       |
| `task_list`                 | 列出活跃任务     |
| `goal_gather_items`         | 目标：收集物品    |
| `goal_craft_item`           | 目标：合成（解依赖） |
| `goal_reach_location`       | 目标：到达位置    |
| `goal_kill_entity`          | 目标：击杀      |
| `goal_build_structure`      | 目标：建造      |
| `goal_survive`              | 目标：存活 N 秒  |
| `query_recipe_dependencies` | 合成依赖树      |

## 事件系统（5）

| Action              | 说明      |
|---------------------|---------|
| `event_subscribe`   | 订阅事件    |
| `event_unsubscribe` | 取消订阅    |
| `event_history`     | 事件历史    |
| `event_wait_custom` | 等待自定义事件 |
| `event_emit_custom` | 发射测试事件  |

## 网络包（5）

| Action                 | 说明     |
|------------------------|--------|
| `packet_capture_start` | 开始抓包   |
| `packet_capture_stop`  | 停止抓包   |
| `packet_query_log`     | 查询包日志  |
| `packet_send_custom`   | 发送自定义包 |
| `packet_intercept`     | 拦截/修改包 |

## 配置管理（4）

| Action          | 说明           |
|-----------------|--------------|
| `config_reload` | 触发 MOD 配置热重载 |
| `config_get`    | 读取配置项        |
| `config_set`    | 设置配置项        |
| `config_list`   | 列出可配置项       |

## 世界/环境（7）

| Action                  | 说明       |
|-------------------------|----------|
| `world_create`          | 创建世界     |
| `world_delete`          | 删除存档     |
| `world_list`            | 列出所有世界   |
| `world_backup`          | 备份世界     |
| `world_restore`         | 恢复备份     |
| `world_set_gamerule`    | 设置游戏规则   |
| `world_query_gamerules` | 查询所有游戏规则 |

## 查询（30）

| Action                   | 说明              |
|--------------------------|-----------------|
| `query_player_state`     | 玩家状态（位置/血量/饥饿等） |
| `query_world_state`      | 世界状态（时间/天气/维度）  |
| `query_nearby_entities`  | 附近实体列表          |
| `query_full_inventory`   | 全部 36+4+1 槽位    |
| `query_inventory_slot`   | 单个槽位详情          |
| `query_held_item`        | 手持物品            |
| `query_item_location`    | 物品位置查询          |
| `query_container_slots`  | 容器槽位内容          |
| `query_container_state`  | 容器状态            |
| `query_block_state`      | 方块状态            |
| `query_block_nbt`        | 方块 NBT 数据       |
| `query_entity_detail`    | 单个实体完整数据        |
| `query_entity_nbt`       | 实体 NBT 数据       |
| `query_entities_in_area` | AABB 区域实体搜索     |
| `query_active_effects`   | 当前药水效果          |
| `query_boss_bar`         | Boss 血条状态       |
| `query_chat_history`     | 聊天历史            |
| `query_chat_style`       | 聊天样式            |
| `query_scoreboard`       | 计分板             |
| `query_tab_list`         | Tab 列表          |
| `query_screen_state`     | 当前 Screen 状态    |
| `query_tooltip_state`    | 提示框状态           |
| `query_slot_tooltip`     | 槽位提示            |
| `query_performance`      | 综合性能指标          |
| `query_light_level`      | 位置光照等级          |
| `query_threat_analysis`  | 分析周围威胁          |
| `query_crop_growth`      | 查询作物生长阶段        |
| `query_crop_area`        | 区域作物状态          |
| `query_villager_info`    | 村民详细信息          |

## 底层协议/杂项（63）

客户端协议级别操作，由 MOD 直接映射 Minecraft 网络包和内部 API：

| Action                          | 说明            |
|---------------------------------|---------------|
| `attack`                        | 攻击            |
| `attack_entity`                 | 攻击实体          |
| `batch`                         | 批量执行多个 action |
| `click_slot`                    | 点击容器槽位        |
| `click_button`                  | 点击按钮          |
| `click_chat_text`               | 点击聊天文本        |
| `hover_slot`                    | 悬停槽位          |
| `interact_entity`               | 交互实体          |
| `interact_entity_at`            | 在指定位置交互实体     |
| `left_click`                    | 左键点击          |
| `use`                           | 使用（右键）        |
| `swing_arm`                     | 挥手            |
| `player_move`                   | 玩家移动包         |
| `player_move_look`              | 移动+视角包        |
| `player_look`                   | 视角包           |
| `player_on_ground`              | 地面状态包         |
| `player_input`                  | 玩家输入包         |
| `player_abilities`              | 玩家能力包         |
| `confirm_teleportation`         | 确认传送          |
| `keep_alive`                    | 心跳包           |
| `pong`                          | Pong 包        |
| `tab_complete`                  | Tab 补全        |
| `client_information`            | 客户端信息         |
| `custom_payload`                | 自定义 payload   |
| `resource_pack_response`        | 资源包响应         |
| `set_carried_item`              | 设置手持物品槽       |
| `creative_set_slot`             | 创造模式设置槽位      |
| `pick_item`                     | 拾取物品          |
| `pick_item_from_block`          | 从方块拾取物品       |
| `pick_item_from_entity`         | 从实体拾取物品       |
| `pick_entity`                   | 拾取实体          |
| `rename_item`                   | 重命名物品         |
| `edit_book`                     | 编辑书           |
| `sign_book`                     | 签署书           |
| `set_beacon_effect`             | 设置信标效果        |
| `update_sign`                   | 更新告示牌         |
| `update_command_block`          | 更新命令方块        |
| `update_command_block_minecart` | 更新命令方块矿车      |
| `update_structure_block`        | 更新结构方块        |
| `update_jigsaw_block`           | 更新拼图方块        |
| `lock_difficulty`               | 锁定难度          |
| `spectator_teleport`            | 旁观者传送         |
| `connect_to_server`             | 连接服务器         |
| `create_world`                  | 创建新世界（客户端菜单）  |
| `join_world`                    | 加入世界          |
| `leave_world`                   | 离开世界          |
| `respawn`                       | 重生            |
| `perform_respawn`               | 执行重生          |
| `leave_bed`                     | 离开床           |
| `screenshot`                    | 截图            |
| `shutdown_client`               | 关闭客户端         |
| `wait`                          | 等待（通用）        |
| `elytra_start`                  | 开始鞘翅飞行        |
| `finish_using`                  | 完成使用物品        |
| `horse_jump_start`              | 马跳跃开始         |
| `horse_jump_stop`               | 马跳跃结束         |
| `paddle_boat`                   | 划船            |
| `advancement_tab`               | 进度标签          |
| `recipe_book_seen`              | 配方书已读         |
| `recipe_book_toggle`            | 配方书切换         |
| `bundle_selected_slot`          | 收纳袋选中槽        |
| `chunk_batch_received`          | 区块批次确认        |
| `slot_state_change`             | 槽位状态变化        |

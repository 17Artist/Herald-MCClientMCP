<p align="center">
  <img src="apps/web/src/assets/logo.svg" width="72" height="72" alt="Herald MCClientMCP" />
</p>

<h1 align="center">Herald MCClientMCP</h1>

<p align="center">
  Minecraft 客户端 AI 自动化调试工具<br/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/rust-1.75+-orange?logo=rust" alt="Rust 1.75+" />
  <img src="https://img.shields.io/badge/MC-1.20.1-green?logo=minecraft" alt="MC 1.20.1" />
  <img src="https://img.shields.io/badge/MCP-2024--11--05-blue" alt="MCP Protocol" />
  <img src="https://img.shields.io/badge/license-GPL--3.0-purple" alt="License" />
</p>

---

## 用法

```powershell
.\herald-mcclient.exe --config config.toml
```

- 打开 `http://localhost:8686`：环境管家 → 启动 MC → AI 通过 MCP 控制客户端。
- 支持 Headless 模式：MC 窗口不可见，画面实时推流到浏览器，不抢鼠标不占屏幕。
- 请注意，该工具是为了便于 AI 工作流调试，请不要接入任何生产环境。它是个测试工具不是一个正式环境的组件。
- 安全相关不做任何保证和相关维护（由于本工具仅用于 AI 调试，大部分代码是 Vibe Coding，并没有经过过多人工审查）。

---

## 配置

```toml
[server]
listen = "127.0.0.1:8686"

[mc]
default_version = "1.20.1"
default_loader = "fabric"
heap_mb = 4096
offline_username = "HeraldDev"

[runtime]
mirror = "bmclapi"   # 国内用 bmclapi 加速下载
```

完整配置见 `config.example.toml`。

---

## MCP 工具（14 个）

| 组   | 工具                                                                                                                   |
|-----|----------------------------------------------------------------------------------------------------------------------|
| 环境  | `mc_env_probe` · `mc_env_install_java` · `mc_env_install_minecraft` · `mc_env_install_loader` · `mc_env_task_status` |
| 客户端 | `mc_client_status` · `mc_client_start` · `mc_client_stop` · `mc_client_logs`                                         |
| MOD | `mc_mod_status` · `mc_mod_list_actions`                                                                              |
| 操控  | `mc_action` · `mc_query`                                                                                             |

`mc_action` / `mc_query` 代理了客户端 MOD 注册的 308 个 action（移动、挖矿、GUI 操作、战斗、红石、附魔等）。

---

## 客户端 MOD 能力（308 actions）

覆盖移动、方块、物品、战斗、建造、GUI、红石、扫描、断言、调试、事件、网络包等 25 个类别。

完整列表见 [docs/ACTIONS.md](docs/ACTIONS.md)，或运行时通过 `mc_mod_list_actions` 动态获取。

---

## Headless 模式

启动时传 `headless: true`，MC 窗口隐藏，画面推流到 Web 面板：

```json
{ "name": "mc_client_start", "arguments": { "headless": true } }
```

浏览器打开 `http://localhost:8686` 即可看到实时游戏画面并操作。

---

## 从源码构建

```bash
cd apps/web && npm install && npm run build && cd ../..
cd client-mod/1.20.1 && ./gradlew :fabric:remapJar && cd ../..
cargo build --release -p herald-mcclient
```

产物：
- `target/release/herald-mcclient.exe`（~10 MB，含前端）
- `client-mod/1.20.1/fabric/build/libs/herald-client-fabric-0.1.0.jar`（MOD，自动注入）

---

## 文档

- [Actions 完整列表](docs/ACTIONS.md)
- [部署指南](docs/DEPLOY.md)

## 许可证

- [GPL-3.0-only](LICENSE)

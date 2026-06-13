//! MCP 工具定义与分发。

use serde_json::{json, Value};
use crate::state::AppState;

/// 工具定义列表（供 tools/list 返回）。
pub fn tool_definitions() -> Vec<Value> {
    vec![
        // ─── 环境层 ────────────────────────────
        tool_def("mc_env_probe", "探测主机环境：OS、已安装Java、MC版本缓存", json!({"type": "object"})),
        tool_def("mc_env_install_java", "下载 Adoptium Temurin JRE", json!({
            "type": "object",
            "properties": { "major": { "type": "integer", "description": "Java major version (17/21)" } },
            "required": ["major"]
        })),
        tool_def("mc_env_install_minecraft", "下载指定版本MC客户端", json!({
            "type": "object",
            "properties": { "version": { "type": "string", "description": "MC version, e.g. 1.20.1" } },
            "required": ["version"]
        })),
        tool_def("mc_env_install_loader", "安装Fabric/Forge/NeoForge", json!({
            "type": "object",
            "properties": {
                "loader": { "type": "string", "enum": ["fabric", "forge", "neoforge"] },
                "mc_version": { "type": "string" }
            },
            "required": ["loader", "mc_version"]
        })),
        tool_def("mc_env_task_status", "查询异步下载任务进度", json!({
            "type": "object",
            "properties": { "task_id": { "type": "string" } },
            "required": ["task_id"]
        })),

        // ─── 启动层 ────────────────────────────
        tool_def("mc_client_status", "客户端当前状态", json!({"type": "object"})),
        tool_def("mc_client_start", "启动MC客户端", json!({
            "type": "object",
            "properties": {
                "version": { "type": "string", "description": "MC version" },
                "loader": { "type": "string", "enum": ["fabric", "forge", "neoforge"] },
                "heap_mb": { "type": "integer" },
                "username": { "type": "string" }
            }
        })),
        tool_def("mc_client_stop", "停止MC客户端", json!({"type": "object"})),
        tool_def("mc_client_logs", "获取MC客户端日志", json!({
            "type": "object",
            "properties": { "tail": { "type": "integer", "default": 200 } }
        })),

        // ─── MOD通信层 ─────────────────────────
        tool_def("mc_mod_status", "检测Herald Client MOD是否在线", json!({"type": "object"})),
        tool_def("mc_mod_list_actions", "列出MOD支持的所有action", json!({"type": "object"})),

        // ─── 客户端操控层 ──────────────────────
        tool_def("mc_action", "执行单个action", json!({
            "type": "object",
            "properties": {
                "action": { "type": "string", "description": "Action ID" },
                "params": { "type": "object", "description": "Action parameters" }
            },
            "required": ["action"]
        })),
        tool_def("mc_query", "查询客户端状态", json!({
            "type": "object",
            "properties": {
                "query": { "type": "string", "description": "Query type: player_info, inventory, nearby_entities, etc." }
            },
            "required": ["query"]
        })),
    ]
}

/// 分发工具调用。
pub async fn dispatch(state: &AppState, name: &str, args: Value) -> Result<Value, (i32, String)> {
    match name {
        "mc_env_probe" => env_probe(state).await,
        "mc_env_install_java" => env_install_java(state, &args).await,
        "mc_env_install_minecraft" => env_install_mc(state, &args).await,
        "mc_env_install_loader" => env_install_loader(state, &args).await,
        "mc_env_task_status" => Ok(json!([{"type":"text","text":"not implemented yet"}])),
        "mc_client_status" => client_status(state).await,
        "mc_client_start" => client_start(state, &args).await,
        "mc_client_stop" => client_stop(state).await,
        "mc_client_logs" => client_logs(state, &args).await,
        "mc_mod_status" => mod_status(state).await,
        "mc_mod_list_actions" => mod_list_actions(state).await,
        "mc_action" => action_call(state, &args).await,
        "mc_query" => query_call(state, &args).await,
        _ => Err((-32601, format!("Unknown tool: {}", name))),
    }
}

// ─── Tool Implementations ──────────────────────────────────────────

async fn env_probe(state: &AppState) -> Result<Value, (i32, String)> {
    let javas = state.java_manager.probe();
    let java_list: Vec<Value> = javas.iter().map(|j| json!({
        "path": j.path.display().to_string(),
        "major_version": j.major_version,
        "vendor": j.vendor,
    })).collect();

    let game_dir = state.config.game_dir();
    let mc_installed = game_dir.join("versions").exists();

    Ok(json!([{
        "type": "text",
        "text": serde_json::to_string_pretty(&json!({
            "os": std::env::consts::OS,
            "arch": std::env::consts::ARCH,
            "java_installations": java_list,
            "game_dir": game_dir.display().to_string(),
            "mc_versions_installed": mc_installed,
        })).unwrap_or_default()
    }]))
}

async fn env_install_java(state: &AppState, args: &Value) -> Result<Value, (i32, String)> {
    let major = args["major"].as_u64().unwrap_or(17) as u32;
    match state.java_manager.install(major).await {
        Ok(install) => Ok(json!([{
            "type": "text",
            "text": format!("Java {} installed at {}", major, install.path.display())
        }])),
        Err(e) => Err((-32000, format!("Failed to install Java: {}", e))),
    }
}

async fn env_install_mc(state: &AppState, args: &Value) -> Result<Value, (i32, String)> {
    let version = args["version"].as_str().unwrap_or("1.20.1");
    match state.mc_downloader.download_version(version).await {
        Ok(()) => Ok(json!([{ "type": "text", "text": format!("MC {} downloaded", version) }])),
        Err(e) => Err((-32000, format!("Failed to download MC: {}", e))),
    }
}

async fn env_install_loader(state: &AppState, args: &Value) -> Result<Value, (i32, String)> {
    let loader_str = args["loader"].as_str()
        .ok_or((-32602, "Missing 'loader' parameter".to_string()))?;
    let mc_version = args["mc_version"].as_str()
        .unwrap_or(&state.config.mc.default_version);

    let loader_type = herald_mcclient_env::loader::LoaderType::from_str(loader_str)
        .ok_or((-32602, format!("Invalid loader: {}", loader_str)))?;

    let java = state.java_manager.find_best_java(17)
        .ok_or((-32000, "No Java 17+ found".to_string()))?;

    let game_dir = state.config.game_dir();
    let installer = herald_mcclient_env::LoaderInstaller::new(
        game_dir, java.path, state.config.runtime.mirror.clone()
    );

    match installer.install(loader_type, mc_version).await {
        Ok(()) => Ok(json!([{
            "type": "text",
            "text": format!("{} installed for MC {}", loader_str, mc_version)
        }])),
        Err(e) => Err((-32000, format!("Failed to install {}: {}", loader_str, e))),
    }
}

async fn client_status(state: &AppState) -> Result<Value, (i32, String)> {
    let info = state.client_process.info();
    Ok(json!([{
        "type": "text",
        "text": serde_json::to_string_pretty(&info).unwrap_or_default()
    }]))
}

async fn client_start(state: &AppState, args: &Value) -> Result<Value, (i32, String)> {
    let version = args["version"].as_str()
        .unwrap_or(&state.config.mc.default_version).to_string();
    let loader = args["loader"].as_str()
        .or(Some(state.config.mc.default_loader.as_str()))
        .map(|s| s.to_string());
    let heap_mb = args["heap_mb"].as_u64().unwrap_or(state.config.mc.heap_mb as u64) as u32;
    let username = args["username"].as_str()
        .unwrap_or(&state.config.mc.offline_username).to_string();

    // 找 Java
    let java = state.java_manager.find_best_java(17)
        .ok_or((-32000, "No Java 17+ found. Use mc_env_install_java first.".to_string()))?;

    // 自动注入 Herald MOD jar 到 mods/
    let game_dir = state.config.game_dir();
    inject_herald_mod(&game_dir, loader.as_deref(), &version)
        .map_err(|e| (-32000, format!("MOD injection failed: {}", e)))?;

    // 确保 Fabric API 就位（同步等待下载完成）
    if loader.as_deref() == Some("fabric") || loader.is_none() {
        ensure_fabric_api_blocking(&game_dir.join("mods")).await
            .map_err(|e| (-32000, format!("Fabric API download failed: {}", e)))?;
    }

    let launch_args = herald_mcclient_launcher::args::LaunchArgs {
        java_path: java.path,
        game_dir,
        version,
        loader,
        heap_mb,
        username,
        headless: args["headless"].as_bool().unwrap_or(false),
    };

    state.client_process.start(launch_args).await
        .map_err(|e| (-32000, format!("Failed to start: {}", e)))?;

    Ok(json!([{ "type": "text", "text": "MC client started. Herald MOD injected." }]))
}

/// 将 Herald Client MOD jar 注入到 mods/ 目录。
fn inject_herald_mod(game_dir: &std::path::Path, loader: Option<&str>, version: &str) -> anyhow::Result<()> {
    let mods_dir = game_dir.join("mods");
    std::fs::create_dir_all(&mods_dir)?;

    // 确定使用的 jar 文件名
    let loader_suffix = match loader {
        Some("forge") => "forge",
        Some("neoforge") => "neoforge",
        _ => "fabric",
    };
    let jar_name = format!("herald-client-{}-0.1.0.jar", loader_suffix);

    // Remove other loader's herald JARs to avoid conflicts
    for other in &["fabric", "forge", "neoforge"] {
        if *other == loader_suffix { continue; }
        let other_jar = mods_dir.join(format!("herald-client-{}-0.1.0.jar", other));
        if other_jar.exists() {
            let _ = std::fs::remove_file(&other_jar);
        }
    }
    // Remove Fabric API when not using Fabric
    if loader_suffix != "fabric" {
        for entry in std::fs::read_dir(&mods_dir).into_iter().flatten() {
            if let Ok(e) = entry {
                let name = e.file_name().to_string_lossy().to_string();
                if name.starts_with("fabric-api") && name.ends_with(".jar") {
                    let _ = std::fs::remove_file(e.path());
                }
            }
        }
    }

    // 查找 jar 文件：先在 exe 同目录下找，再在 client-mod/<version>/build/libs/ 找
    let exe_dir = std::env::current_exe()
        .ok()
        .and_then(|p| p.parent().map(|d| d.to_path_buf()))
        .unwrap_or_else(|| std::env::current_dir().unwrap_or_default());

    let candidates = [
        exe_dir.join(&jar_name),
        exe_dir.join("mods").join(&jar_name),
        std::env::current_dir().unwrap_or_default().join("client-mod").join(&version).join(loader_suffix).join("build").join("libs").join(&jar_name),
    ];

    let source = candidates.iter().find(|p| p.exists());
    if let Some(src) = source {
        let dest = mods_dir.join(&jar_name);
        if !dest.exists() || file_size(src) != file_size(&dest) {
            std::fs::copy(src, &dest)?;
            tracing::info!("Injected {} -> {:?}", jar_name, dest);
        }
    } else {
        tracing::warn!("Herald MOD jar not found ({}), skipping injection. Searched: {:?}", jar_name, candidates);
    }

    // 如果是 Fabric，确保 Fabric API 也在 mods/ 目录下
    if loader_suffix == "fabric" {
        // Fabric API 的下载已在 client_start 中以 await 方式处理
    }

    Ok(())
}

/// 确保 Fabric API 在 mods/ 中（已存在则跳过，不存在则下载并等待完成）。
async fn ensure_fabric_api_blocking(mods_dir: &std::path::Path) -> anyhow::Result<()> {
    // 检查是否已有任何 fabric-api jar
    if let Ok(entries) = std::fs::read_dir(mods_dir) {
        for entry in entries.flatten() {
            let name = entry.file_name().to_string_lossy().to_string();
            if name.starts_with("fabric-api") && name.ends_with(".jar") {
                // 确认文件大小合理（>100KB）
                if let Ok(meta) = entry.metadata() {
                    if meta.len() > 100_000 {
                        return Ok(());
                    }
                }
            }
        }
    }

    // 下载 Fabric API
    let dest = mods_dir.join("fabric-api-0.92.5+1.20.1.jar");
    let url = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.92.5%2B1.20.1/fabric-api-0.92.5%2B1.20.1.jar";
    tracing::info!("Downloading Fabric API (required dependency)...");
    herald_mcclient_env::download::download_file(url, &dest, None).await?;
    tracing::info!("Fabric API ready at {:?}", dest);
    Ok(())
}

fn file_size(p: &std::path::Path) -> u64 {
    std::fs::metadata(p).map(|m| m.len()).unwrap_or(0)
}

async fn client_stop(state: &AppState) -> Result<Value, (i32, String)> {
    // 1. 先通过 MOD /shutdown 端点优雅关闭
    if let Some((port, token)) = state.mod_bridge.read_credentials_if_available() {
        let url = format!("http://127.0.0.1:{}/shutdown", port);
        let client = reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(5))
            .build()
            .unwrap_or_default();
        let _ = client.post(&url).bearer_auth(&token).send().await;
        // 等待 MC 退出（MOD 会在 3 秒后 halt）
        tokio::time::sleep(std::time::Duration::from_secs(5)).await;
    }

    // 2. 无论如何都 force stop 兜底
    state.client_process.stop().await
        .map_err(|e| (-32000, format!("Failed to stop: {}", e)))?;
    Ok(json!([{ "type": "text", "text": "MC client stopped" }]))
}

async fn client_logs(state: &AppState, args: &Value) -> Result<Value, (i32, String)> {
    let tail = args["tail"].as_u64().unwrap_or(200) as usize;
    let logs = state.client_process.logs(tail);
    let text = logs.iter()
        .map(|l| format!("[{}] {}", l.ts, l.text))
        .collect::<Vec<_>>()
        .join("\n");
    Ok(json!([{ "type": "text", "text": text }]))
}

async fn mod_status(state: &AppState) -> Result<Value, (i32, String)> {
    let status = state.mod_bridge.ping().await;
    Ok(json!([{
        "type": "text",
        "text": serde_json::to_string_pretty(&status).unwrap_or_default()
    }]))
}

async fn mod_list_actions(state: &AppState) -> Result<Value, (i32, String)> {
    match state.mod_bridge.list_actions().await {
        Ok(actions) => Ok(json!([{
            "type": "text",
            "text": serde_json::to_string_pretty(&actions).unwrap_or_default()
        }])),
        Err(e) => Err((-32000, format!("MOD not reachable: {}", e))),
    }
}

async fn action_call(state: &AppState, args: &Value) -> Result<Value, (i32, String)> {
    let action = args["action"].as_str()
        .ok_or((-32602, "Missing 'action' parameter".to_string()))?;
    let params = args.get("params").cloned().unwrap_or(json!({}));

    match state.mod_bridge.call_action(action, params).await {
        Ok(result) => Ok(json!([{
            "type": "text",
            "text": serde_json::to_string_pretty(&result).unwrap_or_default()
        }])),
        Err(e) => Err((-32000, format!("Action failed: {}", e))),
    }
}

async fn query_call(state: &AppState, args: &Value) -> Result<Value, (i32, String)> {
    let query = args["query"].as_str().unwrap_or("player_state");
    // 映射 query 类型到 MOD 的 action 名称
    let action_name = if query.starts_with("query_") {
        query.to_string()
    } else {
        format!("query_{}", query)
    };
    let params = json!({});
    match state.mod_bridge.call_action(&action_name, params).await {
        Ok(result) => Ok(json!([{
            "type": "text",
            "text": serde_json::to_string_pretty(&result).unwrap_or_default()
        }])),
        Err(e) => Err((-32000, format!("Query failed: {}", e))),
    }
}

// ─── Helper ────────────────────────────────────────────────────────

fn tool_def(name: &str, desc: &str, schema: Value) -> Value {
    json!({
        "name": name,
        "description": desc,
        "inputSchema": schema
    })
}

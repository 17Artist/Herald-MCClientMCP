use axum::{extract::State, routing::{get, post}, Json, Router};
use serde::Deserialize;
use crate::state::AppState;

/// REST API 路由（供 Web 前端使用）。
pub fn api_routes() -> Router<AppState> {
    Router::new()
        // 全局状态
        .route("/api/status", get(get_status))
        // 环境管家
        .route("/api/env/java", get(get_java_list))
        .route("/api/env/java/install", post(install_java))
        .route("/api/env/mc/status", get(get_mc_status))
        .route("/api/env/mc/install", post(install_mc))
        .route("/api/env/loader/install", post(install_loader))
        // 客户端
        .route("/api/client/info", get(get_client_info))
        .route("/api/client/logs", get(get_client_logs))
        .route("/api/client/start", post(start_client))
        .route("/api/client/stop", post(stop_client))
        // MOD
        .route("/api/mod/status", get(get_mod_status))
        .route("/api/mod/actions", get(get_mod_actions))
        .route("/api/mod/list", get(get_mod_files))
}

// ─── Status ─────────────────────────────────────────────────

async fn get_status(State(state): State<AppState>) -> Json<serde_json::Value> {
    let client_info = state.client_process.info();
    let mod_status = state.mod_bridge.ping().await;
    let game_dir = state.config.game_dir();
    Json(serde_json::json!({
        "client": client_info,
        "mod": mod_status,
        "config": {
            "game_dir": game_dir.display().to_string(),
            "mc_version": state.config.mc.default_version,
            "loader": state.config.mc.default_loader,
            "heap_mb": state.config.mc.heap_mb,
            "username": state.config.mc.offline_username,
        }
    }))
}

// ─── Environment ────────────────────────────────────────────

async fn get_java_list(State(state): State<AppState>) -> Json<serde_json::Value> {
    let javas = state.java_manager.probe();
    let list: Vec<serde_json::Value> = javas.iter().map(|j| serde_json::json!({
        "path": j.path.display().to_string(),
        "major_version": j.major_version,
        "vendor": j.vendor,
    })).collect();
    Json(serde_json::json!({ "javas": list }))
}

#[derive(Deserialize)]
struct InstallJavaReq { major: Option<u32> }

async fn install_java(
    State(state): State<AppState>,
    Json(req): Json<InstallJavaReq>,
) -> Json<serde_json::Value> {
    let major = req.major.unwrap_or(17);
    match state.java_manager.install(major).await {
        Ok(info) => Json(serde_json::json!({
            "ok": true,
            "path": info.path.display().to_string(),
            "major_version": info.major_version,
        })),
        Err(e) => Json(serde_json::json!({ "ok": false, "error": e.to_string() })),
    }
}

async fn get_mc_status(State(state): State<AppState>) -> Json<serde_json::Value> {
    let game_dir = state.config.game_dir();
    let version = &state.config.mc.default_version;
    let installed = state.mc_downloader.is_version_installed(version);
    let versions_dir = game_dir.join("versions");

    // 列出已安装的版本
    let mut installed_versions: Vec<String> = Vec::new();
    if let Ok(entries) = std::fs::read_dir(&versions_dir) {
        for entry in entries.flatten() {
            if entry.path().is_dir() {
                installed_versions.push(entry.file_name().to_string_lossy().to_string());
            }
        }
    }
    installed_versions.sort();

    Json(serde_json::json!({
        "default_version": version,
        "default_installed": installed,
        "installed_versions": installed_versions,
        "game_dir": game_dir.display().to_string(),
    }))
}

#[derive(Deserialize)]
struct InstallMcReq { version: Option<String> }

async fn install_mc(
    State(state): State<AppState>,
    Json(req): Json<InstallMcReq>,
) -> Json<serde_json::Value> {
    let version = req.version.unwrap_or_else(|| state.config.mc.default_version.clone());
    match state.mc_downloader.download_version(&version).await {
        Ok(()) => Json(serde_json::json!({ "ok": true, "version": version })),
        Err(e) => Json(serde_json::json!({ "ok": false, "error": e.to_string() })),
    }
}

#[derive(Deserialize)]
struct InstallLoaderReq {
    loader: String,
    mc_version: Option<String>,
}

async fn install_loader(
    State(state): State<AppState>,
    Json(req): Json<InstallLoaderReq>,
) -> Json<serde_json::Value> {
    let mc_version = req.mc_version.unwrap_or_else(|| state.config.mc.default_version.clone());

    let loader_type = match herald_mcclient_env::loader::LoaderType::from_str(&req.loader) {
        Some(l) => l,
        None => return Json(serde_json::json!({ "ok": false, "error": "Invalid loader type" })),
    };

    // 找 Java
    let java = match state.java_manager.find_best_java(17) {
        Some(j) => j,
        None => return Json(serde_json::json!({ "ok": false, "error": "No Java 17+ found" })),
    };

    let game_dir = state.config.game_dir();
    let installer = herald_mcclient_env::LoaderInstaller::new(
        game_dir, java.path, state.config.runtime.mirror.clone()
    );

    match installer.install(loader_type, &mc_version).await {
        Ok(()) => Json(serde_json::json!({ "ok": true, "loader": req.loader, "mc_version": mc_version })),
        Err(e) => Json(serde_json::json!({ "ok": false, "error": e.to_string() })),
    }
}

// ─── Client ─────────────────────────────────────────────────

async fn get_client_info(State(state): State<AppState>) -> Json<serde_json::Value> {
    Json(serde_json::to_value(state.client_process.info()).unwrap_or_default())
}

async fn get_client_logs(State(state): State<AppState>) -> Json<serde_json::Value> {
    let logs = state.client_process.logs(500);
    Json(serde_json::json!({ "logs": logs }))
}

#[derive(Deserialize)]
struct StartClientReq {
    version: Option<String>,
    loader: Option<String>,
    heap_mb: Option<u32>,
    username: Option<String>,
    headless: Option<bool>,
}

async fn start_client(
    State(state): State<AppState>,
    Json(req): Json<StartClientReq>,
) -> Json<serde_json::Value> {
    let version = req.version.unwrap_or_else(|| state.config.mc.default_version.clone());
    let loader = req.loader.or_else(|| Some(state.config.mc.default_loader.clone()));
    let heap_mb = req.heap_mb.unwrap_or(state.config.mc.heap_mb);
    let username = req.username.unwrap_or_else(|| state.config.mc.offline_username.clone());

    let java = match state.java_manager.find_best_java(17) {
        Some(j) => j,
        None => return Json(serde_json::json!({ "ok": false, "error": "No Java 17+ found" })),
    };

    let launch_args = herald_mcclient_launcher::args::LaunchArgs {
        java_path: java.path,
        game_dir: state.config.game_dir(),
        version,
        loader,
        heap_mb,
        username,
        headless: req.headless.unwrap_or(false),
    };

    match state.client_process.start(launch_args).await {
        Ok(()) => {
            let info = state.client_process.info();
            Json(serde_json::json!({ "ok": true, "info": info }))
        }
        Err(e) => Json(serde_json::json!({ "ok": false, "error": e.to_string() })),
    }
}

async fn stop_client(State(state): State<AppState>) -> Json<serde_json::Value> {
    match state.client_process.stop().await {
        Ok(()) => Json(serde_json::json!({ "ok": true })),
        Err(e) => Json(serde_json::json!({ "ok": false, "error": e.to_string() })),
    }
}

// ─── MOD ────────────────────────────────────────────────────

async fn get_mod_status(State(state): State<AppState>) -> Json<serde_json::Value> {
    let status = state.mod_bridge.ping().await;
    Json(serde_json::to_value(status).unwrap_or_default())
}

async fn get_mod_actions(State(state): State<AppState>) -> Json<serde_json::Value> {
    match state.mod_bridge.list_actions().await {
        Ok(actions) => Json(serde_json::json!({ "actions": actions })),
        Err(e) => Json(serde_json::json!({ "actions": [], "error": e.to_string() })),
    }
}

async fn get_mod_files(State(state): State<AppState>) -> Json<serde_json::Value> {
    let mods_dir = state.config.game_dir().join("mods");
    let mut files: Vec<serde_json::Value> = Vec::new();

    if let Ok(entries) = std::fs::read_dir(&mods_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            if let Some(name) = path.file_name() {
                let meta = std::fs::metadata(&path).ok();
                files.push(serde_json::json!({
                    "name": name.to_string_lossy(),
                    "size": meta.as_ref().map(|m| m.len()).unwrap_or(0),
                    "is_herald": name.to_string_lossy().contains("herald-client"),
                }));
            }
        }
    }

    Json(serde_json::json!({ "mods_dir": mods_dir.display().to_string(), "files": files }))
}

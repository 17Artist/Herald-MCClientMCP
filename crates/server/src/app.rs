use std::sync::Arc;
use axum::{routing::{get, post}, Router};
use tower_http::cors::CorsLayer;
use tracing::info;

use herald_mcclient_bridge::ModBridge;
use herald_mcclient_core::AppConfig;
use herald_mcclient_env::{JavaManager, MinecraftDownloader, TaskManager};
use herald_mcclient_launcher::ClientProcess;

use crate::mcp;
use crate::routes;
use crate::state::{ActivityBus, AppState};
use crate::static_assets;
use crate::ws;

/// 构建并启动 HTTP 服务。
pub async fn run(config: AppConfig) -> anyhow::Result<()> {
    let game_dir = config.game_dir();
    let port_range = config.mod_port_range();

    // 创建各子系统
    let java_cache = game_dir.parent()
        .unwrap_or(&game_dir)
        .join("java");
    let java_manager = Arc::new(JavaManager::new(java_cache, config.runtime.mirror.clone()));
    let mc_downloader = Arc::new(MinecraftDownloader::new(game_dir.clone(), config.runtime.mirror.clone()));
    let task_manager = Arc::new(TaskManager::new());
    let client_process = Arc::new(ClientProcess::new());
    let mod_bridge = Arc::new(ModBridge::new(game_dir.clone(), port_range));
    let activity_bus = Arc::new(ActivityBus::new());

    let state = AppState {
        config: Arc::new(config.clone()),
        java_manager,
        mc_downloader,
        task_manager,
        client_process,
        mod_bridge,
        activity_bus,
    };

    let app = Router::new()
        .route("/mcp", post(mcp::mcp_handler))
        .route("/ws", get(ws::ws_handler))
        .merge(routes::api_routes())
        .fallback(get(static_assets::static_handler))
        .layer(CorsLayer::permissive())
        .with_state(state);

    let listen = &config.server.listen;
    info!("Herald MCClientMCP listening on http://{}", listen);
    let listener = tokio::net::TcpListener::bind(listen).await?;
    axum::serve(listener, app).await?;
    Ok(())
}

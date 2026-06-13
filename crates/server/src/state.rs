use std::sync::Arc;
use herald_mcclient_bridge::ModBridge;
use herald_mcclient_core::AppConfig;
use herald_mcclient_env::{JavaManager, MinecraftDownloader, TaskManager};
use herald_mcclient_launcher::ClientProcess;

/// 应用全局状态，注入到所有 handler。
#[derive(Clone)]
pub struct AppState {
    pub config: Arc<AppConfig>,
    pub java_manager: Arc<JavaManager>,
    pub mc_downloader: Arc<MinecraftDownloader>,
    pub task_manager: Arc<TaskManager>,
    pub client_process: Arc<ClientProcess>,
    pub mod_bridge: Arc<ModBridge>,
    pub activity_bus: Arc<ActivityBus>,
}

/// MCP 活动事件总线 (WebSocket 推送到前端)。
#[derive(Default)]
pub struct ActivityBus {
    subscribers: std::sync::Mutex<Vec<tokio::sync::mpsc::UnboundedSender<ActivityEvent>>>,
}

#[derive(Debug, Clone, serde::Serialize)]
pub struct ActivityEvent {
    pub id: String,
    pub tool: String,
    pub status: String,  // "started" | "completed" | "failed"
    pub ts: u64,
    pub result_preview: Option<String>,
}

impl ActivityBus {
    pub fn new() -> Self { Self::default() }

    pub fn subscribe(&self) -> tokio::sync::mpsc::UnboundedReceiver<ActivityEvent> {
        let (tx, rx) = tokio::sync::mpsc::unbounded_channel();
        self.subscribers.lock().unwrap().push(tx);
        rx
    }

    pub fn emit(&self, event: ActivityEvent) {
        let mut subs = self.subscribers.lock().unwrap();
        subs.retain(|tx| tx.send(event.clone()).is_ok());
    }
}

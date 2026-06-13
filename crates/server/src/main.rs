use std::path::PathBuf;
use tracing_subscriber::{fmt, EnvFilter};
use herald_mcclient_core::AppConfig;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // 初始化日志
    fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| EnvFilter::new("info,herald_mcclient=debug")),
        )
        .init();

    // 加载配置
    let config_path = std::env::args()
        .nth(1)
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("config.toml"));

    let config = AppConfig::load(&config_path)?;
    tracing::info!("Config loaded, game_dir: {:?}", config.game_dir());

    // 确保 game_dir 存在
    let game_dir = config.game_dir();
    tokio::fs::create_dir_all(&game_dir).await?;

    // 启动服务
    herald_mcclient::app::run(config).await
}

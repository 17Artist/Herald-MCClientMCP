use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};

/// 顶层配置结构，对应 config.toml。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    #[serde(default)]
    pub server: ServerConfig,
    #[serde(default)]
    pub mc: McConfig,
    #[serde(default)]
    pub runtime: RuntimeConfig,
    #[serde(default = "default_mod_config")]
    pub r#mod: ModConfig,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerConfig {
    #[serde(default = "default_listen")]
    pub listen: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct McConfig {
    #[serde(default)]
    pub game_dir: String,
    #[serde(default = "default_mc_version")]
    pub default_version: String,
    #[serde(default = "default_loader")]
    pub default_loader: String,
    #[serde(default = "default_heap")]
    pub heap_mb: u32,
    #[serde(default = "default_username")]
    pub offline_username: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RuntimeConfig {
    #[serde(default = "bool_true")]
    pub auto_install_java: bool,
    #[serde(default = "bool_true")]
    pub auto_install_mc: bool,
    #[serde(default = "default_mirror")]
    pub mirror: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModConfig {
    #[serde(default = "bool_true")]
    pub auto_inject: bool,
    #[serde(default = "default_port_range")]
    pub mod_port_range: String,
}

// ─── defaults ───────────────────────────────────────────────────────

fn default_listen() -> String { "127.0.0.1:8686".to_string() }
fn default_mc_version() -> String { "1.20.1".to_string() }
fn default_loader() -> String { "fabric".to_string() }
fn default_heap() -> u32 { 4096 }
fn default_username() -> String { "HeraldDev".to_string() }
fn bool_true() -> bool { true }
fn default_mirror() -> String { "bmclapi".to_string() }
fn default_port_range() -> String { "8888-8898".to_string() }
fn default_mod_config() -> ModConfig {
    ModConfig {
        auto_inject: true,
        mod_port_range: default_port_range(),
    }
}

impl Default for ServerConfig {
    fn default() -> Self { Self { listen: default_listen() } }
}

impl Default for McConfig {
    fn default() -> Self {
        Self {
            game_dir: String::new(),
            default_version: default_mc_version(),
            default_loader: default_loader(),
            heap_mb: default_heap(),
            offline_username: default_username(),
        }
    }
}

impl Default for RuntimeConfig {
    fn default() -> Self {
        Self {
            auto_install_java: true,
            auto_install_mc: true,
            mirror: default_mirror(),
        }
    }
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            server: ServerConfig::default(),
            mc: McConfig::default(),
            runtime: RuntimeConfig::default(),
            r#mod: default_mod_config(),
        }
    }
}

impl AppConfig {
    /// 从文件加载配置。文件不存在时返回默认值。
    pub fn load(path: &Path) -> Result<Self, crate::HeraldError> {
        if !path.exists() {
            tracing::info!("config not found at {:?}, using defaults", path);
            return Ok(Self::default());
        }
        let text = std::fs::read_to_string(path)?;
        let cfg: Self = toml::from_str(&text)?;
        Ok(cfg)
    }

    /// 解析 game_dir，空字符串时用平台默认。
    pub fn game_dir(&self) -> PathBuf {
        if self.mc.game_dir.is_empty() {
            let base = dirs::data_local_dir()
                .unwrap_or_else(|| PathBuf::from("."));
            base.join("herald-mcclient").join("minecraft")
        } else {
            PathBuf::from(&self.mc.game_dir)
        }
    }

    /// 解析 mod 端口范围。
    pub fn mod_port_range(&self) -> (u16, u16) {
        let parts: Vec<&str> = self.r#mod.mod_port_range.split('-').collect();
        if parts.len() == 2 {
            let start = parts[0].parse().unwrap_or(8888);
            let end = parts[1].parse().unwrap_or(8898);
            (start, end)
        } else {
            (8888, 8898)
        }
    }
}

use thiserror::Error;

#[derive(Debug, Error)]
pub enum HeraldError {
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    #[error("Config parse error: {0}")]
    ConfigParse(#[from] toml::de::Error),

    #[error("JSON error: {0}")]
    Json(#[from] serde_json::Error),

    #[error("HTTP error: {0}")]
    Http(String),

    #[error("Environment error: {0}")]
    Env(String),

    #[error("Launcher error: {0}")]
    Launcher(String),

    #[error("Bridge error: {0}")]
    Bridge(String),

    #[error("Not found: {0}")]
    NotFound(String),

    #[error("{0}")]
    Other(String),
}

impl HeraldError {
    pub fn env(msg: impl Into<String>) -> Self { Self::Env(msg.into()) }
    pub fn launcher(msg: impl Into<String>) -> Self { Self::Launcher(msg.into()) }
    pub fn bridge(msg: impl Into<String>) -> Self { Self::Bridge(msg.into()) }
}

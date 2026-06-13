use std::path::PathBuf;
use serde::{Deserialize, Serialize};

/// MOD HTTP 桥接客户端。
/// 通过 HTTP 与嵌入 MC 客户端的 Herald Client MOD 通信。
pub struct ModBridge {
    http: reqwest::Client,
    game_dir: PathBuf,
    port_range: (u16, u16),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModStatus {
    pub online: bool,
    pub port: Option<u16>,
    pub mod_version: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActionResult {
    pub status: String,
    #[serde(default)]
    pub data: serde_json::Value,
    #[serde(default)]
    pub task_id: Option<String>,
    #[serde(default)]
    pub error: Option<ActionError>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActionError {
    pub code: String,
    pub message: String,
}

impl ModBridge {
    pub fn new(game_dir: PathBuf, port_range: (u16, u16)) -> Self {
        let http = reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(30))
            .build()
            .unwrap_or_default();
        Self { http, game_dir, port_range }
    }

    /// 读取 MOD 写入的 token 和 port 文件。
    fn read_credentials(&self) -> Option<(String, u16)> {
        let herald_dir = self.game_dir.join(".herald");
        let token_file = herald_dir.join("client-token");
        let port_file = herald_dir.join("client-port");

        let token = std::fs::read_to_string(&token_file).ok()?.trim().to_string();
        let port: u16 = std::fs::read_to_string(&port_file).ok()?.trim().parse().ok()?;

        if token.is_empty() { return None; }
        Some((token, port))
    }

    /// 暴露 credentials 供外部使用（如优雅关闭时构造 URL）。
    /// 返回 (port, token)。
    pub fn read_credentials_if_available(&self) -> Option<(u16, String)> {
        self.read_credentials().map(|(token, port)| (port, token))
    }

    /// 检测 MOD 是否在线。
    pub async fn ping(&self) -> ModStatus {
        let creds = match self.read_credentials() {
            Some(c) => c,
            None => return ModStatus { online: false, port: None, mod_version: None },
        };

        let (token, port) = creds;
        let url = format!("http://127.0.0.1:{}/ping", port);

        match self.http.get(&url)
            .bearer_auth(&token)
            .send()
            .await
        {
            Ok(resp) if resp.status().is_success() => {
                let body: serde_json::Value = resp.json().await.unwrap_or_default();
                ModStatus {
                    online: true,
                    port: Some(port),
                    mod_version: body["mod_version"].as_str().map(|s| s.to_string()),
                }
            }
            _ => ModStatus { online: false, port: Some(port), mod_version: None },
        }
    }

    /// 调用 MOD action。
    pub async fn call_action(
        &self,
        action_id: &str,
        params: serde_json::Value,
    ) -> anyhow::Result<ActionResult> {
        let (token, port) = self.read_credentials()
            .ok_or_else(|| anyhow::anyhow!("MOD not online (no credentials found)"))?;

        let url = format!("http://127.0.0.1:{}/action/{}", port, action_id);
        let resp = self.http.post(&url)
            .bearer_auth(&token)
            .json(&params)
            .send()
            .await?;

        if !resp.status().is_success() {
            let status = resp.status();
            let body = resp.text().await.unwrap_or_default();
            anyhow::bail!("Action call failed: HTTP {} - {}", status, body);
        }

        let result: ActionResult = resp.json().await?;
        Ok(result)
    }

    /// 列出 MOD 支持的 actions。
    pub async fn list_actions(&self) -> anyhow::Result<Vec<String>> {
        let (token, port) = self.read_credentials()
            .ok_or_else(|| anyhow::anyhow!("MOD not online"))?;

        let url = format!("http://127.0.0.1:{}/actions", port);
        let resp = self.http.get(&url)
            .bearer_auth(&token)
            .send()
            .await?;

        let body: serde_json::Value = resp.json().await?;
        // MOD 响应格式: {"status":"success","data":{"actions":["id1","id2",...]}}
        let actions = body.pointer("/data/actions")
            .and_then(|v| v.as_array())
            .map(|arr| arr.iter().filter_map(|v| v.as_str().map(String::from)).collect())
            .unwrap_or_default();
        Ok(actions)
    }

    /// 查询异步任务状态。
    pub async fn skill_status(&self, task_id: &str) -> anyhow::Result<serde_json::Value> {
        let (token, port) = self.read_credentials()
            .ok_or_else(|| anyhow::anyhow!("MOD not online"))?;

        let url = format!("http://127.0.0.1:{}/skill/{}", port, task_id);
        let resp = self.http.get(&url)
            .bearer_auth(&token)
            .send()
            .await?;

        let body: serde_json::Value = resp.json().await?;
        Ok(body)
    }

    /// 取消异步任务。
    pub async fn skill_cancel(&self, task_id: &str) -> anyhow::Result<()> {
        let (token, port) = self.read_credentials()
            .ok_or_else(|| anyhow::anyhow!("MOD not online"))?;

        let url = format!("http://127.0.0.1:{}/skill/{}/cancel", port, task_id);
        self.http.post(&url)
            .bearer_auth(&token)
            .send()
            .await?;
        Ok(())
    }

    /// 等待 MOD 上线（轮询 ping）。
    pub async fn wait_ready(&self, timeout_secs: u64) -> bool {
        let start = std::time::Instant::now();
        let timeout = std::time::Duration::from_secs(timeout_secs);

        loop {
            if start.elapsed() > timeout { return false; }
            let status = self.ping().await;
            if status.online { return true; }
            tokio::time::sleep(std::time::Duration::from_millis(500)).await;
        }
    }
}

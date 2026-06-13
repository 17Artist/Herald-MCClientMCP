use std::path::PathBuf;
use serde::Deserialize;
use tracing::info;

/// MC 客户端下载器。
pub struct MinecraftDownloader {
    game_dir: PathBuf,
    mirror: String,
}

#[derive(Debug, Deserialize)]
pub struct VersionManifest {
    pub versions: Vec<VersionEntry>,
}

#[derive(Debug, Deserialize)]
pub struct VersionEntry {
    pub id: String,
    pub url: String,
    #[serde(rename = "type")]
    pub release_type: String,
}

// SPLICE_1

fn is_lib_allowed(lib: &serde_json::Value) -> bool {
    let rules = match lib.get("rules") {
        Some(r) => r.as_array(),
        None => return true,
    };
    let rules = match rules {
        Some(r) => r,
        None => return true,
    };
    let mut allowed = false;
    let current_os = if cfg!(windows) { "windows" }
        else if cfg!(target_os = "macos") { "osx" }
        else { "linux" };
    for rule in rules {
        let action = rule["action"].as_str().unwrap_or("disallow");
        let os_name = rule.pointer("/os/name").and_then(|v| v.as_str());
        let os_match = os_name.map_or(true, |n| n == current_os);
        if os_match {
            allowed = action == "allow";
        }
    }
    allowed
}


impl MinecraftDownloader {
    pub fn new(game_dir: PathBuf, mirror: String) -> Self {
        Self { game_dir, mirror }
    }

    pub fn versions_dir(&self) -> PathBuf { self.game_dir.join("versions") }
    pub fn libraries_dir(&self) -> PathBuf { self.game_dir.join("libraries") }
    pub fn assets_dir(&self) -> PathBuf { self.game_dir.join("assets") }

    /// 检查指定版本是否已下载。
    pub fn is_version_installed(&self, version: &str) -> bool {
        let json_file = self.versions_dir().join(version).join(format!("{}.json", version));
        let jar_file = self.versions_dir().join(version).join(format!("{}.jar", version));
        json_file.exists() && jar_file.exists()
    }

    /// 获取版本清单。
    pub async fn fetch_manifest(&self) -> anyhow::Result<VersionManifest> {
        let url = manifest_url(&self.mirror);
        info!("Fetching version manifest from {}", url);
        let client = reqwest::Client::builder()
            .user_agent("Herald-MCClientMCP/0.1.0")
            .build()?;
        let resp = client.get(url).send().await?;
        if !resp.status().is_success() {
            anyhow::bail!("Failed to fetch manifest: HTTP {}", resp.status());
        }
        let manifest: VersionManifest = resp.json().await?;
        Ok(manifest)
    }

    /// 下载指定版本的 MC 客户端。
    pub async fn download_version(&self, version: &str) -> anyhow::Result<()> {
        let version_dir = self.versions_dir().join(version);
        let version_json_path = version_dir.join(format!("{}.json", version));
        let jar_path = version_dir.join(format!("{}.jar", version));

        // 如果已完整安装（json + jar + assets index 存在），跳过
        if version_json_path.exists() && jar_path.exists() {
            let json_text = tokio::fs::read_to_string(&version_json_path).await?;
            let version_info: serde_json::Value = serde_json::from_str(&json_text)?;
            let index_id = version_info.pointer("/assetIndex/id")
                .and_then(|v| v.as_str()).unwrap_or("");
            let index_file = self.assets_dir().join("indexes").join(format!("{}.json", index_id));

            if index_file.exists() {
                // assets index 存在，说明 assets 下载至少启动过
                // 补全可能缺失的 libraries 和 assets（已有的会跳过）
                self.download_libraries(&version_info).await?;
                self.download_assets(&version_info).await?;
                info!("MC {} verified complete", version);
                return Ok(());
            }
        }

        // 下载 version.json（如果缺失）
        if !version_json_path.exists() {
            let manifest = self.fetch_manifest().await?;
            let entry = manifest.versions.iter()
                .find(|v| v.id == version)
                .ok_or_else(|| anyhow::anyhow!("Version {} not found in manifest", version))?;

            tokio::fs::create_dir_all(&version_dir).await?;
            let json_url = mirror_url(&entry.url, &self.mirror);
            crate::download::download_file(&json_url, &version_json_path, None).await?;
        }

        // 下载 client.jar（如果缺失）
        if !jar_path.exists() {
            let text = tokio::fs::read_to_string(&version_json_path).await?;
            let vi: serde_json::Value = serde_json::from_str(&text)?;
            if let Some(client) = vi.pointer("/downloads/client") {
                let url = client["url"].as_str().unwrap_or_default();
                let sha1 = client["sha1"].as_str();
                let dl_url = mirror_url(url, &self.mirror);
                crate::download::download_file(&dl_url, &jar_path, sha1).await?;
            }
        }

        // 读取 version.json 用于后续下载
        let json_text = tokio::fs::read_to_string(&version_json_path).await?;
        let version_info: serde_json::Value = serde_json::from_str(&json_text)?;

        // 下载 libraries
        self.download_libraries(&version_info).await?;

        // 下载 assets（首次较慢，后续版本共享已有文件）
        self.download_assets(&version_info).await?;

        info!("MC {} download complete", version);
        Ok(())
    }

    async fn download_libraries(&self, info: &serde_json::Value) -> anyhow::Result<()> {
        let libs = info["libraries"].as_array()
            .ok_or_else(|| anyhow::anyhow!("No libraries in version json"))?;
        let lib_dir = self.libraries_dir();
        tokio::fs::create_dir_all(&lib_dir).await?;

        // 收集需要下载的任务
        let mut tasks: Vec<(String, std::path::PathBuf, Option<String>)> = Vec::new();
        for lib in libs {
            if !is_lib_allowed(lib) { continue; }
            if let Some(artifact) = lib.pointer("/downloads/artifact") {
                let url = artifact["url"].as_str().unwrap_or_default();
                let path = artifact["path"].as_str().unwrap_or_default();
                let sha1 = artifact["sha1"].as_str().map(|s| s.to_string());
                if url.is_empty() || path.is_empty() { continue; }
                let dest = lib_dir.join(path);
                if dest.exists() { continue; }
                let dl_url = mirror_url(url, &self.mirror);
                tasks.push((dl_url, dest, sha1));
            }
        }

        info!("Downloading {} libraries (concurrent)...", tasks.len());

        // 并发下载，限制 16 个并发
        let semaphore = std::sync::Arc::new(tokio::sync::Semaphore::new(16));
        let mut handles = Vec::new();

        for (url, dest, sha1) in tasks {
            let sem = semaphore.clone();
            handles.push(tokio::spawn(async move {
                let _permit = sem.acquire().await.unwrap();
                crate::download::download_file(&url, &dest, sha1.as_deref()).await
            }));
        }

        let mut failed = 0u32;
        for handle in handles {
            match handle.await {
                Ok(Ok(())) => {},
                Ok(Err(e)) => {
                    tracing::warn!("Library download failed: {}", e);
                    failed += 1;
                }
                Err(e) => {
                    tracing::warn!("Library task panicked: {}", e);
                    failed += 1;
                }
            }
        }
        if failed > 0 {
            tracing::warn!("{} libraries failed to download", failed);
        }
        info!("All libraries downloaded ({} failed)", failed);
        Ok(())
    }

    async fn download_assets(&self, info: &serde_json::Value) -> anyhow::Result<()> {
        let asset_index = match info.get("assetIndex") {
            Some(v) => v,
            None => return Ok(()),
        };
        let index_url = asset_index["url"].as_str().unwrap_or_default();
        let index_id = asset_index["id"].as_str().unwrap_or_default();
        if index_url.is_empty() { return Ok(()); }

        let indexes_dir = self.assets_dir().join("indexes");
        tokio::fs::create_dir_all(&indexes_dir).await?;
        let index_file = indexes_dir.join(format!("{}.json", index_id));

        if !index_file.exists() {
            let dl_url = mirror_url(index_url, &self.mirror);
            crate::download::download_file(&dl_url, &index_file, None).await?;
        }

        let index_text = tokio::fs::read_to_string(&index_file).await?;
        let index_data: serde_json::Value = serde_json::from_str(&index_text)?;
        let objects = match index_data["objects"].as_object() {
            Some(o) => o,
            None => return Ok(()),
        };

        let objects_dir = self.assets_dir().join("objects");
        let resource_base = match self.mirror.as_str() {
            "bmclapi" => "https://bmclapi2.bangbang93.com/assets",
            _ => "https://resources.download.minecraft.net",
        };

        // 收集需要下载的 assets
        let mut tasks: Vec<(String, std::path::PathBuf, String)> = Vec::new();
        for (_name, obj) in objects {
            let hash = obj["hash"].as_str().unwrap_or_default();
            if hash.len() < 2 { continue; }
            let prefix = &hash[..2];
            let dest = objects_dir.join(prefix).join(hash);
            if dest.exists() { continue; }
            let url = format!("{}/{}/{}", resource_base, prefix, hash);
            tasks.push((url, dest, hash.to_string()));
        }

        info!("Downloading {} assets (concurrent)...", tasks.len());

        // 并发下载 assets，限制 32 并发
        let semaphore = std::sync::Arc::new(tokio::sync::Semaphore::new(32));
        let mut handles = Vec::new();

        for (url, dest, hash) in tasks {
            let sem = semaphore.clone();
            handles.push(tokio::spawn(async move {
                let _permit = sem.acquire().await.unwrap();
                crate::download::download_file_best_effort(&url, &dest, Some(&hash)).await
            }));
        }

        let mut failed = 0;
        for handle in handles {
            if let Err(_) = handle.await? {
                failed += 1;
            }
        }
        if failed > 0 {
            info!("{} assets failed to download (non-critical)", failed);
        }
        info!("Assets download complete");
        Ok(())
    }
}

fn mirror_url(url: &str, mirror: &str) -> String {
    match mirror {
        "bmclapi" => url
            .replace("https://piston-data.mojang.com", "https://bmclapi2.bangbang93.com")
            .replace("https://piston-meta.mojang.com", "https://bmclapi2.bangbang93.com")
            .replace("https://launchermeta.mojang.com", "https://bmclapi2.bangbang93.com")
            .replace("https://launcher.mojang.com", "https://bmclapi2.bangbang93.com")
            .replace("https://libraries.minecraft.net", "https://bmclapi2.bangbang93.com/maven")
            .replace("https://resources.download.minecraft.net", "https://bmclapi2.bangbang93.com/assets"),
        _ => url.to_string(),
    }
}

fn manifest_url(mirror: &str) -> &str {
    match mirror {
        "bmclapi" => "https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json",
        _ => "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json",
    }
}

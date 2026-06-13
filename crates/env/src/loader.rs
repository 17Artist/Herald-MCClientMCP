use std::path::PathBuf;
use tracing::info;

/// Mod Loader 安装器（Fabric / Forge / NeoForge）。
pub struct LoaderInstaller {
    game_dir: PathBuf,
    java_path: PathBuf,
    mirror: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LoaderType {
    Fabric,
    Forge,
    NeoForge,
}

impl LoaderType {
    pub fn from_str(s: &str) -> Option<Self> {
        match s.to_lowercase().as_str() {
            "fabric" => Some(Self::Fabric),
            "forge" => Some(Self::Forge),
            "neoforge" => Some(Self::NeoForge),
            _ => None,
        }
    }

    pub fn name(&self) -> &'static str {
        match self {
            Self::Fabric => "Fabric",
            Self::Forge => "Forge",
            Self::NeoForge => "NeoForge",
        }
    }
}

impl LoaderInstaller {
    pub fn new(game_dir: PathBuf, java_path: PathBuf, mirror: String) -> Self {
        Self { game_dir, java_path, mirror }
    }

    /// 检查 loader 是否已安装。
    pub fn is_installed(&self, loader: LoaderType, mc_version: &str) -> bool {
        let versions_dir = self.game_dir.join("versions");
        match loader {
            LoaderType::Fabric => {
                // Fabric 会创建 fabric-loader-*-{mc_version} 目录
                if let Ok(entries) = std::fs::read_dir(&versions_dir) {
                    for entry in entries.flatten() {
                        let name = entry.file_name().to_string_lossy().to_string();
                        if name.starts_with("fabric-loader-") && name.ends_with(mc_version) {
                            return true;
                        }
                    }
                }
                false
            }
            LoaderType::Forge => {
                if let Ok(entries) = std::fs::read_dir(&versions_dir) {
                    for entry in entries.flatten() {
                        let name = entry.file_name().to_string_lossy().to_string();
                        if name.contains(mc_version) && name.contains("forge") {
                            return true;
                        }
                    }
                }
                false
            }
            LoaderType::NeoForge => {
                if let Ok(entries) = std::fs::read_dir(&versions_dir) {
                    for entry in entries.flatten() {
                        let name = entry.file_name().to_string_lossy().to_string();
                        if name.contains("neoforge") {
                            return true;
                        }
                    }
                }
                false
            }
        }
    }

    /// 安装 loader。
    pub async fn install(&self, loader: LoaderType, mc_version: &str) -> anyhow::Result<()> {
        if self.is_installed(loader, mc_version) {
            info!("{} already installed for MC {}", loader.name(), mc_version);
            return Ok(());
        }

        match loader {
            LoaderType::Fabric => self.install_fabric(mc_version).await,
            LoaderType::Forge => self.install_forge(mc_version).await,
            LoaderType::NeoForge => self.install_neoforge(mc_version).await,
        }
    }

    async fn install_fabric(&self, mc_version: &str) -> anyhow::Result<()> {
        info!("Installing Fabric for MC {}...", mc_version);

        // MC 1.x needs older Fabric Loader (0.16.x) and installer that supports it
        let (installer_url, loader_version) = if mc_version.starts_with("1.") {
            (
                "https://maven.fabricmc.net/net/fabricmc/fabric-installer/0.11.2/fabric-installer-0.11.2.jar",
                Some("0.16.14"),
            )
        } else {
            (
                "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar",
                None, // uses latest loader (0.18+)
            )
        };

        let temp_dir = self.game_dir.join("temp");
        tokio::fs::create_dir_all(&temp_dir).await?;
        let installer_path = temp_dir.join("fabric-installer.jar");

        crate::download::download_file(installer_url, &installer_path, None).await?;

        // java -jar fabric-installer.jar client -mcversion <ver> -dir <gameDir> -noprofile [-loader <ver>]
        let mut cmd = tokio::process::Command::new(&self.java_path);
        cmd.arg("-jar")
            .arg(&installer_path)
            .arg("client")
            .arg("-mcversion")
            .arg(mc_version)
            .arg("-dir")
            .arg(&self.game_dir)
            .arg("-noprofile");

        if let Some(lv) = loader_version {
            cmd.arg("-loader").arg(lv);
        }

        let output = cmd.output().await?;

        let _ = tokio::fs::remove_file(&installer_path).await;

        if !output.status.success() {
            let stderr = String::from_utf8_lossy(&output.stderr);
            anyhow::bail!("Fabric installation failed: {}", stderr);
        }

        info!("Fabric installed successfully");
        Ok(())
    }

    async fn install_forge(&self, mc_version: &str) -> anyhow::Result<()> {
        info!("Installing Forge for MC {}...", mc_version);
        // 用 BMCLAPI 或官方 Maven
        let forge_version = self.get_forge_version(mc_version).await?;
        let maven_base = match self.mirror.as_str() {
            "bmclapi" => "https://bmclapi2.bangbang93.com/maven",
            _ => "https://maven.minecraftforge.net",
        };
        let installer_url = format!(
            "{}/net/minecraftforge/forge/{}-{}/forge-{}-{}-installer.jar",
            maven_base, mc_version, forge_version, mc_version, forge_version
        );

        let temp_dir = self.game_dir.join("temp");
        tokio::fs::create_dir_all(&temp_dir).await?;
        let installer_path = temp_dir.join("forge-installer.jar");

        crate::download::download_file(&installer_url, &installer_path, None).await?;

        // Forge 需要 launcher_profiles.json
        let profiles = self.game_dir.join("launcher_profiles.json");
        if !profiles.exists() {
            tokio::fs::write(&profiles, r#"{"profiles":{}}"#).await?;
        }

        let output = tokio::process::Command::new(&self.java_path)
            .arg("-jar")
            .arg(&installer_path)
            .arg("--installClient")
            .arg(&self.game_dir)
            .output()
            .await?;

        let _ = tokio::fs::remove_file(&installer_path).await;

        if !output.status.success() {
            let stderr = String::from_utf8_lossy(&output.stderr);
            let stdout = String::from_utf8_lossy(&output.stdout);
            anyhow::bail!("Forge installation failed:\n{}\n{}", stdout, stderr);
        }

        info!("Forge installed successfully");
        Ok(())
    }

    async fn install_neoforge(&self, mc_version: &str) -> anyhow::Result<()> {
        info!("Installing NeoForge for MC {}...", mc_version);
        let neoforge_version = self.get_neoforge_version(mc_version).await?;
        let installer_url = format!(
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/{}/neoforge-{}-installer.jar",
            neoforge_version, neoforge_version
        );

        let temp_dir = self.game_dir.join("temp");
        tokio::fs::create_dir_all(&temp_dir).await?;
        let installer_path = temp_dir.join("neoforge-installer.jar");

        crate::download::download_file(&installer_url, &installer_path, None).await?;

        let profiles = self.game_dir.join("launcher_profiles.json");
        if !profiles.exists() {
            tokio::fs::write(&profiles, r#"{"profiles":{}}"#).await?;
        }

        let output = tokio::process::Command::new(&self.java_path)
            .arg("-jar")
            .arg(&installer_path)
            .arg("--installClient")
            .arg(&self.game_dir)
            .output()
            .await?;

        let _ = tokio::fs::remove_file(&installer_path).await;

        if !output.status.success() {
            let stderr = String::from_utf8_lossy(&output.stderr);
            anyhow::bail!("NeoForge installation failed: {}", stderr);
        }

        info!("NeoForge installed successfully");
        Ok(())
    }

    /// 获取 MC 版本对应的最新 Forge 版本号。
    async fn get_forge_version(&self, mc_version: &str) -> anyhow::Result<String> {
        match mc_version {
            "1.20.1" => Ok("47.3.0".to_string()),
            "1.20.4" => Ok("49.0.31".to_string()),
            _ => anyhow::bail!("Unsupported Forge MC version: {}", mc_version),
        }
    }

    /// 获取 MC 版本对应的最新 NeoForge 版本号。
    async fn get_neoforge_version(&self, mc_version: &str) -> anyhow::Result<String> {
        match mc_version {
            "1.20.1" => Ok("47.1.106".to_string()),
            "1.21.1" => Ok("21.1.172".to_string()),
            "1.21.4" => Ok("21.4.186".to_string()),
            "1.21.8" => Ok("21.8.52".to_string()),
            "26.1" => Ok("26.1.0.19-beta".to_string()),
            "26.1.2" => Ok("26.1.2.76".to_string()),
            _ => anyhow::bail!("Unsupported NeoForge MC version: {}", mc_version),
        }
    }
}

use std::path::{Path, PathBuf};
use tracing::info;

/// Java 环境管理器：探测、下载 Adoptium Temurin。
pub struct JavaManager {
    cache_dir: PathBuf,
    mirror: String,
}

#[derive(Debug, Clone)]
pub struct JavaInstallation {
    pub path: PathBuf,
    pub major_version: u32,
    pub vendor: String,
}

impl JavaManager {
    pub fn new(cache_dir: PathBuf, mirror: String) -> Self {
        Self { cache_dir, mirror }
    }

    /// 探测系统中已安装的 Java。
    pub fn probe(&self) -> Vec<JavaInstallation> {
        let mut found = Vec::new();

        // 1. 检查托管缓存目录
        self.probe_managed_dir(&mut found);

        // 2. JAVA_HOME
        if let Ok(java_home) = std::env::var("JAVA_HOME") {
            self.probe_java_dir(Path::new(&java_home), &mut found);
        }

        // 3. PATH 中的 java
        if let Ok(paths) = std::env::var("PATH") {
            for dir in paths.split(if cfg!(windows) { ';' } else { ':' }) {
                let java_bin = Path::new(dir).join(java_exe_name());
                if java_bin.exists() {
                    if let Some(install) = self.detect_java(&java_bin) {
                        if !found.iter().any(|f| f.path == install.path) {
                            found.push(install);
                        }
                    }
                }
            }
        }

        // 4. Windows 常见目录
        #[cfg(windows)]
        self.probe_windows_common_dirs(&mut found);

        found
    }

    /// 找到最适合的 Java 17+ 路径。
    pub fn find_best_java(&self, required_major: u32) -> Option<JavaInstallation> {
        let installations = self.probe();

        // 优先精确匹配
        if let Some(exact) = installations.iter().find(|i| i.major_version == required_major) {
            return Some(exact.clone());
        }

        // 接受 >= required_major
        installations.into_iter()
            .filter(|i| i.major_version >= required_major)
            .min_by_key(|i| i.major_version)
    }

    /// 下载并安装指定大版本的 Adoptium Temurin JRE。
    pub async fn install(&self, major: u32) -> anyhow::Result<JavaInstallation> {
        let target_dir = self.cache_dir.join(format!("jdk-{}", major));
        if target_dir.exists() {
            // 已安装，直接返回
            let java_bin = target_dir.join("bin").join(java_exe_name());
            if java_bin.exists() {
                info!("Java {} already installed at {:?}", major, target_dir);
                return Ok(JavaInstallation {
                    path: java_bin,
                    major_version: major,
                    vendor: "Adoptium Temurin".to_string(),
                });
            }
        }

        info!("Downloading Adoptium Temurin JRE {}...", major);
        let url = self.build_adoptium_url(major)?;
        let archive_path = self.cache_dir.join(format!("temurin-{}.zip", major));

        tokio::fs::create_dir_all(&self.cache_dir).await?;
        crate::download::download_file(&url, &archive_path, None).await?;

        // 解压
        info!("Extracting Java {}...", major);
        Self::extract_archive(&archive_path, &self.cache_dir, &target_dir)?;

        // 清理压缩包
        let _ = tokio::fs::remove_file(&archive_path).await;

        let java_bin = Self::find_java_in_dir(&target_dir);
        match java_bin {
            Some(path) => Ok(JavaInstallation {
                path,
                major_version: major,
                vendor: "Adoptium Temurin".to_string(),
            }),
            None => anyhow::bail!("Extracted Java but cannot find java binary in {:?}", target_dir),
        }
    }

    // ─── Private ─────────────────────────────────────────────────────

    fn probe_managed_dir(&self, found: &mut Vec<JavaInstallation>) {
        if !self.cache_dir.exists() {
            return;
        }
        if let Ok(entries) = std::fs::read_dir(&self.cache_dir) {
            for entry in entries.flatten() {
                let path = entry.path();
                if path.is_dir() {
                    self.probe_java_dir(&path, found);
                }
            }
        }
    }

    fn probe_java_dir(&self, dir: &Path, found: &mut Vec<JavaInstallation>) {
        let java_bin = dir.join("bin").join(java_exe_name());
        if java_bin.exists() {
            if let Some(install) = self.detect_java(&java_bin) {
                if !found.iter().any(|f| f.path == install.path) {
                    found.push(install);
                }
            }
        }
    }

    #[cfg(windows)]
    fn probe_windows_common_dirs(&self, found: &mut Vec<JavaInstallation>) {
        let program_files = vec![
            std::env::var("ProgramFiles").unwrap_or_else(|_| "C:\\Program Files".into()),
            std::env::var("ProgramFiles(x86)").unwrap_or_else(|_| "C:\\Program Files (x86)".into()),
            std::env::var("LOCALAPPDATA").unwrap_or_default(),
        ];
        let vendors = ["Java", "Eclipse Adoptium", "Microsoft", "Zulu", "BellSoft"];
        for pf in &program_files {
            if pf.is_empty() { continue; }
            for vendor in &vendors {
                let vendor_dir = Path::new(pf).join(vendor);
                if vendor_dir.is_dir() {
                    if let Ok(entries) = std::fs::read_dir(&vendor_dir) {
                        for entry in entries.flatten() {
                            let dir = entry.path();
                            if dir.is_dir() {
                                self.probe_java_dir(&dir, found);
                            }
                        }
                    }
                }
            }
        }
    }

    fn detect_java(&self, java_bin: &Path) -> Option<JavaInstallation> {
        let output = std::process::Command::new(java_bin)
            .arg("-version")
            .output()
            .ok()?;
        let text = String::from_utf8_lossy(&output.stderr);
        let version = parse_java_version(&text)?;
        let major = parse_major_version(&version)?;
        let vendor = if text.contains("Temurin") { "Adoptium Temurin" }
            else if text.contains("OpenJDK") { "OpenJDK" }
            else { "Unknown" };
        Some(JavaInstallation {
            path: java_bin.to_path_buf(),
            major_version: major,
            vendor: vendor.to_string(),
        })
    }

    fn build_adoptium_url(&self, major: u32) -> anyhow::Result<String> {
        let os = if cfg!(windows) { "windows" }
            else if cfg!(target_os = "macos") { "mac" }
            else { "linux" };
        let arch = if cfg!(target_arch = "x86_64") { "x64" }
            else if cfg!(target_arch = "aarch64") { "aarch64" }
            else { "x64" };
        let ext = if cfg!(windows) { "zip" } else { "tar.gz" };

        let base = match self.mirror.as_str() {
            "bmclapi" => "https://bmclapi2.bangbang93.com/adoptium/releases",
            "tuna" => "https://mirrors.tuna.tsinghua.edu.cn/Adoptium",
            _ => "https://api.adoptium.net",
        };

        // Use Adoptium API v3 format
        Ok(format!(
            "{}/v3/binary/latest/{}/ga/{}/{}/jre/hotspot/normal/eclipse",
            base, major, os, arch
        ))
    }

    fn extract_archive(archive: &Path, dest_parent: &Path, target_dir: &Path) -> anyhow::Result<()> {
        // For Windows .zip files
        let file = std::fs::File::open(archive)?;
        let mut zip = zip::ZipArchive::new(file)?;

        // 创建临时解压目录
        let temp_dir = dest_parent.join("_extract_temp");
        let _ = std::fs::remove_dir_all(&temp_dir);
        std::fs::create_dir_all(&temp_dir)?;

        for i in 0..zip.len() {
            let mut file = zip.by_index(i)?;
            let name = file.name().to_string();
            let out_path = temp_dir.join(&name);
            if file.is_dir() {
                std::fs::create_dir_all(&out_path)?;
            } else {
                if let Some(parent) = out_path.parent() {
                    std::fs::create_dir_all(parent)?;
                }
                let mut out_file = std::fs::File::create(&out_path)?;
                std::io::copy(&mut file, &mut out_file)?;
            }
        }

        // Adoptium archives have a top-level dir like jdk-17.0.x+y-jre/
        // Move its contents to target_dir
        let _ = std::fs::remove_dir_all(target_dir);
        let entries: Vec<_> = std::fs::read_dir(&temp_dir)?
            .flatten()
            .map(|e| e.path())
            .collect();
        if entries.len() == 1 && entries[0].is_dir() {
            std::fs::rename(&entries[0], target_dir)?;
        } else {
            std::fs::rename(&temp_dir, target_dir)?;
        }
        let _ = std::fs::remove_dir_all(&temp_dir);
        Ok(())
    }

    fn find_java_in_dir(dir: &Path) -> Option<PathBuf> {
        let java_bin = dir.join("bin").join(java_exe_name());
        if java_bin.exists() { return Some(java_bin); }
        // 递归一层
        if let Ok(entries) = std::fs::read_dir(dir) {
            for entry in entries.flatten() {
                let p = entry.path();
                if p.is_dir() {
                    let bin = p.join("bin").join(java_exe_name());
                    if bin.exists() { return Some(bin); }
                }
            }
        }
        None
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────

fn java_exe_name() -> &'static str {
    if cfg!(windows) { "java.exe" } else { "java" }
}

fn parse_java_version(text: &str) -> Option<String> {
    // Match: version "17.0.7" or version "1.8.0_333"
    let re_pattern = "version \"";
    let start = text.find(re_pattern)? + re_pattern.len();
    let end = text[start..].find('"')? + start;
    Some(text[start..end].to_string())
}

fn parse_major_version(version: &str) -> Option<u32> {
    if version.starts_with("1.") {
        // Legacy: 1.8.0_xxx → 8
        version.strip_prefix("1.")?.split('.').next()?.parse().ok()
    } else {
        version.split('.').next()?.parse().ok()
    }
}

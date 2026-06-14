use std::path::{Path, PathBuf};
use serde::Serialize;

/// 构建 MC 客户端启动参数。
pub struct LaunchArgs {
    pub java_path: PathBuf,
    pub game_dir: PathBuf,
    pub version: String,
    pub loader: Option<String>,
    pub heap_mb: u32,
    pub username: String,
    pub headless: bool,
}

#[derive(Debug, Serialize)]
pub struct LaunchCommand {
    pub java_path: String,
    pub args: Vec<String>,
    pub game_dir: String,
}

impl LaunchArgs {
    /// 构建完整启动命令。
    pub fn build(&self) -> anyhow::Result<LaunchCommand> {
        let versions_dir = self.game_dir.join("versions");
        let libraries_dir = self.game_dir.join("libraries");
        let assets_dir = self.game_dir.join("assets");

        // 确定要用的 version id（带 loader 前缀的）
        let version_id = self.resolve_version_id(&versions_dir)?;
        let version_dir = versions_dir.join(&version_id);
        let version_json = version_dir.join(format!("{}.json", version_id));

        if !version_json.exists() {
            anyhow::bail!("Version JSON not found: {:?}", version_json);
        }

        let json_text = std::fs::read_to_string(&version_json)?;
        let info: serde_json::Value = serde_json::from_str(&json_text)?;

        // 也要读取 vanilla json（如果当前是 loader 版本）
        let vanilla_json_path = versions_dir.join(&self.version).join(format!("{}.json", self.version));
        let vanilla_info = if vanilla_json_path.exists() && version_id != self.version {
            let text = std::fs::read_to_string(&vanilla_json_path)?;
            Some(serde_json::from_str::<serde_json::Value>(&text)?)
        } else {
            None
        };

        let main_class = info["mainClass"].as_str()
            .unwrap_or("net.minecraft.client.main.Main")
            .to_string();

        let natives_dir = versions_dir.join(&self.version).join("natives");
        let classpath = self.build_classpath(&info, vanilla_info.as_ref(), &libraries_dir, &versions_dir)?;

        let mut args = Vec::new();

        // JVM args from version JSON (Forge/NeoForge needs -p, --add-modules, --add-opens etc.)
        let sep = if cfg!(windows) { ";" } else { ":" };
        let mut module_path_artifacts: Vec<String> = Vec::new(); // track artifacts on module-path
        if let Some(jvm_args) = info.pointer("/arguments/jvm").and_then(|v| v.as_array()) {
            let mut is_module_path_next = false;
            for arg in jvm_args {
                if let Some(s) = arg.as_str() {
                    let mut resolved = s
                        .replace("${library_directory}", &libraries_dir.display().to_string())
                        .replace("${classpath_separator}", sep)
                        .replace("${version_name}", &version_id);
                    // Append herald-client to ignoreList so Forge/NeoForge doesn't load it as a module
                    if resolved.starts_with("-DignoreList=") {
                        resolved.push_str(",herald-client");
                    }
                    // Track module path entries to filter from classpath
                    if is_module_path_next {
                        // Extract artifact names from module path (e.g. "asm-9.7.jar" → "asm")
                        for path_entry in resolved.split(sep) {
                            if let Some(fname) = Path::new(path_entry).file_name() {
                                let fname_str = fname.to_string_lossy();
                                // Extract artifact base name (strip version): "asm-9.7.jar" → "asm"
                                if let Some(artifact) = extract_artifact_name(&fname_str) {
                                    module_path_artifacts.push(artifact);
                                }
                            }
                        }
                        is_module_path_next = false;
                    }
                    if resolved == "-p" || resolved == "--module-path" {
                        is_module_path_next = true;
                    }
                    args.push(resolved);
                }
                // Skip conditional/object args
            }
        }

        // Filter classpath: remove entries whose artifact is already on module-path (different version)
        // Also for Fabric: deduplicate ASM jars (keep only the version referenced by the loader profile)
        let filtered_cp = {
            let entries: Vec<&str> = classpath.split(sep).collect();
            let mut result: Vec<&str> = Vec::new();
            
            // For Fabric, find the expected ASM version from the loader profile
            let is_fabric = self.loader.as_deref() == Some("fabric");
            let fabric_asm_version: Option<String> = if is_fabric {
                // Find ASM version from Fabric loader profile
                let fabric_dir = versions_dir.read_dir().ok()
                    .and_then(|entries| {
                        entries.filter_map(|e| e.ok())
                            .find(|e| {
                                let name = e.file_name().to_string_lossy().to_string();
                                name.contains("fabric-loader") && name.ends_with(&self.version)
                            })
                    });
                fabric_dir.and_then(|dir| {
                    let json_name = dir.file_name().to_string_lossy().to_string();
                    let json_path = dir.path().join(format!("{}.json", json_name));
                    std::fs::read_to_string(&json_path).ok()
                }).and_then(|content| {
                    content.find("org.ow2.asm:asm:").map(|pos| {
                        let after = &content[pos + 16..];
                        after.find('"').map(|end| after[..end].to_string())
                    }).flatten()
                })
            } else {
                None
            };
            
            for entry in &entries {
                let fname = Path::new(entry).file_name()
                    .map(|f| f.to_string_lossy().to_string())
                    .unwrap_or_default();
                
                // Module-path conflict filter (for NeoForge/Forge)
                if !module_path_artifacts.is_empty() {
                    if let Some(artifact) = extract_artifact_name(&fname) {
                        if module_path_artifacts.contains(&artifact) {
                            continue;
                        }
                    }
                }
                
                // ASM dedup for Fabric: only keep the correct ASM version
                if is_fabric {
                    if let Some(ref expected_ver) = fabric_asm_version {
                        // Check if this is an ASM jar (org/ow2/asm/) with wrong version
                        if entry.contains("org/ow2/asm/") || entry.contains("org\\ow2\\asm\\") {
                            if !entry.contains(expected_ver) {
                                continue;
                            }
                        }
                    }
                }
                
                result.push(entry);
            }
            result.join(sep)
        };

        // Our JVM args (after version JSON args so they take precedence)
        args.push(format!("-Xmx{}m", self.heap_mb));
        args.push(format!("-Xms{}m", self.heap_mb / 2));
        args.push(format!("-Djava.library.path={}", natives_dir.display()));
        args.push("-Dminecraft.launcher.brand=Herald".to_string());
        args.push("-Dminecraft.launcher.version=0.1.0".to_string());
        if self.headless {
            args.push("-Dherald.headless=true".to_string());
        }
        args.push("-cp".to_string());
        args.push(filtered_cp);

        // Main class
        args.push(main_class);

        // Game args from version JSON (Forge needs --launchTarget, --fml.* etc.)
        if let Some(game_args) = info.pointer("/arguments/game").and_then(|v| v.as_array()) {
            for arg in game_args {
                if let Some(s) = arg.as_str() {
                    args.push(s.to_string());
                }
            }
        }

        // Standard game args
        let uuid = uuid::Uuid::new_v4().to_string().replace("-", "");
        let asset_index = info.pointer("/assetIndex/id")
            .or_else(|| vanilla_info.as_ref().and_then(|v| v.pointer("/assetIndex/id")))
            .and_then(|v| v.as_str())
            .unwrap_or("1.20");

        args.extend_from_slice(&[
            "--username".to_string(), self.username.clone(),
            "--version".to_string(), self.version.clone(),
            "--gameDir".to_string(), self.game_dir.display().to_string(),
            "--assetsDir".to_string(), assets_dir.display().to_string(),
            "--assetIndex".to_string(), asset_index.to_string(),
            "--uuid".to_string(), uuid,
            "--accessToken".to_string(), "0".to_string(),
            "--userType".to_string(), "legacy".to_string(),
            "--versionType".to_string(), "Herald".to_string(),
        ]);

        Ok(LaunchCommand {
            java_path: self.java_path.display().to_string(),
            args,
            game_dir: self.game_dir.display().to_string(),
        })
    }

    fn resolve_version_id(&self, versions_dir: &Path) -> anyhow::Result<String> {
        if let Some(loader) = &self.loader {
            // 查找 loader 对应的版本目录
            if let Ok(entries) = std::fs::read_dir(versions_dir) {
                for entry in entries.flatten() {
                    let name = entry.file_name().to_string_lossy().to_string();
                    let matches = match loader.as_str() {
                        "fabric" => name.starts_with("fabric-loader-") && name.ends_with(&self.version),
                        "forge" => name.contains(&self.version) && name.contains("forge"),
                        "neoforge" => {
                            let prefix = herald_mcclient_env::loader::neoforge_dir_prefix(&self.version);
                            name.starts_with(&prefix)
                        }
                        _ => false,
                    };
                    if matches {
                        return Ok(name);
                    }
                }
            }
            anyhow::bail!("{} not installed for MC {}", loader, self.version);
        }
        Ok(self.version.clone())
    }

    fn build_classpath(
        &self,
        info: &serde_json::Value,
        vanilla_info: Option<&serde_json::Value>,
        lib_dir: &Path,
        versions_dir: &Path,
    ) -> anyhow::Result<String> {
        let mut paths: Vec<String> = Vec::new();
        let sep = if cfg!(windows) { ";" } else { ":" };

        // Loader libraries (all loaders need them on classpath)
        self.add_libs_to_classpath(info, lib_dir, &mut paths);

        // Vanilla libraries
        if let Some(vanilla) = vanilla_info {
            self.add_libs_to_classpath(vanilla, lib_dir, &mut paths);
        }

        // Client jar (not for Forge/NeoForge - they use BootstrapLauncher to load it)
        let is_forge_like = matches!(self.loader.as_deref(), Some("forge") | Some("neoforge"));
        if !is_forge_like {
            let client_jar = versions_dir.join(&self.version).join(format!("{}.jar", self.version));
            if client_jar.exists() {
                let p = client_jar.display().to_string();
                if !paths.contains(&p) { paths.push(p); }
            }
        }

        Ok(paths.join(sep))
    }

    fn add_libs_to_classpath(&self, info: &serde_json::Value, lib_dir: &Path, paths: &mut Vec<String>) {
        if let Some(libs) = info["libraries"].as_array() {
            for lib in libs {
                // artifact path
                if let Some(path) = lib.pointer("/downloads/artifact/path").and_then(|v| v.as_str()) {
                    let file = lib_dir.join(path);
                    if file.exists() {
                        let p = file.display().to_string();
                        if !paths.contains(&p) { paths.push(p); }
                    }
                } else if let Some(name) = lib["name"].as_str() {
                    // Construct path from Maven coordinate
                    let path = maven_name_to_path(name);
                    let file = lib_dir.join(&path);
                    if file.exists() {
                        let p = file.display().to_string();
                        if !paths.contains(&p) { paths.push(p); }
                    }
                }
            }
        }
    }
}

/// 将 Maven coordinate (group:artifact:version) 转为文件路径。
fn maven_name_to_path(name: &str) -> String {
    let parts: Vec<&str> = name.split(':').collect();
    if parts.len() < 3 { return name.to_string(); }
    let group = parts[0].replace('.', "/");
    let artifact = parts[1];
    let version = parts[2];
    let classifier = if parts.len() > 3 { Some(parts[3]) } else { None };
    match classifier {
        Some(c) => format!("{}/{}/{}/{}-{}-{}.jar", group, artifact, version, artifact, version, c),
        None => format!("{}/{}/{}/{}-{}.jar", group, artifact, version, artifact, version),
    }
}

/// Extract artifact base name from a jar filename.
/// "asm-9.7.jar" → "asm"
/// "bootstraplauncher-2.0.2.jar" → "bootstraplauncher"
/// "securejarhandler-3.0.8.jar" → "securejarhandler"
fn extract_artifact_name(filename: &str) -> Option<String> {
    let name = filename.strip_suffix(".jar")?;
    // Find the last '-' followed by a digit (version separator)
    let bytes = name.as_bytes();
    for i in (0..bytes.len()).rev() {
        if bytes[i] == b'-' && i + 1 < bytes.len() && bytes[i + 1].is_ascii_digit() {
            return Some(name[..i].to_string());
        }
    }
    // No version found, use whole name
    Some(name.to_string())
}

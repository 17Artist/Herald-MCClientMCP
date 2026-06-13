use std::path::Path;
use tracing;

fn client() -> reqwest::Client {
    reqwest::Client::builder()
        .user_agent("Herald-MCClientMCP/0.1.0")
        .timeout(std::time::Duration::from_secs(30))
        .connect_timeout(std::time::Duration::from_secs(10))
        .build()
        .unwrap_or_default()
}

/// 通用文件下载（带可选 SHA1 校验 + 最多3次重试）。
pub async fn download_file(
    url: &str,
    dest: &Path,
    expected_sha1: Option<&str>,
) -> anyhow::Result<()> {
    if let Some(parent) = dest.parent() {
        tokio::fs::create_dir_all(parent).await?;
    }

    let mut last_err = anyhow::anyhow!("download failed");
    for attempt in 0..3u32 {
        if attempt > 0 {
            tracing::debug!("Retry {}/3 for {}", attempt + 1, url);
            tokio::time::sleep(std::time::Duration::from_millis(500 * attempt as u64)).await;
        }
        match try_download(url, dest, expected_sha1).await {
            Ok(()) => return Ok(()),
            Err(e) => {
                if attempt == 2 {
                    tracing::warn!("Download failed after 3 attempts: {} -> {}", url, e);
                }
                last_err = e;
            }
        }
    }
    Err(last_err)
}

async fn try_download(url: &str, dest: &Path, expected_sha1: Option<&str>) -> anyhow::Result<()> {
    let response = client().get(url).send().await?;
    if !response.status().is_success() {
        anyhow::bail!("HTTP {} for {}", response.status(), url);
    }

    let bytes = response.bytes().await?;

    if let Some(expected) = expected_sha1 {
        use sha1::Digest;
        let mut hasher = sha1::Sha1::new();
        hasher.update(&bytes);
        let hash = hex::encode(hasher.finalize());
        if hash != expected {
            anyhow::bail!("SHA1 mismatch for {:?}: expected {}, got {}", dest, expected, hash);
        }
    }

    tokio::fs::write(dest, &bytes).await?;
    Ok(())
}

/// 尽力下载，失败只返回 Err 不 panic。只重试 1 次，用于 assets 等非关键资源。
pub async fn download_file_best_effort(
    url: &str,
    dest: &Path,
    expected_sha1: Option<&str>,
) -> anyhow::Result<()> {
    if let Some(parent) = dest.parent() {
        tokio::fs::create_dir_all(parent).await?;
    }
    for attempt in 0..2u32 {
        if attempt > 0 {
            tokio::time::sleep(std::time::Duration::from_millis(300)).await;
        }
        match try_download(url, dest, expected_sha1).await {
            Ok(()) => return Ok(()),
            Err(_) if attempt == 0 => continue,
            Err(e) => return Err(e),
        }
    }
    Ok(())
}
pub async fn download_file_with_progress<F>(
    url: &str,
    dest: &Path,
    expected_sha1: Option<&str>,
    on_progress: F,
) -> anyhow::Result<()>
where
    F: Fn(u64, Option<u64>),
{
    if let Some(parent) = dest.parent() {
        tokio::fs::create_dir_all(parent).await?;
    }

    let response = client().get(url).send().await?;
    if !response.status().is_success() {
        anyhow::bail!("Download failed: HTTP {} for {}", response.status(), url);
    }

    let total = response.content_length();
    let mut downloaded: u64 = 0;
    let mut bytes_buf = Vec::with_capacity(total.unwrap_or(1024 * 1024) as usize);

    let mut stream = response.bytes_stream();
    use futures::StreamExt;
    while let Some(chunk) = stream.next().await {
        let chunk = chunk?;
        downloaded += chunk.len() as u64;
        bytes_buf.extend_from_slice(&chunk);
        on_progress(downloaded, total);
    }

    if let Some(expected) = expected_sha1 {
        use sha1::Digest;
        let mut hasher = sha1::Sha1::new();
        hasher.update(&bytes_buf);
        let hash = hex::encode(hasher.finalize());
        if hash != expected {
            anyhow::bail!("SHA1 mismatch for {:?}: expected {}, got {}", dest, expected, hash);
        }
    }

    tokio::fs::write(dest, &bytes_buf).await?;
    Ok(())
}

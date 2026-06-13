use std::collections::VecDeque;
use std::sync::{Arc, Mutex};
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::{Child, Command};
use tracing::info;

use crate::args::LaunchArgs;

/// MC 客户端进程状态。
#[derive(Debug, Clone, Copy, PartialEq, Eq, serde::Serialize)]
#[serde(rename_all = "snake_case")]
pub enum ClientStatus {
    Stopped,
    Starting,
    Running,
    Stopping,
}

/// MC 客户端进程信息。
#[derive(Debug, Clone, serde::Serialize)]
pub struct ClientInfo {
    pub status: ClientStatus,
    pub pid: Option<u32>,
    pub version: Option<String>,
    pub loader: Option<String>,
    pub started_at: Option<String>,
}

/// 日志行。
#[derive(Debug, Clone, serde::Serialize)]
pub struct LogLine {
    pub ts: String,
    pub text: String,
}

/// 管理 MC 客户端进程生命周期。
pub struct ClientProcess {
    status: Arc<Mutex<ClientStatus>>,
    child: Arc<Mutex<Option<Child>>>,
    info: Arc<Mutex<ClientInfo>>,
    logs: Arc<Mutex<VecDeque<LogLine>>>,
    max_log_lines: usize,
}

impl ClientProcess {
    pub fn new() -> Self {
        Self {
            status: Arc::new(Mutex::new(ClientStatus::Stopped)),
            child: Arc::new(Mutex::new(None)),
            info: Arc::new(Mutex::new(ClientInfo {
                status: ClientStatus::Stopped,
                pid: None,
                version: None,
                loader: None,
                started_at: None,
            })),
            logs: Arc::new(Mutex::new(VecDeque::with_capacity(5000))),
            max_log_lines: 5000,
        }
    }

    pub fn status(&self) -> ClientStatus {
        *self.status.lock().unwrap()
    }

    pub fn info(&self) -> ClientInfo {
        self.info.lock().unwrap().clone()
    }

    pub fn logs(&self, tail: usize) -> Vec<LogLine> {
        let logs = self.logs.lock().unwrap();
        let start = logs.len().saturating_sub(tail);
        logs.iter().skip(start).cloned().collect()
    }

    /// 启动 MC 客户端。
    pub async fn start(&self, launch_args: LaunchArgs) -> anyhow::Result<()> {
        {
            let s = self.status.lock().unwrap();
            if *s != ClientStatus::Stopped {
                anyhow::bail!("Client is already running or starting");
            }
        }

        // 清空旧日志
        {
            let mut logs = self.logs.lock().unwrap();
            logs.clear();
        }

        *self.status.lock().unwrap() = ClientStatus::Starting;
        self.update_info_status(ClientStatus::Starting);

        let cmd = launch_args.build()?;
        info!("Starting MC client: {} {:?}", cmd.java_path, &cmd.args[..3.min(cmd.args.len())]);

        let mut child = Command::new(&cmd.java_path)
            .args(&cmd.args)
            .current_dir(&cmd.game_dir)
            .stdout(std::process::Stdio::piped())
            .stderr(std::process::Stdio::piped())
            .spawn()?;

        let pid = child.id();

        // 捕获 stdout
        if let Some(stdout) = child.stdout.take() {
            let logs = self.logs.clone();
            let max_lines = self.max_log_lines;
            tokio::spawn(async move {
                let reader = BufReader::new(stdout);
                let mut lines = reader.lines();
                while let Ok(Some(line)) = lines.next_line().await {
                    let log_line = LogLine {
                        ts: chrono_now(),
                        text: line,
                    };
                    let mut buf = logs.lock().unwrap();
                    if buf.len() >= max_lines { buf.pop_front(); }
                    buf.push_back(log_line);
                }
                // stdout EOF — 不在这里判断进程退出，由 wait task 负责
            });
        }

        // 捕获 stderr
        if let Some(stderr) = child.stderr.take() {
            let logs = self.logs.clone();
            let max_lines = self.max_log_lines;
            tokio::spawn(async move {
                let reader = BufReader::new(stderr);
                let mut lines = reader.lines();
                while let Ok(Some(line)) = lines.next_line().await {
                    let log_line = LogLine {
                        ts: chrono_now(),
                        text: format!("[STDERR] {}", line),
                    };
                    let mut buf = logs.lock().unwrap();
                    if buf.len() >= max_lines { buf.pop_front(); }
                    buf.push_back(log_line);
                }
            });
        }

        *self.child.lock().unwrap() = Some(child);
        *self.status.lock().unwrap() = ClientStatus::Running;

        {
            let mut info = self.info.lock().unwrap();
            info.status = ClientStatus::Running;
            info.pid = pid;
            info.version = Some(launch_args.version.clone());
            info.loader = launch_args.loader.clone();
            info.started_at = Some(chrono_now());
        }

        // 独立的进程退出监控 task：通过 child.wait() 检测真正的进程退出
        {
            let status = self.status.clone();
            let info_ref = self.info.clone();
            let child_ref = self.child.clone();
            tokio::spawn(async move {
                loop {
                    tokio::time::sleep(std::time::Duration::from_secs(2)).await;
                    let mut guard = child_ref.lock().unwrap();
                    if let Some(ref mut c) = *guard {
                        // try_wait 非阻塞检查进程是否退出
                        match c.try_wait() {
                            Ok(Some(_exit_status)) => {
                                // 进程已退出
                                drop(guard);
                                *status.lock().unwrap() = ClientStatus::Stopped;
                                {
                                    let mut i = info_ref.lock().unwrap();
                                    i.status = ClientStatus::Stopped;
                                    i.pid = None;
                                }
                                let _ = child_ref.lock().unwrap().take();
                                info!("MC client process exited");
                                break;
                            }
                            Ok(None) => {
                                // 进程还在跑，继续轮询
                            }
                            Err(_) => {
                                // 出错了，假设进程已退出
                                drop(guard);
                                *status.lock().unwrap() = ClientStatus::Stopped;
                                {
                                    let mut i = info_ref.lock().unwrap();
                                    i.status = ClientStatus::Stopped;
                                    i.pid = None;
                                }
                                let _ = child_ref.lock().unwrap().take();
                                info!("MC client process exited (wait error)");
                                break;
                            }
                        }
                    } else {
                        // child 已被 stop() 清理
                        break;
                    }
                }
            });
        }

        info!("MC client started (pid: {:?})", pid);
        Ok(())
    }

    /// 停止 MC 客户端进程。
    /// 如果提供了 graceful_url，先通过 MOD HTTP 发 shutdown 请求让 MC 优雅退出。
    pub async fn stop(&self) -> anyhow::Result<()> {
        self.stop_with_graceful(None).await
    }

    /// 带优雅关闭的停止。graceful_url 是 MOD 的 shutdown_client action URL。
    pub async fn stop_with_graceful(&self, graceful_url: Option<(String, String)>) -> anyhow::Result<()> {
        {
            let status = self.status.lock().unwrap();
            if *status == ClientStatus::Stopped {
                return Ok(());
            }
        }

        *self.status.lock().unwrap() = ClientStatus::Stopping;
        self.update_info_status(ClientStatus::Stopping);

        // 1. 尝试优雅关闭：通过 MOD HTTP 发 shutdown_client
        let mut graceful_success = false;
        if let Some((url, token)) = graceful_url {
            info!("Sending graceful shutdown to MOD...");
            let client = reqwest::Client::builder()
                .timeout(std::time::Duration::from_secs(5))
                .build()
                .unwrap_or_default();
            let _ = client.post(&url)
                .bearer_auth(&token)
                .send()
                .await;

            // 等待进程自行退出（最多 15 秒）
            let start = std::time::Instant::now();
            while start.elapsed() < std::time::Duration::from_secs(15) {
                tokio::time::sleep(std::time::Duration::from_millis(500)).await;
                // 用系统命令检查 PID 是否还存在（比 try_wait 更可靠）
                let pid_alive = if let Some(ref guard) = *self.child.lock().unwrap() {
                    if let Some(pid) = guard.id() {
                        let output = std::process::Command::new("tasklist")
                            .arg("/FI")
                            .arg(format!("PID eq {}", pid))
                            .arg("/NH")
                            .output();
                        match output {
                            Ok(o) => String::from_utf8_lossy(&o.stdout).contains(&pid.to_string()),
                            Err(_) => true,
                        }
                    } else { false }
                } else { false };

                if !pid_alive {
                    graceful_success = true;
                    info!("MC client exited gracefully");
                    break;
                }
            }
        }

        // 2. 如果优雅关闭失败，强制 kill
        if !graceful_success {
            // 从 info 中获取 PID（更可靠）
            let saved_pid = self.info.lock().unwrap().pid;
            let child_opt = self.child.lock().unwrap().take();

            if cfg!(windows) {
                if let Some(pid) = saved_pid {
                    info!("Force killing MC client (pid: {})", pid);
                    let _ = std::process::Command::new("taskkill")
                        .arg("/PID")
                        .arg(pid.to_string())
                        .arg("/F")
                        .arg("/T")
                        .output();
                }
            }

            if let Some(mut child) = child_opt {
                let _ = child.kill().await;
                let _ = child.wait().await;
            }
        } else {
            // 优雅退出了，清理 child 引用
            let _ = self.child.lock().unwrap().take();
        }

        *self.status.lock().unwrap() = ClientStatus::Stopped;
        self.update_info_status(ClientStatus::Stopped);
        info!("MC client stopped");
        Ok(())
    }

    fn update_info_status(&self, status: ClientStatus) {
        let mut info = self.info.lock().unwrap();
        info.status = status;
        if status == ClientStatus::Stopped {
            info.pid = None;
        }
    }
}

impl Default for ClientProcess {
    fn default() -> Self { Self::new() }
}

fn chrono_now() -> String {
    // 简单的时间戳格式
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default();
    format!("{}", now.as_secs())
}

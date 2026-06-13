use parking_lot::RwLock;
use std::collections::HashMap;
use std::sync::Arc;
use uuid::Uuid;

/// 异步任务状态。
#[derive(Debug, Clone, serde::Serialize)]
#[serde(rename_all = "snake_case")]
pub enum TaskStatus {
    Pending,
    Running,
    Done,
    Failed,
}

/// 异步任务信息。
#[derive(Debug, Clone, serde::Serialize)]
pub struct TaskInfo {
    pub id: String,
    pub status: TaskStatus,
    pub label: String,
    pub downloaded: u64,
    pub total: Option<u64>,
    pub error: Option<String>,
}

/// 管理异步下载/安装任务。
#[derive(Clone)]
pub struct TaskManager {
    tasks: Arc<RwLock<HashMap<String, TaskInfo>>>,
}

impl TaskManager {
    pub fn new() -> Self {
        Self {
            tasks: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    /// 创建新任务，返回 task_id。
    pub fn create(&self, label: &str) -> String {
        let id = Uuid::new_v4().to_string();
        let info = TaskInfo {
            id: id.clone(),
            status: TaskStatus::Pending,
            label: label.to_string(),
            downloaded: 0,
            total: None,
            error: None,
        };
        self.tasks.write().insert(id.clone(), info);
        id
    }

    /// 更新任务状态。
    pub fn update(&self, id: &str, status: TaskStatus, downloaded: u64, total: Option<u64>) {
        if let Some(task) = self.tasks.write().get_mut(id) {
            task.status = status;
            task.downloaded = downloaded;
            task.total = total;
        }
    }

    /// 标记任务完成。
    pub fn complete(&self, id: &str) {
        if let Some(task) = self.tasks.write().get_mut(id) {
            task.status = TaskStatus::Done;
        }
    }

    /// 标记任务失败。
    pub fn fail(&self, id: &str, error: &str) {
        if let Some(task) = self.tasks.write().get_mut(id) {
            task.status = TaskStatus::Failed;
            task.error = Some(error.to_string());
        }
    }

    /// 获取任务信息。
    pub fn get(&self, id: &str) -> Option<TaskInfo> {
        self.tasks.read().get(id).cloned()
    }

    /// 获取所有任务。
    pub fn list(&self) -> Vec<TaskInfo> {
        self.tasks.read().values().cloned().collect()
    }

    /// 清除已完成的旧任务。
    pub fn gc(&self) {
        self.tasks.write().retain(|_, t| !matches!(t.status, TaskStatus::Done | TaskStatus::Failed));
    }
}

impl Default for TaskManager {
    fn default() -> Self { Self::new() }
}

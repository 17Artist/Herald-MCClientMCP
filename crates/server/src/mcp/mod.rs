//! MCP Streamable HTTP transport (JSON-RPC 2.0)。
//!
//! 端点: `POST /mcp` — 单条 JSON-RPC 请求 → 单条响应。
//! 无需鉴权（纯本地工具，监听 127.0.0.1）。

pub mod tools;

use axum::{extract::State, Json};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

use crate::state::{ActivityEvent, AppState};

const PROTOCOL_VERSION: &str = "2024-11-05";

#[derive(Deserialize)]
pub struct JsonRpcRequest {
    #[serde(default = "default_jsonrpc")]
    pub jsonrpc: String,
    #[serde(default)]
    pub id: Value,
    pub method: String,
    #[serde(default)]
    pub params: Value,
}

fn default_jsonrpc() -> String { "2.0".to_string() }

#[derive(Serialize)]
pub struct JsonRpcResponse {
    jsonrpc: &'static str,
    id: Value,
    #[serde(skip_serializing_if = "Option::is_none")]
    result: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<JsonRpcError>,
}

#[derive(Serialize)]
pub struct JsonRpcError {
    code: i32,
    message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    data: Option<Value>,
}

/// POST /mcp handler
#[axum::debug_handler]
pub async fn mcp_handler(
    State(state): State<AppState>,
    Json(req): Json<JsonRpcRequest>,
) -> Json<JsonRpcResponse> {
    let result = match req.method.as_str() {
        "initialize" => handle_initialize(),
        "tools/list" => handle_tools_list(),
        "tools/call" => handle_tools_call(&state, &req.params).await,
        _ => Err((-32601, format!("Method not found: {}", req.method))),
    };

    let resp = match result {
        Ok(value) => JsonRpcResponse {
            jsonrpc: "2.0",
            id: req.id,
            result: Some(value),
            error: None,
        },
        Err((code, msg)) => JsonRpcResponse {
            jsonrpc: "2.0",
            id: req.id,
            result: None,
            error: Some(JsonRpcError { code, message: msg, data: None }),
        },
    };

    Json(resp)
}

fn handle_initialize() -> Result<Value, (i32, String)> {
    Ok(json!({
        "protocolVersion": PROTOCOL_VERSION,
        "capabilities": { "tools": {} },
        "serverInfo": {
            "name": "herald-mcclient",
            "version": env!("CARGO_PKG_VERSION")
        },
        "instructions": "Herald MCClientMCP - Minecraft客户端AI自动化调试工具。\n\n## 使用流程（必须按顺序）\n\n1. **mc_env_probe** — 先调用，了解环境（已装Java/MC版本）\n2. **mc_client_start** — 启动MC客户端（指定version+loader），缺少依赖时会返回env_missing错误，根据提示调mc_env_install_*安装\n3. **等待MOD上线** — 轮询 mc_mod_status 直到 online=true（通常60秒内）\n4. **进入世界** — 用 mc_action 操作GUI进入游戏世界：\n   - mc_action(action='gui_click_widget', params={text:'Singleplayer'})\n   - mc_action(action='gui_click_widget', params={text:'Create New World'})\n   - mc_action(action='gui_click_widget', params={text:'Create New World'})\n   - 等待15秒世界生成\n5. **执行操作** — 用 mc_action/mc_query 控制游戏（311个action可用）\n\n## 支持版本\nMC 1.20.1, 1.20.4, 1.21.1, 1.21.4, 1.21.8, 1.21.11, 26.1, 26.1.2\n加载器: Fabric / Forge(1.20.x) / NeoForge(1.21.x+)\n\n## 常用action示例\n- 查询玩家: mc_action(action='query_player_state')\n- 设置创造: mc_action(action='chat_command', params={command:'gamemode creative'})\n- 鼠标移动: mc_action(action='mouse_move', params={dx:10, dy:5})\n- 鼠标点击: mc_action(action='mouse_click', params={x:200, y:200, button:0})\n- 键盘输入: mc_action(action='keyboard_input', params={key:'w', duration_ms:100})\n- 连接服务器: mc_action(action='connect_to_server', params={ip:'play.example.com'})"
    }))
}

fn handle_tools_list() -> Result<Value, (i32, String)> {
    Ok(json!({ "tools": tools::tool_definitions() }))
}

async fn handle_tools_call(state: &AppState, params: &Value) -> Result<Value, (i32, String)> {
    let name = params["name"].as_str()
        .ok_or((-32602, "Missing tool name".to_string()))?;
    let args = params.get("arguments").cloned().unwrap_or(json!({}));

    // Emit activity start
    let activity_id = uuid::Uuid::new_v4().to_string();
    state.activity_bus.emit(ActivityEvent {
        id: activity_id.clone(),
        tool: name.to_string(),
        status: "started".to_string(),
        ts: now_ts(),
        result_preview: None,
    });

    let result = tools::dispatch(state, name, args).await;

    // Emit activity end
    let (status, preview) = match &result {
        Ok(v) => ("completed", v.to_string().chars().take(100).collect::<String>()),
        Err((_, msg)) => ("failed", msg.clone()),
    };
    state.activity_bus.emit(ActivityEvent {
        id: activity_id,
        tool: name.to_string(),
        status: status.to_string(),
        ts: now_ts(),
        result_preview: Some(preview),
    });

    // MCP protocol: tools/call result must be {"content": [...]}
    match result {
        Ok(content_array) => Ok(json!({ "content": content_array })),
        Err(e) => Err(e),
    }
}

fn now_ts() -> u64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()
}

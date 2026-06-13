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
        }
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

    result
}

fn now_ts() -> u64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()
}

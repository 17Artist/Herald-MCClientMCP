# 部署指南

## 接入 MCP 客户端

Herald MCClientMCP 使用 Streamable HTTP 传输协议。在你的 MCP 客户端配置中添加：

```json
{
  "mcpServers": {
    "herald-mcclient": {
      "url": "http://127.0.0.1:8686/mcp"
    }
  }
}
```

如果你的 MCP 客户端仅支持 stdio 子进程模式（如旧版 Claude Desktop），可以用 `npx` 桥接：

```json
{
  "mcpServers": {
    "herald-mcclient": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://127.0.0.1:8686/mcp"]
    }
  }
}
```

---

## 启动服务

```powershell
.\herald-mcclient.exe --config config.toml
```

打开 http://localhost:8686 可访问 Web 面板。

---

## 配置

```toml
[server]
listen = "127.0.0.1:8686"

[mc]
default_version = "1.20.1"
default_loader = "fabric"
heap_mb = 4096
offline_username = "HeraldDev"

[runtime]
mirror = "bmclapi"   # 国内加速：bmclapi / tuna
```

完整示例见 `config.example.toml`。

---

## 端口

| 端口   | 用途                                 |
|------|------------------------------------|
| 8686 | Web 面板 + MCP 协议（Streamable HTTP）   |
| 8888 | MOD 内部 API（loopback，Token 鉴权，无需开放） |

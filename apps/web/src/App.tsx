import { useEffect, useState } from "react"
import { Logo } from "./assets/Logo"
import { useStore } from "./lib/store"
import { ClientPanel } from "./panels/ClientPanel"
import { EnvironmentPanel } from "./panels/EnvironmentPanel"
import { GamePanel } from "./panels/GamePanel"
import { ModsPanel } from "./panels/ModsPanel"
import { StatusPanel } from "./panels/StatusPanel"

type Tab = "client" | "environment" | "mods"

export function App() {
  const [tab, setTab] = useState<Tab>("client")
  const startPolling = useStore(s => s.startPolling)
  const stopPolling = useStore(s => s.stopPolling)
  const status = useStore(s => s.status)

  useEffect(() => {
    startPolling()
    return () => stopPolling()
  }, [startPolling, stopPolling])

  const clientRunning = status?.client.status === 'running'
  const modOnline = status?.mod.online

  return (
    <div className="h-full grid grid-cols-[200px_1fr] grid-rows-[auto_1fr] fade-in">
      {/* Header */}
      <header className="col-span-2 border-b border-ink-800/80 px-5 py-2.5 flex items-center gap-3">
        <Logo size={24} />
        <div className="leading-tight">
          <div className="text-sm font-semibold text-ink-100">Herald MCClientMCP</div>
          <div className="text-[11px] text-ink-400">Client debug bridge</div>
        </div>
        <div className="ml-auto flex items-center gap-3">
          <StatusPill label="客户端" online={clientRunning} />
          <StatusPill label="MOD" online={modOnline} />
        </div>
      </header>

      {/* Sidebar */}
      <nav className="border-r border-ink-800/80 p-3 flex flex-col gap-1 text-sm">
        <NavItem active={tab === "client"} onClick={() => setTab("client")} label="客户端" hint="控制台" />
        <NavItem active={tab === "environment"} onClick={() => setTab("environment")} label="环境管家" hint="Java/MC" />
        <NavItem active={tab === "mods"} onClick={() => setTab("mods")} label="MOD" hint="管理" />
      </nav>

      {/* Content */}
      <main className="overflow-hidden">
        {tab === "client" && <ClientView modOnline={modOnline} />}
        {tab === "environment" && <div className="p-5 overflow-auto h-full"><EnvironmentPanel /></div>}
        {tab === "mods" && <div className="p-5 overflow-auto h-full"><ModsPanel /></div>}
      </main>
    </div>
  )
}

/**
 * Client tab layout:
 * ┌─────────────────────┬────────────────┐
 * │  MCP Activity       │                │
 * ├─────────────────────┤   日志终端      │
 * │  MC Game Screen     │                │
 * │                     │                │
 * └─────────────────────┴────────────────┘
 */
function ClientView({ modOnline }: { modOnline?: boolean }) {
  return (
    <div className="h-full grid grid-cols-[1.4fr_1fr] gap-0">
      {/* Left: MCP activity + game */}
      <div className="h-full flex flex-col border-r border-ink-800/80 min-w-0">
        {/* MCP Activity - fixed height */}
        <div className="h-[180px] shrink-0 border-b border-ink-800/80 p-3 overflow-auto">
          <StatusPanel />
        </div>
        {/* Game screen - vertically centered */}
        <div className="flex-1 min-h-0 p-3 flex items-center justify-center">
          {modOnline ? <GamePanel /> : (
            <div className="w-full h-full rounded-xl border border-ink-800 bg-ink-900/30 flex items-center justify-center">
              <div className="text-center">
                <div className="text-ink-500 text-sm">游戏画面</div>
                <div className="text-ink-600 text-xs mt-1">MOD 在线后自动显示</div>
              </div>
            </div>
          )}
        </div>
      </div>
      {/* Right: log terminal */}
      <div className="h-full p-3 min-w-0 overflow-hidden">
        <ClientPanel />
      </div>
    </div>
  )
}

function NavItem({ active, onClick, label, hint }: {
  active?: boolean; onClick?: () => void; label: string; hint?: string
}) {
  return (
    <button
      onClick={onClick}
      className={`px-3 py-2 rounded-md text-left flex items-center gap-2 transition-colors ${
        active
          ? "bg-violet-500/15 text-violet-100 border border-violet-500/30"
          : "text-ink-200 hover:bg-ink-800/60 border border-transparent"
      }`}
    >
      <span>{label}</span>
      {hint && (
        <span className={`ml-auto text-[10px] uppercase tracking-wider px-1.5 py-0.5 rounded ${
          active ? "bg-violet-500/20 text-violet-200" : "bg-ink-800 text-ink-400"
        }`}>
          {hint}
        </span>
      )}
    </button>
  )
}

function StatusPill({ label, online }: { label: string; online?: boolean }) {
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] border ${
      online
        ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-300"
        : "bg-ink-900/50 border-ink-700 text-ink-400"
    }`}>
      <span className={`w-1.5 h-1.5 rounded-full ${online ? 'bg-emerald-400' : 'bg-ink-600'}`} />
      {label}
    </span>
  )
}

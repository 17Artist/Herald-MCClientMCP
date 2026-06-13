import { useEffect, useRef, useState } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';
import { api } from '../lib/api';
import { useStore } from '../lib/store';

export function ClientPanel() {
  const status = useStore(s => s.status);
  const fetchStatus = useStore(s => s.fetchStatus);
  const [starting, setStarting] = useState(false);
  const [stopping, setStopping] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const clientInfo = status?.client;
  const st = clientInfo?.status ?? 'stopped';
  const isRunning = st === 'running';
  const isStopped = st === 'stopped';
  const busy = st === 'starting' || st === 'stopping';

  const handleStart = async () => {
    setStarting(true); setError(null);
    try {
      const res = await api.startClient();
      if (!res.ok) setError(res.error || 'Start failed');
      else await fetchStatus();
    } catch (e) { setError((e as Error).message); }
    setStarting(false);
  };

  const handleStop = async () => {
    setStopping(true); setError(null);
    try {
      const res = await api.stopClient();
      if (!res.ok) setError(res.error || 'Stop failed');
      else await fetchStatus();
    } catch (e) { setError((e as Error).message); }
    setStopping(false);
  };

  return (
    <div className="h-full flex flex-col gap-3 min-w-0">
      {/* Header + controls */}
      <div className="flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3 min-w-0">
          <span className={`w-2 h-2 rounded-full shrink-0 ${STATUS_DOT[st]} ${busy ? "pulse-dot" : ""}`} />
          <span className="text-sm font-medium text-ink-100 shrink-0">{STATUS_LABEL[st]}</span>
          {clientInfo?.version && (
            <span className="text-[11px] text-ink-500 font-mono truncate">
              {clientInfo.version} · {clientInfo.loader} · PID {clientInfo.pid}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <button className="btn-primary text-[11px] !py-1.5 !px-3" disabled={starting || busy || isRunning} onClick={handleStart}>
            {starting ? '启动中…' : '启动'}
          </button>
          <button className="btn-ghost text-[11px] !py-1.5 !px-3" disabled={stopping || busy || isStopped} onClick={handleStop}>
            {stopping ? '停止中…' : '停止'}
          </button>
        </div>
      </div>

      {error && <div className="alert-error text-xs shrink-0">{error}</div>}

      {/* Terminal */}
      <div className="flex-1 min-h-0">
        <ConsoleTerm running={isRunning} />
      </div>
    </div>
  );
}

const STATUS_LABEL: Record<string, string> = {
  stopped: "已停止", starting: "启动中", running: "运行中", stopping: "停止中",
};
const STATUS_DOT: Record<string, string> = {
  stopped: "bg-ink-500", starting: "bg-amber-400", running: "bg-emerald-400", stopping: "bg-amber-400",
};

function ConsoleTerm({ running }: { running: boolean }) {
  const wrapRef = useRef<HTMLDivElement | null>(null);
  const termRef = useRef<Terminal | null>(null);
  const fitRef = useRef<FitAddon | null>(null);
  const lastLenRef = useRef(0);

  useEffect(() => {
    if (!wrapRef.current) return;
    const term = new Terminal({
      fontFamily: '"JetBrains Mono", "Cascadia Code", ui-monospace, monospace',
      fontSize: 11,
      theme: {
        background: "#0a0a0c",
        foreground: "#e4e4e7",
        cursor: "#a78bfa",
        selectionBackground: "rgba(167,139,250,0.25)",
        black: "#27272d", red: "#fca5a5", green: "#86efac",
        yellow: "#fcd34d", blue: "#7dd3fc", magenta: "#c4b5fd",
        cyan: "#67e8f9", white: "#e4e4e7",
      },
      convertEol: true,
      scrollback: 5000,
      disableStdin: true,
    });
    const fit = new FitAddon();
    term.loadAddon(fit);
    term.open(wrapRef.current);
    fit.fit();
    termRef.current = term;
    fitRef.current = fit;

    const ro = new ResizeObserver(() => fit.fit());
    ro.observe(wrapRef.current);
    return () => { ro.disconnect(); term.dispose(); };
  }, []);

  useEffect(() => {
    if (!running) return;
    const poll = async () => {
      try {
        const data = await api.getClientLogs();
        const term = termRef.current;
        if (!term) return;
        const logs = data.logs;
        if (logs.length < lastLenRef.current) { term.clear(); lastLenRef.current = 0; }
        for (let i = lastLenRef.current; i < logs.length; i++) {
          const line = logs[i];
          const text = (line as unknown as { text: string }).text ?? String(line);
          term.writeln(text);
        }
        lastLenRef.current = logs.length;
      } catch (_) { /* ignore */ }
    };
    poll();
    const timer = setInterval(poll, 1500);
    return () => clearInterval(timer);
  }, [running]);

  return (
    <div className="h-full border border-ink-800 rounded-lg overflow-hidden bg-[#0a0a0c]">
      <div ref={wrapRef} className="h-full px-2 py-1" />
    </div>
  );
}

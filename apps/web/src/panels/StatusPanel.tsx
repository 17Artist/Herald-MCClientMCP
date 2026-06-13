import { useEffect, useRef, useState } from 'react';

interface ActivityEvent {
  id: string;
  tool: string;
  status: string;
  ts: number;
  result_preview: string | null;
}

/**
 * MCP Activity panel - compact mode for embedding in client view.
 * Fixes: deduplicates events by ID so completed events replace running ones.
 */
export function StatusPanel() {
  const [eventsMap, setEventsMap] = useState<Map<string, ActivityEvent>>(new Map());
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const ws = new WebSocket(`${proto}//${location.host}/ws`);
    wsRef.current = ws;
    ws.onmessage = (e) => {
      try {
        const event: ActivityEvent = JSON.parse(e.data);
        setEventsMap(prev => {
          const next = new Map(prev);
          next.set(event.id, event);
          // Keep only last 50
          if (next.size > 50) {
            const keys = [...next.keys()];
            for (let i = 0; i < keys.length - 50; i++) next.delete(keys[i]);
          }
          return next;
        });
      } catch (_) { /* ignore */ }
    };
    return () => { ws.close(); };
  }, []);

  const events = [...eventsMap.values()];
  const activeEvents = events.filter(e => e.status === 'started');
  const completedEvents = events.filter(e => e.status !== 'started');

  return (
    <div className="space-y-2">
      {/* Header */}
      <div className="flex items-center gap-2">
        <span className="text-[10px] font-semibold text-ink-400 uppercase tracking-wider">MCP 活动</span>
        {activeEvents.length > 0 && (
          <span className="mcp-nav-indicator">
            <span className="mcp-nav-dot" />
            {activeEvents.length} active
          </span>
        )}
      </div>

      {/* Active calls */}
      {activeEvents.length > 0 && (
        <div className="space-y-1">
          {activeEvents.map(item => (
            <div key={item.id} className="mcp-active-bar rounded-lg px-3 py-2 flex items-center gap-2.5">
              <div className="mcp-pulse" style={{ width: 20, height: 20 }}>
                <div className="core" style={{ inset: 6 }} />
              </div>
              <span className="mcp-tool-name text-xs font-mono">{item.tool}</span>
              <span className="text-[10px] font-mono text-violet-200/60 ml-auto">running…</span>
            </div>
          ))}
        </div>
      )}

      {/* Completed history */}
      {completedEvents.length > 0 && (
        <div className="space-y-0.5 max-h-[120px] overflow-auto">
          {[...completedEvents].reverse().slice(0, 15).map(ev => (
            <div key={ev.id} className="flex items-center gap-2 text-[11px] font-mono px-1 py-0.5 rounded hover:bg-ink-800/40 transition-colors">
              <span className={`w-2 h-2 rounded-full shrink-0 ${
                ev.status === 'completed' ? 'bg-emerald-400' : 'bg-red-400'
              }`} />
              <span className="text-ink-200 truncate">{ev.tool}</span>
              <span className={`text-[10px] shrink-0 ${ev.status === 'completed' ? 'text-emerald-400/70' : 'text-red-400/70'}`}>
                {ev.status === 'completed' ? 'ok' : 'fail'}
              </span>
              <time className="text-ink-600 ml-auto shrink-0">{formatTs(ev.ts)}</time>
            </div>
          ))}
        </div>
      )}

      {/* Empty state */}
      {events.length === 0 && (
        <div className="text-ink-600 text-[11px] py-2">AI 控制台空闲</div>
      )}
    </div>
  );
}

function formatTs(ts: number): string {
  const d = new Date(ts * 1000);
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

const BASE = '';

async function get<T>(url: string): Promise<T> {
  const res = await fetch(BASE + url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

async function post<T>(url: string, body?: unknown): Promise<T> {
  const res = await fetch(BASE + url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

// ─── Types ──────────────────────────────────────────────────

export interface ClientInfo {
  status: 'stopped' | 'starting' | 'running' | 'stopping';
  pid: number | null;
  version: string | null;
  loader: string | null;
  started_at: string | null;
}

export interface ModStatus {
  online: boolean;
  port: number | null;
  mod_version: string | null;
}

export interface JavaInfo {
  path: string;
  major_version: number;
  vendor: string;
}

export interface LogLine {
  ts: string;
  text: string;
}

export interface StatusResponse {
  client: ClientInfo;
  mod: ModStatus;
  config: {
    game_dir: string;
    mc_version: string;
    loader: string;
    heap_mb: number;
    username: string;
  };
}

export interface McStatus {
  default_version: string;
  default_installed: boolean;
  installed_versions: string[];
  game_dir: string;
}

export interface ModFile {
  name: string;
  size: number;
  is_herald: boolean;
}

// ─── API ────────────────────────────────────────────────────

export const api = {
  getStatus: () => get<StatusResponse>('/api/status'),
  getJavaList: () => get<{ javas: JavaInfo[] }>('/api/env/java'),
  installJava: (major?: number) => post<{ ok: boolean; error?: string }>('/api/env/java/install', { major }),
  getMcStatus: () => get<McStatus>('/api/env/mc/status'),
  installMc: (version?: string) => post<{ ok: boolean; error?: string }>('/api/env/mc/install', { version }),
  installLoader: (loader: string, mc_version?: string) =>
    post<{ ok: boolean; error?: string }>('/api/env/loader/install', { loader, mc_version }),
  getClientInfo: () => get<ClientInfo>('/api/client/info'),
  getClientLogs: () => get<{ logs: LogLine[] }>('/api/client/logs'),
  startClient: (opts?: { version?: string; loader?: string; heap_mb?: number; username?: string }) =>
    post<{ ok: boolean; info?: ClientInfo; error?: string }>('/api/client/start', opts ?? {}),
  stopClient: () => post<{ ok: boolean; error?: string }>('/api/client/stop'),
  getModStatus: () => get<ModStatus>('/api/mod/status'),
  getModActions: () => get<{ actions: string[]; error?: string }>('/api/mod/actions'),
  getModFiles: () => get<{ mods_dir: string; files: ModFile[] }>('/api/mod/list'),
};

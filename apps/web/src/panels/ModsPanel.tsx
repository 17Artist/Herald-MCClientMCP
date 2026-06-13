import { useEffect, useState } from 'react';
import { api, type ModStatus, type ModFile } from '../lib/api';

export function ModsPanel() {
  const [modStatus, setModStatus] = useState<ModStatus | null>(null);
  const [modFiles, setModFiles] = useState<ModFile[]>([]);
  const [modsDir, setModsDir] = useState('');
  const [actions, setActions] = useState<string[]>([]);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 5000);
    return () => clearInterval(timer);
  }, []);

  const refresh = async () => {
    try {
      const [statusRes, filesRes] = await Promise.all([api.getModStatus(), api.getModFiles()]);
      setModStatus(statusRes);
      setModFiles(filesRes.files);
      setModsDir(filesRes.mods_dir);
    } catch (_) { /* ignore */ }
  };

  const fetchActions = async () => {
    try {
      const res = await api.getModActions();
      setActions(res.actions);
    } catch (_) { /* ignore */ }
  };

  useEffect(() => {
    if (modStatus?.online) fetchActions();
  }, [modStatus?.online]);

  return (
    <div className="space-y-4 fade-in">
      <h2 className="text-lg font-semibold">MOD 管理</h2>

      {/* Herald MOD 状态 */}
      <div className="glass rounded-lg p-4">
        <div className="flex items-center gap-3 mb-2">
          <div className={`w-2.5 h-2.5 rounded-full ${modStatus?.online ? 'bg-green-400' : 'bg-ink-500'}`} />
          <div className="text-sm font-medium">Herald Client MOD</div>
          <span className="text-xs text-ink-400 ml-auto">
            {modStatus?.online ? `在线 · 端口 ${modStatus.port}` : '离线'}
          </span>
        </div>
        {modStatus?.mod_version && (
          <div className="text-xs text-ink-400">版本: {modStatus.mod_version}</div>
        )}
      </div>

      {/* Actions 列表 */}
      {actions.length > 0 && (
        <div className="glass rounded-lg p-4">
          <div className="text-xs text-ink-400 mb-2 font-medium uppercase tracking-wider">
            可用 Actions ({actions.length})
          </div>
          <div className="flex flex-wrap gap-1.5">
            {actions.map(a => (
              <span key={a} className="px-2 py-0.5 rounded bg-ink-800 text-ink-300 text-xs font-mono">{a}</span>
            ))}
          </div>
        </div>
      )}

      {/* Mods 文件列表 */}
      <div className="glass rounded-lg p-4">
        <div className="text-xs text-ink-400 mb-2 font-medium uppercase tracking-wider">
          Mods 目录
        </div>
        <div className="text-xs text-ink-500 mb-2 truncate">{modsDir}</div>
        {modFiles.length === 0 ? (
          <div className="text-sm text-ink-400">目录为空</div>
        ) : (
          <div className="space-y-1">
            {modFiles.map((f, i) => (
              <div key={i} className="flex items-center gap-2 text-xs">
                <span className={`w-2 h-2 rounded-full ${f.is_herald ? 'bg-purple-400' : 'bg-ink-600'}`} />
                <span className={`font-mono ${f.is_herald ? 'text-purple-300' : 'text-ink-300'}`}>{f.name}</span>
                <span className="text-ink-500 ml-auto">{formatSize(f.size)}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

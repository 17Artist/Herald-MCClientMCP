import { useEffect, useState } from 'react';
import { api, type JavaInfo, type McStatus } from '../lib/api';

export function EnvironmentPanel() {
  const [javas, setJavas] = useState<JavaInfo[]>([]);
  const [mcStatus, setMcStatus] = useState<McStatus | null>(null);
  const [installing, setInstalling] = useState<string | null>(null);
  const [message, setMessage] = useState<{ text: string; ok: boolean } | null>(null);

  useEffect(() => { refresh(); }, []);

  const refresh = async () => {
    try {
      const [javaRes, mcRes] = await Promise.all([api.getJavaList(), api.getMcStatus()]);
      setJavas(javaRes.javas);
      setMcStatus(mcRes);
    } catch (_) { /* ignore */ }
  };

  const doInstall = async (key: string, fn: () => Promise<{ ok: boolean; error?: string }>, successMsg: string) => {
    setInstalling(key); setMessage(null);
    try {
      const res = await fn();
      setMessage({ text: res.ok ? successMsg : `失败: ${res.error}`, ok: res.ok });
    } catch (e) { setMessage({ text: (e as Error).message, ok: false }); }
    setInstalling(null);
    refresh();
  };

  const hasJava17 = javas.some(j => j.major_version >= 17);

  return (
    <div className="space-y-4">
      <header>
        <h2 className="text-lg font-semibold text-ink-100">环境管家</h2>
        <p className="text-xs text-ink-400 mt-0.5">管理 Java 运行时、MC 客户端及 Mod 加载器</p>
      </header>

      {message && (
        <div className={`text-sm rounded-lg px-3 py-2 ${message.ok ? 'bg-emerald-900/20 border border-emerald-700/40 text-emerald-300' : 'alert-error'}`}>
          {message.text}
        </div>
      )}

      {/* Java */}
      <section className="glass rounded-xl px-4 py-4">
        <div className="flex items-center justify-between mb-3">
          <div>
            <div className="text-sm font-medium text-ink-100">Java 运行时</div>
            <div className="text-[11px] text-ink-400 mt-0.5">Architectury Loom 与 MC 1.20.1 需要 Java 17+</div>
          </div>
          <button
            className="btn-ghost text-xs"
            disabled={installing === 'java'}
            onClick={() => doInstall('java', () => api.installJava(17), 'Java 17 安装完成')}
          >
            {installing === 'java' ? '下载中…' : '安装 Java 17'}
          </button>
        </div>

        {javas.length === 0 ? (
          <div className="text-sm text-ink-500">未检测到 Java 安装</div>
        ) : (
          <div className="space-y-1.5">
            {javas.map((j, i) => (
              <div key={i} className="flex items-center gap-2 text-xs">
                <span className={`px-1.5 py-0.5 rounded font-mono text-[11px] ${
                  j.major_version >= 17
                    ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30'
                    : 'bg-ink-800 text-ink-300'
                }`}>
                  {j.major_version}
                </span>
                <span className="text-ink-400">{j.vendor || 'Unknown'}</span>
                <span className="text-ink-600 truncate ml-auto max-w-[340px] font-mono text-[11px]">{j.path}</span>
              </div>
            ))}
          </div>
        )}

        {!hasJava17 && javas.length > 0 && (
          <div className="mt-2 text-[11px] text-amber-400/80 flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-400" />
            未找到 Java 17+，请安装后再启动客户端
          </div>
        )}
      </section>

      {/* Minecraft */}
      <section className="glass rounded-xl px-4 py-4">
        <div className="flex items-center justify-between mb-3">
          <div>
            <div className="text-sm font-medium text-ink-100">Minecraft 客户端</div>
            <div className="text-[11px] text-ink-400 mt-0.5">从 Mojang/BMCLAPI 下载游戏本体</div>
          </div>
          <button
            className="btn-ghost text-xs"
            disabled={installing === 'mc'}
            onClick={() => doInstall('mc', () => api.installMc(), `MC ${mcStatus?.default_version} 下载完成`)}
          >
            {installing === 'mc' ? '下载中…' : `下载 ${mcStatus?.default_version || '1.20.1'}`}
          </button>
        </div>

        <div className="flex items-center gap-3 text-sm">
          <span className="text-ink-400">默认版本</span>
          <code className={`font-mono ${mcStatus?.default_installed ? 'text-emerald-300' : 'text-amber-300'}`}>
            {mcStatus?.default_version || '1.20.1'}
          </code>
          {mcStatus?.default_installed ? (
            <span className="text-[10px] uppercase tracking-wider px-1.5 py-0.5 rounded bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">已安装</span>
          ) : (
            <span className="text-[10px] uppercase tracking-wider px-1.5 py-0.5 rounded bg-amber-500/15 text-amber-300 border border-amber-500/30">未安装</span>
          )}
        </div>

        {mcStatus && mcStatus.installed_versions.length > 0 && (
          <div className="mt-2 text-[11px] text-ink-500">
            已安装版本: {mcStatus.installed_versions.join(', ')}
          </div>
        )}
      </section>

      {/* Loaders */}
      <section className="glass rounded-xl px-4 py-4">
        <div className="mb-3">
          <div className="text-sm font-medium text-ink-100">Mod 加载器</div>
          <div className="text-[11px] text-ink-400 mt-0.5">安装 Fabric / Forge / NeoForge 到游戏目录</div>
        </div>
        <div className="flex gap-2">
          {(['fabric', 'forge', 'neoforge'] as const).map(loader => (
            <button
              key={loader}
              className="btn-ghost text-xs capitalize"
              disabled={installing === loader}
              onClick={() => doInstall(loader, () => api.installLoader(loader), `${loader} 安装完成`)}
            >
              {installing === loader ? '安装中…' : loader}
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}

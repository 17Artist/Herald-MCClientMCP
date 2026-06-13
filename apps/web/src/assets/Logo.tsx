import logoUrl from "./logo.svg";

/**
 * Herald MCClientMCP Logo
 *
 * 等距 MC 立方体 + AI 观测双眼 + 操控弧线 + 印鉴断口环。
 * 颜色与 Herald 系列共用紫蓝渐变（c4b5fd → a78bfa → 38bdf8）。
 */
export function Logo({
  size = 28,
  className = "",
}: {
  size?: number;
  className?: string;
}) {
  return (
    <img
      src={logoUrl}
      width={size}
      height={size}
      alt="Herald MCClientMCP"
      className={`select-none drop-shadow-[0_0_12px_rgba(167,139,250,0.35)] ${className}`}
      draggable={false}
    />
  );
}

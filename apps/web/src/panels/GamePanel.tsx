import { useEffect, useRef, useState, useCallback } from 'react';
import { useStore } from '../lib/store';

/**
 * Game screen panel: connects to MOD MJPEG stream, renders frames
 * on a <canvas>, and forwards keyboard/mouse input back to the MOD.
 */
export function GamePanel() {
  const status = useStore(s => s.status);
  const modPort = status?.mod.port;
  const modOnline = status?.mod.online;

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [connected, setConnected] = useState(false);
  const [fps, setFps] = useState(0);
  const [resolution, setResolution] = useState('');
  const abortRef = useRef<AbortController | null>(null);
  const frameCountRef = useRef(0);
  const fpsTimerRef = useRef(0);

  const streamUrl = modPort ? `http://${location.hostname}:${modPort}/stream` : null;
  const inputUrl = modPort ? `http://${location.hostname}:${modPort}/stream/input` : null;

  // Connect to MJPEG stream via fetch ReadableStream
  useEffect(() => {
    if (!streamUrl || !modOnline) return;

    const ac = new AbortController();
    abortRef.current = ac;
    let running = true;

    const connect = async () => {
      try {
        const res = await fetch(streamUrl, { signal: ac.signal });
        if (!res.ok || !res.body) return;
        setConnected(true);

        const reader = res.body.getReader();
        let buffer = new Uint8Array(0);

        while (running) {
          const { done, value } = await reader.read();
          if (done) break;

          // Append chunk to buffer
          const tmp = new Uint8Array(buffer.length + value.length);
          tmp.set(buffer);
          tmp.set(value, buffer.length);
          buffer = tmp;

          // Extract JPEG frames from MJPEG multipart
          while (true) {
            const jpegStart = findBytes(buffer, [0xFF, 0xD8]);
            if (jpegStart < 0) break;
            const jpegEnd = findBytes(buffer, [0xFF, 0xD9], jpegStart + 2);
            if (jpegEnd < 0) break;

            const frameEnd = jpegEnd + 2;
            const frame = buffer.slice(jpegStart, frameEnd);
            buffer = buffer.slice(frameEnd);

            // Render frame
            renderFrame(frame);
          }

          // Prevent buffer from growing unbounded
          if (buffer.length > 500000) {
            buffer = buffer.slice(buffer.length - 100000);
          }
        }
      } catch (e) {
        if ((e as Error).name !== 'AbortError') {
          setConnected(false);
          // Retry after delay
          if (running) setTimeout(connect, 2000);
        }
      }
    };

    connect();

    // FPS counter
    fpsTimerRef.current = window.setInterval(() => {
      setFps(frameCountRef.current);
      frameCountRef.current = 0;
    }, 1000);

    return () => {
      running = false;
      ac.abort();
      setConnected(false);
      clearInterval(fpsTimerRef.current);
    };
  }, [streamUrl, modOnline]);

  const renderFrame = useCallback((data: Uint8Array) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const blob = new Blob([data.buffer as ArrayBuffer], { type: 'image/jpeg' });
    createImageBitmap(blob).then(bmp => {
      if (canvas.width !== bmp.width || canvas.height !== bmp.height) {
        canvas.width = bmp.width;
        canvas.height = bmp.height;
        setResolution(`${bmp.width}×${bmp.height}`);
      }
      const ctx = canvas.getContext('2d');
      if (ctx) ctx.drawImage(bmp, 0, 0);
      bmp.close();
      frameCountRef.current++;
    });
  }, []);

  // Input forwarding
  const sendInput = useCallback((msg: object) => {
    if (!inputUrl) return;
    fetch(inputUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(msg),
    }).catch(() => {});
  }, [inputUrl]);

  const onMouseMove = useCallback((e: React.MouseEvent) => {
    const rect = (e.target as HTMLElement).getBoundingClientRect();
    sendInput({ type: 'mousemove', x: e.clientX - rect.left, y: e.clientY - rect.top, sw: rect.width, sh: rect.height });
  }, [sendInput]);

  const onMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    sendInput({ type: 'mousedown', button: e.button });
  }, [sendInput]);

  const onMouseUp = useCallback((e: React.MouseEvent) => {
    sendInput({ type: 'mouseup', button: e.button });
  }, [sendInput]);

  const onWheel = useCallback((e: React.WheelEvent) => {
    e.preventDefault();
    sendInput({ type: 'scroll', dx: e.deltaX / 120, dy: -e.deltaY / 120 });
  }, [sendInput]);

  const onKeyDown = useCallback((e: React.KeyboardEvent) => {
    e.preventDefault();
    sendInput({ type: 'keydown', keyCode: mapKey(e.code), scanCode: 0 });
  }, [sendInput]);

  const onKeyUp = useCallback((e: React.KeyboardEvent) => {
    e.preventDefault();
    sendInput({ type: 'keyup', keyCode: mapKey(e.code), scanCode: 0 });
  }, [sendInput]);

  if (!modOnline) return null;

  return (
    <div className="relative rounded-xl overflow-hidden border border-ink-800 bg-black w-full max-h-full aspect-video">
      <canvas
        ref={canvasRef}
        className="w-full h-full object-contain cursor-crosshair outline-none"
        tabIndex={0}
        onMouseMove={onMouseMove}
        onMouseDown={onMouseDown}
        onMouseUp={onMouseUp}
        onWheel={onWheel}
        onKeyDown={onKeyDown}
        onKeyUp={onKeyUp}
        onContextMenu={e => e.preventDefault()}
      />
      {/* Overlay */}
      <div className="absolute top-2 right-2 bg-black/70 text-emerald-400 text-[11px] font-mono px-2 py-0.5 rounded pointer-events-none">
        {connected ? `${fps} FPS · ${resolution}` : 'Connecting...'}
      </div>
    </div>
  );
}

function findBytes(haystack: Uint8Array, needle: number[], offset = 0): number {
  for (let i = offset; i <= haystack.length - needle.length; i++) {
    let match = true;
    for (let j = 0; j < needle.length; j++) {
      if (haystack[i + j] !== needle[j]) { match = false; break; }
    }
    if (match) return i;
  }
  return -1;
}

function mapKey(code: string): number {
  const map: Record<string, number> = {
    KeyW: 87, KeyA: 65, KeyS: 83, KeyD: 68,
    Space: 32, ShiftLeft: 340, ShiftRight: 344,
    ControlLeft: 341, ControlRight: 345,
    Escape: 256, Enter: 257, Tab: 258, Backspace: 259,
    ArrowUp: 265, ArrowDown: 264, ArrowLeft: 263, ArrowRight: 262,
    KeyE: 69, KeyQ: 81, KeyF: 70, KeyR: 82, KeyT: 84,
    Digit1: 49, Digit2: 50, Digit3: 51, Digit4: 52,
    Digit5: 53, Digit6: 54, Digit7: 55, Digit8: 56, Digit9: 57,
    F1: 290, F2: 291, F3: 292, F4: 293, F5: 294, F11: 300, F12: 301,
    Slash: 47, Period: 46, Comma: 44,
  };
  return map[code] || code.charCodeAt(code.length - 1);
}

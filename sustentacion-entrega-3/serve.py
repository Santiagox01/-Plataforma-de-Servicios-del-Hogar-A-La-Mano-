"""
Local & container HTTP server for A La Mano — Sustentacion Entrega #3.

Serves static presentation files from sustentacion-entrega-3/, exposes GET /health
and GET /api/live-categorias (proxying Railway PostgreSQL 18.7 server-side without
weakening Railway production CORS), and blocks access to server/container files.
"""

from __future__ import annotations

import functools
import json
import os
import time
import urllib.error
import urllib.request
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

HOST = os.environ.get("HOST", "0.0.0.0")
PORT = int(os.environ.get("PORT", "8765"))
RAILWAY_CATEGORIES_URL = os.environ.get(
    "BACKEND_CATEGORIAS_URL",
    "https://plataforma-de-servicios-del-hogar-a-la-mano-production.up.railway.app"
    "/api/categorias",
)
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
BLOCKED_PATHS = {"/serve.py", "/dockerfile", "/.dockerignore"}


class DeckRequestHandler(SimpleHTTPRequestHandler):
    """Serves static presentation assets and live health/backend proxy endpoints."""

    def _is_blocked(self, clean_path: str) -> bool:
        low = clean_path.lower()
        return (
            low in BLOCKED_PATHS
            or "__pycache__" in low
            or low.endswith(".pyc")
        )

    def do_HEAD(self) -> None:
        clean_path = self.path.split("?", 1)[0]
        if self._is_blocked(clean_path):
            self.send_error(404, "Not Found")
            return
        super().do_HEAD()

    def do_GET(self) -> None:
        clean_path = self.path.split("?", 1)[0]
        if self._is_blocked(clean_path):
            self.send_error(404, "Not Found")
            return
        if clean_path == "/health":
            self._send_json(200, {"status": "ok"})
            return
        if clean_path == "/api/live-categorias":
            self._handle_live_categorias()
            return
        super().do_GET()

    def _send_json(self, status_code: int, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _handle_live_categorias(self) -> None:
        started = time.perf_counter()
        req = urllib.request.Request(
            RAILWAY_CATEGORIES_URL,
            headers={"Accept": "application/json"},
        )
        try:
            with urllib.request.urlopen(req, timeout=8) as resp:
                raw = resp.read().decode("utf-8")
                latency_ms = int((time.perf_counter() - started) * 1000)
                parsed = json.loads(raw)
                items = (
                    parsed
                    if isinstance(parsed, list)
                    else (parsed.get("content", []) if isinstance(parsed, dict) else [])
                )
                self._send_json(
                    200,
                    {
                        "ok": True,
                        "status": resp.status,
                        "latencyMs": latency_ms,
                        "source": "railway-live",
                        "categorias": items,
                    },
                )
        except urllib.error.HTTPError as exc:
            latency_ms = int((time.perf_counter() - started) * 1000)
            self._send_json(
                502,
                {
                    "ok": False,
                    "status": exc.code,
                    "latencyMs": latency_ms,
                    "error": f"HTTP {exc.code}: {exc.reason}",
                },
            )
        except Exception as exc:  # noqa: BLE001
            latency_ms = int((time.perf_counter() - started) * 1000)
            self._send_json(
                502,
                {
                    "ok": False,
                    "status": 502,
                    "latencyMs": latency_ms,
                    "error": str(exc),
                },
            )


def main() -> None:
    handler = functools.partial(DeckRequestHandler, directory=BASE_DIR)
    server = ThreadingHTTPServer((HOST, PORT), handler)
    print(f"Serving A La Mano Entrega 3 on http://{HOST}:{PORT}/ (dir={BASE_DIR})", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()

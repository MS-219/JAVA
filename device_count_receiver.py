#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Temporary Python 2 receiver used to count device record replays safely."""

import BaseHTTPServer
import json
import os
import threading
import time
import urlparse

PORT = 15390
OUTPUT_FILE = "/root/yin-lian/device_count_records.jsonl"
LOCK = threading.Lock()
STATE = {"count": 0, "window": ""}


class Handler(BaseHTTPServer.BaseHTTPRequestHandler):
    def _send_json(self, payload):
        body = json.dumps(payload, ensure_ascii=False)
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.end_headers()
        if isinstance(body, unicode):
            body = body.encode("utf-8")
        self.wfile.write(body)

    def do_GET(self):
        parsed = urlparse.urlparse(self.path)
        if parsed.path == "/clear":
            qs = urlparse.parse_qs(parsed.query)
            window = qs.get("window", [""])[0]
            with LOCK:
                STATE["count"] = 0
                STATE["window"] = window
                open(OUTPUT_FILE, "w").close()
            self._send_json({"result": 0, "count": 0, "window": window})
            return

        with LOCK:
            payload = {"result": 0, "count": STATE["count"], "window": STATE["window"]}
        self._send_json(payload)

    def do_POST(self):
        try:
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length)
            record = json.loads(body)
            with LOCK:
                record["_audit_window"] = STATE["window"]
                line = json.dumps(record, ensure_ascii=False, separators=(",", ":"))
                if isinstance(line, unicode):
                    line = line.encode("utf-8")
                with open(OUTPUT_FILE, "a") as f:
                    f.write(line + "\n")
                STATE["count"] += 1
            self._send_json({"message": "ok", "result": 0})
        except Exception as exc:
            # Reply success so the device does not retry the same payload forever.
            self._send_json({"message": "ok", "result": 0, "warning": str(exc)})

    def log_message(self, fmt, *args):
        return


def main():
    if not os.path.exists(os.path.dirname(OUTPUT_FILE)):
        os.makedirs(os.path.dirname(OUTPUT_FILE))
    open(OUTPUT_FILE, "a").close()
    server = BaseHTTPServer.HTTPServer(("0.0.0.0", PORT), Handler)
    print("device_count_receiver listening on %d at %s" % (PORT, time.strftime("%Y-%m-%d %H:%M:%S")))
    server.serve_forever()


if __name__ == "__main__":
    main()

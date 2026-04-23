#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
临时 HTTP 服务 - 接收设备推送的识别记录，保存为 JSON 文件
Python 2.7 兼容
"""

import BaseHTTPServer
import json
import os
import sys
import threading
import time

LISTEN_PORT = 15389
OUTPUT_FILE = "/root/yin-lian/collected_records.json"
LOCK = threading.Lock()
RECORD_COUNT = [0]

class RecordHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length)
            record = json.loads(body)

            # 保存记录
            with LOCK:
                # 追加到文件
                with open(OUTPUT_FILE, "a") as f:
                    f.write(json.dumps(record, ensure_ascii=False).encode('utf-8') + "\n")
                RECORD_COUNT[0] += 1

                name = record.get("name", "unknown")
                time_str = record.get("time", "")
                mac = record.get("Mac_addr", "")
                emp = record.get("employee_number", "")
                if isinstance(name, unicode):
                    name = name.encode('utf-8')
                print("[%d] %s | %s | %s | %s" % (RECORD_COUNT[0], time_str, name, emp, mac[-5:] if mac else ""))

            # 回复设备 (必须回复 result:0 否则设备会重复推送)
            response = json.dumps({"message": "ok", "result": 0})
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(response)
        except Exception as e:
            print("Error: %s" % str(e))
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write('{"message":"ok","result":0}')

    def do_GET(self):
        """返回当前收集状态"""
        response = json.dumps({"count": RECORD_COUNT[0]})
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(response)

    def log_message(self, format, *args):
        pass  # 静默日志

def main():
    # 清空旧文件
    if os.path.exists(OUTPUT_FILE):
        os.remove(OUTPUT_FILE)

    server = BaseHTTPServer.HTTPServer(("0.0.0.0", LISTEN_PORT), RecordHandler)
    print("=== Temp Record Receiver ===")
    print("Listening on port %d" % LISTEN_PORT)
    print("Records will be saved to: %s" % OUTPUT_FILE)
    print("Press Ctrl+C to stop")
    print("=" * 40)

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped. Total records: %d" % RECORD_COUNT[0])
        server.server_close()

if __name__ == "__main__":
    main()

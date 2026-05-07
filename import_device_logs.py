#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
从设备日志中提取识别记录，并回灌到大屏数据库。

思路:
1. 直接拉取设备暴露的 /log/YYYYMMDD.log 日志文件
2. 提取日志里的 "record data:{...}" JSON 块
3. 按 device + person + time 去重
4. 调用大屏现有 /device/record 接口入库

兼容 Python 2.7 / 3.x，方便直接在服务器运行。
"""

from __future__ import print_function

import argparse
import io
import datetime
import json
import sys
import time

try:
    from urllib.request import Request, urlopen
    from urllib.error import HTTPError, URLError
except ImportError:
    from urllib2 import Request, urlopen, HTTPError, URLError

try:
    text_type = unicode
except NameError:
    text_type = str


DEFAULT_DEVICES = [
    ("10.20.250.45", "61:B2", "大门东-中间 出口"),
    ("10.20.250.43", "61:CD", "大门东-东侧 入口"),
    ("10.20.250.44", "61:A5", "大门东-西侧 入口"),
    ("10.20.250.40", "61:D2", "大门西-东侧 入口"),
    ("10.20.250.37", "61:CF", "大门西-西侧 出口"),
    ("10.20.250.42", "61:D5", "大门西-中间 入口"),
]


def parse_args():
    parser = argparse.ArgumentParser(description="从设备日志恢复识别记录到大屏数据库")
    parser.add_argument("--start-date", default="2026-04-17", help="开始日期, 例如 2026-04-17")
    parser.add_argument("--end-date", default=datetime.date.today().isoformat(), help="结束日期, 例如 2026-04-24")
    parser.add_argument("--api-url", default="http://127.0.0.1:5389/device/record", help="大屏入库接口")
    parser.add_argument("--timeout", type=int, default=20, help="HTTP 超时秒数")
    parser.add_argument("--pause", type=float, default=0.05, help="每条导入后的暂停秒数")
    parser.add_argument("--dry-run", action="store_true", help="只统计，不写入数据库")
    parser.add_argument("--sql-output", default="", help="输出 SQL 文件路径，只生成 SQL，不直接调用接口")
    parser.add_argument("--only-device", action="append", default=[], help="只处理指定设备 IP，可重复传入")
    return parser.parse_args()


def daterange(start_date, end_date):
    start = datetime.datetime.strptime(start_date, "%Y-%m-%d").date()
    end = datetime.datetime.strptime(end_date, "%Y-%m-%d").date()
    current = start
    while current <= end:
        yield current
        current += datetime.timedelta(days=1)


def http_get_text(url, timeout):
    try:
        resp = urlopen(url, timeout=timeout)
        data = resp.read()
        if not isinstance(data, type(u"")):
            data = data.decode("utf-8", "ignore")
        return data
    except HTTPError as exc:
        if exc.code == 404:
            return None
        raise
    except URLError:
        return None


def http_post_json(url, payload, timeout):
    body = json.dumps(payload).encode("utf-8")
    req = Request(url, data=body, headers={"Content-Type": "application/json"})
    resp = urlopen(req, timeout=timeout)
    data = resp.read()
    if not isinstance(data, type(u"")):
        data = data.decode("utf-8", "ignore")
    return data


def extract_json_blocks(text):
    marker = "record data:{"
    blocks = []
    index = 0
    length = len(text)

    while True:
        pos = text.find(marker, index)
        if pos < 0:
            break

        start = text.find("{", pos)
        if start < 0:
            break

        depth = 0
        end = None
        cursor = start
        while cursor < length:
            char = text[cursor]
            if char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    end = cursor + 1
                    break
            cursor += 1

        if end is None:
            break

        blocks.append(text[start:end])
        index = end

    return blocks


def normalize_text(value):
    if value is None:
        return ""
    if isinstance(value, bytes):
        return value.decode("utf-8", "ignore").strip()
    return str(value).strip()


def record_key(record):
    device = normalize_text(record.get("Mac_addr") or record.get("dev_sno") or record.get("SN"))
    person = normalize_text(
        record.get("employee_number")
        or record.get("userid")
        or record.get("person_id")
        or record.get("userId")
        or record.get("id")
    )
    capture_time = normalize_text(record.get("time") or record.get("capture_time"))
    return device.upper(), person, capture_time


def in_range(record_time, start_date, end_date):
    if not record_time:
        return False
    return start_date + " 00:00:00" <= record_time <= end_date + " 23:59:59"


def collect_records(devices, start_date, end_date, timeout):
    records = []
    seen = set()
    file_hits = []

    for device_ip, _, device_name in devices:
        for current in daterange(start_date, end_date):
            day_token = current.strftime("%Y%m%d")
            url = "http://%s/log/%s.log" % (device_ip, day_token)
            text = http_get_text(url, timeout)
            if not text:
                continue

            file_hits.append((device_ip, device_name, day_token))
            for block in extract_json_blocks(text):
                try:
                    record = json.loads(block)
                except Exception:
                    continue

                event_time = normalize_text(record.get("time") or record.get("capture_time"))
                if not in_range(event_time, start_date, end_date):
                    continue

                key = record_key(record)
                if not key[0] or not key[2]:
                    continue
                if key in seen:
                    continue

                seen.add(key)
                records.append(record)

    return records, file_hits


def summarize(records):
    by_day = {}
    by_device = {}
    for record in records:
        event_time = normalize_text(record.get("time") or record.get("capture_time"))
        day = event_time[:10]
        device = normalize_text(record.get("Mac_addr") or record.get("dev_sno") or record.get("SN")).upper()
        device_suffix = device[-5:] if len(device) >= 5 else device

        by_day[day] = by_day.get(day, 0) + 1
        by_device[device_suffix] = by_device.get(device_suffix, 0) + 1

    return by_day, by_device


def escape_sql(value):
    text = normalize_text(value)
    return text.replace("'", "''")


def enrich_for_storage(record):
    device, person, capture_time = record_key(record)
    enriched = dict(record)
    enriched["__deviceCode"] = device
    enriched["__personCode"] = person
    enriched["__captureTime"] = capture_time
    enriched["status"] = "imported"
    return enriched


def write_sql(records, output_path):
    with io.open(output_path, "w", encoding="utf-8") as handle:
        handle.write(text_type("SET AUTOCOMMIT FALSE;\n"))
        for record in records:
            enriched = enrich_for_storage(record)
            device = escape_sql(enriched.get("__deviceCode"))
            person = escape_sql(enriched.get("__personCode"))
            capture_time = escape_sql(enriched.get("__captureTime"))
            raw_json = json.dumps(enriched, ensure_ascii=False, separators=(",", ":"))
            raw_json = raw_json.replace("'", "''")
            sql = text_type(
                "INSERT INTO ACCESS_RECORD (CAPTURE_TIME, CREATED_AT, DEVICE_CODE, PERSON_CODE, RAW_JSON, STATUS) "
                "SELECT '%s', CURRENT_TIMESTAMP, '%s', '%s', '%s', 'imported' "
                "WHERE NOT EXISTS (SELECT 1 FROM ACCESS_RECORD WHERE DEVICE_CODE='%s' AND PERSON_CODE='%s' AND CAPTURE_TIME='%s');\n"
                % (capture_time, device, person, raw_json, device, person, capture_time)
            )
            handle.write(sql)
        handle.write(text_type("COMMIT;\n"))


def import_records(records, api_url, timeout, pause):
    success = 0
    failed = 0
    samples = []

    for idx, record in enumerate(records, 1):
        try:
            http_post_json(api_url, record, timeout)
            success += 1
        except Exception as exc:
            failed += 1
            if len(samples) < 10:
                samples.append("%s | %s" % (record.get("time", ""), exc))

        if pause > 0:
            time.sleep(pause)

        if idx % 100 == 0:
            print("已处理 %d / %d" % (idx, len(records)))

    return success, failed, samples


def main():
    args = parse_args()
    devices = DEFAULT_DEVICES
    if args.only_device:
        allowed = set(args.only_device)
        devices = [item for item in devices if item[0] in allowed]

    print("开始扫描设备日志")
    print("时间范围: %s ~ %s" % (args.start_date, args.end_date))
    print("设备数: %d" % len(devices))

    records, file_hits = collect_records(devices, args.start_date, args.end_date, args.timeout)
    by_day, by_device = summarize(records)

    print("")
    print("命中的日志文件: %d" % len(file_hits))
    for device_ip, device_name, day_token in file_hits:
        print("  %s %s %s" % (device_ip, day_token, device_name))

    print("")
    print("可恢复唯一记录: %d" % len(records))
    print("按天统计:")
    for day in sorted(by_day):
        print("  %s %d" % (day, by_day[day]))

    print("按设备统计:")
    for device in sorted(by_device):
        print("  %s %d" % (device, by_device[device]))

    if args.dry_run:
        print("")
        print("dry-run 模式，不写入数据库")
        return 0

    if args.sql_output:
        write_sql(records, args.sql_output)
        print("")
        print("SQL 已生成: %s" % args.sql_output)
        return 0

    print("")
    print("开始写入数据库: %s" % args.api_url)
    success, failed, samples = import_records(records, args.api_url, args.timeout, args.pause)
    print("写入完成: success=%d failed=%d" % (success, failed))
    if samples:
        print("失败样例:")
        for item in samples:
            print("  %s" % item)
    return 0


if __name__ == "__main__":
    sys.exit(main())

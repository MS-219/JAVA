#!/bin/bash
# 第二轮: 重新触发重提, 等待时间更长

TEMP_URL="http://10.10.245.252:15389/record/upload/revert"
ORIG_URL="http://10.10.245.252:5389/record/upload/online"
PASSWORD="123456"
START_TIME="2026-04-17 00:00:00"
END_TIME="2026-04-23 23:59:59"

DEVICES=("10.20.250.45" "10.20.250.43" "10.20.250.44" "10.20.250.40" "10.20.250.37" "10.20.250.42")
NAMES=("大门东-中间出口" "大门东-东侧入口" "大门东-西侧入口" "大门西-东侧入口" "大门西-西侧出口" "大门西-中间入口")

echo "=============================="
echo "步骤1: 改设备 platformIp"
echo "=============================="
for i in "${!DEVICES[@]}"; do
    IP="${DEVICES[$i]}"
    echo -n "  ${NAMES[$i]} ($IP) ... "
    RESP=$(curl -s --connect-timeout 5 --max-time 10 -X POST "http://$IP:8091/setIdentifyCallBck" \
        -H "Content-Type: application/json" \
        -d "{\"password\":\"$PASSWORD\",\"platformEnable\":1,\"platformIp\":\"$TEMP_URL\"}" 2>&1)
    R=$(echo "$RESP" | grep -o '"result" : [0-9]' | grep -o '[0-9]')
    [ "$R" = "0" ] && echo "✅" || echo "❌ $RESP"
    sleep 0.3
done

echo ""
echo "=============================="
echo "步骤2: 触发记录重提"
echo "=============================="
for i in "${!DEVICES[@]}"; do
    IP="${DEVICES[$i]}"
    echo -n "  ${NAMES[$i]} ($IP) ... "
    RESP=$(curl -s --connect-timeout 5 --max-time 15 -X POST "http://$IP:8091/setDeviceRecordRevert" \
        -H "Content-Type: application/json" \
        -d "{\"password\":\"$PASSWORD\",\"data\":{\"startTime\":\"$START_TIME\",\"endTime\":\"$END_TIME\"}}" 2>&1)
    R=$(echo "$RESP" | grep -o '"result" : [0-9]' | grep -o '[0-9]')
    [ "$R" = "0" ] && echo "✅" || echo "❌ $RESP"
    sleep 0.5
done

echo ""
echo "=============================="
echo "步骤3: 等待 (最多10分钟, 连续60秒无新记录则结束)"
echo "=============================="
LAST_COUNT=0
STABLE=0
for i in $(seq 1 120); do
    sleep 5
    COUNT=$(curl -s --connect-timeout 3 "http://10.10.245.252:15389/" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('count',0))" 2>/dev/null || echo "?")
    echo "  [$((i*5))s] 收到: $COUNT 条"
    if [ "$COUNT" = "$LAST_COUNT" ] && [ "$COUNT" != "0" ] && [ "$COUNT" != "?" ]; then
        STABLE=$((STABLE+1))
        if [ $STABLE -ge 12 ]; then
            echo "  >>> 连续60秒无新记录, 推送完成!"
            break
        fi
    else
        STABLE=0
    fi
    LAST_COUNT=$COUNT
done

echo ""
echo "总计收到: $LAST_COUNT 条"

echo ""
echo "=============================="
echo "步骤4: 恢复设备 platformIp"
echo "=============================="
for i in "${!DEVICES[@]}"; do
    IP="${DEVICES[$i]}"
    echo -n "  ${NAMES[$i]} ($IP) ... "
    RESP=$(curl -s --connect-timeout 10 --max-time 15 -X POST "http://$IP:8091/setIdentifyCallBck" \
        -H "Content-Type: application/json" \
        -d "{\"password\":\"$PASSWORD\",\"platformEnable\":1,\"platformIp\":\"$ORIG_URL\"}" 2>&1)
    R=$(echo "$RESP" | grep -o '"result" : [0-9]' | grep -o '[0-9]')
    [ "$R" = "0" ] && echo "✅" || echo "❌ $RESP"
    sleep 0.5
done

echo ""
echo "====== 第二轮完成! ======"

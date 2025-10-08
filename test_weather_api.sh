#!/bin/bash

# WeatherController API 测试脚本
# 使用方法: ./test_weather_api.sh

BASE_URL="http://localhost:8080"

echo "=== WeatherController API 测试 ==="
echo

# 测试工具状态
echo "1. 测试工具状态..."
curl -s "$BASE_URL/api/weather/tool/status" | jq .
echo

# 测试工具信息
echo "2. 测试工具信息..."
curl -s "$BASE_URL/api/weather/tool/info" | jq .
echo

# 测试当前天气查询
echo "3. 测试当前天气查询..."
curl -s "$BASE_URL/api/weather/current?city=北京" | jq .
echo

# 测试天气预报查询
echo "4. 测试天气预报查询..."
curl -s "$BASE_URL/api/weather/forecast?city=上海" | jq .
echo

# 测试智能天气查询
echo "5. 测试智能天气查询..."
curl -s -X POST "$BASE_URL/api/weather/query" \
  -H "Content-Type: application/json" \
  -d '{"question": "北京今天天气怎么样？"}' | jq .
echo

# 测试流式天气查询
echo "6. 测试流式天气查询..."
echo "正在获取流式天气数据..."
curl -s "$BASE_URL/api/weather/current/stream?city=广州" | head -20
echo

echo "=== 测试完成 ==="

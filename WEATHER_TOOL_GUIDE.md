# 天气工具使用示例

## 智能问答系统 - WeatherTool 使用指南

### 功能概述

WeatherTool 是一个基于 MCP (Model Context Protocol) 的天气查询工具，可以智能识别用户的问题并自动调用天气API获取信息。

### 支持的功能

1. **当前天气查询** - 获取指定城市的实时天气信息
2. **天气预报查询** - 获取指定城市的未来几天天气预报
3. **智能城市识别** - 自动从用户问题中提取城市名称
4. **多语言支持** - 支持中英文城市名称

### 使用方法

#### 1. 直接API调用

**获取当前天气：**
```bash
curl "http://localhost:8080/api/weather/current?city=北京"
```

**获取天气预报：**
```bash
curl "http://localhost:8080/api/weather/forecast?city=上海"
```

**流式获取天气信息：**
```bash
curl "http://localhost:8080/api/weather/current/stream?city=北京"
```

**智能天气查询：**
```bash
curl -X POST http://localhost:8080/api/weather/query \
  -H "Content-Type: application/json" \
  -d '{"question": "北京今天天气怎么样？"}'
```

**流式智能天气查询：**
```bash
curl -X POST http://localhost:8080/api/weather/query/stream \
  -H "Content-Type: application/json" \
  -d '{"question": "上海未来几天的天气预报"}'
```

#### 2. 智能问答调用

在聊天界面中直接提问，系统会自动识别并调用天气工具：

**示例问题：**
- "北京今天天气怎么样？"
- "上海未来几天的天气预报"
- "广州现在下雨吗？"
- "What's the weather like in Beijing?"
- "深圳的温度是多少？"

#### 3. 工具信息查询

**获取工具状态：**
```bash
curl http://localhost:8080/api/weather/tool/status
```

**获取工具详细信息：**
```bash
curl http://localhost:8080/api/weather/tool/info
```

### 配置说明

在 `application.yml` 中配置 OpenWeatherMap API Key：

```yaml
weather:
  api:
    key: your-openweathermap-api-key-here
    base-url: https://api.openweathermap.org/data/2.5
```

### 支持的城市

工具支持全球主要城市，包括：

**中国城市：**
- 北京 (Beijing)
- 上海 (Shanghai)
- 广州 (Guangzhou)
- 深圳 (Shenzhen)
- 杭州 (Hangzhou)
- 南京 (Nanjing)
- 武汉 (Wuhan)
- 成都 (Chengdu)
- 西安 (Xian)
- 重庆 (Chongqing)

**国际城市：**
- New York
- London
- Tokyo
- Paris
- Sydney
- 等等...

### 响应格式

**当前天气响应示例：**
```
📍 城市：北京
🌡️ 温度：15°C
🌡️ 体感温度：13°C
💧 湿度：65%
📊 气压：1013 hPa
☁️ 天气：多云
💨 风速：3.2 m/s
👁️ 能见度：10000 m
```

**天气预报响应示例：**
```
📍 城市：上海

📅 未来5天天气预报：

🕐 2024-01-15 12:00:00
🌡️ 温度：18°C
💧 湿度：70%
☁️ 天气：晴
💨 风速：2.1 m/s

🕐 2024-01-15 15:00:00
🌡️ 温度：20°C
💧 湿度：65%
☁️ 天气：晴
💨 风速：2.5 m/s
```

### 错误处理

工具会自动处理各种错误情况：

1. **无效城市名称** - 返回友好的错误提示
2. **API调用失败** - 显示网络或服务错误信息
3. **API Key无效** - 提示配置问题
4. **网络超时** - 自动重试或返回超时提示

### 扩展开发

如需添加新的天气数据源或功能，可以：

1. 继承或修改 `WeatherTool` 类
2. 添加新的API端点
3. 更新城市识别逻辑
4. 扩展响应格式

### 注意事项

1. **API限制** - OpenWeatherMap 免费版本有调用次数限制
2. **城市名称** - 建议使用标准的城市名称以获得最佳结果
3. **网络依赖** - 需要稳定的网络连接访问天气API
4. **时区** - 天气数据基于UTC时间，可能需要时区转换

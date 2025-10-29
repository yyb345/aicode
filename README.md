# 智能问答系统

这是一个基于Spring Boot的智能问答系统，支持多种AI服务提供商，包括OpenAI和Spring AI Alibaba，支持流式输出和Web界面交互。

## 功能特性

- 🤖 支持多种AI服务提供商（OpenAI + Spring AI Alibaba）
- 🌊 流式输出，实时显示AI回答
- 💬 现代化的Web聊天界面
- ⚡ 响应式设计，支持移动端
- 🔧 易于配置和部署
- 🔄 动态切换AI服务提供商
- 🌤️ 智能工具调用（天气查询）
- 🛠️ MCP (Model Context Protocol) 支持

## 技术栈

- **后端**: Spring Boot 3.2.0 + Spring AI + Spring AI Alibaba
- **前端**: 原生HTML + CSS + JavaScript
- **AI模型**: OpenAI GPT-3.5-turbo / 阿里云通义千问
- **Java版本**: 17

## 快速开始

### 1. 环境准备

- Java 17+
- Maven 3.6+
- OpenAI API Key（可选）
- 阿里云DashScope API Key（可选）

### 2. 配置API Key

**获取OpenAI API Key**
1. 访问 [OpenAI官网](https://platform.openai.com/)
2. 登录你的账户
3. 进入 API Keys 页面
4. 创建新的API Key（格式通常为 `sk-...`）

**配置方法**

修改 `src/main/resources/application.yml` 文件：

```yaml
# OpenAI配置
openai:
  api-key: sk-your-actual-openai-api-key-here  # 替换为你的实际API Key
  base-url: https://api.openai.com/v1
  model: gpt-3.5-turbo

# Spring AI Alibaba配置
spring:
  ai:
    alibaba:
      endpoint: https://dashscope.aliyuncs.com/api/v1
      api-key: your-dashscope-api-key-here  # 替换为你的实际API Key

```

**环境变量方式**
```bash
export OPENAI_API_KEY=your-openai-api-key-here
export ALIBABA_API_KEY=your-dashscope-api-key-here
```

### 3. 运行应用

```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

### 4. 访问应用

打开浏览器访问：http://localhost:8080

## API接口

### OpenAI服务接口

**流式问答接口**
- **POST** `/api/qa/stream`

**普通问答接口**
- **POST** `/api/qa/ask`

### Spring AI Alibaba服务接口

**流式问答接口**
- **POST** `/api/qa/spring-ai/stream`

**普通问答接口**
- **POST** `/api/qa/spring-ai/ask`

**获取支持的模型列表**
- **GET** `/api/qa/spring-ai/models`

**检查服务状态**
- **GET** `/api/qa/spring-ai/status`

### 请求格式

所有问答接口的请求体格式相同：
```json
{
  "question": "你的问题"
}
```

### 响应格式

**流式接口响应**: Server-Sent Events (SSE) 流式数据

**普通接口响应**:
```json
{
  "answer": "AI的回答"
}
```

**模型列表响应**:
```json
{
  "models": ["qwen-turbo", "qwen-plus", "qwen-max"],
  "status": "success"
}
```

**服务状态响应**:
```json
{
  "available": true,
  "status": "success",
  "message": "服务正常"
}
```


## 项目结构

```
src/
├── main/
│   ├── java/com/example/qa/
│   │   ├── QAApplication.java          # 应用启动类
│   │   ├── controller/
│   │   │   └── QAController.java       # 问答控制器
│   │   ├── service/
│   │   │   ├── OpenAIService.java      # OpenAI服务实现
│   │   │   └── SpringAIService.java     # Spring AI Alibaba服务实现
│   │   └── tool/
│   └── resources/
│       ├── application.yml             # 应用配置
│       └── static/
│           ├── index.html              # 前端页面
│           ├── script.js               # 前端JavaScript
│           └── styles.css              # 前端样式
```

## 配置说明

### OpenAI配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `openai.api-key` | OpenAI API密钥 | `your-api-key-here` |
| `openai.base-url` | API基础URL | `https://api.openai.com/v1` |
| `openai.model` | 使用的模型 | `gpt-3.5-turbo` |

### Spring AI Alibaba配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.ai.alibaba.api-key` | DashScope API密钥 | `your-dashscope-api-key` |
| `spring.ai.alibaba.endpoint` | API端点 | `https://dashscope.aliyuncs.com/api/v1` |

### 服务器配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | `8080` |
| `server.servlet.context-path` | 上下文路径 | `/` |

## 开发说明

### 添加新的AI服务提供商

1. 在 `service` 包下创建新的服务类
2. 实现与 `OpenAIService` 和 `SpringAIService` 相同的接口方法
3. 在 `QAController.java` 中注入并使用新的服务
4. 更新配置文件添加相应的配置项
5. 更新前端页面添加服务选择选项

### 添加新的工具

1. 在 `tool` 包下创建新的工具类，使用 `@Component` 注解
2. 实现工具的核心功能方法
3. 在 `QAController.java` 中添加工具调用逻辑
4. 更新 `shouldCallTool()` 方法添加工具检测逻辑
5. 更新前端页面显示新工具的信息

### 自定义前端样式

修改 `src/main/resources/static/styles.css` 文件来自定义界面外观。

### 添加新功能

- 在 `qa` 包下创建新的Controller类
- 使用 `@RestController` 和 `@RequestMapping` 注解
- 实现相应的业务逻辑

## 故障排除

### 常见问题

1. **API Key错误**
   - 检查OpenAI或DashScope API Key是否正确配置
   - 确认API Key有足够的额度
   - 验证API Key的权限设置

2. **依赖下载失败**
   - 检查网络连接
   - 确认Maven仓库配置正确
   - 尝试使用阿里云Maven镜像

3. **Java版本不兼容**
   - 确认使用Java 17或更高版本
   - 检查JAVA_HOME环境变量

4. **Spring AI Alibaba服务不可用**
   - 检查DashScope API Key是否正确
   - 确认网络可以访问阿里云服务
   - 查看应用日志获取详细错误信息

5. **前端服务切换不生效**
   - 检查浏览器控制台是否有JavaScript错误
   - 确认后端服务正常运行
   - 验证API端点是否正确


### 日志调试

启用调试日志：

```yaml
logging:
  level:
    com.example.qa: DEBUG
    org.springframework.ai: DEBUG
    com.alibaba.cloud: DEBUG
    root: INFO
```

### 测试服务状态

可以通过以下API端点测试服务状态：

```bash
# 测试Spring AI Alibaba服务状态
curl http://localhost:8080/api/qa/spring-ai/status

# 获取支持的模型列表
curl http://localhost:8080/api/qa/spring-ai/models

```

## 许可证

MIT License

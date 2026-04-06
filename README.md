# Life Narrative Archive

当前仓库已完成并验证的范围：

- 前端：`Vue 3 + Vite + TypeScript` 工作台界面
- 后端：`Spring Boot + JWT + H2` 演示持久化
- 已打通链路：注册/登录、档案列表、档案详情、新建档案、追加时间线
- AI 当前状态：`/api/rag/query` 仍是占位返回，尚未接入真实检索链路

## 目录结构

- `frontend/`：Vue 前端应用
- `backend/`：Spring Boot 后端应用
- `data/`：历史原型数据与本地数据目录
- `修改计划.md`：目标架构与迁移方向说明
- `开发清单.md`：分阶段工程执行清单

## 后端说明

### 已实现接口

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/archives`
- `GET /api/archives/{archiveId}`
- `POST /api/archives`
- `POST /api/archives/{archiveId}/timeline`
- `POST /api/rag/query`（当前为占位实现）

### 运行默认配置

后端当前按 OpenAI 兼容方式接 Kimi：

- `OPENAI_BASE_URL=https://api.moonshot.ai/v1`
- `OPENAI_CHAT_MODEL=kimi-k2.5`
- `OPENAI_API_KEY=<运行时注入>`

不要把 API Key 写入仓库。

### 启动后端

要求：

- JDK 21+
- Maven 3.9+

示例：

```powershell
$env:OPENAI_API_KEY='your-runtime-key'
$env:OPENAI_BASE_URL='https://api.moonshot.ai/v1'
$env:OPENAI_CHAT_MODEL='kimi-k2.5'
mvn spring-boot:run
```

### 测试后端

```powershell
mvn clean test
```

## 前端说明

### 已实现界面

- 登录 / 注册入口
- 基于 JWT 的会话恢复
- 档案搜索与列表视图
- 档案详情面板
- 新建档案表单
- 时间线补录表单
- 工作台概览指标区

### 启动前端

要求：

- Node.js 20+
- npm

```powershell
npm install
npm run dev
```

如果开发环境下前后端不是同源部署，请显式配置 `VITE_API_BASE_URL`。

### 测试前端

```powershell
npm test
npm run build
```

## 本轮已完成验证

- 后端集成测试通过：`mvn clean test`
- 前端组件测试通过：`npm test`
- 前端生产构建通过：`npm run build`
- 本地真实服务烟测通过，验证了以下主链路：
  - 注册用户
  - 拉取档案列表
  - 查看种子档案详情
  - 新建档案
  - 追加时间线

## 当前已知缺口

- RAG 服务仍是 scaffold，并非真实检索回答
- 素材上传 / OCR / ASR / 任务编排尚未实现
- 还没有引入正式 migration 工具，当前 schema 仍依赖 SQL 初始化文件
# 校园二手物品共享平台 · Codex 项目配置

## 项目概况
前后端分离的校园二手交易平台：
- `backend/` — Spring Boot 3.3 + Java 17 + MyBatis-Plus + JWT + MySQL
- `frontend/` — Vue 3 + Vite + Element Plus + Pinia + ECharts

## 技能自动调用规则

| 场景 | 触发条件 | 自动使用的技能 |
|------|---------|---------------|
| **新增功能** | 用户说"新增/添加/实现/做"任何功能 | `/brainstorming` 先讨论思路，再写代码 |
| **后端开发** | 修改/新增 Controller/Service/Entity/DTO/Mapper | `/backend-dev` 遵循项目后端规范 |
| **前端开发** | 修改/新增 Vue 页面/组件/API 模块/路由/样式 | `/frontend-dev` 遵循项目前端规范 |
| **UI/设计** | 涉及配色、字体、排版、CSS、页面布局 | `/ui-ux-pro-max` 获取设计资源 |
| **前端视觉** | "好不好看"/"美化"/"调整样式"/"设计感" | `/frontend-design` 指导视觉方向 |
| **代码审查** | "review"/"检查代码"/"有什么问题" | `/code-review` 自动审查 |
| **方案质疑** | 方案定稿前、重大架构决策 | `/grill-me` 灵魂拷问验证方案 |
| **实施规划** | 复杂任务需要分步规划、生成实施计划 | `/planning-with-files` 生成结构化计划文件 |
| **数据库变更** | 新增/修改表结构、字段、索引、种子数据 | `/db-migration` 遵循数据库规范 |
| **启动项目** | "启动"/"运行"/"重启"/"停止"项目 | `/run-dev` 管理开发环境 |

## 编码约定
- 后端：Java 17，必须在 JDK 17 环境下编译运行
- 前端：npm 包管理，构建命令 `npm run build`
- 后端启动前设置 `JAVA_HOME=D:\java\jdk17`
- 主题色 `#10B981`（翠绿），强调色 `#F59E0B`（暖橙），背景 `#F0F9F4`

## 禁止事项
- 不要引入不必要的第三方依赖
- 后端不要硬编码敏感信息在代码中

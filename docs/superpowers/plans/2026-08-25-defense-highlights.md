# 答辩亮点技术文档 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 依据规格 `docs/superpowers/specs/2026-08-25-defense-highlights-design.md`，撰写 `docs/答辩亮点技术文档.md`（全模块覆盖、方案+代码级答辩素材）。

**Architecture:** 单一 Markdown 文档，四部分结构（项目总览 / 上篇核心亮点 5 个 / 下篇模块速览 9 个 / 附录速查表）。核心亮点用四段式（背景→方案→实现→答辩价值），模块速览用简版四段式（150-250 字）。所有事实已在本计划中核查完毕，写作时直接引用"事实清单"，不得虚构。

**Tech Stack:** Markdown；事实来源为 `backend/src/main/java/com/campus/secondhand/` 与 `frontend/src/` 源码。

**Git 约定：** 本环境规则要求"未经用户明确请求不得 commit"。每个任务完成后**不要自动 commit**，仅在用户明确要求时统一提交。

---

### Task 1: 创建文档骨架 + 项目总览

**Files:**
- Create: `docs/答辩亮点技术文档.md`

- [ ] **Step 1: 创建文档，写入标题、目录与"一、项目总览"**

内容要点（事实已核实）：
- 技术栈表：后端 Spring Boot 3.3.5 / Java 17 / MyBatis-Plus 3.5.9 / MySQL 8.0 / JJWT 0.12.6（HMAC-SHA384）/ Hutool 5.8.25；前端 Vue 3 / Vite / Element Plus / Pinia / Vue Router 4 / Axios / ECharts
- 架构分层：前端 Vue SPA → Vite 代理 `/api` → Spring Boot（Controller → Service → Mapper/MySQL），JWT 拦截器横切认证，`UserContext` 透传当前用户
- 统一返回格式 `{code, message, data}`；逻辑删除 + 自动时间填充（MyBatis-Plus）
- 数据流一句话：用户操作 → axios（`request.js` 自动带 Bearer Token）→ 拦截器鉴权 → Service 业务 → Mapper → MySQL
- 目录占位（二、三、四章先留标题，后续任务填充）

- [ ] **Step 2: 自检**：确认无未核实的技术栈版本号；与 `README.md`、`backend/pom.xml` 一致。

---

### Task 2: 上篇 2.1 订单状态机与超时预留回收

**Files:**
- Modify: `docs/答辩亮点技术文档.md`

- [ ] **Step 1: 按四段式撰写 2.1 节**

事实清单（源码：`OrderServiceImpl`，已核实）：
- `STATUS_FLOW` 静态 Map 定义合法流转：PENDING→[PAID, CANCELLED]、PAID→[SHIPPED]、SHIPPED→[COMPLETED]，非法流转抛 `BusinessException`
- `createOrder`：校验商品在售、不能买自己的商品；检查是否已有他人未过期 PENDING 预留（`expire_time > now`）防一物多卖；同一买家旧待付订单自动置 CANCELLED；订单号用 Hutool 雪花算法 `ORD + IdUtil.getSnowflakeNextIdStr()`；下单即预留（商品转 OFF_SHELF）+ 30 分钟 `expireTime`
- `payOrder`：付款前检查超时，超时则取消订单并恢复商品；付款后商品转 SOLD
- `cancelExpiredOrders`：`@Scheduled(fixedRate = 300000)` 每 5 分钟回收过期订单，`restoreProduct` 将 OFF_SHELF 恢复为 ON_SALE
- `checkOrderPermission`：付款/确认收货仅买家、发货仅卖家、取消买卖双方皆可；管理员放行
- 全程 `@Transactional(rollbackFor = Exception.class)`
- 表结构依据：`t_order.expire_time`、`status` 字段（migration 脚本）

- [ ] **Step 2: 自检**：四段式齐全；答辩价值落点为"并发控制、状态一致性、事务边界"。

---

### Task 3: 上篇 2.2 小黑屋自动风控算法

**Files:**
- Modify: `docs/答辩亮点技术文档.md`

- [ ] **Step 1: 按四段式撰写 2.2 节**

事实清单（源码：`BlacklistServiceImpl`，已核实）：
- `autoScan` 挂 `@Scheduled(cron = "0 0 3 * * ?")` 每日凌晨 3 点，也可由管理员 `triggerScan` 手动触发
- 执行顺序：先解封到期用户（`blacklist_until <= now`），`justUnblacklisted` 集合排除刚解封者，防止解封后立即再罚
- 只统计 90 天窗口内的 1 星评论（减少数据量 + 避免历史违规反复处罚）
- 卖家规则：收到的评论（按商品归属卖家聚合）总数 ≥5 且其中 1 星 ≥5 且占比 >30% → 拉黑
- 买家规则：发出的 1 星占自己全部评论 >80% 且 COMPLETED 订单数 = 0 → 拉黑（防职业差评/恶意刷分）
- 刑期递增：`BASE_DAYS=14`，实际天数 `14 × 2^blacklist_count`（再犯加重）
- 受过处罚者（`blacklist_count > 0`）只统计解封（`update_time`）之后的新评论
- 拉黑/解封均通过 `NotificationService.send` 发站内通知
- 数据字段依据：`t_user.blacklist_status/reason/until/count`（`migration_blacklist.sql`）
- 答辩价值段必须回答"为什么这么设计"：阈值取整数便于解释；90 天窗口平衡数据量与追溯；翻倍刑期体现累犯加重

- [ ] **Step 2: 自检**：确认阈值数字（5/30%/80%/14 天/90 天）与源码一致。

---

### Task 4: 上篇 2.3 投诉-申诉治理闭环 + 2.4 JWT 联动拦截

**Files:**
- Modify: `docs/答辩亮点技术文档.md`

- [ ] **Step 1: 按四段式撰写 2.3 节**

事实清单（源码：`AdminServiceImpl`，已核实）：
- 投诉：`t_complaint`（target_user_id/reporter_id/reason/evidence/status），处理 `handleComplaint`：PENDING→RESOLVED（成立即 `manualBlacklist` 被投诉人 14 天 + 通知投诉人）或 DISMISSED；记录 handler_id/handler_note；管理员角色豁免拉黑
- 申诉：`t_appeal`，处理 `handleAppeal`：PENDING→APPROVED（`unblacklist` 解封 + 通知）或 REJECTED（通知）；均事务化
- 后台待办：`getNotifications` 聚合 pending 投诉数 + pending 申诉数 + 黑名单总数，驱动前端红点
- 闭环叙事：自动检测（2.2）→ 处罚 → 人工投诉补充 → 申诉救济 → 解封，全链路通知留痕

- [ ] **Step 2: 按四段式撰写 2.4 节**

事实清单（源码：`JwtInterceptor`、`request.js`，已核实）：
- `JwtInterceptor.preHandle`：OPTIONS 放行；解析 Bearer Token，`validateToken` 失败返回 401 JSON；成功后将 userId/role 注入 request attribute，供 `UserContext` 使用
- 小黑屋联动：`blacklistService.isBlacklisted(userId)` 命中时，对 `RESTRICTED_PATHS`（`/product/`、`/comment/`、`/order/`、`/favorite/`）下的**非 GET** 请求返回 403 + 解封时间；GET 放行——"可看不可动"的柔性处罚
- 前端配合：`request.js` 响应拦截器收到 401 → `clearAuth()` + 跳转登录页
- 答辩价值：横切关注点集中处理（对比在每个 Controller 写校验）、处罚粒度设计（封写不封读）

- [ ] **Step 3: 自检**：受限路径列表与源码 `RESTRICTED_PATHS` 数组逐字一致。

---

### Task 5: 上篇 2.5 ECharts 后台数据可视化

**Files:**
- Modify: `docs/答辩亮点技术文档.md`

- [ ] **Step 1: 按四段式撰写 2.5 节**

事实清单（源码：`Admin.vue`、`AdminServiceImpl.dashboard/categoryStats`，已核实）：
- 后端：`DashboardVO` 返回用户数/商品数/订单数/今日订单数（今日用 `createTime` between 当日 MIN-MAX 统计）；`/admin/category-stats` 返回各分类商品数
- 前端加载策略：`Promise.all` 并发拉取仪表盘 + 订单 + 商品数据，`nextTick` + 100ms 延时确保 Tab 内 DOM 挂载完成再渲染
- 玫瑰图：`roseType: 'area'`，环形（radius 45%-75%），翠绿系渐变色板（`#10B981` 起）；数据由商品列表按 `categoryName` 聚合
- 柱状图：订单状态语义化颜色映射（待付款 `#F59E0B` 暖橙、已付款 `#3B82F6`、已发货 `#8B5CF6`、已完成 `#10B981` 翠绿、已取消 `#9CA3AF`），呼应设计规范主题色
- 生命周期管理：渲染前 `dispose` 旧实例；Tab 不可见（`offsetParent === null`）时 200ms 重试；`window resize` 监听调用 `chart.resize()`；空数据显示"暂无数据"兜底标题
- 答辩价值：前后端数据流完整链路 + 工程细节（图表在 Tab/懒渲染场景的常见坑及解法）

- [ ] **Step 2: 自检**：颜色值与 `Admin.vue` colorMap 一致。

---

### Task 6: 下篇 全模块速览

**Files:**
- Modify: `docs/答辩亮点技术文档.md`

- [ ] **Step 1: 撰写 9 个模块的简版四段式（每个 150-250 字）**

各模块事实清单（均已核实）：
- **用户**：`UserServiceImpl` 登录（Hutool `DigestUtil.md5Hex` 密码比对、禁用账号拦截、统一报"用户名或密码错误"防枚举）、注册查重、JWT 签发（`JwtUtil.generateToken(userId, username, role)`）；`getCurrentUser` 返回前 `setPassword(null)` 脱敏。短板如实写：MD5 无盐，改进方向为 BCrypt
- **商品**：`ProductServiceImpl` 发布（images 存 JSON 数组、`JsonListTypeHandler`）、编辑校验归属、下架、删除（管理员可删任意）；公开列表默认只查 ON_SALE；详情页浏览量 +1；复杂分页查询在 Mapper XML 连表
- **订单**：摘要 + "详见 2.1"
- **收藏**：`FavoriteServiceImpl.toggle` 一个接口切换收藏/取消；`t_favorite` 有 `uk_user_product` 唯一约束兜底防重复
- **评论**：1-5 星评分 + 内容；是风控算法（2.2）的数据源，指向上篇
- **通知**：`NotificationServiceImpl` 站内信（send/unreadCount/markRead/markAllRead/clearRead）；业务事件自动触发（拉黑/解封/投诉处理/申诉结果）；`@Scheduled(cron = "0 0 4 * * ?")` 每日清理 30 天前通知；表索引 `idx_user_read`
- **分类**：二级分类树（`t_category.parent_id`，7 个一级 + 30+ 二级）；`AdminServiceImpl.deleteCategory` 删除保护（分类下有商品禁删）
- **后台管理**：Dashboard（指向 2.5）+ 用户/商品/订单分页管理 + 投诉申诉处理（指向 2.3）+ 小黑屋手动拉黑/解封/手动扫描
- **文件上传**：`FileController.upload` → `FileUploadUtil`；`FileServerController` 提供 `/uploads/{filename:.+}` 访问，`@PostConstruct` 解析绝对路径并建目录，**canonicalPath 前缀校验防路径穿越攻击**（安全亮点，值得单独强调）

- [ ] **Step 2: 自检**：核心亮点相关模块（订单/后台）只有摘要+指针，无重复展开。

---

### Task 7: 附录 技术点速查表

**Files:**
- Modify: `docs/答辩亮点技术文档.md`

- [ ] **Step 1: 撰写速查表**

格式：`| 技术点 | 代码位置 | 一句话原理/设计取舍 |`。至少覆盖以下条目（取舍项如实写）：
- 30 分钟订单预留为什么不用 Redis → `OrderServiceImpl` → 单体规模下 DB expire_time + 定时扫描足够，避免引入中间件复杂度（取舍：高并发下需 Redis 分布式锁）
- 为什么用 MD5 而不是 BCrypt → `UserServiceImpl.encryptPassword` → 取舍如实写：历史实现，生产应升级 BCrypt + 盐
- 定时任务多实例部署问题 → 订单回收/小黑屋扫描/通知清理三处 `@Scheduled` → 当前单实例部署；多实例需 ShedLock/分布式锁
- JWT 无状态的注销问题 → `JwtUtil`/`JwtInterceptor` → Token 到期前无法主动失效（取舍：可加黑名单或短有效期+刷新）
- 拉黑阈值设定依据 → `BlacklistServiceImpl` → 5 条/30%/80% 为经验值，防小样本误伤（≥5 条门槛）+ 防职业差评（订单数=0）
- 一物多卖防护 → `createOrder` 预留检查 → 应用层检查 + 商品状态即时下架；强一致可用数据库行锁/唯一索引
- 路径穿越防护 → `FileServerController` → canonicalPath 前缀校验
- 统一返回与全局异常 → `Result`/`GlobalExceptionHandler`/`BusinessException` → 前后端契约一致

- [ ] **Step 2: 自检**：每条"代码位置"真实存在；无"待补充"字样。

---

### Task 8: 全文自审与收尾

**Files:**
- Modify: `docs/答辩亮点技术文档.md`

- [ ] **Step 1: 自审清单**
  - 占位符扫描：搜索 TBD/TODO/待补充，应为 0
  - 一致性：上篇 5 个亮点标题与目录、下篇指针一致
  - 事实抽查：随机抽 5 个数字/类名与源码对照（阈值、颜色值、cron 表达式、路径数组、表名）
  - 夸大扫描：不出现"高并发"、"海量"等未验证表述
  - 篇幅：4000-6000 字
- [ ] **Step 2: 向用户汇报完成情况**（不自动 commit；如用户要求则统一提交）。

---

## Self-Review 结论

- 规格覆盖：项目总览（Task 1）、上篇 5 亮点（Task 2-5）、下篇 9 模块（Task 6）、附录（Task 7）、质量保证自审（Task 8）——全覆盖
- 无占位符：每个任务含已核实的完整事实清单
- 一致性：类名/方法名/阈值与规格及源码核对一致

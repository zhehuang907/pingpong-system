# backend-py — 乒乓球装备推荐系统 Python 后端

由原 Spring Boot 2.7 + MyBatis-Plus 后端迁移而来的 FastAPI 实现。
**前端零改动**：接口路径、请求/响应结构、字段命名、错误语义与 Java 侧完全一致。

---

## 1. 快速开始

```bash
# 1) 创建虚拟环境（建议装在项目目录之外）
python -m venv ~/.workbuddy/binaries/python/envs/pingpong-py

# 2) 安装依赖
pip install -r requirements.txt

# 3) 配置环境变量
cp .env.example .env      # 按需修改 DB_* / JWT_SECRET / REDIS_*

# 4) 启动（默认 http://127.0.0.1:8081）
python -m app.main
# 或
uvicorn app.main:app --host 0.0.0.0 --port 8081 --reload
```

启动后会打印自检结果：

```
============================================================
pingpong-recommendation 启动完成
  监听地址 : http://0.0.0.0:8081/api
  数据库   : 已连接
  Redis    : 未连接（已降级为无缓存/无限流）
============================================================
```

接口文档：<http://127.0.0.1:8081/api/docs>

> **数据库不需要改动。** Python 直接复用现有 MySQL 库表；用户密码的 BCrypt
> 哈希可被 `bcrypt` 直接校验，JWT 密钥与算法与 Java 一致，因此**无需重置密码、
> 也不会踢掉已登录用户**。

## 2. 测试

```bash
python -m pytest
```

33 个用例跑在 **SQLite 内存库**上，不依赖 MySQL / Redis，
覆盖推荐算法、JWT 鉴权、响应契约、收藏/购物车/评论/管理端闭环。

---

## 3. 目录结构

```
backend-py/
├── app/
│   ├── main.py              # 应用工厂、中间件装配、静态资源挂载
│   ├── core/
│   │   ├── config.py        # 配置中心（对标 application.yml）
│   │   ├── result.py        # {code,msg,data} 统一响应封装
│   │   ├── security.py      # BCrypt + JWT
│   │   ├── redis_client.py  # 带降级的 Redis 客户端
│   │   └── exceptions.py    # 业务 / 鉴权 / 权限 / 限流异常
│   ├── db/
│   │   ├── base.py          # DeclarativeBase + 时间戳 / 逻辑删除 Mixin
│   │   └── session.py       # 引擎与会话
│   ├── models/              # 27 张表的 ORM 模型
│   ├── schemas/             # Pydantic 模型（驼峰别名）
│   ├── services/            # 业务逻辑（对应 Java *ServiceImpl）
│   ├── middleware/          # JWT 鉴权 / 限流（对应 HandlerInterceptor）
│   ├── api/routes/          # 路由（对应 *Controller）
│   └── scheduler/tasks.py   # 榜单重算定时任务（对应 @Scheduled）
└── tests/                   # pytest 用例
```

---

## 4. Java → Python 映射速查

| Java / Spring Boot        | Python / FastAPI                          |
| ------------------------- | ----------------------------------------- |
| `@RestController`         | `APIRouter`                               |
| `@Autowired`              | `Depends(...)`                            |
| `HandlerInterceptor`      | `BaseHTTPMiddleware` 子类                 |
| `@Scheduled`              | APScheduler `CronTrigger`                 |
| `@Value` / `application.yml` | `pydantic-settings` 的 `Settings`      |
| `LambdaQueryWrapper`      | SQLAlchemy `select()` / `update()`        |
| MyBatis-Plus 逻辑删除     | `LogicDeleteMixin`（`deleted` 字段）      |
| Jackson 驼峰序列化        | `CamelModel`（`alias_generator=to_camel`）|
| `Result<T>` 统一响应      | `Result` + `ResultJSONResponse`           |
| Hutool `BCrypt.hashpw`    | `bcrypt.hashpw`（哈希格式互通）           |
| jjwt HS512                | `PyJWT` HS512（同一密钥下 token 互认）    |

### 几个刻意为之的设计

**1）业务错误返回 HTTP 200。**
Java 侧 `GlobalExceptionHandler` 把业务异常包成 `{code:500, msg:...}` 且状态码保持
200；前端 `request.js` 拦截器依赖 `res.code !== 200` 弹错。Python 沿用该语义，
否则前端会走进「网络错误」分支。HTTP 状态码仅用于 401/403/404/429 等框架层语义。

**2）字段名一律驼峰。**
响应侧由 `CamelModel` 的 alias 保证；**请求侧**由 `normalize_payload()` 把前端
传入的 `categoryId` 等键名映射回模型的 `category_id`，避免多词字段被静默丢弃。

**3）Redis 缺失时自动降级。**
缓存、token 绑定、限流均为可缺失组件；连不上时降级为「无缓存 / 不绑定 / 不限流」，
而不是启动失败——对齐 Java 侧的行为。

**4）测试用 SQLite 跑同构逻辑。**
主键用 `BigInteger().with_variant(Integer, "sqlite")`；收藏计数递减用 SQL `CASE`
替代 MySQL 的 `GREATEST()`，保证两种方言下行为一致。

---

## 5. 推荐算法（原样移植）

三路召回 + 多因子打分，权重均可在 `.env` 调整：

- **行为权重**：浏览 1 / 收藏 3 / 评论 5 / 购买 10
- **相似度**：标签 Jaccard 0.6 + 同类目 0.3 + 价格接近度 0.1，低于阈值 0.3 不入选
- **候选池**上限 300 条，控制打分开销
- **榜单热度**：`base = 收藏×0.3 + 评论×0.2 + 评分×0.5 + 时间衰减`,
  `衰减 = exp(-小时/240)×1.8`，`热度 = base + log1p(浏览)×0.55`

对应源码：`app/services/recommend.py`、`app/services/leaderboard.py`。

---

## 6. 与原 Java 后端的差异说明

| 项目               | 处理方式                                                     |
| ------------------ | ------------------------------------------------------------ |
| 验证码             | Java 返回图片流；此处返回 **SVG**（`<img>` 可直接引用），避免引入 Pillow |
| 装备/分类/标签增删改 | Java 为公开接口；此处加 `require_admin` 鉴权（更符合生产预期，如需放开请移除依赖） |
| 定时任务           | 默认关闭（`SCHEDULER_ENABLED=false`），开启后由 APScheduler 驱动，用 `schedule_job_lock` 表做分布式锁 |
| AI 顾问            | DeepSeek 接口调用逻辑一致；未配置 `DEEPSEEK_API_KEY` 时返回兜底文案而非报错 |

### 迁移过程中修正的问题

核对前端实际契约时发现并修正了以下偏差：

1. **开球王榜路径不一致**：前端调用 `/kaiqiuwang-ranking/overview`，原实现暴露的是
   `/kickoff-king/overview`，已补齐前端实际使用的路径。
2. **缺失 26 个前端依赖端点**：评论后台审核全套、购物车勾选/全选、浏览历史单条删除、
   装备/分类/标签管理端增删、`/user/preferences`、`POST /upload` 等，已全部补齐。
3. **瞬态字段被误建模为数据库列**：`comment.equipment_name`、`comment.reviewer_name`、
   `browse_history.equipment_name` 在 Java 侧是 `@TableField(exist = false)` 的
   非表字段，原先写进了 ORM 模型，在 MySQL 上会触发 `Unknown column`，已删除。
4. **管理端写操作丢失驼峰字段**：`Equipment(**payload)` 以 `hasattr` 过滤 key，
   前端传来的 `categoryId` 会被静默丢弃，改为先做驼峰→下划线归一化。

---

## 7. 切换回 Java / 双跑

两套后端使用同一份库表与同一套 JWT 密钥，因此：

- 可以直接**互切**，无需数据迁移；
- 若要双跑验证，让两者监听不同端口，前端 `vue.config.js` 的 proxy 指向其中之一即可。

注意：若两边都启用了榜单定时任务，会重复计算——保持一侧开启即可。

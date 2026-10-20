# 贡献指南 / Contributing Guide

感谢参与本项目。提交前请阅读以下约定，这能显著降低评审成本。

## 开发环境

参见 [README 快速开始](./README.md#6-快速开始)。核心要求：JDK 8+、Node.js 18+、
Python 3.10+、MySQL 8（Redis 可选）。

## 分支与提交

- 从 `main` 切出特性分支：`feat/xxx`、`fix/xxx`、`docs/xxx`、`chore/xxx`。
- 提交信息建议遵循 [Conventional Commits](https://www.conventionalcommits.org/)：
  `feat(recommend): 增加标签权重可配置`。
- 一次提交只做一件事，避免把格式化改动与逻辑改动混在一起。

## 代码规范

| 语言 | 约定 |
| --- | --- |
| Java | 4 空格缩进；包结构沿用 `com.pingpong.{controller,service,entity,mapper,config,common}`；新增接口必须复用统一响应 `Result<T>` |
| Python | 4 空格缩进；遵循 PEP 8；业务逻辑放 `app/services`，路由层只做参数校验与编排 |
| Vue / JS | 2 空格缩进；接口调用统一走 `src/api/*.js`，不要在页面里直接写 `axios` |
| SQL | 迁移脚本放 `database/migrations/`，文件名以 `YYYYMMDD_` 开头，只增不改 |

## 契约一致性（重要）

`backend/`（Java）与 `backend-py/`（Python）是同一契约的两套实现。任何接口改动必须
**同时**满足：

1. 与 `frontend/src/api/*.js` 的调用路径、方法、参数字段一致；
2. 响应字段保持驼峰；
3. 统一使用 `{code, msg, data}` 结构，业务错误保持 HTTP 200 + `code != 200`。

若只改一侧，请在 PR 描述中显式说明原因。

## 提交前自检

```bash
cd backend && ./mvnw test          # 或 .\mvnw.cmd test
cd backend-py && python -m pytest
cd frontend && npm run build
```

请在 PR 描述中贴出实际执行的测试命令与结果。

## 不要提交的内容

- 任何真实凭据：数据库密码、`JWT_SECRET`、`DEEPSEEK_API_KEY`（请用环境变量注入）。
- `node_modules/`、`target/`、`dist/`、`backend/uploads/` 等构建 / 运行期产物。
- 个人隐私数据与学校教务材料（已在 `.gitignore` 中排除）。

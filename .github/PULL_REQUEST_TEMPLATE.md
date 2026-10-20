## 变更说明

<!-- 这个 PR 做了什么，为什么这么做 -->

## 变更类型

- [ ] 新功能（feat）
- [ ] 缺陷修复（fix）
- [ ] 重构 / 优化（refactor / perf）
- [ ] 文档（docs）
- [ ] 构建 / 配置（chore）

## 涉及范围

- [ ] 前端（`frontend/`）
- [ ] Java 后端（`backend/`）
- [ ] Python 后端（`backend-py/`）
- [ ] 数据库（`database/`）
- [ ] 文档 / 其他

## 契约一致性确认

<!-- 见 CONTRIBUTING.md；若本次只改动单侧后端，请说明原因 -->

- [ ] 接口路径与 `frontend/src/api/*.js` 一致
- [ ] 响应字段保持驼峰，统一 `{code, msg, data}` 结构
- [ ] Java / Python 两套后端均已同步（或已说明仅改单侧的原因）
- [ ] 未提交任何真实凭据、构建产物或隐私数据

## 测试结果

<!-- 贴出实际执行的命令与结果 -->

```
cd backend && ./mvnw test
cd backend-py && python -m pytest
cd frontend && npm run build
```

## 关联 Issue

<!-- 例如 Closes #12 -->

<template>
  <div class="auth-page register-page">
    <div class="auth-shell">
      <section class="auth-showcase">
        <div class="court-lines"></div>
        <div class="paddle paddle-top"></div>
        <div class="paddle paddle-bottom"></div>
        <div class="pingpong-ball ball-main"></div>
        <div class="pingpong-ball ball-secondary"></div>

        <div class="showcase-content">
          <div class="showcase-badge">新用户入场</div>
          <h1 class="showcase-title">建立你的专属器材训练档案</h1>
          <p class="showcase-text">
            完成注册后，你可以记录打法偏好、收藏心仪器材，
            并获得更贴近个人训练阶段的推荐结果。
          </p>

          <div class="showcase-tags">
            <span>打法画像</span>
            <span>装备收藏</span>
            <span>推荐进阶</span>
          </div>

          <div class="showcase-panel">
            <div class="panel-card">
              <div class="panel-label">账号安全规则</div>
              <ul class="panel-list">
                <li>用户名需以字母开头，长度 6 到 20 位</li>
                <li>密码需同时包含大小写字母、数字和特殊字符</li>
                <li>登录时会根据设备和网络环境进行异常提醒</li>
              </ul>
            </div>

            <div class="panel-metrics">
              <div class="metric-item">
                <strong>唯一账号</strong>
                <span>便于长期维护你的训练档案</span>
              </div>
              <div class="metric-item">
                <strong>自动登录</strong>
                <span>注册成功后立即进入系统继续使用</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <el-card class="auth-card" shadow="never">
        <div class="card-header">
          <div>
            <div class="card-kicker">创建账号</div>
            <h2>加入训练阵地</h2>
            <p>填写基础信息后即可开始使用乒乓球装备推荐服务。</p>
          </div>
          <el-link class="back-link" underline="never" @click="router.push('/login')">
            返回登录
          </el-link>
        </div>

        <div class="tip-row">
          <span class="tip-chip">唯一用户名</span>
          <span class="tip-chip">高强度密码</span>
          <span class="tip-chip">注册后自动登录</span>
        </div>

        <el-form
          ref="formRef"
          :model="registerForm"
          :rules="rules"
          label-position="top"
          class="auth-form"
        >
          <div class="form-grid">
            <el-form-item prop="username" label="用户名">
              <el-input
                v-model="registerForm.username"
                :prefix-icon="User"
                clearable
                maxlength="50"
                autocomplete="username"
                placeholder="请输入用户名"
              />
              <div class="field-tip">示例：`PaddleAce_26`，需以字母开头。</div>
            </el-form-item>

            <el-form-item prop="nickname" label="昵称">
              <el-input
                v-model="registerForm.nickname"
                :prefix-icon="Avatar"
                clearable
                maxlength="50"
                placeholder="请输入昵称，可留空"
              />
            </el-form-item>
          </div>

          <el-form-item prop="email" label="邮箱">
            <el-input
              v-model="registerForm.email"
              :prefix-icon="Message"
              clearable
              autocomplete="email"
              placeholder="请输入邮箱，便于后续找回账号"
            />
          </el-form-item>

          <div class="form-grid">
            <el-form-item prop="password" label="密码">
              <el-input
                v-model="registerForm.password"
                type="password"
                :prefix-icon="Lock"
                show-password
                autocomplete="new-password"
                placeholder="请输入密码"
              />
              <div class="field-tip">密码需为 10 到 32 位，包含大小写字母、数字和特殊字符。</div>
            </el-form-item>

            <el-form-item prop="confirmPassword" label="确认密码">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                :prefix-icon="Lock"
                show-password
                autocomplete="new-password"
                placeholder="请再次输入密码"
              />
            </el-form-item>
          </div>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
            立即注册
          </el-button>

          <div class="footer-actions">
            <span>已有账号？</span>
            <el-link type="primary" underline="never" @click="$router.push('/login')">
              去登录
            </el-link>
            <span class="divider">|</span>
            <el-link underline="never" @click="$router.push('/')">返回首页</el-link>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar, Message } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { useUserStore } from '@/store/user'
import { rememberLastAccount } from '@/utils/auth'

const USERNAME_PATTERN = /^[A-Za-z][A-Za-z0-9_]{5,49}$/
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{10,32}$/

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!USERNAME_PATTERN.test(value || '')) {
          callback(new Error('用户名需以字母开头，长度 6 到 50 位，仅允许字母、数字和下划线'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  nickname: [{ max: 50, message: '昵称长度不能超过 50 位', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!PASSWORD_PATTERN.test(value || '')) {
          callback(new Error('密码需为 10 到 32 位，且包含大小写字母、数字和特殊字符'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  loading.value = true

  try {
    const payload = {
      username: registerForm.username,
      nickname: registerForm.nickname,
      email: registerForm.email,
      password: registerForm.password
    }

    const res = await userApi.register(payload)
    userStore.setSession({
      token: res.data,
      userInfo: {},
      rememberMe: false
    })
    const userInfoRes = await userApi.info()
    const userInfo = userInfoRes.data

    userStore.setUserInfo(userInfo)

    rememberLastAccount({
      username: userInfo.username,
      displayName: userInfo.nickname || userInfo.username
    })

    ElMessage.success('注册成功，已自动登录')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-page {
  --ink: #17352e;
  --ink-soft: #617269;
  --court-green: #1d5846;
  --court-green-deep: #143d31;
  --accent-orange: #f08a24;
  --accent-orange-deep: #d96b1f;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background:
    radial-gradient(circle at 10% 14%, rgba(240, 138, 36, 0.24), transparent 28%),
    radial-gradient(circle at 88% 85%, rgba(29, 88, 70, 0.22), transparent 26%),
    linear-gradient(135deg, #f8f3ea 0%, #f3f8ef 52%, #edf2f6 100%);
}

.auth-shell {
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(420px, 0.98fr);
  gap: 28px;
  align-items: stretch;
}

.auth-showcase,
.auth-card {
  position: relative;
  border-radius: 32px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(23, 53, 46, 0.12);
}

.auth-showcase {
  min-height: 680px;
  padding: 44px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(249, 255, 248, 0.8)),
    linear-gradient(145deg, #fff8ef 0%, #e8f4eb 58%, #f4f0e7 100%);
  border: 1px solid rgba(255, 255, 255, 0.72);
}

.showcase-content {
  position: relative;
  z-index: 2;
  max-width: 500px;
}

.showcase-badge {
  display: inline-flex;
  padding: 10px 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: var(--court-green-deep);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.showcase-title {
  margin: 28px 0 18px;
  font-size: clamp(40px, 5vw, 62px);
  line-height: 1.05;
  color: var(--ink);
}

.showcase-text {
  margin: 0;
  font-size: 18px;
  line-height: 1.9;
  color: rgba(23, 53, 46, 0.82);
}

.showcase-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;

  span {
    padding: 10px 16px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.68);
    color: var(--court-green);
    font-size: 14px;
    font-weight: 600;
  }
}

.showcase-panel {
  display: grid;
  gap: 16px;
  margin-top: 34px;
}

.panel-card,
.panel-metrics {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.panel-card {
  padding: 24px;
}

.panel-label {
  margin-bottom: 14px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(23, 53, 46, 0.58);
}

.panel-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;

  li {
    position: relative;
    padding-left: 18px;
    font-size: 15px;
    line-height: 1.8;
    color: var(--ink-soft);

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 10px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--accent-orange), var(--court-green));
    }
  }
}

.panel-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 18px;
}

.metric-item {
  padding: 16px 14px;
  border-radius: 18px;
  background: rgba(247, 242, 233, 0.92);

  strong {
    display: block;
    margin-bottom: 8px;
    font-size: 20px;
    color: var(--accent-orange-deep);
  }

  span {
    font-size: 13px;
    line-height: 1.7;
    color: var(--ink-soft);
  }
}

.court-lines {
  position: absolute;
  inset: 22px;
  border: 2px solid rgba(29, 88, 70, 0.08);
  border-radius: 28px;
  pointer-events: none;

  &::before,
  &::after {
    content: '';
    position: absolute;
    background: rgba(29, 88, 70, 0.08);
  }

  &::before {
    top: 28px;
    bottom: 28px;
    left: 50%;
    width: 2px;
    transform: translateX(-50%);
  }

  &::after {
    left: 28px;
    right: 28px;
    top: 50%;
    height: 2px;
    transform: translateY(-50%);
  }
}

.paddle,
.pingpong-ball {
  position: absolute;
  z-index: 1;
}

.paddle {
  width: 148px;
  height: 172px;
  border-radius: 50% 50% 46% 46%;
  box-shadow: 0 18px 36px rgba(23, 53, 46, 0.12);

  &::after {
    content: '';
    position: absolute;
    width: 20px;
    height: 92px;
    left: 50%;
    bottom: -68px;
    transform: translateX(-50%);
    border-radius: 999px;
    background: linear-gradient(180deg, #8d5d33, #5d3921);
  }
}

.paddle-top {
  right: 92px;
  top: 92px;
  transform: rotate(18deg);
  background: linear-gradient(145deg, #205f4b, #153f33);
}

.paddle-bottom {
  right: 210px;
  bottom: 116px;
  transform: rotate(-24deg);
  background: linear-gradient(145deg, #f29a38, #d96b1f);
}

.pingpong-ball {
  border-radius: 50%;
  background: radial-gradient(circle at 32% 32%, #ffffff 0%, #fff7e5 62%, #f7deaf 100%);
  box-shadow: 0 12px 26px rgba(23, 53, 46, 0.12);
}

.ball-main {
  top: 132px;
  right: 178px;
  width: 64px;
  height: 64px;
}

.ball-secondary {
  bottom: 168px;
  right: 86px;
  width: 42px;
  height: 42px;
}

.auth-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.7);
  padding: 30px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  h2 {
    margin: 8px 0 6px;
    font-size: 34px;
    color: var(--ink);
  }

  p {
    margin: 0;
    font-size: 15px;
    line-height: 1.7;
    color: var(--ink-soft);
  }
}

.card-kicker {
  color: var(--accent-orange-deep);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.back-link {
  margin-top: 8px;
  color: var(--court-green);
  font-weight: 600;
}

.tip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.tip-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f4f8f2;
  color: var(--court-green);
  font-size: 13px;
  font-weight: 600;
}

.auth-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-form-item__label) {
    color: var(--ink);
    font-weight: 600;
  }

  :deep(.el-input__wrapper) {
    min-height: 50px;
    border-radius: 16px;
    box-shadow: inset 0 0 0 1px rgba(23, 53, 46, 0.08);
    background: #fffefb;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: inset 0 0 0 1px rgba(240, 138, 36, 0.75);
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-soft);
}

.submit-btn {
  width: 100%;
  height: 52px;
  margin-top: 6px;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--court-green) 0%, var(--accent-orange) 100%);
  box-shadow: 0 16px 30px rgba(29, 88, 70, 0.18);
}

.footer-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;
  color: var(--ink-soft);
  font-size: 14px;
}

.divider {
  color: rgba(23, 53, 46, 0.26);
}

@media (max-width: 1080px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-card {
    order: 1;
  }

  .auth-showcase {
    order: 2;
    min-height: 520px;
  }
}

@media (max-width: 768px) {
  .auth-page {
    padding: 18px;
  }

  .auth-showcase,
  .auth-card {
    border-radius: 24px;
  }

  .auth-showcase {
    min-height: auto;
    padding: 28px 24px;
  }

  .showcase-title {
    font-size: 38px;
  }

  .showcase-text {
    font-size: 16px;
  }

  .form-grid,
  .panel-metrics {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
  }

  .paddle-bottom,
  .ball-secondary {
    display: none;
  }
}
</style>

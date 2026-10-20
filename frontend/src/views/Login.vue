<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="background">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
      <div class="bg-grid"></div>
    </div>

    <div class="login-container">
      <!-- 左侧介绍区 -->
      <div class="intro-section">
        <div class="intro-content">
          <div class="logo-wrapper">
            <div class="logo-icon">
              <el-icon>
                <Basketball />
              </el-icon>
            </div>
            <h1 class="brand-title">PingPong</h1>
            <p class="brand-subtitle">乒乓球装备推荐系统</p>
          </div>

          <div class="intro-text">
            <h2>欢迎回来</h2>
            <p>探索专业乒乓球装备，找到最适合您的利器</p>
          </div>

          <div class="features">
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon>
                  <Guide />
                </el-icon>
              </div>
              <div class="feature-text">
                <h4>智能推荐</h4>
                <p>基于您的打法和水平精准推荐</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon>
                  <Shop />
                </el-icon>
              </div>
              <div class="feature-text">
                <h4>品牌齐全</h4>
                <p>汇聚全球知名乒乓球品牌</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon>
                  <User />
                </el-icon>
              </div>
              <div class="feature-text">
                <h4>专业评测</h4>
                <p>真实用户评价和详细参数</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="form-section">
        <div class="login-box">
          <div class="form-header">
            <h2>用户登录</h2>
            <p>请输入您的账号信息</p>
          </div>

          <el-form ref="formRef" :model="loginForm" :rules="rules" class="login-form" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名/邮箱" prefix-icon="User" maxlength="100" clearable />
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password
                @keyup.enter="handleLogin" />
            </el-form-item>

            <el-form-item prop="captcha">
              <div class="captcha-input">
                <el-input v-model="loginForm.captcha" placeholder="请输入验证码" prefix-icon="Key" clearable />
                <div class="captcha-image" @click="refreshCaptcha">
                  <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" />
                  <span v-else>点击获取</span>
                </div>
              </div>
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码？</el-link>
            </div>

            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>

            <div class="divider">
              <span>或</span>
            </div>

            <div class="form-footer">
              <span>还没有账号？</span>
              <el-link type="primary" @click="$router.push('/register')">
                立即注册
              </el-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElForm } from 'element-plus'
import { User, Lock, Key, Basketball, Guide, Shop } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { useUserStore } from '@/store/user'
import { getLastAccount, getStoredUserInfo, getToken, rememberLastAccount } from '@/utils/auth'
import { setLoggingIn } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const captchaUrl = ref('')
const captchaKey = ref('')

const loginForm = ref({
  username: '',
  password: '',
  captcha: '',
  remember: false
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码长度为 4 位', trigger: 'blur' }
  ]
}

const getCaptcha = () => {
  captchaKey.value = Date.now().toString()
  captchaUrl.value = `/api/captcha?k=${captchaKey.value}&t=${Date.now()}`
  console.log('验证码已获取:', captchaUrl.value)
}

const refreshCaptcha = () => {
  console.log('刷新验证码')
  getCaptcha()
  loginForm.value.captcha = ''
}

const validateCaptcha = async () => {
  if (!loginForm.value.captcha) {
    console.log('验证码为空')
    return false
  }
  try {
    const res = await fetch(`/api/captcha/verify?key=${captchaKey.value}&code=${loginForm.value.captcha}`)
    const data = await res.json()
    console.log('验证码验证响应:', data)
    return data.code === 200
  } catch (error) {
    console.error('验证码验证失败:', error)
    ElMessage.error('验证码服务不可用')
    return false
  }
}

const handleLogin = async () => {
  console.log('=== 登录按钮被点击 ===')
  console.log('formRef:', formRef.value)
  console.log('表单数据:', loginForm.value)
  
  if (!formRef.value) {
    console.error('formRef 为空')
    ElMessage.error('表单初始化失败，请刷新页面')
    return
  }
  
  loading.value = true
  
  // 设置登录标志，防止 401 自动跳转
  setLoggingIn(true)
  
  try {
    const valid = await formRef.value.validate()
    console.log('表单验证结果:', valid)
    
    if (!valid) {
      console.log('表单验证失败')
      ElMessage.error('请填写完整的登录信息')
      return
    }
    
    // 暂时禁用验证码验证
    // const captchaValid = await validateCaptcha()
    // console.log('验证码验证结果:', captchaValid)
    
    // if (!captchaValid) {
    //   console.log('验证码错误')
    //   ElMessage.error('验证码错误，请重新输入')
    //   refreshCaptcha()
    //   return
    // }
    
    console.log('发送登录请求...')
    const res = await userApi.login({
      username: loginForm.value.username,
      password: loginForm.value.password,
      rememberMe: loginForm.value.remember
    })
    console.log('登录响应:', res)
    
    console.log('登录成功，准备 Token...')
    const token = res.data
    console.log('Token 值:', token)
    console.log('Token 长度:', token?.length)
    
    console.log('先存储 token，确保后续请求带上鉴权头...')
    userStore.setToken(token, loginForm.value.remember)

    console.log('获取详细用户信息...')
    const userInfo = await userApi.info()
    
    console.log('完善会话信息...')
    userStore.setUserInfo(userInfo.data)

    rememberLastAccount({
      username: userInfo.data?.username || loginForm.value.username,
      displayName: userInfo.data?.nickname || userInfo.data?.username,
      deviceName: res.loginPayload?.deviceName || ''
    })

    ;(res.loginPayload?.alerts || []).forEach((alert) => {
      ElMessage({
        type: alert?.level === 'warning' ? 'warning' : 'info',
        message: alert?.message || '登录环境发生变化',
        duration: 5000
      })
    })
    
    // 再次验证 token 是否已正确存储
    const storedToken = getToken()
    const storedUserInfo = getStoredUserInfo()
    console.log('最终验证 - Token:', storedToken ? '存在 (' + storedToken.length + ' chars)' : '不存在')
    console.log('最终验证 - UserInfo:', storedUserInfo ? '存在' : '不存在')
    
    if (!storedToken) {
      console.error('严重错误：token 未能存储到 localStorage!')
      ElMessage.error('登录状态存储失败，请刷新页面重试')
      return
    }
    
    console.log('登录成功，准备跳转...')
    ElMessage.success('登录成功')
    
    // 等待消息显示后再跳转
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 优先跳转到重定向地址，否则回首页
    const redirectPath = route.query.redirect || '/'
    console.log('跳转到:', redirectPath)
    
    router.push(redirectPath).catch(err => {
      console.error('路由跳转失败:', err)
      // 如果跳转失败，强制刷新页面
      window.location.href = redirectPath
    })
  } catch (error) {
    console.error('登录失败详情:', error)
    console.error('错误响应:', error.response)
    console.error('错误消息:', error.message)
    console.error('错误状态码:', error.response?.status)
    console.error('错误数据:', error.response?.data)
    userStore.logout()
    ElMessage.error(error.response?.data?.msg || error.message || '登录失败')
  } finally {
    loading.value = false
    // 重置登录标志
    setLoggingIn(false)
  }
}

onMounted(() => {
  const lastAccount = getLastAccount()
  if (lastAccount?.username) {
    loginForm.value.username = lastAccount.username
  }
  console.log('组件已挂载')
  getCaptcha()
})
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.3);
  top: -150px;
  left: -150px;
  animation-delay: 0s;
}

.shape-2 {
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.2);
  top: -100px;
  right: -100px;
  animation-delay: 2s;
}

.shape-3 {
  width: 150px;
  height: 150px;
  background: rgba(255, 255, 255, 0.1);
  bottom: -75px;
  left: 20%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
}

.bg-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 50px 50px;
  }
}

.login-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  max-width: 1100px;
  width: 100%;
  background: white;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.6s ease-out;
  position: relative;
  z-index: 1;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.intro-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 50px;
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;

  .intro-content {
    position: relative;
    z-index: 1;
  }

  .logo-wrapper {
    margin-bottom: 40px;
    text-align: center;
  }

  .logo-icon {
    font-size: 64px;
    margin-bottom: 16px;
    color: white;
  }

  .brand-title {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 8px;
    color: white;
  }

  .brand-subtitle {
    font-size: 16px;
    opacity: 0.9;
    margin-bottom: 40px;
  }

  .intro-text {
    margin-bottom: 40px;
    text-align: center;

    h2 {
      font-size: 28px;
      font-weight: 600;
      margin-bottom: 12px;
    }

    p {
      font-size: 16px;
      opacity: 0.9;
    }
  }

  .features {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .feature-item {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    padding: 16px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    backdrop-filter: blur(10px);
  }

  .feature-icon {
    font-size: 32px;
    color: rgba(255, 255, 255, 0.9);
    flex-shrink: 0;
  }

  .feature-text {
    h4 {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 4px;
      color: white;
    }

    p {
      font-size: 14px;
      opacity: 0.8;
      margin: 0;
    }
  }
}

.form-section {
  padding: 60px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-box {
  width: 100%;
  max-width: 400px;
}

.form-header {
  text-align: center;
  margin-bottom: 32px;

  h2 {
    font-size: 28px;
    font-weight: 700;
    color: #333;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #666;
  }
}

.login-form {
  .el-form-item {
    margin-bottom: 20px;
  }

  .el-input {
    height: 48px;
    font-size: 16px;

    :deep(.el-input__wrapper) {
      border-radius: 12px;
    }
  }
}

.captcha-input {
  display: flex;
  gap: 12px;
  align-items: center;

  :deep(.el-input) {
    flex: 1;
  }
}

.captcha-image {
  width: 120px;
  height: 40px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  overflow: hidden;
  transition: all 0.3s ease;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  span {
    color: #999;
    font-size: 12px;
  }

  &:hover {
    border-color: #667eea;
    box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

.divider {
  position: relative;
  text-align: center;
  margin: 24px 0;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    width: 40%;
    height: 1px;
    background: #e0e0e0;
  }

  &::before {
    left: 0;
  }

  &::after {
    right: 0;
  }

  span {
    background: white;
    padding: 0 12px;
    color: #999;
    font-size: 14px;
    position: relative;
    z-index: 1;
  }
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #666;

  span {
    margin-right: 8px;
  }
}

@media (max-width: 768px) {
  .login-container {
    grid-template-columns: 1fr;
  }

  .intro-section {
    display: none;
  }

  .form-section {
    padding: 40px 20px;
  }
}
</style>

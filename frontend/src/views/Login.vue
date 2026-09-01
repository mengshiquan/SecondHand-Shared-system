<template>
  <div class="login-page">
    <AuthBackground />
    <div class="left-section">
      <div class="logo-section">
        <a href="/" class="logo-link">
          <img src="/logo-vertical.png" alt="校园物品" class="logo-vertical-img">
        </a>
      </div>

      <div class="characters-section">
        <AnimatedCharacters :isTyping="isTyping" :showPassword="showPassword" :passwordLength="form.password.length"
          :loginFailed="loginFailed" :loginSuccess="loginSuccess" />
      </div>

      <div class="footer-links">
        <a href="https://www.asu.edu.cn/" target="_blank" rel="noopener" class="footer-link">安顺学院</a>
        <a href="/" class="footer-link">校园二手平台</a>
      </div>
    </div>

    <div class="right-section">
      <div class="form-wrapper">
        <div class="mobile-logo">
          <img src="/logo-horizontal.png" alt="校园物品" class="mobile-logo-img">
        </div>

        <div class="auth-header">
          <img src="/logo-horizontal.png" alt="校园物品" class="auth-logo">
          <h2>欢迎回来</h2>
          <p>登录你的账号，发现校园好物</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password @visible-change="showPassword = $event" @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="验证码" prefix-icon="Key" size="large" maxlength="4" @keyup.enter="handleLogin" />
              <img :src="captchaImg" class="captcha-img" title="点击刷新验证码" @click="refreshCaptcha">
            </div>
          </el-form-item>
          
          <el-form-item>
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我 30 天</el-checkbox>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </div>

       
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import AnimatedCharacters from '@/components/AnimatedCharacters.vue'
import AuthBackground from '@/components/AuthBackground.vue'
import { getCaptcha } from '@/api/user'


/**
 * 登录页：左侧品牌插画 + 右侧登录表单（验证码、记住我、动画角色反馈）
 */
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()                 // 表单引用
const loading = ref(false)            // 登录提交中
const rememberMe = ref(true)          // 是否 30 天记住登录
const showPassword = ref(false)       // 密码是否明文展示（控制动画）
const isTyping = ref(false)           // 是否正在输入（控制动画）
const loginFailed = ref(false)        // 登录失败动画触发
const loginSuccess = ref(false)       // 登录成功动画触发
const form = reactive({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const captchaImg = ref('')
/**
 * 刷新图形验证码并清空已输入的验证码
 */
async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    captchaImg.value = res.data.image
    form.captchaKey = res.data.key
    form.captchaCode = ''
  } catch { /* 验证码加载失败时登录提交会提示 */ }
}
onMounted(refreshCaptcha)

/**
 * 登录提交：校验表单 → 调用登录 → 成功延迟跳转首页/redirect，失败刷新验证码
 */
async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  loginFailed.value = false
  loginSuccess.value = false
  try {
    await userStore.login({ ...form }, rememberMe.value)
    loginSuccess.value = true
    ElMessage.success('登录成功')
    setTimeout(() => {
      router.push(route.query.redirect || '/')
    }, 1000)
  } catch (error) {
    loginFailed.value = true
    refreshCaptcha()
    setTimeout(() => {
      loginFailed.value = false
    }, 3000)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
  max-height: 100vh;
  overflow: hidden;
}

.left-section {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 3rem;
  color: var(--sh-ink);
}

.logo-section {
  position: relative;
}

.logo-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 600;
  text-decoration: none;
  color: inherit;
}

.logo-vertical-img {
  height: 168px;
  width: auto;
  display: block;
}

.characters-section {
  position: relative;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: 500px;
}

.footer-links {
  position: relative; z-index: 20;
  display: flex; align-items: center; gap: 2rem;
  font-size: 0.875rem; color: var(--sh-muted);
}
.footer-link { color: inherit; text-decoration: none; transition: color 0.2s; }
.footer-link:hover { color: var(--sh-primary-deep); }

.right-section {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.form-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 36px 32px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06), 0 1px 3px rgba(0,0,0,0.04);
}

.mobile-logo {
  display: none;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 3rem;
}

.mobile-logo-img {
  height: 56px;
  width: auto;
  display: block;
}

.auth-logo {
  height: 46px;
  width: auto;
  margin: 0 auto 6px;
  display: block;
}

/* ---- 验证码 ---- */
.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.captcha-img {
  height: 40px;
  flex: none;
  border-radius: 8px;
  border: 1px solid var(--sh-line);
  cursor: pointer;
  background: #fff;
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}
.auth-header h2 {
  margin: 10px 0 6px;
  font-size: 24px; font-weight: 700;
  color: var(--sh-ink);
  position: relative;
  display: inline-block;
}
/* 三色马克笔下划线：绿/橙/靖蓝，呼应背景涂鸦笔色 */
.auth-header h2::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -8px;
  width: 56px; height: 4px; border-radius: 2px;
  background: linear-gradient(90deg, var(--sh-primary) 0 34%, var(--sh-accent) 34% 67%, #6366F1 67% 100%);
}

.auth-header p {
  color: #909399;
  font-size: 14px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.forgot-link {
  color: #10B981;
  font-size: 14px;
}

.auth-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
}

.auth-footer a {
  color: #10B981;
}

/* ---- 输入框焦点微交互 ---- */
.form-wrapper :deep(.el-input .el-input__wrapper) {
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
  border-left: 3px solid transparent;
}
.form-wrapper :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #10B981 inset, 0 0 10px rgba(16,185,129,0.15);
}

/* ---- 登录按钮光泽动效 ---- */
.form-wrapper :deep(.el-button--primary) {
  position: relative;
  overflow: hidden;
}
.form-wrapper :deep(.el-button--primary)::after {
  content: '';
  position: absolute; top: -50%; left: -60%;
  width: 40%; height: 200%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transform: skewX(-25deg);
  transition: left 0.5s ease;
}
.form-wrapper :deep(.el-button--primary):hover::after {
  left: 120%;
}

.demo-tip {
  text-align: center;
  margin-top: 12px;
  font-size: 12px;
  color: #c0c4cc;
}

@media (max-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .left-section {
    display: none;
  }

  .mobile-logo {
    display: flex;
  }
}
</style>
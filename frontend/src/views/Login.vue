<template>
  <div class="login-page">
    <div class="left-section">
      <div class="logo-section">
        <a href="/" class="logo-link">
          <span>校园二手共享平台</span>
        </a>
      </div>

      <div class="characters-section">
        <AnimatedCharacters :isTyping="isTyping" :showPassword="showPassword" :passwordLength="form.password.length"
          :loginFailed="loginFailed" :loginSuccess="loginSuccess" />
      </div>

      <div class="footer-links">
        <a href="/privacy-policy" class="footer-link">Privacy Policy</a>
        <a href="/terms" class="footer-link">Terms of Service</a>
      </div>

      <div class="grid-overlay"></div>
      <div class="blur-circle blur-circle-1"></div>
      <div class="blur-circle blur-circle-2"></div>
    </div>

    <div class="right-section">
      <div class="form-wrapper">
        <div class="mobile-logo">
          <span>校园二手共享平台</span>
        </div>

        <div class="auth-header">
          <el-icon :size="40" color="#10B981"><Shop /></el-icon>
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
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import AnimatedCharacters from '@/components/AnimatedCharacters.vue'


const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const rememberMe = ref(false)
const showPassword = ref(false)
const isTyping = ref(false)
const loginFailed = ref(false)
const loginSuccess = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  loginFailed.value = false
  loginSuccess.value = false
  try {
    await userStore.login({ ...form })
    loginSuccess.value = true
    ElMessage.success('登录成功')
    setTimeout(() => {
      router.push(route.query.redirect || '/')
    }, 1000)
  } catch (error) {
    loginFailed.value = true
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
  background: linear-gradient(160deg, #064E3B 0%, #047857 30%, #059669 60%, #10B981 100%);
  padding: 3rem;
  color: white;
}

.logo-section {
  position: relative;
  z-index: 20;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 600;
  text-decoration: none;
  color: inherit;
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
  font-size: 0.875rem; color: rgba(255,255,255,0.6);
}
.footer-link { color: inherit; text-decoration: none; transition: color 0.2s; }
.footer-link:hover { color: #fff; }

.grid-overlay {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.06) 1px, transparent 1px);
  background-size: 24px 24px;
}

.blur-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(96px);
}

.blur-circle-1 {
  top: 20%; right: 20%;
  width: 18rem; height: 18rem;
  background: rgba(16, 185, 129, 0.15);
}
.blur-circle-2 {
  bottom: 20%; left: 20%;
  width: 26rem; height: 26rem;
  background: rgba(5, 150, 105, 0.12);
}

.right-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: linear-gradient(to bottom, #ffffff, #F0FDF4);
}

.form-wrapper {
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

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}
.auth-header h2 {
  margin: 10px 0 6px;
  font-size: 24px; font-weight: 700;
  color: #1F2937;
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
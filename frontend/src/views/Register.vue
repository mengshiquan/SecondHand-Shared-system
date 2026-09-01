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
          :loginFailed="registerFailed" :loginSuccess="registerSuccess" />
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
          <h2>注册新账号</h2>
          <p>加入校园二手共享社区</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名（3-20字符）" prefix-icon="User" size="large" @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="昵称" prefix-icon="UserFilled" size="large" @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码（6-20字符）" prefix-icon="Lock" size="large" show-password @visible-change="showPassword = $event" @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" size="large" show-password @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="手机号" prefix-icon="Phone" size="large" @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="studentId">
            <el-input v-model="form.studentId" placeholder="学号" prefix-icon="Postcard" size="large" maxlength="12" @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="schoolName">
            <el-input v-model="form.schoolName" placeholder="学校名称" prefix-icon="School" size="large" maxlength="100" @focus="isTyping = true" @blur="isTyping = false" />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="验证码" prefix-icon="Key" size="large" maxlength="4" />
              <img :src="captchaImg" class="captcha-img" title="点击刷新验证码" @click="refreshCaptcha">
            </div>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleRegister">
              注 册
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register, getCaptcha } from '@/api/user'
import { ElMessage } from 'element-plus'
import AnimatedCharacters from '@/components/AnimatedCharacters.vue'
import AuthBackground from '@/components/AuthBackground.vue'

/**
 * 注册页：收集用户名、昵称、密码、手机号、学号、学校及验证码完成注册
 */
const router = useRouter()
const formRef = ref()                // 表单引用
const loading = ref(false)           // 注册提交中
const showPassword = ref(false)      // 密码是否明文展示（控制动画）
const isTyping = ref(false)          // 是否正在输入（控制动画）
const registerFailed = ref(false)    // 注册失败动画触发
const registerSuccess = ref(false)   // 注册成功动画触发

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  phone: '',
  studentId: '',
  schoolName: '',
  captchaKey: '',
  captchaCode: ''
})

/**
 * 校验两次输入的密码是否一致
 * @param {*} rule 校验规则
 * @param {string} value 确认密码值
 * @param {Function} callback 校验回调
 */
const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度为3-20个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  schoolName: [{ required: true, message: '请输入学校名称', trigger: 'blur' }],
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
  } catch { /* 验证码加载失败时提交会提示 */ }
}
onMounted(refreshCaptcha)

/**
 * 注册提交：校验表单 → 调用注册接口 → 成功延迟跳转登录页，失败刷新验证码
 */
async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  registerFailed.value = false
  registerSuccess.value = false
  try {
    await register({
      username: form.username,
      nickname: form.nickname,
      password: form.password,
      phone: form.phone,
      studentId: form.studentId,
      schoolName: form.schoolName,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    })
    registerSuccess.value = true
    ElMessage.success('注册成功，校园身份审核通过后即可发布与交易')
    setTimeout(() => {
      router.push('/login')
    }, 1800)
  } catch (error) {
    registerFailed.value = true
    refreshCaptcha()
    setTimeout(() => {
      registerFailed.value = false
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
  display: flex;
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
  display: flex; align-items: center; justify-content: center;
  padding: 2rem;
}

.form-wrapper {
  position: relative;
  z-index: 1;
  width: 100%; max-width: 400px;
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
  margin-bottom: 2rem;
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
  text-align: center; margin-bottom: 28px;
}
.auth-header h2 {
  margin: 10px 0 6px;
  font-size: 24px; font-weight: 700; color: var(--sh-ink);
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

/* ---- 注册按钮光泽动效 ---- */
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
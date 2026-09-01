<template>
  <div class="publish page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-button text :icon="ArrowLeft" class="back-btn" @click="router.back()">返回</el-button>
      <h1 class="page-title">{{ isEdit ? '编辑商品' : '发布闲置商品' }}</h1>
      <p class="page-sub">{{ isEdit ? '修改商品信息并保存' : '填写商品信息，让更多人看到你的闲置好物' }}</p>
    </div>

    <!-- 校园认证状态提示：未通过认证的用户无法发布 -->
    <el-alert
      v-if="!isEdit && verifyStatus === 'PENDING'"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
      title="你的校园身份认证正在审核中，管理员审核通过后即可发布商品"
    />
    <el-alert
      v-else-if="!isEdit && verifyStatus === 'REJECTED'"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
      title="你的校园身份认证未通过，请联系管理员重新审核后才能发布商品"
    />

    <div class="form-card">
      <div class="form-card-glow"></div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="publish-form">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-head">
            <el-icon :size="18"><Edit /></el-icon>
            <span>基本信息</span>
          </div>
          <el-form-item label="商品标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入商品标题" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="商品分类" prop="categoryId">
            <CategoryPicker v-model="form.categoryId" />
          </el-form-item>
        </div>

        <!-- 价格设置 -->
        <div class="form-section">
          <div class="section-head">
            <el-icon :size="18"><PriceTag /></el-icon>
            <span>价格设置</span>
          </div>
          <el-form-item label="售价" prop="price">
            <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item label="原价">
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="1" />
          </el-form-item>
        </div>

        <!-- 图片上传 -->
        <div class="form-section">
          <div class="section-head">
            <el-icon :size="18"><Picture /></el-icon>
            <span>商品图片</span>
            <span class="section-tip">最多上传 5 张</span>
          </div>
          <el-form-item label="商品图片">
            <ImageUpload v-model="form.images" :limit="5" />
          </el-form-item>
        </div>

        <!-- 详细描述 -->
        <div class="form-section">
          <div class="section-head">
            <el-icon :size="18"><Document /></el-icon>
            <span>详细描述</span>
          </div>
          <el-form-item label="商品描述" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="5" placeholder="详细描述商品成色、使用情况等" />
          </el-form-item>
        </div>

        <div class="form-actions">
          <el-button type="primary" size="large" :loading="loading" class="btn-submit" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '立即发布' }}
          </el-button>
          <el-button size="large" @click="router.back()">取消</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Edit, PriceTag, Picture, Document } from '@element-plus/icons-vue'
import { publishProduct, updateProduct, getProductDetail } from '@/api/product'
import { getUserInfo } from '@/api/user'
import ImageUpload from '@/components/ImageUpload.vue'
import CategoryPicker from '@/components/CategoryPicker.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)
const isEdit = ref(false)
// 认证状态默认按已认证处理，避免接口失败时误拦已认证用户；实际拦截以后端 VerifyGuard 为准
const verifyStatus = ref('APPROVED')

const form = reactive({
  id: null,
  title: '',
  categoryId: null,
  price: null,
  originalPrice: null,
  images: [],
  description: ''
})

const rules = {
  title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }]
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (isEdit.value) {
      await updateProduct(form)
      ElMessage.success('修改成功')
    } else {
      await publishProduct(form)
      ElMessage.success('发布成功')
    }
    router.push('/profile')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (route.query.id) {
    isEdit.value = true
    const res = await getProductDetail(route.query.id)
    Object.assign(form, res.data)
  } else {
    // 新发布时检查校园认证状态，提前告知用户，避免提交后才报错
    try {
      const res = await getUserInfo()
      verifyStatus.value = res.data?.verifyStatus || 'PENDING'
    } catch { /* 忽略，后端发布时仍会拦截 */ }
  }
})
</script>

<style scoped>
/* ====== 页面标题 ====== */
.page-header { margin-bottom: 24px; }
.back-btn { color: #6B7280; font-weight: 500; margin-bottom: 8px; }

/* ====== 表单卡片 ====== */
.form-card {
  position: relative;
  background: #fff;
  border-radius: 20px;
  padding: 36px 40px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
  overflow: hidden;
}
.form-card-glow {
  position: absolute; top: 0; left: 0; right: 0;
  height: 4px;
  background: linear-gradient(90deg, #059669, #10B981, #34D399);
}
.publish-form { max-width: 700px; position: relative; z-index: 1; }

/* ====== 分区 ====== */
.form-section {
  margin-bottom: 28px;
  padding-bottom: 28px;
  border-bottom: 1px solid #F3F4F6;
}
.form-section:last-of-type { border-bottom: none; margin-bottom: 24px; padding-bottom: 0; }
.section-head {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 700; color: #374151;
  margin-bottom: 16px;
}
.section-head .el-icon { color: #10B981; }
.section-tip { font-size: 12px; color: #9CA3AF; font-weight: 400; margin-left: auto; }

/* ====== 操作按钮 ====== */
.form-actions {
  display: flex; gap: 14px;
  padding-top: 8px;
}
.btn-submit {
  flex: 1; height: 48px; font-size: 16px; font-weight: 700;
  max-width: 280px;
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .form-card { padding: 20px 16px; border-radius: 14px; }
  .form-actions { flex-direction: column; }
  .btn-submit { max-width: 100%; }
}
/* 手机端 */
@media (max-width: 480px) {
  
  .form-card { padding: 16px 12px; border-radius: 12px; }
  .publish-form { max-width: 100%; }
  .section-head { font-size: 14px; }
  .section-head .el-icon { font-size: 16px; }
  .btn-submit { height: 44px; font-size: 15px; }
}
</style>

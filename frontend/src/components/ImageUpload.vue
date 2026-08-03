<template>
  <div class="image-upload">
    <el-upload
      :file-list="fileList"
      list-type="picture-card"
      :http-request="handleUpload"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
      :limit="limit"
      accept="image/*"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>

    <el-dialog v-model="previewVisible" title="图片预览" width="600px">
      <img :src="previewUrl" style="width: 100%" alt="preview" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { uploadFile } from '@/api/file'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  limit: { type: Number, default: 5 }
})

const emit = defineEmits(['update:modelValue'])

const fileList = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')

watch(
  () => props.modelValue,
  (val) => {
    fileList.value = (val || []).map((url, index) => ({
      name: `image-${index}`,
      url
    }))
  },
  { immediate: true }
)

async function handleUpload({ file }) {
  try {
    const res = await uploadFile(file)
    const urls = [...props.modelValue, res.data]
    emit('update:modelValue', urls)
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败')
  }
}

function handleRemove(file) {
  const urls = props.modelValue.filter((url) => url !== file.url)
  emit('update:modelValue', urls)
}

function handlePreview(file) {
  previewUrl.value = file.url
  previewVisible.value = true
}
</script>

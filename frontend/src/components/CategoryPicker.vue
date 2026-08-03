<template>
  <div class="category-picker">
    <!-- 当前层级标题 + 面包屑 -->
    <div class="picker-header" v-if="breadcrumb.length > 0">
      <el-button text size="small" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回上级
      </el-button>
      <span class="breadcrumb-path">
        <span v-for="(b, i) in breadcrumb" :key="b.id">
          <span v-if="i > 0" class="bc-sep"> › </span>
          <span class="bc-item" :class="{ active: i === breadcrumb.length - 1 }">{{ b.name }}</span>
        </span>
      </span>
    </div>

    <!-- 当前层级选项 -->
    <div class="picker-options">
      <div
        v-for="item in currentOptions"
        :key="item.id"
        class="picker-chip"
        :class="{ selected: modelValue === item.id, 'has-children': item.children?.length }"
        @click="handleSelect(item)"
      >
        <span class="chip-icon">{{ getEmoji(item.name) }}</span>
        <span class="chip-name">{{ item.name }}</span>
        <el-icon v-if="item.children?.length" class="chip-arrow"><ArrowRight /></el-icon>
        <el-icon v-else class="chip-check" v-show="modelValue === item.id"><Check /></el-icon>
      </div>

      <div v-if="currentOptions.length === 0" class="picker-empty">
        暂无分类数据
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getCategoryTree } from '@/api/category'

const props = defineProps({
  modelValue: { type: Number, default: null }
})

const emit = defineEmits(['update:modelValue'])

const tree = ref([])
const breadcrumb = ref([])
const currentOptions = ref([])

const catEmoji = {
  '数码电子': '📱', '手机': '📱', '平板电脑': '💻', '笔记本电脑': '💻', '智能手表/手环': '⌚', '移动电源/充电器': '🔋', '蓝牙耳机/音箱': '🎧', '相机/摄像机': '📷',
  '图书教材': '📚', '教材/教辅': '📖', '文学/小说': '📕', '考试用书': '📝', '专业书籍': '📘', '杂志/期刊': '📰',
  '生活用品': '🏠', '家居装饰': '🛋️', '厨房用品': '🍳', '洗护/个护': '🧴', '收纳/整理': '📦', '日用杂货': '🛒',
  '服饰鞋包': '👔', '男装': '👨', '女装': '👩', '鞋子': '👟', '箱包': '🎒', '配饰/首饰': '💍',
  '运动户外': '⚽', '健身器材': '🏋️', '球类': '🏀', '户外装备': '🏕️', '骑行': '🚲', '游泳': '🏊',
  '音乐器材': '🎵', '吉他': '🎸', '钢琴/电子琴': '🎹', '打击乐器': '🥁', '管乐器': '🎺', '其他乐器': '🎻',
  '其他闲置': '📦', '玩具/玩偶': '🧸', '礼品/工艺品': '🎁', '票券/卡券': '🎫', '其他': '📌'
}

function getEmoji(name) {
  return catEmoji[name] || '📌'
}

function handleSelect(item) {
  if (item.children && item.children.length > 0) {
    // 有子分类：钻入下一级
    breadcrumb.value.push({ id: item.id, name: item.name })
    currentOptions.value = item.children
  } else {
    // 叶子节点：选中
    emit('update:modelValue', item.id)
  }
}

function goBack() {
  if (breadcrumb.value.length === 0) return
  const prev = breadcrumb.value.pop()

  // 找到上一级的选项列表
  if (breadcrumb.value.length === 0) {
    // 回到根级（一级分类）
    currentOptions.value = tree.value
  } else {
    // 回到上一级子分类
    findAndSetOptions(tree.value, breadcrumb.value)
  }

  // 如果当前选中的值在当前层级不可见，清除选中
  if (props.modelValue) {
    const visible = currentOptions.value.some(o => o.id === props.modelValue)
    if (!visible) emit('update:modelValue', null)
  }
}

function findAndSetOptions(nodes, path) {
  let current = nodes
  for (const crumb of path) {
    const found = current.find(n => n.id === crumb.id)
    if (found && found.children) {
      current = found.children
    } else {
      break
    }
  }
  currentOptions.value = current
}

onMounted(async () => {
  try {
    const res = await getCategoryTree()
    tree.value = res.data || []
    currentOptions.value = tree.value

    // 如果已有选中值，导航到对应层级
    if (props.modelValue) {
      navigateToSelection(tree.value, [])
    }
  } catch {
    // 静默处理
  }
})

// 当外部传入 modelValue 时，尝试定位到对应位置
function navigateToSelection(nodes, path) {
  for (const node of nodes) {
    if (node.id === props.modelValue && node.children?.length > 0) {
      // 选中了一个有子分类的节点，直接显示它
      breadcrumb.value = [...path]
      currentOptions.value = [node]
      return
    }
    // 在子分类中搜索
    if (node.children?.length) {
      const found = navigateToSub(node.children, [...path, node])
      if (found) return
    }
  }
  // 如果没找到确切节点，但 modelValue 指向一个叶子节点
  for (const node of nodes) {
    if (node.id === props.modelValue) {
      breadcrumb.value = [...path]
      return
    }
    if (node.children?.length) {
      const found = findLeaf(node.children, [...path, node])
      if (found) {
        breadcrumb.value = found
        return
      }
    }
  }
}

function navigateToSub(nodes, path) {
  for (const node of nodes) {
    if (node.id === props.modelValue) {
      breadcrumb.value = path
      currentOptions.value = nodes
      return true
    }
    if (node.children?.length) {
      if (navigateToSub(node.children, [...path, node])) return true
    }
  }
  return false
}

function findLeaf(nodes, path) {
  for (const node of nodes) {
    if (node.id === props.modelValue) return path
    if (node.children?.length) {
      const found = findLeaf(node.children, [...path, node])
      if (found) return found
    }
  }
  return null
}
</script>

<style scoped>
.category-picker {
  width: 100%;
}

.picker-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #F0FDF4;
  border-radius: 8px;
  font-size: 13px;
}

.breadcrumb-path { color: #6B7280; }
.bc-sep { color: #D1D5DB; margin: 0 4px; }
.bc-item.active { color: #10B981; font-weight: 600; }

.picker-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.picker-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fff;
  border: 2px solid #E5E7EB;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.picker-chip:hover {
  border-color: #10B981;
  background: #F0FDF4;
}
.picker-chip.selected {
  border-color: #10B981;
  background: #ECFDF5;
  box-shadow: 0 0 0 2px rgba(16,185,129,0.15);
}

.chip-icon { font-size: 18px; }
.chip-name { font-size: 14px; font-weight: 500; color: #374151; }
.chip-arrow { color: #D1D5DB; }
.picker-chip:hover .chip-arrow { color: #10B981; }
.chip-check { color: #10B981; }

.picker-empty {
  padding: 24px;
  text-align: center;
  color: #9CA3AF;
  font-size: 14px;
  width: 100%;
}

/* 手机端缩小间距 */
@media (max-width: 480px) {
  .picker-options { gap: 6px; }
  .picker-chip { padding: 8px 12px; border-radius: 10px; }
  .chip-icon { font-size: 16px; }
  .chip-name { font-size: 13px; }
}
</style>

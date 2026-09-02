<template>
  <div class="notify-page page-container">
    <div class="page-header">
      <h1 class="page-title">消息通知</h1>
      <p class="page-sub">管理你收到的系统通知</p>
    </div>

    <div class="notify-actions">
      <el-button v-if="unreadCount > 0" type="primary" size="small" @click="handleMarkAll">全部已读</el-button>
      <el-button size="small" @click="handleClear">清空已读</el-button>
    </div>

    <div v-if="notifications.length > 0" class="notify-card">
      <div
        v-for="n in notifications"
        :key="n.id"
        class="notify-row"
        :class="{ unread: !n.isRead }"
        @click="handleRead(n)"
      >
        <div class="notify-dot" v-if="!n.isRead"></div>
        <div class="notify-body">
          <div class="notify-title">{{ n.title }}</div>
          <div class="notify-content">{{ n.content }}</div>
          <div class="notify-time">{{ n.createTime }}</div>
        </div>
        <el-button class="notify-del" text size="small" type="danger" @click.stop="handleDelete(n.id)">删除</el-button>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon"><el-icon :size="40"><Bell /></el-icon></div>
      <p class="empty-title">暂无通知</p>
    </div>
  </div>
</template>

<script setup>
// 通知中心页：加载、已读、删除和清空当前用户通知。
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { getNotificationList, getUnreadCount, markRead, markAllRead, deleteNotification, clearRead } from '@/api/notification'

const notifications = ref([])
const unreadCount = ref(0)

/** 分页加载通知列表。 */
async function load() {
  const [listRes, countRes] = await Promise.all([
    getNotificationList({ pageNum: 1, pageSize: 50 }),
    getUnreadCount()
  ])
  notifications.value = listRes.data.records || []
  unreadCount.value = countRes.data.count
}

/** 标记单条通知为已读。 */
async function handleRead(n) {
  if (!n.isRead) {
    await markRead(n.id)
    n.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

/** 标记全部通知为已读。 */
async function handleMarkAll() {
  await markAllRead()
  ElMessage.success('已全部标为已读')
  load()
}

/** 删除单条通知。 */
async function handleDelete(id) {
  await deleteNotification(id)
  ElMessage.success('已删除')
  load()
}

/** 清空所有已读通知。 */
async function handleClear() {
  await ElMessageBox.confirm('确认清空所有已读通知？', '确认', { type: 'warning' })
  await clearRead()
  ElMessage.success('已清空')
  load()
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 24px; }

.notify-actions { display: flex; gap: 8px; margin-bottom: 16px; justify-content: flex-end; }

.notify-card {
  background: #fff; border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
  overflow: hidden;
}
.notify-row {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 20px; cursor: pointer;
  border-bottom: 1px solid #F3F4F6;
  transition: background 0.15s;
}
.notify-row:last-child { border-bottom: none; }
.notify-row:hover { background: #F9FAFB; }
.notify-row.unread { background: #F0FDF4; }
.notify-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #10B981; flex-shrink: 0;
}
.notify-body { flex: 1; min-width: 0; }
.notify-title { font-size: 15px; font-weight: 600; color: #1F2937; margin-bottom: 4px; }
.notify-content { font-size: 14px; color: #6B7280; margin-bottom: 6px; }
.notify-time { font-size: 12px; color: #D1D5DB; }
.notify-del { flex-shrink: 0; opacity: 0; transition: opacity 0.15s; }
.notify-row:hover .notify-del { opacity: 1; }

.empty-state { text-align: center; padding: 64px 20px; }
.empty-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 72px; height: 72px; border-radius: 16px;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  color: #10B981; margin-bottom: 16px;
}
.empty-title { font-size: 16px; font-weight: 600; color: #374151; margin: 0; }

/* 平板端：操作按钮始终可见 */
@media (max-width: 768px) {
  .notify-row { gap: 8px; }
  .notify-del { opacity: 1; }
  .notify-actions { justify-content: stretch; }
  .notify-actions .el-button { flex: 1; }
  .notify-card { border-radius: 12px; }
  
}
/* 手机端：缩小间距和字体 */
@media (max-width: 480px) {
  .notify-row { padding: 12px 14px; }
  .notify-title { font-size: 14px; }
  .notify-content { font-size: 13px; }
  .notify-time { font-size: 11px; }
  .notify-actions { flex-wrap: wrap; }
}
</style>

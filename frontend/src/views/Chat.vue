<template>
  <div class="page-container chat-container">
    <div class="chat-page">
      <!-- 会话列表 -->
      <aside class="chat-side" :class="{ 'side-hidden': peerId && isMobile }">
        <div class="side-head">消息</div>
        <div class="side-search">
          <el-input v-model="convKeyword" size="small" placeholder="搜索会话" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div v-if="!filteredConversations.length" class="side-empty">
          {{ conversations.length ? '没有匹配的会话' : '暂无会话' }}<br><span>去商品页联系卖家开始聊天</span>
        </div>
        <div
          v-for="c in filteredConversations"
          :key="c.peerId"
          class="conv-item"
          :class="{ active: c.peerId === peerId }"
          @click="openChat(c.peerId)"
        >
          <el-avatar :size="44" :src="c.peerAvatar || undefined">
            {{ (c.peerName || '?').slice(0, 1) }}
          </el-avatar>
          <div class="conv-mid">
            <div class="conv-name">{{ c.peerName || '用户' + c.peerId }}</div>
            <div class="conv-last">{{ c.lastContent }}</div>
          </div>
          <div class="conv-right">
            <span class="conv-time">{{ fmtTime(c.lastTime) }}</span>
            <span v-if="c.unread" class="conv-badge">{{ c.unread > 99 ? '99+' : c.unread }}</span>
            <el-popconfirm title="删除会话？将清空双方聊天记录" @confirm="removeConversation(c.peerId)">
              <template #reference>
                <el-icon class="conv-del" title="删除会话" @click.stop><Delete /></el-icon>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </aside>

      <!-- 聊天窗口 -->
      <section class="chat-main" :class="{ 'main-hidden': !peerId && isMobile }">
        <template v-if="peerId">
          <div class="main-head">
            <el-button v-if="isMobile" link @click="router.push('/chat')">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <span class="peer-name">{{ peerName }}</span>
            <el-tag v-if="productTitle" size="small" effect="plain" class="head-tag">
              {{ productTitle }}
            </el-tag>
          </div>

          <div ref="bodyRef" class="chat-body">
            <div v-if="!messages.length" class="body-empty">还没有消息，打个招呼吧</div>
            <div v-else-if="hasMore" class="load-older">
              <el-button size="small" text :loading="loadingOlder" @click="loadOlder">加载更早消息</el-button>
            </div>
            <div v-for="m in messages" :key="m.id" class="msg-row" :class="{ mine: m.mine }">
              <el-avatar :size="36" :src="m.mine ? myAvatar : peerAvatar" class="msg-avatar">
                {{ (m.mine ? myName : peerName).slice(0, 1) }}
              </el-avatar>
              <div v-if="editingId === m.id" class="msg-edit">
                <el-input
                  v-model="editDraft"
                  type="textarea"
                  :autosize="{ minRows: 1, maxRows: 4 }"
                  maxlength="500"
                  @keydown.enter.exact.prevent="saveEdit(m)"
                />
                <div class="msg-edit-btns">
                  <el-button size="small" type="primary" @click="saveEdit(m)">保存</el-button>
                  <el-button size="small" link @click="editingId = null">取消</el-button>
                </div>
              </div>
              <template v-else>
                <div class="msg-bubble">{{ m.content }}</div>
                <div v-if="m.mine" class="msg-actions">
                  <el-icon title="编辑" @click="startEdit(m)"><EditPen /></el-icon>
                  <el-popconfirm title="删除这条消息？" @confirm="removeMessage(m.id)">
                    <template #reference>
                      <el-icon title="删除"><Delete /></el-icon>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="draft"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 4 }"
              placeholder="输入消息，回车发送"
              maxlength="500"
              @keydown.enter.exact.prevent="send"
            />
            <el-button type="primary" :disabled="!draft.trim()" @click="send">发送</el-button>
          </div>
        </template>
        <div v-else class="main-placeholder">
          <el-icon :size="48" color="#A7F3D0"><ChatDotRound /></el-icon>
          <p>选择左侧会话开始聊天</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { getConversations, getMessages, sendChatMessage, markChatRead, updateChatMessage, deleteChatMessage, deleteConversation } from '@/api/chat'
import { ArrowLeft, ChatDotRound, Search, Delete, EditPen } from '@element-plus/icons-vue'

/**
 * 聊天页：左侧会话列表 + 右侧消息窗口
 * 支持发送/编辑/删除消息、加载历史、删除会话、WebSocket 实时推送
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const conversations = ref([])          // 当前用户所有会话
const messages = ref([])               // 当前聊天窗口的消息列表
const draft = ref('')                  // 输入框草稿
const convKeyword = ref('')            // 会话搜索关键词
const editingId = ref(null)            // 正在编辑的消息 ID
const editDraft = ref('')              // 编辑框草稿
const bodyRef = ref()                  // 消息滚动容器
const hasMore = ref(false)             // 是否还有更多历史消息
const loadingOlder = ref(false)        // 是否正在加载更早消息
const isMobile = ref(window.innerWidth <= 768) // 移动端响应式标识

const peerId = computed(() => route.params.peerId ? Number(route.params.peerId) : null)       // 当前聊天对象用户 ID
const productId = computed(() => route.query.productId ? Number(route.query.productId) : null) // 从商品页带入的商品 ID

// 当前选中的会话对象
const current = computed(() => conversations.value.find(c => c.peerId === peerId.value))
const filteredConversations = computed(() => {
  const kw = convKeyword.value.trim()
  if (!kw) return conversations.value
  return conversations.value.filter(c =>
    (c.peerName || '').includes(kw) || (c.lastContent || '').includes(kw))
})
const peerName = computed(() => current.value?.peerName || '用户' + peerId.value)         // 对方昵称
const peerAvatar = computed(() => current.value?.peerAvatar || undefined)                // 对方头像
const productTitle = ref('')                                                             // 关联商品标题
const myName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '我')
const myAvatar = computed(() => userStore.userInfo?.avatar || undefined)

/**
 * 格式化会话最后一条消息时间：当天显示时分，跨天显示月/日
 * @param {string} t ISO 时间字符串
 * @returns {string} 友好时间
 */
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  return sameDay
    ? `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    : `${d.getMonth() + 1}/${d.getDate()}`
}

/**
 * 加载当前用户会话列表
 */
async function loadConversations() {
  try {
    const res = await getConversations()
    conversations.value = res.data || []
  } catch { /* 会话加载失败不阻塞页面 */ }
}

/**
 * 加载当前聊天窗口的消息、商品标题、刷新未读数与会话列表
 * peerId 变化时自动触发
 */
async function loadMessages() {
  if (!peerId.value) return
  messages.value = []
  hasMore.value = false
  try {
    const res = await getMessages(peerId.value)
    messages.value = res.data || []
    hasMore.value = messages.value.length >= 100
    productTitle.value = messages.value.find(m => m.productTitle)?.productTitle || ''
    chatStore.refreshUnread()
    await loadConversations()
    scrollBottom()
  } catch { /* 忽略 */ }
}

/**
 * 向上翻页加载更早消息
 * 以当前最早一条消息 ID 为游标，加载后保持原有滚动位置
 */
async function loadOlder() {
  if (!peerId.value || !messages.value.length || loadingOlder.value) return
  loadingOlder.value = true
  const body = bodyRef.value
  const prevHeight = body ? body.scrollHeight : 0
  try {
    const res = await getMessages(peerId.value, messages.value[0].id)
    const older = res.data || []
    messages.value = [...older, ...messages.value]
    hasMore.value = older.length >= 100
    nextTick(() => {
      if (body) body.scrollTop = body.scrollHeight - prevHeight
    })
  } catch { /* 忽略 */ } finally {
    loadingOlder.value = false
  }
}

/**
 * 点击会话列表切换聊天对象
 * @param {number} id 对方用户 ID
 */
function openChat(id) {
  router.push({ path: `/chat/${id}`, query: productId.value ? { productId: productId.value } : {} })
}

/**
 * 滚动消息列表到底部（发送或收到新消息后调用）
 */
function scrollBottom() {
  nextTick(() => {
    if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  })
}

/**
 * 发送消息：校验内容 → 调接口 → 追加到列表 → 清空输入框 → 刷新会话
 */
async function send() {
  const content = draft.value.trim()
  if (!content || !peerId.value) return
  try {
    const res = await sendChatMessage({
      receiverId: peerId.value,
      productId: productId.value || undefined,
      content
    })
    messages.value.push(res.data)
    draft.value = ''
    scrollBottom()
    loadConversations()
  } catch { /* 发送失败由拦截器提示 */ }
}

// ====== 改：编辑自己发送的消息 ======
/**
 * 进入消息编辑状态
 * @param {Object} m 消息对象
 */
function startEdit(m) {
  editingId.value = m.id
  editDraft.value = m.content
}

/**
 * 保存编辑后的消息内容
 * @param {Object} m 消息对象
 */
async function saveEdit(m) {
  const content = editDraft.value.trim()
  if (!content) return
  try {
    const res = await updateChatMessage(m.id, content)
    m.content = res.data.content
    editingId.value = null
    loadConversations()
  } catch { /* 编辑失败由拦截器提示 */ }
}

// ====== 删：删除单条消息 / 删除会话 ======
/**
 * 删除单条消息并刷新会话列表
 * @param {number} id 消息 ID
 */
async function removeMessage(id) {
  try {
    await deleteChatMessage(id)
    messages.value = messages.value.filter(x => x.id !== id)
    loadConversations()
  } catch { /* 删除失败由拦截器提示 */ }
}

/**
 * 删除会话：若删除的是当前会话则清空消息并回到空页面
 * @param {number} id 对方用户 ID
 */
async function removeConversation(id) {
  try {
    await deleteConversation(id)
    if (id === peerId.value) {
      messages.value = []
      router.push('/chat')
    }
    loadConversations()
  } catch { /* 删除失败由拦截器提示 */ }
}

/**
 * WebSocket 消息分发：
 * - chat：新消息，当前会话追加并滚动、标记已读；非当前会话仅刷新列表
 * - chat_edit：更新本地消息内容
 * - chat_delete：移除本地消息
 * - chat_clear：清空当前会话本地消息
 */
let offMessage = null
onMounted(() => {
  window.addEventListener('resize', onResize)
  chatStore.connect()
  loadConversations()
  loadMessages()
  offMessage = chatStore.onMessage((data) => {
    if (data.type === 'chat' && data.message) {
      const msg = data.message
      if (msg.senderId === peerId.value) {
        messages.value.push(msg)
        scrollBottom()
        markChatRead(peerId.value).then(() => chatStore.refreshUnread())
      }
      loadConversations()
    } else if (data.type === 'chat_edit' && data.message) {
      const t = messages.value.find(x => x.id === data.message.id)
      if (t) t.content = data.message.content
      loadConversations()
    } else if (data.type === 'chat_delete') {
      messages.value = messages.value.filter(x => x.id !== data.id)
      loadConversations()
    } else if (data.type === 'chat_clear') {
      if (data.peerId === peerId.value) messages.value = []
      loadConversations()
    }
  })
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (offMessage) offMessage()
})

/**
 * 窗口大小变化时更新移动端标识，控制列表/窗口二选一展示
 */
function onResize() {
  isMobile.value = window.innerWidth <= 768
}

// 切换聊天对象时重新加载消息
watch(peerId, () => loadMessages())
</script>

<style scoped>
.chat-container {
  max-width: 1200px;
}
.chat-page {
  display: flex;
  gap: 16px;
  height: calc(100vh - 160px);
  min-height: 480px;
}

/* ====== 会话列表 ====== */
.chat-side {
  width: 300px;
  flex: none;
  background: var(--sh-surface);
  border: 1px solid var(--sh-line-soft);
  border-radius: var(--sh-radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.side-head {
  padding: 14px 18px;
  font-weight: 700;
  color: var(--sh-pine);
  border-bottom: 1px solid var(--sh-line-soft);
}
.side-search {
  padding: 10px 12px;
  border-bottom: 1px solid var(--sh-line-soft);
}
.side-empty {
  padding: 48px 20px;
  text-align: center;
  color: var(--sh-muted);
  font-size: 14px;
  line-height: 2;
}
.side-empty span { font-size: 12px; color: var(--sh-faint); }
.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: background var(--sh-dur-fast) var(--sh-ease);
}
.conv-item:hover { background: var(--sh-line-soft); }
.conv-item.active { background: #ECFDF5; }
.conv-mid { flex: 1; min-width: 0; }
.conv-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--sh-ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.conv-last {
  font-size: 12px;
  color: var(--sh-muted);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-top: 2px;
}
.conv-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.conv-del {
  color: var(--sh-faint);
  cursor: pointer;
  visibility: hidden;
  transition: color var(--sh-dur-fast) var(--sh-ease);
}
.conv-del:hover { color: #EF4444; }
.conv-item:hover .conv-del { visibility: visible; }
.conv-time { font-size: 11px; color: var(--sh-faint); }
.conv-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #EF4444;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

/* ====== 聊天窗口 ====== */
.chat-main {
  flex: 1;
  min-width: 0;
  background: var(--sh-surface);
  border: 1px solid var(--sh-line-soft);
  border-radius: var(--sh-radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.main-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--sh-line-soft);
}
.peer-name { font-weight: 700; color: var(--sh-pine); }
.head-tag { max-width: 260px; overflow: hidden; text-overflow: ellipsis; }

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  background: #F7FBF8;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.body-empty {
  margin: auto;
  color: var(--sh-faint);
  font-size: 13px;
}
.load-older {
  text-align: center;
  padding: 4px 0 8px;
}
.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.msg-row.mine { flex-direction: row-reverse; }
.msg-bubble {
  max-width: 62%;
  padding: 10px 14px;
  border-radius: 12px;
  background: var(--sh-surface);
  border: 1px solid var(--sh-line-soft);
  color: var(--sh-ink);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-row.mine .msg-bubble {
  background: var(--sh-primary);
  border-color: var(--sh-primary);
  color: #fff;
}

/* 悬停显示编辑/删除操作 */
.msg-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--sh-faint);
  visibility: hidden;
}
.msg-actions .el-icon {
  cursor: pointer;
  transition: color var(--sh-dur-fast) var(--sh-ease);
}
.msg-actions .el-icon:hover { color: var(--sh-primary-deep); }
.msg-row.mine:hover .msg-actions { visibility: visible; }

.msg-edit {
  width: 60%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.msg-edit-btns {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid var(--sh-line-soft);
}
.chat-input .el-button { flex: none; }

.main-placeholder {
  margin: auto;
  text-align: center;
  color: var(--sh-faint);
}
.main-placeholder p { margin-top: 10px; font-size: 14px; }

/* ====== 移动端：列表/窗口二选一 ====== */
@media (max-width: 768px) {
  .chat-page { height: calc(100vh - 130px); }
  .chat-side { width: 100%; }
  .side-hidden { display: none; }
  .main-hidden { display: none; }
  .msg-bubble { max-width: 76%; }
}
</style>

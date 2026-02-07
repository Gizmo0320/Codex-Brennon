<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import api from '../api'
import { wsClient } from '../ws'

const channels = ref<any[]>([])
const activeChannel = ref('global')
const messages = ref<any[]>([])
const messageInput = ref('')
const chatContainer = ref<HTMLElement | null>(null)

onMounted(async () => {
  const res = await api.get('/chat/channels')
  channels.value = res.data
  wsClient.on('chat_message', handleChatMessage)
})

onUnmounted(() => { wsClient.off('chat_message', handleChatMessage) })

function handleChatMessage(data: any) {
  messages.value.push({
    sender: data.senderName || data.sender || 'Unknown',
    channel: data.channelId || data.channel || '?',
    message: data.message || data.content || '',
    timestamp: Date.now()
  })
  if (messages.value.length > 200) messages.value.shift()
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

async function sendMessage() {
  if (!messageInput.value.trim()) return
  await api.post('/chat/send', {
    sender: '00000000-0000-0000-0000-000000000000',
    senderName: 'Dashboard',
    channelId: activeChannel.value,
    message: messageInput.value
  })
  messageInput.value = ''
}

function formatTime(ts: number): string {
  return new Date(ts).toLocaleTimeString()
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Chat</h1>
    </div>

    <div class="card" style="margin-bottom: 16px;">
      <div style="display: flex; gap: 8px;">
        <button
          v-for="ch in channels" :key="ch.id"
          @click="activeChannel = ch.id"
          :style="{ background: activeChannel === ch.id ? 'var(--accent)' : 'var(--bg-input)' }"
        >
          {{ ch.displayName }}
        </button>
      </div>
    </div>

    <div class="card chat-box">
      <div ref="chatContainer" class="messages">
        <div v-for="(msg, i) in messages" :key="i" class="chat-msg">
          <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
          <span class="msg-channel">[{{ msg.channel }}]</span>
          <span class="msg-sender">{{ msg.sender }}</span>
          <span class="msg-text">{{ msg.message }}</span>
        </div>
        <div v-if="messages.length === 0" style="color: var(--text-secondary); text-align: center; padding: 40px;">
          Waiting for messages...
        </div>
      </div>
      <div class="chat-input">
        <input v-model="messageInput" placeholder="Type a message..." @keyup.enter="sendMessage" />
        <button @click="sendMessage">Send</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-box { display: flex; flex-direction: column; height: 500px; }
.messages { flex: 1; overflow-y: auto; padding: 8px 0; }
.chat-msg { padding: 4px 0; font-size: 14px; font-family: monospace; }
.msg-time { color: var(--text-secondary); margin-right: 8px; }
.msg-channel { color: var(--accent); margin-right: 4px; }
.msg-sender { color: var(--success); margin-right: 4px; font-weight: 600; }
.msg-text { color: var(--text-primary); }
.chat-input { display: flex; gap: 8px; margin-top: 8px; }
</style>

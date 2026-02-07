<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const id = (route.params.id as string).toUpperCase()
const ticket = ref<any>(null)
const replyContent = ref('')

onMounted(loadTicket)

async function loadTicket() {
  const res = await api.get(`/tickets/${id}`)
  ticket.value = res.data
}

async function sendReply() {
  if (!replyContent.value.trim()) return
  await api.post(`/tickets/${id}/reply`, {
    author: '00000000-0000-0000-0000-000000000000',
    authorName: 'Dashboard',
    content: replyContent.value,
    isStaff: true
  })
  replyContent.value = ''
  await loadTicket()
}

async function setStatus(status: string) {
  await api.put(`/tickets/${id}/status`, { status })
  await loadTicket()
}

async function closeTicket() {
  await api.put(`/tickets/${id}/close`, { closedBy: '00000000-0000-0000-0000-000000000000' })
  await loadTicket()
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Ticket {{ id }}</h1>
      <div style="display: flex; gap: 8px;" v-if="ticket">
        <button @click="setStatus('IN_PROGRESS')">In Progress</button>
        <button class="danger" @click="closeTicket">Close</button>
      </div>
    </div>

    <template v-if="ticket">
      <div class="card">
        <h2>{{ ticket.subject }}</h2>
        <div style="margin-top: 8px; display: flex; gap: 16px; color: var(--text-secondary);">
          <span>Creator: {{ ticket.creatorName }}</span>
          <span>Status: {{ ticket.status }}</span>
          <span>Priority: {{ ticket.priority }}</span>
          <span>Server: {{ ticket.server }}</span>
        </div>
      </div>

      <div class="card">
        <h3 style="margin-bottom: 12px;">Messages</h3>
        <div v-for="(msg, i) in ticket.messages" :key="i" class="message"
          :class="{ staff: msg.isStaffMessage }">
          <div class="msg-header">
            <strong>{{ msg.authorName }}</strong>
            <span v-if="msg.isStaffMessage" class="badge online" style="margin-left: 8px;">Staff</span>
            <span style="color: var(--text-secondary); margin-left: auto; font-size: 12px;">{{ msg.timestamp }}</span>
          </div>
          <div class="msg-content">{{ msg.content }}</div>
        </div>
      </div>

      <div class="card">
        <h3 style="margin-bottom: 8px;">Reply</h3>
        <textarea v-model="replyContent" rows="3" placeholder="Type your reply..."></textarea>
        <button @click="sendReply" style="margin-top: 8px;">Send Reply</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.message {
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 8px;
  background: var(--bg-input);
}
.message.staff { border-left: 3px solid var(--accent); }
.msg-header { display: flex; align-items: center; margin-bottom: 4px; }
.msg-content { color: var(--text-primary); }
</style>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'

const tickets = ref<any[]>([])
const selectedTicket = ref<any>(null)
const showCreate = ref(false)
const createForm = ref({ subject: '', description: '' })
const replyContent = ref('')
const loading = ref(false)

onMounted(loadTickets)

async function loadTickets() {
  const res = await api.get('/player/tickets')
  tickets.value = res.data
}

async function viewTicket(id: string) {
  const res = await api.get(`/player/tickets/${id}`)
  selectedTicket.value = res.data
}

async function createTicket() {
  if (!createForm.value.subject.trim()) {
    alert('Subject is required')
    return
  }
  loading.value = true
  try {
    await api.post('/player/tickets', createForm.value)
    showCreate.value = false
    createForm.value = { subject: '', description: '' }
    await loadTickets()
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to create ticket')
  }
  loading.value = false
}

async function sendReply() {
  if (!replyContent.value.trim() || !selectedTicket.value) return
  loading.value = true
  try {
    await api.post(`/player/tickets/${selectedTicket.value.id}/reply`, {
      content: replyContent.value
    })
    replyContent.value = ''
    await viewTicket(selectedTicket.value.id)
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to send reply')
  }
  loading.value = false
}

function priorityColor(p: string) {
  if (p === 'HIGH' || p === 'URGENT') return 'var(--danger)'
  if (p === 'MEDIUM') return 'var(--warning)'
  return 'var(--text-secondary)'
}
</script>

<template>
  <div class="portal">
    <div class="portal-nav">
      <div class="portal-brand">
        <h2>Brennon</h2>
        <span class="version">Player Portal</span>
      </div>
      <div class="portal-links">
        <router-link to="/portal" class="nav-link">Profile</router-link>
        <router-link to="/portal/appeals" class="nav-link">Appeals</router-link>
        <router-link to="/portal/tickets" class="nav-link" active-class="active">Tickets</router-link>
      </div>
    </div>

    <div class="portal-content">
      <!-- Ticket List -->
      <template v-if="!selectedTicket">
        <div class="page-header">
          <h1>My Tickets</h1>
          <button @click="showCreate = true">New Ticket</button>
        </div>

        <div class="card">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Subject</th>
                <th>Status</th>
                <th>Priority</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in tickets" :key="t.id" @click="viewTicket(t.id)" style="cursor: pointer;">
                <td style="font-family: monospace;">{{ t.id }}</td>
                <td>{{ t.subject }}</td>
                <td>
                  <span class="badge" :class="t.status === 'OPEN' ? 'online' : t.status === 'CLOSED' ? 'offline' : 'pending'">
                    {{ t.status }}
                  </span>
                </td>
                <td :style="{ color: priorityColor(t.priority) }">{{ t.priority }}</td>
              </tr>
              <tr v-if="tickets.length === 0">
                <td colspan="4" style="text-align: center; color: var(--text-secondary);">
                  No tickets yet
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <!-- Ticket Detail -->
      <template v-else>
        <div class="page-header">
          <h1>Ticket {{ selectedTicket.id }}</h1>
          <button style="background: var(--bg-input);" @click="selectedTicket = null">Back to List</button>
        </div>

        <div class="card">
          <h2>{{ selectedTicket.subject }}</h2>
          <div style="margin-top: 8px; display: flex; gap: 16px; color: var(--text-secondary); font-size: 13px;">
            <span>Status: {{ selectedTicket.status }}</span>
            <span>Priority: {{ selectedTicket.priority }}</span>
            <span v-if="selectedTicket.server">Server: {{ selectedTicket.server }}</span>
          </div>
        </div>

        <div class="card">
          <h3 style="margin-bottom: 12px;">Messages</h3>
          <div v-for="(msg, i) in selectedTicket.messages" :key="i" class="message"
            :class="{ staff: msg.isStaffMessage }">
            <div class="msg-header">
              <strong>{{ msg.authorName }}</strong>
              <span v-if="msg.isStaffMessage" class="badge online" style="margin-left: 8px;">Staff</span>
              <span style="color: var(--text-secondary); margin-left: auto; font-size: 12px;">{{ msg.timestamp }}</span>
            </div>
            <div class="msg-content">{{ msg.content }}</div>
          </div>
          <div v-if="!selectedTicket.messages?.length"
            style="color: var(--text-secondary); text-align: center; padding: 12px;">
            No messages yet
          </div>
        </div>

        <div v-if="selectedTicket.status !== 'CLOSED'" class="card">
          <h3 style="margin-bottom: 8px;">Reply</h3>
          <textarea v-model="replyContent" rows="3" placeholder="Type your reply..."></textarea>
          <button @click="sendReply" :disabled="loading" style="margin-top: 8px;">Send Reply</button>
        </div>
      </template>

      <!-- Create Ticket Modal -->
      <teleport to="body">
        <div v-if="showCreate" class="overlay" @click.self="showCreate = false">
          <div class="dialog card">
            <h2 style="margin-bottom: 16px;">Create Ticket</h2>

            <div class="form-group">
              <label>Subject</label>
              <input v-model="createForm.subject" placeholder="Brief description of your issue" />
            </div>

            <div class="form-group">
              <label>Description</label>
              <textarea v-model="createForm.description" rows="4"
                placeholder="Provide details about your issue..."></textarea>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 8px;">
              <button style="background: var(--bg-input);" @click="showCreate = false">Cancel</button>
              <button :disabled="loading" @click="createTicket">
                {{ loading ? 'Creating...' : 'Create Ticket' }}
              </button>
            </div>
          </div>
        </div>
      </teleport>
    </div>
  </div>
</template>

<style scoped>
.portal-nav {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid #2d3748;
}
.portal-brand { display: flex; align-items: center; gap: 8px; }
.portal-brand h2 { font-size: 18px; font-weight: 700; }
.version { color: var(--text-secondary); font-size: 12px; }
.portal-links { display: flex; gap: 4px; margin-left: 24px; }
.nav-link {
  padding: 8px 16px;
  border-radius: 6px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.15s;
}
.nav-link:hover { background: rgba(99, 102, 241, 0.1); color: var(--text-primary); }
.nav-link.active { background: var(--accent); color: white; }
.portal-content { padding: 24px; max-width: 1000px; margin: 0 auto; }
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}
.dialog { min-width: 460px; max-width: 540px; }
.badge.pending { background: rgba(245, 158, 11, 0.2); color: var(--warning); }
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

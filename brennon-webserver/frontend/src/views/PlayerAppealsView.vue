<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const appeals = ref<any[]>([])
const punishments = ref<any[]>([])
const showCreate = ref(false)
const createForm = ref({ punishmentId: '', reason: '' })
const loading = ref(false)

onMounted(async () => {
  await loadData()
  // Pre-fill from query param if coming from profile
  const pid = route.query.punishmentId as string
  if (pid) {
    createForm.value.punishmentId = pid
    showCreate.value = true
  }
})

async function loadData() {
  const [appealsRes, punishRes] = await Promise.all([
    api.get('/appeals/mine'),
    api.get('/player/punishments').catch(() => ({ data: [] }))
  ])
  appeals.value = appealsRes.data
  punishments.value = punishRes.data
}

async function submitAppeal() {
  if (!createForm.value.punishmentId || !createForm.value.reason.trim()) {
    alert('Please select a punishment and provide a reason')
    return
  }
  loading.value = true
  try {
    await api.post('/appeals', createForm.value)
    showCreate.value = false
    createForm.value = { punishmentId: '', reason: '' }
    await loadData()
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to submit appeal')
  }
  loading.value = false
}

function formatDate(ts: number) {
  return new Date(ts).toLocaleString()
}

function statusClass(status: string) {
  if (status === 'PENDING') return 'pending'
  if (status === 'APPROVED') return 'online'
  return 'offline'
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
        <router-link to="/portal/appeals" class="nav-link" active-class="active">Appeals</router-link>
        <router-link to="/portal/tickets" class="nav-link">Tickets</router-link>
      </div>
    </div>

    <div class="portal-content">
      <div class="page-header">
        <h1>My Appeals</h1>
        <button @click="showCreate = true" v-if="punishments.length > 0">New Appeal</button>
      </div>

      <div class="card">
        <table>
          <thead>
            <tr>
              <th>Punishment</th>
              <th>Reason</th>
              <th>Status</th>
              <th>Submitted</th>
              <th>Staff Response</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in appeals" :key="a.id">
              <td style="font-family: monospace; font-size: 12px;">{{ a.punishmentId.substring(0, 8) }}...</td>
              <td>{{ a.reason.length > 40 ? a.reason.substring(0, 40) + '...' : a.reason }}</td>
              <td><span class="badge" :class="statusClass(a.status)">{{ a.status }}</span></td>
              <td>{{ formatDate(a.createdAt) }}</td>
              <td>{{ a.staffResponse || '-' }}</td>
            </tr>
            <tr v-if="appeals.length === 0">
              <td colspan="5" style="text-align: center; color: var(--text-secondary);">
                No appeals submitted
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <teleport to="body">
        <div v-if="showCreate" class="overlay" @click.self="showCreate = false">
          <div class="dialog card">
            <h2 style="margin-bottom: 16px;">Submit Appeal</h2>

            <div class="form-group">
              <label>Punishment</label>
              <select v-model="createForm.punishmentId">
                <option value="" disabled>Select a punishment...</option>
                <option v-for="p in punishments" :key="p.id" :value="p.id">
                  {{ p.type }} - {{ p.reason }} ({{ p.isPermanent ? 'Permanent' : new Date(p.expiresAt).toLocaleDateString() }})
                </option>
              </select>
            </div>

            <div class="form-group">
              <label>Why should this be appealed?</label>
              <textarea v-model="createForm.reason" rows="5"
                placeholder="Explain why you believe this punishment should be reversed..."></textarea>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 8px;">
              <button style="background: var(--bg-input);" @click="showCreate = false">Cancel</button>
              <button :disabled="loading" @click="submitAppeal">
                {{ loading ? 'Submitting...' : 'Submit Appeal' }}
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
</style>

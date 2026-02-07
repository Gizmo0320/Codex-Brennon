<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'

const appeals = ref<any[]>([])
const selectedAppeal = ref<any>(null)
const showResolve = ref(false)
const resolveForm = ref({ status: 'APPROVED', response: '' })
const loading = ref(false)

onMounted(loadAppeals)

async function loadAppeals() {
  const res = await api.get('/appeals')
  appeals.value = res.data
}

async function viewAppeal(id: string) {
  const res = await api.get(`/appeals/${id}`)
  selectedAppeal.value = res.data
  showResolve.value = true
  resolveForm.value = { status: 'APPROVED', response: '' }
}

async function resolve() {
  if (!selectedAppeal.value) return
  loading.value = true
  try {
    await api.put(`/appeals/${selectedAppeal.value.id}/resolve`, resolveForm.value)
    showResolve.value = false
    selectedAppeal.value = null
    await loadAppeals()
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to resolve appeal')
  }
  loading.value = false
}

function formatDate(ts: number) {
  return new Date(ts).toLocaleString()
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Ban Appeals</h1>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>Player</th>
            <th>Reason</th>
            <th>Status</th>
            <th>Submitted</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in appeals" :key="a.id">
            <td>{{ a.playerName }}</td>
            <td>{{ a.reason.length > 60 ? a.reason.substring(0, 60) + '...' : a.reason }}</td>
            <td>
              <span class="badge" :class="a.status === 'PENDING' ? 'pending' : a.status === 'APPROVED' ? 'online' : 'offline'">
                {{ a.status }}
              </span>
            </td>
            <td>{{ formatDate(a.createdAt) }}</td>
            <td>
              <button v-if="a.status === 'PENDING'" @click="viewAppeal(a.id)">Review</button>
            </td>
          </tr>
          <tr v-if="appeals.length === 0">
            <td colspan="5" style="text-align: center; color: var(--text-secondary);">No pending appeals</td>
          </tr>
        </tbody>
      </table>
    </div>

    <teleport to="body">
      <div v-if="showResolve" class="overlay" @click.self="showResolve = false">
        <div class="dialog card">
          <h2 style="margin-bottom: 16px;">Review Appeal</h2>

          <template v-if="selectedAppeal">
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">Player</span>
                <span>{{ selectedAppeal.playerName }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Punishment ID</span>
                <span style="font-family: monospace; font-size: 12px;">{{ selectedAppeal.punishmentId }}</span>
              </div>
            </div>

            <div v-if="selectedAppeal.punishment" class="card" style="background: var(--bg-input); margin: 12px 0;">
              <strong>Punishment Details</strong>
              <div style="margin-top: 8px; color: var(--text-secondary); font-size: 13px;">
                <div>Type: <span class="badge offline">{{ selectedAppeal.punishment.type }}</span></div>
                <div style="margin-top: 4px;">Reason: {{ selectedAppeal.punishment.reason }}</div>
                <div style="margin-top: 4px;">Issued: {{ selectedAppeal.punishment.issuedAt }}</div>
                <div v-if="selectedAppeal.punishment.expiresAt" style="margin-top: 4px;">
                  Expires: {{ selectedAppeal.punishment.expiresAt }}
                </div>
                <div v-if="selectedAppeal.punishment.isPermanent" style="margin-top: 4px; color: var(--danger);">
                  Permanent Ban
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>Player's Appeal</label>
              <div class="appeal-text">{{ selectedAppeal.reason }}</div>
            </div>

            <div class="form-group">
              <label>Decision</label>
              <select v-model="resolveForm.status">
                <option value="APPROVED">Approve (Unban)</option>
                <option value="DENIED">Deny</option>
              </select>
            </div>

            <div class="form-group">
              <label>Staff Response</label>
              <textarea v-model="resolveForm.response" rows="3" placeholder="Reason for your decision..."></textarea>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 8px;">
              <button style="background: var(--bg-input);" @click="showResolve = false">Cancel</button>
              <button :class="resolveForm.status === 'APPROVED' ? 'success' : 'danger'"
                :disabled="loading" @click="resolve">
                {{ resolveForm.status === 'APPROVED' ? 'Approve & Unban' : 'Deny Appeal' }}
              </button>
            </div>
          </template>
        </div>
      </div>
    </teleport>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}
.dialog {
  min-width: 500px;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}
.badge.pending {
  background: rgba(245, 158, 11, 0.2);
  color: var(--warning);
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-label {
  color: var(--text-secondary);
  font-size: 12px;
  text-transform: uppercase;
}
.appeal-text {
  padding: 12px;
  background: var(--bg-input);
  border-radius: 6px;
  color: var(--text-primary);
  white-space: pre-wrap;
}
</style>

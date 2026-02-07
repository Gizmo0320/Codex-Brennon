<script setup lang="ts">
import { ref } from 'vue'
import api from '../api'

const searchUuid = ref('')
const punishments = ref<any[]>([])
const searched = ref(false)

const showAction = ref(false)
const actionType = ref('ban')
const form = ref({ target: '', reason: '', issuer: '00000000-0000-0000-0000-000000000000', duration: 0 })

async function search() {
  if (!searchUuid.value) return
  try {
    // Try by name first
    const playerRes = await api.get(`/players/name/${searchUuid.value}`)
    const uuid = playerRes.data.uuid
    searchUuid.value = uuid
    const res = await api.get(`/punishments/${uuid}`)
    punishments.value = res.data
  } catch {
    try {
      const res = await api.get(`/punishments/${searchUuid.value}`)
      punishments.value = res.data
    } catch {
      punishments.value = []
    }
  }
  searched.value = true
}

function openAction(type: string) {
  actionType.value = type
  form.value = { target: searchUuid.value, reason: '', issuer: '00000000-0000-0000-0000-000000000000', duration: 0 }
  showAction.value = true
}

async function executeAction() {
  try {
    await api.post(`/punishments/${actionType.value}`, form.value)
    showAction.value = false
    if (searchUuid.value) await search()
  } catch (e: any) {
    alert(e.response?.data?.error || 'Action failed')
  }
}

async function unban() {
  await api.delete(`/punishments/unban/${searchUuid.value}`)
  await search()
}

async function unmute() {
  await api.delete(`/punishments/unmute/${searchUuid.value}`)
  await search()
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Punishments</h1>
    </div>

    <div class="card" style="margin-bottom: 16px;">
      <div style="display: flex; gap: 8px;">
        <input v-model="searchUuid" placeholder="Player name or UUID..." @keyup.enter="search" />
        <button @click="search">Search</button>
      </div>
    </div>

    <div v-if="searched" class="card" style="margin-bottom: 16px;">
      <div style="display: flex; gap: 8px; flex-wrap: wrap;">
        <button @click="openAction('ban')">Ban</button>
        <button @click="openAction('mute')">Mute</button>
        <button @click="openAction('kick')">Kick</button>
        <button @click="openAction('warn')">Warn</button>
        <button class="success" @click="unban">Unban</button>
        <button class="success" @click="unmute">Unmute</button>
      </div>
    </div>

    <div class="card" v-if="searched">
      <h3 style="margin-bottom: 12px;">History ({{ punishments.length }})</h3>
      <table>
        <thead>
          <tr><th>Type</th><th>Reason</th><th>Issued</th><th>Expires</th><th>Active</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in punishments" :key="p.id">
            <td>{{ p.type }}</td>
            <td>{{ p.reason }}</td>
            <td>{{ p.issuedAt }}</td>
            <td>{{ p.isPermanent ? 'Permanent' : (p.expiresAt || 'N/A') }}</td>
            <td>
              <span :class="['badge', p.isActive ? 'online' : 'offline']">
                {{ p.isActive ? 'Active' : 'Expired' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <teleport to="body">
      <div v-if="showAction" class="overlay" @click.self="showAction = false">
        <div class="dialog card" style="min-width: 400px;">
          <h3>{{ actionType.charAt(0).toUpperCase() + actionType.slice(1) }} Player</h3>
          <form @submit.prevent="executeAction">
            <div class="form-group">
              <label>Target UUID</label>
              <input v-model="form.target" required />
            </div>
            <div class="form-group">
              <label>Reason</label>
              <input v-model="form.reason" required />
            </div>
            <div class="form-group" v-if="actionType === 'ban' || actionType === 'mute'">
              <label>Duration (ms, 0 = permanent)</label>
              <input v-model.number="form.duration" type="number" />
            </div>
            <div style="display: flex; gap: 8px; justify-content: flex-end;">
              <button type="button" @click="showAction = false" style="background: var(--bg-input);">Cancel</button>
              <button type="submit" class="danger">Execute</button>
            </div>
          </form>
        </div>
      </div>
    </teleport>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
</style>

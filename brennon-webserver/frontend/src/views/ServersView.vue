<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api'
import { wsClient } from '../ws'
import StatCard from '../components/StatCard.vue'

const servers = ref<any[]>([])
const totalPlayers = ref(0)
const sendUuid = ref('')
const sendServer = ref('')

async function loadServers() {
  const res = await api.get('/servers/online')
  totalPlayers.value = res.data.totalPlayers

  const allRes = await api.get('/servers')
  servers.value = allRes.data
}

async function sendPlayer() {
  if (!sendUuid.value || !sendServer.value) return
  try {
    // Try to resolve name to UUID
    const playerRes = await api.get(`/players/name/${sendUuid.value}`)
    await api.post('/servers/send', { uuid: playerRes.data.uuid, server: sendServer.value })
    alert('Player sent!')
  } catch (e: any) {
    alert(e.response?.data?.error || 'Failed to send player')
  }
}

function handleServerStatus() { loadServers() }

onMounted(() => {
  loadServers()
  wsClient.on('server_status', handleServerStatus)
})
onUnmounted(() => { wsClient.off('server_status', handleServerStatus) })
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Servers</h1>
    </div>

    <div class="stat-grid">
      <StatCard label="Total Players" :value="totalPlayers" color="var(--success)" />
      <StatCard label="Servers" :value="servers.length" color="var(--accent)" />
    </div>

    <div class="card" style="margin-bottom: 16px;">
      <h3 style="margin-bottom: 8px;">Send Player</h3>
      <div style="display: flex; gap: 8px;">
        <input v-model="sendUuid" placeholder="Player name..." />
        <input v-model="sendServer" placeholder="Server name..." />
        <button @click="sendPlayer">Send</button>
      </div>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr><th>Name</th><th>Group</th><th>Players</th><th>Max</th><th>Status</th></tr>
        </thead>
        <tbody>
          <tr v-for="s in servers" :key="s.name">
            <td>{{ s.name }}</td>
            <td>{{ s.group }}</td>
            <td>{{ s.playerCount }}</td>
            <td>{{ s.maxPlayers }}</td>
            <td>
              <span :class="['badge', s.online ? 'online' : 'offline']">
                {{ s.online ? 'Online' : 'Offline' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

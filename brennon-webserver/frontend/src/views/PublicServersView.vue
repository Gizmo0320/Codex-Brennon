<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

interface Server {
  name: string
  group: string
  playerCount: number
  maxPlayers: number
  online: boolean
}

const servers = ref<Server[]>([])
const totalPlayers = ref(0)
const loading = ref(true)

async function fetchServers() {
  loading.value = true
  try {
    const res = await axios.get('/api/public/servers')
    servers.value = res.data.servers
    totalPlayers.value = res.data.totalPlayers
  } catch {
    servers.value = []
  }
  loading.value = false
}

onMounted(fetchServers)
</script>

<template>
  <div class="public-servers">
    <div class="page-header">
      <div>
        <h1>Servers</h1>
        <p class="subtitle">{{ totalPlayers }} players online across {{ servers.length }} servers</p>
      </div>
    </div>

    <div v-if="loading" class="loading">Loading...</div>

    <div v-else-if="servers.length === 0" class="card empty">
      No servers online.
    </div>

    <div v-else class="server-grid">
      <div v-for="server in servers" :key="server.name" class="server-card card">
        <div class="server-header">
          <span class="server-name">{{ server.name }}</span>
          <span :class="['badge', server.online ? 'online' : 'offline']">
            {{ server.online ? 'Online' : 'Offline' }}
          </span>
        </div>
        <div class="server-group" v-if="server.group">{{ server.group }}</div>
        <div class="server-bar">
          <div class="bar-fill" :style="{ width: (server.playerCount / Math.max(server.maxPlayers, 1)) * 100 + '%' }"></div>
        </div>
        <div class="server-count">{{ server.playerCount }} / {{ server.maxPlayers }} players</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.public-servers {
  max-width: 1000px;
  margin: 0 auto;
}

.subtitle {
  color: var(--text-muted);
  margin: 4px 0 0;
}

.loading, .empty {
  text-align: center;
  padding: 48px;
  color: var(--text-muted);
}

.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.server-card {
  padding: 20px;
}

.server-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.server-name {
  font-weight: 600;
  font-size: 16px;
}

.server-group {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 12px;
}

.server-bar {
  height: 6px;
  background: var(--bg-input);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}

.bar-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 3px;
  transition: width 0.3s;
}

.server-count {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>

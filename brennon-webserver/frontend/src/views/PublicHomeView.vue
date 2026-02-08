<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

interface ServerInfo {
  name: string
  group: string
  playerCount: number
  maxPlayers: number
}

const networkName = ref('Network')
const onlinePlayers = ref(0)
const totalPlayers = ref(0)
const serverCount = ref(0)
const servers = ref<ServerInfo[]>([])
const loading = ref(true)

async function fetchNetwork() {
  loading.value = true
  try {
    const res = await axios.get('/api/public/network')
    networkName.value = res.data.networkName || 'Network'
    onlinePlayers.value = res.data.onlinePlayers
    totalPlayers.value = res.data.totalPlayers
    serverCount.value = res.data.serverCount
    servers.value = res.data.servers
  } catch {
    // Silently fail
  }
  loading.value = false
}

onMounted(fetchNetwork)
</script>

<template>
  <div class="public-home">
    <div class="hero">
      <h1>{{ networkName }}</h1>
      <p class="hero-subtitle">Welcome to our Minecraft network</p>
    </div>

    <div v-if="loading" class="loading">Loading...</div>

    <template v-else>
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-value">{{ onlinePlayers }}</div>
          <div class="stat-label">Players Online</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ totalPlayers }}</div>
          <div class="stat-label">Total Players</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ serverCount }}</div>
          <div class="stat-label">Servers Online</div>
        </div>
      </div>

      <div class="quick-links">
        <h2>Explore</h2>
        <div class="link-grid">
          <router-link to="/servers" class="link-card">
            <div class="link-icon">S</div>
            <div class="link-info">
              <div class="link-title">Servers</div>
              <div class="link-desc">View all servers and player counts</div>
            </div>
          </router-link>
          <router-link to="/players" class="link-card">
            <div class="link-icon">P</div>
            <div class="link-info">
              <div class="link-title">Players</div>
              <div class="link-desc">Browse our player directory</div>
            </div>
          </router-link>
          <router-link to="/bans" class="link-card">
            <div class="link-icon">B</div>
            <div class="link-info">
              <div class="link-title">Ban List</div>
              <div class="link-desc">Public record of network bans</div>
            </div>
          </router-link>
          <router-link to="/leaderboard" class="link-card">
            <div class="link-icon">L</div>
            <div class="link-info">
              <div class="link-title">Leaderboard</div>
              <div class="link-desc">Top players and statistics</div>
            </div>
          </router-link>
        </div>
      </div>

      <div v-if="servers.length > 0" class="server-preview">
        <h2>Servers</h2>
        <div class="server-grid">
          <div v-for="server in servers" :key="server.name" class="server-card">
            <div class="server-name">{{ server.name }}</div>
            <div class="server-players">{{ server.playerCount }} / {{ server.maxPlayers }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.public-home {
  max-width: 1000px;
  margin: 0 auto;
}

.hero {
  text-align: center;
  padding: 48px 0 32px;
}

.hero h1 {
  font-size: 36px;
  font-weight: 700;
  margin: 0;
}

.hero-subtitle {
  color: var(--text-secondary);
  margin: 8px 0 0;
  font-size: 16px;
}

.loading {
  text-align: center;
  padding: 48px;
  color: var(--text-muted);
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: 8px;
  padding: 24px;
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--accent);
}

.stat-label {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 4px;
}

.quick-links {
  margin-bottom: 32px;
}

.quick-links h2, .server-preview h2 {
  font-size: 20px;
  margin: 0 0 16px;
}

.link-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.link-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-card);
  border-radius: 8px;
  padding: 16px 20px;
  text-decoration: none;
  transition: all 0.15s;
}

.link-card:hover {
  background: var(--bg-input);
}

.link-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.15);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.link-title {
  font-weight: 600;
  color: var(--text-primary);
}

.link-desc {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 2px;
}

.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.server-card {
  background: var(--bg-card);
  border-radius: 8px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.server-name {
  font-weight: 600;
}

.server-players {
  color: var(--text-secondary);
  font-size: 14px;
}
</style>

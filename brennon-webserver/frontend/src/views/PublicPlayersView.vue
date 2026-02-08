<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'

interface Player {
  uuid: string
  name: string
  primaryRank: string
  firstJoin: number
  lastSeen: number
  online: boolean
}

const players = ref<Player[]>([])
const total = ref(0)
const limit = ref(20)
const offset = ref(0)
const loading = ref(false)

const totalPages = computed(() => Math.ceil(total.value / limit.value))
const currentPage = computed(() => Math.floor(offset.value / limit.value) + 1)

async function fetchPlayers() {
  loading.value = true
  try {
    const res = await axios.get('/api/public/players', {
      params: { limit: limit.value, offset: offset.value }
    })
    players.value = res.data.players
    total.value = res.data.total
  } catch {
    players.value = []
  }
  loading.value = false
}

function goToPage(page: number) {
  offset.value = (page - 1) * limit.value
  fetchPlayers()
}

function formatDate(ms: number): string {
  return new Date(ms).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric'
  })
}

function timeAgo(ms: number): string {
  const seconds = Math.floor((Date.now() - ms) / 1000)
  if (seconds < 60) return 'Just now'
  if (seconds < 3600) return Math.floor(seconds / 60) + 'm ago'
  if (seconds < 86400) return Math.floor(seconds / 3600) + 'h ago'
  if (seconds < 604800) return Math.floor(seconds / 86400) + 'd ago'
  return formatDate(ms)
}

onMounted(fetchPlayers)
</script>

<template>
  <div class="public-players">
    <div class="page-header">
      <div>
        <h1>Players</h1>
        <p class="subtitle">{{ total }} total players</p>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="loading">Loading...</div>
      <table v-else>
        <thead>
          <tr>
            <th>Player</th>
            <th>Rank</th>
            <th>First Joined</th>
            <th>Last Seen</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="player in players" :key="player.uuid">
            <td>
              <router-link :to="'/players/' + player.uuid" class="player-link">
                {{ player.name }}
              </router-link>
            </td>
            <td>{{ player.primaryRank }}</td>
            <td>{{ formatDate(player.firstJoin) }}</td>
            <td>{{ player.online ? 'Now' : timeAgo(player.lastSeen) }}</td>
            <td>
              <span :class="['badge', player.online ? 'online' : 'offline']">
                {{ player.online ? 'Online' : 'Offline' }}
              </span>
            </td>
          </tr>
          <tr v-if="players.length === 0">
            <td colspan="5" style="text-align: center; color: var(--text-muted);">No players found.</td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">Prev</button>
        <span class="page-info">Page {{ currentPage }} of {{ totalPages }}</span>
        <button :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">Next</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.public-players {
  max-width: 1000px;
  margin: 0 auto;
}

.subtitle {
  color: var(--text-muted);
  margin: 4px 0 0;
}

.loading {
  text-align: center;
  padding: 32px;
  color: var(--text-muted);
}

.player-link {
  color: var(--accent);
  text-decoration: none;
}

.player-link:hover {
  text-decoration: underline;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.page-info {
  color: var(--text-muted);
  font-size: 0.9em;
}
</style>

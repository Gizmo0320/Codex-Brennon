<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

interface PlayerProfile {
  uuid: string
  name: string
  ranks: { id: string; displayName: string }[]
  firstJoin: string
  lastSeen: string
  online: boolean
}

interface Ban {
  id: string
  reason: string
  issuedAt: number
  expiresAt: number | null
  isPermanent: boolean
  status: string
  appealStatus: string
}

const route = useRoute()
const player = ref<PlayerProfile | null>(null)
const bans = ref<Ban[]>([])
const loading = ref(true)
const notFound = ref(false)

async function fetchPlayer() {
  loading.value = true
  const uuid = route.params.uuid as string
  try {
    const res = await axios.get(`/api/public/player/${uuid}`)
    player.value = res.data
  } catch {
    notFound.value = true
    loading.value = false
    return
  }

  try {
    const banRes = await axios.get(`/api/public/bans/${uuid}`)
    bans.value = banRes.data.bans || []
  } catch {
    bans.value = []
  }

  loading.value = false
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric'
  })
}

function formatTimestamp(ms: number): string {
  return new Date(ms).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

onMounted(fetchPlayer)
</script>

<template>
  <div class="public-player">
    <div v-if="loading" class="loading">Loading...</div>

    <div v-else-if="notFound" class="not-found card">
      <h2>Player Not Found</h2>
      <p>The player you are looking for does not exist or has never joined the network.</p>
      <router-link to="/players">Back to Players</router-link>
    </div>

    <template v-else-if="player">
      <div class="page-header">
        <div>
          <h1>{{ player.name }}</h1>
          <span :class="['badge', player.online ? 'online' : 'offline']">
            {{ player.online ? 'Online' : 'Offline' }}
          </span>
        </div>
      </div>

      <div class="info-grid">
        <div class="card info-card">
          <div class="info-label">Ranks</div>
          <div class="info-value">
            <span v-for="rank in player.ranks" :key="rank.id" class="rank-badge">
              {{ rank.displayName }}
            </span>
            <span v-if="player.ranks.length === 0" class="text-muted">None</span>
          </div>
        </div>
        <div class="card info-card">
          <div class="info-label">First Joined</div>
          <div class="info-value">{{ formatDate(player.firstJoin) }}</div>
        </div>
        <div class="card info-card">
          <div class="info-label">Last Seen</div>
          <div class="info-value">{{ player.online ? 'Now' : formatDate(player.lastSeen) }}</div>
        </div>
      </div>

      <div v-if="bans.length > 0" class="bans-section">
        <h2>Punishment History</h2>
        <div class="card">
          <table>
            <thead>
              <tr>
                <th>Reason</th>
                <th>Date</th>
                <th>Expires</th>
                <th>Status</th>
                <th>Appeal</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ban in bans" :key="ban.id">
                <td>{{ ban.reason }}</td>
                <td>{{ formatTimestamp(ban.issuedAt) }}</td>
                <td>{{ ban.isPermanent ? 'Permanent' : (ban.expiresAt ? formatTimestamp(ban.expiresAt) : 'N/A') }}</td>
                <td>
                  <span :class="['badge', ban.status === 'ACTIVE' ? 'online' : 'offline']">
                    {{ ban.status }}
                  </span>
                </td>
                <td>
                  <span v-if="ban.appealStatus !== 'NONE'" class="text-muted">{{ ban.appealStatus }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.public-player {
  max-width: 900px;
  margin: 0 auto;
}

.loading {
  text-align: center;
  padding: 48px;
  color: var(--text-muted);
}

.not-found {
  text-align: center;
  padding: 48px;
}

.not-found h2 {
  margin: 0 0 8px;
}

.not-found p {
  color: var(--text-muted);
  margin-bottom: 16px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  display: inline;
  margin-right: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.info-card {
  padding: 16px 20px;
}

.info-label {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 4px;
}

.info-value {
  font-weight: 600;
}

.rank-badge {
  display: inline-block;
  background: rgba(99, 102, 241, 0.15);
  color: var(--accent);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
  margin-right: 4px;
}

.text-muted {
  color: var(--text-muted);
}

.bans-section {
  margin-top: 8px;
}

.bans-section h2 {
  font-size: 20px;
  margin: 0 0 16px;
}
</style>

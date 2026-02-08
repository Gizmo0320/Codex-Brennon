<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'

interface Ban {
  id: string
  playerUuid: string
  playerName: string
  reason: string
  issuedAt: number
  expiresAt: number | null
  isPermanent: boolean
  status: string
  appealStatus: string
}

const bans = ref<Ban[]>([])
const total = ref(0)
const limit = ref(20)
const offset = ref(0)
const loading = ref(false)
const searchQuery = ref('')

const totalPages = computed(() => Math.ceil(total.value / limit.value))
const currentPage = computed(() => Math.floor(offset.value / limit.value) + 1)

const filteredBans = computed(() => {
  if (!searchQuery.value) return bans.value
  const q = searchQuery.value.toLowerCase()
  return bans.value.filter(b => b.playerName.toLowerCase().includes(q))
})

async function fetchBans() {
  loading.value = true
  try {
    const res = await axios.get('/api/public/bans', {
      params: { limit: limit.value, offset: offset.value }
    })
    bans.value = res.data.bans
    total.value = res.data.total
  } catch {
    bans.value = []
  }
  loading.value = false
}

function goToPage(page: number) {
  offset.value = (page - 1) * limit.value
  fetchBans()
}

function formatDate(ms: number): string {
  return new Date(ms).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

function formatExpiry(ban: Ban): string {
  if (ban.isPermanent) return 'Permanent'
  if (!ban.expiresAt) return 'N/A'
  return formatDate(ban.expiresAt)
}

onMounted(fetchBans)
</script>

<template>
  <div class="public-bans">
    <div class="page-header">
      <h1>Ban List</h1>
      <p class="subtitle">Public record of network bans</p>
    </div>

    <div class="card" style="margin-bottom: 16px;">
      <input
        v-model="searchQuery"
        placeholder="Filter by player name..."
        class="search-input"
      />
    </div>

    <div class="card">
      <div v-if="loading" class="loading">Loading...</div>
      <table v-else>
        <thead>
          <tr>
            <th>Player</th>
            <th>Reason</th>
            <th>Date</th>
            <th>Expires</th>
            <th>Status</th>
            <th>Appeal</th>
            <th>ID</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ban in filteredBans" :key="ban.id">
            <td>
              <router-link :to="'/players/' + ban.playerUuid" class="player-link">
                {{ ban.playerName }}
              </router-link>
            </td>
            <td>{{ ban.reason }}</td>
            <td>{{ formatDate(ban.issuedAt) }}</td>
            <td>{{ formatExpiry(ban) }}</td>
            <td>
              <span :class="['badge', statusClass(ban.status)]">
                {{ ban.status }}
              </span>
            </td>
            <td>
              <span v-if="ban.appealStatus !== 'NONE'" :class="['badge', appealClass(ban.appealStatus)]">
                {{ ban.appealStatus }}
              </span>
            </td>
            <td class="id-cell">{{ ban.id.substring(0, 8) }}</td>
          </tr>
          <tr v-if="filteredBans.length === 0">
            <td colspan="7" style="text-align: center; color: var(--text-muted);">No bans found.</td>
          </tr>
        </tbody>
      </table>

      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">Prev</button>
        <span class="page-info">Page {{ currentPage }} of {{ totalPages }} ({{ total }} total)</span>
        <button :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">Next</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  methods: {
    statusClass(status: string): string {
      switch (status) {
        case 'ACTIVE': return 'danger'
        case 'EXPIRED': return 'offline'
        case 'REVOKED': return 'success'
        default: return ''
      }
    },
    appealClass(status: string): string {
      switch (status) {
        case 'PENDING': return 'warning'
        case 'APPROVED': return 'success'
        case 'DENIED': return 'danger'
        default: return ''
      }
    }
  }
}
</script>

<style scoped>
.public-bans {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  color: var(--text-primary);
}

.subtitle {
  color: var(--text-muted);
  margin: 4px 0 0;
}

.search-input {
  width: 100%;
  box-sizing: border-box;
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

.id-cell {
  font-family: monospace;
  color: var(--text-muted);
  font-size: 0.85em;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.8em;
  font-weight: 600;
  text-transform: uppercase;
}

.badge.danger { background: rgba(255, 69, 58, 0.2); color: #ff453a; }
.badge.success { background: rgba(50, 215, 75, 0.2); color: #32d74b; }
.badge.warning { background: rgba(255, 214, 10, 0.2); color: #ffd60a; }
.badge.offline { background: rgba(152, 152, 157, 0.2); color: #98989d; }

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

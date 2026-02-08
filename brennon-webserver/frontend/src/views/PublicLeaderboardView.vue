<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'

interface LeaderboardEntry {
  rank: number
  uuid: string
  name: string
  value: number
}

const statOptions = [
  { id: 'playtime', label: 'Playtime' },
  { id: 'kills', label: 'Kills' },
  { id: 'deaths', label: 'Deaths' },
  { id: 'blocks_placed', label: 'Blocks Placed' },
  { id: 'blocks_broken', label: 'Blocks Broken' },
  { id: 'messages_sent', label: 'Messages Sent' },
]

const selectedStat = ref('playtime')
const entries = ref<LeaderboardEntry[]>([])
const loading = ref(false)

async function fetchLeaderboard() {
  loading.value = true
  try {
    const res = await axios.get(`/api/public/leaderboard/${selectedStat.value}`, {
      params: { limit: 25 }
    })
    entries.value = res.data.entries || []
  } catch {
    entries.value = []
  }
  loading.value = false
}

function formatValue(stat: string, value: number): string {
  if (stat === 'playtime') {
    const hours = Math.floor(value / 3600)
    const minutes = Math.floor((value % 3600) / 60)
    return `${hours}h ${minutes}m`
  }
  return value.toLocaleString()
}

watch(selectedStat, fetchLeaderboard)
onMounted(fetchLeaderboard)
</script>

<template>
  <div class="public-leaderboard">
    <div class="page-header">
      <div>
        <h1>Leaderboard</h1>
        <p class="subtitle">Top players across the network</p>
      </div>
    </div>

    <div class="card" style="margin-bottom: 16px;">
      <div class="stat-selector">
        <button
          v-for="stat in statOptions"
          :key="stat.id"
          :class="['stat-btn', { active: selectedStat === stat.id }]"
          @click="selectedStat = stat.id"
        >
          {{ stat.label }}
        </button>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="loading">Loading...</div>
      <table v-else>
        <thead>
          <tr>
            <th style="width: 60px;">#</th>
            <th>Player</th>
            <th style="text-align: right;">Value</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in entries" :key="entry.uuid" :class="{ 'top-3': entry.rank <= 3 }">
            <td>
              <span :class="['rank-num', 'rank-' + entry.rank]">{{ entry.rank }}</span>
            </td>
            <td>
              <router-link :to="'/players/' + entry.uuid" class="player-link">
                {{ entry.name }}
              </router-link>
            </td>
            <td style="text-align: right; font-weight: 600;">
              {{ formatValue(selectedStat, entry.value) }}
            </td>
          </tr>
          <tr v-if="entries.length === 0">
            <td colspan="3" style="text-align: center; color: var(--text-muted);">
              No data available for this stat.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.public-leaderboard {
  max-width: 800px;
  margin: 0 auto;
}

.subtitle {
  color: var(--text-muted);
  margin: 4px 0 0;
}

.stat-selector {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat-btn {
  background: var(--bg-input);
  color: var(--text-secondary);
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.stat-btn:hover {
  color: var(--text-primary);
  background: rgba(99, 102, 241, 0.15);
}

.stat-btn.active {
  background: var(--accent);
  color: white;
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

.rank-num {
  font-weight: 700;
  font-size: 14px;
}

.rank-1 { color: #ffd700; }
.rank-2 { color: #c0c0c0; }
.rank-3 { color: #cd7f32; }

.top-3 {
  background: rgba(99, 102, 241, 0.03);
}
</style>

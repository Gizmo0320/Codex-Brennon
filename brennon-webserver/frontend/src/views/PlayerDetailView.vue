<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const uuid = route.params.uuid as string
const player = ref<any>(null)
const punishments = ref<any[]>([])
const stats = ref<Record<string, number>>({})
const balance = ref(0)
const loading = ref(true)

onMounted(async () => {
  try {
    const [playerRes, punishRes, econRes] = await Promise.all([
      api.get(`/players/${uuid}`),
      api.get(`/punishments/${uuid}`),
      api.get(`/economy/${uuid}`)
    ])
    player.value = playerRes.data
    punishments.value = punishRes.data
    balance.value = econRes.data.balance

    try {
      const statsRes = await api.get(`/stats/${uuid}`)
      stats.value = statsRes.data.stats
    } catch { /* stats module may be disabled */ }
  } catch (e) {
    console.error('Failed to load player:', e)
  }
  loading.value = false
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Player Detail</h1>
    </div>

    <div v-if="loading" class="card">Loading...</div>

    <template v-if="player">
      <div class="card">
        <h2>{{ player.name }}</h2>
        <p style="color: var(--text-secondary); margin-top: 4px;">{{ player.uuid }}</p>
        <div style="margin-top: 12px; display: flex; gap: 16px; flex-wrap: wrap;">
          <div><strong>Ranks:</strong> {{ player.ranks.join(', ') || 'None' }}</div>
          <div><strong>Server:</strong> {{ player.lastServer || 'Offline' }}</div>
          <div>
            <strong>Status:</strong>
            <span :class="['badge', player.online ? 'online' : 'offline']">
              {{ player.online ? 'Online' : 'Offline' }}
            </span>
          </div>
          <div><strong>Balance:</strong> ${{ balance.toFixed(2) }}</div>
        </div>
        <div style="margin-top: 8px; display: flex; gap: 16px;">
          <div><strong>First Join:</strong> {{ player.firstJoin }}</div>
          <div><strong>Last Seen:</strong> {{ player.lastSeen }}</div>
        </div>
      </div>

      <div class="card" v-if="Object.keys(stats).length > 0">
        <h3 style="margin-bottom: 12px;">Stats</h3>
        <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 8px;">
          <div v-for="(val, key) in stats" :key="key" style="padding: 8px; background: var(--bg-input); border-radius: 6px;">
            <div style="color: var(--text-secondary); font-size: 12px;">{{ key }}</div>
            <div style="font-size: 18px; font-weight: 600;">{{ val }}</div>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 style="margin-bottom: 12px;">Punishment History ({{ punishments.length }})</h3>
        <table>
          <thead>
            <tr><th>Type</th><th>Reason</th><th>Issued</th><th>Active</th></tr>
          </thead>
          <tbody>
            <tr v-for="p in punishments" :key="p.id">
              <td>{{ p.type }}</td>
              <td>{{ p.reason }}</td>
              <td>{{ p.issuedAt }}</td>
              <td>
                <span :class="['badge', p.isActive ? 'online' : 'offline']">
                  {{ p.isActive ? 'Active' : 'Expired' }}
                </span>
              </td>
            </tr>
            <tr v-if="punishments.length === 0">
              <td colspan="4" style="text-align: center; color: var(--text-secondary);">Clean record</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import api from '../api'

const statId = ref('kills')
const leaderboard = ref<any[]>([])
const playerName = ref('')
const playerStats = ref<Record<string, number>>({})

async function loadLeaderboard() {
  const res = await api.get(`/leaderboard/${statId.value}?limit=20`)
  leaderboard.value = res.data.entries
}

async function lookupPlayer() {
  try {
    const playerRes = await api.get(`/players/name/${playerName.value}`)
    const res = await api.get(`/stats/${playerRes.data.uuid}`)
    playerStats.value = res.data.stats
  } catch {
    playerStats.value = {}
    alert('Player not found')
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Stats & Leaderboards</h1>
    </div>

    <div class="card" style="margin-bottom: 16px;">
      <h3 style="margin-bottom: 12px;">Leaderboard</h3>
      <div style="display: flex; gap: 8px; margin-bottom: 12px;">
        <input v-model="statId" placeholder="Stat ID (e.g. kills)" @keyup.enter="loadLeaderboard" />
        <button @click="loadLeaderboard">Load</button>
      </div>
      <table v-if="leaderboard.length > 0">
        <thead>
          <tr><th>#</th><th>Player</th><th>Value</th></tr>
        </thead>
        <tbody>
          <tr v-for="entry in leaderboard" :key="entry.uuid">
            <td>{{ entry.rank }}</td>
            <td>{{ entry.name }}</td>
            <td>{{ entry.value }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <h3 style="margin-bottom: 12px;">Player Lookup</h3>
      <div style="display: flex; gap: 8px; margin-bottom: 12px;">
        <input v-model="playerName" placeholder="Player name..." @keyup.enter="lookupPlayer" />
        <button @click="lookupPlayer">Lookup</button>
      </div>
      <div v-if="Object.keys(playerStats).length > 0" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 8px;">
        <div v-for="(val, key) in playerStats" :key="key" style="padding: 8px; background: var(--bg-input); border-radius: 6px;">
          <div style="color: var(--text-secondary); font-size: 12px;">{{ key }}</div>
          <div style="font-size: 18px; font-weight: 600;">{{ val }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

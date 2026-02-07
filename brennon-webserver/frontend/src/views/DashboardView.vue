<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import StatCard from '../components/StatCard.vue'
import ActivityFeed from '../components/ActivityFeed.vue'

const playerCount = ref(0)
const serverCount = ref(0)
const openTickets = ref(0)
const modules = ref<Record<string, boolean>>({})

onMounted(async () => {
  try {
    const [serversRes, modulesRes] = await Promise.all([
      api.get('/servers/online'),
      api.get('/modules')
    ])
    playerCount.value = serversRes.data.totalPlayers
    serverCount.value = serversRes.data.serverCount
    modules.value = modulesRes.data

    if (modules.value.tickets) {
      const ticketsRes = await api.get('/tickets')
      openTickets.value = ticketsRes.data.length
    }
  } catch (e) {
    console.error('Dashboard load error:', e)
  }
})
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Dashboard</h1>
    </div>

    <div class="stat-grid">
      <StatCard label="Online Players" :value="playerCount" color="var(--success)" />
      <StatCard label="Servers Online" :value="serverCount" color="var(--accent)" />
      <StatCard label="Open Tickets" :value="openTickets" color="var(--warning)" />
    </div>

    <ActivityFeed />
  </div>
</template>

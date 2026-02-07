<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const players = ref<any[]>([])
const searchName = ref('')
const searchResult = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  const res = await api.get('/players/online')
  players.value = res.data
})

async function searchPlayer() {
  if (!searchName.value) return
  loading.value = true
  try {
    const res = await api.get(`/players/name/${searchName.value}`)
    searchResult.value = res.data
    router.push(`/players/${res.data.uuid}`)
  } catch {
    searchResult.value = null
    alert('Player not found')
  }
  loading.value = false
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Players</h1>
    </div>

    <div class="card" style="margin-bottom: 16px;">
      <div style="display: flex; gap: 8px;">
        <input v-model="searchName" placeholder="Search by name..." @keyup.enter="searchPlayer" />
        <button @click="searchPlayer" :disabled="loading">Search</button>
      </div>
    </div>

    <div class="card">
      <h3 style="margin-bottom: 12px;">Online Players ({{ players.length }})</h3>
      <table>
        <thead>
          <tr><th>Name</th><th>Server</th><th>Actions</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in players" :key="p.uuid">
            <td>{{ p.name }}</td>
            <td>{{ p.server }}</td>
            <td><router-link :to="`/players/${p.uuid}`">View</router-link></td>
          </tr>
          <tr v-if="players.length === 0">
            <td colspan="3" style="text-align: center; color: var(--text-secondary);">No players online</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

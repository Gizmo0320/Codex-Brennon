<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api'
import { wsClient } from '../ws'

const staff = ref<any[]>([])

async function loadStaff() {
  const res = await api.get('/staff')
  staff.value = res.data
}

onMounted(() => {
  loadStaff()
  wsClient.on('staff_alert', loadStaff)
})
onUnmounted(() => { wsClient.off('staff_alert', loadStaff) })
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Staff</h1>
      <button @click="loadStaff">Refresh</button>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr><th>Name</th><th>Server</th><th>Vanished</th></tr>
        </thead>
        <tbody>
          <tr v-for="s in staff" :key="s.uuid">
            <td>{{ s.name }}</td>
            <td>{{ s.server }}</td>
            <td>
              <span :class="['badge', s.vanished ? 'offline' : 'online']">
                {{ s.vanished ? 'Vanished' : 'Visible' }}
              </span>
            </td>
          </tr>
          <tr v-if="staff.length === 0">
            <td colspan="3" style="text-align: center; color: var(--text-secondary);">No staff online</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

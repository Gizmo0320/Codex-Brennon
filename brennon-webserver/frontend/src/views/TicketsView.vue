<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const tickets = ref<any[]>([])

onMounted(async () => {
  const res = await api.get('/tickets')
  tickets.value = res.data
})

function priorityColor(p: string): string {
  switch (p) {
    case 'URGENT': return 'var(--danger)'
    case 'HIGH': return 'var(--warning)'
    default: return 'var(--text-secondary)'
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Tickets</h1>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr><th>ID</th><th>Subject</th><th>Creator</th><th>Status</th><th>Priority</th><th>Assignee</th></tr>
        </thead>
        <tbody>
          <tr v-for="t in tickets" :key="t.id" @click="router.push(`/tickets/${t.id}`)" style="cursor: pointer;">
            <td>{{ t.id }}</td>
            <td>{{ t.subject }}</td>
            <td>{{ t.creatorName }}</td>
            <td>{{ t.status }}</td>
            <td :style="{ color: priorityColor(t.priority) }">{{ t.priority }}</td>
            <td>{{ t.assignee || 'Unassigned' }}</td>
          </tr>
          <tr v-if="tickets.length === 0">
            <td colspan="6" style="text-align: center; color: var(--text-secondary);">No open tickets</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

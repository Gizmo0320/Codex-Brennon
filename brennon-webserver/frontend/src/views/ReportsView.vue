<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'

const reports = ref<any[]>([])

onMounted(loadReports)

async function loadReports() {
  const res = await api.get('/reports')
  reports.value = res.data
}

async function claim(id: string) {
  await api.put(`/reports/${id}/claim`, { staffUuid: '00000000-0000-0000-0000-000000000000' })
  await loadReports()
}

async function resolve(id: string, status: string) {
  await api.put(`/reports/${id}/resolve`, { status })
  await loadReports()
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Reports</h1>
      <button @click="loadReports">Refresh</button>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr><th>ID</th><th>Reporter</th><th>Target</th><th>Reason</th><th>Server</th><th>Status</th><th>Actions</th></tr>
        </thead>
        <tbody>
          <tr v-for="r in reports" :key="r.id">
            <td style="font-size: 12px;">{{ r.id.substring(0, 8) }}...</td>
            <td>{{ r.reporterName }}</td>
            <td>{{ r.targetName }}</td>
            <td>{{ r.reason }}</td>
            <td>{{ r.server }}</td>
            <td>
              <span :class="['badge', r.status === 'OPEN' ? 'online' : 'offline']">{{ r.status }}</span>
            </td>
            <td>
              <button v-if="r.status === 'OPEN'" @click="claim(r.id)" style="margin-right: 4px; font-size: 12px;">Claim</button>
              <button v-if="r.status !== 'RESOLVED'" class="success" @click="resolve(r.id, 'RESOLVED')" style="margin-right: 4px; font-size: 12px;">Resolve</button>
              <button v-if="r.status !== 'DISMISSED'" class="danger" @click="resolve(r.id, 'DISMISSED')" style="font-size: 12px;">Dismiss</button>
            </td>
          </tr>
          <tr v-if="reports.length === 0">
            <td colspan="7" style="text-align: center; color: var(--text-secondary);">No open reports</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

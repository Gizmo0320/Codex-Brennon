<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import api from './api'
import { wsClient } from './ws'

const router = useRouter()
const route = useRoute()
const modules = ref<Record<string, boolean>>({})
const isLoggedIn = computed(() => !!localStorage.getItem('brennon_token'))
const isPlayerRole = computed(() => localStorage.getItem('brennon_role') === 'player')
const isPortalRoute = computed(() => route.path.startsWith('/portal'))
const isLoginRoute = computed(() => route.path === '/login' || route.path === '/player-login')
const showSidebar = computed(() => isLoggedIn.value && !isLoginRoute.value && !isPortalRoute.value && !isPlayerRole.value)

onMounted(async () => {
  if (isLoggedIn.value && !isPlayerRole.value) {
    try {
      const res = await api.get('/modules')
      modules.value = res.data
      wsClient.connect()
    } catch {
      localStorage.removeItem('brennon_token')
      localStorage.removeItem('brennon_role')
      router.push('/login')
    }
  }
})
</script>

<template>
  <div class="app-layout">
    <Sidebar v-if="showSidebar" :modules="modules" />
    <main :class="{ 'with-sidebar': showSidebar }">
      <router-view :key="route.fullPath" />
    </main>
  </div>
</template>

<style>
:root {
  --bg-primary: #1a1a2e;
  --bg-secondary: #16213e;
  --bg-card: #1f2937;
  --bg-input: #374151;
  --text-primary: #e5e7eb;
  --text-secondary: #9ca3af;
  --accent: #6366f1;
  --accent-hover: #818cf8;
  --success: #10b981;
  --warning: #f59e0b;
  --danger: #ef4444;
  --sidebar-width: 240px;
}

body {
  background: var(--bg-primary);
  color: var(--text-primary);
  min-height: 100vh;
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

main.with-sidebar {
  margin-left: var(--sidebar-width);
}

a { color: var(--accent); text-decoration: none; }
a:hover { color: var(--accent-hover); }

button {
  background: var(--accent);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
button:hover { background: var(--accent-hover); }
button.danger { background: var(--danger); }
button.danger:hover { background: #dc2626; }
button.success { background: var(--success); }

input, select, textarea {
  background: var(--bg-input);
  color: var(--text-primary);
  border: 1px solid #4b5563;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  width: 100%;
}
input:focus, select:focus, textarea:focus {
  outline: none;
  border-color: var(--accent);
}

.card {
  background: var(--bg-card);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-header h1 { font-size: 24px; font-weight: 600; }

table {
  width: 100%;
  border-collapse: collapse;
}
th, td {
  text-align: left;
  padding: 12px;
  border-bottom: 1px solid #374151;
}
th {
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 13px;
  text-transform: uppercase;
}
tr:hover { background: rgba(99, 102, 241, 0.05); }

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.badge.online { background: rgba(16, 185, 129, 0.2); color: var(--success); }
.badge.offline { background: rgba(239, 68, 68, 0.2); color: var(--danger); }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  margin-bottom: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>

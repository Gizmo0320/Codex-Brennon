<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = defineProps<{ modules: Record<string, boolean> }>()
const router = useRouter()

const navItems = [
  { path: '/admin', label: 'Dashboard', icon: '~', always: true },
  { path: '/admin/players', label: 'Players', icon: 'P', always: true },
  { path: '/admin/ranks', label: 'Ranks', icon: 'R', module: 'ranks' },
  { path: '/admin/punishments', label: 'Punishments', icon: '!', module: 'punishments' },
  { path: '/admin/economy', label: 'Economy', icon: '$', module: 'economy' },
  { path: '/admin/tickets', label: 'Tickets', icon: 'T', module: 'tickets' },
  { path: '/admin/appeals', label: 'Appeals', icon: 'A', module: 'punishments' },
  { path: '/admin/stats', label: 'Stats', icon: '#', module: 'stats' },
  { path: '/admin/servers', label: 'Servers', icon: 'S', module: 'serverManager' },
  { path: '/admin/pterodactyl', label: 'Pterodactyl', icon: 'D', module: 'pterodactyl' },
  { path: '/admin/chat', label: 'Chat', icon: 'C', module: 'chat' },
  { path: '/admin/staff', label: 'Staff', icon: '*', module: 'staffTools' },
  { path: '/admin/reports', label: 'Reports', icon: '?', module: 'staffTools' }
]

function isVisible(item: any): boolean {
  if (item.always) return true
  return !item.module || props.modules[item.module] !== false
}

function logout() {
  localStorage.removeItem('brennon_token')
  localStorage.removeItem('brennon_role')
  localStorage.removeItem('brennon_player_name')
  localStorage.removeItem('brennon_player_uuid')
  router.push('/login')
}
</script>

<template>
  <nav class="sidebar">
    <div class="sidebar-header">
      <h2>Brennon</h2>
      <span class="version">v2.0</span>
    </div>

    <div class="nav-items">
      <router-link
        v-for="item in navItems.filter(isVisible)"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        active-class="active"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </router-link>
    </div>

    <div class="sidebar-footer">
      <button class="logout-btn" @click="logout">Logout</button>
    </div>
  </nav>
</template>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--bg-secondary);
  display: flex;
  flex-direction: column;
  border-right: 1px solid #2d3748;
  z-index: 100;
}

.sidebar-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #2d3748;
}
.sidebar-header h2 { font-size: 20px; font-weight: 700; }
.version { color: var(--text-secondary); font-size: 12px; }

.nav-items {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 6px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.15s;
}
.nav-item:hover { background: rgba(99, 102, 241, 0.1); color: var(--text-primary); }
.nav-item.active { background: var(--accent); color: white; }

.nav-icon {
  width: 20px;
  text-align: center;
  font-weight: 700;
  font-size: 13px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #2d3748;
}
.logout-btn {
  width: 100%;
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid #4b5563;
}
.logout-btn:hover { color: var(--danger); border-color: var(--danger); }
</style>

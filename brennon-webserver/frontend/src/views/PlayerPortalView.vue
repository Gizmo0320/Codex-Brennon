<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const profile = ref<any>(null)
const punishments = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [profileRes, punishRes] = await Promise.all([
      api.get('/player/profile'),
      api.get('/player/punishments').catch(() => ({ data: [] }))
    ])
    profile.value = profileRes.data
    punishments.value = punishRes.data
  } catch {
    alert('Failed to load profile')
  }
  loading.value = false
})

function logout() {
  localStorage.removeItem('brennon_token')
  localStorage.removeItem('brennon_role')
  localStorage.removeItem('brennon_player_name')
  localStorage.removeItem('brennon_player_uuid')
  router.push('/player-login')
}
</script>

<template>
  <div class="portal">
    <div class="portal-nav">
      <div class="portal-brand">
        <h2>Brennon</h2>
        <span class="version">Player Portal</span>
      </div>
      <div class="portal-links">
        <router-link to="/portal" class="nav-link" active-class="active" :exact="true">Profile</router-link>
        <router-link to="/portal/appeals" class="nav-link" active-class="active">Appeals</router-link>
        <router-link to="/portal/tickets" class="nav-link" active-class="active">Tickets</router-link>
      </div>
      <button class="logout-btn" @click="logout">Logout</button>
    </div>

    <div class="portal-content" v-if="!loading">
      <template v-if="profile">
        <div class="page-header">
          <h1>Welcome, {{ profile.name }}</h1>
          <span class="badge" :class="profile.online ? 'online' : 'offline'">
            {{ profile.online ? 'Online' : 'Offline' }}
          </span>
        </div>

        <div class="stat-grid">
          <div class="stat-card card">
            <div class="stat-label">Balance</div>
            <div class="stat-value">${{ (profile.balance ?? 0).toLocaleString() }}</div>
          </div>
          <div class="stat-card card">
            <div class="stat-label">Ranks</div>
            <div class="stat-value">{{ profile.ranks?.map((r: any) => r.displayName).join(', ') || 'None' }}</div>
          </div>
          <div class="stat-card card">
            <div class="stat-label">Current Server</div>
            <div class="stat-value">{{ profile.currentServer || 'N/A' }}</div>
          </div>
          <div class="stat-card card">
            <div class="stat-label">First Joined</div>
            <div class="stat-value">{{ new Date(profile.firstJoin).toLocaleDateString() }}</div>
          </div>
        </div>

        <div v-if="profile.stats && Object.keys(profile.stats).length > 0" class="card">
          <h3 style="margin-bottom: 12px;">Your Stats</h3>
          <div class="stats-grid">
            <div v-for="(value, key) in profile.stats" :key="key" class="stat-box">
              <div class="stat-box-label">{{ key }}</div>
              <div class="stat-box-value">{{ value }}</div>
            </div>
          </div>
        </div>

        <div v-if="punishments.length > 0" class="card">
          <h3 style="margin-bottom: 12px;">Active Punishments</h3>
          <table>
            <thead>
              <tr>
                <th>Type</th>
                <th>Reason</th>
                <th>Issued</th>
                <th>Expires</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in punishments" :key="p.id">
                <td><span class="badge offline">{{ p.type }}</span></td>
                <td>{{ p.reason }}</td>
                <td>{{ new Date(p.issuedAt).toLocaleDateString() }}</td>
                <td>{{ p.isPermanent ? 'Permanent' : (p.expiresAt ? new Date(p.expiresAt).toLocaleDateString() : 'N/A') }}</td>
                <td>
                  <router-link :to="`/portal/appeals?punishmentId=${p.id}`">
                    <button style="font-size: 12px; padding: 4px 10px;">Appeal</button>
                  </router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>
    <div v-else class="portal-content" style="text-align: center; padding-top: 100px; color: var(--text-secondary);">
      Loading...
    </div>
  </div>
</template>

<style scoped>
.portal { min-height: 100vh; }
.portal-nav {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid #2d3748;
}
.portal-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.portal-brand h2 { font-size: 18px; font-weight: 700; }
.version { color: var(--text-secondary); font-size: 12px; }
.portal-links { display: flex; gap: 4px; margin-left: 24px; }
.nav-link {
  padding: 8px 16px;
  border-radius: 6px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.15s;
}
.nav-link:hover { background: rgba(99, 102, 241, 0.1); color: var(--text-primary); }
.nav-link.active { background: var(--accent); color: white; }
.logout-btn {
  margin-left: auto;
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid #4b5563;
  font-size: 13px;
}
.logout-btn:hover { color: var(--danger); border-color: var(--danger); }
.portal-content { padding: 24px; max-width: 1000px; margin: 0 auto; }
.stat-card { text-align: center; }
.stat-label { color: var(--text-secondary); font-size: 12px; text-transform: uppercase; margin-bottom: 4px; }
.stat-value { font-size: 18px; font-weight: 600; }
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}
.stat-box {
  background: var(--bg-input);
  padding: 8px;
  border-radius: 6px;
  text-align: center;
}
.stat-box-label { color: var(--text-secondary); font-size: 11px; text-transform: uppercase; }
.stat-box-value { font-size: 16px; font-weight: 600; margin-top: 2px; }
</style>
